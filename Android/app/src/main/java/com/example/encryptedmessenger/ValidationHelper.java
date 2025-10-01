package com.example.encryptedmessenger;

import java.util.regex.Pattern;

public class ValidationHelper {
    // Regex patterns
    private static final Pattern USERNAME_REGEX = Pattern.compile("^[A-Za-z0-9]*$");
    private static final Pattern ROOM_REGEX = Pattern.compile("^[A-Za-z0-9]*$");
    private static final Pattern PASSWORD_REGEX = Pattern.compile("^[A-Za-z0-9]*$");

    /**
     * Validates an input against regex
     *
     * @param text The text.
     * @param validationType The validation type ('username', 'room', or 'password').
     * @return If it passes the validation.
     */
    public static boolean inputValidate(String text, String validationType) {
        boolean value = false;

        switch (validationType) {
            case "username":
                value = USERNAME_REGEX.matcher(text).matches();
                break;
            case "room":
                value = ROOM_REGEX.matcher(text).matches();
                break;
            case "password":
                value = PASSWORD_REGEX.matcher(text).matches();
                break;
        }
        return value;
    }

    /**
     * Validates that text length is within max length.
     *
     * @param text The text.
     * @param length The length of the text.
     * @return If it passes the validation.
     */
    public static boolean lengthValidate(String text, int length) {
        return text.length() <= length;
    }
}
