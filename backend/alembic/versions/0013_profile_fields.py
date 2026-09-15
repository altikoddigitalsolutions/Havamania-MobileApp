"""Add profile fields previously present only in ORM metadata."""
from alembic import op
import sqlalchemy as sa

revision = "0013_profile_fields"
down_revision = "0012_push_token_uniqueness"
branch_labels = None
depends_on = None


def upgrade():
    existing = {column["name"] for column in sa.inspect(op.get_bind()).get_columns("profiles")}
    columns = [
        sa.Column("avatar_url", sa.String(255), nullable=True),
        sa.Column("interest", sa.Text(), nullable=True),
        sa.Column("health_sensitivities", sa.Text(), nullable=True),
        sa.Column("travel_preferences", sa.Text(), nullable=True),
        sa.Column("activity_types", sa.Text(), nullable=True),
        sa.Column("assistant_tone", sa.String(32), nullable=False, server_default="friendly"),
    ]
    for column in columns:
        if column.name not in existing:
            op.add_column("profiles", column)


def downgrade():
    # Retain optional profile data for old application versions; no destructive rollback.
    pass
