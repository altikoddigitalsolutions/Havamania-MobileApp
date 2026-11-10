"""Start an isolated loopback-only PostgreSQL cluster, test migrations, then stop it.
Requires existing PostgreSQL binaries. Never connects to an installed service.
Logs/data remain under ignored .local-tests/ for diagnosis.
"""
import argparse
import os
import secrets
import socket
import subprocess
import sys
from pathlib import Path
from uuid import uuid4


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--pg-bin", type=Path, required=True)
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    audit_root = root / ".local-tests" / ("postgres-" + uuid4().hex)
    audit_root.mkdir(parents=True)
    data = audit_root / "data"
    password = secrets.token_hex(24)
    password_file = audit_root / "password.txt"
    password_file.write_text(password, encoding="ascii")
    env = dict(os.environ, PGPASSWORD=password, PGUSER="audit", PGHOST="127.0.0.1")
    with socket.socket() as sock:
        sock.bind(("127.0.0.1", 0))
        port = sock.getsockname()[1]
    env["PGPORT"] = str(port)
    flags = subprocess.CREATE_NO_WINDOW if os.name == "nt" else 0

    def pg(binary, *argv):
        executable = args.pg_bin / (binary + (".exe" if os.name == "nt" else ""))
        # A detached Windows postgres child may inherit pipe handles. Use a file
        # so waiting for pg_ctl never waits for the server's entire lifetime.
        command_log = audit_root / f"{binary}.log"
        with command_log.open("a", encoding="utf-8") as output:
            result = subprocess.run([str(executable), *map(str, argv)], env=env, check=False,
                                    timeout=120, creationflags=flags, stdout=output,
                                    stderr=subprocess.STDOUT)
        if result.returncode:
            print(command_log.read_text(encoding="utf-8", errors="replace"))
            raise RuntimeError(f"{binary} failed with code {result.returncode}")

    started = False
    try:
        pg("initdb", "-D", data, "-U", "audit", "--pwfile", password_file,
           "--auth=scram-sha-256", "--encoding=UTF8", "--locale=C")
        started = True
        pg("pg_ctl", "-D", data, "-l", audit_root / "server.log", "-o",
           f"-h 127.0.0.1 -p {port}", "-w", "start")
        pg("createdb", "havamania_migration_audit")
        test_env = dict(env, APP_ENV="test", PG_MIGRATION_TEST_URL=
                        f"postgresql+psycopg://audit:{password}@127.0.0.1:{port}/havamania_migration_audit")
        result = subprocess.run([sys.executable, "-m", "pytest", "tests/test_postgres_migrations.py",
                                 "-q", "-p", "no:cacheprovider"], cwd=root / "backend", env=test_env,
                                timeout=300, creationflags=flags, check=False,
                                capture_output=True, text=True)
        output = result.stdout + result.stderr
        (audit_root / "test-results.txt").write_text(output, encoding="utf-8")
        print(output)
        return result.returncode
    finally:
        if started:
            pg("pg_ctl", "-D", data, "-m", "fast", "-w", "stop")
        password_file.unlink(missing_ok=True)
        print(f"Audit logs: {audit_root}")


if __name__ == "__main__":
    sys.exit(main())
