package com.example.bluechat;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MessageDao {
    @Insert
    void insert(MessageEntity message);

    @Query("SELECT * FROM messages WHERE deviceAddress = :deviceAddress ORDER BY timestamp ASC")
    List<MessageEntity> getMessagesForDevice(String deviceAddress);

    @Query("DELETE FROM messages WHERE deviceAddress = :deviceAddress")
    void deleteMessagesForDevice(String deviceAddress);
}
