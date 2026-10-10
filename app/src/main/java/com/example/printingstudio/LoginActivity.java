package com.example.printingstudio;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class LoginActivity extends AppCompatActivity {

    private static final String TERMS =
            "Terms and Conditions\n\n"
          + "1. Using the app\n"
          + "Printing Studio lets you send print orders to the college print shop. You must give a correct email address and keep your password private.\n\n"
          + "2. Your files\n"
          + "You may only upload files you have the right to print. The shop can refuse any file that is illegal, abusive or that breaks copyright. Your file is saved only so the shop can print your order.\n\n"
          + "3. Prices and orders\n"
          + "The price shown in the app is the price you pay. Black and white prints cost Rs. 2 per sheet and color prints cost Rs. 10 per sheet. Back to back printing uses one sheet for two pages. An order cannot be cancelled once printing has started.\n\n"
          + "4. Pickup and payment\n"
          + "Orders are collected from the college print shop when the status shows Ready. Payment is made at the shop.\n\n"
          + "5. Privacy\n"
          + "We store your email, your orders and the file of each order. We do not sell your data. The shop owner can see your email and orders to serve you.\n\n"
          + "6. Changes\n"
          + "These terms may change. Using the app after a change means you accept the new terms.\n\n"
          + "Questions? Email printingstudio0711@gmail.com.";

    private boolean signup = false;
    private boolean pwVisible = false;
    private boolean busy = false;

    private EditText etUsername, etEmail, etPassword, etConfirm;
    private CheckBox cbTerms;
    private TextView tvError, btnSubmit;
    private GoogleSignInClient googleClient;
    private ActivityResultLauncher<Intent> googleLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        BottomNav.padForNavBar(findViewById(android.R.id.content));

        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirm = findViewById(R.id.etConfirm);
        cbTerms = findViewById(R.id.cbTerms);
        tvError = findViewById(R.id.tvError);
        btnSubmit = findViewById(R.id.btnSubmit);

        findViewById(R.id.tabLogin).setOnClickListener(v -> setMode(false));
        findViewById(R.id.tabSignup).setOnClickListener(v -> setMode(true));
        findViewById(R.id.tvTerms).setOnClickListener(v -> showTerms());
        findViewById(R.id.btnForgot).setOnClickListener(v -> forgot());
        findViewById(R.id.btnShowPw).setOnClickListener(v -> togglePassword((TextView) v));
        btnSubmit.setOnClickListener(v -> submit());

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleClient = GoogleSignIn.getClient(this, gso);
        googleLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            try {
                GoogleSignInAccount acct = GoogleSignIn.getSignedInAccountFromIntent(result.getData())
                        .getResult(ApiException.class);
                AuthCredential cred = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
                FirebaseAuth.getInstance().signInWithCredential(cred).addOnCompleteListener(t -> {
                    setBusy(false);
                    if (t.isSuccessful()) {
                        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
                        enter(u != null ? u.getEmail() : acct.getEmail());
                    } else {
                        error("Google sign-in failed. Try again.");
                    }
                });
            } catch (ApiException e) {
                setBusy(false);
                error("Google sign-in cancelled or failed (code " + e.getStatusCode() + ").");
            }
        });
        findViewById(R.id.btnGoogle).setOnClickListener(v -> {
            if (busy) return;
            error(null);
            setBusy(true);
            googleClient.signOut().addOnCompleteListener(t -> googleLauncher.launch(googleClient.getSignInIntent()));
        });

        // Already signed in: go straight to the app.
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            enter(FirebaseAuth.getInstance().getCurrentUser().getEmail());
            return;
        }
        setMode(false);
    }

    private void setMode(boolean isSignup) {
        signup = isSignup;
        style((TextView) findViewById(R.id.tabLogin), !signup);
        style((TextView) findViewById(R.id.tabSignup), signup);
        ((TextView) findViewById(R.id.tvFormTitle)).setText(signup ? "Create your account" : "Welcome back");
        ((TextView) findViewById(R.id.tvFormSub)).setText(signup
                ? "It takes a minute. Then you can order prints."
                : "Log in to place and track your orders.");
        etUsername.setVisibility(signup ? View.VISIBLE : View.GONE);
        etConfirm.setVisibility(signup ? View.VISIBLE : View.GONE);
        findViewById(R.id.termsRow).setVisibility(signup ? View.VISIBLE : View.GONE);
        findViewById(R.id.btnForgot).setVisibility(signup ? View.GONE : View.VISIBLE);
        btnSubmit.setText(signup ? "Create account" : "Log in");
        error(null);
    }

    private void style(TextView v, boolean on) {
        v.setBackgroundResource(on ? R.drawable.ps_seg_on : R.drawable.ps_seg_off);
        v.setTextColor(on ? Color.WHITE : Color.parseColor("#10213F"));
    }

    private void error(String msg) {
        tvError.setText(msg == null ? "" : msg);
        tvError.setVisibility(msg == null ? View.GONE : View.VISIBLE);
    }

    private void togglePassword(TextView btn) {
        pwVisible = !pwVisible;
        int type = pwVisible
                ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD;
        etPassword.setInputType(type);
        etPassword.setSelection(etPassword.getText().length());
        btn.setText(pwVisible ? "Hide" : "Show");
    }

    private void showTerms() {
        new AlertDialog.Builder(this)
                .setMessage(TERMS)
                .setPositiveButton("I agree", (d, w) -> cbTerms.setChecked(true))
                .setNegativeButton("Close", null)
                .show();
    }

    private void forgot() {
        String email = etEmail.getText().toString().trim();
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            error("Enter your email above, then tap Forgot password.");
            return;
        }
        FirebaseAuth.getInstance().sendPasswordResetEmail(email).addOnCompleteListener(t ->
                Toast.makeText(this, t.isSuccessful()
                        ? "Reset link sent to " + email
                        : "Could not send the reset link. Check the email and try again.",
                        Toast.LENGTH_LONG).show());
    }

    private void submit() {
        if (busy) return;
        error(null);
        String email = etEmail.getText().toString().trim();
        String pw = etPassword.getText().toString();

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { error("Enter a valid email address."); return; }
        if (pw.length() < 6) { error("Password must be at least 6 characters."); return; }
        String username = etUsername.getText().toString().trim();
        if (signup) {
            if (username.length() < 2) { error("Enter your name (at least 2 letters)."); return; }
            if (!pw.equals(etConfirm.getText().toString())) { error("The two passwords do not match."); return; }
            if (!cbTerms.isChecked()) { error("Please accept the Terms and Conditions to continue."); return; }
        }

        setBusy(true);
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (signup) {
            auth.createUserWithEmailAndPassword(email, pw).addOnCompleteListener(t -> {
                if (!t.isSuccessful()) { done(false, t.getException(), email); return; }
                FirebaseUser u = auth.getCurrentUser();
                if (u == null) { done(true, null, email); return; }
                // Save the name on the account, then continue.
                u.updateProfile(new UserProfileChangeRequest.Builder().setDisplayName(username).build())
                        .addOnCompleteListener(x -> done(true, null, email));
            });
        } else {
            auth.signInWithEmailAndPassword(email, pw).addOnCompleteListener(t -> done(t.isSuccessful(), t.getException(), email));
        }
    }

    private void done(boolean ok, Exception e, String email) {
        setBusy(false);
        if (ok) { enter(email); return; }
        String msg;
        if (e instanceof FirebaseAuthWeakPasswordException) msg = "Choose a stronger password.";
        else if (e instanceof FirebaseAuthUserCollisionException) msg = "An account with this email already exists. Log in instead.";
        else if (e instanceof FirebaseAuthInvalidUserException) msg = "No account found for this email. Sign up first.";
        else if (e instanceof FirebaseAuthInvalidCredentialsException) msg = "Wrong email or password.";
        else msg = "Something went wrong. Check your internet and try again.";
        error(msg);
    }

    private void setBusy(boolean b) {
        busy = b;
        btnSubmit.setEnabled(!b);
        btnSubmit.setText(b ? "Please wait..." : (signup ? "Create account" : "Log in"));
    }

    private void enter(String email) {
        String e = email == null ? "" : email;
        OrderStore.get().userEmail = e;
        FirebaseUser fu = FirebaseAuth.getInstance().getCurrentUser();
        String dn = fu != null ? fu.getDisplayName() : null;
        OrderStore.get().userName = (dn != null && !dn.trim().isEmpty()) ? dn.trim()
                : (e.contains("@") ? e.substring(0, e.indexOf("@")) : "Student");
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }
}
