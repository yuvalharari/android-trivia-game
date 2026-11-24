package com.example.trivia;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class SplashActivity extends AppCompatActivity {

    FirebaseAuth mAuth;
    DatabaseReference mDatabase;
    ProgressBar progressBar;
    TextView loadingText;
    Handler handler = new Handler();
    int progress = 0;

    boolean splashDone = false;
    boolean authDone = false;
    String resolvedRole = null;
    boolean isLoggedIn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance("https://triviaapp-61460-default-rtdb.europe-west1.firebasedatabase.app").getReference();

        ImageView logo = findViewById(R.id.ivLogo);
        progressBar = findViewById(R.id.progressBar);
        loadingText = findViewById(R.id.loadingText);

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(logo, "scaleX", 1f, 1.3f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(logo, "scaleY", 1f, 1.3f, 1f);
        scaleX.setDuration(800);
        scaleY.setDuration(800);
        scaleX.setRepeatCount(3);
        scaleY.setRepeatCount(3);

        AnimatorSet set = new AnimatorSet();
        set.play(scaleX).with(scaleY);
        set.start();

        Runnable progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (progress <= 100) {
                    progressBar.setProgress(progress);
                    loadingText.setText("Loading " + progress + "%");
                    progress++;
                    handler.postDelayed(this, 30);
                }
            }
        };
        handler.post(progressRunnable);

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            isLoggedIn = true;
            mDatabase.child("users").child(currentUser.getUid()).child("role")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            String role = snapshot.getValue(String.class);
                            resolvedRole = (role != null) ? role : "user";
                            authDone = true;
                            tryNavigate();
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {
                            resolvedRole = "user";
                            authDone = true;
                            tryNavigate();
                        }
                    });
        } else {
            authDone = true;
        }

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                splashDone = true;
                tryNavigate();
            }
        }, 3000);
    }

    void tryNavigate() {
        if (!splashDone || !authDone) return;

        if (isLoggedIn && resolvedRole != null) {
            SharedPreferences prefs = getSharedPreferences("TriviaPrefs", MODE_PRIVATE);
            prefs.edit().putString("role", resolvedRole).apply();

            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        } else {
            goToLogin();
        }
        finish();
    }

    void goToLogin() {
        Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
