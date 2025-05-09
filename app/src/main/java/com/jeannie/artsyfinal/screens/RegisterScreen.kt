package com.jeannie.artsyfinal.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jeannie.artsyfinal.viewmodel.AuthViewModel
import com.jeannie.artsyfinal.viewmodel.RegisterViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onNavigateBack: () -> Unit,
    onRegisterSuccess: () -> Unit,
    onLoginClick: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val viewModel: RegisterViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return RegisterViewModel(context) as T
            }
        }
    )

    val fullName by viewModel.fullName.collectAsState()
    val email by viewModel.email.collectAsState()
    val password by viewModel.password.collectAsState()
    val fullNameError by viewModel.fullNameError.collectAsState()
    val emailError by viewModel.emailError.collectAsState()
    val passwordError by viewModel.passwordError.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val registerSuccess by viewModel.registerSuccess.collectAsState()
    val registerError by viewModel.registerError.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var fullNameFocused by remember { mutableStateOf(false) }
    var emailFocused by remember { mutableStateOf(false) }
    var passwordFocused by remember { mutableStateOf(false) }

    LaunchedEffect(registerSuccess) {
        if (registerSuccess) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Registered successfully")
                onRegisterSuccess()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.BottomStart) {
                        Text("Register", fontSize = 30.sp, modifier = Modifier.padding(bottom = 16.dp))
                    }
                },
                navigationIcon = {
                    Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.BottomStart) {
                        IconButton(onClick = onNavigateBack, modifier = Modifier.padding(bottom = 12.dp)) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.changes.all { it.pressed }) {
                                focusManager.clearFocus()
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { viewModel.onFullNameChange(it) },
                    label = { Text("Enter Full Name", color = if (fullNameError != null) Color.Red else Color.Gray) },
                    isError = fullNameError != null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (fullNameError != null) Color.Red else Color(0xFF4A5FAF),
                        unfocusedBorderColor = if (fullNameError != null) Color.Red else Color.Gray,
                        errorBorderColor = Color.Red,
                        focusedLabelColor = if (fullNameError != null) Color.Red else Color(0xFF4A5FAF),
                        unfocusedLabelColor = if (fullNameError != null) Color.Red else Color.Gray,
                        errorLabelColor = Color.Red,
                        focusedTextColor = if (fullNameError != null) Color.Red else Color.Black,
                        unfocusedTextColor = if (fullNameError != null) Color.Red else Color.Black,
                        errorTextColor = Color.Red,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged {
                            val wasFocused = fullNameFocused
                            fullNameFocused = it.isFocused
                            if (wasFocused && !it.isFocused) viewModel.onFullNameFocusChanged(false)
                            else if (!wasFocused && it.isFocused) viewModel.onFullNameFocusChanged(true)
                        }
                )
                if (fullNameError != null) {
                    Text(
                        text = fullNameError ?: "",
                        color = Color.Red,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { viewModel.onEmailChange(it) },
                    label = { Text("Enter Email", color = if (emailError != null) Color.Red else Color.Gray) },
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
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged {
                            val wasFocused = emailFocused
                            emailFocused = it.isFocused
                            if (wasFocused && !it.isFocused) viewModel.onEmailFocusChanged(false)
                            else if (!wasFocused && it.isFocused) viewModel.onEmailFocusChanged(true)
                        }
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

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { viewModel.onPasswordChange(it) },
                    label = { Text("Password", color = if (passwordError != null) Color.Red else Color.Gray) },
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
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged {
                            val wasFocused = passwordFocused
                            passwordFocused = it.isFocused
                            if (wasFocused && !it.isFocused) viewModel.onPasswordFocusChanged(false)
                            else if (!wasFocused && it.isFocused) viewModel.onPasswordFocusChanged(true)
                        }
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

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { viewModel.registerUser(authViewModel) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4A5FAF),
                        contentColor = Color.White
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text("Register")
                    }
                }

                if (registerError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = registerError ?: "",
                        color = Color.Red,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                val annotatedString = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = Color.Black)) {
                        append("Already have an account? ")
                    }
                    withStyle(style = SpanStyle(color = Color(0xFF4A5FAF))) {
                        append("Login")
                    }
                }

                Text(
                    text = annotatedString,
                    modifier = Modifier.clickable { onLoginClick() }
                )
            }
        }
    }
}