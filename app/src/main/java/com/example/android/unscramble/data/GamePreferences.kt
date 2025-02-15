package com.example.android.unscramble.data

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore


val Context.gameDataStore by preferencesDataStore(
    name = "GamePreferences"
)

object PreferenceKeys {
    val HIGH_SCORE = intPreferencesKey("highScore")
}

data class GamePreferences(
    val highScore: Int
)