package com.theantsbot

import android.os.Environment
import com.google.gson.Gson
import java.io.File


class SettingsEntity {
    data class SettingsMain(
        val enabled: Boolean,
        val minStamina: Int,

        val sleepShort: Long,
        val sleepMedium: Long,
        val sleepLong: Long,

        val marchScreenTopBarHeight: Int,
        val marchUnitHeight: Int,

        val positions: Positions,
        val swipes: Swipes
    )

    data class Positions(
        val searchButton: Position,
        val searchGoButton: Position,
        val centerScreen: Position,
        val attackButton: Position,
        val backButton: Position,
        val marchButton: Position
    )

    data class Swipes(
        val swipeMarchScreen: Swipe
    )

    data class Position(
        val x: Int,
        val y: Int
    )

    data class Swipe(
        val x: Int,
        val y: Int,
        val direction: String,
        val length: Long
    )
}

class Settings {
    fun loadSettings(): SettingsEntity.SettingsMain {
        val path = Environment.getExternalStoragePublicDirectory(
            "TheAntsBot"
        ).toString()
        val text = File("$path/settings.json").readText()
        val gson = Gson()
        val settingsEntity = gson.fromJson(text, SettingsEntity.SettingsMain::class.java)
        return settingsEntity
    }

    fun checkEnabled(_settings: SettingsEntity.SettingsMain? = null): Boolean {
        val settings = _settings ?: loadSettings()

        return settings.enabled
    }
}
