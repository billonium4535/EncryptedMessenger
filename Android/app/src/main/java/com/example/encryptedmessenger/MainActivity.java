package com.example.encryptedmessenger;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity {

    // Server config
    private static final String SERVER_IP = "CHANGEME";
    private static final int SERVER_PORT = CHANGEME;

    // Setup config
    private static final String MESSAGE_PREFIX = "CHANGEME";
    // private static final String ROOM = "CHANGEME";
    // private static final String PASSPHRASE = "CHANGEME";
    private static final String AAD_STR = "CHANGEME";

    // UI
    private TextView chatBox;
    private EditText inputBox;
    private ScrollView scrollView;

    // Networking
    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;

    // Key
    private byte[] key;

    // User/session info
    private String USERNAME;
    private String ROOM;
    private String PASSPHRASE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get login data
        Intent intent = getIntent();
        String USERNAME = intent.getStringExtra("USERNAME");
        String ROOM = intent.getStringExtra("ROOM");
        String PASSPHRASE = intent.getStringExtra("PASSWORD");

        // UI references
        chatBox = findViewById(R.id.chatBox);
        inputBox = findViewById(R.id.inputBox);
        Button sendButton = findViewById(R.id.sendButton);
        scrollView = findViewById(R.id.scrollView);

        // Derive key
        try {
            key = EncryptionHelper.deriveRoomKey(ROOM, PASSPHRASE);
        } catch (Exception e) {
            chatBox.setText("[!] Key derivation failed: " + e.getMessage());
            return;
        }

        // Connect
        new Thread(() -> {
            try {
                socket = new Socket(SERVER_IP, SERVER_PORT);
                writer = new PrintWriter(socket.getOutputStream(), true);
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                runOnUiThread(() -> chatBox.append("\n[+] Connected to server"));

                String line;
                while ((line = reader.readLine()) != null) {
                    handleIncoming(line);
                }
            } catch (Exception e) {
                runOnUiThread(() -> chatBox.append("\n[!] Connection error: " + e.getMessage()));
            }
        }).start();

        // Send button
        sendButton.setOnClickListener(v -> {
            String msg = inputBox.getText().toString().trim();
            if (!msg.isEmpty()) {
                sendEncrypted(USERNAME + ": " + msg);
                inputBox.setText("");
            }
        });
    }

    private void sendEncrypted(String plaintext) {
        new Thread(() -> {
            try {
                if (writer != null && key != null) {
                    String payloadB64 = EncryptionHelper.encrypt(
                            key,
                            plaintext.getBytes(StandardCharsets.UTF_8),
                            AAD_STR.getBytes(StandardCharsets.UTF_8)
                    );
                    writer.println(MESSAGE_PREFIX + payloadB64);
                }
            } catch (Exception e) {
                runOnUiThread(() ->
                        chatBox.append("\n[!] Encrypt/send error: " + e.getMessage()));
            }
        }).start();
    }

    private void handleIncoming(String line) {
        if (line.startsWith(MESSAGE_PREFIX)) {
            String payloadB64 = line.substring(MESSAGE_PREFIX.length());
            try {
                byte[] pt = EncryptionHelper.decrypt(
                        key,
                        payloadB64,
                        AAD_STR.getBytes(StandardCharsets.UTF_8)
                );
                String text = new String(pt, StandardCharsets.UTF_8);
                runOnUiThread(() -> {
                    chatBox.append("\n" + text);
                    scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                });
            } catch (Exception ex) {
                runOnUiThread(() -> chatBox.append("\n[!] Failed to decrypt a message"));
            }
        } else {
            // Optional: show non-protocol lines (server logs, etc.)
            runOnUiThread(() -> {
                chatBox.append("\n" + line);
                scrollView.fullScroll(ScrollView.FOCUS_DOWN);
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try { if (socket != null) socket.close(); } catch (Exception ignored) {}
    }
}
