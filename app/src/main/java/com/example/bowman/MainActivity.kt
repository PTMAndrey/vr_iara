package com.example.bowman

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.bowman.ui.theme.BowmanTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.FirebaseDatabase

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        FirebaseApp.initializeApp(this);
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Apelăm GameScreen și transmitem FirebaseDatabase ca parametru
        val database = FirebaseDatabase.getInstance()

        setContent {
            BowmanTheme {
                GameScreen(database)
            }
        }
    }
}
