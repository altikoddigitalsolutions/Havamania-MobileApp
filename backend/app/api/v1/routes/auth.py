from fastapi import APIRouter, Depends, status
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.schemas.auth import LoginRequest, MessageResponse, RefreshRequest, SignupRequest, TokenResponse
from app.services.auth_service import login, logout, refresh, signup

router = APIRouter()


@router.post("/signup", response_model=TokenResponse, status_code=status.HTTP_201_CREATED)
def signup_endpoint(payload: SignupRequest, db: Session = Depends(get_db)) -> TokenResponse:
    user = signup(db=db, email=payload.email, password=payload.password, full_name=payload.full_name)
    access_token, refresh_token = login(db=db, email=payload.email, password=payload.password)
    return TokenResponse(access_token=access_token, refresh_token=refresh_token)


@router.post("/login", response_model=TokenResponse)
def login_endpoint(payload: LoginRequest, db: Session = Depends(get_db)) -> TokenResponse:
    access_token, refresh_token = login(db=db, email=payload.email, password=payload.password)
    return TokenResponse(access_token=access_token, refresh_token=refresh_token)


@router.post("/refresh", response_model=TokenResponse)
def refresh_endpoint(payload: RefreshRequest, db: Session = Depends(get_db)) -> TokenResponse:
    access_token, refresh_token = refresh(db=db, refresh_token=payload.refresh_token)
    return TokenResponse(access_token=access_token, refresh_token=refresh_token)


@router.post("/logout", response_model=MessageResponse)
def logout_endpoint(payload: RefreshRequest, db: Session = Depends(get_db)) -> MessageResponse:
    logout(db=db, refresh_token=payload.refresh_token)
    return MessageResponse(detail="Logged out")
