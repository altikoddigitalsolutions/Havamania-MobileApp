from concurrent.futures import ThreadPoolExecutor
from uuid import uuid4

from app.db.base import Base
from app.models.user import User
from app.services.chatbot_usage import ChatbotUsageService
from sqlalchemy import create_engine
from sqlalchemy.orm import Session


def test_parallel_first_requests_cannot_exceed_quota(tmp_path):
    engine = create_engine(f"sqlite:///{tmp_path / 'quota.sqlite'}")
    Base.metadata.create_all(engine)
    service = ChatbotUsageService()
    with Session(engine) as db:
        user = User(email="quota@example.com", password_hash="unused")
        db.add(user)
        db.commit()
        uid = user.id

    def reserve(_):
        with Session(engine) as db:
            return service.reserve_request(db, uid, 3, str(uuid4()))

    try:
        with ThreadPoolExecutor(max_workers=8) as pool:
            results = list(pool.map(reserve, range(16)))
        assert sum(result is not None for result in results) == 3
        with Session(engine) as db:
            assert service.get_or_create_today_usage(db, uid).message_count == 3
            service.finish_request(db, next(result for result in results if result), success=False)
            assert service.reserve_request(db, uid, 3, str(uuid4())) is not None
            assert service.reserve_request(db, uid, 3, str(uuid4())) is None
            assert service.get_or_create_today_usage(db, uid).message_count == 3
    finally:
        engine.dispose()


def test_abandoned_request_recovery_is_persistent_and_refunds_once(db_session):
    import time

    import pytest
    from app.models.chatbot_request import ChatbotRequest
    from app.services.chatbot_usage import DuplicateChatbotRequest

    service = ChatbotUsageService()
    user = User(email="recovery@example.com", password_hash="unused")
    db_session.add(user)
    db_session.commit()
    uid = user.id
    request_id = str(uuid4())
    record_id = service.reserve_request(db_session, uid, 1, request_id)
    row = db_session.get(ChatbotRequest, record_id)
    row.expires_at = int(time.time()) - 1
    db_session.commit()
    # New service/session simulates resuming after a process exit.
    with Session(db_session.bind) as restarted:
        other = ChatbotUsageService()
        other.recover_expired(restarted, uid)
        other.recover_expired(restarted, uid)
        assert other.get_or_create_today_usage(restarted, uid).message_count == 0
        assert not other.finish_request(restarted, record_id, success=True)
        with pytest.raises(DuplicateChatbotRequest):
            other.reserve_request(restarted, uid, 1, request_id)
        new = other.reserve_request(restarted, uid, 1, str(uuid4()))
        assert other.finish_request(restarted, new, success=False)
        assert not other.finish_request(restarted, new, success=False)
        assert other.get_or_create_today_usage(restarted, uid).message_count == 0


def test_same_request_id_is_isolated_per_user(db_session):
    service = ChatbotUsageService()
    users = [User(email=f"owner{i}@example.com", password_hash="unused") for i in range(2)]
    db_session.add_all(users)
    db_session.commit()
    first_uid, second_uid = [user.id for user in users]
    request_id = str(uuid4())
    first = service.reserve_request(db_session, first_uid, 1, request_id)
    second = service.reserve_request(db_session, second_uid, 1, request_id)
    assert first and second and first != second
    service.finish_request(db_session, first, success=False)
    assert service.get_or_create_today_usage(db_session, first_uid).message_count == 0
    assert service.get_or_create_today_usage(db_session, second_uid).message_count == 1
