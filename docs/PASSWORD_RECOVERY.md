# Password recovery

Open Sign In → Forgot password? → enter the account email → enter the emailed six-digit code and confirm a new password. After success, sign in normally with the new password.

The existing ignored Brevo SMTP settings are reused. No new provider credentials are needed. Google-only accounts must continue using Google sign-in; this flow does not convert them to password accounts.

## Backend behavior

- `POST /api/auth/password/forgot` takes `email` and returns 202 with a random `resetId`, expiry, resend time and the same message for existing, unknown and Google-only addresses.
- `POST /api/auth/password/reset` takes `resetId`, `code` and `password`. It does not sign the user in.
- Codes use a cryptographic random generator and BCrypt storage; expiry is five minutes. Five incorrect attempts persist and block that challenge. A new request replaces the previous challenge/code after a 60-second cooldown, with at most five sends per email per hour.
- Requests share the registration limiter: five email requests per IP per ten minutes. This limiter is per process and resets on restart. A public multi-instance deployment needs shared rate limits and trusted proxy configuration.
- Only email/password accounts receive codes. Unknown addresses have the same database challenge shape and quota behavior, but cannot reset an account. Passwords and reset codes are never returned, logged or stored in the browser.
- Resetting checks the account's previous password hash under a database lock, replaces the BCrypt password and consumes the code while retaining the hourly send count. Roles remain unchanged.
- Credential versions expire all previous store sessions on their next API request. The reset request also invalidates its current store session. Normal and Google sign-ins record the current version.
- Email dispatch starts only after the database transaction commits, using a bounded background queue. A confirmation email is sent after a successful change without including the password. SMTP delays/errors do not reveal whether an address exists or roll back a successful password change.
- This queue is local and not durable: a restart, full queue or provider failure can lose delivery. The user can request another code after the cooldown; operational failures produce a generic server warning without secrets. Use a durable queue/outbox for production delivery guarantees.

## Database

Local `ddl-auto=update` creates `password_reset` and adds `user.credential_version` with default zero. Fresh container databases use the updated baseline schema. Existing production databases using validation must apply `backend/db/migrations/003-password-reset.sql` once before starting this version.

## Validation

Integration tests cover password hashing, old-password rejection, fresh login, single-use codes, expiry, persistent incorrect attempts, cooldown and hourly limits, unknown/Google accounts, delivery failure, changed-password snapshots, validation and revocation of old sessions. UI checks cover the login link, recovery form and code/password confirmation step.

Design reference: [OWASP Forgot Password Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Forgot_Password_Cheat_Sheet.html).
