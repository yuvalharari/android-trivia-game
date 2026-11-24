package com.example.trivia;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

import android.widget.TextView;

public class MyQuizzesActivity extends AppCompatActivity {

    ListView listViewQuizzes;
    Button btnCreateQuiz, btnBackToMenu;
    ArrayList<String> quizList;
    ArrayList<String> quizIdList;
    ArrayAdapter<String> adapter;
    DatabaseReference mDatabase;
    FirebaseAuth mAuth;
    String role;
    String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_quizzes);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance("https://triviaapp-61460-default-rtdb.europe-west1.firebasedatabase.app").getReference();

        listViewQuizzes = findViewById(R.id.listViewQuizzes);
        btnCreateQuiz = findViewById(R.id.btnCreateQuiz);
        btnBackToMenu = findViewById(R.id.btnBackToMenu);

        setClickAnimation(btnCreateQuiz);
        setClickAnimation(btnBackToMenu);

        quizList = new ArrayList<>();
        quizIdList = new ArrayList<>();

        // Read role from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("TriviaPrefs", MODE_PRIVATE);
        role = prefs.getString("role", "user");
        userId = mAuth.getCurrentUser().getUid();

        // Custom adapter with white text
        adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                android.R.id.text1,
                quizList) {
            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView tv = view.findViewById(android.R.id.text1);
                tv.setTextColor(android.graphics.Color.WHITE);
                tv.setTextSize(18);
                return view;
            }
        };

        listViewQuizzes.setAdapter(adapter);

        loadQuizzes();

        listViewQuizzes.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedQuiz = quizList.get(position);
                String selectedQuizId = quizIdList.get(position);
                showQuizOptions(selectedQuiz, selectedQuizId, position);
            }
        });

        btnCreateQuiz.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MyQuizzesActivity.this, CreateEditQuizActivity.class);
                intent.putExtra("mode", "create");
                intent.putExtra("role", role);
                startActivity(intent);
            }
        });

        btnBackToMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MyQuizzesActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        });
    }

    void loadQuizzes() {
        quizList.clear();
        quizIdList.clear();

        // Admin loads general quizzes, user loads personal quizzes
        if (role.equals("admin")) {
            mDatabase.child("my_quizzes").child("general")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            for (DataSnapshot quiz : snapshot.getChildren()) {
                                String quizName = quiz.child("quizName").getValue(String.class);
                                if (quizName != null) {
                                    quizList.add(quizName);
                                    quizIdList.add(quiz.getKey());
                                }
                            }
                            adapter.notifyDataSetChanged();
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {
                            Toast.makeText(MyQuizzesActivity.this,
                                    "Error loading quizzes",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            mDatabase.child("my_quizzes").child("personal").child(userId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            for (DataSnapshot quiz : snapshot.getChildren()) {
                                String quizName = quiz.child("quizName").getValue(String.class);
                                if (quizName != null) {
                                    quizList.add(quizName);
                                    quizIdList.add(quiz.getKey());
                                }
                            }
                            adapter.notifyDataSetChanged();
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {
                            Toast.makeText(MyQuizzesActivity.this,
                                    "Error loading quizzes",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    void showQuizOptions(String quizName, String quizId, int position) {
        String[] options = {"Play", "Edit", "Delete"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(quizName);
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        Intent intent = new Intent(MyQuizzesActivity.this, GameSettingsActivity.class);
                        intent.putExtra("quizName", quizName);
                        intent.putExtra("quizId", quizId);
                        intent.putExtra("role", role);
                        intent.putExtra("isGeneralQuiz", role.equals("admin"));
                        startActivity(intent);
                        break;

                    case 1:
                        Intent editIntent = new Intent(MyQuizzesActivity.this, CreateEditQuizActivity.class);
                        editIntent.putExtra("mode", "edit");
                        editIntent.putExtra("quizName", quizName);
                        editIntent.putExtra("quizId", quizId);
                        editIntent.putExtra("role", role);
                        startActivity(editIntent);
                        break;

                    case 2:
                        confirmDelete(quizName, quizId, position);
                        break;
                }
            }
        });
        builder.show();
    }

    void confirmDelete(String quizName, String quizId, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Quiz");
        builder.setMessage("Are you sure you want to delete \"" + quizName + "\"?");
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Delete from correct Firebase path
                String path = role.equals("admin") ? "general" : "personal/" + userId;
                mDatabase.child("my_quizzes").child(path).child(quizId).removeValue();

                quizList.remove(position);
                quizIdList.remove(position);
                adapter.notifyDataSetChanged();
                Toast.makeText(MyQuizzesActivity.this,
                        "Quiz deleted!",
                        Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
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