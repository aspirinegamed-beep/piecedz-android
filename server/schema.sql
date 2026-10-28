CREATE TABLE IF NOT EXISTS users (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(120) NOT NULL,
  phone VARCHAR(30) NOT NULL UNIQUE,
  whatsapp VARCHAR(30) NOT NULL DEFAULT '',
  facebook_url VARCHAR(500) NOT NULL DEFAULT '',
  shop_address VARCHAR(300) NOT NULL DEFAULT '',
  latitude DOUBLE PRECISION,
  longitude DOUBLE PRECISION,
  password_hash TEXT NOT NULL,
  user_type VARCHAR(30) NOT NULL CHECK (user_type IN ('محل قطع غيار','تاجر','فرد','ورشة صيانة')),
  role VARCHAR(20) NOT NULL DEFAULT 'seller' CHECK (role IN ('seller','admin')),
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS listings (
  id BIGSERIAL PRIMARY KEY,
  owner_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  name VARCHAR(160) NOT NULL,
  reference_number VARCHAR(100) NOT NULL DEFAULT '',
  category VARCHAR(30) NOT NULL CHECK (category IN ('سيارات','شاحنات','دراجات نارية','دراجات هوائية','هيدروليك')),
  price_dzd NUMERIC(14,2),
  quantity INTEGER NOT NULL DEFAULT 1 CHECK (quantity BETWEEN 1 AND 1000000),
  condition VARCHAR(20) NOT NULL DEFAULT 'غير محدد' CHECK (condition IN ('جديد','مستعمل','غير محدد')),
  wilaya VARCHAR(80) NOT NULL,
  description VARCHAR(2000) NOT NULL DEFAULT '',
  status VARCHAR(20) NOT NULL DEFAULT 'pending' CHECK (status IN ('pending','active','rejected','sold')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS listings_status_created_idx ON listings(status, created_at DESC);
CREATE INDEX IF NOT EXISTS listings_category_wilaya_idx ON listings(category, wilaya);
CREATE INDEX IF NOT EXISTS listings_owner_idx ON listings(owner_id);

CREATE TABLE IF NOT EXISTS purchase_requests (
  id BIGSERIAL PRIMARY KEY,
  buyer_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  listing_id BIGINT REFERENCES listings(id) ON DELETE SET NULL,
  part_name VARCHAR(160) NOT NULL,
  reference_number VARCHAR(100) NOT NULL DEFAULT '',
  category VARCHAR(30) NOT NULL CHECK (category IN ('سيارات','شاحنات','دراجات نارية','دراجات هوائية','هيدروليك')),
  wilaya VARCHAR(80) NOT NULL,
  details VARCHAR(2000) NOT NULL DEFAULT '',
  status VARCHAR(20) NOT NULL DEFAULT 'open' CHECK (status IN ('open','matched','closed')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS purchase_requests_open_idx ON purchase_requests(status, created_at DESC);

CREATE TABLE IF NOT EXISTS reports (
  id BIGSERIAL PRIMARY KEY,
  reporter_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  listing_id BIGINT NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
  reason VARCHAR(500) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'open' CHECK (status IN ('open','reviewed','dismissed')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


-- ترقية آمنة لقواعد البيانات الموجودة مسبقًا.
ALTER TABLE users ADD COLUMN IF NOT EXISTS whatsapp VARCHAR(30) NOT NULL DEFAULT '';
ALTER TABLE users ADD COLUMN IF NOT EXISTS facebook_url VARCHAR(500) NOT NULL DEFAULT '';
ALTER TABLE users ADD COLUMN IF NOT EXISTS shop_address VARCHAR(300) NOT NULL DEFAULT '';
ALTER TABLE users ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION;
ALTER TABLE users ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION;
