package com.example.domain.model

/**
 * Room type (category) from the PMS bootstrap payload.
 * Inventory is never static: instances only come from PMS import.
 */
data class RoomType(
  /** Stable PMS identifier. */
  val id: String,
  /** Short property code, e.g. "DLX". */
  val code: String,
  val nameEn: String,
  val nameBn: String,
  val bedSummary: String,
  val baseOccupancy: Int,
)
