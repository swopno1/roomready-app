package com.vivereply.roomready.domain.model

import androidx.annotation.StringRes
import com.vivereply.roomready.R

/**
 * The 8-step room turnover checklist, in fixed order.
 * Labels are verbatim per product contract; display text is keyed in strings.xml.
 */
enum class TurnoverStep(
  val order: Int,
  val key: String,
  @param:StringRes val labelRes: Int,
) {
  BED_LINEN(1, "bed_linen", R.string.checklist_step_bed_linen),
  TOWELS_TOILETRIES(2, "towels_toiletries", R.string.checklist_step_towels_toiletries),
  VACUUM_MOP(3, "vacuum_mop", R.string.checklist_step_vacuum_mop),
  BATHROOM_DEEP_CLEAN(4, "bathroom_deep_clean", R.string.checklist_step_bathroom_deep_clean),
  MINIBAR_RESTOCK(5, "minibar_restock", R.string.checklist_step_minibar_restock),
  AC_LIGHTS_SOCKETS(6, "ac_lights_sockets", R.string.checklist_step_ac_lights_sockets),
  BALCONY_TIDY(7, "balcony_tidy", R.string.checklist_step_balcony_tidy),
  FINAL_WALKTHROUGH(8, "final_walkthrough", R.string.checklist_step_final_walkthrough),
  ;

  companion object {
    val TOTAL: Int = entries.size

    fun fromKey(key: String?): TurnoverStep? = entries.firstOrNull { it.key == key }
  }
}

/** Checklist completion snapshot for one room unit. */
data class ChecklistProgress(
  val unitId: String,
  val done: Set<TurnoverStep>,
) {
  val total: Int = TurnoverStep.TOTAL
  val doneCount: Int = done.size
  val missingCount: Int = (total - doneCount).coerceAtLeast(0)
  val isComplete: Boolean = doneCount >= total
}
