package com.example.android.unscramble.data

import android.app.Application
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


class GameRepository(
    application: Application,
    private val dataSource: GameDataSource = GameDataSource(application)
) {
    /*
    // 단순하기 때문에 dataStore를 감출 필요가 굳이 없어서 이런식으로 구현
    // 반드시 감춰야할 경우엔 dataSource를 만들어 넣으면 됨

    private val dataStore = application.gameDataStore

    // 의도적으로 immutable 로 만듬, 전체 변경
    val gamePreferenceFlow: Flow<GamePreferences> = dataStore.data.map { preferences ->
        // 반드시 리턴 값이 Int 임을 보장
        val highScore = preferences[PreferenceKeys.HIGH_SCORE] ?: 0
        GamePreferences(highScore = highScore)
    }
     */

    val highScore: Flow<Int> = dataSource.gamePreferenceFlow.map { preferences ->
        preferences.highScore
    }

    suspend fun updateScore(score: Int) {
        dataSource.updateHighScore(score)
    }
}