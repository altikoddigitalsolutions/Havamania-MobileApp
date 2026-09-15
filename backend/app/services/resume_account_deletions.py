"""Run periodically on the server: python -m app.services.resume_account_deletions.

Processes previously authorized jobs only; cannot initiate account deletion.
Exit status 1 means at least one job still needs a retry/operator investigation.
"""
from sqlalchemy import select

from app.db.session import SessionLocal
from app.models.account_deletion import AccountDeletion
from app.services.account_deletion import execute_deletion


def main():
    failed = False
    with SessionLocal() as db:
        keys = db.scalars(select(AccountDeletion.key_hash)
                          .where(AccountDeletion.status == "pending")).all()
        db.rollback()
        for key in keys:
            if not execute_deletion(db, key):
                failed = True
    return 1 if failed else 0


if __name__ == "__main__":
    raise SystemExit(main())
