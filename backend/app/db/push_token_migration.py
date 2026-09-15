"""Explicit schema migration helper; never called from request handling."""
from alembic.migration import MigrationContext
from alembic.operations import Operations
from sqlalchemy import inspect, text


def migrate_push_tokens(connection):
    inspector = inspect(connection)
    if "push_tokens" not in inspector.get_table_names():
        raise RuntimeError("push_tokens table missing; apply earlier migrations first")
    duplicates = connection.execute(text(
        "SELECT token FROM push_tokens GROUP BY token HAVING COUNT(*) > 1 LIMIT 1"
    )).first()
    if duplicates:
        raise RuntimeError("Duplicate push tokens detected; resolve conflicts before migration")

    operations = Operations(MigrationContext.configure(connection))
    constraints = inspector.get_unique_constraints("push_tokens")
    indexes = inspector.get_indexes("push_tokens")
    old_constraints = [c for c in constraints if set(c["column_names"]) == {"user_id", "platform"}]
    if old_constraints:
        convention = {"uq": "uq_%(table_name)s_%(column_0_name)s_%(column_1_name)s"}
        with operations.batch_alter_table("push_tokens", naming_convention=convention) as batch:
            for constraint in old_constraints:
                name = constraint["name"] or "uq_push_tokens_" + "_".join(constraint["column_names"])
                batch.drop_constraint(name, type_="unique")
    for index in indexes:
        if index.get("unique") and set(index["column_names"]) == {"user_id", "platform"}:
            operations.drop_index(index["name"], table_name="push_tokens")

    inspector = inspect(connection)
    unique_sets = [set(c["column_names"]) for c in inspector.get_unique_constraints("push_tokens")]
    unique_sets += [set(i["column_names"]) for i in inspector.get_indexes("push_tokens") if i.get("unique")]
    if {"token"} not in unique_sets:
        operations.create_index("ix_push_tokens_token", "push_tokens", ["token"], unique=True)

    inspector = inspect(connection)
    unique_sets = [set(c["column_names"]) for c in inspector.get_unique_constraints("push_tokens")]
    unique_sets += [set(i["column_names"]) for i in inspector.get_indexes("push_tokens") if i.get("unique")]
    if {"token"} not in unique_sets or {"user_id", "platform"} in unique_sets:
        raise RuntimeError("Push token schema verification failed")
