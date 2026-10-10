package com.example.printingstudio;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {

    private static final String SHOP_EMAIL = "printingstudio0711@gmail.com";

    private SharedPreferences prefs;
    private String email = "";
    private ListenerRegistration statsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String uid = user != null ? user.getUid() : "guest";
        email = user != null && user.getEmail() != null ? user.getEmail() : "";
        prefs = getSharedPreferences("profile_" + uid, MODE_PRIVATE);

        if (user != null && user.getMetadata() != null) {
            String since = new SimpleDateFormat("MMM yyyy", Locale.getDefault())
                    .format(new Date(user.getMetadata().getCreationTimestamp()));
            ((TextView) findViewById(R.id.tvSince)).setText("Member since " + since);
        }

        findViewById(R.id.btnEditTop).setOnClickListener(v -> editProfile());
        findViewById(R.id.rowEdit).setOnClickListener(v -> editProfile());
        findViewById(R.id.rowPassword).setOnClickListener(v -> resetPassword());
        findViewById(R.id.rowHistory).setOnClickListener(v -> {
            Intent i = new Intent(this, OrderHistoryActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(i);
        });
        findViewById(R.id.rowFeedback).setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + SHOP_EMAIL));
            i.putExtra(Intent.EXTRA_SUBJECT, "Printing Studio feedback");
            try { startActivity(i); }
            catch (Exception e) { Toast.makeText(this, "No email app found.", Toast.LENGTH_SHORT).show(); }
        });
        findViewById(R.id.rowShare).setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_TEXT, "Order prints from the college print shop on your phone with Printing Studio.");
            startActivity(Intent.createChooser(i, "Share the app"));
        });
        findViewById(R.id.rowHelp).setOnClickListener(v -> info("Help & support",
                "Visit the college print shop desk, or email " + SHOP_EMAIL + " with your order number."));
        findViewById(R.id.rowPrivacy).setOnClickListener(v -> info("Privacy policy",
                "We store your email, your orders, and each order's file. The shop uses the file only to print your order."));
        findViewById(R.id.rowAbout).setOnClickListener(v -> info("About app",
                "Printing Studio v1.0\nOnline print ordering for the college print shop."));

        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            OrderStore.get().logout();
            Intent i = new Intent(this, LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null) return;
        statsListener = FirebaseFirestore.getInstance().collection("orders")
                .whereEqualTo("userId", u.getUid())
                .addSnapshotListener((snap, err) -> {
                    if (snap == null) return;
                    int orders = 0;
                    long pages = 0;
                    double spent = 0;
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        orders++;
                        Long pg = d.getLong("pageCount");
                        Long cp = d.getLong("copies");
                        Double pr = d.getDouble("price");
                        if (pg != null) pages += pg * (cp != null ? cp : 1);
                        if (pr != null) spent += pr;
                    }
                    ((TextView) findViewById(R.id.stOrders)).setText(String.valueOf(orders));
                    ((TextView) findViewById(R.id.stPages)).setText(String.valueOf(pages));
                    ((TextView) findViewById(R.id.stSpent)).setText("₹" + Math.round(spent));
                });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (statsListener != null) {
            statsListener.remove();
            statsListener = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNav.setup(this, R.id.nav_profile);
        show();
    }

    private String get(String key) {
        return prefs.getString(key, "").trim();
    }

    private void show() {
        String name = get("name");
        FirebaseUser cu = FirebaseAuth.getInstance().getCurrentUser();
        if (name.isEmpty() && cu != null && cu.getDisplayName() != null) name = cu.getDisplayName().trim();
        if (name.isEmpty() && email.contains("@")) name = email.split("@")[0];
        if (name.isEmpty()) name = "Student";
        String mobile = get("mobile");
        ((TextView) findViewById(R.id.tvProfileName)).setText(name);
        ((TextView) findViewById(R.id.tvAvatar)).setText(name.substring(0, 1).toUpperCase());
        ((TextView) findViewById(R.id.tvProfileEmail)).setText(email);
        ((TextView) findViewById(R.id.pName)).setText(name);
        ((TextView) findViewById(R.id.pEmail)).setText(email.isEmpty() ? "Not provided" : email);
        ((TextView) findViewById(R.id.pMobile)).setText(mobile.isEmpty() ? "Not provided" : mobile);
    }

    private void editProfile() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad / 2, pad, 0);

        final EditText name = new EditText(this);
        name.setHint("Full name");
        name.setSingleLine(true);
        String cur = get("name");
        FirebaseUser cu0 = FirebaseAuth.getInstance().getCurrentUser();
        if (cur.isEmpty() && cu0 != null && cu0.getDisplayName() != null) cur = cu0.getDisplayName();
        name.setText(cur);
        box.addView(name);

        final EditText mobile = new EditText(this);
        mobile.setHint("Mobile number (10 digits)");
        mobile.setSingleLine(true);
        mobile.setInputType(InputType.TYPE_CLASS_PHONE);
        mobile.setText(get("mobile"));
        box.addView(mobile);

        new AlertDialog.Builder(this)
                .setTitle("Edit profile")
                .setView(box)
                .setPositiveButton("Save", (d, w) -> {
                    String m = mobile.getText().toString().trim();
                    if (!m.isEmpty() && !m.matches("\\d{10}")) {
                        Toast.makeText(this, "Enter a 10 digit mobile number.", Toast.LENGTH_LONG).show();
                        return;

                    }String nm = name.getText().toString().trim();
                    prefs.edit().putString("name", nm).putString("mobile", m).apply();
                    FirebaseUser cu = FirebaseAuth.getInstance().getCurrentUser();
                    if (cu != null && !nm.isEmpty()) {
                        cu.updateProfile(new com.google.firebase.auth.UserProfileChangeRequest.Builder().setDisplayName(nm).build());
                    }
                    show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void resetPassword() {
        if (email.isEmpty()) return;
        FirebaseAuth.getInstance().sendPasswordResetEmail(email).addOnCompleteListener(t ->
                Toast.makeText(this, t.isSuccessful()
                        ? "Reset link sent to " + email
                        : "Could not send the reset link. Try again.", Toast.LENGTH_LONG).show());
    }

    private void info(String title, String msg) {
        new AlertDialog.Builder(this).setTitle(title).setMessage(msg)
                .setPositiveButton("OK", null).show();
    }
}
