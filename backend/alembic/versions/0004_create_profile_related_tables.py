"""create profile related tables

Revision ID: 0004_create_profile_related_tables
Revises: 0003_create_refresh_tokens
Create Date: 2026-02-13 10:47:00
"""

from alembic import op
import sqlalchemy as sa

# revision identifiers, used by Alembic.
revision = "0004_create_profiles"
down_revision = "0003_create_refresh_tokens"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.create_table(
        "locations",
        sa.Column("id", sa.String(length=36), primary_key=True),
        sa.Column("user_id", sa.String(length=36), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("label", sa.String(length=255), nullable=False),
        sa.Column("lat", sa.Float(), nullable=False),
        sa.Column("lon", sa.Float(), nullable=False),
        sa.Column("is_primary", sa.Boolean(), nullable=False, server_default=sa.text("false")),
        sa.Column("is_tracking_enabled", sa.Boolean(), nullable=False, server_default=sa.text("true")),
    )
    op.create_index("ix_locations_user_id", "locations", ["user_id"], unique=False)

    op.create_table(
        "profiles",
        sa.Column("user_id", sa.String(length=36), sa.ForeignKey("users.id", ondelete="CASCADE"), primary_key=True),
        sa.Column("primary_location_id", sa.String(length=36), sa.ForeignKey("locations.id", ondelete="SET NULL"), nullable=True),
        sa.Column("temperature_unit", sa.String(length=8), nullable=False, server_default="C"),
        sa.Column("wind_unit", sa.String(length=16), nullable=False, server_default="kmh"),
        sa.Column("theme", sa.String(length=16), nullable=False, server_default="system"),
    )

    op.create_table(
        "notification_preferences",
        sa.Column("user_id", sa.String(length=36), sa.ForeignKey("users.id", ondelete="CASCADE"), primary_key=True),
        sa.Column("severe_alert_enabled", sa.Boolean(), nullable=False, server_default=sa.text("true")),
        sa.Column("daily_summary_enabled", sa.Boolean(), nullable=False, server_default=sa.text("true")),
        sa.Column("rain_alert_enabled", sa.Boolean(), nullable=False, server_default=sa.text("false")),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.func.now()),
    )


def downgrade() -> None:
    op.drop_table("notification_preferences")
    op.drop_table("profiles")
    op.drop_index("ix_locations_user_id", table_name="locations")
    op.drop_table("locations")
