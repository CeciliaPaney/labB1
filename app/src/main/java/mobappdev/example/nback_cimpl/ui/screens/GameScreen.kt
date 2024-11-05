package mobappdev.example.nback_cimpl.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import mobappdev.example.nback_cimpl.ui.viewmodels.GameType
import mobappdev.example.nback_cimpl.ui.viewmodels.GameViewModel
import mobappdev.example.nback_cimpl.ui.viewmodels.FakeVM
import android.util.Log



@Composable
fun GameScreen(vm: GameViewModel, onGameEnd: () -> Unit) {
    val gameState by vm.gameState.collectAsState()
    val score by vm.score.collectAsState()

    // State to track if there was an error in the last guess
    var isError by remember { mutableStateOf(false) }

    // Animate button color based on `isError`
    val buttonColor by animateColorAsState(
        targetValue = if (gameState.isError) Color.Red else MaterialTheme.colorScheme.primary,
        animationSpec = tween(durationMillis = 500)
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Score: $score", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 24.dp))
        Text("Current Event: ${gameState.currentEventNumber}", style = MaterialTheme.typography.bodyLarge)

        when (gameState.gameType) {
            GameType.Visual -> VisualGameGrid(gameState.eventValue)
            GameType.Audio -> AudioGameScreen(gameState.eventValue, currentEventNumber = gameState.currentEventNumber, isError = gameState.isError, onMarkMatch = { vm.checkMatch() }, onGameEnd = onGameEnd)
            GameType.AudioVisual -> AudioVisualGameScreen(gameState.eventValue)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Button for user to mark an N-back match
        Button(
            onClick = {
                vm.checkMatch()
            },
            colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
            modifier = Modifier.padding(top = 24.dp)
        ) {
            Text("Mark N-back Match")
        }
        Button(onClick = { onGameEnd() }) {
            Text("End Game and Return to Home")
        }
    }
}

@Composable
fun VisualGameGrid(eventValue: Int) {
    Log.d("VisualGameGrid", "Current eventValue: $eventValue") // Debug log
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Visual Mode", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                for (row in 0 until 3) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (col in 0 until 3) {
                            val cellIndex = row * 3 + col + 1 // 1-based index
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .weight(1f)
                                    .padding(4.dp)
                                    .background(
                                        color = if (cellIndex == eventValue) Color.Red else Color.Gray // Use distinct colors
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$cellIndex",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (cellIndex == eventValue) Color.White else Color.Black
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}




@Composable
fun AudioGameScreen(eventValue: Int, currentEventNumber: Int, isError: Boolean, onMarkMatch: () -> Unit, onGameEnd: () -> Unit) {
    // Animate the button color based on `isError`
    val buttonColor by animateColorAsState(
        targetValue = if (isError) Color.Red else MaterialTheme.colorScheme.primary,
        animationSpec = tween(durationMillis = 500)
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text("Audio Mode", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))

        // Button for user to mark an N-back match
        Button(
            onClick = onMarkMatch,
            colors = ButtonDefaults.buttonColors(containerColor = buttonColor),  // Use animated color
            modifier = Modifier.padding(top = 24.dp)
        ) {
            Text("Mark N-back Match")
        }

        // Button to end the game and return to home
        Button(
            onClick = onGameEnd, // Calls the provided onGameEnd function
            modifier = Modifier.padding(top = 24.dp)
        ) {
            Text("End Game and Return to Home")
        }
    }
}

@Composable
fun AudioVisualGameScreen(eventValue: Int) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Audio-Visual Mode", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
        VisualGameGrid(eventValue) // Display the 3x3 grid
        Text("Audio Cue: $eventValue", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 16.dp))


    }
}

@Preview
@Composable
fun GameScreenPreview() {
    GameScreen(FakeVM(), onGameEnd = {})
}
