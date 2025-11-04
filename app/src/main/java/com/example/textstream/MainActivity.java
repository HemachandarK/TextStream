package com.example.textstream;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.widget.SearchView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import androidx.activity.EdgeToEdge;

public class MainActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private RadioGroup rgRole;
    private RadioButton rbStudent, rbProfessor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        // Initialize views
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        rgRole = findViewById(R.id.rgRole);
        rbStudent = findViewById(R.id.rbStudent);
        rbProfessor = findViewById(R.id.rbProfessor);

        // Login button click listener
        btnLogin.setOnClickListener(view -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(MainActivity.this, "Please enter all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Determine role
            String role = rbStudent.isChecked() ? "Student" : "Professor";

            // Hardcoded credentials for demo
            if (role.equals("Student") && email.equals("stud") && password.equals("1234")) {
                Toast.makeText(MainActivity.this, "Student Login Successful", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(MainActivity.this, StudentHome.class));
                finish();
            } else if (role.equals("Professor") && email.equals("prof") && password.equals("1234")) {
                Toast.makeText(MainActivity.this, "Professor Login Successful", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(MainActivity.this, StaffAssignmentActivity.class));
                finish();
            } else {
                Toast.makeText(MainActivity.this, "Invalid Credentials", Toast.LENGTH_SHORT).show();
            }
        });
    }

}