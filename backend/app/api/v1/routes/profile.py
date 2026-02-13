from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.dependencies.auth import get_current_user
from app.models.location import Location
from app.models.notification_preference import NotificationPreference
from app.models.profile import Profile
from app.models.user import User
from app.schemas.profile import (
    LocationCreateRequest,
    LocationResponse,
    LocationUpdateRequest,
    NotificationPreferenceResponse,
    NotificationPreferenceUpdateRequest,
    ProfileResponse,
    ProfileUpdateRequest,
)

router = APIRouter()


def _ensure_profile(db: Session, user_id: str) -> Profile:
    profile = db.get(Profile, user_id)
    if not profile:
        profile = Profile(user_id=user_id)
        db.add(profile)
        db.commit()
        db.refresh(profile)
    return profile


def _ensure_notification_preferences(db: Session, user_id: str) -> NotificationPreference:
    prefs = db.get(NotificationPreference, user_id)
    if not prefs:
        prefs = NotificationPreference(user_id=user_id)
        db.add(prefs)
        db.commit()
        db.refresh(prefs)
    return prefs


def _apply_primary_location_uniqueness(db: Session, user_id: str, primary_location_id: str) -> None:
    db.query(Location).filter(Location.user_id == user_id, Location.id != primary_location_id).update(
        {Location.is_primary: False}, synchronize_session=False
    )


@router.get("", response_model=ProfileResponse)
def get_profile(current_user: User = Depends(get_current_user), db: Session = Depends(get_db)) -> ProfileResponse:
    profile = _ensure_profile(db, current_user.id)
    return ProfileResponse(
        user_id=profile.user_id,
        primary_location_id=profile.primary_location_id,
        temperature_unit=profile.temperature_unit,
        wind_unit=profile.wind_unit,
        theme=profile.theme,
    )


@router.patch("", response_model=ProfileResponse)
def update_profile(
    payload: ProfileUpdateRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> ProfileResponse:
    profile = _ensure_profile(db, current_user.id)

    if payload.temperature_unit is not None:
        profile.temperature_unit = payload.temperature_unit
    if payload.wind_unit is not None:
        profile.wind_unit = payload.wind_unit
    if payload.theme is not None:
        profile.theme = payload.theme

    if payload.primary_location_id is not None:
        location = (
            db.query(Location)
            .filter(Location.id == payload.primary_location_id, Location.user_id == current_user.id)
            .first()
        )
        if not location:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Location not found")

        location.is_primary = True
        _apply_primary_location_uniqueness(db, current_user.id, location.id)
        profile.primary_location_id = location.id

    db.add(profile)
    db.commit()
    db.refresh(profile)

    return ProfileResponse(
        user_id=profile.user_id,
        primary_location_id=profile.primary_location_id,
        temperature_unit=profile.temperature_unit,
        wind_unit=profile.wind_unit,
        theme=profile.theme,
    )


@router.get("/locations", response_model=list[LocationResponse])
def list_locations(
    current_user: User = Depends(get_current_user), db: Session = Depends(get_db)
) -> list[LocationResponse]:
    locations = db.query(Location).filter(Location.user_id == current_user.id).all()
    return [
        LocationResponse(
            id=location.id,
            user_id=location.user_id,
            label=location.label,
            lat=location.lat,
            lon=location.lon,
            is_primary=location.is_primary,
            is_tracking_enabled=location.is_tracking_enabled,
        )
        for location in locations
    ]


@router.post("/locations", response_model=LocationResponse, status_code=status.HTTP_201_CREATED)
def create_location(
    payload: LocationCreateRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> LocationResponse:
    location = Location(
        user_id=current_user.id,
        label=payload.label,
        lat=payload.lat,
        lon=payload.lon,
        is_primary=payload.is_primary,
        is_tracking_enabled=payload.is_tracking_enabled,
    )

    db.add(location)
    db.flush()

    profile = _ensure_profile(db, current_user.id)

    if payload.is_primary:
        _apply_primary_location_uniqueness(db, current_user.id, location.id)
        profile.primary_location_id = location.id
        db.add(profile)

    db.commit()
    db.refresh(location)

    return LocationResponse(
        id=location.id,
        user_id=location.user_id,
        label=location.label,
        lat=location.lat,
        lon=location.lon,
        is_primary=location.is_primary,
        is_tracking_enabled=location.is_tracking_enabled,
    )


@router.patch("/locations/{location_id}", response_model=LocationResponse)
def update_location(
    location_id: str,
    payload: LocationUpdateRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> LocationResponse:
    location = (
        db.query(Location).filter(Location.id == location_id, Location.user_id == current_user.id).first()
    )
    if not location:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Location not found")

    if payload.label is not None:
        location.label = payload.label
    if payload.lat is not None:
        location.lat = payload.lat
    if payload.lon is not None:
        location.lon = payload.lon
    if payload.is_tracking_enabled is not None:
        location.is_tracking_enabled = payload.is_tracking_enabled

    profile = _ensure_profile(db, current_user.id)

    if payload.is_primary is True:
        location.is_primary = True
        _apply_primary_location_uniqueness(db, current_user.id, location.id)
        profile.primary_location_id = location.id
        db.add(profile)
    elif payload.is_primary is False:
        location.is_primary = False
        if profile.primary_location_id == location.id:
            profile.primary_location_id = None
            db.add(profile)

    db.add(location)
    db.commit()
    db.refresh(location)

    return LocationResponse(
        id=location.id,
        user_id=location.user_id,
        label=location.label,
        lat=location.lat,
        lon=location.lon,
        is_primary=location.is_primary,
        is_tracking_enabled=location.is_tracking_enabled,
    )


@router.delete("/locations/{location_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_location(
    location_id: str,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> None:
    location = (
        db.query(Location).filter(Location.id == location_id, Location.user_id == current_user.id).first()
    )
    if not location:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Location not found")

    profile = _ensure_profile(db, current_user.id)
    if profile.primary_location_id == location.id:
        profile.primary_location_id = None
        db.add(profile)

    db.delete(location)
    db.commit()


@router.get("/notifications", response_model=NotificationPreferenceResponse)
def get_notification_preferences(
    current_user: User = Depends(get_current_user), db: Session = Depends(get_db)
) -> NotificationPreferenceResponse:
    prefs = _ensure_notification_preferences(db, current_user.id)
    return NotificationPreferenceResponse(
        user_id=prefs.user_id,
        severe_alert_enabled=prefs.severe_alert_enabled,
        daily_summary_enabled=prefs.daily_summary_enabled,
        rain_alert_enabled=prefs.rain_alert_enabled,
    )


@router.patch("/notifications", response_model=NotificationPreferenceResponse)
def update_notification_preferences(
    payload: NotificationPreferenceUpdateRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> NotificationPreferenceResponse:
    prefs = _ensure_notification_preferences(db, current_user.id)

    if payload.severe_alert_enabled is not None:
        prefs.severe_alert_enabled = payload.severe_alert_enabled
    if payload.daily_summary_enabled is not None:
        prefs.daily_summary_enabled = payload.daily_summary_enabled
    if payload.rain_alert_enabled is not None:
        prefs.rain_alert_enabled = payload.rain_alert_enabled

    db.add(prefs)
    db.commit()
    db.refresh(prefs)

    return NotificationPreferenceResponse(
        user_id=prefs.user_id,
        severe_alert_enabled=prefs.severe_alert_enabled,
        daily_summary_enabled=prefs.daily_summary_enabled,
        rain_alert_enabled=prefs.rain_alert_enabled,
    )
