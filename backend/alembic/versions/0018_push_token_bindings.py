"""Monotonic device binding revisions, tombstones and data-message capability."""

import sqlalchemy as sa
from alembic import op

revision = "0018_push_token_bindings"
down_revision = "0017_push_deliveries"
branch_labels = None
depends_on = None


def upgrade():
    op.add_column(
        "push_tokens",
        sa.Column("owner_protocol", sa.Integer(), server_default=sa.text("0"), nullable=False),
    )
    op.create_table(
        "push_token_bindings",
        sa.Column("token_hash", sa.String(64), primary_key=True),
        sa.Column("installation_id", sa.String(36), nullable=False),
        sa.Column("revision", sa.BigInteger(), nullable=False),
        sa.Column(
            "user_id", sa.String(36), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False
        ),
        sa.Column("bound", sa.Boolean(), nullable=False),
    )
    op.create_index("ix_push_token_bindings_user_id", "push_token_bindings", ["user_id"])


def downgrade():
    op.drop_table("push_token_bindings")
    op.drop_column("push_tokens", "owner_protocol")
