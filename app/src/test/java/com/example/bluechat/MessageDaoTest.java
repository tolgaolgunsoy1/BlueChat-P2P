package com.example.bluechat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class MessageDaoTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private AppDatabase database;
    private MessageDao messageDao;

    @Before
    public void setup() {
        Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        messageDao = database.messageDao();
    }

    @After
    public void teardown() {
        database.close();
    }

    @Test
    public void insertAndRetrieveMessage() {
        // Given
        MessageEntity message = new MessageEntity("Test message", true, "00:11:22:33:44:55");

        // When
        messageDao.insert(message);
        List<MessageEntity> messages = messageDao.getMessagesForDevice("00:11:22:33:44:55");

        // Then
        assertNotNull(messages);
        assertEquals(1, messages.size());
        assertEquals("Test message", messages.get(0).getContent());
        assertEquals(true, messages.get(0).isSent());
    }

    @Test
    public void searchMessages() {
        // Given
        MessageEntity message1 = new MessageEntity("Hello world", true, "00:11:22:33:44:55");
        MessageEntity message2 = new MessageEntity("Goodbye world", false, "00:11:22:33:44:55");
        messageDao.insert(message1);
        messageDao.insert(message2);

        // When
        List<MessageEntity> results = messageDao.searchMessages("00:11:22:33:44:55", "world");

        // Then
        assertNotNull(results);
        assertEquals(2, results.size());
    }

    @Test
    public void getMessageCount() {
        // Given
        MessageEntity message1 = new MessageEntity("Message 1", true, "00:11:22:33:44:55");
        MessageEntity message2 = new MessageEntity("Message 2", false, "00:11:22:33:44:55");
        messageDao.insert(message1);
        messageDao.insert(message2);

        // When
        int count = messageDao.getMessageCount("00:11:22:33:44:55");

        // Then
        assertEquals(2, count);
    }

    @Test
    public void getMessagesPaged() {
        // Given - Insert 5 messages
        for (int i = 0; i < 5; i++) {
            MessageEntity message = new MessageEntity("Message " + i, true, "00:11:22:33:44:55");
            messageDao.insert(message);
        }

        // When - Get first 3 messages
        List<MessageEntity> pagedMessages = messageDao.getMessagesPaged("00:11:22:33:44:55", 3, 0);

        // Then
        assertNotNull(pagedMessages);
        assertEquals(3, pagedMessages.size());
    }
}
