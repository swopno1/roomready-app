package com.vivereply.roomready

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.vivereply.roomready.notifications.AlarmScheduler
import com.vivereply.roomready.notifications.RoomReadyNotifications
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
  private val notificationPermission =
    registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

  override fun onCreate(savedInstanceState: Bundle?) {
    val splashStart = SystemClock.uptimeMillis()
    val splash = installSplashScreen()
    // Let the 800ms mark animation read; cold start usually exceeds this anyway.
    splash.setKeepOnScreenCondition { SystemClock.uptimeMillis() - splashStart < 800 }
    super.onCreate(savedInstanceState)

    // Local daily alarms; posting no-ops until the permission is granted.
    RoomReadyNotifications.ensureChannel(this)
    AlarmScheduler.scheduleAll(this)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
      checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
      PackageManager.PERMISSION_GRANTED
    ) {
      notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

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
