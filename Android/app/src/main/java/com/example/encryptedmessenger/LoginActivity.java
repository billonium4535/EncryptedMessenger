package com.example.encryptedmessenger;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


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

        // Initialise config
        AppConfig.init(this);

        // Set layout for login screen
        setContentView(R.layout.activity_login);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        1001);
            }
        }

        // Initialise UI
        usernameInput = findViewById(R.id.usernameInput);
        roomInput = findViewById(R.id.roomInput);
        passwordInput = findViewById(R.id.passwordInput);
        Button connectButton = findViewById(R.id.connectButton);

        // Load saved details
        loadSavedInputs();

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

            // Save details
            saveInputs(username, room, password);

            // Create an intent to start MainActivity with login details
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.putExtra("USERNAME", username);
            intent.putExtra("ROOM", room);
            intent.putExtra("PASSWORD", password);

            // Start background message listener
            Intent serviceIntent = new Intent(LoginActivity.this, MessageListenerService.class);
            ContextCompat.startForegroundService(LoginActivity.this, serviceIntent);

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

    /**
     * Save login inputs
     */
    private void saveInputs(String username, String room, String password) {
        getSharedPreferences("LoginPrefs", MODE_PRIVATE)
                .edit()
                .putString("username", username)
                .putString("room", room)
                .putString("password", password)
                .apply();
    }

    /**
     * Load saved inputs
     */
    private void loadSavedInputs() {
        var prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        usernameInput.setText(prefs.getString("username", ""));
        roomInput.setText(prefs.getString("room", ""));
        passwordInput.setText(prefs.getString("password", ""));
    }

    /**
     * TODO
     * Create a logout function and button
     */
    private void logout() {
        // Clear details
        getSharedPreferences("LoginPrefs", MODE_PRIVATE).edit().clear().apply();

        // Stop notification service
        stopService(new Intent(this, MessageListenerService.class));
    }
}
