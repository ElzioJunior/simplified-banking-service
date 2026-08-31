# BDR-0004 — Successful Transfer Notification Policy

- Status: Accepted
- Date: 2026-08-31
- Deciders: Engineering and Product Team
- Supersedes: none
- Superseded by: none

## Context

A client should be informed when a financial transfer has been successfully completed. Notification processing is secondary to the financial transaction itself and must not change the outcome of an already completed transfer.

## Decision

Every successfully completed transfer must create one notification intent for
the holder of the source account.

Notification rules:

- Notifications are created only for successfully completed transfers.
- The intended recipient is identified by the source account ID; resolving a
  delivery address or channel remains outside the current scope.
- A failed or rejected transfer must not generate a successful-transfer notification.
- Notification processing must occur asynchronously relative to the transfer request.
- A notification-delivery failure must not roll back or invalidate a successfully completed financial transfer.
- A transfer must not produce duplicate notifications for the same intended notification recipient and event.
- The notification must contain enough transfer context to identify the related transaction without exposing unnecessary sensitive information.
- For the current scope, notification creation means durably recording the
  unique asynchronous intent. Successful channel delivery is not part of the
  synchronous transfer response.

The specific delivery channel is outside the current business scope.

## Consequences

### Positive

- Keeps clients informed about completed financial activity.
- Separates notification availability from the correctness of financial processing.
- Prevents notification failures from affecting account balances.
- Supports future notification channels without changing the transfer business rules.

### Negative or trade-offs

- A successfully completed transfer may temporarily exist before its notification is delivered.
- Notification failures require independent handling and observability.
- Duplicate prevention is required for notification processing.

## Alternatives considered

- Send notifications synchronously before completing the transfer response — not selected because notification availability should not determine financial transaction success.
- Roll back transfers when notification delivery fails — rejected because notification delivery is not part of the financial atomicity boundary.
- Do not generate transfer notifications — not selected because successful transfer notification is a stated business requirement.

## Validation

Tests must verify that successful transfers create the expected notification event, failed or rejected transfers do not create successful-transfer notifications, duplicate notification processing is prevented, and notification-delivery failure does not alter the completed transfer or account balances.
