package com.daycare.sleepcheck.log

import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.annotation.StringRes
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import com.daycare.sleepcheck.log.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

@RunWith(AndroidJUnit4::class)
class CriticalPathTest {
    private val composeRule = createAndroidComposeRule<MainActivity>()
    @get:Rule val ruleChain: TestRule = RuleChain
        .outerRule(databaseResetRule())
        .around(composeRule)

    private fun databaseResetRule(): TestRule = object : TestRule {
        override fun apply(base: Statement, description: Description): Statement = object : Statement() {
            override fun evaluate() {
                resetDatabase()
                base.evaluate()
            }
        }
    }

    private fun resetDatabase() {
        val db = AppDatabase.create(ApplicationProvider.getApplicationContext<Context>())
        runBlocking(Dispatchers.IO) { db.clearAllTables() }
        db.close()
    }

    @Test fun startDueWholeRoomExceptionCompleteHistory() {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(text(R.string.setup_title)).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasSetTextAction(), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
        scrollTo(R.string.save_setup)
        node(R.string.save_setup).assertIsNotEnabled()
        input(R.string.facility_name_label, MockTestFixtures.facility)
        input(R.string.room_name_label, MockTestFixtures.room)
        input(R.string.staff_name_label, MockTestFixtures.staff)
        input(R.string.child_name_label, MockTestFixtures.firstChild)
        input(R.string.interval_minutes, MockTestFixtures.intervalMinutes)
        scrollTo(R.string.save_setup)
        node(R.string.save_setup).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(text(R.string.start_session)).fetchSemanticsNodes().isNotEmpty()
        }
        node(R.string.nav_more).performClick()
        node(R.string.people).performClick()
        input(R.string.add_child, MockTestFixtures.secondChild)
        composeRule.onNodeWithContentDescription(text(R.string.add_child)).performClick()
        composeRule.onNodeWithText(MockTestFixtures.secondChild).assertExists()
        composeRule.onNodeWithContentDescription(text(R.string.back)).performClick()
        composeRule.onNodeWithText(MockTestFixtures.facility).assertExists()
        node(R.string.start_session).performClick()
        node(R.string.whole_room_check).assertExists()
        node(R.string.complete_check).assertIsNotEnabled()
        scrollTo(R.string.check_confirmation_hint)
        node(R.string.check_confirmation_hint).assertExists()
        check(R.string.direct_visual_confirmation).performScrollTo().performClick()
        check(R.string.direct_visual_confirmation).assertIsOn()
        check(R.string.exception_observation).performScrollTo().performClick()
        check(R.string.exception_observation).assertIsOn()
        input(R.string.notes_label, MockTestFixtures.notes)
        node(R.string.complete_check).assertIsEnabled()
        node(R.string.complete_check).performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText(text(R.string.check_saved), substring = true)).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNode(hasText(text(R.string.check_saved), substring = true)).assertExists()
        node(R.string.active_sleep_session).assertExists()
        composeRule.runOnUiThread { composeRule.activity.recreate() }
        composeRule.waitForIdle()
        composeRule.onNodeWithText(MockTestFixtures.facility).assertExists()
        node(R.string.nav_history).performClick()
        node(R.string.history).assertExists()
        scrollToText(MockTestFixtures.room)
        composeRule.onNode(hasText(MockTestFixtures.room, substring = true)).assertExists()
        scrollToText(MockTestFixtures.notes)
        composeRule.onNodeWithText(MockTestFixtures.notes).assertExists()
        scrollTo(R.string.add_correction)
        node(R.string.add_correction).performClick()
        input(R.string.correction_reason, MockTestFixtures.correctionReason)
        node(R.string.save_correction).assertIsEnabled()
        node(R.string.save_correction).performClick()
        composeRule.waitUntil(timeoutMillis = 15_000) { correctionExistsInDatabase() }
        scrollToText(MockTestFixtures.correctionReason)
        composeRule.onNode(hasText(MockTestFixtures.correctionReason, substring = true)).assertExists()
        node(R.string.nav_home).performClick()
        node(R.string.nav_history).performClick()
        scrollToText(MockTestFixtures.notes)
        composeRule.onNodeWithText(MockTestFixtures.notes).assertExists()
    }

    private fun text(@StringRes id: Int): String = composeRule.activity.getString(id)

    private fun node(@StringRes id: Int): SemanticsNodeInteraction =
        composeRule.onNodeWithText(text(id))

    private fun check(@StringRes id: Int): SemanticsNodeInteraction =
        composeRule.onNodeWithContentDescription(text(id))

    private fun input(@StringRes labelId: Int, value: String) {
        val label = text(labelId)
        val fieldMatcher = hasSetTextAction() and hasAnyDescendant(hasText(label, substring = true))
        if (labelId == R.string.correction_reason) {
            composeRule.waitUntil(timeoutMillis = 15_000) {
                composeRule.onAllNodes(fieldMatcher, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
            }
        }
        val matchingFields = composeRule.onAllNodes(fieldMatcher, useUnmergedTree = true).fetchSemanticsNodes()
        if (matchingFields.isNotEmpty()) {
            composeRule.onNode(fieldMatcher, useUnmergedTree = true).performTextInput(value)
            return
        }
        scrollTo(labelId)
        composeRule.onNode(
            fieldMatcher,
            useUnmergedTree = true,
        ).performTextInput(value)
    }

    private fun scrollTo(@StringRes id: Int) {
        scrollToText(text(id))
    }

    private fun scrollToText(target: String) {
        val scrollNodes = composeRule.onAllNodes(hasScrollToNodeAction()).fetchSemanticsNodes()
        if (scrollNodes.isNotEmpty()) {
            composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(target, substring = true))
        }
    }

    private fun correctionExistsInDatabase(): Boolean {
        val db = AppDatabase.create(ApplicationProvider.getApplicationContext<Context>())
        return try {
            runBlocking(Dispatchers.IO) {
                db.auditDao().all().any { it.correctionReason == MockTestFixtures.correctionReason }
            }
        } finally {
            db.close()
        }
    }
}
