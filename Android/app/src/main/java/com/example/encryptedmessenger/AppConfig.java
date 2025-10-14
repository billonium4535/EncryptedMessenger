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

    public static String getSystemTag() {
        return props.getProperty("SYSTEM_TAG");
    }

    public static String getDBName() {
        return props.getProperty("DB_NAME");
    }

    public static String getDBVersion() {
        return props.getProperty("DB_VERSION");
    }

    public static String getTableName() {
        return props.getProperty("TABLE_NAME");
    }

    public static String getColID() {
        return props.getProperty("COL_ID");
    }

    public static String getColRoom() {
        return props.getProperty("COL_ROOM");
    }

    public static String getColPassword() {
        return props.getProperty("COL_PASSWORD");
    }
}
