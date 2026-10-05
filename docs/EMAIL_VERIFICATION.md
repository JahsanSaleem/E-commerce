# Registration email verification

Email/password registration requires a six-digit email OTP. Existing users, administrators and staff continue to sign in normally. Google registration continues using Google's verified email claim.

## Local Brevo setup

Use a verified sender in Brevo and generate an **SMTP key** (not an API key).
Put these in the ignored `backend/src/main/resources/application-local.properties`:

```properties
spring.mail.username=YOUR_BREVO_SMTP_LOGIN
spring.mail.password=YOUR_BREVO_SMTP_KEY
store.mail.from=YOUR_VERIFIED_SENDER_EMAIL
```

Host defaults to `smtp-relay.brevo.com`, port 587. Authentication and STARTTLS are required, including certificate hostname validation. Copy the SMTP login displayed by Brevo; it may differ from the account email. Never commit the real local file or paste keys into chat.

Restart the backend after saving settings: `cd backend && bash mvnw spring-boot:run`.
Open `/register`, register a new address you own, check its inbox/spam folder, and enter the code. Only then is the account created and the store session authenticated. Sign out and sign in with email/password to confirm OTP is not required again.

If email sending is unconfigured or fails, registration returns 503 and does not create a user or pending record. Brevo account approval, sender verification and phone verification may be required before sending. Existing logins and public catalogue browsing remain available.

## Implementation

- `POST /api/auth/register` validates details and returns 202 with a random registration ID, recipient email, expiry and resend time. No authenticated session or user is created.
- `POST /api/auth/register/verify` accepts `registrationId` and `code`, creates a CUSTOMER, consumes the pending record and rotates the store session ID. Codes cannot be replayed.
- `POST /api/auth/register/resend` accepts `registrationId` and sends a replacement code. Previous code hashes are replaced.
- Pending passwords and codes are BCrypt hashes. Plaintext codes are only passed to the mail sender, never returned through the API or logged.
- Codes expire after five minutes; five failed attempts block verification until a new code is requested. Attempts persist even when verification returns an error.
- Resends have a 60-second cooldown and a maximum of five email sends per registration. The registration expires after one hour; stale records are removed hourly.
- Additional local IP protection allows five email requests per ten minutes. This limiter is per backend instance and resets on restart; use shared limits for multiple instances and configure trusted proxy handling before public deployment. NAT users share this limit.
- The frontend preserves only challenge details in sessionStorage so a page refresh can resume verification; it clears the registration password after the initial request. Losing that browser state requires starting again with the same email after the one-hour pending window, or using another address.

## Database and containers

Local development (`ddl-auto=update`) creates `pending_registration` automatically.
For an existing production database using schema validation, apply `backend/db/migrations/002-registration-verification.sql` before starting the updated backend. New container databases use the updated `backend/db/schema.sql`; existing volumes need the migration.
Container configuration passes `SMTP_LOGIN`, `SMTP_KEY` and `MAIL_FROM` from the ignored `.env` file. No live secrets are included in committed examples.

## Validation

Backend integration tests cover blocked login before verification, hash storage, normal login afterward, code consumption, persisted failed attempts, expiry, cooldowns, resend limits, duplicate emails, send-failure rollback, IP limits and registration role/session behavior. Live inbox delivery requires the local credentials above and a registration using an address you own.
