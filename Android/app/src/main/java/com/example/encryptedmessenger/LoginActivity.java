package com.example.encryptedmessenger;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText usernameInput;
    private EditText roomInput;
    private EditText passwordInput;
    private Button connectButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        usernameInput = findViewById(R.id.usernameInput);
        roomInput = findViewById(R.id.roomInput);
        passwordInput = findViewById(R.id.passwordInput);
        connectButton = findViewById(R.id.connectButton);

        connectButton.setOnClickListener(v -> {
            String username = usernameInput.getText().toString().trim();
            String room = roomInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (username.isEmpty() || room.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.putExtra("USERNAME", username);
            intent.putExtra("ROOM", room);
            intent.putExtra("PASSWORD", password);
            startActivity(intent);
            finish();
        });
    }
}
