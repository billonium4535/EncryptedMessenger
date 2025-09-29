package com.example.encryptedmessenger;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;


/**
 * MainActivity handles the UI and networking for a secure encrypted chat.
 * <p>
 * It connects to a server, encrypts messages before sending, and decrypts incoming messages.
 * </p>
 */
public class MainActivity extends AppCompatActivity {

    // Server config pulled from build settings
    String SERVER_IP = BuildConfig.SERVER_IP;
    int SERVER_PORT = BuildConfig.SERVER_PORT;

    // Setup config pulled from build settings
    String MESSAGE_PREFIX = BuildConfig.MESSAGE_PREFIX;
    String AAD_STR = BuildConfig.AAD_STR;

    // UI
    private TextView chatBox;
    private EditText inputBox;
    private ScrollView scrollView;
    private TextView connectionStatusText;

    // Networking
    private Socket socket;

    // Reader and writer
    private PrintWriter writer;
    private BufferedReader reader;

    // Key
    private byte[] key;

    /**
     * Called when the activity is first created.
     * <p>
     * Sets up the UI, gets the user login info, derives encryption key, and connects to server.
     * </p>
     *
     * @param savedInstanceState Standard bundle containing activity state.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get login data from Intent extras
        Intent intent = getIntent();
        String USERNAME = intent.getStringExtra("USERNAME");
        String ROOM = intent.getStringExtra("ROOM");
        String PASSPHRASE = intent.getStringExtra("PASSWORD");

        // Room name and connection
        TextView roomNameText = findViewById(R.id.roomNameText);
        connectionStatusText = findViewById(R.id.connectionStatusText);

        // UI references
        chatBox = findViewById(R.id.chatBox);
        inputBox = findViewById(R.id.inputBox);
        scrollView = findViewById(R.id.scrollView);
        Button sendButton = findViewById(R.id.sendButton);
        ImageButton exitButton = findViewById(R.id.exitButton);

        // Show room name
        roomNameText.setText(ROOM);
        setDisconnected();

        // Exit button
        exitButton.setOnClickListener(v -> {
            try {
                if (socket != null && !socket.isClosed()) {
                    // Close connection
                    socket.close();
                }
            } catch (Exception ignored) {}

            // Return to login screen
            Intent backIntent = new Intent(MainActivity.this, LoginActivity.class);
            backIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(backIntent);
            finish();
        });

        // Derive key
        try {
            assert PASSPHRASE != null;
            key = EncryptionHelper.deriveRoomKey(ROOM, PASSPHRASE);
        } catch (Exception e) {

            // Show error if key derivation fails
            appendMessage(getString(R.string.error_key_derivation, e.getMessage()));
            return;
        }

        // Start connection thread
        new Thread(this::connectToServer).start();

        // Send message on button click
        sendButton.setOnClickListener(v -> {
            String msg = inputBox.getText().toString().trim();
            if (!msg.isEmpty()) {
                // Encrypt and send message
                sendEncrypted(USERNAME + ": " + msg);

                // Clear input box
                inputBox.setText("");
            }
        });

        // Handle system back press using OnBackPressedDispatcher
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                try {
                    if (socket != null && !socket.isClosed()) {
                        socket.close();
                    }
                } catch (Exception ignored) {}

                // Go back to login
                Intent backIntent = new Intent(MainActivity.this, LoginActivity.class);
                backIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(backIntent);
                finish();
            }
        });
    }

    private void connectToServer() {
        new Thread(() -> {
            while (true) {
                try {
                    // Show user reconnecting
                    runOnUiThread(this::setReconnecting);

                    // Connect to server
                    socket = new Socket();
                    socket.connect(new InetSocketAddress(SERVER_IP, SERVER_PORT), 5000);

                    // Set up writer
                    writer = new PrintWriter(socket.getOutputStream(), true);

                    // Set up reader
                    reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    // Connected
                    runOnUiThread(this::setConnected);

                    // Continuously read incoming messages
                    String line;
                    while ((line = reader.readLine()) != null) {
                        // Process incoming message
                        handleIncoming(line);
                    }

                    runOnUiThread(this::setDisconnected);
                } catch (Exception e) {
                    runOnUiThread(this::setDisconnected);
                }

                // Wait 5s
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ignored) {}
            }
        }).start();
    }

    private void setConnected() {
        connectionStatusText.setText(R.string.connected_status);
        connectionStatusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
    }

    private void setReconnecting() {
        connectionStatusText.setText(R.string.reconnecting_status);
        connectionStatusText.setTextColor(ContextCompat.getColor(this, R.color.reconnecting_orange));
    }

    private void setDisconnected() {
        connectionStatusText.setText(R.string.disconnected_status);
        connectionStatusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
    }


    /**
     * Encrypts a plaintext message and sends it to the server.
     * <p>
     * Runs in a separate thread to avoid blocking the UI.
     * </p>
     *
     * @param plaintext The plaintext message to send.
     */
    private void sendEncrypted(String plaintext) {
        new Thread(() -> {
            try {
                if (writer != null && key != null) {
                    // Encrypt the message using key and AAD, encode as Base64
                    String payloadB64 = EncryptionHelper.encrypt(
                            key,
                            plaintext.getBytes(StandardCharsets.UTF_8),
                            AAD_STR.getBytes(StandardCharsets.UTF_8)
                    );
                    // Send the encrypted message with protocol prefix
                    writer.println(MESSAGE_PREFIX + payloadB64);
                }
            } catch (Exception e) {
                // Show error if encryption or sending fails
                runOnUiThread(() ->
                        appendMessage("\n[!] Encrypt/send error: " + e.getMessage()));
            }
        }).start();
    }

    /**
     * Handles incoming messages from the server.
     * <p>
     * Decrypts messages with the protocol prefix and displays them in the chatBox.
     * </p>
     *
     * @param line The raw line received from the server.
     */
    private void handleIncoming(String line) {
        if (line.startsWith(MESSAGE_PREFIX)) {
            // Extract Base64 payload from protocol message
            String payloadB64 = line.substring(MESSAGE_PREFIX.length());
            try {
                // Decrypt message
                byte[] pt = EncryptionHelper.decrypt(
                        key,
                        payloadB64,
                        AAD_STR.getBytes(StandardCharsets.UTF_8)
                );
                String text = new String(pt, StandardCharsets.UTF_8);

                // Display decrypted message and scroll to bottom
                runOnUiThread(() -> appendMessage(text));
            } catch (Exception ex) {
                // Failed decryption, ignore silently

            }
        } else {
            // Show non-protocol lines (server logs, etc.)
            runOnUiThread(() -> appendMessage(line));
        }
    }

    /**
     * Appends a message to the chatBox and scrolls to the bottom automatically.
     */
    private void appendMessage(String message) {
        runOnUiThread(() -> {
            chatBox.append(message + "\n");
            scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
        });
    }

    /**
     * Called when the activity is destroyed.
     * <p>
     * Ensures socket connection is closed properly.
     * </p>
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            // Close network socket
            if (socket != null) socket.close();
        } catch (Exception ignored) {}
        setDisconnected();
    }
}
