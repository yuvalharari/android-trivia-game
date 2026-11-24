package com.example.trivia;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    EditText etUsername, etPassword;
    Button btnGoToMain, btnSignUp;
    FirebaseAuth mAuth;
    DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance("https://triviaapp-61460-default-rtdb.europe-west1.firebasedatabase.app").getReference();

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnGoToMain = findViewById(R.id.btnGoToMain);
        btnSignUp = findViewById(R.id.btnSignUp);

        setClickAnimation(btnGoToMain);
        setClickAnimation(btnSignUp);

        btnGoToMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = etUsername.getText().toString().trim();
                String password = etPassword.getText().toString().trim();

                if (email.isEmpty()) {
                    etUsername.setError("Please enter your email");
                    etUsername.requestFocus();
                    return;
                }

                if (password.isEmpty()) {
                    etPassword.setError("Please enter your password");
                    etPassword.requestFocus();
                    return;
                }

                if (password.length() < 6) {
                    etPassword.setError("Password must be at least 6 characters");
                    etPassword.requestFocus();
                    return;
                }

                // Authenticate with Firebase
                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                String userId = mAuth.getCurrentUser().getUid();
                                fetchRoleAndGoToMenu(userId, true);
                            } else {
                                String errorMessage = task.getException().getMessage();

                                if (errorMessage.contains("no user record")) {
                                    Toast.makeText(LoginActivity.this,
                                            "No account found with this email!",
                                            Toast.LENGTH_LONG).show();
                                } else if (errorMessage.contains("password is invalid") ||
                                        errorMessage.contains("wrong password")) {
                                    Toast.makeText(LoginActivity.this,
                                            "Wrong password!",
                                            Toast.LENGTH_LONG).show();
                                } else if (errorMessage.contains("badly formatted")) {
                                    Toast.makeText(LoginActivity.this,
                                            "Invalid email format!",
                                            Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(LoginActivity.this,
                                            "Login failed: " + errorMessage,
                                            Toast.LENGTH_LONG).show();
                                }
                            }
                        });
            }
        });

        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
                startActivity(intent);
            }
        });
    }

    void fetchRoleAndGoToMenu(String userId, boolean showToast) {
        mDatabase.child("users").child(userId).child("role")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        String role = snapshot.getValue(String.class);
                        if (role == null) role = "user";

                        // Save role to SharedPreferences
                        SharedPreferences prefs = getSharedPreferences("TriviaPrefs", MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString("role", role);
                        editor.apply();

                        // Show role-based welcome toast
                        if (showToast) {
                            if (role.equals("admin")) {
                                Toast.makeText(LoginActivity.this,
                                        "Welcome Admin! Login successful!",
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(LoginActivity.this,
                                        "Login successful!",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }

                        goToMainMenu();
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        goToMainMenu();
                    }
                });
    }

    void goToMainMenu() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    void setClickAnimation(View view) {
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start();
                } else if (event.getAction() == MotionEvent.ACTION_UP ||
                        event.getAction() == MotionEvent.ACTION_CANCEL) {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                }
                return false;
            }
        });
    }
}