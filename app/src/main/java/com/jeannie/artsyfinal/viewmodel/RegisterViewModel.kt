package com.jeannie.artsyfinal.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeannie.artsyfinal.network.ApiService
import com.jeannie.artsyfinal.network.RegisterRequest
import com.jeannie.artsyfinal.network.RetrofitClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.jeannie.artsyfinal.network.LoginRequest
import kotlinx.coroutines.flow.combine

class RegisterViewModel(private val context: Context) : ViewModel() {
    private val TAG = "RegisterViewModel"

    private val apiService: ApiService by lazy {
        RetrofitClient.getClientWithCookies(context).create(ApiService::class.java)
    }

    private val _fullName = MutableStateFlow("")
    val fullName = _fullName.asStateFlow()

    private val _email = MutableStateFlow("")
    val email = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password = _password.asStateFlow()

    private val _fullNameError = MutableStateFlow<String?>(null)
    val fullNameError = _fullNameError.asStateFlow()

    private val _emailError = MutableStateFlow<String?>(null)
    val emailError = _emailError.asStateFlow()

    private val _passwordError = MutableStateFlow<String?>(null)
    val passwordError = _passwordError.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _registerSuccess = MutableStateFlow(false)
    val registerSuccess = _registerSuccess.asStateFlow()

    private val _registerError = MutableStateFlow<String?>(null)
    val registerError = _registerError.asStateFlow()

    private val _fullNameInteracted = MutableStateFlow(false)
    private val _emailInteracted = MutableStateFlow(false)
    private val _passwordInteracted = MutableStateFlow(false)

    fun onFullNameChange(newValue: String) {
        _fullName.value = newValue
        if (_fullNameInteracted.value) validateFullName()
        _fullNameError.value = null
    }

    fun onEmailChange(newValue: String) {
        _email.value = newValue
        if (_emailInteracted.value) validateEmail()
        _emailError.value = null
    }

    fun onPasswordChange(newValue: String) {
        _password.value = newValue
        if (_passwordInteracted.value) validatePassword()
        _passwordError.value = null
    }

    fun onFullNameFocusChanged(isFocused: Boolean) {
        if (isFocused) {
            _fullNameError.value = null
        } else {
            _fullNameInteracted.value = true
            validateFullName()
        }
    }

//    val isFormValid = combine(
//        fullName,
//        email,
//        password,
//        fullNameError,
//        emailError,
//        passwordError
//    ) { fullName, email, password, nameErr, emailErr, passErr ->
//        fullName.isNotBlank() &&
//                email.isNotBlank() &&
//                android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() &&
//                password.isNotBlank() &&
//                nameErr == null && emailErr == null && passErr == null
//    }.stateIn(
//        scope = viewModelScope,
//        started = SharingStarted.WhileSubscribed(5000),
//        initialValue = false
//    )


    fun onEmailFocusChanged(isFocused: Boolean) {
        if (isFocused) {
            _emailError.value = null
        } else {
            _emailInteracted.value = true
            validateEmail()
        }
    }

    fun onPasswordFocusChanged(isFocused: Boolean) {
        if (isFocused) {
            _passwordError.value = null
        } else {
            _passwordInteracted.value = true
            validatePassword()
        }
    }

    private fun validateFullName(): Boolean {
        val name = _fullName.value.trim()
        return if (name.isEmpty()) {
            _fullNameError.value = "Full name cannot be empty"
            false
        } else {
            _fullNameError.value = null
            true
        }
    }

    private fun validateEmail(): Boolean {
        val emailVal = _email.value.trim()
        return when {
            emailVal.isEmpty() -> {
                _emailError.value = "Email cannot be empty"
                false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(emailVal).matches() -> {
                _emailError.value = "Invalid email format"
                false
            }
            else -> {
                _emailError.value = null
                true
            }
        }
    }

    private fun validatePassword(): Boolean {
        val passwordVal = _password.value
        return if (passwordVal.isEmpty()) {
            _passwordError.value = "Password cannot be empty"
            false
        } else {
            _passwordError.value = null
            true
        }
    }

    private fun validateAllFields(): Boolean {
        _fullNameInteracted.value = true
        _emailInteracted.value = true
        _passwordInteracted.value = true
        return validateFullName() && validateEmail() && validatePassword()
    }

    fun registerUser(authViewModel: AuthViewModel) {
        _registerError.value = null
        _registerSuccess.value = false

        if (!validateAllFields()) return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                Log.d(TAG, "===== REGISTRATION ATTEMPT =====")

                authViewModel.logout()
                delay(300)

                val freshApiService = RetrofitClient.createFreshClient(context).create(ApiService::class.java)
                val response = freshApiService.register(RegisterRequest(_fullName.value.trim(), _email.value.trim(), _password.value.trim()))
                val loginResponse = freshApiService.login(LoginRequest(_email.value.trim(), _password.value))

                if (loginResponse.user != null) {
                    authViewModel.setNewUser(loginResponse.user)
                } else {
                    authViewModel.checkAuthStatus()
                }

                delay(300)
                _registerSuccess.value = true

            } catch (e: Exception) {
                Log.e(TAG, "Registration error: ${e.message}")
                _registerError.value = e.message ?: "Something went wrong"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
