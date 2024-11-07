package mobappdev.example.nback_cimpl.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import mobappdev.example.nback_cimpl.GameApplication
import mobappdev.example.nback_cimpl.NBackHelper
import mobappdev.example.nback_cimpl.data.UserPreferencesRepository
import mobappdev.example.nback_cimpl.data.AppDatabase
import mobappdev.example.nback_cimpl.data.GameResult
import mobappdev.example.nback_cimpl.services.AudioPlayer
import android.content.Context

/**
 * This is the GameViewModel.
 *
 * It is good practice to first make an interface, which acts as the blueprint
 * for your implementation. With this interface we can create fake versions
 * of the viewmodel, which we can use to test other parts of our app that depend on the VM.
 *
 * Our viewmodel itself has functions to start a game, to specify a gametype,
 * and to check if we are having a match
 *
 * Date: 25-08-2023
 * Version: Version 1.0
 * Author: Yeetivity
 *
 */


interface GameViewModel {
    val gameState: StateFlow<GameState>
    val score: StateFlow<Int>
    val highscore: StateFlow<Int>
    val nBack: Int

    fun setGameType(gameType: GameType)
    fun startGame()

    fun checkMatch()

    fun setGameSettings(nBackValue: Int, timeBetweenEvents: Int, numberOfEvents: Int, mode: String)
}

