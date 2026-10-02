package com.mela.ussdrunner

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class HomeFlowTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun showsAppNameOnLaunch() {
        rule.onNodeWithText("Mela USSD Runner").assertIsDisplayed()
    }
}
