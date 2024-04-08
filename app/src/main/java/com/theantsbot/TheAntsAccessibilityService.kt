package com.theantsbot

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.graphics.*
import android.os.Environment
import android.util.Log
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException


class TheAntsAccessibilityService : AccessibilityService() {
    companion object {
        const val TAG = "TheAntsAccessibilityService"
    }

    private fun makeGray(bitmap: Bitmap): Bitmap {
        // Create OpenCV mat object and copy content from bitmap
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        // Convert to grayscale
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2GRAY)

        // Make a mutable bitmap to copy grayscale image
        val grayBitmap = bitmap.copy(bitmap.config, true)
        Utils.matToBitmap(mat, grayBitmap)

        return grayBitmap
    }


    private fun logText(text: String) {
        Log.d(TAG, text)

        val path = Environment.getExternalStoragePublicDirectory(
            "TheAntsBot"
        ).toString()

        val logFile = File("$path/the_ants_bot.log")
        if (!logFile.exists()) {
            try {
                logFile.createNewFile()
            } catch (e: IOException) {
                // TODO Auto-generated catch block
                e.printStackTrace()
            }
        }
        try {
            // BufferedWriter for performance, true to set append to file flag
            val buf = BufferedWriter(FileWriter(logFile, true))
            buf.append(text)
            buf.newLine()
            buf.close()
        } catch (e: IOException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }
    }

    private fun dispatchGestureWithCallback(gesture: GestureDescription) {
        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription) {
                val msg = "Info: dispatchGesture: Success"
                logText(msg)
                super.onCompleted(gestureDescription)
            }

            override fun onCancelled(gestureDescription: GestureDescription) {
                val msg = "Error: dispatchGesture: Cancel"
                logText(msg)
                super.onCancelled(gestureDescription)
            }
        }, null)
    }

    private fun pressLocation(x: Int, y: Int) {
        val builder = GestureDescription.Builder()
        val p = Path()
        p.moveTo(x.toFloat(), y.toFloat())
        p.lineTo(x.toFloat() + 5, y.toFloat() + 5)
        builder.addStroke(StrokeDescription(p, 10L, 200L))
        val gesture = builder.build()
        dispatchGestureWithCallback(gesture)

    }

    private fun swipeLocation(swipe: SettingsEntity.Swipe) {
        val builder = GestureDescription.Builder()
        val p = Path()
        p.moveTo(swipe.x.toFloat(), swipe.y.toFloat())
        if (swipe.direction == "down") {
            p.lineTo(swipe.x.toFloat(), swipe.y.toFloat() - swipe.length)
        }
        else if (swipe.direction == "up") {
            p.lineTo(swipe.x.toFloat(), swipe.y.toFloat() + swipe.length)
        }
        builder.addStroke(StrokeDescription(p, 10L, 500L))
        val gesture = builder.build()
        dispatchGestureWithCallback(gesture)
    }

    private fun analyzeMarchUnit(isSwiped: Boolean = false) {
        val settings = Settings().loadSettings()

        if (!Settings().checkEnabled()) {
            return
        }

        val msg = "Info: Ready to take screenshot"
        logText(msg)
        takeScreenshot(
            Display.DEFAULT_DISPLAY,
            mainExecutor, object : TakeScreenshotCallback {
                override fun onSuccess(screenshotResult: ScreenshotResult) {
                    val msg2 = "Info: takeScreenshot: Success"
                    logText(msg2)

                    val bitmap = Bitmap.wrapHardwareBuffer(
                        screenshotResult.hardwareBuffer,
                        screenshotResult.colorSpace
                    )

                    if (bitmap != null) {
                        val softwareBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false)
                        val bmpGrayscale = makeGray(softwareBitmap)

                        val text = ExtractText().run(bmpGrayscale)
                        if (text != null) {
                            logText("Info: Extracted text: $text")

                            val matcher = "(\\d{1,3}\\/100)|(ing)".toRegex()  // (\d{1,3}\/100)|(Marching)|(Collecting)
                            val staminaValues = matcher.findAll(text.toString()).map{it.value}.toList()
                            val staminaText = staminaValues.joinToString(
                                prefix = "[",
                                separator = ",",
                                postfix = "]",
                            )
                            logText("Info: Stamina values: $staminaText")

                            var found = false
                            var number = 0
                            var isMarching = false
                            val words = arrayOf("ing")  // ("Marching", "Collecting")
                            for (i in staminaValues.indices) {
                                val stamina = staminaValues[i]
                                var staminaNext = ""
                                if (i + 1 <= staminaValues.size - 1) {
                                    staminaNext = staminaValues[i + 1]
                                }
                                if (stamina !in words) {
                                    number += 1
                                    if (staminaNext !in words) {
                                        var staminaValue = stamina.split("/").toTypedArray()[0].toInt()
                                        if (staminaValue > 100) {
                                            logText("Warning: Stamina value $staminaValue is more than 100, get last two digits")
                                            staminaValue %= 100
                                        }
                                        if (staminaValue >= settings.minStamina) {
                                            found = true
                                            break
                                        }
                                    }
                                }
                                else {
                                    isMarching = true
                                }
                            }

                            if (found) {
                                // pressLocation(540F, 700F + 460F * number - 230F)  // 400F + 468F * number
                                pressLocation(
                                    settings.positions.centerScreen.x,
                                    settings.marchScreenTopBarHeight
                                            + settings.marchUnitHeight * number
                                            - settings.marchUnitHeight / 2
                                )
                                Thread.sleep(settings.sleepShort)
                                pressMarchButton(settings)
                                mainLoop()
                            }
                            else {
                                if (isSwiped) {
                                    if (isMarching) {
                                        pressBackButton(settings)
                                        Thread.sleep(settings.sleepLong)
                                        mainLoop()
                                    }
                                    else {
                                        // Temporarily rerun main loop, because of mistakes in screenshot recognition
                                        pressBackButton(settings)
                                        Thread.sleep(settings.sleepLong)
                                        mainLoop()
                                    }
                                }
                                else {
                                    swipeLocation(settings.swipes.swipeMarchScreen)
                                    Thread.sleep(settings.sleepMedium)
                                    analyzeMarchUnit(true)
                                }
                            }
                        }
                    }
                }

                override fun onFailure(i: Int) {
                    val msg2 = "Error: takeScreenshot: Failure code is $i"
                    logText(msg2)
                }
            }
        )
    }

    private fun pressSearchButton(settings: SettingsEntity.SettingsMain) {
//        pressLocation(75F, 1725F)  // 75, 1250
        pressLocation(settings.positions.searchButton.x, settings.positions.searchButton.y)
        Thread.sleep(settings.sleepMedium)
    }

    private fun pressSearchGoButton(settings: SettingsEntity.SettingsMain) {
//        pressLocation(920F, 2125F)  // 920, 1650
        pressLocation(settings.positions.searchGoButton.x, settings.positions.searchGoButton.y)
        Thread.sleep(settings.sleepMedium)
    }

    private fun pressCenterScreen(settings: SettingsEntity.SettingsMain) {
//        pressLocation(540F, 1200F)  // 540, 960
        pressLocation(settings.positions.centerScreen.x, settings.positions.centerScreen.y)
        Thread.sleep(settings.sleepMedium)
    }

    private fun pressAttackButton(settings: SettingsEntity.SettingsMain) {
//        pressLocation(540F, 1550F)  // 540, 1275
        pressLocation(settings.positions.attackButton.x, settings.positions.attackButton.y)
        Thread.sleep(settings.sleepMedium)
    }

    private fun pressBackButton(settings: SettingsEntity.SettingsMain) {
//        pressLocation(80F, 150F)  // 50, 50
        pressLocation(settings.positions.backButton.x, settings.positions.backButton.y)
        Thread.sleep(settings.sleepMedium)
    }

    private fun pressMarchButton(settings: SettingsEntity.SettingsMain) {
//        pressLocation(800F, 2295F)  // 800, 1800
        pressLocation(settings.positions.marchButton.x, settings.positions.marchButton.y)
        Thread.sleep(settings.sleepMedium)
    }

    private fun mainLoop() {
        val settings = Settings().loadSettings()

        if (!Settings().checkEnabled()) {
            return
        }

        Thread.sleep(settings.sleepMedium)
        pressSearchButton(settings)
        pressSearchGoButton(settings)
        pressCenterScreen(settings)
        pressAttackButton(settings)
        analyzeMarchUnit() // no need to pass settings, because this fun reloads it
    }

    override fun onInterrupt() {
        val msg = "Error: onInterrupt: Something went wrong"
        logText(msg)
    }

    override fun onAccessibilityEvent(accessibilityEvent: AccessibilityEvent) {
        if (!Settings().checkEnabled()) {
            return
        }
        val msg = "Info: onAccessibilityEvent: $accessibilityEvent"
        logText(msg)

        if (accessibilityEvent.packageName == "com.star.union.planetant"
            || accessibilityEvent.packageName == "com.star.union.planetant.flexion") {
            mainLoop()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo()
        info.apply {
            // Set the type of events that this service wants to listen to. Others
            // won't be passed to this service.
            eventTypes = AccessibilityEvent.TYPE_VIEW_CLICKED or AccessibilityEvent.TYPE_VIEW_FOCUSED

            // Set the type of feedback your service will provide.
            feedbackType = AccessibilityServiceInfo.FEEDBACK_SPOKEN

            notificationTimeout = 100
        }

        this.serviceInfo = info
    }
}
