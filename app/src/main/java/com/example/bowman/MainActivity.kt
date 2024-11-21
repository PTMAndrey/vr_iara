package com.example.bowman

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.bowman.ui.theme.BowmanTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.FirebaseDatabase
import android.provider.Settings
import com.google.firebase.messaging.FirebaseMessaging


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        FirebaseApp.initializeApp(this);
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val deviceId = fetchDeviceId(this)
        //fetchDeviceId { deviceId ->
            // Apelăm GameScreen și transmitem FirebaseDatabase ca parametru
            val database = FirebaseDatabase.getInstance()
//            val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
            setContent {
                BowmanTheme {
                    GameScreen(database, deviceId)
                }
            }
        //}
    }
    fun fetchDeviceId(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }


//    fun fetchDeviceId(onTokenReceived: (String) -> Unit) {
//        FirebaseMessaging.getInstance().token
//            .addOnCompleteListener { task ->
//                if (task.isSuccessful) {
//                    val token = task.result
//                    onTokenReceived(token)
//                } else {
//                    println("Failed to fetch Firebase Instance ID: ${task.exception?.message}")
//                }
//            }
//    }

}
