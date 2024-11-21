package com.example.bowman

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

// Variabile globale pentru valori implicite
const val defaultHealth = 100
const val hitValue = 25

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun GameScreen(database: FirebaseDatabase) {
    // Definim stările pentru datele jocului
    var player1Health by remember { mutableStateOf(defaultHealth) }
    var player2Health by remember { mutableStateOf(defaultHealth) }
    var playerTurn by remember { mutableStateOf("") }
    var gameWinner by remember { mutableStateOf<String?>(null) } // Variabilă pentru câștigător

    val gameSessionRef = database.getReference("game_session/game_123")

    // Ascultăm modificările bazei de date
    LaunchedEffect(Unit) {
        observeGameSession(
            gameSessionRef,
            onPlayer1HealthChanged = { player1Health = it },
            onPlayer2HealthChanged = { player2Health = it },
            onPlayerTurnChanged = { playerTurn = it }
        )
    }

    // Verificăm dacă unul dintre jucători a câștigat
    LaunchedEffect(player1Health, player2Health) {
        if (player1Health <= 0) {
            gameWinner = "Player 2" // Player 2 câștigă
        } else if (player2Health <= 0) {
            gameWinner = "Player 1" // Player 1 câștigă
        }
    }

    // UI-ul jocului
    Scaffold {
        if (gameWinner != null) {
            // UI-ul pentru câștigător și restartare joc
            Column(modifier = Modifier.padding(64.dp)) {
                Text(
                    text = "${gameWinner!!} has won the game!",
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Button(onClick = {
                    restartGame(database)
                    gameWinner = null // Resetăm câștigătorul local
                }) {
                    Text("Restart Game")
                }
            }
        } else {
            // UI-ul normal al jocului
            GameUI(
                playerTurn = playerTurn,
                player1Health = player1Health,
                player2Health = player2Health,
                database = database
            )
        }
    }
}

@Composable
fun GameUI(
    playerTurn: String,
    player1Health: Int,
    player2Health: Int,
    database: FirebaseDatabase
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "\nPlayer Turn: $playerTurn\n")
        Text(text = "Player 1 Health: $player1Health")
        Text(text = "Player 2 Health: $player2Health")

        // Butoane pentru a actualiza datele jocului
        if (playerTurn == "2" && player1Health > 0) {
            Button(onClick = {
                val player1HealthRef =
                    database.getReference("game_session/game_123/player1_health")
                player1HealthRef.setValue(player1Health - hitValue)
            }) {
                Text("Player 1 gets hit!")
            }
        }

        if (playerTurn == "1" && player2Health > 0) {
            Button(onClick = {
                val player2HealthRef =
                    database.getReference("game_session/game_123/player2_health")
                player2HealthRef.setValue(player2Health - hitValue)
            }) {
                Text("Player 2 gets hit!")
            }
        }

        Button(onClick = {
            val playerTurnRef = database.getReference("game_session/game_123/player_turn")

            // Determinăm valoarea următoare
            val nextTurn = if (playerTurn == "1") "2" else "1"

            // Actualizăm valoarea în Firebase
            playerTurnRef.setValue(nextTurn)
        }) {
            Text("Swap player")
        }
    }
}

fun observeGameSession(
    gameSessionRef: com.google.firebase.database.DatabaseReference,
    onPlayer1HealthChanged: (Int) -> Unit,
    onPlayer2HealthChanged: (Int) -> Unit,
    onPlayerTurnChanged: (String) -> Unit
) {
    gameSessionRef.addValueEventListener(object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            // Actualizăm stările cu datele din Firebase
            onPlayer1HealthChanged(
                snapshot.child("player1_health").getValue(Int::class.java) ?: defaultHealth
            )
            onPlayer2HealthChanged(
                snapshot.child("player2_health").getValue(Int::class.java) ?: defaultHealth
            )
            onPlayerTurnChanged(snapshot.child("player_turn").getValue(String::class.java) ?: "1")
        }

        override fun onCancelled(error: DatabaseError) {
            println("Eroare la citirea datelor: ${error.message}")
        }
    })
}

// Funcție pentru a reseta jocul
fun restartGame(database: FirebaseDatabase) {
    val gameSessionRef = database.getReference("game_session/game_123")

    // Resetăm toate valorile din baza de date
    gameSessionRef.child("player1_health").setValue(defaultHealth)
    gameSessionRef.child("player2_health").setValue(defaultHealth)
    gameSessionRef.child("player_turn").setValue("1")
}
