# Personal fork development plan

The user develops all three historical SE2012 domains personally in this fork: catalogue, cart/orders, and users/roles/inventory. Reference context was read on 2 October 2026. This checkout and ECOM are independent of the college checkout/database.

## Completed increments

1. Admin order/inventory UI, local demo admin and isolated ECOM configuration.
2. Catalogue availability filtering and visible stock.
3. Admin-only server catalogue writes; guest/customer/Staff permission tests.
4. Friendly referenced deletion conflicts and rollback.
5. Admin account listing/creation, Staff role and workspaces.
6. Admin role editing, last-admin protection and session permission refresh.
7. Order transitions and exactly-once pre-shipping cancellation restock.
8. Atomic stock deduction, transactional/serialized cart changes, checkout retry identity.
9. Automated workflow/concurrency tests, MySQL live acceptance and browser smoke checks.
10. Code-aligned requirements, four workflow scenarios and editable UML sources.
11. Production configuration, database upgrade scripts, health endpoint and container definitions.

12. Registration email verification and forgotten-password recovery.
13. Backend catalogue pagination, search and filters.
14. Delivery/collection checkout, LKR 249 delivery, free collection and order review.
15. Account profile/password changes and owned saved addresses.
16. Validated admin product image uploads with persistent storage.
17. Admin order/delivered-value/low-stock dashboard.
18. Order emails after commit and collection readiness status.
19. Login throttling, HTTPS deployment overlay, local production validation and a verified database restore.

Each increment has a focused commit. The user authorized continuing and pushing increments without repeated approval on 2 October. Hosting execution still requires an actual deployment target; no hosted URL is claimed.

## Remaining evidence / business decisions

- Actual container and hosted deployment, HTTPS and uptime checks. Local database backup/restore is verified; image-volume restore remains to be exercised in Docker.
- Representative user study, full browser/device matrix and load testing against agreed targets.
- Client decisions on payment handling, refunds, cancellation and repricing. The user approved delivery/collection and the LKR 249/free fees. Current transition/cancellation policy is provisional and documented.
- Lecturer exception for the original three-member team, client meeting evidence, course deadlines and actual assessment submission are not established by this fork.

Real payment gateways, GPS tracking, AI recommendations, multi-vendor selling and live chat were excluded by the supplied scope. Separate SE2032 schema drafts are not application migrations.

See REQUIREMENTS.md, WORKFLOWS.md, VERIFICATION.md and DEPLOYMENT.md for current evidence and implementation limits.
