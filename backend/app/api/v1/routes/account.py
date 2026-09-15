from fastapi import APIRouter, Depends, Header, Response
from pydantic import BaseModel, Field
from sqlalchemy.orm import Session

from app.core.config import get_settings
from app.db.session import get_db
from app.services.account_deletion import execute_deletion, prepare_deletion

router = APIRouter()
AUTHORIZATION = Header(default=None)
DATABASE = Depends(get_db)


class DeletionRequest(BaseModel):
    continuation_key: str = Field(pattern=r"^[0-9a-f]{64}$")


@router.get("/deletion/availability")
def deletion_availability(response: Response):
    settings = get_settings()
    response.headers["Cache-Control"] = "no-store"
    return {"available": bool(settings.account_deletion_enabled and settings.firebase_project_id
                              and settings.firebase_storage_bucket)}


@router.post("/deletion")
def delete_account(payload: DeletionRequest, response: Response,
                   authorization: str | None = AUTHORIZATION,
                   db: Session = DATABASE):
    token = authorization[7:] if authorization and authorization.startswith("Bearer ") else None
    digest = prepare_deletion(db, payload.continuation_key, token)
    complete = execute_deletion(db, digest)
    response.status_code = 200 if complete else 202
    response.headers["Cache-Control"] = "no-store"
    return {"status": "complete" if complete else "pending"}
