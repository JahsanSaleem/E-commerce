# Verification evidence — 2 October 2026

## Automated

- Backend full suite: 73 tests passing in the final clean verification, including context startup, server permissions, Staff access, account creation/role rules, negative validation, deletion conflicts, checkout rollback/concurrency and retry identity.
- Tests use H2 in MySQL mode with separate in-memory data. This does not replace MySQL verification.
- Frontend production build and Oxlint pass. Compose YAML parses; container runtime is unverified.
- Git diff whitespace check passes.

Run backend `bash mvnw clean verify`, and frontend `npm run build` / `npm run lint`.

## Actual ECOM / MySQL

Verified through the running backend:

- Database connection, public catalogue reads, admin login, seven tables and admin bootstrap.
- Guest catalogue writes denied; Admin writes reach services; CORS preflight succeeds.
- Referenced category and cart product deletion return 409; records remain; deleting after removing references works.
- Staff creation/login/order access; Staff denied catalogue/accounts; demotion revokes an existing session; last-admin demotion rejected.
- Checkout returns an order, clears cart and deducts stock; retry key returns the same order.
- Invalid status jump rejected. Cancellation restores stock; repeating it does not restore twice.
- Simultaneous two-customer checkout of the last unit yields one 200 and one 409, with stock zero.

The final workflow smoke run made 29 requests: median 7.5 ms, maximum 71.7 ms locally. This is not a performance/load benchmark or an agreed service target. Temporary records were cleaned up using only IDs created by that run.

The MySQL role enum initially rejected STAFF although the isolated tests passed. Migration 001 corrected ECOM; the live workflow suite then passed. This is why migrations and MySQL checks accompany the portable tests.

## Browser

Local in-app-browser smoke checks: homepage, login, catalogue/availability controls, admin navigation, Users form/table, Orders empty state and Inventory empty state. Admin login succeeds and account data loads. Navigation wraps; controls have labels and status/error feedback.

This is not a completed cross-browser/device matrix or representative user study. Populated-table and Staff workflow behavior is supported by API/integration tests; full visual acceptance remains a follow-up.

## Release limits

Clean backend verify/package succeeded. Local credential files are absent from the packaged JAR. The packaged production profile started outside the checkout on port 8082, validated ECOM and returned health UP; that validation server was stopped. Docker is not installed on the current host, so container runtime testing and an actual hosted deployment are unverified. Client approvals, course submission, uptime, recovery targets and backup restore exercises were not verified.

## UI polish — 3 October 2026 (`new-ui`)

Refreshed the storefront header/footer, homepage/category cards, catalogue/product details, login/register and cart/order panels. Added shared SVG icons, clearer stock badges and prices, mobile filter disclosure, a skip-to-content link and reduced-motion styling. The management workspace shares the new colors, controls and panels. Homepage totals/category counts come from the catalogue rather than fixed marketing numbers.

Frontend build and lint pass. Browser checks verified nine product cards with loaded images, Next pagination, category filtering, ascending price sorting, out-of-stock filtering and reset behavior. Product details, existing admin login/management navigation, cart/orders, logout and the enabled Google button were checked without placing orders or changing inventory. Mobile layouts were rendered in temporary same-origin preview frames at 390px and 320px; home/catalogue/login had no horizontal overflow. The mobile disclosure opened and search returned the two drill products, resetting the page parameter. The temporary preview file was removed and the browser viewport restored.

The existing store data and backend authentication rules were not changed. This is local visual/interaction verification, not a full browser/device matrix or a real Google account sign-in test.

### Typography revision — 2026-10-03

- Replaced the application font stack with self-hosted Source Sans 3, including its SIL OFL license and source record.
- Removed widely spaced uppercase labels and tight heading tracking; reduced heavy heading weights across storefront and management pages.
- Verified the font loads, catalogue still shows nine products, and desktop catalogue plus 390px home/catalogue/login have no horizontal overflow.
- Frontend production build and lint passed.

### Retail storefront revision — 2026-10-04

Design references: https://www.toolstation.com/ and https://www.screwfix.com/ .
Adapted the prominent search, department navigation and rectangular merchandising structure using Mustafa branding and the existing local product photos.

- Replaced rounded icon department cards with eight photo-led department links; added four in-stock products with actual database prices and detail links.
- Added accessible header search that navigates to the existing catalogue filter.
- Applied compact corners to shared panels, controls and product cards; removed the old decorative homepage styling.
- Browser checks: search for drill returns two products, Power Tools links to three products, pagination shows nine on both first and second pages, mobile filters expand correctly.
- Desktop, 390px and 320px layout checks passed without page overflow; department images remain inside their allotted area. Home, catalogue and login reviewed.
- Frontend build, lint and Git whitespace checks passed.

### Banana.lk font match — 2026-10-04

- Confirmed https://banana.lk/ loads Inter and renders body, department headings and product text using Inter.
- Switched the shared application font to self-hosted Inter (SIL OFL license included); removed unused Source Sans 3 assets.
- Browser confirmed Inter loads and is inherited by headings and controls; desktop/home and 390px home/catalogue have no page overflow. Catalogue still shows nine products.
- Frontend build, lint and whitespace checks passed.

### Registration email OTP — 2026-10-05

- Added registration-only verification with hashed pending credentials, five-minute codes, retry limits, resend cooldowns and delivery-failure rollback. Existing email/password logins remain unchanged.
- All 92 backend tests passed, including OTP expiry, failed-attempt persistence, code consumption, role/session handling and normal login after verification.
- Frontend production build, lint and Git whitespace checks passed. Registration form reviewed in the browser at 1280px without horizontal overflow.
- Local SMTP credentials remain ignored by Git. Live Brevo inbox delivery still requires local SMTP login, SMTP key and a verified sender.

### Contact footer — 2026-10-04

- Adapted Banana.lk's light contact-led footer with brand/contact block, quick navigation, customer-care links and a copyright strip.
- Added real department links and clearly labelled sample address, phone and reserved .example email in frontend/src/config/storeContact.js.
- Desktop footer reviewed visually; 320px and 390px iframe checks confirm two-column links, eight departments and no horizontal overflow.
- Frontend build, lint and whitespace checks passed.

### Slightly larger text — 2026-10-04

- Increased explicit UI text sizes by 1px, shared small/body text by 1px and responsive heading limits by 2px.
- Checked desktop and 320px/390px home, catalogue and login layouts; no horizontal overflow. Footer links now render at 14px, body at 17px and desktop navigation at 14px.
- Frontend build, lint and whitespace checks passed.

### Forgot password — 2026-10-05

- Added the sign-in recovery link, email reset code form, password confirmation, resend countdown and sign-in success message.
- All 101 backend tests passed, including password hashing/replacement, consumed and expired codes, persisted incorrect attempts, send limits, unknown/Google-only accounts, email failures and expiry of existing sessions.
- Frontend production build, lint and whitespace checks passed. Browser checks verified the link, code form and password mismatch feedback; desktop 1280px and mobile 390px had no page overflow.
- Restarted the local backend successfully; existing Brevo settings are reused. Recovery UI verification used a nonexistent reserved test address and sent no real email. Password reset and confirmation delivery were verified with a mocked mail sender in integration tests.
