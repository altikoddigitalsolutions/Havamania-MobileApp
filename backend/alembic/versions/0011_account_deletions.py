"""Durable account-deletion continuation records, independent of user lifetime."""
from alembic import op
import sqlalchemy as sa

revision = "0011_account_deletions"
down_revision = "0010"
branch_labels = None
depends_on = None


def upgrade():
    op.create_table("account_deletions",
        sa.Column("key_hash", sa.String(64), primary_key=True),
        sa.Column("firebase_uid", sa.String(128), nullable=True),
        sa.Column("project_id", sa.String(255), nullable=True),
        sa.Column("storage_bucket", sa.String(255), nullable=True),
        sa.Column("status", sa.String(16), nullable=False))


def downgrade():
    op.drop_table("account_deletions")
