from fastapi import APIRouter

from app.api.v1.routes import alerts, auth, chatbot, devices, metrics, profile, subscription, weather

api_router = APIRouter()
api_router.include_router(auth.router, prefix="/auth", tags=["auth"])
api_router.include_router(profile.router, prefix="/profile", tags=["profile"])
api_router.include_router(weather.router, prefix="/weather", tags=["weather"])
api_router.include_router(alerts.router, prefix="/alerts", tags=["alerts"])
api_router.include_router(chatbot.router, prefix="/chatbot", tags=["chatbot"])
api_router.include_router(subscription.router, prefix="/subscription", tags=["subscription"])
api_router.include_router(devices.router, prefix="/devices", tags=["devices"])
api_router.include_router(metrics.router, prefix="/metrics", tags=["metrics"])
