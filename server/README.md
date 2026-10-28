# PièceDZ API starter

This is a starter API, not ready for public production use.

## Local setup
1. Install Node.js 20+ and PostgreSQL.
2. Create a database named `piecedz`.
3. Run `psql "$DATABASE_URL" -f schema.sql`.
4. Copy `.env.example` to `.env` and configure `DATABASE_URL`.
5. Run `npm install` and `npm start`.

Endpoints:
- `GET /api/health`
- `GET /api/listings?q=filter&category=سيارات&wilaya=المسيلة`
- `POST /api/listings` deliberately returns 501 until authenticated posting and moderation are implemented.

Before a public launch, add real authentication, role-based authorization, verified phone numbers, rate limiting, audit logging, image upload with type/size checks, moderation/admin tools, database backups, HTTPS, and privacy/terms pages. Never expose the database URL in the Android app.
