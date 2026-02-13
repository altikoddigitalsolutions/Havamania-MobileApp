"""create chatbot usage daily table

Revision ID: 0007_create_chatbot_usage_daily
Revises: 0006_create_alerts
Create Date: 2026-02-13 11:24:00
"""

from alembic import op
import sqlalchemy as sa

# revision identifiers, used by Alembic.
revision = "0007_create_chatbot_usage_daily"
down_revision = "0006_create_alerts"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.create_table(
        "chatbot_usage_daily",
        sa.Column("id", sa.String(length=36), primary_key=True),
        sa.Column("user_id", sa.String(length=36), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("date", sa.Date(), nullable=False),
        sa.Column("message_count", sa.Integer(), nullable=False, server_default="0"),
        sa.Column("token_count", sa.Integer(), nullable=False, server_default="0"),
        sa.UniqueConstraint("user_id", "date", name="uq_chatbot_usage_user_date"),
    )
    op.create_index("ix_chatbot_usage_daily_user_id", "chatbot_usage_daily", ["user_id"], unique=False)


def downgrade() -> None:
    op.drop_index("ix_chatbot_usage_daily_user_id", table_name="chatbot_usage_daily")
    op.drop_table("chatbot_usage_daily")
