package com.example.quickstage

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()
    
    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.CAMERA)

    @Test
    fun loginAndNavigateToTickets() {
        // 1. Verify Login Screen is displayed
        composeTestRule.onNodeWithText("Admin Login").assertIsDisplayed()

        // 2. Enter Password
        composeTestRule.onNodeWithText("Password").performTextInput("admin123")

        // 3. Click Login
        composeTestRule.onNodeWithText("Login").performClick()

        // 4. Verify Tickets Screen is displayed
        composeTestRule.onNodeWithText("Tickets").assertIsDisplayed()
    }
}
