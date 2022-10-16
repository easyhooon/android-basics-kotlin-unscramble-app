/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.unscramble.ui.game

// Fragment / Activity 인스턴스가 종료되어도 동일한 화면 내용을 유지시키기 -> Complete
// LiveData -> StateFlow -> Complete
// SavedStateHandler 도입, 중요 데이터 저장(SaveStateHandle) -> Key 는 companion object로 관리해주자 -> Complete
// T초기화 구문 개선 (init{}) -> Complete

import android.text.Spannable
import android.text.SpannableString
import android.text.style.TtsSpan
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android.unscramble.data.GameRepository
import com.example.android.unscramble.util.Constants.CURRENT_SCRAMBLE_WORD
import com.example.android.unscramble.util.Constants.CURRENT_WORD
import com.example.android.unscramble.util.Constants.CURRENT_WORD_COUNT
import com.example.android.unscramble.util.Constants.SCORE
import com.example.android.unscramble.util.Constants.WORD_LIST
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import kotlin.random.Random


// 앱을 백그라운드로 보내고 게임을 실행하는 등의 활동을 할 경우 메모리관리 때문에
// 앱은 백그라운드에 남았지만(살아있음) 액티비티/프래그먼트는 파괴가 될 수 있음 -> 뷰모델도 같이 사라짐
// 터미널에 adb shell am kill "{packageName}" 을 통해 이를 테스트할 수 있음

// 생성자로 StateHandle 만 추가해줘도 다른 처리는 해줄 필요가 없다 by keyword 를 통해 뷰모델을 생성하였으므로

// StateHandler 를 사용할 경우 기존의 방식과 값을 수정하고 조작하는 과정이 달라짐
// 기존의 방법과 일관성있는 값 수정, 사용을 위한 helper class
class SaveableMutableStateFlow<T>(
    private val savedStateHandle: SavedStateHandle,
    private val key: String,
    initialValue: T
) {
    private val state: StateFlow<T> = savedStateHandle.getStateFlow(key, initialValue)
    var value: T
        get() = state.value
        set(value) {
            savedStateHandle[key] = value
        }

    fun asStateFlow(): StateFlow<T> = state
}

fun <T> SavedStateHandle.getMutableStateFlow(
    key: String,
    initialValue: T
): SaveableMutableStateFlow<T> =
    SaveableMutableStateFlow(this, key, initialValue)


/**
 * ViewModel containing the app data and methods to process the data
 */
// Room db, dataStore 를 사용할때 applicationContext 가 필요하므로 이런 경우에 applicationContext 를 가지는 형태의 뷰모델
// 뷰모델 위의 뷰모델을 만들 때, application 전체의 전역적인 값을 갖기위해 사용될 수 있음
// AndroidViewModel 로 변경하면 by viewModels() 라는 delegation 함수에서 자동으로 application 을 넣어줌, stateHandler 와 마찬가지로
// 기본적으로 viewModels 라고 하는 delegation 에서 viewModel 에 파라미터를 넣는 것을 허용하지 않는데 stateHandler 와 application 같은 경우는 자동으로 지원
// 하지만 repository 와 같은 커스텀한 클래스를 생성자 파라미터에 주입하려면 팩토리를 커스텀하게 구현해줘야 한다.
class GameViewModel(
    // Factory 를 통해 application 주입
    // application: Application,
    private val stateHandler: SavedStateHandle,
    // private val repository: GameRepository = GameRepository(application)
    private val repository: GameRepository
//) : AndroidViewModel(application) {
) : ViewModel() {


//    1. LiveData
//    private val _score = MutableLiveData(0)
//    val score: LiveData<Int>
//        get() = _score

    // 이후 업데이트에선(아직은 Experimental API) 이런식으로 더 간략하게 사용할 수 있음
//    val score: LiveData<Int>
//        field = MutableLiveData(0)

//    2. LiveData -> StateFlow
//    private val _score = MutableStateFlow(0)
//    val score: StateFlow<Int>
//        get() = _score

//    3. SavedStateHandle 도입
//    val score: StateFlow<Int> = stateHandler.getStateFlow("score", 0)
//    private fun setScore(value: Int) {
//        stateHandler["score"] = value
//    }

    //   4. Helper Class 를 통해 기존의 방식과 거의 비슷하게 선언해줄 수 있음(나머지 변수들도 똑같이 선언해줌)
    private val _score = stateHandler.getMutableStateFlow(SCORE, 0)
    val score: StateFlow<Int>
        get() = _score.asStateFlow()

    //dataBinding 을 통해서 읽어져야하기 때문에 StateFlow
    val highScore: StateFlow<Int> = repository.highScore.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(), 0
    )

