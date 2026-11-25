"""Local-only real Admin SDK revocation acceptance; never load production credentials."""
import os
import sys
import time
from pathlib import Path
from urllib.parse import urlsplit
from uuid import uuid4

if os.environ.get("FIREBASE_AUTH_EMULATOR_HOST") != "127.0.0.1:9096":
    raise RuntimeError("Only the fixed local Auth emulator is permitted")
os.environ.update(APP_ENV="test", FIREBASE_PROJECT_ID="demo-havamania-audit",
                  DATABASE_URL="sqlite://", SECRET_KEY="local-emulator-test-only",
                  SENTRY_DSN_BACKEND="", GOOGLE_APPLICATION_CREDENTIALS="")
sys.path.insert(0, str(Path(__file__).resolve().parents[2] / "backend"))

import requests
from firebase_admin import auth, delete_app
from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import Session
from sqlalchemy.pool import StaticPool
from app import models  # register metadata
from app.db.base import Base
from app.db.session import get_db
from app.main import app
from app.services.firebase_identity import firebase_app

# Block accidental external network even if a library ignores emulator configuration.
original_send = requests.adapters.HTTPAdapter.send

def local_send(self, request, **kwargs):
    target = urlsplit(request.url)
    if target.scheme != "http" or target.hostname != "127.0.0.1" or target.port != 9096:
        raise RuntimeError("External network is forbidden in this acceptance test")
    kwargs["timeout"] = 5
    return original_send(self, request, **kwargs)

requests.adapters.HTTPAdapter.send = local_send
sdk_app = firebase_app("demo-havamania-audit")
engine = create_engine("sqlite://", poolclass=StaticPool, connect_args={"check_same_thread": False})
Base.metadata.create_all(engine)
created = []
password = "LocalOnly123!"

def sign_in(email):
    response = requests.post(
        "http://127.0.0.1:9096/identitytoolkit.googleapis.com/v1/accounts:signInWithPassword",
        params={"key": "local-emulator-key"},
        json={"email": email, "password": password, "returnSecureToken": True}, timeout=5)
    response.raise_for_status()
    return response.json()["idToken"]

def db_override():
    with Session(engine) as session:
        yield session

app.dependency_overrides[get_db] = db_override
try:
    with TestClient(app) as client:
        def profile(token):
            return client.get("/v1/profile", headers={"Authorization": "Bearer " + token})

        for mode in ("revoked", "disabled", "deleted"):
            user = auth.create_user(email=f"{uuid4().hex}@example.test", password=password, app=sdk_app)
            created.append(user.uid)
            token = sign_in(user.email)
            accepted = profile(token)
            assert accepted.status_code == 200, (mode, accepted.status_code)
            owner = accepted.json()["user_id"]
            if mode == "revoked":
                # SDK compares whole-second token iat with validSince; cross that boundary.
                issued = auth.verify_id_token(token, app=sdk_app)["iat"]
                deadline = time.monotonic() + 4
                while int(time.time()) <= issued:
                    assert time.monotonic() < deadline
                    time.sleep(0.05)
                auth.revoke_refresh_tokens(user.uid, app=sdk_app)
                assert auth.get_user(user.uid, app=sdk_app).tokens_valid_after_timestamp > issued * 1000
            elif mode == "disabled":
                auth.update_user(user.uid, disabled=True, app=sdk_app)
            else:
                auth.delete_user(user.uid, app=sdk_app)
                created.remove(user.uid)
            rejected = profile(token)
            assert rejected.status_code == 401, (mode, rejected.status_code)
            assert rejected.json() == {"detail": "Invalid Firebase credentials"}
            if mode != "deleted":
                if mode == "disabled":
                    auth.update_user(user.uid, disabled=False, app=sdk_app)
                fresh = sign_in(user.email)
                recovered = profile(fresh)
                assert recovered.status_code == 200, (mode, recovered.status_code)
                assert recovered.json()["user_id"] == owner
                if mode == "revoked":
                    assert profile(token).status_code == 401
            print(f"PASS {mode}: accepted -> rejected" + (" -> explicit login recovered" if mode != "deleted" else ""))
finally:
    for uid in created:
        auth.delete_user(uid, app=sdk_app)
    app.dependency_overrides.pop(get_db, None)
    engine.dispose()
    delete_app(sdk_app)
    firebase_app.cache_clear()
    requests.adapters.HTTPAdapter.send = original_send
