package com.renaisn.reader

import android.net.Uri
import android.util.Log
import androidx.test.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun testContentProvider() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getTargetContext()
        Log.d("test",
            appContext.contentResolver.query(Uri.parse("content://com.renaisn.reader.api.ReaderProvider/sources/query"),null,null,null,null)
                !!.getString(0)
        )
          }
}
