package com.example.encryptedmessenger;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
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
    String SERVER_IP = AppConfig.getServerIp();
    int SERVER_PORT = AppConfig.getServerPort();

    // Setup config pulled from build settings
    String MESSAGE_PREFIX = AppConfig.getMessagePrefix();
    String AAD_STR = AppConfig.getAadStr();
    String SYSTEM_TAG = AppConfig.getSystemTag();

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

    // Vars
    private String ROOM;
    private String USERNAME;
    private volatile boolean isRunning = true;

    @Override
    protected void onResume() {
        super.onResume();
        MessageListenerService.isChatVisible = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        MessageListenerService.isChatVisible = false;
    }

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

        // Get login data from Intent extras or SharedPreferences fallback
        Intent intent = getIntent();
        var prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);

        USERNAME = intent.getStringExtra("USERNAME");
        ROOM = intent.getStringExtra("ROOM");
        String PASSPHRASE = intent.getStringExtra("PASSWORD");

        // If activity launched from notification, fallback to saved login details
        if (USERNAME == null || ROOM == null || PASSPHRASE == null) {
            USERNAME = prefs.getString("username", null);
            ROOM = prefs.getString("room", null);
            PASSPHRASE = prefs.getString("password", null);
        }

        // If still missing something, fail
        if (USERNAME == null || ROOM == null || PASSPHRASE == null) {
            finish(); // nothing usable, go back to login
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }

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
            isRunning = false;
            try {
                if (writer != null && key != null && socket != null && socket.isConnected()) {
                    // Send leave message
                    sendSystemMessage(USERNAME + " has left the chat room");
                    Thread.sleep(100);
                }

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
                isRunning = false;
                new Thread(() -> {
                    try {
                        if (writer != null && key != null && socket != null && socket.isConnected()) {
                            sendSystemMessage(USERNAME + " has left the chat room");
                            Thread.sleep(100);
                        }
                        if (socket != null && !socket.isClosed()) socket.close();
                    } catch (Exception ignored) {}

                    runOnUiThread(() -> {
                        Intent backIntent = new Intent(MainActivity.this, LoginActivity.class);
                        backIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(backIntent);
                        finish();
                    });
                }).start();
            }
        });
    }

    private void connectToServer() {
        new Thread(() -> {
            while (isRunning) {
                try {
                    Log.d("DEBUG", "Connecting to " + SERVER_IP + ":" + SERVER_PORT);
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

                    // Send join message
                    sendSystemMessage(USERNAME + " has entered the chat room");

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
                if (isRunning) {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ignored) {}
                }
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
     * Sends a system message.
     * <p>
     * Sends a system message with a unique prefix that prevents user spoofing.
     * </p>
     *
     * @param content The system message text
     */
    private void sendSystemMessage(String content) {
        sendEncrypted(SYSTEM_TAG + content);
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

                boolean isSystemMessage = text.startsWith(SYSTEM_TAG);
                if (isSystemMessage) {
                    text = text.substring(SYSTEM_TAG.length());
                }

                // Broadcast message
                Intent msgIntent = new Intent("NEW_MESSAGE_RECEIVED");
                msgIntent.putExtra("message", text);
                msgIntent.putExtra("room", ROOM);
                msgIntent.putExtra("isSystemMessage", isSystemMessage);
                LocalBroadcastManager.getInstance(this).sendBroadcast(msgIntent);

                // Display decrypted message and scroll to bottom
                String finalText = text;
                boolean finalIsSystemMessage = isSystemMessage;
                runOnUiThread(() -> appendMessage(finalText, finalIsSystemMessage));
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
    private void appendMessage(String message, boolean isSystem) {
        runOnUiThread(() -> {
            SpannableString styledMessage;

            if (isSystem) {
                // System message
                styledMessage = new SpannableString("*" + message + "*\n");
                styledMessage.setSpan(
                        new ForegroundColorSpan(Color.GRAY),
                        0,
                        styledMessage.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
                styledMessage.setSpan(
                        new StyleSpan(Typeface.ITALIC),
                        0,
                        styledMessage.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            } else {
                // Normal message
                styledMessage = new SpannableString(message + "\n");
            }

            chatBox.append(styledMessage);
            scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
        });
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

        // Stop reconnect loop
        isRunning = false;

        try {
            // Send leave message
            if (writer != null && key != null && socket != null && socket.isConnected()) {
                sendSystemMessage(USERNAME + " has left the chat room");

                // Delay to make sure it sends
                Thread.sleep(100);
            }

            // Close network socket
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (Exception ignored) {}

        setDisconnected();
    }
}
