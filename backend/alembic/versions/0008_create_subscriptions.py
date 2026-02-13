"""create subscriptions table

Revision ID: 0008_create_subscriptions
Revises: 0007_create_chatbot_usage_daily
Create Date: 2026-02-13 11:37:00
"""

from alembic import op
import sqlalchemy as sa

# revision identifiers, used by Alembic.
revision = "0008_create_subscriptions"
down_revision = "0007_create_chatbot_usage_daily"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.create_table(
        "subscriptions",
        sa.Column("id", sa.String(length=36), primary_key=True),
        sa.Column("user_id", sa.String(length=36), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("plan_code", sa.String(length=64), nullable=False, server_default="free"),
        sa.Column("status", sa.String(length=32), nullable=False, server_default="expired"),
        sa.Column("expires_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("store", sa.String(length=32), nullable=True),
        sa.Column("original_transaction_id", sa.String(length=255), nullable=True),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.func.now()),
    )
    op.create_index("ix_subscriptions_user_id", "subscriptions", ["user_id"], unique=True)


def downgrade() -> None:
    op.drop_index("ix_subscriptions_user_id", table_name="subscriptions")
    op.drop_table("subscriptions")
