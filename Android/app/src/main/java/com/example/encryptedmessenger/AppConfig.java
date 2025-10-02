package com.example.encryptedmessenger;

import android.content.Context;

import java.io.InputStream;
import java.util.Properties;

public class AppConfig {
    private static final Properties props = new Properties();

    public static void init(Context context) {
        try {
            InputStream is = context.getAssets().open("config.properties");
            props.load(is);
        } catch (Exception ignored) {}
    }

    public static String getServerIp() {
        return props.getProperty("SERVER_IP");
    }

    public static int getServerPort() {
        try {
            return Integer.parseInt(props.getProperty("SERVER_PORT"));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static String getMessagePrefix() {
        return props.getProperty("MESSAGE_PREFIX");
    }

    public static String getAadStr() {
        return props.getProperty("AAD_STR");
    }
}
