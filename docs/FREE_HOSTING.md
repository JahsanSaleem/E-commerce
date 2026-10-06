# Free demo hosting: Render + Aiven + Cloudinary

Prepared 6 October 2026. This is a demo deployment plan, not a live deployment or a capacity guarantee. Free limits and provider policies can change.

## Topology

One Render Free Docker web service builds React and serves it from Spring Boot. Website, API and Google callbacks use the same HTTPS hostname, avoiding cross-site session-cookie issues. Aiven runs MySQL; Cloudinary stores new admin uploads. Brevo delivers registration/reset/order mail using HTTPS rather than blocked SMTP ports. Local development keeps its current SMTP and local upload defaults.

## 1. Create the database

In Aiven, create **MySQL on the Free plan**, not a paid trial. Its current storage limit is 1GB. Choose a nearby region offered by that plan. Wait until the service is running.

Use MySQL Workbench to connect with the host, port, username and password shown by Aiven. Configure SSL with the downloadable Aiven CA certificate and verify the server certificate. Choose the provided database (often `defaultdb`), then run `backend/db/schema.sql` only against that empty database. Do not run it on local ECOM or the college database. This creates tables but does not import products or customer data.

For the Java connection, download Aiven's CA certificate. Set up a PKCS12 truststore locally:

```sh
keytool -importcert -alias aiven -file ca.pem -keystore aiven-truststore.p12 -storetype PKCS12
```

Enter a truststore password when prompted. Encode that file as Base64 locally and store the result in Render's private `AIVEN_TRUSTSTORE_BASE64` environment variable. Store its password as `AIVEN_TRUSTSTORE_PASSWORD`. Neither value belongs in Git or chat. The container reconstructs the file under /tmp before starting Java.

`DB_URL` in Render must use a JDBC URL, not Aiven's `mysql://` URI:

```text
jdbc:mysql://YOUR_AIVEN_HOST:YOUR_PORT/YOUR_DATABASE?sslMode=VERIFY_IDENTITY&trustCertificateKeyStoreUrl=file:/tmp/aiven-truststore.p12&trustCertificateKeyStoreType=PKCS12
```

The startup script passes the truststore password through MySQL Connector configuration. Copy `DB_USERNAME` and `DB_PASSWORD` privately from Aiven. Do not add a password to the JDBC URL. Certificate verification must remain enabled.

## 2. Prepare provider credentials

- Brevo: Settings → SMTP & API → **API keys**. Generate an API key; this differs from the SMTP key. Put it in `BREVO_API_KEY`. Use an existing verified sender as `MAIL_FROM`.
- Cloudinary: Settings → API Keys. Put the cloud name, API key and API secret into `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY` and `CLOUDINARY_API_SECRET`. These credentials stay on the backend. Uploads remain admin-only and are decoded/re-encoded before transmission.
- No existing local customer/account information is transferred automatically. Start with an empty hosted database and demo catalogue, or agree on a selective catalogue import first.

## 3. Create the Render service

Choose New → Web Service, connect `JahsanSaleem/E-commerce`, branch **new-ui**. Select Docker, keep the root directory empty, use **Dockerfile.render**, and choose **Free**. Do not create an additional paid frontend service. The root `render.yaml` also describes this setup for a Blueprint.

Set these private environment variables before deployment:

| Variable | Value |
| --- | --- |
| SPRING_PROFILES_ACTIVE | prod |
| PORT | 10000 |
| DB_URL / DB_USERNAME / DB_PASSWORD | Aiven JDBC connection and credentials above |
| AIVEN_TRUSTSTORE_BASE64 / AIVEN_TRUSTSTORE_PASSWORD | Certificate truststore above |
| SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE | 3 |
| FRONTEND_ORIGINS | Your public Render HTTPS URL, without a trailing slash |
| COOKIE_SECURE | true |
| MAIL_PROVIDER | brevo-api |
| BREVO_API_KEY / MAIL_FROM | API key and verified sender |
| IMAGE_PROVIDER | cloudinary |
| CLOUDINARY_CLOUD_NAME / CLOUDINARY_API_KEY / CLOUDINARY_API_SECRET | Cloudinary credentials |
| GOOGLE_LOGIN_ENABLED | false initially |
| BOOTSTRAP_ADMIN_ENABLED | true for first startup only |
| INITIAL_ADMIN_EMAIL | Your chosen admin email |
| INITIAL_ADMIN_PASSWORD | Unique strong password: at least 12 characters, at most 72 UTF-8 bytes |

Set health check `/api/health`. The Docker image sets a conservative JVM memory budget for Render's 512MB limit; actual memory and cold-start behavior still need hosted testing. After verifying the administrator account, set `BOOTSTRAP_ADMIN_ENABLED=false` and remove `INITIAL_ADMIN_PASSWORD`. Do not import the local demo admin with its weak password.

## 4. Google sign-in

After normal login works, add the Render URL to Google's allowed JavaScript origins and `https://YOUR_SERVICE.onrender.com/login/oauth2/code/google` to redirect URIs. Set GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET, GOOGLE_REDIRECT_URI, GOOGLE_FRONTEND_URL and GOOGLE_LOGIN_ENABLED=true in Render. The frontend URL and redirect must use the same public service origin.

## 5. Acceptance and limits

Verify `/api/health`, page refresh on `/products` and `/account`, login/logout, registration/reset inbox delivery, admin image upload and its persistence after redeployment, catalogue filters, checkout fee/retry, and own-order permissions. Test a cold start. Do not claim hosting complete before these pass.

Render Free sleeps after inactivity and has no persistent local disk. New images must use Cloudinary; the existing repository catalogue photos are bundled in the image. Any older `/api/media/...` files need a separate selective migration; container builds do not include local uploads. Memory, monthly build/traffic allowances and Cloudinary credits are limited. Aiven's 1GB is database storage, not 5GB. Monitor each dashboard.

Order notifications use a bounded in-memory queue; process shutdowns can lose queued notifications. This remains a demo limitation. Download database backups privately and test restoring into a separate database. Uploaded Cloudinary images require their own export/backup plan.

Local verification: 113 backend tests passed with zero failures/errors; frontend build/lint passed. A packaged combined build was checked against local ECOM using the Render JVM limits: database health and SPA routes returned HTTP 200. Docker itself and real cloud API/inbox delivery remain unverified; provider tests use mocked HTTP responses.

Sources: https://aiven.io/docs/platform/concepts/tls-ssl-certificates, https://render.com/docs/free, https://aiven.io/docs/products/mysql/concepts/mysql-free-tier, https://cloudinary.com/documentation/image_upload_api_reference, https://developers.brevo.com/reference/send-transac-email
