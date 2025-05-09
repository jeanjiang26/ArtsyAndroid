package com.jeannie.artsyfinal.screens
import androidx.compose.ui.graphics.Color
import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jeannie.artsyfinal.viewmodel.AuthViewModel
import com.jeannie.artsyfinal.viewmodel.LoginViewModel
import com.jeannie.artsyfinal.viewmodel.LoginViewModelFactory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onNavigateBack: () -> Unit,
    onLoginSuccess: () -> Unit,
    onRegisterClick: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current.applicationContext as Application
    val viewModel: LoginViewModel = viewModel(factory = LoginViewModelFactory(context))

    val email by viewModel.email.collectAsState()
    val password by viewModel.password.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val loginError by viewModel.loginError.collectAsState()
    val loginSuccess by viewModel.loginSuccess.collectAsState()

    // Collect validation error states
    val emailError by viewModel.emailError.collectAsState()
    val passwordError by viewModel.passwordError.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Track focus states locally
    var emailFieldFocused by remember { mutableStateOf(false) }
    var passwordFieldFocused by remember { mutableStateOf(false) }

    LaunchedEffect(loginSuccess) {
        if (loginSuccess) {
            scope.launch {
                snackbarHostState.showSnackbar("Logged in successfully")
                onLoginSuccess()
            }
        }
    }

    // Focus manager to handle clearing focus
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier.fillMaxHeight(),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Text(
                            "Login",
                            fontSize = 30.sp,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                },
                navigationIcon = {
                    Box(
                        modifier = Modifier.fillMaxHeight(),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFE8E8FF),
                    titleContentColor = Color.Black
                ),
                modifier = Modifier.height(120.dp)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.White
    ){ paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Email field with validation
            OutlinedTextField(
                value = email,
                onValueChange = { viewModel.updateEmail(it) },
                label = { Text("Email", color = if (emailError != null) Color.Red else Color.Gray) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged {
                        val wasFocused = emailFieldFocused
                        emailFieldFocused = it.isFocused
                        // Only call onEmailFocusChanged when focus is lost
                        if (wasFocused && !it.isFocused) {
                            viewModel.onEmailFocusChanged(false)
                        }
                    },
                isError = emailError != null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (emailError != null) Color.Red else Color(0xFF4A5FAF),
                    unfocusedBorderColor = if (emailError != null) Color.Red else Color.Gray,
                    errorBorderColor = Color.Red,
                    focusedLabelColor = if (emailError != null) Color.Red else Color(0xFF4A5FAF),
                    unfocusedLabelColor = if (emailError != null) Color.Red else Color.Gray,
                    errorLabelColor = Color.Red,
                    focusedTextColor = if (emailError != null) Color.Red else Color.Black,
                    unfocusedTextColor = if (emailError != null) Color.Red else Color.Black,
                    errorTextColor = Color.Red,
                ),
                supportingText = null, // Remove the default supporting text
                singleLine = true
            )

            if (emailError != null) {
                Text(
                    text = emailError ?: "",
                    color = Color.Red,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Password field with validation
            OutlinedTextField(
                value = password,
                onValueChange = { viewModel.updatePassword(it) },
                label = { Text("Password", color = if (passwordError != null) Color.Red else Color.Gray) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged {
                        val wasFocused = passwordFieldFocused
                        passwordFieldFocused = it.isFocused
                        // Only call onPasswordFocusChanged when focus is lost
                        if (wasFocused && !it.isFocused) {
                            viewModel.onPasswordFocusChanged(false)
                        }
                    },
                isError = passwordError != null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (passwordError != null) Color.Red else Color(0xFF4A5FAF),
                    unfocusedBorderColor = if (passwordError != null) Color.Red else Color.Gray,
                    errorBorderColor = Color.Red,
                    focusedLabelColor = if (passwordError != null) Color.Red else Color(0xFF4A5FAF),
                    unfocusedLabelColor = if (passwordError != null) Color.Red else Color.Gray,
                    errorLabelColor = Color.Red,
                    focusedTextColor = if (passwordError != null) Color.Red else Color.Black,
                    unfocusedTextColor = if (passwordError != null) Color.Red else Color.Black,
                    errorTextColor = Color.Red,
                ),
                supportingText = null, // Remove the default supporting text
                singleLine = true
            )

            if (passwordError != null) {
                Text(
                    text = passwordError ?: "",
                    color = Color.Red,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Login button
            Button(
                onClick = { viewModel.loginUser(authViewModel) },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4A5FAF),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFFADBDC6)
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text("Login")
                }
            }

            // General login error (e.g. incorrect credentials)
            if (loginError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = loginError ?: "",
                    color = Color.Red,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Register link
            val annotatedString = buildAnnotatedString {
                withStyle(style = SpanStyle(color = Color.Black)) {
                    append("Don't have an account yet? ")
                }
                withStyle(style = SpanStyle(
                    color = Color(0xFF4A5FAF),
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )) {
                    append("Register")
                }
            }

            Text(
                text = annotatedString,
                modifier = Modifier.clickable { onRegisterClick() }
            )
        }
    }
}