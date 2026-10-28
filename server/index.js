import express from "express";
import helmet from "helmet";
import rateLimit from "express-rate-limit";
import bcrypt from "bcryptjs";
import jwt from "jsonwebtoken";
import pg from "pg";

const { Pool } = pg;
const app = express();
const port = Number(process.env.PORT || 8787);
const jwtSecret = process.env.JWT_SECRET;
if (!process.env.DATABASE_URL || !jwtSecret || jwtSecret.length < 32) {
  console.error("Required: DATABASE_URL and JWT_SECRET (at least 32 characters).");
  process.exit(1);
}
const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
  ssl: process.env.PGSSLMODE === "require" ? { rejectUnauthorized: true } : undefined,
  max: 10,
  idleTimeoutMillis: 30000,
  connectionTimeoutMillis: 5000
});
app.disable("x-powered-by");
app.use(helmet());
app.use(express.json({ limit: "32kb" }));
app.use("/api/auth", rateLimit({ windowMs: 15 * 60 * 1000, limit: 30, standardHeaders: true, legacyHeaders: false }));
app.use("/api", rateLimit({ windowMs: 60 * 1000, limit: 120, standardHeaders: true, legacyHeaders: false }));

const categories = new Set(["سيارات","شاحنات","دراجات نارية","دراجات هوائية","هيدروليك"]);
const userTypes = new Set(["محل قطع غيار","تاجر","فرد","ورشة صيانة"]);
const conditions = new Set(["جديد","مستعمل","غير محدد"]);
const phonePattern = /^[+0-9 ()-]{6,30}$/;

function fail(res, code, message) { return res.status(code).json({ error: message }); }
function cleanText(value, max) { return String(value ?? "").trim().slice(0, max); }
function auth(req, res, next) {
  const raw = req.headers.authorization || "";
  const token = raw.startsWith("Bearer ") ? raw.slice(7) : "";
  if (!token) return fail(res, 401, "authentication_required");
  try {
    const decoded = jwt.verify(token, jwtSecret, { algorithms: ["HS256"] });
    req.user = { id: Number(decoded.sub), role: decoded.role };
    if (!Number.isSafeInteger(req.user.id) || req.user.id < 1) return fail(res, 401, "invalid_token");
    next();
  } catch { return fail(res, 401, "invalid_token"); }
}
function admin(req, res, next) {
  if (req.user?.role !== "admin") return fail(res, 403, "admin_required");
  next();
}
function issueToken(user) {
  return jwt.sign({ sub: String(user.id), role: user.role }, jwtSecret, {
    algorithm: "HS256", expiresIn: "7d", issuer: "piecedz-api", audience: "piecedz-app"
  });
}
function safeUser(row) {
  return {
    id: row.id, name: row.name, phone: row.phone,
    userType: row.user_type, role: row.role, createdAt: row.created_at,
    whatsapp: row.whatsapp || "", facebookUrl: row.facebook_url || "",
    shopAddress: row.shop_address || "",
    latitude: row.latitude ?? null, longitude: row.longitude ?? null
  };
}

app.get("/api/health", async (_req, res) => {
  try { await pool.query("SELECT 1"); res.json({ ok: true, service: "piecedz-api" }); }
  catch { res.status(503).json({ ok: false, error: "database_unavailable" }); }
});

app.post("/api/auth/register", async (req, res) => {
  const name = cleanText(req.body?.name, 120);
  const phone = cleanText(req.body?.phone, 30);
  const password = String(req.body?.password ?? "");
  const userType = cleanText(req.body?.userType, 30);
  if (name.length < 2 || !phonePattern.test(phone) || password.length < 10 || password.length > 72 || !userTypes.has(userType)) {
    return fail(res, 400, "invalid_registration_fields");
  }
  try {
    const hash = await bcrypt.hash(password, 12);
    const result = await pool.query(
      "INSERT INTO users(name,phone,password_hash,user_type) VALUES($1,$2,$3,$4) RETURNING *",
      [name, phone, hash, userType]
    );
    const user = result.rows[0];
    res.status(201).json({ token: issueToken(user), user: safeUser(user) });
  } catch (e) {
    if (e.code === "23505") return fail(res, 409, "phone_already_registered");
    console.error(e.message); return fail(res, 500, "server_error");
  }
});

app.post("/api/auth/login", async (req, res) => {
  const phone = cleanText(req.body?.phone, 30);
  const password = String(req.body?.password ?? "");
  try {
    const result = await pool.query("SELECT * FROM users WHERE phone=$1 AND is_active=TRUE", [phone]);
    const user = result.rows[0];
    if (!user || !(await bcrypt.compare(password, user.password_hash))) return fail(res, 401, "invalid_credentials");
    res.json({ token: issueToken(user), user: safeUser(user) });
  } catch (e) { console.error(e.message); return fail(res, 500, "server_error"); }
});

app.get("/api/me", auth, async (req, res) => {
  try {
    const result = await pool.query("SELECT * FROM users WHERE id=$1 AND is_active=TRUE", [req.user.id]);
    if (!result.rowCount) return fail(res, 401, "account_unavailable");
    res.json({ user: safeUser(result.rows[0]) });
  } catch (e) { console.error(e.message); return fail(res, 500, "server_error"); }
});


