from uuid import NAMESPACE_URL, uuid5

from app.core.config import get_settings
from app.models.account_deletion import AccountDeletion
from app.models.location import Location
from app.models.profile import Profile
from app.models.push_token import PushToken
from app.models.refresh_token import RefreshToken
from app.models.user import User
from app.services import firebase_identity
from sqlalchemy import func, select


def test_pending_deletion_blocks_writes_login_refresh_and_preserves_other_account(client, db_session, tmp_path, monkeypatch):
    from app.api.v1.routes import profile
    monkeypatch.setattr(profile, 'AVATAR_DIR', str(tmp_path))
    tokens = client.post('/v1/auth/signup', json={'email': 'deleting@example.com', 'password': 'Password123'}).json()
    other = client.post('/v1/auth/signup', json={'email': 'other@example.com', 'password': 'Password123'}).json()
    headers = {'Authorization': 'Bearer ' + tokens['access_token']}
    uid = db_session.scalar(select(User.id).where(User.email == 'deleting@example.com'))
    refresh_count = db_session.scalar(select(func.count()).select_from(RefreshToken))
    db_session.add(AccountDeletion(key_hash='d' * 64, backend_user_id=uid, status='pending'))
    db_session.commit()
    requests = [
        ('PATCH', '/v1/profile', {'json': {'theme': 'dark'}}),
        ('POST', '/v1/profile/locations', {'json': {'label': 'City', 'lat': 1, 'lon': 2}}),
        ('PATCH', '/v1/profile/notifications', {'json': {'rain_alert_enabled': False}}),
        ('POST', '/v1/profile/avatar', {'files': {'file': ('avatar.png', b'\x89PNG\r\n\x1a\n', 'image/png')}}),
        ('POST', '/v1/devices/push-token', {'json': {'platform': 'android', 'token': 'test-token-12345'}}),
        ('GET', '/v1/subscription/status', {}),
        ('POST', '/v1/auth/login', {'json': {'email': 'deleting@example.com', 'password': 'Password123'}}),
        ('POST', '/v1/auth/refresh', {'json': {'refresh_token': tokens['refresh_token']}}),
    ]
    for method, url, kwargs in requests:
        response = client.request(method, url, headers=headers, **kwargs)
        assert response.status_code == 409, (url, response.text)
        assert response.json()['detail']['error'] == 'account_deletion_pending'
        assert response.headers['cache-control'] == 'no-store'
    for model in (Profile, Location, PushToken):
        assert db_session.scalar(select(func.count()).select_from(model)) == 0
    assert list(tmp_path.iterdir()) == []
    assert db_session.scalar(select(func.count()).select_from(RefreshToken)) == refresh_count
    response = client.patch('/v1/profile', headers={'Authorization': 'Bearer ' + other['access_token']},
                            json={'theme': 'dark'})
    assert response.status_code == 200


def test_firebase_pending_deletion_cannot_recreate_sql_identity(client, db_session, monkeypatch):
    monkeypatch.setattr(get_settings(), 'firebase_project_id', 'test-project')
    monkeypatch.setattr(firebase_identity, 'verify_firebase_token',
                        lambda *_: {'uid': 'firebase-user', 'email': 'firebase@example.com'})
    uid = str(uuid5(NAMESPACE_URL, 'https://securetoken.google.com/test-project/firebase-user'))
    db_session.add(AccountDeletion(key_hash='e' * 64, backend_user_id=uid, status='pending'))
    db_session.commit()
    response = client.get('/v1/profile', headers={'Authorization': 'Bearer firebase-test-token'})
    assert response.status_code == 409
    assert db_session.get(User, uid) is None
