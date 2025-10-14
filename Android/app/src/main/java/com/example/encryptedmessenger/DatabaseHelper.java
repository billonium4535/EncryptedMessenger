package com.example.encryptedmessenger;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = AppConfig.getDBName();
    private static final int DB_VERSION = Integer.parseInt(AppConfig.getDBVersion());

    private static final String TABLE_NAME = AppConfig.getTableName();
    private static final String COL_ID = AppConfig.getColID();
    private static final String COL_ROOM = AppConfig.getColRoom();
    private static final String COL_PASSWORD = AppConfig.getColPassword();

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_NAME + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_ROOM + " TEXT UNIQUE, " +
                COL_PASSWORD + " TEXT)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public boolean insertLogin(String room, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_ROOM, room);
        cv.put(COL_PASSWORD, password);
        long result = db.insertWithOnConflict(TABLE_NAME, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        return result != -1;
    }

    public Cursor getAllLogins() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_NAME, null);
    }

    public boolean deleteLogin(String room) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete("saved_logins", "room=?", new String[]{room});
        return result > 0;
    }
}
