package com.example.unscramble.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.unscramble.data.MAX_NO_OF_WORDS
import com.example.unscramble.data.SCORE_INCREASE
import com.example.unscramble.data.allWords
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class GameViewModel : ViewModel() {

    // ---------- 1. Estado del juego expuesto como StateFlow de solo lectura ----------
    // Propiedad de copia de seguridad (backing property): _uiState solo se puede
    // modificar dentro de esta clase; la IU solo puede leer `uiState`.
    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    // ---------- 2. Intento actual del usuario ----------
    // No forma parte de GameUiState porque no necesita sobrevivir a un
    // cambio de configuración de la misma forma "persistida" que el resto;
    // se observa con mutableStateOf para que Compose recomponga al cambiar.
    var userGuess by mutableStateOf("")
        private set

    // ---------- 3. Datos internos del juego (no expuestos a la IU) ----------
    private lateinit var currentWord: String
    private var usedWords: MutableSet<String> = mutableSetOf()

    init {
        resetGame()
    }

    // ---------- 4. Iniciar / reiniciar la partida ----------
    fun resetGame() {
        usedWords.clear()
        _uiState.value = GameUiState(currentScrambledWord = pickRandomWordAndShuffle())
    }

    // ---------- 5. Actualizar lo que el usuario va escribiendo ----------
    fun updateUserGuess(guessedWord: String) {
        userGuess = guessedWord
    }

    // ---------- 6. Verificar el intento del usuario al presionar Submit ----------
    fun checkUserGuess() {
        if (userGuess.equals(currentWord, ignoreCase = true)) {
            // Intento correcto: sumar puntaje y preparar la siguiente ronda
            val updatedScore = _uiState.value.score.plus(SCORE_INCREASE)
            updateGameState(updatedScore)
        } else {
            // Intento incorrecto: solo activar la bandera de error
            _uiState.update { currentState ->
                currentState.copy(isGuessedWordWrong = true)
            }
        }
        // Limpiar el campo de texto en ambos casos
        updateUserGuess("")
    }

    // ---------- 7. Omitir la palabra actual al presionar Skip ----------
    fun skipWord() {
        updateGameState(_uiState.value.score)
        updateUserGuess("")
    }

    // ---------- 8. Preparar el estado para la siguiente ronda, o terminar el juego ----------
    private fun updateGameState(updatedScore: Int) {
        if (usedWords.size == MAX_NO_OF_WORDS) {
            // Última ronda: no se elige palabra nueva, se marca el fin del juego
            _uiState.update { currentState ->
                currentState.copy(
                    isGuessedWordWrong = false,
                    score = updatedScore,
                    isGameOver = true
                )
            }
        } else {
            // Ronda normal: se elige una palabra nueva y se incrementa el conteo
            _uiState.update { currentState ->
                currentState.copy(
                    isGuessedWordWrong = false,
                    currentScrambledWord = pickRandomWordAndShuffle(),
                    currentWordCount = currentState.currentWordCount.inc(),
                    score = updatedScore
                )
            }
        }
    }

    // ---------- 9. Elegir una palabra aleatoria no usada y desordenarla ----------
    private fun pickRandomWordAndShuffle(): String {
        currentWord = allWords.random()
        return if (usedWords.contains(currentWord)) {
            // Si ya se usó, se intenta de nuevo con otra palabra aleatoria
            pickRandomWordAndShuffle()
        } else {
            usedWords.add(currentWord)
            shuffleCurrentWord(currentWord)
        }
    }

    // ---------- 10. Desordenar las letras de una palabra ----------
    private fun shuffleCurrentWord(word: String): String {
        val tempWord = word.toCharArray()
        tempWord.shuffle()
        while (String(tempWord) == word) {
            tempWord.shuffle()
        }
        return String(tempWord)
    }
}
