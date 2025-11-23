package capstone.defaultapps.contacts

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import capstone.defaultapps.contacts.LoginActivity.LoginActivity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val application = application as ContactsApplication
            val viewModel: ContactsViewModel = viewModel(
                factory = ContactsViewModelFactory(application.repository)
            )
            val uiState by viewModel.uiState.collectAsState()
            
            // Handle logout navigation
            LaunchedEffect(uiState.isLoggedOut) {
                if (uiState.isLoggedOut) {
                    startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                    finish()
                }
            }
            
            ContactsScreen(viewModel = viewModel)
        }
    }
}

