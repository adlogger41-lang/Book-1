package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("The Old Book", appName)
  }

  @Test
  fun `index html asset exists and contains core application elements`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val assetManager = context.assets
    val inputStream = assetManager.open("index.html")
    assertNotNull(inputStream)

    val htmlContent = inputStream.bufferedReader().use { it.readText() }
    assertTrue("Should contain writing area", htmlContent.contains("id=\"writing\""))
    assertTrue("Should contain drawing canvas", htmlContent.contains("id=\"drawing\""))
    assertTrue("Should contain book cover", htmlContent.contains("id=\"cover\""))
    assertTrue("Should contain turn-layer", htmlContent.contains("id=\"turnLayer\""))
    assertTrue("Should contain AndroidBridge hook", htmlContent.contains("AndroidBridge"))
    assertTrue("Should contain onNativeBackPressed", htmlContent.contains("onNativeBackPressed"))
  }

  @Test
  fun `activity launches and sets up state correctly`() {
    val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
    val activity = controller.get()
    assertNotNull(activity)

    // Verify storage bridge logic
    val bridge = activity.AndroidBridge()
    val sampleState = "{\"title\":\"Test Book\",\"pages\":{\"1\":{\"html\":\"Hello\",\"drawings\":[]}}}"
    bridge.saveNotebook(sampleState)

    val loadedState = bridge.loadNotebook()
    assertEquals(sampleState, loadedState)

    // Cleanup
    val stateFile = File(activity.filesDir, "the_old_book_state.json")
    if (stateFile.exists()) {
      stateFile.delete()
    }

    controller.pause().stop().destroy()
  }
}
