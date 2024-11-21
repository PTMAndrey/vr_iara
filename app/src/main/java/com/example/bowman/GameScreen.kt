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
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener

// Variabile globale pentru valori implicite
const val defaultHealth = 100
const val hitValue = 25

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun GameScreen(database: FirebaseDatabase, deviceId: String) {
    var playerRole by remember { mutableStateOf<String?>(null) }
    var playerHealth by remember { mutableStateOf(defaultHealth) }
    var opponentHealth by remember { mutableStateOf(defaultHealth) }
    var isMyTurn by remember { mutableStateOf(false) }
    var winnerMessage by remember { mutableStateOf<String?>(null) }

    val gameSessionRef = database.getReference("game_session/game_123")
    Text(text = "Your Role: $deviceId")
    println("Assigned device as $deviceId")
    // Assign the device to a role (player1 or player2) if not already assigned
    LaunchedEffect(Unit) {
        println("Device ID: $deviceId - Attempting to assign a role.")

        gameSessionRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val player1 = currentData.child("player1").getValue(String::class.java)
                val player2 = currentData.child("player2").getValue(String::class.java)

                return when {
                    player1 == deviceId -> {
                        // Already assigned as player1
                        println("Device $deviceId is already assigned as player1.")
                        Transaction.success(currentData)
                    }

                    player2 == deviceId -> {
                        // Already assigned as player2
                        println("Device $deviceId is already assigned as player2.")
                        Transaction.success(currentData)
                    }

                    player1.isNullOrEmpty() -> {
                        // Assign as player1
                        currentData.child("player1").value = deviceId
                        println("Device $deviceId assigned as player1.")
                        Transaction.success(currentData)
                    }

                    player2.isNullOrEmpty() -> {
                        // Assign as player2
                        currentData.child("player2").value = deviceId
                        println("Device $deviceId assigned as player2.")
                        Transaction.success(currentData)
                    }

                    else -> {
                        // No roles available
                        println("No roles available for device $deviceId.")
                        Transaction.abort()
                    }
                }
            }

            override fun onComplete(
                error: DatabaseError?,
                committed: Boolean,
                currentData: DataSnapshot?
            ) {
                if (error != null) {
                    println("Transaction failed: ${error.message}")
                    return
                }

                if (committed) {
                    val player1 = currentData?.child("player1")?.getValue(String::class.java)
                    val player2 = currentData?.child("player2")?.getValue(String::class.java)

                    when {
                        player1 == deviceId -> {
                            playerRole = "player1"
                            println("Device $deviceId successfully assigned as player1.")
                        }
                        player2 == deviceId -> {
                            playerRole = "player2"
                            println("Device $deviceId successfully assigned as player2.")
                        }
                        else -> {
                            println("Device $deviceId failed to get a role.")
                        }
                    }
                }
            }
        })
    }





    // Listen to game session updates
    LaunchedEffect(playerRole) {
        if (playerRole != null) {
            println("Listening for updates as $playerRole.")
            gameSessionRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val player1Health = snapshot.child("player1_health").getValue(Int::class.java) ?: defaultHealth
                    val player2Health = snapshot.child("player2_health").getValue(Int::class.java) ?: defaultHealth
                    val turn = snapshot.child("player_turn").getValue(String::class.java) ?: "player1"
                    val winner = snapshot.child("game_winner").getValue(String::class.java)

                    // If there's a winner, display the winner message
                    if (winner != null) {
                        winnerMessage = if (winner == playerRole) {
                            "Congratulations! You won the game."
                        } else {
                            "Game Over. You lost the game."
                        }
                        return
                    }

                    if (playerRole == "player1") {
                        playerHealth = player1Health
                        opponentHealth = player2Health
                        isMyTurn = turn == "player1"
                    } else if (playerRole == "player2") {
                        playerHealth = player2Health
                        opponentHealth = player1Health
                        isMyTurn = turn == "player2"
                    }
                    // Check if any player's health is 0 and determine the winner
                    if (player1Health <= 0 || player2Health <= 0) {
                        val winnerRole = if (player1Health <= 0) "player2" else "player1"
                        gameSessionRef.child("game_winner").setValue(winnerRole)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    println("Eroare la citirea datelor: ${error.message}")
                }
            })
        }
    }

    // UI for each player
    Scaffold {
        Column(modifier = Modifier.padding(64.dp)) {
            when {
                winnerMessage != null -> {
                    Text(text = winnerMessage!!)
                }

                playerRole == "player1" || playerRole == "player2" -> {
                    Text(text = "Your Role: $playerRole")
                    Text(text = "Your Health: $playerHealth")
                    Text(text = "Opponent Health: $opponentHealth")

                    if (isMyTurn) {
                        Button(onClick = {
                            // Reduce opponent health
                            val opponentHealthRef = if (playerRole == "player1") {
                                gameSessionRef.child("player2_health")
                            } else {
                                gameSessionRef.child("player1_health")
                            }
                            opponentHealthRef.setValue(opponentHealth - hitValue)

                            // Switch turn
                            val nextTurn = if (playerRole == "player1") "player2" else "player1"
                            gameSessionRef.child("player_turn").setValue(nextTurn)
                        }) {
                            Text("Hit Opponent")
                        }

                        Button(onClick = {
                            // Skip turn
                            val nextTurn = if (playerRole == "player1") "player2" else "player1"
                            gameSessionRef.child("player_turn").setValue(nextTurn)
                        }) {
                            Text("Miss Shot")
                        }
                    } else {
                        Text(text = "Waiting for your turn...")
                    }
                }

                else -> {
                    Text(text = "Identifying your role...")
                }
            }
        }
    }
}


// Funcție pentru a reseta jocul
fun restartGame(database: FirebaseDatabase) {
    val gameSessionRef = database.getReference("game_session/game_123")

    // Resetăm toate valorile din baza de date
    gameSessionRef.child("player1_health").setValue(defaultHealth)
    gameSessionRef.child("player2_health").setValue(defaultHealth)
    gameSessionRef.child("player_turn").setValue("player1")
    gameSessionRef.child("player1").setValue(null)
    gameSessionRef.child("player2").setValue(null)
}
