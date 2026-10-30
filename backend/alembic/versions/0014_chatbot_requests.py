"""Durable per-user assistant reservation identities; no message content."""
from alembic import op
import sqlalchemy as sa

revision = "0014_chatbot_requests"
down_revision = "0013_profile_fields"
branch_labels = None
depends_on = None


def upgrade():
    op.create_table(
        "chatbot_requests",
        sa.Column("id", sa.String(36), primary_key=True),
        sa.Column("user_id", sa.String(36), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("request_id", sa.String(36), nullable=False),
        sa.Column("usage_id", sa.String(36), sa.ForeignKey("chatbot_usage_daily.id", ondelete="CASCADE"), nullable=False),
        sa.Column("status", sa.String(16), nullable=False),
        sa.Column("expires_at", sa.BigInteger(), nullable=False),
        sa.UniqueConstraint("user_id", "request_id", name="uq_chatbot_request_owner"),
    )
    op.create_index("ix_chatbot_requests_expires_at", "chatbot_requests", ["expires_at"])


def downgrade():
    op.drop_table("chatbot_requests")
