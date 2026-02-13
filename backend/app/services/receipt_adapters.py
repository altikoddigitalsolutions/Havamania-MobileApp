from dataclasses import dataclass
from datetime import UTC, datetime, timedelta


@dataclass
class ValidationResult:
    is_valid: bool
    plan_code: str
    status: str
    expires_at: datetime | None
    original_transaction_id: str | None


class ReceiptValidationAdapter:
    def validate(self, receipt_data: str, plan_code: str) -> ValidationResult:
        raise NotImplementedError


class IOSReceiptValidationAdapter(ReceiptValidationAdapter):
    def validate(self, receipt_data: str, plan_code: str) -> ValidationResult:
        # Skeleton: replace with App Store Server API verification.
        if not receipt_data:
            return ValidationResult(False, "free", "expired", None, None)
        return ValidationResult(
            is_valid=True,
            plan_code=plan_code,
            status="active",
            expires_at=datetime.now(UTC) + timedelta(days=30),
            original_transaction_id="ios-demo-transaction",
        )


class AndroidReceiptValidationAdapter(ReceiptValidationAdapter):
    def validate(self, receipt_data: str, plan_code: str) -> ValidationResult:
        # Skeleton: replace with Google Play Developer API verification.
        if not receipt_data:
            return ValidationResult(False, "free", "expired", None, None)
        return ValidationResult(
            is_valid=True,
            plan_code=plan_code,
            status="active",
            expires_at=datetime.now(UTC) + timedelta(days=30),
            original_transaction_id="android-demo-transaction",
        )
