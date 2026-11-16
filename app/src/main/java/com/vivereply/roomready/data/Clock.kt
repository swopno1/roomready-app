package com.vivereply.roomready.data

/** Testable clock; production uses [SystemClock]. */
interface Clock {
  fun nowEpochMillis(): Long
}

object SystemClock : Clock {
  override fun nowEpochMillis(): Long = System.currentTimeMillis()
}
