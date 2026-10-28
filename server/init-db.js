import fs from "node:fs";
import pg from "pg";
const { Pool } = pg;
if (!process.env.DATABASE_URL) {
  console.error("Set DATABASE_URL before initializing the database.");
  process.exit(1);
}
const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
  ssl: process.env.PGSSLMODE === "require" ? { rejectUnauthorized: true } : undefined
});
try {
  const schema = fs.readFileSync(new URL("./schema.sql", import.meta.url), "utf8");
  await pool.query(schema);
  console.log("PièceDZ schema initialized.");
} finally {
  await pool.end();
}
