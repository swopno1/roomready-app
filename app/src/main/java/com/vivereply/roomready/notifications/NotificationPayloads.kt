package com.vivereply.roomready.notifications

/**
 * Pure payload decisions for local-only notifications. No push, no server:
 * the receivers call [MorningSummary.build] / [WaitingInspection.build]
 * with counts read from the local database, and nothing posts when there
 * is no work to report. String resolution stays with the caller so the
 * receivers can use system-locale resources.
 */
object MorningSummary {
  data class Payload(val title: String, val body: String)

  /**
   * Builds the day-start payload, or null when every room is already
   * Ready (nothing worth waking a housekeeper for).
   */
  fun build(
    dirtyCount: Int,
    waitingCount: Int,
    title: String,
    dirtyLine: (Int) -> String,
    waitingLine: (Int) -> String,
  ): Payload? {
    if (dirtyCount <= 0 && waitingCount <= 0) return null
    val lines =
      buildList {
        if (dirtyCount > 0) add(dirtyLine(dirtyCount))
        if (waitingCount > 0) add(waitingLine(waitingCount))
      }
    return Payload(title = title, body = lines.joinToString("\n"))
  }
}

object WaitingInspection {
  data class Payload(val title: String, val body: String)

  /** Builds the supervisor nudge, or null when nothing waits. */
  fun build(waitingCount: Int, title: String, body: (Int) -> String): Payload? =
    if (waitingCount <= 0) {
      null
    } else {
      Payload(title = title, body = body(waitingCount))
    }
}
