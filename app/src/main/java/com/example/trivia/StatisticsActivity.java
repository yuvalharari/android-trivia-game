package com.example.trivia;

import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Locale;

public class StatisticsActivity extends AppCompatActivity {

    TextView tvLastScore, tvStatsHighScore, tvTotalGames, tvSuccessRate, tvCalendarInfo;
    CalendarView calendarView;
    LinearLayout calendarContainer;
    Button btnHistory, btnBackToMenu;
    boolean isCalendarVisible = false;
    DatabaseReference mDatabase;
    FirebaseAuth mAuth;
    String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance("https://triviaapp-61460-default-rtdb.europe-west1.firebasedatabase.app").getReference();
        userId = mAuth.getCurrentUser().getUid();

        tvLastScore = findViewById(R.id.tvLastScore);
        tvStatsHighScore = findViewById(R.id.tvStatsHighScore);
        tvTotalGames = findViewById(R.id.tvTotalGames);
        tvSuccessRate = findViewById(R.id.tvSuccessRate);
        tvCalendarInfo = findViewById(R.id.tvCalendarInfo);
        calendarView = findViewById(R.id.calendarView);
        calendarContainer = findViewById(R.id.calendarContainer);
        btnHistory = findViewById(R.id.btnHistory);
        btnBackToMenu = findViewById(R.id.btnBackToMenu);

        setClickAnimation(btnHistory);
        setClickAnimation(btnBackToMenu);

        tvLastScore.setText("Last Game: Loading...");
        tvStatsHighScore.setText("High Score: Loading...");
        tvTotalGames.setText("Total Games: Loading...");
        tvSuccessRate.setText("Success Rate: Loading...");

        loadStatsFromFirebase();

        // Toggle calendar visibility
        btnHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isCalendarVisible) {
                    calendarContainer.setVisibility(View.VISIBLE);
                    btnHistory.setText("Hide History");
                    isCalendarVisible = true;
                } else {
                    calendarContainer.setVisibility(View.GONE);
                    btnHistory.setText("Show History");
                    isCalendarVisible = false;
                }
            }
        });

        calendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(CalendarView view, int year, int month, int dayOfMonth) {
                String date = String.format(Locale.getDefault(),
                        "%04d-%02d-%02d", year, month + 1, dayOfMonth);

                // Load game history for selected date
                mDatabase.child("game_history").child(userId).child(date)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot snapshot) {
                                if (snapshot.exists()) {
                                    Integer gamesPlayed = snapshot.child("gamesPlayed").getValue(Integer.class);
                                    Integer dayHighScore = snapshot.child("highScore").getValue(Integer.class);
                                    Integer dayHighScoreTotal = snapshot.child("highScoreTotal").getValue(Integer.class);
                                    Integer lastScore = snapshot.child("lastScore").getValue(Integer.class);
                                    Integer lastTotal = snapshot.child("lastTotal").getValue(Integer.class);

                                    // Format last game with percentage
                                    String lastGameStr;
                                    if (lastScore != null && lastTotal != null && lastTotal > 0) {
                                        int lastPct = (lastScore * 100) / lastTotal;
                                        lastGameStr = lastScore + "/" + lastTotal + " (" + lastPct + "%)";
                                    } else {
                                        lastGameStr = "No games yet";
                                    }

                                    // Format high score with percentage
                                    String highScoreStr;
                                    if (dayHighScore != null && dayHighScoreTotal != null && dayHighScoreTotal > 0) {
                                        int bestPct = (dayHighScore * 100) / dayHighScoreTotal;
                                        highScoreStr = dayHighScore + "/" + dayHighScoreTotal + " (" + bestPct + "%)";
                                    } else {
                                        highScoreStr = (dayHighScore != null ? dayHighScore : 0) + "";
                                    }

                                    String info = "Date: " + dayOfMonth + "/" + (month + 1) + "/" + year + "\n" +
                                            "Last Game: " + lastGameStr + "\n" +
                                            "High Score: " + highScoreStr + "\n" +
                                            "Total Games: " + (gamesPlayed != null ? gamesPlayed : 0);

                                    tvCalendarInfo.setText(info);
                                } else {
                                    tvCalendarInfo.setText("Date: " + dayOfMonth + "/" + (month + 1) + "/" + year +
                                            "\nNo games played on this day");
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError error) {
                                Toast.makeText(StatisticsActivity.this,
                                        "Error loading history!",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });

        btnBackToMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(StatisticsActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        });
    }

    void loadStatsFromFirebase() {
        mDatabase.child("stats").child(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            Integer lastScore = snapshot.child("lastScore").getValue(Integer.class);
                            Integer lastTotal = snapshot.child("lastTotal").getValue(Integer.class);
                            Integer highScore = snapshot.child("highScore").getValue(Integer.class);
                            Integer highScoreTotal = snapshot.child("highScoreTotal").getValue(Integer.class);
                            Integer totalGames = snapshot.child("totalGames").getValue(Integer.class);
                            Integer totalCorrect = snapshot.child("totalCorrect").getValue(Integer.class);
                            Integer totalQuestions = snapshot.child("totalQuestions").getValue(Integer.class);

                            // Last game with percentage
                            if (lastScore != null && lastTotal != null && lastTotal > 0) {
                                int lastPct = (lastScore * 100) / lastTotal;
                                tvLastScore.setText("Last Game: " + lastScore + "/" + lastTotal + " (" + lastPct + "%)");
                            } else {
                                tvLastScore.setText("Last Game: No games yet");
                            }

                            // High score with percentage
                            if (highScore != null && highScoreTotal != null && highScoreTotal > 0) {
                                int pct = (highScore * 100) / highScoreTotal;
                                tvStatsHighScore.setText("High Score: " + highScore + "/" + highScoreTotal + " (" + pct + "%)");
                            } else {
                                tvStatsHighScore.setText("High Score: " + (highScore != null ? highScore : 0));
                            }

                            tvTotalGames.setText("Total Games: " + (totalGames != null ? totalGames : 0));

                            // Overall success rate
                            if (totalCorrect != null && totalQuestions != null && totalQuestions > 0) {
                                int successRate = (totalCorrect * 100) / totalQuestions;
                                tvSuccessRate.setText("Success Rate: " + successRate + "% (" + totalCorrect + "/" + totalQuestions + ")");
                            } else {
                                tvSuccessRate.setText("Success Rate: No games yet");
                            }

                        } else {
                            tvLastScore.setText("Last Game: No games yet");
                            tvStatsHighScore.setText("High Score: 0");
                            tvTotalGames.setText("Total Games: 0");
                            tvSuccessRate.setText("Success Rate: No games yet");
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(StatisticsActivity.this,
                                "Error loading stats!",
                                Toast.LENGTH_SHORT).show();
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