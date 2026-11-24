package com.example.trivia;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CreateEditQuizActivity extends AppCompatActivity {

    TextView tvTitle;
    EditText etQuizName;
    Button btnSaveQuiz, btnCancel, btnAddQuestion;
    LinearLayout questionsContainer;
    DatabaseReference mDatabase;
    FirebaseAuth mAuth;
    String mode = "create";
    String quizName = "";
    String quizId = "";
    String role = "user";
    String userId = "";

    ArrayList<View> questionViews = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_edit_quiz);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance("https://triviaapp-61460-default-rtdb.europe-west1.firebasedatabase.app").getReference();

        tvTitle = findViewById(R.id.tvTitle);
        etQuizName = findViewById(R.id.etQuizName);
        btnSaveQuiz = findViewById(R.id.btnSaveQuiz);
        btnCancel = findViewById(R.id.btnCancel);
        btnAddQuestion = findViewById(R.id.btnAddQuestion);
        questionsContainer = findViewById(R.id.questionsContainer);

        setClickAnimation(btnSaveQuiz);
        setClickAnimation(btnCancel);
        setClickAnimation(btnAddQuestion);

        mode = getIntent().getStringExtra("mode");
        quizName = getIntent().getStringExtra("quizName");
        quizId = getIntent().getStringExtra("quizId");
        role = getIntent().getStringExtra("role");
        if (role == null) role = "user";
        userId = mAuth.getCurrentUser().getUid();

        if (mode != null && mode.equals("edit")) {
            tvTitle.setText("Edit Quiz");
            loadExistingQuiz();
        } else {
            tvTitle.setText("Create Quiz");
            addQuestionView("", "", "", "");
            addQuestionView("", "", "", "");
            addQuestionView("", "", "", "");
        }

        btnAddQuestion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addQuestionView("", "", "", "");
            }
        });

        btnSaveQuiz.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveQuiz();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    void addQuestionView(String question, String correctAnswer, String wrong1, String wrong2) {
        int questionNumber = questionViews.size() + 1;

        LinearLayout questionLayout = new LinearLayout(this);
        questionLayout.setOrientation(LinearLayout.VERTICAL);
        questionLayout.setPadding(0, 0, 0, 16);

        // Question number label
        TextView tvQuestion = new TextView(this);
        tvQuestion.setText("Question " + questionNumber + ":");
        tvQuestion.setTextSize(18);
        tvQuestion.setTextColor(0xFFFFFFFF);
        tvQuestion.setTypeface(null, Typeface.BOLD);
        tvQuestion.setPadding(0, 0, 0, 8);
        questionLayout.addView(tvQuestion);

        // Question text input
        EditText etQuestion = new EditText(this);
        etQuestion.setHint("Enter question");
        etQuestion.setHintTextColor(0xFFCCCCCC);
        etQuestion.setTextColor(0xFFFFFFFF);
        etQuestion.getBackground().mutate().setColorFilter(0xFFFFFFFF, PorterDuff.Mode.SRC_ATOP);
        etQuestion.setPadding(0, 8, 0, 8);
        etQuestion.setText(question);
        questionLayout.addView(etQuestion);

        // Correct answer input
        EditText etCorrect = new EditText(this);
        etCorrect.setHint("Correct answer");
        etCorrect.setHintTextColor(0xFF66BB6A);
        etCorrect.setTextColor(0xFFFFFFFF);
        etCorrect.getBackground().mutate().setColorFilter(0xFF66BB6A, PorterDuff.Mode.SRC_ATOP);
        etCorrect.setPadding(0, 8, 0, 8);
        etCorrect.setText(correctAnswer);
        questionLayout.addView(etCorrect);

        // Wrong answer inputs
        EditText etWrong1 = new EditText(this);
        etWrong1.setHint("Wrong answer 1");
        etWrong1.setHintTextColor(0xFFEF5350);
        etWrong1.setTextColor(0xFFFFFFFF);
        etWrong1.getBackground().mutate().setColorFilter(0xFFEF5350, PorterDuff.Mode.SRC_ATOP);
        etWrong1.setPadding(0, 8, 0, 8);
        etWrong1.setText(wrong1);
        questionLayout.addView(etWrong1);

        EditText etWrong2 = new EditText(this);
        etWrong2.setHint("Wrong answer 2");
        etWrong2.setHintTextColor(0xFFEF5350);
        etWrong2.setTextColor(0xFFFFFFFF);
        etWrong2.getBackground().mutate().setColorFilter(0xFFEF5350, PorterDuff.Mode.SRC_ATOP);
        etWrong2.setPadding(0, 8, 0, 8);
        etWrong2.setText(wrong2);
        questionLayout.addView(etWrong2);

        // Delete button with minimum 3 questions check
        Button btnDelete = new Button(this);
        btnDelete.setText("Delete Question");
        btnDelete.setTextColor(0xFFFFFFFF);
        btnDelete.setBackgroundTintList(ColorStateList.valueOf(0xFFEF5350));
        btnDelete.setPadding(0, 8, 0, 8);
        setClickAnimation(btnDelete);
        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (questionViews.size() <= 3) {
                    Toast.makeText(CreateEditQuizActivity.this,
                            "Cannot delete! Minimum 3 questions required.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                questionsContainer.removeView(questionLayout);
                questionViews.remove(questionLayout);
                updateQuestionNumbers();
            }
        });
        questionLayout.addView(btnDelete);

        // Divider line
        View divider = new View(this);
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 2));
        divider.setBackgroundColor(0x55FFFFFF);
        questionLayout.addView(divider);

        questionsContainer.addView(questionLayout);
        questionViews.add(questionLayout);
    }

    void updateQuestionNumbers() {
        for (int i = 0; i < questionViews.size(); i++) {
            LinearLayout layout = (LinearLayout) questionViews.get(i);
            TextView tvNum = (TextView) layout.getChildAt(0);
            tvNum.setText("Question " + (i + 1) + ":");
        }
    }

    void loadExistingQuiz() {
        String path = role.equals("admin") ?
                "my_quizzes/general/" + quizId :
                "my_quizzes/personal/" + userId + "/" + quizId;

        mDatabase.child(path).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                String name = snapshot.child("quizName").getValue(String.class);
                if (name != null) etQuizName.setText(name);

                for (DataSnapshot q : snapshot.child("questions").getChildren()) {
                    String question = q.child("question").getValue(String.class);
                    String correct = q.child("correctAnswer").getValue(String.class);
                    String wrong1 = q.child("wrongAnswer1").getValue(String.class);
                    String wrong2 = q.child("wrongAnswer2").getValue(String.class);

                    addQuestionView(
                            question != null ? question : "",
                            correct != null ? correct : "",
                            wrong1 != null ? wrong1 : "",
                            wrong2 != null ? wrong2 : ""
                    );
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(CreateEditQuizActivity.this,
                        "Error loading quiz!",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    void saveQuiz() {
        String name = etQuizName.getText().toString().trim();

        if (name.isEmpty()) {
            etQuizName.setError("Please enter a quiz name!");
            etQuizName.requestFocus();
            return;
        }

        if (questionViews.size() < 3) {
            Toast.makeText(this,
                    "Please add at least 3 questions!",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> quiz = new HashMap<>();
        quiz.put("quizName", name);
        Map<String, Object> questions = new HashMap<>();

        for (int i = 0; i < questionViews.size(); i++) {
            LinearLayout qLayout = (LinearLayout) questionViews.get(i);

            EditText etQ = (EditText) qLayout.getChildAt(1);
            EditText etCorrect = (EditText) qLayout.getChildAt(2);
            EditText etW1 = (EditText) qLayout.getChildAt(3);
            EditText etW2 = (EditText) qLayout.getChildAt(4);

            String q = etQ.getText().toString().trim();
            String correct = etCorrect.getText().toString().trim();
            String w1 = etW1.getText().toString().trim();
            String w2 = etW2.getText().toString().trim();

            if (q.isEmpty() || correct.isEmpty() || w1.isEmpty() || w2.isEmpty()) {
                Toast.makeText(this,
                        "Please fill all fields in question " + (i + 1),
                        Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> questionMap = new HashMap<>();
            questionMap.put("question", q);
            questionMap.put("correctAnswer", correct);
            questionMap.put("wrongAnswer1", w1);
            questionMap.put("wrongAnswer2", w2);
            questions.put("question" + (i + 1), questionMap);
        }

        quiz.put("questions", questions);

        // Save to admin general or user personal path
        DatabaseReference quizRef;
        if (role.equals("admin")) {
            quizRef = (mode.equals("edit") && quizId != null) ?
                    mDatabase.child("my_quizzes").child("general").child(quizId) :
                    mDatabase.child("my_quizzes").child("general").push();
        } else {
            quizRef = (mode.equals("edit") && quizId != null) ?
                    mDatabase.child("my_quizzes").child("personal").child(userId).child(quizId) :
                    mDatabase.child("my_quizzes").child("personal").child(userId).push();
        }

        quizRef.setValue(quiz).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(CreateEditQuizActivity.this,
                        "Quiz saved successfully!",
                        Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(CreateEditQuizActivity.this, MyQuizzesActivity.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(CreateEditQuizActivity.this,
                        "Failed to save quiz!",
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