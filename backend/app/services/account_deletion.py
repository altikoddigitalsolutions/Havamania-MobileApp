import logging
import time
from hashlib import sha256
from pathlib import Path
from uuid import NAMESPACE_URL, uuid5

from fastapi import HTTPException
from firebase_admin import auth, firestore, storage
from google.api_core.exceptions import NotFound
from sqlalchemy import delete, select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.core.config import get_settings
from app.models.account_deletion import AccountDeletion
from app.models.user import User
from app.services.firebase_identity import firebase_app, verify_firebase_token

logger = logging.getLogger(__name__)
AVATAR_DIR = Path(__file__).resolve().parents[2] / "static" / "avatars"


class FirebaseDeletionGateway:
    def __init__(self, project_id: str):
        settings = get_settings()
        if not settings.firebase_storage_bucket or project_id != settings.firebase_project_id:
            raise RuntimeError("Account deletion project/bucket configuration mismatch")
        self.app = firebase_app(project_id)
        self.db = firestore.client(app=self.app)
        self.bucket = storage.bucket(settings.firebase_storage_bucket, app=self.app)

    def freeze(self, uid: str):
        # Rules deny stale-token writes after this marker, including profile recreation.
        self.db.collection("account_deletions").document(uid).set({"blocked": True})
        try:
            auth.update_user(uid, disabled=True, app=self.app)
            auth.revoke_refresh_tokens(uid, app=self.app)
        except auth.UserNotFoundError:
            pass

    def remove_storage(self, uid: str):
        for blob in self.bucket.list_blobs(prefix=f"profile-images/{uid}/"):
            try:
                blob.delete()
            except NotFound:
                pass

    def remove_documents(self, uid: str):
        failures = []
        writer = self.db.bulk_writer()

        def on_error(error, _writer):
            if error.attempts < 3:
                return True
            failures.append(error)
            return False

        writer.on_write_error(on_error)
        try:
            self.db.recursive_delete(self.db.collection("users").document(uid), bulk_writer=writer)
        finally:
            writer.close()
        # BulkWriter otherwise silently stops after exhausting retries.
        if failures:
            raise RuntimeError("Firestore document cleanup incomplete")

    def remove_identity(self, uid: str):
        try:
            auth.delete_user(uid, app=self.app)
        except auth.UserNotFoundError:
            pass


def prepare_deletion(db: Session, key: str, token: str | None) -> str:
    settings = get_settings()
    digest = sha256(key.encode()).hexdigest()
    existing = db.get(AccountDeletion, digest)
    if existing:
        return digest
    if (not settings.account_deletion_enabled or not settings.firebase_project_id
            or not settings.firebase_storage_bucket):
        raise HTTPException(503, "Account deletion is not configured")
    if not token:
        raise HTTPException(401, "Recent Firebase authentication required")
    try:
        claims = verify_firebase_token(token, settings.firebase_project_id)
    except (ValueError, auth.InvalidIdTokenError, auth.UserDisabledError, auth.RevokedIdTokenError) as exc:
        raise HTTPException(401, "Recent Firebase authentication required") from exc
    uid = claims.get("uid") or claims.get("sub")
    auth_time = claims.get("auth_time")
    if not isinstance(uid, str) or not uid or len(uid) > 128 or "/" in uid:
        raise HTTPException(401, "Invalid identity")
    if not isinstance(auth_time, (int, float)) or not 0 <= time.time() - auth_time <= 300:
        raise HTTPException(401, "Please authenticate again before deleting the account")
    db.add(AccountDeletion(key_hash=digest, firebase_uid=uid,
                           project_id=settings.firebase_project_id,
                           storage_bucket=settings.firebase_storage_bucket, status="pending"))
    try:
        db.commit()
    except IntegrityError:
        db.rollback()
        if db.get(AccountDeletion, digest) is None:
            raise
    return digest


def execute_deletion(db: Session, digest: str, gateway_factory=FirebaseDeletionGateway) -> bool:
    # Lock the durable job for the operation. A crash rolls back DB changes; external
    # deletion steps are idempotent and will run again using the same continuation key.
    job = db.scalar(select(AccountDeletion).where(AccountDeletion.key_hash == digest).with_for_update())
    if job is None:
        raise ValueError("Unknown deletion job")
    if job.status == "complete":
        db.rollback()
        return True
    try:
        uid, project = job.firebase_uid, job.project_id
        settings = get_settings()
        if project != settings.firebase_project_id or job.storage_bucket != settings.firebase_storage_bucket:
            raise RuntimeError("Deletion job configuration changed; restore original project/bucket")
        gateway = gateway_factory(project)
        gateway.freeze(uid)
        gateway.remove_storage(uid)
        gateway.remove_documents(uid)
        user_id = str(uuid5(NAMESPACE_URL, f"https://securetoken.google.com/{project}/{uid}"))
        for avatar in AVATAR_DIR.glob(f"{user_id}_*"):
            if avatar.is_file():
                avatar.unlink(missing_ok=True)
        db.execute(delete(User).where(User.id == user_id))
        db.flush()  # Verify relational cascades before removing the Firebase identity.
        gateway.remove_identity(uid)
        job.status = "complete"
        job.firebase_uid = None
        job.project_id = None
        job.storage_bucket = None
        db.commit()
        return True
    except Exception:
        db.rollback()
        logger.exception("Account deletion pending; retry required")
        return False
