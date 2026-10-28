# PièceDZ API v0.2

This API starter includes:
- Registration and login with bcrypt password hashing and 7-day JWT tokens.
- Rate limits, Helmet security headers, request body limits, parameterized SQL.
- Public search of moderated active listings.
- Authenticated listing creation (new listings stay pending moderation).
- Seller's own listing list and mark-as-sold.
- Purchase requests, reports, and admin moderation endpoints.
- PostgreSQL schema and health check.

## Important security and deployment notes
This is a functional backend starter, not a complete audited production service.
- Use HTTPS only in deployment.
- Use a long random `JWT_SECRET`; do not commit `.env` or include secrets in the APK.
- Create the first admin account using a controlled database operation: after registering a trusted account, run `UPDATE users SET role='admin' WHERE phone='YOUR_VERIFIED_PHONE';` using a secure database console. Never expose this query through a public endpoint.
- The registration endpoint does not verify phone ownership yet. Before a public launch, add SMS/OTP verification or another verification workflow, anti-abuse controls, privacy/terms pages, monitoring, backup/restore testing, and image storage with file type/size validation.
- For production, configure PostgreSQL TLS and `PGSSLMODE=require` when required by your provider.
- CORS is not enabled because the intended client is Android; do not enable broad origins if a web client is added.
- Public listing search returns phone numbers for active offers. Tell sellers this clearly and obtain their consent before publishing; consider an in-app contact relay instead.
- Admin moderation is API-only in this version; there is no admin UI yet.

## Initialize
```bash
npm install
cp .env.example .env
# Edit .env with a real PostgreSQL DATABASE_URL and random JWT_SECRET
set -a; . ./.env; set +a
npm run db:init
npm start
```

Test health:
```bash
curl http://127.0.0.1:8787/api/health
```
Do not expose a local development server to the public internet without HTTPS and deployment hardening.
