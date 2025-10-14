package com.example.encryptedmessenger;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioAttributes;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class MessageListenerService extends Service {
    private static final String CHANNEL_ID = "message_listener_channel";

    public static boolean isChatVisible = false;
    private BroadcastReceiver messageReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();

        // Foreground "service running" notification (silent)
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setSilent(true)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setContentTitle("Encrypted Messenger")
                .setContentText("Listening for new messages")
                .build();

        startForeground(1, notification);

        // Register to receive messages from MainActivity
        messageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("NEW_MESSAGE_RECEIVED".equals(intent.getAction())) {
                    String message = intent.getStringExtra("message");
                    String room = intent.getStringExtra("room");
                    boolean isSystemMessage = intent.getBooleanExtra("isSystemMessage", false);

                    // Only notify if chat is not visible and not system messages
                    if (!isChatVisible && !isSystemMessage) {
                        showNewMessageNotification(room, message);
                    }
                }
            }
        };
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(messageReceiver, new IntentFilter("NEW_MESSAGE_RECEIVED"));
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Message Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifies you when new chat messages arrive");
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setSound(
                    Settings.System.DEFAULT_NOTIFICATION_URI,
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void showNewMessageNotification(String room, String message) {
        var prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        String username = prefs.getString("username", "");
        String password = prefs.getString("password", "");

        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("ROOM", room);
        intent.putExtra("ROOM", room);
        intent.putExtra("PASSWORD", password);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("New message in room " + room)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setDefaults(Notification.DEFAULT_ALL);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (messageReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
