/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.wear.watchface.client.test

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.screenshot.assertAgainstGolden
import androidx.wear.complications.SystemProviders
import androidx.wear.complications.data.ComplicationText
import androidx.wear.complications.data.ComplicationType
import androidx.wear.complications.data.ShortTextComplicationData
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.RenderParameters
import androidx.wear.watchface.client.DeviceConfig
import androidx.wear.watchface.client.SystemState
import androidx.wear.watchface.client.WatchFaceControlClientImpl
import androidx.wear.watchface.control.WatchFaceControlService
import androidx.wear.watchface.data.ComplicationBoundsType
import androidx.wear.watchface.samples.EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID
import androidx.wear.watchface.samples.EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID
import androidx.wear.watchface.samples.ExampleCanvasWatchFaceService
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import java.util.concurrent.TimeUnit

private const val CONNECT_TIMEOUT_MILLIS = 500L

@RunWith(AndroidJUnit4::class)
@MediumTest
class WatchFaceControlClientTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val service = WatchFaceControlClientImpl(
        context,
        Intent(context, WatchFaceControlTestService::class.java).apply {
            action = WatchFaceControlService.ACTION_WATCHFACE_CONTROL_SERVICE
        }
    )

    @Mock
    private lateinit var surfaceHolder: SurfaceHolder

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
    }

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule("wear/wear-watchface-client")

    private val exampleWatchFaceComponentName = ComponentName(
        "androidx.wear.watchface.samples.test",
        "androidx.wear.watchface.samples.ExampleCanvasWatchFaceService"
    )

    private val deviceConfig = DeviceConfig(
        false,
        false,
        1,
        0,
        0
    )

    private val systemState = SystemState(false, 0)

    private val complications = mapOf(
        EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID to
            ShortTextComplicationData.Builder(ComplicationText.plain("ID"))
                .setTitle(ComplicationText.plain("Left"))
                .build(),
        EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID to
            ShortTextComplicationData.Builder(ComplicationText.plain("ID"))
                .setTitle(ComplicationText.plain("Right"))
                .build()
    )

    @Test
    fun headlessScreenshot() {
        val headlessInstance = service.createHeadlessWatchFaceClient(
            ComponentName(
                "androidx.wear.watchface.samples.test",
                "androidx.wear.watchface.samples.ExampleCanvasWatchFaceService"
            ),
            DeviceConfig(
                false,
                false,
                1,
                0,
                0
            ),
            400,
            400
        ).get(CONNECT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)!!
        val bitmap = headlessInstance.takeWatchFaceScreenshot(
            RenderParameters(DrawMode.INTERACTIVE, RenderParameters.DRAW_ALL_LAYERS, null),
            100,
            1234567,
            null,
            complications
        )

        bitmap.assertAgainstGolden(screenshotRule, "headlessScreenshot")

        headlessInstance.close()
        service.close()
    }

    @Test
    fun headlessComplicationDetails() {
        val headlessInstance = service.createHeadlessWatchFaceClient(
            exampleWatchFaceComponentName,
            deviceConfig,
            400,
            400
        ).get(CONNECT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)!!

        assertThat(headlessInstance.complicationState.size).isEqualTo(2)

        val leftComplicationDetails = headlessInstance.complicationState[
            EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID
        ]!!
        assertThat(leftComplicationDetails.bounds).isEqualTo(Rect(80, 160, 160, 240))
        assertThat(leftComplicationDetails.boundsType).isEqualTo(ComplicationBoundsType.ROUND_RECT)
        assertThat(leftComplicationDetails.defaultProviderPolicy.systemProviderFallback).isEqualTo(
            SystemProviders.DAY_OF_WEEK
        )
        assertThat(leftComplicationDetails.defaultProviderType).isEqualTo(
            ComplicationType.SHORT_TEXT
        )
        assertThat(leftComplicationDetails.supportedTypes).containsExactly(
            ComplicationType.RANGED_VALUE,
            ComplicationType.LONG_TEXT,
            ComplicationType.SHORT_TEXT,
            ComplicationType.MONOCHROMATIC_IMAGE,
            ComplicationType.SMALL_IMAGE
        )
        assertTrue(leftComplicationDetails.isEnabled)

        val rightComplicationDetails = headlessInstance.complicationState[
            EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID
        ]!!
        assertThat(rightComplicationDetails.bounds).isEqualTo(Rect(240, 160, 320, 240))
        assertThat(rightComplicationDetails.boundsType).isEqualTo(ComplicationBoundsType.ROUND_RECT)
        assertThat(rightComplicationDetails.defaultProviderPolicy.systemProviderFallback).isEqualTo(
            SystemProviders.STEP_COUNT
        )
        assertThat(rightComplicationDetails.defaultProviderType).isEqualTo(
            ComplicationType.SHORT_TEXT
        )
        assertThat(rightComplicationDetails.supportedTypes).containsExactly(
            ComplicationType.RANGED_VALUE,
            ComplicationType.LONG_TEXT,
            ComplicationType.SHORT_TEXT,
            ComplicationType.MONOCHROMATIC_IMAGE,
            ComplicationType.SMALL_IMAGE
        )
        assertTrue(rightComplicationDetails.isEnabled)

        headlessInstance.close()
        service.close()
    }

    @Test
    @Ignore("Creation of new screenshots is currently broken b/171983840")
    fun getOrCreateWallpaperServiceBackedInteractiveWatchFaceWcsClient() {
        val interactiveInstanceFuture =
            service.getOrCreateWallpaperServiceBackedInteractiveWatchFaceWcsClient(
                "testId",
                deviceConfig,
                systemState,
                null,
                complications
            )

        Mockito.`when`(surfaceHolder.surfaceFrame)
            .thenReturn(Rect(0, 0, 400, 400))

        val wallpaperService = TestExampleCanvasWatchFaceService(context, surfaceHolder)

        // Create the engine which triggers creation of InteractiveWatchFaceWcsClient.
        val handler = Handler(Looper.getMainLooper())
        lateinit var engine: WallpaperService.Engine
        handler.post { engine = wallpaperService.onCreateEngine() }

        val interactiveInstance =
            interactiveInstanceFuture.get(CONNECT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)!!

        val bitmap = interactiveInstance.takeWatchFaceScreenshot(
            RenderParameters(DrawMode.INTERACTIVE, RenderParameters.DRAW_ALL_LAYERS, null),
            100,
            1234567,
            null,
            complications
        )

        try {
            bitmap.assertAgainstGolden(screenshotRule, "interactiveScreenshot")
        } finally {
            engine.onDestroy()
            interactiveInstance.close()
            service.close()
        }
    }

    @Test
    fun getOrCreateWallpaperServiceBackedInteractiveWatchFaceWcsClient_existingOpenInstance() {
        val interactiveInstanceFuture =
            service.getOrCreateWallpaperServiceBackedInteractiveWatchFaceWcsClient(
                "testId",
                deviceConfig,
                systemState,
                null,
                complications
            )

        Mockito.`when`(surfaceHolder.surfaceFrame)
            .thenReturn(Rect(0, 0, 400, 400))

        val wallpaperService = TestExampleCanvasWatchFaceService(context, surfaceHolder)

        // Create the engine which triggers creation of InteractiveWatchFaceWcsClient.
        val handler = Handler(Looper.getMainLooper())
        lateinit var engine: WallpaperService.Engine
        handler.post { engine = wallpaperService.onCreateEngine() }

        // Wait for the instance to be created.
        interactiveInstanceFuture.get(CONNECT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)!!

        val existingInstance =
            service.getOrCreateWallpaperServiceBackedInteractiveWatchFaceWcsClient(
                "testId",
                deviceConfig,
                systemState,
                null,
                complications
            )

        try {
            assertTrue(existingInstance.isDone)
        } finally {
            engine.onDestroy()
            service.close()
        }
    }

    @Test
    fun getOrCreateWallpaperServiceBackedInteractiveWatchFaceWcsClient_existingClosedInstance() {
        val interactiveInstanceFuture =
            service.getOrCreateWallpaperServiceBackedInteractiveWatchFaceWcsClient(
                "testId",
                deviceConfig,
                systemState,
                null,
                complications
            )

        Mockito.`when`(surfaceHolder.surfaceFrame)
            .thenReturn(Rect(0, 0, 400, 400))

        val wallpaperService = TestExampleCanvasWatchFaceService(context, surfaceHolder)

        // Create the engine which triggers creation of InteractiveWatchFaceWcsClient.
        val handler = Handler(Looper.getMainLooper())
        lateinit var engine: WallpaperService.Engine
        handler.post { engine = wallpaperService.onCreateEngine() }

        // Wait for the instance to be created.
        val interactiveInstance =
            interactiveInstanceFuture.get(CONNECT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)!!

        // Closing this interface means the subsequent
        // getOrCreateWallpaperServiceBackedInteractiveWatchFaceWcsClient won't immediately return
        // a resolved future.
        interactiveInstance.close()

        val existingInstance =
            service.getOrCreateWallpaperServiceBackedInteractiveWatchFaceWcsClient(
                "testId",
                deviceConfig,
                systemState,
                null,
                complications
            )

        try {
            assertFalse(existingInstance.isDone)

            // We don't want to leave a pending request or it'll mess up subsequent tests.
            handler.post { engine = wallpaperService.onCreateEngine() }
            existingInstance.get(CONNECT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)!!
        } finally {
            engine.onDestroy()
            service.close()
        }
    }

    @Test
    fun getInteractiveWatchFaceInstanceSysUi() {
        val interactiveInstanceFuture =
            service.getOrCreateWallpaperServiceBackedInteractiveWatchFaceWcsClient(
                "testId",
                deviceConfig,
                systemState,
                null,
                complications
            )

        Mockito.`when`(surfaceHolder.surfaceFrame)
            .thenReturn(Rect(0, 0, 400, 400))

        val wallpaperService = TestExampleCanvasWatchFaceService(context, surfaceHolder)

        // Create the engine which triggers creation of InteractiveWatchFaceWcsClient.
        val handler = Handler(Looper.getMainLooper())
        lateinit var engine: WallpaperService.Engine
        handler.post { engine = wallpaperService.onCreateEngine() }

        // Wait for the instance to be created.
        interactiveInstanceFuture.get(CONNECT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)!!

        val sysUiInterface = service.getInteractiveWatchFaceSysUiClientInstance("testId")
            .get(CONNECT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)!!

        try {
            val contentDescriptionLabels = sysUiInterface.contentDescriptionLabels
            assertThat(contentDescriptionLabels.size).isEqualTo(3)
            // Central clock element. Note we don't know the timezone this test will be running in
            // so we can't assert the contents of the clock's test.
            assertThat(contentDescriptionLabels[0].bounds).isEqualTo(Rect(100, 100, 300, 300))
            assertThat(contentDescriptionLabels[0].getTextAt(context.resources, 0).isNotEmpty())

            // Left complication.
            assertThat(contentDescriptionLabels[1].bounds).isEqualTo(Rect(80, 160, 160, 240))
            assertThat(contentDescriptionLabels[1].getTextAt(context.resources, 0))
                .isEqualTo("ID Left")

            // Right complication.
            assertThat(contentDescriptionLabels[2].bounds).isEqualTo(Rect(240, 160, 320, 240))
            assertThat(contentDescriptionLabels[2].getTextAt(context.resources, 0))
                .isEqualTo("ID Right")
        } finally {
            engine.onDestroy()
            service.close()
        }
    }
}

internal class TestExampleCanvasWatchFaceService(
    testContext: Context,
    private var surfaceHolderOverride: SurfaceHolder
) : ExampleCanvasWatchFaceService() {

    init {
        attachBaseContext(testContext)
    }

    override fun getWallpaperSurfaceHolderOverride() = surfaceHolderOverride
}
