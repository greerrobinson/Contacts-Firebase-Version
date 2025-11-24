package capstone.defaultapps.contacts.LoginActivity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import capstone.defaultapps.contacts.R
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: (String) -> Unit, // Pass user ID when login succeeds
    viewModel: LoginViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Navigate to main app when user is authenticated
    LaunchedEffect(uiState.currentUser) {
        uiState.currentUser?.let { user ->
            onLoginSuccess(user.uid)
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.blue_1)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Title
            Text(
                text = "Contacts App",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = colorResource(R.color.black)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (uiState.isLoginMode) "Sign In" else "Create Account",
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                color = colorResource(R.color.black)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Email Field
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email", color = colorResource(R.color.blue_3)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                enabled = !uiState.isLoading,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colorResource(R.color.blue_3),
                    unfocusedBorderColor = Color.Gray
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Password Field
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password", color = colorResource(R.color.blue_3)) },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                enabled = !uiState.isLoading,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colorResource(R.color.blue_3),
                    unfocusedBorderColor = Color.Gray
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Main Action Button
            Button(
                onClick = {
                    if (uiState.isLoginMode) {
                        viewModel.signInWithEmailPassword(email, password)
                    } else {
                        viewModel.registerWithEmailPassword(email, password)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !uiState.isLoading && email.isNotBlank() && password.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(R.color.blue_3),
                    disabledContainerColor = Color.Gray,
                    contentColor = Color.White,
                    disabledContentColor = Color.White
                )
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = colorResource(R.color.black)
                    )
                } else {
                    Text(
                        text = if (uiState.isLoginMode) "Sign In" else "Create Account",
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Toggle Mode Button
            TextButton(
                onClick = { viewModel.toggleMode() },
                enabled = !uiState.isLoading
            ) {
                Text(
                    text = if (uiState.isLoginMode) {
                        "Don't have an account? Sign up"
                    } else {
                        "Already have an account? Sign in"
                    },
                    color = colorResource(R.color.blue_3)
                )
            }

            // Error Message
            uiState.errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = colorResource(R.color.error_background))
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = colorResource(R.color.error_text)
                    )
                }
            }
        }
    }
}