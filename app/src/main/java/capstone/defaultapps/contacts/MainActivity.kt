package capstone.defaultapps.contacts

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val application = application as ContactsApplication
            val viewModel: ContactsViewModel = viewModel(
                factory = ContactsViewModelFactory(application.repository)
            )
            ContactsScreen(viewModel = viewModel)
        }
    }
}

