# Mustafa Hardware — personal development fork

React + Spring Boot online store for hardware, electronics and student project components. This fork extends the three-member SE2012 college project as a personal learning project. It uses its own **ECOM** database.

## Run locally

Requirements: Java 17+, Node compatible with Vite 8 (Node 22.12+ or 24), MySQL and a browser.

1. Create ECOM in your local MySQL server.
2. Copy `backend/src/main/resources/application-local.properties.example` to `application-local.properties` in the same folder, and fill in your local credentials. That file is ignored by Git and excluded from packaged artifacts.
3. For an existing installation, back up the fork database and apply the relevant full filenames in `backend/db/migrations/` in the feature order documented in [Deployment](docs/DEPLOYMENT.md) against ECOM. For a fresh development database, Hibernate creates tables. Never run these scripts against the college database.
4. From `backend`, run `bash mvnw spring-boot:run`.
5. From `frontend`, run `npm ci`, then `npm run dev -- --port 5173 --strictPort`.
6. Open http://localhost:5173. Backend runs on 8081.

Local demo startup creates `admin@mustafa.com` / `admin` only if that email is absent. Existing accounts/passwords are preserved. Production disables this default initializer; see deployment instructions.

Google sign-in is optional and disabled until configured. See [Google login setup](docs/GOOGLE_SIGN_IN.md).

## Workspaces

- Customers: server-paged catalogue (nine products per page), search and filters, details, cart, delivery/collection checkout with review, own orders, profile, password changes and saved addresses. Registration verifies email once; forgotten passwords use an emailed reset code.
- Delivery costs **LKR 249**; store collection is free. The backend calculates fees and keeps fulfilment details with each order.
- Staff: `/staff/orders`, `/staff/inventory`.
- Admin: `/admin/dashboard`, `/admin/products`, `/admin/categories`, `/admin/orders`, `/admin/inventory`, `/admin/users`.
- Admins create accounts and change roles. Passwords are hashed and omitted from API responses. The last admin cannot be demoted. Existing sessions pick up role changes on the next API request.

Admins can upload JPEG/PNG product photos directly. The dashboard shows order counts, delivered order value and low stock. Order placement and status changes queue email notifications after the database commit; see the delivery limitations in the deployment guide.

## Verify

From `backend`: `bash mvnw clean verify`. Automated tests use an isolated H2 database, not ECOM.

From `frontend`: `npm run build` and `npm run lint`.

`GET /api/health` returns 200/UP if the database query succeeds, or 503/DOWN if it fails.

## Documents

- [Requirements and architecture](docs/REQUIREMENTS.md)
- [Four main workflows](docs/WORKFLOWS.md)
- [Verification evidence](docs/VERIFICATION.md)
- [Deployment and database upgrades](docs/DEPLOYMENT.md)
- [Free demo hosting walkthrough](docs/FREE_HOSTING.md)
- [Development plan](docs/DEVELOPMENT_PLAN.md)
- Editable UML sources: `docs/uml/` (PlantUML).

Order policy for this development fork: PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED for delivery, or READY_FOR_COLLECTION → DELIVERED after PROCESSING for collection. Cancellation is allowed before shipping/collection readiness and restores stock once. DELIVERED/CANCELLED are final. This is a provisional development policy, not independently confirmed client approval.

Payment gateways, GPS delivery tracking, AI recommendations, multi-vendor selling and real-time chat are outside the supplied scope. Client approval, lecturer exceptions, assessment submission and hosted deployment are not implied by this fork's code or commits.
