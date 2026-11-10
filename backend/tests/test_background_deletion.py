from datetime import UTC, datetime, timedelta
from unittest.mock import Mock

import pytest
from app.models.account_deletion import AccountDeletion
from app.models.alert import Alert, AlertSeverity
from app.models.location import Location
from app.models.notification_preference import NotificationPreference
from app.models.push_token import PushToken
from app.models.user import User
from app.services import alerts_ingest
from app.services.push_service import PushService
from sqlalchemy import func, select


@pytest.fixture
def queued_alerts(db_session):
    now = datetime.now(UTC)
    for uid in ('a', 'b'):
        db_session.add(User(id=uid, email=f'{uid}@example.invalid', password_hash='unused'))
    db_session.commit()
    for uid in ('a', 'b'):
        db_session.add(Location(id='loc-' + uid, user_id=uid, label='City', lat=41, lon=29))
        db_session.add(PushToken(owner_protocol=1, user_id=uid, platform='android', token='token-' + uid))
    db_session.commit()
    for uid in ('a', 'b'):
        db_session.add(Alert(id='alert-' + uid, location_id='loc-' + uid, severity=AlertSeverity.CRITICAL,
                            title='Fırtına', description='Test only', starts_at=now - timedelta(minutes=1),
                            ends_at=now + timedelta(hours=1)))
    db_session.commit()
    return db_session.get(Alert, 'alert-a'), db_session.get(Alert, 'alert-b')


def test_pending_deletion_skips_queued_push_and_ingestion_without_affecting_b(db_session, queued_alerts, monkeypatch):
    a, b = queued_alerts
    db_session.add(AccountDeletion(key_hash='c' * 64, backend_user_id='a', status='pending'))
    db_session.commit()
    provider = Mock()
    fetch = Mock()
    monkeypatch.setattr(alerts_ingest.ExternalAlertProvider, 'fetch_alerts', fetch)
    service = PushService(provider)
    assert service.send_critical_alert(db_session, a) == 0
    assert alerts_ingest.sync_alerts_for_location(db_session, 'loc-a', 0, 0) == 0
    provider.send.assert_not_called()
    fetch.assert_not_called()
    assert service.send_critical_alert(db_session, b) == 1
    assert provider.send.call_args.kwargs['token'] == 'token-b'
    assert provider.send.call_args.kwargs['title'] == 'Kritik Hava Uyarısı: Fırtına'


@pytest.mark.parametrize('case', ['future', 'expired', 'advisory', 'missing', 'disabled'])
def test_stale_or_disabled_alert_cannot_be_delivered(db_session, queued_alerts, case):
    a, _ = queued_alerts
    if case == 'future':
        a.starts_at = datetime.now(UTC) + timedelta(minutes=5)
    elif case == 'expired':
        a.ends_at = datetime.now(UTC) - timedelta(seconds=1)
    elif case == 'advisory':
        a.severity = AlertSeverity.ADVISORY
    elif case == 'missing':
        db_session.delete(a)
    else:
        db_session.add(NotificationPreference(user_id='a', severe_alert_enabled=False))
    db_session.commit()
    provider = Mock()
    assert PushService(provider).send_critical_alert(db_session, a) == 0
    provider.send.assert_not_called()


def test_alert_ingestion_uses_stored_coordinates_and_does_not_fabricate_provider_data(db_session, queued_alerts, monkeypatch):
    with pytest.raises(NotImplementedError):
        alerts_ingest.sync_alerts_for_location(db_session, 'loc-a', 0, 0)
    assert db_session.scalar(select(func.count()).select_from(Alert)) == 2
    now = datetime.now(UTC)
    fetch = Mock(return_value=[{'severity': AlertSeverity.CRITICAL, 'title': 'Test only', 'description': 'Test only',
                                    'starts_at': now, 'ends_at': now + timedelta(hours=1)}])
    monkeypatch.setattr(alerts_ingest.ExternalAlertProvider, 'fetch_alerts', fetch)
    assert alerts_ingest.sync_alerts_for_location(db_session, 'loc-a', 0, 0) == 1
    fetch.assert_called_once_with(lat=41, lon=29)
    fetch.reset_mock()
    assert alerts_ingest.sync_alerts_for_location(db_session, 'missing-location', 0, 0) == 0
    fetch.assert_not_called()


def test_delivery_receipt_survives_session_restart_and_repeated_queue_runs(db_session, queued_alerts):
    from app.models.push_delivery import PushDelivery
    from sqlalchemy.orm import Session
    a, _ = queued_alerts
    provider = Mock(return_value=True)
    provider.send.return_value = True
    service = PushService(provider)
    assert service.send_critical_alert(db_session, a) == 1
    with Session(db_session.get_bind()) as restarted:
        assert service.send_critical_alert(restarted, a) == 0
        receipt = restarted.scalar(select(PushDelivery))
        assert receipt.status == 'sent'
        assert len(receipt.token_hash) == 64 and receipt.token_hash != 'token-a'
    provider.send.assert_called_once()


