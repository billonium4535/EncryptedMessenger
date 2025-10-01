package com.example.encryptedmessenger;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;


/**
 * LoginActivity handles user input for logging into the encrypted chat.
 * <p>
 * Users provide a username, room name, and passphrase.
 * After validation, the activity launches MainActivity with the login details.
 * </p>
 */
public class LoginActivity extends AppCompatActivity {

    // UI
    private EditText usernameInput;
    private EditText roomInput;
    private EditText passwordInput;

    // Back button press time
    private long backPressedTime = 0;
    private static final int BACK_PRESS_INTERVAL = 2000;

    /**
     * Called when the activity is first created.
     * <p>
     * Initialises the UI and sets up the click listener for the connect button.
     * </p>
     *
     * @param savedInstanceState Standard bundle containing activity state.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set layout for login screen
        setContentView(R.layout.activity_login);

        // Initialise UI
        usernameInput = findViewById(R.id.usernameInput);
        roomInput = findViewById(R.id.roomInput);
        passwordInput = findViewById(R.id.passwordInput);
        Button connectButton = findViewById(R.id.connectButton);

        // Set listener for the connect button
        connectButton.setOnClickListener(v -> {
            // Retrieve user inputs and trim whitespace
            String username = usernameInput.getText().toString().trim();
            String room = roomInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            // Validation
            if (username.isEmpty() || room.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, getString(R.string.error_all_fields_required), Toast.LENGTH_SHORT).show();
                return;
            }

            if (!ValidationHelper.inputValidate(username, "username") ||
            !ValidationHelper.lengthValidate(username, 20)) {
                Toast.makeText(this, getString(R.string.error_invalid_username), Toast.LENGTH_SHORT).show();
                return;
            }

            if (!ValidationHelper.inputValidate(room, "room") ||
                    !ValidationHelper.lengthValidate(room, 20)) {
                Toast.makeText(this, getString(R.string.error_invalid_room), Toast.LENGTH_SHORT).show();
                return;
            }

            if (!ValidationHelper.inputValidate(password, "password") ||
                    !ValidationHelper.lengthValidate(password, 20)) {
                Toast.makeText(this, getString(R.string.error_invalid_password), Toast.LENGTH_SHORT).show();
                return;
            }

            // Create an intent to start MainActivity with login details
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.putExtra("USERNAME", username);
            intent.putExtra("ROOM", room);
            intent.putExtra("PASSWORD", password);

            // Launch MainActivity
            startActivity(intent);

            // Close LoginActivity so user cannot go back
            finish();
        });

        // Handle back button press logic
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (backPressedTime + BACK_PRESS_INTERVAL > System.currentTimeMillis()) {
                    // Exit app
                    finishAffinity();
                } else {
                    Toast.makeText(LoginActivity.this, getString(R.string.toast_press_back_again), Toast.LENGTH_SHORT).show();
                }
                backPressedTime = System.currentTimeMillis();
            }
        });
    }
}
