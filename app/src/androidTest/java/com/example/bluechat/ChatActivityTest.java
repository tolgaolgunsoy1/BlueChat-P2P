package com.example.bluechat;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ChatActivityTest {

    @Rule
    public ActivityScenarioRule<ChatActivity> activityRule =
            new ActivityScenarioRule<>(ChatActivity.class);

    @Test
    public void testChatActivityLaunches() {
        // Check that the activity launches and basic UI elements are displayed
        onView(withId(R.id.message_edit_text)).check(matches(isDisplayed()));
        onView(withId(R.id.send_button)).check(matches(isDisplayed()));
        onView(withId(R.id.message_recycler_view)).check(matches(isDisplayed()));
    }

    @Test
    public void testMessageInput() {
        // Type a message in the input field
        onView(withId(R.id.message_edit_text))
                .perform(typeText("Test message"));

        // Verify the text was entered
        onView(withId(R.id.message_edit_text))
                .check(matches(withText("Test message")));
    }

    @Test
    public void testSendButtonClick() {
        // This test would require mocking Bluetooth connection
        // For now, just verify the button is clickable
        onView(withId(R.id.send_button))
                .check(matches(isDisplayed()))
                .perform(click());
    }
}
