# Deployment and database upgrades

This repository contains deployment preparation; no hosted release is claimed. Docker is unavailable on the development host, so the container definitions have not been executed here.

## Container demo

1. Install Docker using its official distribution.
2. Copy root `.env.example` to `.env`. Supply distinct strong database/root passwords and an initial admin password of at least 12 characters, at most 72 UTF-8 bytes. Keep `.env` private.
3. Run `docker compose up --build`.
4. Open http://localhost:8080. The frontend proxies /api to backend; the database is isolated in the `store-data` volume and has no published port. Your local ECOM server is separate.
5. Verify `/api/health`, login, catalogue, Staff access and a temporary checkout.

The demo binds only to localhost. COOKIE_SECURE=false is for this HTTP localhost container demo only. Backend startup validates the database schema. The baseline SQL initializes a new container database; changing that file does not migrate an existing volume.

Never use `docker compose down -v` on data you want to retain; it removes the database volume. Ordinary `docker compose down` preserves it.

## Hosted production

Choose a hosting account/project, database and domain before deployment. No provider pricing or free-tier assumption is made.

- Build backend: `bash mvnw clean verify` from backend. Java 17+ runs the packaged JAR.
- Build frontend using the actual HTTPS API URL, or use the supplied same-origin proxy. Backend CORS is centrally configured.
- Activate `SPRING_PROFILES_ACTIVE=prod`.
- Set DB_URL, DB_USERNAME, DB_PASSWORD and FRONTEND_ORIGINS (comma-separated explicit allowed origins).
- Set PORT if the host requires it. Keep COOKIE_SECURE=true behind HTTPS. Same-origin hosting uses COOKIE_SAME_SITE=lax; genuinely cross-site hosting requires carefully verified cookie settings and HTTPS.
- BOOTSTRAP_ADMIN_ENABLED defaults false. For first initialization only, enable it and provide INITIAL_ADMIN_EMAIL and a strong INITIAL_ADMIN_PASSWORD. Disable it again after verifying the account. Existing accounts/passwords are never overwritten.
- Do not use the local demo admin password in a public deployment.

The production profile validates rather than modifies schema. Passwords stay out of Git, the Docker build context and the packaged JAR. The optional local properties file is loaded from the source checkout only for local development, not bundled.

## Schema procedure

Back up the target database before changes. Ensure you selected the fork/deployment database, never the college database.

- New empty MySQL database: apply `backend/db/schema.sql` before starting the production profile.
- Existing fork database: apply migration 001 for STAFF enum support and migration 002 for version/checkout key fields and uniqueness.
- These scripts are manually applied, not a Flyway/Liquibase-managed migration history. Keep a deployment record of script versions.
- Migration 002 is repeatable and checks information_schema before adding fields/indexes. Migration 001 preserves all existing supported roles.

Example, letting the MySQL client prompt for the password:

```sh
mysql -h HOST -u USER -p DATABASE < backend/db/schema.sql
# Existing installations instead:
mysql -h HOST -u USER -p DATABASE < backend/db/migrations/001-enable-staff.sql
mysql -h HOST -u USER -p DATABASE < backend/db/migrations/002-checkout-integrity.sql
```

Keep the legacy order_items.price column while the compatibility model still uses it.

## Release verification

Verified locally: MySQL migration/acceptance workflow, clean backend verification, frontend build/lint, and exclusion of local credentials from the JAR. The packaged production profile was started outside the source checkout on temporary port 8082 with bootstrap disabled; ECOM schema validation and /api/health succeeded. It was then stopped.

Before calling a hosted release complete, verify HTTPS/API/cookies/CORS, routes on refresh, all three roles, checkout concurrency/retry, monitoring and backup/restore. Establish measurable uptime/recovery/capacity targets with the owner. The local 29-request smoke timing is not a production load test.

## HTTPS overlay and uploads

For an actual server with ports 80/443 available and domain DNS pointing to it, set `STORE_DOMAIN` and `TLS_EMAIL` in the ignored `.env`, then use `docker compose -f compose.yaml -f compose.production.yaml up -d --build`. Caddy terminates HTTPS and renews certificates; Nginx preserves the forwarded HTTPS scheme. The backend remains internal, with secure cookies enabled. Configure Google’s production redirect/origin separately in Google Cloud. This overlay is preparation, not evidence of a live deployment; Docker is not installed on the current development host.

Uploaded images live in the persistent `product-images` volume, separate from the database. Back up and restore this volume with the database. Each upload is admin-only, bounded to 5 MB, limited to JPEG/PNG, decoded/re-encoded and stored with a random name. Pending unused uploads are retained; remove unreferenced files during maintenance after a backup.

Apply new manual migrations in feature order after the earlier STAFF, checkout-integrity and Google-login migrations: registration verification, password reset, checkout fulfilment, saved addresses, and collection status. Several historical scripts share a numeric prefix, so use their full filenames; these scripts do not form an automatic migration runner. Never reapply the non-repeatable ALTER scripts. Record each applied filename.

Sign-in throttling allows up to ten attempts per email and thirty per IP in ten minutes. It is local to each backend process; use shared limits and validate proxy handling when scaling to multiple instances.

Order and account emails use a bounded local queue after commit. The queue is not durable, so provider errors, capacity exhaustion or restarts can lose notifications. Use a transactional outbox/durable queue before promising reliable production notifications. Email failures do not roll back orders.


## Verification on 6 October 2026

- Clean backend release verification passed 110 tests. Frontend build and lint passed.
- The packaged JAR excludes application-local.properties. Started outside the checkout with the production profile, bootstrap disabled and ECOM schema validation; database health returned HTTP 200. The temporary process was stopped.
- An ECOM backup was created in the ignored local backups directory with owner-only permissions. It was restored into a temporary database; row counts for all ten tables matched the source. The temporary database was then removed. The college database was not used.
- Docker is not installed here: Compose/HTTPS execution and uploaded-image-volume restoration remain unverified. No public deployment is claimed.

For a restore, stop application writes, restore the SQL into an empty recovery database, restore the matching upload storage, start the production profile against the recovery database and verify health, catalogue images, accounts and orders. Keep backups private: they contain account hashes and customer/order details. Agree on recovery time and backup retention before hosting.
