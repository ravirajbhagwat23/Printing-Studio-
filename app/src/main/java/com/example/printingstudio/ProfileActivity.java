package com.example.printingstudio;

import android.content.Intent;
import android.content.SharedPreferences;
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

public class ProfileActivity extends AppCompatActivity {

    private static final String[] KEYS = {"name", "enroll", "mobile", "dept", "cls"};
    private static final String[] LABELS = {"Full name", "Enrollment number", "Mobile number", "Department", "Class"};

    private SharedPreferences prefs;
    private String email = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String uid = user != null ? user.getUid() : "guest";
        email = user != null && user.getEmail() != null ? user.getEmail() : "";
        prefs = getSharedPreferences("profile_" + uid, MODE_PRIVATE);

        findViewById(R.id.btnEditTop).setOnClickListener(v -> editProfile());
        findViewById(R.id.rowEdit).setOnClickListener(v -> editProfile());
        findViewById(R.id.rowPassword).setOnClickListener(v -> resetPassword());
        findViewById(R.id.rowHistory).setOnClickListener(v -> {
            Intent i = new Intent(this, OrderHistoryActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(i);
        });
        findViewById(R.id.rowHelp).setOnClickListener(v -> info("Help & support",
                "Visit the college print shop desk, or email printingstudio0711@gmail.com with your order number."));
        findViewById(R.id.rowPrivacy).setOnClickListener(v -> info("Privacy policy",
                "We store your email, your orders and the file name and page count of each order. Your files are not uploaded or stored."));
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
    protected void onResume() {
        super.onResume();
        BottomNav.setup(this, R.id.nav_profile);
        show();
    }

    private String get(String key) {
        return prefs.getString(key, "").trim();
    }

    private String orNot(String v) {
        return v.isEmpty() ? "Not provided" : v;
    }

    private void show() {
        String name = get("name");
        if (name.isEmpty() && email.contains("@")) name = email.split("@")[0];
        if (name.isEmpty()) name = "Student";
        ((TextView) findViewById(R.id.tvProfileName)).setText(name);
        ((TextView) findViewById(R.id.tvAvatar)).setText(name.substring(0, 1).toUpperCase());
        String en = get("enroll");
        ((TextView) findViewById(R.id.tvEnrollSub)).setText("Enrollment no: " + (en.isEmpty() ? "not set" : en));
        ((TextView) findViewById(R.id.pName)).setText(name);
        ((TextView) findViewById(R.id.pEnroll)).setText(orNot(en));
        ((TextView) findViewById(R.id.pEmail)).setText(email.isEmpty() ? "Not provided" : email);
        ((TextView) findViewById(R.id.pMobile)).setText(orNot(get("mobile")));
        ((TextView) findViewById(R.id.pDept)).setText(orNot(get("dept")));
        ((TextView) findViewById(R.id.pClass)).setText(orNot(get("cls")));
    }

    private void editProfile() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad / 2, pad, 0);
        final EditText[] fields = new EditText[KEYS.length];
        for (int i = 0; i < KEYS.length; i++) {
            EditText e = new EditText(this);
            e.setHint(LABELS[i]);
            e.setSingleLine(true);
            if (KEYS[i].equals("mobile")) e.setInputType(InputType.TYPE_CLASS_PHONE);
            e.setText(get(KEYS[i]));
            box.addView(e);
            fields[i] = e;
        }
        new AlertDialog.Builder(this)
                .setTitle("Edit profile")
                .setView(box)
                .setPositiveButton("Save", (d, w) -> {
                    SharedPreferences.Editor ed = prefs.edit();
                    for (int i = 0; i < KEYS.length; i++) {
                        ed.putString(KEYS[i], fields[i].getText().toString().trim());
                    }
                    ed.apply();
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
