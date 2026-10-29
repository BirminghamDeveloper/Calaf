package com.calaf.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.calaf.core.designsystem.CalafTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as CalafApplication
        setContent { CalafTheme { CalafApp(app.auth, app.profiles, app.community, BuildConfig.DEBUG && BuildConfig.USE_FIREBASE_EMULATORS) } }
    }
}
