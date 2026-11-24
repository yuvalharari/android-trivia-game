package com.example.trivia;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;

public class GameActivity extends AppCompatActivity {

    TextView tvQuestion, tvScore, tvTimer, tvQuestionNumber;
    Button btnAnswer1, btnAnswer2, btnAnswer3;
    VideoView videoBackground;

    int score = 0;
    int currentQuestion = 0;
    int numQuestions = 10;
    boolean timerEnabled = true;
    boolean isPersonalQuiz = false;
    boolean isGeneralQuiz = false;
    boolean showCorrectAnswer = false;
    String quizId = "";
    String role = "user";
    String userId = "";
    CountDownTimer countDownTimer;
    DatabaseReference mDatabase;
    FirebaseAuth mAuth;

    static class QuestionModel {
        String question;
        String correctAnswer;
        String wrongAnswer1;
        String wrongAnswer2;

        QuestionModel(String q, String correct, String w1, String w2) {
            this.question = q;
            this.correctAnswer = correct;
            this.wrongAnswer1 = w1;
            this.wrongAnswer2 = w2;
        }
    }

    ArrayList<QuestionModel> questionList = new ArrayList<>();
    ArrayList<String> currentAnswers = new ArrayList<>();
    String currentCorrectAnswer = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance("https://triviaapp-61460-default-rtdb.europe-west1.firebasedatabase.app").getReference();
        userId = mAuth.getCurrentUser().getUid();

        quizId = getIntent().getStringExtra("quizId");
        role = getIntent().getStringExtra("role");
        timerEnabled = getIntent().getBooleanExtra("timerEnabled", true);
        isPersonalQuiz = getIntent().getBooleanExtra("isPersonalQuiz", false);
        isGeneralQuiz = getIntent().getBooleanExtra("isGeneralQuiz", false);
        showCorrectAnswer = getIntent().getBooleanExtra("showCorrectAnswer", false);
        String numQ = getIntent().getStringExtra("numQuestions");
        if (numQ != null) numQuestions = Integer.parseInt(numQ);
        if (role == null) role = "user";

        tvQuestion = findViewById(R.id.tvQuestion);
        tvScore = findViewById(R.id.tvScore);
        tvTimer = findViewById(R.id.tvTimer);
        tvQuestionNumber = findViewById(R.id.tvQuestionNumber);
        btnAnswer1 = findViewById(R.id.btnAnswer1);
        btnAnswer2 = findViewById(R.id.btnAnswer2);
        btnAnswer3 = findViewById(R.id.btnAnswer3);
        videoBackground = findViewById(R.id.videoBackground);

