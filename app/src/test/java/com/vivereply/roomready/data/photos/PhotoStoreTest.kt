package com.vivereply.roomready.data.photos

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PhotoStoreTest {

  private fun store(): PhotoStore =
    PhotoStore(ApplicationProvider.getApplicationContext<Context>())

  @Test
  fun `pending refs round trip and clear`() {
    val store = store()
    assertNull(store.pendingRef("u-101"))
    store.savePending("u-101", "u-101_123.jpg")
    assertEquals("u-101_123.jpg", store.pendingRef("u-101"))
    store.clearPending("u-101")
    assertNull(store.pendingRef("u-101"))
  }

  @Test
  fun `file resolution rejects blank and traversal refs`() {
    val store = store()
    assertNull(store.fileFor(null))
    assertNull(store.fileFor(""))
    assertNull(store.fileFor("../../outside.jpg"))
    assertNull(store.ticketFileFor("../cleaning_photos/x.jpg"))
  }

  @Test
  fun `compress downscales stores and deletes the source`() {
    val store = store()
    val source = File.createTempFile("pflarge_", ".jpg")
    writeJpeg(source, 2560, 1920)

    val ref = store.compress(source, "u-101")

    assertTrue(ref.startsWith("u-101_"))
    assertTrue(ref.endsWith(".jpg"))
    assertTrue(!source.exists())
    val stored = store.fileFor(ref)
    assertNotNull(stored)
    assertTrue(stored!!.exists())
    val thumb = store.loadThumbnail(ref)
    assertNotNull(thumb)
    assertTrue(thumb!!.width <= PhotoStore.THUMB_DIMENSION)
    assertTrue(thumb.height <= PhotoStore.THUMB_DIMENSION)
    stored.delete()
  }

  @Test
  fun `ticket bucket is separate from cleaning photos`() {
    val store = store()
    val source = File.createTempFile("pftick_", ".jpg")
    writeJpeg(source, 64, 48)

    val ref = store.compressTicket(source, "t-1")

    assertTrue(ref.startsWith("t-1_"))
    assertTrue(store.ticketFileFor(ref)!!.exists())
    assertTrue(!store.fileFor(ref)!!.exists())
    store.ticketFileFor(ref)!!.delete()
  }

  @Test
  fun `unit ids are sanitized into file names`() {
    val store = store()
    val source = File.createTempFile("pfsan_", ".jpg")
    writeJpeg(source, 64, 48)

    val ref = store.compress(source, "../../u-101")

    assertTrue(!ref.contains("/"))
    assertTrue(!ref.contains(".."))
    assertTrue(ref.endsWith(".jpg"))
    store.fileFor(ref)!!.delete()
  }

  private fun writeJpeg(file: File, width: Int, height: Int) {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    bitmap.eraseColor(0xFF336699.toInt())
    FileOutputStream(file).use { out ->
      assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out))
    }
    bitmap.recycle()
  }
}
