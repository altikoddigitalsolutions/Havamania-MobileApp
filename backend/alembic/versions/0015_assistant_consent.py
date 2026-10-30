"""Server-owned consent state and audit history; no health/message content."""
from alembic import op
import sqlalchemy as sa

revision = "0015_assistant_consent"
down_revision = "0014_chatbot_requests"
branch_labels = None
depends_on = None


def fields():
    return [sa.Column("assistant_accepted", sa.Boolean(), nullable=False),
            sa.Column("assistant_version", sa.Integer(), nullable=True),
            sa.Column("assistant_hash", sa.String(64), nullable=True),
            sa.Column("health_accepted", sa.Boolean(), nullable=False),
            sa.Column("health_version", sa.Integer(), nullable=True),
            sa.Column("health_hash", sa.String(64), nullable=True),
            sa.Column("recorded_at", sa.DateTime(timezone=True), nullable=False)]


def upgrade():
    op.create_table("assistant_consents", sa.Column("user_id", sa.String(36),
        sa.ForeignKey("users.id", ondelete="CASCADE"), primary_key=True), *fields())
    op.create_table("assistant_consent_events", sa.Column("id", sa.String(36), primary_key=True),
        sa.Column("user_id", sa.String(36), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False), *fields())
    op.create_index("ix_assistant_consent_events_user_id", "assistant_consent_events", ["user_id"])


def downgrade():
    op.drop_table("assistant_consent_events")
    op.drop_table("assistant_consents")