        // Start looping background video
        Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.trivia_bg);
        videoBackground.setVideoURI(videoUri);
        videoBackground.setOnCompletionListener(mp -> videoBackground.start());
        videoBackground.start();

        // Apply press animation to answer buttons
        setClickAnimation(btnAnswer1);
        setClickAnimation(btnAnswer2);
        setClickAnimation(btnAnswer3);

        tvQuestion.setText("Loading questions...");
        setButtonsEnabled(false);

        loadQuestionsFromFirebase();

        btnAnswer1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { checkAnswer(btnAnswer1.getText().toString()); }
        });

        btnAnswer2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { checkAnswer(btnAnswer2.getText().toString()); }
        });

        btnAnswer3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { checkAnswer(btnAnswer3.getText().toString()); }
        });
    }

    void loadQuestionsFromFirebase() {
        DatabaseReference ref;

        // Load from personal or general quiz path
        if (isPersonalQuiz && !isGeneralQuiz) {
            ref = mDatabase.child("my_quizzes")
                    .child("personal")
                    .child(userId)
                    .child(quizId)
                    .child("questions");
        } else {
            ref = mDatabase.child("my_quizzes")
                    .child("general")
                    .child(quizId)
                    .child("questions");
        }

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                questionList.clear();

                for (DataSnapshot q : snapshot.getChildren()) {
                    String question = q.child("question").getValue(String.class);
                    String correct = q.child("correctAnswer").getValue(String.class);
                    String wrong1 = q.child("wrongAnswer1").getValue(String.class);
                    String wrong2 = q.child("wrongAnswer2").getValue(String.class);

                    if (question != null && correct != null && wrong1 != null && wrong2 != null) {
                        questionList.add(new QuestionModel(question, correct, wrong1, wrong2));
                    }
                }

                if (questionList.isEmpty()) {
                    Toast.makeText(GameActivity.this,
                            "No questions found!",
                            Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                // Shuffle and limit to selected count
                Collections.shuffle(questionList);
                numQuestions = Math.min(numQuestions, questionList.size());
                setButtonsEnabled(true);
                loadQuestion();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(GameActivity.this,
                        "Error loading questions!",
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    void loadQuestion() {
        if (currentQuestion >= numQuestions) {
            goToResults();
            return;
        }

        QuestionModel q = questionList.get(currentQuestion);

        tvQuestionNumber.setText("Question: " + (currentQuestion + 1) + "/" + numQuestions);
        tvQuestion.setText(q.question);

        // Shuffle answers each round
        currentAnswers.clear();
        currentAnswers.add(q.correctAnswer);
        currentAnswers.add(q.wrongAnswer1);
        currentAnswers.add(q.wrongAnswer2);

        Collections.shuffle(currentAnswers);
        currentCorrectAnswer = q.correctAnswer;

        btnAnswer1.setText(currentAnswers.get(0));
        btnAnswer2.setText(currentAnswers.get(1));
        btnAnswer3.setText(currentAnswers.get(2));

        resetButtonColors();
        setButtonsEnabled(true);

        if (timerEnabled) {
            startTimer();
        } else {
            tvTimer.setText("No Timer");
        }
    }

    void startTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        tvTimer.setTextColor(0xFFFFFFFF);

        countDownTimer = new CountDownTimer(10000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long secondsLeft = millisUntilFinished / 1000;
                tvTimer.setText("Time: " + secondsLeft);

                // Turn red when 3 seconds or less remain
                if (secondsLeft <= 3) {
                    tvTimer.setTextColor(0xFFEF5350);
                } else {
                    tvTimer.setTextColor(0xFFFFFFFF);
                }
            }

            @Override
            public void onFinish() {
                tvTimer.setText("Time: 0");
                tvTimer.setTextColor(0xFFEF5350);
                if (showCorrectAnswer) {
                    Toast.makeText(GameActivity.this,
                            "Time's up! Correct answer: " + currentCorrectAnswer,
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(GameActivity.this,
                            "Time's up!",
                            Toast.LENGTH_SHORT).show();
                }
                currentQuestion++;
                loadQuestion();
            }
        }.start();
    }

    void checkAnswer(String selectedAnswer) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        setButtonsEnabled(false);

        boolean isCorrect = selectedAnswer.equals(currentCorrectAnswer);

        // Color selected button green or red
        Button[] buttons = {btnAnswer1, btnAnswer2, btnAnswer3};
        for (Button btn : buttons) {
            if (btn.getText().toString().equals(selectedAnswer)) {
                if (isCorrect) {
                    btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF4CAF50));
                } else {
                    btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFEF5350));
                    if (showCorrectAnswer) {
                        Toast.makeText(GameActivity.this,
                                "Correct answer: " + currentCorrectAnswer,
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }

        if (isCorrect) {
            score++;
        }

        tvScore.setText("Score: " + score);

        btnAnswer1.postDelayed(new Runnable() {
            @Override
            public void run() {
                currentQuestion++;
                loadQuestion();
            }
        }, 1000);
    }

    void setButtonsEnabled(boolean enabled) {
        btnAnswer1.setEnabled(enabled);
        btnAnswer2.setEnabled(enabled);
        btnAnswer3.setEnabled(enabled);
    }

    void resetButtonColors() {
        int white = 0xFFFFFFFF;
        btnAnswer1.setBackgroundTintList(android.content.res.ColorStateList.valueOf(white));
        btnAnswer2.setBackgroundTintList(android.content.res.ColorStateList.valueOf(white));
        btnAnswer3.setBackgroundTintList(android.content.res.ColorStateList.valueOf(white));
    }

    void goToResults() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        if (videoBackground != null) {
            videoBackground.stopPlayback();
        }
        Intent intent = new Intent(GameActivity.this, ResultsActivity.class);
        intent.putExtra("score", score);
        intent.putExtra("numQuestions", numQuestions);
        intent.putExtra("quizName", getIntent().getStringExtra("quizName"));
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        if (videoBackground != null) {
            videoBackground.stopPlayback();
        }
    }
}