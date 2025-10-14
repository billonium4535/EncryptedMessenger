package com.example.encryptedmessenger;

import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SavedLoginsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scrollView = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);
        scrollView.addView(layout);

        setContentView(scrollView);

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        Cursor cursor = dbHelper.getAllLogins();

        // Title for username section
        TextView usernameLabel = new TextView(this);
        usernameLabel.setText("Username:");                     // TODO
        usernameLabel.setTextSize(16f);
        usernameLabel.setPadding(0, 0, 0, 8);
        layout.addView(usernameLabel);

        // Username input
        EditText usernameInput = new EditText(this);
        usernameInput.setHint("Username");
        usernameInput.setText(getIntent().getStringExtra("USERNAME"));
        layout.addView(usernameInput);

        // Spacer
        TextView spacer = new TextView(this);
        spacer.setText("\nSaved Rooms:");
        spacer.setTextSize(18f);
        spacer.setPadding(0, 24, 0, 16);
        layout.addView(spacer);

        // Loop through saved logins
        while (cursor.moveToNext()) {
            String room = cursor.getString(cursor.getColumnIndexOrThrow("room"));
            String password = cursor.getString(cursor.getColumnIndexOrThrow("password"));

            // Container for each entry
            LinearLayout entryLayout = new LinearLayout(this);
            entryLayout.setOrientation(LinearLayout.VERTICAL);
            entryLayout.setPadding(24, 24, 24, 24);
            entryLayout.setBackgroundColor(0xFF1E1E1E);
            entryLayout.setElevation(4);
            LinearLayout.LayoutParams entryParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            entryParams.setMargins(0, 0, 0, 32);
            entryLayout.setLayoutParams(entryParams);

            // Room name
            TextView roomText = new TextView(this);
            roomText.setText("Room: " + room);                  // TODO
            roomText.setTextSize(16f);
            entryLayout.addView(roomText);

            // Password text
            TextView passwordText = new TextView(this);
            passwordText.setText("Password: " + password);      // TODO
            passwordText.setTextSize(14f);
            passwordText.setTextColor(0xFFAAAAAA);
            passwordText.setPadding(0, 8, 0, 16);
            entryLayout.addView(passwordText);

            // Horizontal layout for buttons
            LinearLayout buttonLayout = new LinearLayout(this);
            buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
            buttonLayout.setGravity(Gravity.END);

            // Connect button
            Button connectBtn = new Button(this);
            connectBtn.setText("Connect");                      // TODO
            connectBtn.setOnClickListener(v -> {
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra("USERNAME", usernameInput.getText().toString());
                intent.putExtra("ROOM", room);
                intent.putExtra("PASSWORD", password);
                startActivity(intent);
            });
            buttonLayout.addView(connectBtn);

            // Spacer between buttons
            TextView space = new TextView(this);
            space.setWidth(16);
            buttonLayout.addView(space);

            // Delete button
            Button deleteBtn = new Button(this);
            deleteBtn.setText("Delete");                        // TODO
            deleteBtn.setBackgroundColor(0xFFAA3333);
            deleteBtn.setTextColor(0xFFFFFFFF);
            deleteBtn.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("Delete Saved Login")
                        .setMessage("Are you sure you want to delete the saved login for \"" + room + "\"?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            dbHelper.deleteLogin(room);
                            Toast.makeText(this, "Deleted " + room, Toast.LENGTH_SHORT).show();
                            // Reload list
                            recreate();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
            buttonLayout.addView(deleteBtn);

            entryLayout.addView(buttonLayout);
            layout.addView(entryLayout);
        }
        cursor.close();
    }
}
