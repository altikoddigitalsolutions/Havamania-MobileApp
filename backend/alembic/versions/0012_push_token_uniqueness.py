"""Make device tokens unique without per-request DDL or data loss."""
from alembic import op

from app.db.push_token_migration import migrate_push_tokens

revision = "0012_push_token_uniqueness"
down_revision = "0011_account_deletions"
branch_labels = None
depends_on = None


def upgrade():
    migrate_push_tokens(op.get_bind())


def downgrade():
    # Restoring user/platform uniqueness could destroy valid multi-device data.
    # Earlier app code already supports token uniqueness; retain the constraint.
    pass
