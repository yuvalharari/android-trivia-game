package com.example.trivia;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class GameSettingsActivity extends AppCompatActivity {

    Spinner spinnerCategory;
    SeekBar seekBarQuestions;
    TextView tvCategory, tvNumQuestions;
    SwitchCompat switchTimer;
    CheckBox checkBoxShowAnswer;
    Button btnStartGame, btnBackToMenu;
    String quizId = null;
    String quizName = null;
    String role = "user";
    boolean isPersonalQuiz = false;
    DatabaseReference mDatabase;
    ArrayList<String> categoryIds = new ArrayList<>();
    ArrayList<String> categoryNames = new ArrayList<>();
    SharedPreferences prefs;
    int selectedNumQuestions = 3;
    static final int MIN_QUESTIONS = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_settings);

        mDatabase = FirebaseDatabase.getInstance("https://triviaapp-61460-default-rtdb.europe-west1.firebasedatabase.app").getReference();
        prefs = getSharedPreferences("TriviaPrefs", MODE_PRIVATE);

        spinnerCategory = findViewById(R.id.spinnerCategory);
        seekBarQuestions = findViewById(R.id.seekBarQuestions);
        tvCategory = findViewById(R.id.tvCategory);
        tvNumQuestions = findViewById(R.id.tvNumQuestions);
        switchTimer = findViewById(R.id.switchTimer);
        checkBoxShowAnswer = findViewById(R.id.checkBoxShowAnswer);
        btnStartGame = findViewById(R.id.btnStartGame);
        btnBackToMenu = findViewById(R.id.btnBackToMenu);

        setClickAnimation(btnStartGame);
        setClickAnimation(btnBackToMenu);

        quizId = getIntent().getStringExtra("quizId");
        quizName = getIntent().getStringExtra("quizName");
        role = getIntent().getStringExtra("role");
        if (role == null) role = "user";

        isPersonalQuiz = quizId != null;

        boolean lastTimer = prefs.getBoolean("lastTimer", true);
        switchTimer.setChecked(lastTimer);

        boolean lastShowAnswer = prefs.getBoolean("showCorrectAnswer", false);
        checkBoxShowAnswer.setChecked(lastShowAnswer);

        // SeekBar: actual count = progress + MIN_QUESTIONS
        seekBarQuestions.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                selectedNumQuestions = progress + MIN_QUESTIONS;
                tvNumQuestions.setText("Number of Questions: " + selectedNumQuestions);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        if (isPersonalQuiz) {
            tvCategory.setVisibility(View.GONE);
            spinnerCategory.setVisibility(View.GONE);
            loadPersonalQuizQuestionCount();
        } else {
            loadCategoriesFromFirebase();
        }

        btnStartGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean timerEnabled = switchTimer.isChecked();
                boolean showCorrectAnswer = checkBoxShowAnswer.isChecked();

                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("lastTimer", timerEnabled);
                editor.putBoolean("showCorrectAnswer", showCorrectAnswer);
                editor.putInt("lastNumQuestions", selectedNumQuestions);

                Intent intent = new Intent(GameSettingsActivity.this, GameActivity.class);
                intent.putExtra("timerEnabled", timerEnabled);
                intent.putExtra("showCorrectAnswer", showCorrectAnswer);
                intent.putExtra("role", role);
                intent.putExtra("numQuestions", String.valueOf(selectedNumQuestions));

                if (isPersonalQuiz) {
                    intent.putExtra("quizId", quizId);
                    intent.putExtra("quizName", quizName);
                    intent.putExtra("isPersonalQuiz", true);
                    intent.putExtra("isGeneralQuiz", role.equals("admin"));
                    Toast.makeText(GameSettingsActivity.this,
                            "Starting: " + quizName,
                            Toast.LENGTH_SHORT).show();
                } else {
                    if (categoryIds.isEmpty()) {
                        Toast.makeText(GameSettingsActivity.this,
                                "No categories available!",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int selectedIndex = spinnerCategory.getSelectedItemPosition();
                    String selectedQuizId = categoryIds.get(selectedIndex);
                    String selectedQuizName = categoryNames.get(selectedIndex).split("  ")[0];

                    editor.putInt("lastCategory", selectedIndex);

                    intent.putExtra("quizId", selectedQuizId);
                    intent.putExtra("quizName", selectedQuizName);
                    intent.putExtra("isPersonalQuiz", false);
                    intent.putExtra("isGeneralQuiz", true);

                    Toast.makeText(GameSettingsActivity.this,
                            "Starting: " + selectedQuizName,
                            Toast.LENGTH_SHORT).show();
                }

                editor.apply();
                startActivity(intent);
            }
        });

        btnBackToMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    // Sets SeekBar max based on question count, offset by MIN_QUESTIONS
    void updateSeekBar(int count) {
        int maxProgress = count - MIN_QUESTIONS;
        if (maxProgress < 0) maxProgress = 0;

        seekBarQuestions.setMax(maxProgress);

        int lastNum = prefs.getInt("lastNumQuestions", MIN_QUESTIONS);
        int progress = Math.min(lastNum, count) - MIN_QUESTIONS;
        if (progress < 0) progress = 0;

        seekBarQuestions.setProgress(progress);
        selectedNumQuestions = progress + MIN_QUESTIONS;
        tvNumQuestions.setText("Number of Questions: " + selectedNumQuestions);
    }

    void loadPersonalQuizQuestionCount() {
        com.google.firebase.auth.FirebaseAuth auth = com.google.firebase.auth.FirebaseAuth.getInstance();
        String userId = auth.getCurrentUser().getUid();

        // Load question count from personal or admin general quiz
        DatabaseReference ref = role.equals("admin") ?
                mDatabase.child("my_quizzes").child("general").child(quizId).child("questions") :
                mDatabase.child("my_quizzes").child("personal").child(userId).child(quizId).child("questions");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                int count = (int) snapshot.getChildrenCount();
                if (count < MIN_QUESTIONS) count = MIN_QUESTIONS;
                updateSeekBar(count);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                updateSeekBar(20);
            }
        });
    }

    void loadCategoriesFromFirebase() {
        mDatabase.child("my_quizzes").child("general")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        ArrayList<String> tempNames = new ArrayList<>();
                        ArrayList<String> tempIds = new ArrayList<>();

                        for (DataSnapshot category : snapshot.getChildren()) {
                            String name = category.child("quizName").getValue(String.class);
                            if (name != null) {
                                tempNames.add(name);
                                tempIds.add(category.getKey());
                            }
                        }

                        loadRatingsForCategories(tempNames, tempIds);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(GameSettingsActivity.this,
                                "Error loading categories!",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    void loadRatingsForCategories(ArrayList<String> names, ArrayList<String> ids) {
        mDatabase.child("ratings")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        categoryNames.clear();
                        categoryIds.clear();

                        for (int i = 0; i < names.size(); i++) {
                            String name = names.get(i);
                            String displayName;

                            // Show average rating if available
                            if (snapshot.hasChild(name)) {
                                Float average = snapshot.child(name)
                                        .child("average").getValue(Float.class);
                                if (average != null && average > 0) {
                                    displayName = name + "  " + getStars(average) +
                                            " (" + String.format("%.1f", average) + ")";
                                } else {
                                    displayName = name + "  (No rating yet)";
                                }
                            } else {
                                displayName = name + "  (No rating yet)";
                            }

                            categoryNames.add(displayName);
                            categoryIds.add(ids.get(i));
                        }

                        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                                GameSettingsActivity.this,
                                android.R.layout.simple_spinner_item,
                                categoryNames);
                        categoryAdapter.setDropDownViewResource(
                                android.R.layout.simple_spinner_dropdown_item);
                        spinnerCategory.setAdapter(categoryAdapter);

                        int lastCategory = prefs.getInt("lastCategory", 0);
                        if (lastCategory < categoryNames.size()) {
                            spinnerCategory.setSelection(lastCategory);
                        }

                        updateSeekBarForSelectedCategory();
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(GameSettingsActivity.this,
                                "Error loading ratings!",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    void updateSeekBarForSelectedCategory() {
        if (categoryIds.isEmpty()) return;

        int selectedIndex = spinnerCategory.getSelectedItemPosition();
        if (selectedIndex < 0 || selectedIndex >= categoryIds.size()) return;

        String selectedId = categoryIds.get(selectedIndex);

        // Update SeekBar limit based on selected category question count
        mDatabase.child("my_quizzes").child("general").child(selectedId).child("questions")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        int count = (int) snapshot.getChildrenCount();
                        if (count < MIN_QUESTIONS) count = MIN_QUESTIONS;
                        updateSeekBar(count);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {}
                });
    }

    String getStars(float rating) {
        int fullStars = (int) rating;
        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            if (i < fullStars) {
                stars.append("★");
            } else {
                stars.append("☆");
            }
        }
        return stars.toString();
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