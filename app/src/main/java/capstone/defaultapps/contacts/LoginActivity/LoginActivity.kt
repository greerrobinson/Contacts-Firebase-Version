package capstone.defaultapps.contacts.LoginActivity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import capstone.defaultapps.contacts.ContactsApplication
import capstone.defaultapps.contacts.MainActivity
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private val TAG = "LoginActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LoginScreen(
                onLoginSuccess = { userId ->
                    Log.d(TAG, "Login successful for user: $userId")
                    // Set user ID in repository for Firestore
                    val application = applicationContext as ContactsApplication
                    // Use a coroutine since setUUID is now suspend
                    GlobalScope.launch {
                        application.repository.setUUID(userId)
                    }
                    // Launch main activity
                    launchApplication()
                }
            )
        }
    }

    private fun launchApplication() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}