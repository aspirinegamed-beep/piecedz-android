import express from "express";
import helmet from "helmet";
import pg from "pg";

const { Pool } = pg;
const app = express();
const port = Number(process.env.PORT || 8787);
if (!process.env.DATABASE_URL) {
  console.error("DATABASE_URL is required. Copy .env.example to .env and configure a PostgreSQL database.");
  process.exit(1);
}
const pool = new Pool({ connectionString: process.env.DATABASE_URL, ssl: process.env.PGSSLMODE === "require" ? { rejectUnauthorized: true } : undefined });
app.disable("x-powered-by");
app.use(helmet());
app.use(express.json({ limit: "100kb" }));

const allowedCategories = new Set(["سيارات","شاحنات","دراجات نارية","دراجات هوائية","هيدروليك"]);
const allowedSellerTypes = new Set(["محل قطع غيار","تاجر","فرد","ورشة صيانة"]);
const allowedConditions = new Set(["جديد","مستعمل","غير محدد"]);

app.get("/api/health", async (_req, res) => {
  try { await pool.query("SELECT 1"); res.json({ ok: true, service: "piecedz-api" }); }
  catch { res.status(503).json({ ok: false }); }
});

app.get("/api/listings", async (req, res) => {
  const q = String(req.query.q || "").trim().slice(0, 100);
  const category = String(req.query.category || "").trim();
  const wilaya = String(req.query.wilaya || "").trim().slice(0, 80);
  if (category && !allowedCategories.has(category)) return res.status(400).json({ error: "invalid_category" });
  try {
    const result = await pool.query(
      `SELECT id,name,reference_number,category,price_dzd,quantity,condition,wilaya,seller_name,seller_type,phone,description,created_at
       FROM listings WHERE status='active'
       AND ($1='' OR name ILIKE '%'||$1||'%' OR reference_number ILIKE '%'||$1||'%')
       AND ($2='' OR category=$2) AND ($3='' OR wilaya=$3)
       ORDER BY created_at DESC LIMIT 100`,
      [q, category, wilaya]
    );
    res.json({ items: result.rows });
  } catch (e) { console.error(e.message); res.status(500).json({ error: "server_error" }); }
});

/*
 * Public submission is intentionally not enabled until authentication, rate limiting,
 * phone verification, moderation/admin authentication, and abuse controls are added.
 * This avoids exposing an unmoderated write endpoint on a public server.
 */
app.post("/api/listings", (_req, res) => {
  res.status(501).json({ error: "submission_not_enabled", message: "Enable authenticated submissions and moderation before public launch." });
});

app.listen(port, "0.0.0.0", () => console.log(`PièceDZ API listening on ${port}`));
