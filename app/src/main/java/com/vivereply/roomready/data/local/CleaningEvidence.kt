package com.vivereply.roomready.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import com.vivereply.roomready.domain.model.CleaningEvidence

@Entity(tableName = "cleaning_evidence")
data class CleaningEvidenceEntity(
  @PrimaryKey val unitId: String,
  val photoRef: String?,
  val skipReason: String?,
  val finishedBy: String,
  val finishedAt: Long,
) {
  fun toDomain(): CleaningEvidence =
    CleaningEvidence(
      unitId = unitId,
      photoRef = photoRef,
      skipReason = skipReason,
      finishedBy = finishedBy,
      finishedAtEpochMillis = finishedAt,
    )

  companion object {
    fun fromDomain(evidence: CleaningEvidence): CleaningEvidenceEntity =
      CleaningEvidenceEntity(
        unitId = evidence.unitId,
        photoRef = evidence.photoRef,
        skipReason = evidence.skipReason,
        finishedBy = evidence.finishedBy,
        finishedAt = evidence.finishedAtEpochMillis,
      )
  }
}

@Dao
interface CleaningEvidenceDao {
  @Upsert
  suspend fun upsert(evidence: CleaningEvidenceEntity)

  @Query("SELECT * FROM cleaning_evidence WHERE unitId = :unitId")
  suspend fun getForUnit(unitId: String): CleaningEvidenceEntity?

  @Query("DELETE FROM cleaning_evidence WHERE unitId = :unitId")
  suspend fun clearUnit(unitId: String)
}
