"""Bounded platform request snapshots and local successful response replay."""

import sqlalchemy as sa
from alembic import op

revision = "0019_chatbot_replay"
down_revision = "0018_push_token_bindings"
branch_labels = None
depends_on = None


def upgrade():
    op.add_column("chatbot_requests", sa.Column("input_hash", sa.String(64), nullable=True))
    op.add_column("chatbot_requests", sa.Column("platform_payload", sa.JSON(), nullable=True))
    op.add_column("chatbot_requests", sa.Column("cached_reply", sa.JSON(), nullable=True))
    op.add_column("chatbot_requests", sa.Column("replay_until", sa.BigInteger(), nullable=True))
    op.create_index("ix_chatbot_requests_replay_until", "chatbot_requests", ["replay_until"])


def downgrade():
    op.drop_index("ix_chatbot_requests_replay_until", table_name="chatbot_requests")
    for column in ("replay_until", "cached_reply", "platform_payload", "input_hash"):
        op.drop_column("chatbot_requests", column)
