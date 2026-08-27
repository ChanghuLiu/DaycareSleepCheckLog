package com.daycare.sleepcheck.log

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import com.daycare.sleepcheck.log.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CriticalPathTest {
    @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()

    @Before fun resetDatabase() {
        val db = AppDatabase.create(ApplicationProvider.getApplicationContext<Context>())
        runBlocking(Dispatchers.IO) { db.clearAllTables() }
        db.close()
        composeRule.waitForIdle()
    }

    @Test fun startDueWholeRoomExceptionCompleteHistory() {
        composeRule.onNodeWithText("Facility name").performTextInput("Test Daycare")
        composeRule.onNodeWithText("First room").performTextInput("Nap Room")
        composeRule.onNodeWithText("Staff name").performTextInput("Staff One")
        composeRule.onNodeWithText("First child").performTextInput("Child One")
        composeRule.onNodeWithText("Check interval in minutes").performTextInput("15")
        composeRule.onNodeWithText("Save setup").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Start sleep session").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Start sleep session").performClick()
        composeRule.onNodeWithText("Whole-room check").assertExists()
        composeRule.onNodeWithContentDescription("I was physically present and completed a direct visual check of every sleeping child.").performClick()
        composeRule.onNodeWithContentDescription("Exception observed").performClick()
        composeRule.onNodeWithText("Complete check").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText("Check saved", substring = true)).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNode(hasText("Check saved", substring = true)).assertExists()
        composeRule.onNodeWithText("Active sleep sessions").assertExists()
        composeRule.onNodeWithText("Open inspector history").performClick()
        composeRule.onNodeWithText("Inspector history").assertExists()
    }
}
