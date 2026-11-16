package com.vivereply.roomready

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class RoomReadyShellRobolectricTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun `all five tabs render designed empty states and brand lockups`() {
    composeTestRule.waitForIdle()

    // Ink-on-light brand lockup in top bar and bottom navigation bar
    composeTestRule.onNodeWithTag("logo_lockup_ink_on_light").assertIsDisplayed()
    composeTestRule.onNodeWithTag("bottom_navigation_bar").assertIsDisplayed()

    val routes = listOf("home", "rooms", "tickets", "activity", "profile")
    routes.forEach { route ->
      composeTestRule.onNodeWithTag("nav_item_$route").performClick()
      composeTestRule.waitForIdle()
      composeTestRule.onNodeWithTag("empty_artwork_$route").performScrollTo().assertIsDisplayed()
      composeTestRule.onNodeWithTag("empty_title_$route").performScrollTo().assertIsDisplayed()
      composeTestRule.onNodeWithTag("empty_copy_$route").performScrollTo().assertIsDisplayed()
      composeTestRule.onNodeWithTag("empty_action_$route").performScrollTo().assertIsDisplayed()
    }

    // White-on-ink lockup renders on Profile brand banner
    composeTestRule.onNodeWithTag("logo_lockup_white_on_ink").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun `switching language to Bengali updates navigation and empty state copy`() {
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("nav_item_profile").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("lang_chip_bn").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("হোম").assertIsDisplayed()
    composeTestRule.onNodeWithText("রুম").assertIsDisplayed()
    composeTestRule.onNodeWithText("হাউসকিপার প্রোফাইল সেটআপ করুন").performScrollTo().assertIsDisplayed()
  }
}
