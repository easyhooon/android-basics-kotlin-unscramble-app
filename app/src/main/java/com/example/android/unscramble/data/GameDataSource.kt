package com.example.android.unscramble.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

// ApplicationContext 라는 어노테이션을 통해 ApplicaitonContext 를 context 에 자동으로 주입
class GameDataSource @Inject constructor(
    // application: Application,
    @ApplicationContext context: Context
) {
    // dataStore 는 private 으로 선언하여 외부로부터 감춤
    // private val dataStore = application.gameDataStore
    private val dataStore = context.gameDataStore

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