class GameVM(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val database: AppDatabase,
    private val context: Context
): GameViewModel, ViewModel() {

    private var isGameInitialized = false

    private val audioPlayer = AudioPlayer(context)  // Initialize AudioPlayer

    private val _gameState = MutableStateFlow(GameState())
    override val gameState: StateFlow<GameState>
        get() = _gameState.asStateFlow()

    private val _score = MutableStateFlow(0)
    override val score: StateFlow<Int>
        get() = _score

    private val _highscore = MutableStateFlow(0)
    override val highscore: StateFlow<Int>
        get() = _highscore

    // Game Settings
    private var nBackValue: Int = 2
    override val nBack: Int
        get() = nBackValue
    private var timeBetweenEvents: Long = 2000L
    private var numberOfEvents: Int = 20
    private var gameMode: GameType = GameType.Visual

    private var matchAttemptedForCurrentEvent = false

    private var job: Job? = null  // coroutine job for the game event

    private val nBackHelper = NBackHelper()  // Helper that generate the event array
    private var events: Array<Int> = emptyArray()  // Array with all events

    init {
        // Start the game as soon as the ViewModel is created
        initializeGameIfNeeded()
    }

    private fun initializeGameIfNeeded() {
        if (!isGameInitialized) {
            startGame()
            isGameInitialized = true
        }
    }

    // Call `releaseMediaPlayer` when ViewModel is cleared
    override fun onCleared() {
        super.onCleared()
        endGame()
    }

    fun playAudioForEvent(eventValue: Int) {
        audioPlayer.playAudio(eventValue)  // Delegate audio playback to AudioPlayer
    }


    override fun setGameType(gameType: GameType) {
        // Update the game mode
        this.gameMode = gameType
        _gameState.value = _gameState.value.copy(gameType = gameType)
    }

    override fun setGameSettings(nBackValue: Int, timeBetweenEvents: Int, numberOfEvents: Int, mode: String) {
        this.nBackValue = nBackValue
        this.timeBetweenEvents = timeBetweenEvents.toLong()
        this.numberOfEvents = numberOfEvents
        this.gameMode = GameType.valueOf(mode)

        _gameState.value = _gameState.value.copy(gameType = gameMode)

    }

    override fun startGame() {
        job?.cancel()  // Cancel any existing game loop

        // Reset score and game state at the beginning of each game
        _score.value = 0
        _gameState.value = _gameState.value.copy(
            correctResponses = 0,
            matches = emptyList(),
            currentEventNumber = 0,
            isError = false,
            isGameFinished = false,
            currentIndex = -1,
            eventValue = -1
        )
        matchAttemptedForCurrentEvent = false

        // Get the events from our C-model (returns IntArray, so we need to convert to Array<Int>)
        job = viewModelScope.launch {
            val percentMatch = 30
            Log.d("GameVM", "Calling generateNBackString with percentMatch=$percentMatch")
            events = withContext(Dispatchers.Default) {
                nBackHelper.generateNBackString(numberOfEvents, 9, percentMatch, nBackValue).toList().toTypedArray()
            }

            // Log generated sequence
            Log.d("GameVM", "Generated sequence: ${events.contentToString()}")

            if (events.isEmpty()) {
                Log.e("GameVM", "Events array is empty. Sequence generation failed.")
            } else {
                // Start the game based on the selected mode
                when (gameState.value.gameType) {
                    GameType.Audio -> runAudioGame()
                    GameType.AudioVisual -> runAudioVisualGame()
                    GameType.Visual -> runVisualGame()
                }
            }
        }
    }

    override fun checkMatch() {
        /**
         * This function should check if there is a match when the user presses a match button
         * Make sure the user can only register a match once for each event.
         */
        val currentIndex = _gameState.value.currentIndex
        val nBackValue = this.nBack

        if (!matchAttemptedForCurrentEvent && currentIndex >= nBackValue) {
            val isMatch = events[currentIndex] == events[currentIndex - nBackValue]

            if (isMatch && !_gameState.value.matches.contains(currentIndex)) {
                _score.value += 1 // Correct match
                _gameState.value = _gameState.value.copy(
                    matches = _gameState.value.matches + currentIndex,
                    isError = false,
                    correctResponses = _gameState.value.correctResponses + 1
                )
            } else if (!isMatch) {
                _score.value -= 1 // Incorrect match
                _gameState.value = _gameState.value.copy(isError = true)

                // Reset `isError` to false after a short delay
                viewModelScope.launch {
                    delay(500) // Match this to the animation duration
                    _gameState.value = _gameState.value.copy(isError = false)
                }
            }
            matchAttemptedForCurrentEvent = true // Prevent multiple checks per event
        }
    }

    // Save game results and update high score if necessary
    private fun saveGameResultAndCheckHighScore(finalScore: Int, correctResponses: Int) {
        viewModelScope.launch {
            // Insert the game result into the database
            database.gameResultDao().insertGameResult(
                GameResult(score = finalScore, correctResponses = correctResponses)
            )

            // Update high score if final score is higher
            userPreferencesRepository.highscore.collect { currentHighScore ->
                if (finalScore > currentHighScore) {
                    userPreferencesRepository.saveHighScore(finalScore)
                }
            }
        }
    }

    private fun runAudioGame() {
        job = viewModelScope.launch {
            for ((index, value) in events.withIndex()) {
                // Reset the match attempt flag for each new event
                matchAttemptedForCurrentEvent = false

                // Play the audio for the current event
                audioPlayer.playAudio(value)

                // Update the game state with the current event value, index, and incremented event number
                _gameState.value = _gameState.value.copy(
                    eventValue = value,
                    currentIndex = index,
                    currentEventNumber = index + 1 // Update current event number
                )

                // Wait for the specified interval for the audio event to play
                delay(timeBetweenEvents)

                if (!isActive) break // Check if the coroutine is still active (if job hasn't been cancelled)
                if (!currentCoroutineContext().isActive) break

                // Set a "blank" state between events to reset the event display
                _gameState.value = _gameState.value.copy(eventValue = -1)

                // Add a brief delay for the blank state, similar to `runVisualGame`
                delay(300) // Adjust this delay as needed
            }

            audioPlayer.release()  // Ensure media player is released when the game finishes
            saveGameResultAndCheckHighScore(_score.value, _gameState.value.correctResponses)

            // Reset the state and mark the game as finished
            _gameState.value =
                _gameState.value.copy(eventValue = -1, currentIndex = -1, isGameFinished = true)
        }
    }

    private fun playSoundForEvent(eventValue: Int) {
        // Implement this function to play a sound based on the event value
        // e.g., map event values to specific sound files or tones
    }

    private suspend fun runVisualGame() {
        events.forEachIndexed { index, value ->
            // Reset the match attempt flag for each new event
            matchAttemptedForCurrentEvent = false

            // Update the game state with the current event value and index
            _gameState.value = _gameState.value.copy(eventValue = value, currentIndex = index, currentEventNumber = index + 1)

            // Log each step to verify the sequence
            Log.d("GameVM", "Visual Game - Showing value: $value at index: $index")

            // Wait for the specified interval before updating to the next event
            delay(timeBetweenEvents.toLong())

            // Reset the highlight to give a "blank" state between events
            _gameState.value = _gameState.value.copy(eventValue = -1)

            // Add a brief delay for the "blank" state before the next event
            delay(300)
        }
        saveGameResultAndCheckHighScore(_score.value, _gameState.value.correctResponses)

        _gameState.value = _gameState.value.copy(eventValue = -1, currentIndex = -1, isGameFinished = true) // Reset state after game ends
    }


    private fun runAudioVisualGame(){
        // Todo: Make work for Higher grade
    }

    fun endGame() {
        job?.cancel()
        job = null
        audioPlayer.release()  // Ensure media player is released at the end of the game
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as GameApplication)
                GameVM(application.userPreferencesRepository, application.database, application.applicationContext)
            }
        }
    }

    init {
        // Code that runs during creation of the vm
        viewModelScope.launch {
            userPreferencesRepository.highscore.collect {
                _highscore.value = it
            }
        }
    }
}

// Class with the different game types
enum class GameType{
    Audio,
    Visual,
    AudioVisual
}

data class GameState(
    val gameType: GameType = GameType.Visual,
    val eventValue: Int = -1,
    val currentIndex: Int = 0, // Track current position in the event list
    val matches: List<Int> = emptyList(), // Track indices where matches were found
    val currentEventNumber: Int = 0,  // Number of the current event
    val correctResponses: Int = 0,  // Number of correct responses
    val isError: Boolean = false, // Tracks if last match was incorrect
    val isGameFinished: Boolean = false // New property to indicate game end
)

class FakeVM: GameViewModel {
    override val gameState: StateFlow<GameState>
        get() = MutableStateFlow(GameState()).asStateFlow()
    override val score: StateFlow<Int>
        get() = MutableStateFlow(2).asStateFlow()
    override val highscore: StateFlow<Int>
        get() = MutableStateFlow(42).asStateFlow()
    override val nBack: Int
        get() = 2

    override fun setGameType(gameType: GameType) {
    }

    override fun startGame() {
    }

    override fun checkMatch() {
    }

    override fun setGameSettings(
        nBackValue: Int,
        timeBetweenEvents: Int,
        numberOfEvents: Int,
        mode: String
    ) {
    }
}