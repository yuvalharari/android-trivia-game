package com.example.trivia;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    FirebaseAuth mAuth;
    DatabaseReference mDatabase;
    TextView tvWelcome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance("https://triviaapp-61460-default-rtdb.europe-west1.firebasedatabase.app").getReference();

        tvWelcome = findViewById(R.id.tvWelcome);
        Button btnPlayTrivia = findViewById(R.id.btnPlayTrivia);
        Button btnStatistics = findViewById(R.id.btnStatistics);
        Button btnMyQuizzes = findViewById(R.id.btnMyQuizzes);
        Button btnInformation = findViewById(R.id.btnInformation);
        Button btnLogout = findViewById(R.id.btnLogout);

        setClickAnimation(btnPlayTrivia);
        setClickAnimation(btnStatistics);
        setClickAnimation(btnMyQuizzes);
        setClickAnimation(btnInformation);
        setClickAnimation(btnLogout);

        // Load username and display welcome message
        if (mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();
            mDatabase.child("users").child(userId).child("username")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            String username = snapshot.getValue(String.class);
                            if (username != null) {
                                SharedPreferences prefs = getSharedPreferences("TriviaPrefs", MODE_PRIVATE);
                                String role = prefs.getString("role", "user");
                                if (role.equals("admin")) {
                                    tvWelcome.setText("Welcome Manager, " + username);
                                } else {
                                    tvWelcome.setText("Welcome, " + username);
                                }
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {}
                    });
        }

        btnPlayTrivia.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, GameSettingsActivity.class);
                startActivity(intent);
            }
        });

        btnStatistics.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, StatisticsActivity.class);
                startActivity(intent);
            }
        });

        btnMyQuizzes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, MyQuizzesActivity.class);
                startActivity(intent);
            }
        });

        btnInformation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, InformationActivity.class);
                startActivity(intent);
            }
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences prefs = getSharedPreferences("TriviaPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.remove("role");
                editor.apply();

                mAuth.signOut();
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
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
