package com.example.printingstudio;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        EditText etEmail = findViewById(R.id.etEmail);
        Button btnEmailLogin = findViewById(R.id.btnEmailLogin);
        Button btnGoogleLogin = findViewById(R.id.btnGoogleLogin);

        btnEmailLogin.setOnClickListener(v -> {
            // TODO: call POST /auth/login (or /auth/signup if new)
            String email = etEmail.getText().toString();
            if (email.isEmpty()) return;
            String name = email.contains("@") ? email.substring(0, email.indexOf("@")) : "Student";
            OrderStore.get().userEmail = email;
            OrderStore.get().userName = name;
            goHome();
        });

        btnGoogleLogin.setOnClickListener(v -> {
            // TODO: launch Google Sign-In, then POST /auth/google with the ID token
            OrderStore.get().userEmail = "student@college.edu";
            OrderStore.get().userName = "Student";
            goHome();
        });
    }

    private void goHome() {
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }
}
