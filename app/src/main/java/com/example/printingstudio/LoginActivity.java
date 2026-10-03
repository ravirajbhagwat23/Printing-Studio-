package com.example.printingstudio;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private EditText etEmail, etPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();

        // Already logged in? Skip this screen.
        FirebaseUser current = auth.getCurrentUser();
        if (current != null) {
            saveUser(current);
            goHome();
            return;
        }

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        Button btnLogin = findViewById(R.id.btnEmailLogin);
        Button btnSignup = findViewById(R.id.btnSignup);
        Button btnGoogle = findViewById(R.id.btnGoogleLogin);
        TextView tvForgot = findViewById(R.id.tvForgot);

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String pw = etPassword.getText().toString();
            if (!valid(email, pw)) return;
            auth.signInWithEmailAndPassword(email, pw).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    saveUser(auth.getCurrentUser());
                    goHome();
                } else {
                    toast(task.getException() != null ? task.getException().getMessage() : "Login failed");
                }
            });
        });

        btnSignup.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String pw = etPassword.getText().toString();
            if (!valid(email, pw)) return;
            auth.createUserWithEmailAndPassword(email, pw).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    saveUser(auth.getCurrentUser());
                    toast("Account created");
                    goHome();
                } else {
                    toast(task.getException() != null ? task.getException().getMessage() : "Sign up failed");
                }
            });
        });

        tvForgot.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            if (email.isEmpty()) {
                toast("Type your email first, then tap Forgot password");
                return;
            }
            auth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    toast("Reset link sent. Check your email (and spam).");
                } else {
                    toast(task.getException() != null ? task.getException().getMessage() : "Could not send email");
                }
            });
        });

        btnGoogle.setOnClickListener(v -> toast("Google sign-in comes next"));
    }

    private boolean valid(String email, String pw) {
        if (email.isEmpty() || pw.isEmpty()) {
            toast("Enter email and password");
            return false;
        }
        if (pw.length() < 6) {
            toast("Password must be at least 6 characters");
            return false;
        }
        return true;
    }

    private void saveUser(FirebaseUser user) {
        if (user == null) return;
        String email = user.getEmail() == null ? "" : user.getEmail();
        OrderStore.get().userEmail = email;
        OrderStore.get().userName = email.contains("@") ? email.substring(0, email.indexOf("@")) : "Student";
    }

    private void goHome() {
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }
}