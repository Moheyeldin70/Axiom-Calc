package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h915dp")
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Axiom Calc", appName)
    }

    @Test
    fun `calculator computes expression and switches tabs`() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("key_2").performClick()
        composeRule.onNodeWithTag("key_add").performClick()
        composeRule.onNodeWithTag("key_3").performClick()
        composeRule.onNodeWithTag("key_equals").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("calculator_result_text")
            .assertIsDisplayed()
            .assertTextContains("5")

        // Switch to Solve tab and solve default equation
        composeRule.onNodeWithTag("nav_solve").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("solve_equation_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("root_result_0").assertIsDisplayed()

        // Switch to Graph tab
        composeRule.onNodeWithTag("nav_graph").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("graph_canvas").assertIsDisplayed()

        // Switch to Units tab
        composeRule.onNodeWithTag("nav_units").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("converter_result_text").assertIsDisplayed()
    }
}
