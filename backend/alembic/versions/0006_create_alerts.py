"""create alerts table

Revision ID: 0006_create_alerts
Revises: 0005_create_weather_snapshots
Create Date: 2026-02-13 11:14:00
"""

from alembic import op
import sqlalchemy as sa

# revision identifiers, used by Alembic.
revision = "0006_create_alerts"
down_revision = "0005_create_weather_snapshots"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.create_table(
        "alerts",
        sa.Column("id", sa.String(length=36), primary_key=True),
        sa.Column("location_id", sa.String(length=36), sa.ForeignKey("locations.id", ondelete="CASCADE"), nullable=False),
        sa.Column("severity", sa.Enum("ADVISORY", "ACTIVE", "CRITICAL", name="alertseverity"), nullable=False),
        sa.Column("title", sa.String(length=255), nullable=False),
        sa.Column("description", sa.String(length=1000), nullable=False),
        sa.Column("starts_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("ends_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now()),
    )
    op.create_index("ix_alerts_location_id", "alerts", ["location_id"], unique=False)


def downgrade() -> None:
    op.drop_index("ix_alerts_location_id", table_name="alerts")
    op.drop_table("alerts")
    severity_enum = sa.Enum("ADVISORY", "ACTIVE", "CRITICAL", name="alertseverity")
    severity_enum.drop(op.get_bind(), checkfirst=True)
