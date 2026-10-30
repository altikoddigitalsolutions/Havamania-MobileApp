from app.models.push_token import PushToken
from sqlalchemy import select


def signup(client, email):
    response = client.post('/v1/auth/signup', json={'email': email, 'password': 'Password123'})
    assert response.status_code == 201
    return {'Authorization': 'Bearer ' + response.json()['access_token']}


def test_delayed_old_owner_unregister_cannot_remove_transferred_token(client, db_session):
    a, b = signup(client, 'token-a@example.com'), signup(client, 'token-b@example.com')
    payload = {'platform': 'android', 'token': 'shared-installation-token'}
    first = client.post('/v1/devices/push-token', headers=a, json=payload)
    second = client.post('/v1/devices/push-token', headers=b, json=payload)
    assert first.status_code == second.status_code == 201
    assert first.json()['id'] == second.json()['id']
    assert first.json()['user_id'] != second.json()['user_id']
    response = client.request('DELETE', '/v1/devices/push-token', headers=a, json=payload)
    assert response.status_code == 204
    token = db_session.scalar(select(PushToken).execution_options(populate_existing=True))
    assert token.user_id == second.json()['user_id']
    assert client.request('DELETE', '/v1/devices/push-token', headers=b, json=payload).status_code == 204
    assert db_session.scalar(select(PushToken)) is None


def test_repeated_registration_is_stable_and_owner_is_never_taken_from_body(client, db_session):
    headers = signup(client, 'token-stable@example.com')
    payload = {'platform': 'android', 'token': 'stable-installation-token', 'user_id': 'someone-else'}
    first = client.post('/v1/devices/push-token', headers=headers, json=payload)
    second = client.post('/v1/devices/push-token', headers=headers, json=payload)
    assert first.status_code == second.status_code == 201
    assert first.json() == second.json()
    assert second.json()['user_id'] != 'someone-else'
    assert len(db_session.scalars(select(PushToken)).all()) == 1


def test_versioned_binding_blocks_late_register_downgrade_and_logout_replay(client, db_session):
    from app.models.push_token_binding import PushTokenBinding
    from app.models.user import User
    from sqlalchemy import func
    a, b = signup(client, 'revision-a@example.com'), signup(client, 'revision-b@example.com')
    payload = {'platform': 'android', 'token': 'revision-installation-token', 'owner_protocol': 1,
                   'installation_id': '032985f1-b932-48d9-8540-be4ffb65f421', 'revision': 10}
    first = client.post('/v1/devices/push-token', headers=a, json=payload)
    assert first.status_code == 201
    assert first.json()['revision'] == 10 and first.json()['owner_protocol'] == 1
    assert client.post('/v1/devices/push-token', headers=a, json=payload).json() == first.json()
    newer = dict(payload, revision=11)
    second = client.post('/v1/devices/push-token', headers=b, json=newer)
    assert second.status_code == 201
    for headers, request in [(a, payload), (a, newer), (a, dict(payload, revision=9)),
                             (b, dict(newer, installation_id='e1a53d37-2f73-43d7-ac62-1df31266ffca', revision=99)),
                             (a, {'platform': 'android', 'token': payload['token']})]:
        response = client.post('/v1/devices/push-token', headers=headers, json=request)
        assert response.status_code == 409
    assert client.request('DELETE', '/v1/devices/push-token', headers=a, json=dict(payload, revision=12)).status_code == 409
    assert client.request('DELETE', '/v1/devices/push-token', headers=b,
                          json={'platform': 'android', 'token': payload['token']}).status_code == 409
    unbind = dict(payload, revision=12)
    assert client.request('DELETE', '/v1/devices/push-token', headers=b, json=unbind).status_code == 204
    assert client.request('DELETE', '/v1/devices/push-token', headers=b, json=unbind).status_code == 204
    assert db_session.scalar(select(PushToken)) is None
    assert client.post('/v1/devices/push-token', headers=b, json=newer).status_code == 409
    assert client.post('/v1/devices/push-token', headers=b, json=dict(payload, revision=13)).status_code == 201
    token = db_session.scalar(select(PushToken).execution_options(populate_existing=True))
    assert token.user_id == second.json()['user_id'] and token.owner_protocol == 1
    assert db_session.scalar(select(func.count()).select_from(PushTokenBinding)) == 1
    db_session.delete(db_session.get(User, token.user_id))
    db_session.commit()
    assert db_session.scalar(select(PushTokenBinding)) is None
    assert db_session.scalar(select(PushToken)) is None


def test_binding_metadata_is_all_or_none_and_strict_integer(client):
    headers = signup(client, 'metadata@example.com')
    base = {'platform': 'android', 'token': 'metadata-installation-token'}
    full = dict(base, owner_protocol=1, revision=1, installation_id='032985f1-b932-48d9-8540-be4ffb65f421')
    cases = [dict(base, owner_protocol=1), dict(base, revision=1), dict(full, revision=True),
             dict(full, revision=1.0), dict(full, revision=0), dict(full, revision=9007199254740992),
             dict(full, owner_protocol=True), dict(full, owner_protocol=2), dict(full, platform='ios')]
    for payload in cases:
        assert client.post('/v1/devices/push-token', headers=headers, json=payload).status_code == 422


def test_legacy_token_can_upgrade_but_never_downgrade(client):
    headers = signup(client, 'upgrade@example.com')
    legacy = {'platform': 'android', 'token': 'upgrade-installation-token'}
    first = client.post('/v1/devices/push-token', headers=headers, json=legacy)
    assert first.status_code == 201 and first.json()['owner_protocol'] == 0
    newer = dict(legacy, owner_protocol=1, revision=1, installation_id='032985f1-b932-48d9-8540-be4ffb65f421')
    response = client.post('/v1/devices/push-token', headers=headers, json=newer)
    assert response.status_code == 201 and response.json()['id'] == first.json()['id']
    assert client.post('/v1/devices/push-token', headers=headers, json=legacy).status_code == 409


def test_unbind_before_initial_registration_prevents_late_creation(client, db_session):
    headers = signup(client, 'offline-logout@example.com')
    request = {'platform': 'android', 'token': 'offline-before-register-token', 'owner_protocol': 1,
               'installation_id': '032985f1-b932-48d9-8540-be4ffb65f421', 'revision': 2}
    assert client.request('DELETE', '/v1/devices/push-token', headers=headers, json=request).status_code == 204
    assert client.post('/v1/devices/push-token', headers=headers, json=dict(request, revision=1)).status_code == 409
    assert client.post('/v1/devices/push-token', headers=headers,
                       json={'platform': 'android', 'token': request['token']}).status_code == 409
    assert db_session.scalar(select(PushToken)) is None
