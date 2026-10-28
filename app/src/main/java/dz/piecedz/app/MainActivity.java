package dz.piecedz.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Locale;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class MainActivity extends Activity {
    private final String[] categories = {
        "الكل", "سيارات", "شاحنات", "دراجات نارية", "دراجات هوائية", "هيدروليك"
    };
    private LinearLayout root, listBox, topBar;
    private EditText searchBox;
    private String activeCategory = "الكل";
    private SharedPreferences prefs;
    private JSONArray listings;
    private EditText barcodeTarget;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(0xFF102A43);
        getWindow().setNavigationBarColor(0xFF102A43);
        prefs = getSharedPreferences("piecedz", MODE_PRIVATE);
        listings = loadListings();
        buildUi();
    }

    private int dp(float n) { return (int)(n * getResources().getDisplayMetrics().density); }

    private TextView text(String s, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setGravity(Gravity.RIGHT);
        t.setPadding(dp(4), dp(5), dp(4), dp(5));
        return t;
    }

    private GradientDrawableBox shape(int color, int radius) {
        return new GradientDrawableBox(color, dp(radius));
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setPadding(dp(16), dp(12), dp(16), dp(20));
        root.setBackgroundColor(0xFFF4F7FA);
        scroll.addView(root);
        setContentView(scroll);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(18), dp(18), dp(18), dp(18));
        header.setBackground(shape(0xFF102A43, 20).drawable);
        TextView brand = text("PièceDZ", 27, 0xFFFFFFFF, true);
        brand.setGravity(Gravity.RIGHT);
        header.addView(brand);
        TextView subtitle = text("منصتك الجزائرية لقطع الغيار", 15, 0xFFD9EAF7, false);
        header.addView(subtitle);
        root.addView(header);

        // PièceDZ animated engine showcase
        EngineAnimationView engineView = new EngineAnimationView(this);
        LinearLayout.LayoutParams engineParams =
                new LinearLayout.LayoutParams(-1, dp(190));
        engineParams.setMargins(0, dp(12), 0, dp(12));
        root.addView(engineView, engineParams);


        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleRow.setPadding(0, dp(16), 0, dp(6));
        TextView title = text("ابحث عن القطعة اللي تحتاجها", 19, 0xFF102A43, true);
        titleRow.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        Button add = button("+ أضف عرض", 0xFFF97316);
        add.setOnClickListener(v -> showAddDialog());
        titleRow.addView(add);
        root.addView(titleRow);

        searchBox = new EditText(this);
        searchBox.setSingleLine(true);
        searchBox.setTextSize(15);
        searchBox.setHint("اسم القطعة أو رقمها المرجعي أو الولاية");
        searchBox.setInputType(InputType.TYPE_CLASS_TEXT);
        searchBox.setPadding(dp(14), dp(10), dp(14), dp(10));
        searchBox.setBackground(shape(0xFFFFFFFF, 14).drawable);
        root.addView(searchBox, new LinearLayout.LayoutParams(-1, dp(52)));
        searchBox.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s,int st,int c,int a){}
            public void onTextChanged(CharSequence s,int st,int before,int count){ renderListings(); }
            public void afterTextChanged(android.text.Editable e){}
        });

        Button scanSearch = button("▦ مسح باركود للبحث", 0xFFF97316);
        LinearLayout.LayoutParams scanParams = new LinearLayout.LayoutParams(-1, dp(46));
        scanParams.setMargins(0, dp(8), 0, dp(4));
        root.addView(scanSearch, scanParams);
        scanSearch.setOnClickListener(v -> startBarcodeScan(searchBox));

        TextView catTitle = text("الأقسام", 18, 0xFF102A43, true);
        catTitle.setPadding(0, dp(16), 0, dp(6)); root.addView(catTitle);
        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout catRow = new LinearLayout(this);
        catRow.setOrientation(LinearLayout.HORIZONTAL);
        for (String c : categories) {
            Button b = button(c, c.equals(activeCategory) ? 0xFFF97316 : 0xFFE1EAF0);
            b.setTextColor(c.equals(activeCategory) ? 0xFFFFFFFF : 0xFF102A43);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-2, dp(42));
            p.setMargins(dp(3), dp(2), dp(3), dp(2));
            catRow.addView(b, p);
            b.setOnClickListener(v -> { activeCategory = c; buildUi(); });
        }
        hsv.addView(catRow); root.addView(hsv);

        TextView listingTitle = text("أحدث العروض", 19, 0xFF102A43, true);
        listingTitle.setPadding(0, dp(15), 0, dp(4)); root.addView(listingTitle);
        listBox = new LinearLayout(this); listBox.setOrientation(LinearLayout.VERTICAL);
        root.addView(listBox);
        TextView note = text("نسخة تجريبية: العروض محفوظة على هذا الهاتف فقط. يلزم ربط خادم حتى تظهر العروض لجميع المستخدمين.", 12, 0xFF526777, false);
        note.setPadding(dp(4), dp(18), dp(4), dp(8));
        root.addView(note);
        renderListings();
    }

    private Button button(String label, int color) {
        Button b = new Button(this);
        b.setText(label); b.setTextSize(13); b.setAllCaps(false);
        b.setTextColor(color == 0xFFE1EAF0 ? 0xFF102A43 : 0xFFFFFFFF);
        b.setBackground(shape(color, 12).drawable);
        b.setPadding(dp(10), 0, dp(10), 0);
        return b;
    }

    private void renderListings() {
        if (listBox == null) return;
        listBox.removeAllViews();
        String q = searchBox == null ? "" : searchBox.getText().toString().trim().toLowerCase(Locale.ROOT);
        int shown = 0;
        for (int i = listings.length()-1; i >= 0; i--) {
            JSONObject o = listings.optJSONObject(i); if (o == null) continue;
            String cat = o.optString("category", "");
            String searchable = (o.optString("name","") + " " + o.optString("ref","") + " " + o.optString("city","") + " " + cat).toLowerCase(Locale.ROOT);
            if (!activeCategory.equals("الكل") && !activeCategory.equals(cat)) continue;
            if (!searchable.contains(q)) continue;
            listBox.addView(listingCard(o));
            shown++;
        }
        if (shown == 0) {
            TextView empty = text("ما لقيناش عروض مطابقة.\nتقدر تضيف عرض جديد أو تطلب القطعة من البائعين.", 15, 0xFF526777, false);
            empty.setPadding(dp(12), dp(20), dp(12), dp(20));
            listBox.addView(empty);
        }
    }

    private View listingCard(JSONObject o) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(10), dp(14), dp(10));
        card.setBackground(shape(0xFFFFFFFF, 16).drawable);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
        cp.setMargins(0, dp(5), 0, dp(7)); card.setLayoutParams(cp);

        TextView name = text(o.optString("name","قطعة غيار"), 17, 0xFF102A43, true);
        card.addView(name);
        card.addView(text(o.optString("category","") + " • " + o.optString("city","الجزائر"), 12, 0xFF526777, false));
        card.addView(text("السعر: " + o.optString("price","حسب الاتفاق") + " دج", 15, 0xFFF97316, true));
        if (!o.optString("ref","").isEmpty()) card.addView(text("رقم القطعة: " + o.optString("ref",""), 12, 0xFF526777, false));
        card.addView(text("الحالة: " + o.optString("condition","جديد") + " • الكمية: " + o.optString("quantity","1"), 12, 0xFF526777, false));
        card.addView(text("البائع: " + o.optString("seller","") + " • " + o.optString("sellerType",""), 13, 0xFF102A43, false));

        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.RIGHT);
        Button call = button("اتصال بالبائع", 0xFFF97316);
        Button share = button("مشاركة", 0xFF526777);
        Button whatsapp = button("واتساب", 0xFF168A52);
        Button facebook = button("فيسبوك", 0xFF1877F2);
        Button maps = button("موقع المحل", 0xFF102A43);
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(-2, dp(42)); ap.setMargins(dp(4), dp(8), 0, 0);
        actions.addView(call, ap); actions.addView(share, ap);
        actions.addView(whatsapp, ap); actions.addView(facebook, ap); actions.addView(maps, ap);
        call.setOnClickListener(v -> {
            String phone = o.optString("phone","");
            if (phone.trim().isEmpty()) { toast("رقم الهاتف غير متوفر"); return; }
            try { startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone))); }
            catch (Exception e) { toast("تعذر فتح الاتصال"); }
        });
        share.setOnClickListener(v -> {
            String s = "PièceDZ — عرض قطعة غيار\n" + o.optString("name","") + "\nالفئة: " + o.optString("category","") +
                "\nالسعر: " + o.optString("price","") + " دج\nالولاية: " + o.optString("city","") +
                "\nرقم البائع: " + o.optString("phone","");
            Intent i = new Intent(Intent.ACTION_SEND); i.setType("text/plain"); i.putExtra(Intent.EXTRA_TEXT, s);
            startActivity(Intent.createChooser(i, "مشاركة العرض"));
        });
        whatsapp.setOnClickListener(v -> {
            String phone = o.optString("whatsapp", o.optString("phone", "")).trim();
            String digits = phone.replaceAll("[^0-9]", "");
            if (digits.startsWith("0")) digits = "213" + digits.substring(1);
            if (digits.isEmpty()) { toast("رقم واتساب غير متوفر"); return; }
            try {
                startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://wa.me/" + digits)));
            } catch (Exception e) { toast("تعذر فتح واتساب"); }
        });
        facebook.setOnClickListener(v -> {
            String url = o.optString("facebook", "").trim();
            if (!url.matches("(?i)^https://(www\\.)?facebook\\.com/.*")) {
                toast("رابط صفحة فيسبوك غير متوفر"); return;
            }
            openExternal(url);
        });
        maps.setOnClickListener(v -> {
            String address = o.optString("address", "").trim();
            String lat = o.optString("latitude", "");
            String lon = o.optString("longitude", "");
            String query = !lat.isEmpty() && !lon.isEmpty()
                ? lat + "," + lon : address;
            if (query.isEmpty()) query = o.optString("city", "");
            if (query.trim().isEmpty()) { toast("موقع المحل غير متوفر"); return; }
            openExternal("https://www.google.com/maps/search/?api=1&query="
                + Uri.encode(query));
        });
        card.addView(actions);
        return card;
    }

    private void openExternal(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) { toast("تعذر فتح الرابط"); }
    }

    private void startBarcodeScan(EditText target) {
        barcodeTarget = target;
        try {
            IntentIntegrator scanner = new IntentIntegrator(this);
            scanner.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
            scanner.setPrompt("وجّه الكاميرا نحو باركود القطعة");
            scanner.setBeepEnabled(false);
            scanner.setOrientationLocked(false);
            scanner.initiateScan();
        } catch (Exception e) {
            toast("تعذر تشغيل ماسح الباركود");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() != null) {
                EditText target = barcodeTarget;
                if (target != null) {
                    target.setText(result.getContents());
                    target.setSelection(target.getText().length());
                    toast("تمت قراءة الباركود");
                }
            } else {
                toast("تم إلغاء المسح");
            }
            barcodeTarget = null;
        }
    }

    private void showAddDialog() {
        ScrollView sc = new ScrollView(this);
        LinearLayout form = new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(18), dp(8), dp(18), dp(8));
        sc.addView(form);
        EditText name = field(form, "اسم القطعة *");
        EditText ref = field(form, "رقم القطعة أو الباركود (اختياري)");
        Button scanRef = button("▦ مسح باركود القطعة", 0xFFF97316);
        form.addView(scanRef, new LinearLayout.LayoutParams(-1, dp(44)));
        scanRef.setOnClickListener(v -> startBarcodeScan(ref));
        Spinner cat = new Spinner(this);
        ArrayAdapter<String> ca = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
            new String[]{"سيارات","شاحنات","دراجات نارية","دراجات هوائية","هيدروليك"});
        cat.setAdapter(ca); form.addView(cat);
        EditText price = field(form, "السعر بالدينار الجزائري");
        EditText qty = field(form, "الكمية");
        EditText city = field(form, "الولاية");
        EditText condition = field(form, "الحالة: جديد أو مستعمل");
        EditText seller = field(form, "اسم البائع أو المحل");
        Spinner sellerType = new Spinner(this);
        sellerType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
            new String[]{"محل قطع غيار","تاجر","فرد","ورشة صيانة"}));
        form.addView(sellerType);
        EditText phone = field(form, "رقم الهاتف");
        EditText whatsapp = field(form, "رقم واتساب (اختياري)");
        EditText facebook = field(form, "رابط صفحة فيسبوك https://...");
        EditText address = field(form, "عنوان المحل");
        EditText latitude = field(form, "خط العرض Latitude (اختياري)");
        EditText longitude = field(form, "خط الطول Longitude (اختياري)");
        new AlertDialog.Builder(this).setTitle("إضافة عرض قطعة غيار").setView(sc)
            .setNegativeButton("إلغاء", (d,w)->{})
            .setPositiveButton("حفظ العرض", (d,w)-> {
                if (name.getText().toString().trim().isEmpty() || city.getText().toString().trim().isEmpty()) {
                    toast("اسم القطعة والولاية مطلوبان"); return;
                }
                JSONObject o = new JSONObject();
                try {
                    o.put("id", System.currentTimeMillis());
                    o.put("name", name.getText().toString().trim());
                    o.put("ref", ref.getText().toString().trim());
                    o.put("category", cat.getSelectedItem().toString());
                    o.put("price", price.getText().toString().trim().isEmpty() ? "حسب الاتفاق" : price.getText().toString().trim());
                    o.put("quantity", qty.getText().toString().trim().isEmpty() ? "1" : qty.getText().toString().trim());
                    o.put("city", city.getText().toString().trim());
                    o.put("condition", condition.getText().toString().trim().isEmpty() ? "غير محدد" : condition.getText().toString().trim());
                    o.put("seller", seller.getText().toString().trim().isEmpty() ? "بائع" : seller.getText().toString().trim());
                    o.put("sellerType", sellerType.getSelectedItem().toString());
                    o.put("phone", phone.getText().toString().trim());
                    o.put("whatsapp", whatsapp.getText().toString().trim().isEmpty() ? phone.getText().toString().trim() : whatsapp.getText().toString().trim());
                    o.put("facebook", facebook.getText().toString().trim());
                    o.put("address", address.getText().toString().trim());
                    o.put("latitude", latitude.getText().toString().trim());
                    o.put("longitude", longitude.getText().toString().trim());
                    listings.put(o); saveListings(); buildUi(); toast("تم حفظ العرض على هذا الهاتف");
                } catch (Exception e) { toast("تعذر حفظ العرض"); }
            }).show();
    }

    private EditText field(LinearLayout parent, String hint) {
        EditText e = new EditText(this); e.setHint(hint); e.setTextSize(14);
        e.setSingleLine(true); e.setPadding(dp(10), dp(4), dp(10), dp(4));
        parent.addView(e, new LinearLayout.LayoutParams(-1, dp(48)));
        return e;
    }

    private JSONArray loadListings() {
        try {
            String raw = prefs.getString("listings", null);
            if (raw != null) return new JSONArray(raw);
        } catch (Exception ignored) {}
        JSONArray seed = new JSONArray();
        try {
            seed.put(new JSONObject().put("id",1).put("name","فلتر زيت — مثال تجريبي").put("ref","DEMO-001")
                .put("category","سيارات").put("price","1800").put("quantity","3").put("city","المسيلة")
                .put("condition","جديد").put("seller","عرض توضيحي").put("sellerType","محل قطع غيار").put("phone",""));
            seed.put(new JSONObject().put("id",2).put("name","مضخة هيدروليكية — مثال تجريبي").put("ref","DEMO-HYD-02")
                .put("category","هيدروليك").put("price","حسب الاتفاق").put("quantity","1").put("city","الجزائر")
                .put("condition","مستعمل").put("seller","عرض توضيحي").put("sellerType","ورشة صيانة").put("phone",""));
        } catch (Exception ignored) {}
        return seed;
    }

    private void saveListings() { prefs.edit().putString("listings", listings.toString()).apply(); }
    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }

    private static class GradientDrawableBox {
        android.graphics.drawable.GradientDrawable drawable;
        GradientDrawableBox(int color, int radius) {
            drawable = new android.graphics.drawable.GradientDrawable();
            drawable.setColor(color); drawable.setCornerRadius(radius);
        }
    }
}
