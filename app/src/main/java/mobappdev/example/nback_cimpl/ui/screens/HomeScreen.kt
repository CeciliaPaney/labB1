package mobappdev.example.nback_cimpl.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import mobappdev.example.nback_cimpl.R
import mobappdev.example.nback_cimpl.ui.viewmodels.FakeVM
import mobappdev.example.nback_cimpl.ui.viewmodels.GameViewModel
import mobappdev.example.nback_cimpl.ui.viewmodels.GameType
import mobappdev.example.nback_cimpl.ui.viewmodels.GameVM



/**
 * This is the Home screen composable
 *
 * Currently this screen shows the saved highscore
 * It also contains a button which can be used to show that the C-integration works
 * Furthermore it contains two buttons that you can use to start a game
 *
 * Date: 25-08-2023
 * Version: Version 1.0
 * Author: Yeetivity
 *
 */

@Composable
fun HomeScreen(
    vm: GameViewModel,
    onStartGame: (GameType) -> Unit // New parameter to handle navigation to GameScreen
) {

    val gameVM = vm as? GameVM
    LaunchedEffect(Unit) {
        if (vm is GameVM) {
            vm.endGame() // Call endGame to stop any playing audio
        }
    }
    val highscore by vm.highscore.collectAsState()
    val gameState by vm.gameState.collectAsState()
    val snackBarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // State variables for game settings
    var nBackValue by remember { mutableStateOf(2) }
    var timeBetweenEvents by remember { mutableStateOf(1000) }
    var numberOfEvents by remember { mutableStateOf(20) }
    var selectedMode by remember { mutableStateOf("Visual") }

    Scaffold(
        snackbarHost = { SnackbarHost(snackBarHostState) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                modifier = Modifier.padding(32.dp),
                text = "High-Score = $highscore",
                style = MaterialTheme.typography.headlineLarge
            )

            // Display current settings and sliders to configure game
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                // N-Back Value Selector
                Text(text = "N-Back Value: $nBackValue")
                Slider(
                    value = nBackValue.toFloat(),
                    onValueChange = { nBackValue = it.toInt() },
                    valueRange = 1f..5f,
                    steps = 4
                )

                // Time Between Events Selector
                Text(text = "Time Between Events (ms): $timeBetweenEvents")
                Slider(
                    value = timeBetweenEvents.toFloat(),
                    onValueChange = { timeBetweenEvents = it.toInt() },
                    valueRange = 1000f..4000f,
                    steps = 5
                )

                // Number of Events Selector
                Text(text = "Number of Events: $numberOfEvents")
                Slider(
                    value = numberOfEvents.toFloat(),
                    onValueChange = { numberOfEvents = it.toInt() },
                    valueRange = 10f..30f,
                    steps = 20
                )
            }

            // Buttons to select mode and start game
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = {
                    selectedMode = "Audio"
                    vm.setGameSettings(nBackValue, timeBetweenEvents, numberOfEvents, selectedMode)
                    scope.launch {
                        snackBarHostState.showSnackbar(
                            message = "Audio mode selected"
                        )
                    }
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.sound_on),
                        contentDescription = "Sound",
                        modifier = Modifier
                            .height(48.dp)
                            .aspectRatio(3f / 2f)
                    )
                }

                Button(onClick = {
                    selectedMode = "Visual"
                    vm.setGameSettings(nBackValue, timeBetweenEvents, numberOfEvents, selectedMode)
                    scope.launch {
                        snackBarHostState.showSnackbar(
                            message = "Visual mode selected"
                        )
                    }
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.visual),
                        contentDescription = "Visual",
                        modifier = Modifier
                            .height(48.dp)
                            .aspectRatio(3f / 2f)
                    )
                }
            }

            // Start the game with the current settings
            Button(
                onClick = {
                    vm.setGameSettings(nBackValue, timeBetweenEvents, numberOfEvents, selectedMode)
                    vm.startGame()  // Start the game
                    onStartGame(GameType.valueOf(selectedMode))   // Navigate to GameScreen
                },
                modifier = Modifier.padding(16.dp)
            ) {
                Text(text = "Start Game")
            }
        }
    }
}

@Preview
@Composable
fun HomeScreenPreview() {
    Surface {
        HomeScreen(
            vm = FakeVM(),
            onStartGame = {} // Provide an empty lambda for preview
        )
    }
}