@pytest.mark.parametrize('outcome', ['false', 'exception', 'crash'])
def test_ambiguous_or_crashed_delivery_is_never_automatically_replayed(db_session, queued_alerts, outcome):
    from app.models.push_delivery import PushDelivery
    a, _ = queued_alerts
    class ProcessStopped(BaseException):
        pass
    provider = Mock()
    if outcome == 'false':
        provider.send.return_value = False
    else:
        provider.send.side_effect = RuntimeError('local unknown outcome') if outcome == 'exception' else ProcessStopped()
    service = PushService(provider)
    if outcome == 'crash':
        with pytest.raises(ProcessStopped):
            service.send_critical_alert(db_session, a)
    else:
        assert service.send_critical_alert(db_session, a) == 0
    provider.send.side_effect = None
    provider.send.return_value = True
    assert service.send_critical_alert(db_session, a) == 0
    provider.send.assert_called_once()
    receipt = db_session.scalar(select(PushDelivery))
    assert receipt.status == ('reserved' if outcome == 'crash' else 'unknown')


def test_each_device_has_its_own_receipt_and_account_delete_cascades(db_session, queued_alerts):
    from app.models.push_delivery import PushDelivery
    a, _ = queued_alerts
    db_session.add(PushToken(owner_protocol=1, user_id='a', platform='android', token='second-token-a'))
    db_session.commit()
    provider = Mock()
    provider.send.return_value = True
    service = PushService(provider)
    assert service.send_critical_alert(db_session, a) == 2
    assert service.send_critical_alert(db_session, a) == 0
    assert db_session.scalar(select(func.count()).select_from(PushDelivery)) == 2
    db_session.delete(db_session.get(User, 'a'))
    db_session.commit()
    assert db_session.scalar(select(func.count()).select_from(PushDelivery)) == 0
    assert db_session.get(User, 'b') is not None


def test_dispatch_does_not_commit_callers_work_and_receipt_survives_caller_rollback(db_session, queued_alerts):
    from app.models.profile import Profile
    from app.models.push_delivery import PushDelivery
    from sqlalchemy.orm import Session
    a, _ = queued_alerts
    pending = Profile(user_id='a')
    db_session.add(pending)
    provider = Mock()
    provider.send.return_value = True
    service = PushService(provider)
    assert service.send_critical_alert(db_session, a) == 1
    assert pending in db_session.new
    with Session(db_session.get_bind()) as fresh:
        assert fresh.get(Profile, 'a') is None
        assert fresh.scalar(select(PushDelivery.status)) == 'sent'
    db_session.rollback()
    assert service.send_critical_alert(db_session, a) == 0
    provider.send.assert_called_once()


def test_reregistering_same_token_does_not_erase_dispatch_history(db_session, queued_alerts):
    a, _ = queued_alerts
    provider = Mock()
    provider.send.return_value = True
    service = PushService(provider)
    assert service.send_critical_alert(db_session, a) == 1
    old = db_session.scalar(select(PushToken).where(PushToken.user_id == 'a'))
    db_session.delete(old)
    db_session.commit()
    db_session.add(PushToken(owner_protocol=1, user_id='a', platform='android', token='token-a'))
    db_session.commit()
    assert service.send_critical_alert(db_session, a) == 0
    provider.send.assert_called_once()


def test_deletion_accepted_after_claim_prevents_external_dispatch(db_session, queued_alerts, monkeypatch):
    from app.models.push_delivery import PushDelivery
    from app.services import push_service as module
    a, _ = queued_alerts
    original = module.require_active_account
    checks = []
    def accept_before_final_check(db, uid):
        checks.append(uid)
        if len(checks) == 3:
            db.add(AccountDeletion(key_hash='f' * 64, backend_user_id=uid, status='pending'))
            db.commit()
        return original(db, uid)
    monkeypatch.setattr(module, 'require_active_account', accept_before_final_check)
    provider = Mock()
    assert PushService(provider).send_critical_alert(db_session, a) == 0
    provider.send.assert_not_called()
    assert db_session.scalar(select(PushDelivery.status)) == 'cancelled'


def test_connection_bound_caller_cannot_roll_back_durable_dispatch_claim(db_session, queued_alerts):
    from app.models.push_delivery import PushDelivery
    from sqlalchemy.orm import Session
    a, _ = queued_alerts
    engine = db_session.get_bind()
    provider = Mock()
    provider.send.return_value = True
    service = PushService(provider)
    with engine.connect() as connection:
        transaction = connection.begin()
        with Session(connection) as caller:
            assert service.send_critical_alert(caller, a) == 1
        transaction.rollback()
    assert db_session.scalar(select(PushDelivery.status)) == 'sent'
    assert service.send_critical_alert(db_session, a) == 0
    provider.send.assert_called_once()


def test_disabled_owner_protocol_does_not_consume_dispatch_claim(db_session, queued_alerts):
    from app.models.push_delivery import PushDelivery
    a, _ = queued_alerts
    provider = Mock()
    provider.owner_protocol_enabled = False
    assert PushService(provider).send_critical_alert(db_session, a) == 0
    provider.send.assert_not_called()
    assert db_session.scalar(select(func.count()).select_from(PushDelivery)) == 0


def test_legacy_device_capability_never_receives_owner_bound_data(db_session, queued_alerts):
    from app.models.push_delivery import PushDelivery
    a, _ = queued_alerts
    token = db_session.scalar(select(PushToken).where(PushToken.user_id == 'a'))
    token.owner_protocol = 0
    db_session.commit()
    provider = Mock()
    assert PushService(provider).send_critical_alert(db_session, a) == 0
    provider.send.assert_not_called()
    assert db_session.scalar(select(func.count()).select_from(PushDelivery)) == 0
