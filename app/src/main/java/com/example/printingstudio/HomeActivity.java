package com.example.printingstudio;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.graphics.pdf.PdfRenderer;
import android.provider.OpenableColumns;
import android.graphics.Color;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

public class HomeActivity extends AppCompatActivity {

    private ListenerRegistration trackListener;

    TextView tvFileName, tvCopies, tvPrice, tvBreakdown, tvSheets, tvGreeting;
    TextView btnBw, btnColor, btnNormal, btnUrgent, btnSingle, btnDouble, btnConfirm;

    private final ActivityResultLauncher<String[]> picker =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) handleFile(uri);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        OrderStore store = OrderStore.get();

        tvFileName = findViewById(R.id.tvFileName);
        tvCopies = findViewById(R.id.tvCopies);
        tvPrice = findViewById(R.id.tvPrice);
        tvBreakdown = findViewById(R.id.tvBreakdown);
        tvSheets = findViewById(R.id.tvSheets);
        tvGreeting = findViewById(R.id.tvGreeting);
        findViewById(R.id.btnEditPages).setOnClickListener(v -> editPages());
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u != null && u.getEmail() != null) {
            String n = u.getEmail().split("@")[0];
            tvGreeting.setText("Hi, " + n);
        }
        btnBw = findViewById(R.id.btnBw);
        btnColor = findViewById(R.id.btnColor);
        btnNormal = findViewById(R.id.btnNormal);
        btnUrgent = findViewById(R.id.btnUrgent);
        btnSingle = findViewById(R.id.btnSingle);
        btnDouble = findViewById(R.id.btnDouble);
        btnConfirm = findViewById(R.id.btnConfirm);

        findViewById(R.id.uploadBox).setOnClickListener(v -> chooseFile());
        findViewById(R.id.btnChooseFile).setOnClickListener(v -> chooseFile());

        btnBw.setOnClickListener(v -> setPrintType(Order.BW));
        btnColor.setOnClickListener(v -> setPrintType(Order.COLOR));
        btnNormal.setOnClickListener(v -> setSpeed(Order.NORMAL));
        btnUrgent.setOnClickListener(v -> setSpeed(Order.URGENT));
        btnSingle.setOnClickListener(v -> setSides("single"));
        btnDouble.setOnClickListener(v -> setSides("double"));

        findViewById(R.id.btnMinus).setOnClickListener(v -> {
            if (store.copies > 1) store.copies--;
            refresh();
        });
        findViewById(R.id.btnPlus).setOnClickListener(v -> {
            store.copies++;
            refresh();
        });

        btnConfirm.setOnClickListener(v -> {
            if (store.fileName == null || !btnConfirm.isEnabled()) return;
            store.submitOrder(getContentResolver());
            startActivity(new Intent(this, OrderConfirmedActivity.class));
        });

        refresh();
    }

    @Override
    protected void onStart() {
        super.onStart();
        startTracking();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (trackListener != null) {
            trackListener.remove();
            trackListener = null;
        }
    }

    private void startTracking() {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null) return;
        final android.view.View card = findViewById(R.id.trackCard);
        trackListener = FirebaseFirestore.getInstance().collection("orders")
                .whereEqualTo("userId", u.getUid())
                .addSnapshotListener((snap, err) -> {
                    if (snap == null) return;
                    DocumentSnapshot best = null;
                    Timestamp bestTime = null;
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        String st = d.getString("status");
                        if (st == null || st.trim().equals(Order.READY)) continue;
                        Timestamp t = d.getTimestamp("createdAt");
                        if (best == null || (t != null && (bestTime == null || t.compareTo(bestTime) > 0))) {
                            best = d;
                            bestTime = t;
                        }
                    }
                    if (best == null) {
                        card.setVisibility(android.view.View.GONE);
                        return;
                    }
                    final String id = best.getString("orderId") != null ? best.getString("orderId") : "";
                    final String file = best.getString("fileName") != null ? best.getString("fileName") : "";
                    final String status = best.getString("status").trim();
                    ((TextView) findViewById(R.id.tvTrackTitle)).setText("#" + id + " · " + file);
                    boolean printing = status.equals(Order.PRINTING);
                    findViewById(R.id.trk1).setBackgroundResource(R.drawable.ps_track_on);
                    findViewById(R.id.trk2).setBackgroundResource(printing ? R.drawable.ps_track_on : R.drawable.ps_track_off);
                    findViewById(R.id.trk3).setBackgroundResource(R.drawable.ps_track_off);
                    ((TextView) findViewById(R.id.tvTrackStatus)).setText(
                            printing ? "Your order is being printed" : "Order received. Waiting for the shop to start");
                    card.setVisibility(android.view.View.VISIBLE);

                    Long pg = best.getLong("pageCount");
                    Long cp = best.getLong("copies");
                    String ty = best.getString("printType");
                    String sp = best.getString("speed");
                    final Order o = new Order(id, file, pg != null ? pg.intValue() : 0,
                            ty != null ? ty : Order.BW, sp != null ? sp : Order.NORMAL,
                            cp != null ? cp.intValue() : 1);
                    o.status = status;
                    String sd = best.getString("sides");
                    if (sd != null) o.sides = sd;
                    card.setOnClickListener(v -> {
                        OrderStore.get().activeOrder = o;
                        startActivity(new Intent(this, OrderTrackingActivity.class));
                    });
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNav.setup(this, R.id.nav_order);
    }

    private void chooseFile() {
        picker.launch(new String[]{
                "application/pdf",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.ms-powerpoint",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                "text/plain",
                "image/*"});
    }

    private void handleFile(Uri uri) {
        String name = "document.pdf";
        long size = -1;
        Cursor c = null;
        try {
            c = getContentResolver().query(uri, null, null, null, null);
            if (c != null && c.moveToFirst()) {
                int i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (i >= 0) name = c.getString(i);
                int si = c.getColumnIndex(OpenableColumns.SIZE);
                if (si >= 0 && !c.isNull(si)) size = c.getLong(si);
            }
        } catch (Exception ignored) {
        } finally {
            if (c != null) c.close();
        }

        if (size > OrderStore.MAX_FILE_BYTES) {
            Toast.makeText(this, "File is too large. Maximum size is 100 MB.", Toast.LENGTH_LONG).show();
            return;
        }
        OrderStore.get().fileUri = uri;
        OrderStore.get().fileMime = getContentResolver().getType(uri);

        int pages = countPages(uri, name);
        boolean sure = pages > 0;
        if (!sure) pages = 1;
        OrderStore.get().fileName = name;
        OrderStore.get().pageCount = pages;
        refresh();
        if (!sure) {
            Toast.makeText(this, "Could not read the page count. Please check it with Edit pages.", Toast.LENGTH_LONG).show();
            editPages();
        }
    }

    // Returns page count, or -1 when it cannot be read.
    private int countPages(Uri uri, String name) {
        String n = name.toLowerCase();
        String type = getContentResolver().getType(uri);
        try {
            if (n.endsWith(".pdf") || "application/pdf".equals(type)) {
                ParcelFileDescriptor pfd = null;
                PdfRenderer renderer = null;
                try {
                    pfd = getContentResolver().openFileDescriptor(uri, "r");
                    renderer = new PdfRenderer(pfd);
                    return renderer.getPageCount();
                } finally {
                    if (renderer != null) renderer.close();
                    if (pfd != null) { try { pfd.close(); } catch (IOException ignored) {} }
                }
            }
            if ((type != null && type.startsWith("image/")) || n.matches(".*\\.(jpg|jpeg|png|webp|bmp|gif)$")) {
                return 1;
            }
            if (n.endsWith(".docx")) return readOfficeCount(uri, "Pages");
            if (n.endsWith(".pptx")) return readOfficeCount(uri, "Slides");
            if (n.endsWith(".txt") || "text/plain".equals(type)) {
                java.io.InputStream in = getContentResolver().openInputStream(uri);
                int lines = 0, b;
                boolean any = false;
                while (in != null && (b = in.read()) != -1) { any = true; if (b == '\n') lines++; }
                if (in != null) in.close();
                if (any) lines++;
                return Math.max(1, (lines + 44) / 45);
            }
        } catch (Exception ignored) {
        }
        return -1; // old .doc / .ppt and unknown types
    }

    // docx and pptx are zip files; the page or slide count is in docProps/app.xml
    private int readOfficeCount(Uri uri, String tag) throws IOException {
        java.io.InputStream raw = getContentResolver().openInputStream(uri);
        if (raw == null) return -1;
        java.util.zip.ZipInputStream zin = new java.util.zip.ZipInputStream(raw);
        try {
            java.util.zip.ZipEntry e;
            while ((e = zin.getNextEntry()) != null) {
                if (e.getName().equals("docProps/app.xml")) {
                    java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
                    byte[] buf = new byte[4096];
                    int r;
                    while ((r = zin.read(buf)) != -1) bo.write(buf, 0, r);
                    java.util.regex.Matcher m = java.util.regex.Pattern
                            .compile("<" + tag + ">(\\d+)</" + tag + ">").matcher(bo.toString("UTF-8"));
                    if (m.find()) {
                        int v = Integer.parseInt(m.group(1));
                        return v > 0 ? v : -1;
                    }
                    return -1;
                }
            }
        } finally {
            zin.close();
        }
        return -1;
    }

    private void editPages() {
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(OrderStore.get().pageCount));
        input.setSelection(input.getText().length());
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Number of pages")
                .setView(input)
                .setPositiveButton("Save", (d, w) -> {
                    try {
                        int v = Integer.parseInt(input.getText().toString().trim());
                        if (v >= 1 && v <= 5000) {
                            OrderStore.get().pageCount = v;
                            refresh();
                        }
                    } catch (NumberFormatException ignored) {}
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setPrintType(String type) {
        OrderStore.get().printType = type;
        refresh();
    }

    private void setSpeed(String speed) {
        OrderStore.get().speed = speed;
        refresh();
    }

    private void setSides(String sides) {
        OrderStore.get().sides = sides;
        refresh();
    }

    private void refresh() {
        OrderStore store = OrderStore.get();
        tvFileName.setText(store.fileName == null ? "Tap to upload file" : store.fileName + " (" + store.pageCount + " pages)");
        tvCopies.setText(String.valueOf(store.copies));
        tvPrice.setText("Estimated price: ₹" + (int) store.estimatedPrice());

        style(btnBw, store.printType.equals(Order.BW));
        style(btnColor, store.printType.equals(Order.COLOR));
        style(btnNormal, store.speed.equals(Order.NORMAL));
        style(btnUrgent, store.speed.equals(Order.URGENT));
        style(btnSingle, store.sides.equals("single"));
        style(btnDouble, store.sides.equals("double"));

        findViewById(R.id.btnEditPages).setVisibility(store.fileName == null ? android.view.View.GONE : android.view.View.VISIBLE);
        if (store.fileName == null) {
            tvBreakdown.setText("Upload a PDF to see the price");
            tvSheets.setText("");
        } else {
            int rate = store.printType.equals(Order.COLOR) ? 10 : 2;
            int sheetsPerCopy = store.sides.equals("double") ? (store.pageCount + 1) / 2 : store.pageCount;
            String b = sheetsPerCopy + " sheets × " + store.copies + " copies × ₹" + rate + " per sheet";
            if (store.speed.equals(Order.URGENT)) b += " × 2 urgent";
            tvBreakdown.setText(b);
            tvSheets.setText(store.sides.equals("double")
                    ? store.pageCount + " pages printed back to back (2 pages per sheet)"
                    : store.pageCount + " pages printed on one side (1 page per sheet)");
        }

        btnConfirm.setEnabled(store.fileName != null);
    }

    private void style(TextView v, boolean on) {
        v.setBackgroundResource(on ? R.drawable.ps_seg_on : R.drawable.ps_seg_off);
        v.setTextColor(on ? Color.WHITE : Color.parseColor("#10213F"));
    }
}
