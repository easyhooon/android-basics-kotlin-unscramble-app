package com.example.android.unscramble.data

import android.app.Application
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GameDataSource(
    application: Application
) {
    // dataStore 는 private 으로 선언하여 외부로부터 감춤
    private val dataStore = application.gameDataStore

    // 의도적으로 immutable 로 만듬, 전체 변경
    val gamePreferenceFlow: Flow<GamePreferences> = dataStore.data.map { preferences ->
        // 반드시 리턴 값이 Int 임을 보장
        val highScore = preferences[PreferenceKeys.HIGH_SCORE] ?: 0
        GamePreferences(highScore = highScore)
    }

    suspend fun updateHighScore(score: Int) {
        dataStore.edit { preferences ->
            val currentHighScore = preferences[PreferenceKeys.HIGH_SCORE] ?: 0
            if (currentHighScore < score) {
                preferences[PreferenceKeys.HIGH_SCORE] = score
            }

        }
    }
}
