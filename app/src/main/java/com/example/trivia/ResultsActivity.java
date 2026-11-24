package com.example.trivia;

import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ResultsActivity extends AppCompatActivity {

    TextView tvFinalScore, tvHighScore, tvRatingThankYou;
    RatingBar ratingBar;
    ImageView ivResultImage;
    Button btnPlayAgain, btnInviteFriend, btnSendInvite, btnBackToMenu;
    LinearLayout phoneContainer;
    EditText etPhoneNumber;
    DatabaseReference mDatabase;
    FirebaseAuth mAuth;
    String userId;
    String quizName;

    int score = 0;
    int numQuestions = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance("https://triviaapp-61460-default-rtdb.europe-west1.firebasedatabase.app").getReference();
        userId = mAuth.getCurrentUser().getUid();

        score = getIntent().getIntExtra("score", 0);
        numQuestions = getIntent().getIntExtra("numQuestions", 10);
        quizName = getIntent().getStringExtra("quizName");

        tvFinalScore = findViewById(R.id.tvFinalScore);
        tvHighScore = findViewById(R.id.tvHighScore);
        tvRatingThankYou = findViewById(R.id.tvRatingThankYou);
        ratingBar = findViewById(R.id.ratingBar);
        ivResultImage = findViewById(R.id.ivResultImage);
        btnPlayAgain = findViewById(R.id.btnPlayAgain);
        btnInviteFriend = findViewById(R.id.btnInviteFriend);
        btnSendInvite = findViewById(R.id.btnSendInvite);
        btnBackToMenu = findViewById(R.id.btnBackToMenu);
        phoneContainer = findViewById(R.id.phoneContainer);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);

        setClickAnimation(btnPlayAgain);
        setClickAnimation(btnInviteFriend);
        setClickAnimation(btnSendInvite);
        setClickAnimation(btnBackToMenu);

        tvFinalScore.setText("Your Score: " + score + "/" + numQuestions);
        tvHighScore.setText("Loading...");
        tvRatingThankYou.setVisibility(View.GONE);

        // Show result image based on score percentage
        int percentage = numQuestions > 0 ? (score * 100) / numQuestions : 0;
        if (percentage == 100) {
            ivResultImage.setImageResource(R.drawable.trophy);
        } else if (percentage >= 80) {
            ivResultImage.setImageResource(R.drawable.ok);
        } else if (percentage >= 60) {
            ivResultImage.setImageResource(R.drawable.like);
        } else {
            ivResultImage.setImageResource(R.drawable.fail);
        }

        saveStatsToFirebase();

        // Save rating and hide RatingBar after submission
        ratingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                if (fromUser && quizName != null) {
                    saveRatingToFirebase(rating);
                    ratingBar.setVisibility(View.GONE);
                    tvRatingThankYou.setVisibility(View.VISIBLE);
                    tvRatingThankYou.setText("Thank you for rating! ⭐");
                }
            }
        });

        btnPlayAgain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ResultsActivity.this, GameSettingsActivity.class);
                startActivity(intent);
                finish();
            }
        });

        // Toggle phone input visibility
        btnInviteFriend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (phoneContainer.getVisibility() == View.GONE) {
                    phoneContainer.setVisibility(View.VISIBLE);
                } else {
                    phoneContainer.setVisibility(View.GONE);
                }
            }
        });

        btnSendInvite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String phone = etPhoneNumber.getText().toString().trim();

                if (phone.isEmpty()) {
                    etPhoneNumber.setError("Please enter a phone number");
                    etPhoneNumber.requestFocus();
                    return;
                }

                if (phone.length() != 9) {
                    etPhoneNumber.setError("Please enter exactly 9 digits");
                    etPhoneNumber.requestFocus();
                    return;
                }

                if (!phone.matches("[0-9]+")) {
                    etPhoneNumber.setError("Numbers only please");
                    etPhoneNumber.requestFocus();
                    return;
                }

                // Send SMS invite with score and download link
                String fullPhone = "+972" + phone;
                String message = "Hey! I just scored " + score + "/" + numQuestions +
                        " in Trivia App! 🎯\n" +
                        "Think you can beat me?\n" +
                        "Download now: https://play.google.com/store/apps/details?id=com.example.trivia";

                try {
                    SmsManager smsManager = SmsManager.getDefault();
                    smsManager.sendTextMessage(fullPhone, null, message, null, null);
                    Toast.makeText(ResultsActivity.this,
                            "Invite sent to " + fullPhone,
                            Toast.LENGTH_SHORT).show();
                    phoneContainer.setVisibility(View.GONE);
                    etPhoneNumber.setText("");
                } catch (Exception e) {
                    Toast.makeText(ResultsActivity.this,
                            e.getMessage().toString(),
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        btnBackToMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ResultsActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        });
    }

    void saveRatingToFirebase(float newRating) {
        DatabaseReference ratingRef = mDatabase.child("ratings").child(quizName);

        ratingRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                float totalRating = 0;
                int ratingCount = 0;

                if (snapshot.exists()) {
                    Float tr = snapshot.child("totalRating").getValue(Float.class);
                    Integer rc = snapshot.child("ratingCount").getValue(Integer.class);
                    if (tr != null) totalRating = tr;
                    if (rc != null) ratingCount = rc;
                }

                // Update global average rating
                totalRating += newRating;
                ratingCount++;
                float average = totalRating / ratingCount;

                Map<String, Object> ratingData = new HashMap<>();
                ratingData.put("totalRating", totalRating);
                ratingData.put("ratingCount", ratingCount);
                ratingData.put("average", average);
                ratingRef.setValue(ratingData);
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }

    void saveStatsToFirebase() {
        DatabaseReference statsRef = mDatabase.child("stats").child(userId);

        statsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                int totalGames = 0;
                int totalCorrect = 0;
                int totalQuestions = 0;
                int highScore = 0;
                int highScoreTotal = 0;

                if (snapshot.exists()) {
                    Integer tg = snapshot.child("totalGames").getValue(Integer.class);
                    Integer tc = snapshot.child("totalCorrect").getValue(Integer.class);
                    Integer tq = snapshot.child("totalQuestions").getValue(Integer.class);
                    Integer hs = snapshot.child("highScore").getValue(Integer.class);
                    Integer hst = snapshot.child("highScoreTotal").getValue(Integer.class);
                    if (tg != null) totalGames = tg;
                    if (tc != null) totalCorrect = tc;
                    if (tq != null) totalQuestions = tq;
                    if (hs != null) highScore = hs;
                    if (hst != null) highScoreTotal = hst;
                }

                totalGames++;
                totalCorrect += score;
                totalQuestions += numQuestions;

                // Update high score based on percentage
                int currentPercentage = numQuestions > 0 ? (score * 100) / numQuestions : 0;
                int highScorePercentage = highScoreTotal > 0 ? (highScore * 100) / highScoreTotal : 0;

                if (currentPercentage > highScorePercentage) {
                    highScore = score;
                    highScoreTotal = numQuestions;
                    Toast.makeText(ResultsActivity.this,
                            "New High Score!",
                            Toast.LENGTH_SHORT).show();
                }

                tvHighScore.setText("High Score: " + highScore + "/" + highScoreTotal);

                Map<String, Object> stats = new HashMap<>();
                stats.put("totalGames", totalGames);
                stats.put("totalCorrect", totalCorrect);
                stats.put("totalQuestions", totalQuestions);
                stats.put("highScore", highScore);
                stats.put("highScoreTotal", highScoreTotal);
                stats.put("lastScore", score);
                stats.put("lastTotal", numQuestions);
                statsRef.setValue(stats);

                saveGameHistory();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                tvHighScore.setText("High Score: -");
            }
        });
    }

    void saveGameHistory() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        DatabaseReference dayRef = mDatabase.child("game_history").child(userId).child(today);

        dayRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                int gamesPlayed = 0;
                int dayHighScore = 0;
                int dayHighScoreTotal = 0;

                if (snapshot.exists()) {
                    Integer eg = snapshot.child("gamesPlayed").getValue(Integer.class);
                    Integer eh = snapshot.child("highScore").getValue(Integer.class);
                    Integer eht = snapshot.child("highScoreTotal").getValue(Integer.class);
                    if (eg != null) gamesPlayed = eg;
                    if (eh != null) dayHighScore = eh;
                    if (eht != null) dayHighScoreTotal = eht;
                }

                gamesPlayed++;

                // Update daily high score based on percentage
                int currentPercentage = numQuestions > 0 ? (score * 100) / numQuestions : 0;
                int dayHighPercentage = dayHighScoreTotal > 0 ? (dayHighScore * 100) / dayHighScoreTotal : 0;

                if (currentPercentage > dayHighPercentage) {
                    dayHighScore = score;
                    dayHighScoreTotal = numQuestions;
                }

                Map<String, Object> dayData = new HashMap<>();
                dayData.put("gamesPlayed", gamesPlayed);
                dayData.put("highScore", dayHighScore);
                dayData.put("highScoreTotal", dayHighScoreTotal);
                dayData.put("lastScore", score);
                dayData.put("lastTotal", numQuestions);
                dayRef.setValue(dayData);
            }

            @Override
            public void onCancelled(DatabaseError error) {}
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