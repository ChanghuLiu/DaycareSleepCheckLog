package com.daycare.sleepcheck.log

import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import com.daycare.sleepcheck.log.ui.SleepCheckTheme
import com.daycare.sleepcheck.log.ui.SuccessDialog
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertTrue

class SuccessDialogUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun purchaseSuccessDialogIsVisibleAndDismissible() {
        composeRule.activity.setContent {
            SleepCheckTheme {
                val visible = remember { mutableStateOf(true) }
                if (visible.value) {
                    SuccessDialog(
                        title = "Purchase successful",
                        body = "Daycare Pro is ready.",
                        onDismiss = { visible.value = false },
                        confirmText = "Done",
                    )
                }
            }
        }

        composeRule.onNodeWithText("Purchase successful").assertIsDisplayed()
        composeRule.onNodeWithText("Daycare Pro is ready.").assertIsDisplayed()
        composeRule.onNodeWithText("Done").performClick()
        composeRule.waitForIdle()
        assertTrue(composeRule.onAllNodesWithText("Purchase successful").fetchSemanticsNodes().isEmpty())
    }
}
