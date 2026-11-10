"""Pin new platform deletion targets; legacy jobs require operator target verification."""
from uuid import NAMESPACE_URL, uuid5

import sqlalchemy as sa
from alembic import op

revision = "0016_deletion_target"
down_revision = "0015_assistant_consent"
branch_labels = None
depends_on = None


def upgrade():
    op.add_column("account_deletions", sa.Column("backend_user_id", sa.String(36), nullable=True))
    op.add_column("account_deletions", sa.Column("platform_base_url", sa.String(2048), nullable=True))
    op.create_index("ix_account_deletions_user_status", "account_deletions", ["backend_user_id", "status"])
    connection = op.get_bind()
    rows = connection.execute(sa.text(
        "SELECT key_hash, project_id, firebase_uid FROM account_deletions WHERE status = 'pending'"
    )).mappings().all()
    for row in rows:
        if row["project_id"] and row["firebase_uid"]:
            user_id = str(uuid5(NAMESPACE_URL,
                f"https://securetoken.google.com/{row['project_id']}/{row['firebase_uid']}"))
            connection.execute(sa.text(
                "UPDATE account_deletions SET backend_user_id = :uid WHERE key_hash = :key"
            ), {"uid": user_id, "key": row["key_hash"]})
    # Never guess a historical platform URL from the deployment's current environment.


def downgrade():
    op.drop_index("ix_account_deletions_user_status", table_name="account_deletions")
    op.drop_column("account_deletions", "platform_base_url")
    op.drop_column("account_deletions", "backend_user_id")