//    private val _currentWordCount = MutableLiveData(0)
//    val currentWordCount: LiveData<Int>
//        get() = _currentWordCount

//    private val _currentWordCount = MutableStateFlow(0)
//    val currentWordCount: StateFlow<Int>
//        get() = _currentWordCount

    private val _currentWordCount = stateHandler.getMutableStateFlow(CURRENT_WORD_COUNT, 0)
    val currentWordCount: StateFlow<Int>
        get() = _currentWordCount.asStateFlow()

    //    private val _currentScrambledWord = MutableLiveData<String>()
    // stateFlow 는 initial value를 필요로 함
    // liveData 는 기본적으로 자바 기반으로 만들어져있기 때문에 nullable 처리가 따로 되어있지 않음
    // private val _currentScrambledWord = MutableStateFlow<String>("")
    private val _currentScrambledWord = stateHandler.getMutableStateFlow(CURRENT_SCRAMBLE_WORD, "")

    // liveData 들은 xml 에서 참조가 가능
    // liveData 의 단점: Transformation 을 통해 다른 liveData 의 결과를 받거나, liveData 여러개를 조합해서 하나의 결과를 만듬
    // 별로 직관적이지 못함
//    val currentScrambledWord: LiveData<Spannable> = Transformations.map(_currentScrambledWord) {
//        if (it == null) {
//            SpannableString("")
//        } else {
//            val scrambledWord = it.toString()
//            val spannable: Spannable = SpannableString(scrambledWord)
//            spannable.setSpan(
//                    TtsSpan.VerbatimBuilder(scrambledWord).build(),
//                    0,
//                    scrambledWord.length,
//                    Spannable.SPAN_INCLUSIVE_INCLUSIVE
//            )
//            spannable
//        }
//    }
    val currentScrambledWord: StateFlow<Spannable> = _currentScrambledWord
        .asStateFlow()
        // 중요한 이벤트를 옵저빙하기 시작하는 시점
        // 생성자가 존재하지 않아서 액티비티가 내려가서 다시 호출되든 상관없이 필요할 경우에만 새로운 단어가 생성
        .onSubscription {
            // nextWord() if necessary
            // init{} 에서 호출 -> 초기화가 필요한 상태일 때 호출 하는 방식으로 변경
            if (currentWord.isEmpty())
                nextWord()
        }
        // map: 받아온 값을 (여기선 String) 값을 바꾸거나, type 을 변경하는 역할을 수행
        .map { scrambledWord ->
            // val scrambledWord = it.toString()
            val spannable: Spannable = SpannableString(scrambledWord)
            spannable.setSpan(
                TtsSpan.VerbatimBuilder(scrambledWord).build(),
                0,
                scrambledWord.length,
                Spannable.SPAN_INCLUSIVE_INCLUSIVE
            )
            spannable
        }
        // map 연산의 결과로 Flow type 이 되기때문에 stateIn 을 통해 StateFlow 로 변환
        // TODO stated parameter 인자에 관한 학습 필요(궁금했던거 5000 millis second 관련) 찰스님 글
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SpannableString("")
        )


    // List of words used in the game
    //private var wordsList: MutableList<String> = mutableListOf()

    // wordsList 보존을 위해
    // 위에 helper 클래스를 만든 이유 -> 이런식으로 모든 변수를 바꿔줘야하는데 번거롭고 반복되는 코드가 많으니
    // 기존의 방식과 비슷하게 stateHandler 를 달아주는 작업을 수행하기 위해
    // 저장 방식 수정
    // 초기 상태 빈 mutableListOf() 를 얻게됨
    // 여기에 add 를 통해 단어들을 넣어주는데 한번도 stateHandler 에는 저장된 적이 없음
    // activity 가 내려가면 저장되지 않았기 때문에 내용이 사라짐
    // 굉장히 실수가 많이 발생하는 경우 (mutable 에 값을 저장하는데 원하지 않게 저장되지 않는 경우)
    // -> immutable 로 만들어서 add 는 불가능하지만 새로운 값을 대입의 형태로 저장
    // 새로운 리스트를 생성해야하기에 성능 lose 는 생갸도 저장이 안될 일은 없음

    // private var wordsList: MutableList<String>
    private var wordsList: List<String>
        // get() = stateHandler["wordsList"] ?: mutableListOf()
        get() = stateHandler[WORD_LIST] ?: emptyList()
        set(value) {
            stateHandler[WORD_LIST] = value
        }

    // private lateinit var currentWord: String
    private var currentWord: String
        // 초기 상태
        get() = stateHandler[CURRENT_WORD] ?: ""
        // 매번 대입의 형태로 값을 저장하였기 때문에
        // 액티비티가 내려갔어도 정상적으로 값 복원 가능
        // value -> 새로운 값
        set(value) {
            val tempWord = value.toCharArray()
            tempWord.shuffle()
            // 한번은 일어나야 하므로
            do {
                tempWord.shuffle()
            } while (String(tempWord) == value)
            Log.d("Unscramble", "currentWord= $value")
            _currentScrambledWord.value = String(tempWord)
            // _currentWordCount.value = _currentWordCount.value?.inc()
            // null 이 들어갈 수 없음
            _currentWordCount.value += 1
            // wordsList.add(currentWord)
            wordsList = wordsList + currentWord
            stateHandler[CURRENT_WORD] = value
        }

    /*
    // activity, fragment kill 로 인해 뷰모델도 날아갈 경우 init block 도 재실행됨 (생성자 초기화의 문제점)
    // 나머지는 savedStateHandle 처리로 저장되었지만 단어 배열은 바뀌게 됨
    init {
        // 이 위치가 아닌 주요한 이벤트가 관찰되기 시작했을 때로 위치를 변경
        // getNextWord()
    }
     */

    /*
     * Updates currentWord and currentScrambledWord with the next word.
     */
    // refactoring 된 getNextWord() 함수를 다른 곳으로 이동
    /*
    private fun getNextWord() {
        // 매번 실행시 배열이 바뀌기 위해 시간을 사용한 random 으로 변경
        // 확정되지 않은 내용은 로컬에 저장시키는게 맞음(다른 곳에서 중간에 참조될 수도 있음)
        var nextWord: String
        // 최소 한번은 만들어져야하기 때문에 do while
        do {
            nextWord = allWordsList.random(Random(Calendar.getInstance().timeInMillis))
        } while (wordsList.contains(currentWord))
        // 확정된 nextWord 값을 맴버 변수에 대입
        currentWord = nextWord
        //currentWord = allWordsList.random(Random(Calendar.getInstance().timeInMillis))

        /*
        //다른 곳으로 이동
        val tempWord = currentWord.toCharArray()
        tempWord.shuffle()

        while (String(tempWord).equals(currentWord, false)) {
            tempWord.shuffle()
        }
        if (wordsList.contains(currentWord)) {
            getNextWord()
        } else {
            Log.d("Unscramble", "currentWord= $currentWord")
            _currentScrambledWord.value = String(tempWord)
            _currentWordCount.value = _currentWordCount.value?.inc()
            // wordsList.add(currentWord)
            wordsList = wordsList + currentWord
        }
         */
    }
    */

    /*
     * Re-initializes the game data to restart the game.
     */
    // 새로운 게임을 시작
    fun reinitializeData() {
        _score.value = 0
        _currentWordCount.value = 0
        // wordsList.clear()
        // 비어있는 리스트를 대입
        wordsList = emptyList()
        //getNextWord()
        nextWord()
    }

    /*
    * Increases the game score if the player’s word is correct.
    */
    private fun increaseScore() {
        //nullable 하지 않아서 ? 제거
        _score.value = _score.value.plus(SCORE_INCREASE)

        viewModelScope.launch {
            repository.updateScore(_score.value)
        }
    }

    /*
    * Returns true if the player word is correct.
    * Increases the score accordingly.
    */
    fun isUserWordCorrect(playerWord: String): Boolean {
        if (playerWord.equals(currentWord, true)) {
            increaseScore()
            return true
        }
        return false
    }

    /*
    * Returns true if the current word count is less than MAX_NO_OF_WORDS
    */
    // getNextWord()의 부작용, activity가 내려갔다가 다시 생성되면 호출 즉시 실행되므로 심지어 최종 워드 카운드가 올라가서 11라운드로 넘어간다던지 에 문제점이 발생할 수도 있음
    // 이를 원천적으로 해결
    fun nextWord(): Boolean {
        // TODO LiveData 와 StateFlow 의 차이점 확실하게 정리 (nullable(LiveData가 자바기반이므로), notNull, 샘명주기, 다양한 변형함수(?)) 기술 면접에 나올만 하니깐...
        // !! -> 제거 (LiveData 가 아니기 때문에 null 일 가능성이 없음)
        return if (_currentWordCount.value < MAX_NO_OF_WORDS) {
            // refactoring 된 getNextWord 호출
            var nextWord: String
            do {
                nextWord = allWordsList.random(Random(Calendar.getInstance().timeInMillis))
            } while (wordsList.contains(currentWord))
            currentWord = nextWord
            true
        } else false
    }
}

