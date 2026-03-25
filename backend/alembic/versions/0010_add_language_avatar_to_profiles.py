"""add language and avatar_emoji to profiles

Revision ID: 0010
Revises: 0009
Create Date: 2026-03-25
"""
from alembic import op
import sqlalchemy as sa

revision = '0010'
down_revision = '0009_create_push_tokens'
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.add_column(
        'profiles',
        sa.Column('language', sa.String(8), nullable=False, server_default='tr'),
    )
    op.add_column(
        'profiles',
        sa.Column('avatar_emoji', sa.String(16), nullable=False, server_default='🧑'),
    )


def downgrade() -> None:
    op.drop_column('profiles', 'avatar_emoji')
    op.drop_column('profiles', 'language')
