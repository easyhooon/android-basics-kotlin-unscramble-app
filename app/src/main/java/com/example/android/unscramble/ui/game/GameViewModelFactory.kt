package com.example.android.unscramble.ui.game

/*
//SavedStateHandle 를 직접 팩토리를 통해 주입해주는 것은 상당한 코드가 필요하기 때문에 viewModelProvider.Factory -> AbstractSavedStateViewModelFactory 를 통해 구현
class GameViewModelFactory(
    private val application: Application,
    owner: SavedStateRegistryOwner,
    defaultArgs: Bundle? = null
) : AbstractSavedStateViewModelFactory(owner, defaultArgs) {
    // T: ViewModel? -> ? 제거
    override fun <T : ViewModel> create(
        key: String,
        modelClass: Class<T>,
        handle: SavedStateHandle
    ): T {
        // 해당 클래스가 없으면 앱 크래시가 나도록
        require(modelClass.isAssignableFrom(GameViewModel::class.java)) {
            "Unknown ViewModel class"
        }
        return GameViewModel(
            stateHandler = handle,
            repository = GameRepository(application)
        ) as T
        // 원형이 되는 Factory class 가 Java 로 구현되어있기 떼문에 as T 를 통해 해결
        // Java 가 nullable 과 non-null 의 차이가 없는데 Kotlin 으로 cast 하면서 둘의 구분이 생기기 때문에 박스가 쳐짐
        // 당연히 null 이 아닌 값을 return 하도록 의도하기 때문에 무시해도 상관없음 @Suppress("UNCHECKED_CAST")
        // class 가 Kotlin 이 었으면 심각한 문제이지만 Java 이므로 괜춘
    }
}
 */
