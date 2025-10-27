package com.example.bluechat;

import android.content.Context;
import androidx.room.Room;

public class DatabaseHelper {
    private static AppDatabase instance;

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                    AppDatabase.class, "bluechat_database")
                    .build();
        }
        return instance;
    }
}
