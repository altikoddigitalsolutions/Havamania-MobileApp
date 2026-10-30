"""Durable, conservative push dispatch claims without storing device tokens."""

import sqlalchemy as sa
from alembic import op

revision = "0017_push_deliveries"
down_revision = "0016_deletion_target"
branch_labels = None
depends_on = None


def upgrade():
    op.create_table(
        "push_deliveries",
        sa.Column("id", sa.String(36), primary_key=True),
        sa.Column(
            "alert_id",
            sa.String(36),
            sa.ForeignKey("alerts.id", ondelete="CASCADE"),
            nullable=False,
        ),
        sa.Column(
            "user_id", sa.String(36), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False
        ),
        sa.Column("token_hash", sa.String(64), nullable=False),
        sa.Column("status", sa.String(16), nullable=False),
        sa.Column(
            "created_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False
        ),
        sa.UniqueConstraint("alert_id", "user_id", "token_hash", name="uq_push_delivery_target"),
    )
    op.create_index("ix_push_deliveries_alert_id", "push_deliveries", ["alert_id"])
    op.create_index("ix_push_deliveries_user_id", "push_deliveries", ["user_id"])


def downgrade():
    op.drop_table("push_deliveries")
