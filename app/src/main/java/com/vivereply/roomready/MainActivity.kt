package com.vivereply.roomready

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.vivereply.roomready.ui.RoomReadyApp
import com.vivereply.roomready.ui.theme.RoomReadyTheme
import java.util.Locale

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen()
    super.onCreate(savedInstanceState)

    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
    )

    val initialLang = resources.configuration.locales[0]?.language ?: "en"

    setContent {
      var selectedLanguageTag by rememberSaveable { mutableStateOf(initialLang) }
      val baseContext = this@MainActivity
      val localizedContext =
        remember(selectedLanguageTag, baseContext) {
          baseContext.withLocale(selectedLanguageTag)
        }

      CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides localizedContext.resources.configuration,
      ) {
        RoomReadyTheme {
          RoomReadyApp(
            graph = (application as RoomReadyApplication).graph,
            currentLanguageTag = selectedLanguageTag,
            onLanguageSelected = { tag -> selectedLanguageTag = tag },
          )
        }
      }
    }
  }
}

private fun Context.withLocale(languageTag: String): Context {
  val locale = Locale.forLanguageTag(languageTag)
  val config = Configuration(resources.configuration)
  config.setLocale(locale)
  return createConfigurationContext(config)
}
