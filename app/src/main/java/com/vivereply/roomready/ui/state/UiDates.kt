package com.vivereply.roomready.ui.state

import java.text.DateFormat
import java.util.Date
import java.util.Locale

/** Locale-aware timestamps honouring the app's selected language. */
object UiDates {
  fun dateTime(languageTag: String, atEpochMillis: Long): String =
    DateFormat.getDateTimeInstance(
      DateFormat.SHORT,
      DateFormat.SHORT,
      Locale.forLanguageTag(languageTag),
    ).format(Date(atEpochMillis))
}
