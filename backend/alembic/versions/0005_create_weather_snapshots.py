"""create weather snapshots table

Revision ID: 0005_create_weather_snapshots
Revises: 0004_create_profile_related_tables
Create Date: 2026-02-13 11:01:00
"""

from alembic import op
import sqlalchemy as sa

# revision identifiers, used by Alembic.
revision = "0005_create_weather_snapshots"
down_revision = "0004_create_profiles"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.create_table(
        "weather_snapshots",
        sa.Column("id", sa.String(length=36), primary_key=True),
        sa.Column("location_key", sa.String(length=128), nullable=False),
        sa.Column("provider", sa.String(length=64), nullable=False),
        sa.Column("payload_jsonb", sa.JSON(), nullable=False),
        sa.Column("fetched_at", sa.DateTime(timezone=True), server_default=sa.func.now()),
    )
    op.create_index("ix_weather_snapshots_location_key", "weather_snapshots", ["location_key"], unique=False)


def downgrade() -> None:
    op.drop_index("ix_weather_snapshots_location_key", table_name="weather_snapshots")
    op.drop_table("weather_snapshots")
