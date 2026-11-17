package com.vivereply.roomready.data.photos

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.exifinterface.media.ExifInterface
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.edit
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.max

/**
 * Local finish-photo storage. Photos never leave the device except through
 * the PMS sync channel (refs travel in the queue; bytes follow later).
 *
 * 2 GB device care: captures are downscaled to [MAX_DIMENSION] px, re-encoded
 * at JPEG quality 70, and decoded with inSampleSize so full frames are never
 * held in memory. Pending refs survive process death via plain prefs.
 */
class PhotoStore(context: Context) {
  private val appContext: Context = context.applicationContext
  private val dir: File = File(appContext.filesDir, PHOTOS_DIR).apply { mkdirs() }
  private val ticketDir: File = File(appContext.filesDir, TICKET_PHOTOS_DIR).apply { mkdirs() }
  private val prefs = appContext.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

  fun pendingRef(unitId: String): String? = prefs.getString(key(unitId), null)

  fun savePending(unitId: String, ref: String) {
    prefs.edit { putString(key(unitId), ref) }
  }

  fun clearPending(unitId: String) {
    prefs.edit { remove(key(unitId)) }
  }

  /** Deletes all stored photo bytes and pending refs. Blocking: call on Dispatchers.IO. */
  fun clearAll() {
    prefs.edit(commit = true) { clear() }
    dir.deleteRecursively()
    ticketDir.deleteRecursively()
    dir.mkdirs()
    ticketDir.mkdirs()
  }

  fun newTempFile(): File = File.createTempFile("capture_", ".jpg", appContext.cacheDir)

  /**
   * Compresses [source] into the photo dir and deletes the source.
   * Returns the stored ref (file name). Blocking: call on Dispatchers.IO.
   */
  fun compress(source: File, unitId: String): String = compressInto(source, dir, unitId)

  /**
   * Compresses a ticket photo into the ticket bucket. Blocking: call on
   * Dispatchers.IO. Temp capture files stay in cache until raise succeeds.
   */
  fun compressTicket(source: File, ticketId: String): String =
    compressInto(source, ticketDir, ticketId)

  private fun compressInto(source: File, targetDir: File, prefix: String): String {
    val bounds =
      BitmapFactory.Options().apply {
        inJustDecodeBounds = true
        BitmapFactory.decodeFile(source.absolutePath, this)
      }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
      throw IOException("unreadable image")
    }
    var sample = 1
    while (max(bounds.outWidth, bounds.outHeight) / sample > MAX_DIMENSION) sample *= 2
    val decoded =
      BitmapFactory.decodeFile(source.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample })
        ?: throw IOException("decode failed")
    val oriented = applyExifRotation(source, decoded)
    val safeUnit = prefix.replace(Regex("[^A-Za-z0-9_-]"), "_")
    val target = File(targetDir, "${safeUnit}_${System.currentTimeMillis()}.jpg")
    try {
      FileOutputStream(target).use { out ->
        if (!oriented.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)) {
          throw IOException("encode failed")
        }
      }
    } finally {
      if (oriented !== decoded) decoded.recycle()
      source.delete()
    }
    return target.name
  }

  /** Resolves a stored ref, guarding against path traversal. Never null for valid refs. */
  fun fileFor(ref: String?): File? = fileInDir(dir, ref)

  /** Resolves a stored ticket photo ref. */
  fun ticketFileFor(ref: String?): File? = fileInDir(ticketDir, ref)

  private fun fileInDir(bucket: File, ref: String?): File? {
    if (ref.isNullOrBlank()) return null
    val file = File(bucket, ref)
    return if (file.canonicalPath.startsWith(bucket.canonicalPath + File.separator)) file else null
  }

  /** Downsampled bitmap for thumbnails. Blocking: call on Dispatchers.IO. */
  fun loadThumbnail(ref: String?, maxDim: Int = THUMB_DIMENSION): ImageBitmap? {
    val file = fileFor(ref) ?: return null
    return decodeSampled(file, maxDim)?.asImageBitmap()
  }

  /** Downsampled bitmap for a fresh capture awaiting review. Blocking: call on Dispatchers.IO. */
  fun loadTempThumbnail(file: File, maxDim: Int = THUMB_DIMENSION): ImageBitmap? =
    decodeSampled(file, maxDim)?.asImageBitmap()

  /** Downsampled bitmap for a stored ticket photo. Blocking: call on Dispatchers.IO. */
  fun loadTicketThumbnail(ref: String?, maxDim: Int = THUMB_DIMENSION): ImageBitmap? {
    val file = ticketFileFor(ref) ?: return null
    return decodeSampled(file, maxDim)?.asImageBitmap()
  }

  private fun decodeSampled(file: File, maxDim: Int): Bitmap? {
    if (!file.exists()) return null
    val bounds =
      BitmapFactory.Options().apply {
        inJustDecodeBounds = true
        BitmapFactory.decodeFile(file.absolutePath, this)
      }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (max(bounds.outWidth, bounds.outHeight) / sample > maxDim) sample *= 2
    return BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample })
  }

  private fun applyExifRotation(source: File, bitmap: Bitmap): Bitmap {
    val orientation =
      try {
        ExifInterface(source.absolutePath)
          .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
      } catch (e: IOException) {
        ExifInterface.ORIENTATION_NORMAL
      }
    val degrees =
      when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
      }
    if (degrees == 0f) return bitmap
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
  }

  private fun key(unitId: String): String = "pending_photo_$unitId"

  companion object {
    const val PHOTOS_DIR: String = "cleaning_photos"
    const val TICKET_PHOTOS_DIR: String = "ticket_photos"
    const val PREFS_FILE: String = "roomready_photos"
    const val MAX_DIMENSION: Int = 1280
    const val JPEG_QUALITY: Int = 70
    const val THUMB_DIMENSION: Int = 512
  }
}
