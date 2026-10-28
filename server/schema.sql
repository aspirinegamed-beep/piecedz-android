CREATE TABLE IF NOT EXISTS listings (
  id BIGSERIAL PRIMARY KEY,
  name TEXT NOT NULL CHECK (char_length(name) BETWEEN 2 AND 160),
  reference_number TEXT NOT NULL DEFAULT '' CHECK (char_length(reference_number) <= 100),
  category TEXT NOT NULL CHECK (category IN ('سيارات','شاحنات','دراجات نارية','دراجات هوائية','هيدروليك')),
  price_dzd NUMERIC(14,2),
  quantity INTEGER NOT NULL DEFAULT 1 CHECK (quantity BETWEEN 1 AND 1000000),
  condition TEXT NOT NULL DEFAULT 'غير محدد' CHECK (condition IN ('جديد','مستعمل','غير محدد')),
  wilaya TEXT NOT NULL CHECK (char_length(wilaya) BETWEEN 2 AND 80),
  seller_name TEXT NOT NULL CHECK (char_length(seller_name) BETWEEN 2 AND 120),
  seller_type TEXT NOT NULL CHECK (seller_type IN ('محل قطع غيار','تاجر','فرد','ورشة صيانة')),
  phone TEXT NOT NULL CHECK (char_length(phone) BETWEEN 6 AND 30),
  description TEXT NOT NULL DEFAULT '' CHECK (char_length(description) <= 2000),
  status TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending','active','rejected','sold')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS listings_active_created_idx ON listings(status, created_at DESC);
CREATE INDEX IF NOT EXISTS listings_category_wilaya_idx ON listings(category, wilaya);