app.get("/api/my/profile", auth, async (req, res) => {
  try {
    const result = await pool.query(
      `SELECT id,name,phone,user_type,role,created_at,whatsapp,facebook_url,
              shop_address,latitude,longitude
       FROM users WHERE id=$1 AND is_active=TRUE`,
      [req.user.id]
    );
    if (!result.rowCount) return fail(res, 404, "account_unavailable");
    res.json({ profile: safeUser(result.rows[0]) });
  } catch (e) { console.error(e.message); return fail(res, 500, "server_error"); }
});

app.patch("/api/my/profile", auth, async (req, res) => {
  const b = req.body || {};
  const whatsapp = cleanText(b.whatsapp, 30);
  const facebookUrl = cleanText(b.facebookUrl, 500);
  const shopAddress = cleanText(b.shopAddress, 300);
  const latitude = b.latitude === "" || b.latitude == null ? null : Number(b.latitude);
  const longitude = b.longitude === "" || b.longitude == null ? null : Number(b.longitude);

  if ((whatsapp && !phonePattern.test(whatsapp)) ||
      (facebookUrl && (!/^https:\/\/(www\.)?facebook\.com\//i.test(facebookUrl))) ||
      (latitude !== null && (!Number.isFinite(latitude) || latitude < -90 || latitude > 90)) ||
      (longitude !== null && (!Number.isFinite(longitude) || longitude < -180 || longitude > 180)) ||
      ((latitude === null) !== (longitude === null))) {
    return fail(res, 400, "invalid_profile_fields");
  }

  try {
    const result = await pool.query(
      `UPDATE users SET whatsapp=$1,facebook_url=$2,shop_address=$3,
              latitude=$4,longitude=$5
       WHERE id=$6 AND is_active=TRUE
       RETURNING id,name,phone,user_type,role,created_at,whatsapp,
                 facebook_url,shop_address,latitude,longitude`,
      [whatsapp,facebookUrl,shopAddress,latitude,longitude,req.user.id]
    );
    if (!result.rowCount) return fail(res, 404, "account_unavailable");
    res.json({ profile: safeUser(result.rows[0]) });
  } catch (e) { console.error(e.message); return fail(res, 500, "server_error"); }
});

app.get("/api/listings", async (req, res) => {
  const q = cleanText(req.query.q, 100);
  const category = cleanText(req.query.category, 30);
  const wilaya = cleanText(req.query.wilaya, 80);
  if (category && !categories.has(category)) return fail(res, 400, "invalid_category");
  try {
    const result = await pool.query(
      `SELECT l.id,l.name,l.reference_number,l.category,l.price_dzd,l.quantity,l.condition,l.wilaya,l.description,l.created_at,
              u.name AS seller_name,u.phone AS seller_phone,u.user_type AS seller_type,
              u.whatsapp AS seller_whatsapp,u.facebook_url AS seller_facebook_url,
              u.shop_address AS seller_shop_address,u.latitude AS seller_latitude,
              u.longitude AS seller_longitude
       FROM listings l JOIN users u ON u.id=l.owner_id
       WHERE l.status='active' AND u.is_active=TRUE
       AND ($1='' OR l.name ILIKE '%'||$1||'%' OR l.reference_number ILIKE '%'||$1||'%')
       AND ($2='' OR l.category=$2) AND ($3='' OR l.wilaya=$3)
       ORDER BY l.created_at DESC LIMIT 100`,
      [q, category, wilaya]
    );
    res.json({ items: result.rows });
  } catch (e) { console.error(e.message); return fail(res, 500, "server_error"); }
});

app.post("/api/listings", auth, async (req, res) => {
  const b = req.body || {};
  const name = cleanText(b.name, 160);
  const ref = cleanText(b.referenceNumber, 100);
  const category = cleanText(b.category, 30);
  const wilaya = cleanText(b.wilaya, 80);
  const description = cleanText(b.description, 2000);
  const condition = cleanText(b.condition || "غير محدد", 20);
  const quantity = Number(b.quantity ?? 1);
  const price = b.priceDzd === "" || b.priceDzd == null ? null : Number(b.priceDzd);
  if (name.length < 2 || !categories.has(category) || wilaya.length < 2 ||
      !conditions.has(condition) || !Number.isInteger(quantity) || quantity < 1 || quantity > 1000000 ||
      (price !== null && (!Number.isFinite(price) || price < 0 || price > 1e12))) {
    return fail(res, 400, "invalid_listing_fields");
  }
  try {
    const result = await pool.query(
      `INSERT INTO listings(owner_id,name,reference_number,category,price_dzd,quantity,condition,wilaya,description,status)
       VALUES($1,$2,$3,$4,$5,$6,$7,$8,$9,'pending')
       RETURNING id,name,reference_number,category,price_dzd,quantity,condition,wilaya,description,status,created_at`,
      [req.user.id,name,ref,category,price,quantity,condition,wilaya,description]
    );
    res.status(201).json({ item: result.rows[0], message: "listing_pending_moderation" });
  } catch (e) { console.error(e.message); return fail(res, 500, "server_error"); }
});

app.get("/api/my/listings", auth, async (req, res) => {
  try {
    const result = await pool.query(
      "SELECT id,name,reference_number,category,price_dzd,quantity,condition,wilaya,description,status,created_at FROM listings WHERE owner_id=$1 ORDER BY created_at DESC LIMIT 100",
      [req.user.id]
    );
    res.json({ items: result.rows });
  } catch (e) { console.error(e.message); return fail(res, 500, "server_error"); }
});

app.patch("/api/my/listings/:id/sold", auth, async (req, res) => {
  const id = Number(req.params.id);
  if (!Number.isSafeInteger(id) || id < 1) return fail(res, 400, "invalid_listing_id");
  try {
    const result = await pool.query(
      "UPDATE listings SET status='sold',updated_at=NOW() WHERE id=$1 AND owner_id=$2 AND status='active' RETURNING id,status",
      [id, req.user.id]
    );
    if (!result.rowCount) return fail(res, 404, "listing_not_found_or_not_active");
    res.json({ item: result.rows[0] });
  } catch (e) { console.error(e.message); return fail(res, 500, "server_error"); }
});

app.post("/api/requests", auth, async (req, res) => {
  const b = req.body || {};
  const partName = cleanText(b.partName, 160);
  const referenceNumber = cleanText(b.referenceNumber, 100);
  const category = cleanText(b.category, 30);
  const wilaya = cleanText(b.wilaya, 80);
  const details = cleanText(b.details, 2000);
  if (partName.length < 2 || !categories.has(category) || wilaya.length < 2) return fail(res, 400, "invalid_request_fields");
  try {
    const result = await pool.query(
      "INSERT INTO purchase_requests(buyer_id,part_name,reference_number,category,wilaya,details) VALUES($1,$2,$3,$4,$5,$6) RETURNING *",
      [req.user.id,partName,referenceNumber,category,wilaya,details]
    );
    res.status(201).json({ request: result.rows[0] });
  } catch (e) { console.error(e.message); return fail(res, 500, "server_error"); }
});

app.get("/api/my/requests", auth, async (req, res) => {
  try {
    const result = await pool.query("SELECT * FROM purchase_requests WHERE buyer_id=$1 ORDER BY created_at DESC LIMIT 100", [req.user.id]);
    res.json({ items: result.rows });
  } catch (e) { console.error(e.message); return fail(res, 500, "server_error"); }
});

app.post("/api/listings/:id/report", auth, async (req, res) => {
  const id = Number(req.params.id);
  const reason = cleanText(req.body?.reason, 500);
  if (!Number.isSafeInteger(id) || id < 1 || reason.length < 5) return fail(res, 400, "invalid_report");
  try {
    const listing = await pool.query("SELECT id FROM listings WHERE id=$1 AND status='active'", [id]);
    if (!listing.rowCount) return fail(res, 404, "listing_not_found");
    await pool.query("INSERT INTO reports(reporter_id,listing_id,reason) VALUES($1,$2,$3)", [req.user.id,id,reason]);
    res.status(201).json({ ok: true });
  } catch (e) { console.error(e.message); return fail(res, 500, "server_error"); }
});

app.get("/api/admin/listings/pending", auth, admin, async (_req, res) => {
  try {
    const result = await pool.query(
      `SELECT l.*,u.name AS seller_name,u.phone AS seller_phone FROM listings l JOIN users u ON u.id=l.owner_id
       WHERE l.status='pending' ORDER BY l.created_at ASC LIMIT 200`
    );
    res.json({ items: result.rows });
  } catch (e) { console.error(e.message); return fail(res, 500, "server_error"); }
});

app.patch("/api/admin/listings/:id/status", auth, admin, async (req, res) => {
  const id = Number(req.params.id);
  const status = cleanText(req.body?.status, 20);
  if (!Number.isSafeInteger(id) || id < 1 || !["active","rejected"].includes(status)) return fail(res, 400, "invalid_moderation_action");
  try {
    const result = await pool.query(
      "UPDATE listings SET status=$1,updated_at=NOW() WHERE id=$2 AND status='pending' RETURNING id,status",
      [status,id]
    );
    if (!result.rowCount) return fail(res, 404, "pending_listing_not_found");
    res.json({ item: result.rows[0] });
  } catch (e) { console.error(e.message); return fail(res, 500, "server_error"); }
});

app.use((err, _req, res, _next) => {
  console.error("Unhandled request error:", err?.message || "unknown");
  if (res.headersSent) return;
  res.status(err?.type === "entity.too.large" ? 413 : 500).json({ error: err?.type === "entity.too.large" ? "payload_too_large" : "server_error" });
});

const server = app.listen(port, "0.0.0.0", () => console.log(`PièceDZ API listening on ${port}`));
async function shutdown() {
  server.close(async () => { await pool.end(); process.exit(0); });
  setTimeout(() => process.exit(1), 10000).unref();
}
process.on("SIGTERM", shutdown);
process.on("SIGINT", shutdown);
