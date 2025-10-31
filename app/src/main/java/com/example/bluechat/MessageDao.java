package com.example.bluechat;

import androidx.paging.PagingSource;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface MessageDao {
    @Insert
    void insert(MessageEntity message);

    @Update
    void update(MessageEntity message);

    @Query("SELECT * FROM messages WHERE deviceAddress = :deviceAddress ORDER BY timestamp ASC")
    List<MessageEntity> getMessagesForDevice(String deviceAddress);

    // Pagination query for efficient loading
    @Query("SELECT * FROM messages WHERE deviceAddress = :deviceAddress ORDER BY timestamp ASC LIMIT :limit OFFSET :offset")
    List<MessageEntity> getMessagesPaged(String deviceAddress, int limit, int offset);

    @Query("SELECT COUNT(*) FROM messages WHERE deviceAddress = :deviceAddress")
    int getMessageCount(String deviceAddress);

    // Search functionality
    @Query("SELECT * FROM messages WHERE deviceAddress = :deviceAddress AND content LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    List<MessageEntity> searchMessages(String deviceAddress, String query);

    @Query("DELETE FROM messages WHERE deviceAddress = :deviceAddress")
    void deleteMessagesForDevice(String deviceAddress);

    // Typing indicators
    @Query("UPDATE messages SET isTyping = :isTyping WHERE deviceAddress = :deviceAddress AND isTyping = 1")
    void updateTypingStatus(String deviceAddress, boolean isTyping);

    // Reactions
    @Query("UPDATE messages SET reactions = :reactions WHERE id = :messageId")
    void updateReactions(long messageId, String reactions);
}
