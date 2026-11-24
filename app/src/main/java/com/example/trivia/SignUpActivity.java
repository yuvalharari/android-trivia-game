package com.example.trivia;

import android.content.Intent;
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

public class SignUpActivity extends AppCompatActivity {

    EditText etNewUsername, etEmail, etNewPassword;
    Button btnRegister, btnBackToLogin;
    FirebaseAuth mAuth;
    DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance("https://triviaapp-61460-default-rtdb.europe-west1.firebasedatabase.app").getReference();

        etNewUsername = findViewById(R.id.etNewUsername);
        etEmail = findViewById(R.id.etEmail);
        etNewPassword = findViewById(R.id.etNewPassword);
        btnRegister = findViewById(R.id.btnRegister);
        btnBackToLogin = findViewById(R.id.btnBackToLogin);

        setClickAnimation(btnRegister);
        setClickAnimation(btnBackToLogin);

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = etNewUsername.getText().toString().trim();
                String email = etEmail.getText().toString().trim();
                String password = etNewPassword.getText().toString().trim();

                if (username.isEmpty()) {
                    etNewUsername.setError("Please enter a username");
                    etNewUsername.requestFocus();
                    return;
                }

                if (username.length() < 3) {
                    etNewUsername.setError("Username must be at least 3 characters");
                    etNewUsername.requestFocus();
                    return;
                }

                if (email.isEmpty()) {
                    etEmail.setError("Please enter your email");
                    etEmail.requestFocus();
                    return;
                }

                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    etEmail.setError("Please enter a valid email");
                    etEmail.requestFocus();
                    return;
                }

                if (password.isEmpty()) {
                    etNewPassword.setError("Please enter a password");
                    etNewPassword.requestFocus();
                    return;
                }

                if (password.length() < 6) {
                    etNewPassword.setError("Password must be at least 6 characters");
                    etNewPassword.requestFocus();
                    return;
                }

                mDatabase.child("users").orderByChild("username")
                        .equalTo(username)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot snapshot) {
                                if (snapshot.exists()) {
                                    etNewUsername.setError("Username already taken!");
                                    etNewUsername.requestFocus();
                                } else {
                                    registerUser(username, email, password);
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError error) {
                                Toast.makeText(SignUpActivity.this,
                                        "Error: " + error.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });

        btnBackToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    void registerUser(String username, String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String userId = mAuth.getCurrentUser().getUid();

                        UserModel user = new UserModel(username, email, "user");
                        mDatabase.child("users").child(userId).setValue(user);

                        Toast.makeText(SignUpActivity.this,
                                "Account created successfully!",
                                Toast.LENGTH_SHORT).show();
                        goToMainMenu();

                    } else {
                        String errorMessage = task.getException().getMessage();

                        if (errorMessage.contains("email address is already in use")) {
                            etEmail.setError("This email is already registered!");
                            etEmail.requestFocus();
                        } else if (errorMessage.contains("badly formatted")) {
                            etEmail.setError("Invalid email format!");
                            etEmail.requestFocus();
                        } else {
                            Toast.makeText(SignUpActivity.this,
                                    "Registration failed: " + errorMessage,
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    void goToMainMenu() {
        Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
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
