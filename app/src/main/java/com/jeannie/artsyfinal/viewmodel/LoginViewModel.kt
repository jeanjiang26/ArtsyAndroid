package com.jeannie.artsyfinal.viewmodel

import android.app.Application
import android.util.Log
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jeannie.artsyfinal.network.ApiService
import com.jeannie.artsyfinal.network.LoginRequest
import com.jeannie.artsyfinal.network.RetrofitClient
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import kotlinx.coroutines.delay

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val apiService = RetrofitClient.getClientWithCookies(application).create(ApiService::class.java)

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _loginSuccess = MutableStateFlow(false)
    val loginSuccess: StateFlow<Boolean> = _loginSuccess

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError

    // Add new states for individual field validations
    private val _emailError = MutableStateFlow<String?>(null)
    val emailError: StateFlow<String?> = _emailError

    private val _passwordError = MutableStateFlow<String?>(null)
    val passwordError: StateFlow<String?> = _passwordError

    // Track if fields have been actually interacted with (touched and focus lost)
    private val _emailInteracted = MutableStateFlow(false)
    private val _passwordInteracted = MutableStateFlow(false)

    fun updateEmail(newEmail: String) {
        _email.value = newEmail

        // Clear login error when user types
        _loginError.value = null

        // Only validate if user has already interacted with this field
        if (_emailInteracted.value) {
            validateEmail()
        }
    }

    fun updatePassword(newPassword: String) {
        _password.value = newPassword

        // Clear login error when user types
        _loginError.value = null

        // Only validate if user has already interacted with this field
        if (_passwordInteracted.value) {
            validatePassword()
        }
    }

    // Call this when email field loses focus
//    fun onEmailFocusChanged(isFocused: Boolean) {
//        if (!isFocused && _email.value.isNotEmpty()) {
//            // Only mark as interacted and validate when focus is lost and
//            // there was some interaction (not on initial render)
//            _emailInteracted.value = true
//            validateEmail()
//        } else if (!isFocused && _email.value.isEmpty() && _emailInteracted.value) {
//            // Only show error if field was previously interacted with and is now empty
//            _emailError.value = "Email cannot be empty"
//        }
//    }

    fun onEmailFocusChanged(isFocused: Boolean) {
        if (isFocused) {
            _emailError.value = null
        } else {
            _emailInteracted.value = true
            validateEmail()
        }
    }

    // Call this when password field loses focus
//    fun onPasswordFocusChanged(isFocused: Boolean) {
//        if (!isFocused && _password.value.isNotEmpty()) {
//            // Only mark as interacted and validate when focus is lost and
//            // there was some interaction
//            _passwordInteracted.value = true
//            validatePassword()
//        } else if (!isFocused && _password.value.isEmpty() && _passwordInteracted.value) {
//            // Only show error if field was previously interacted with and is now empty
//            _passwordError.value = "Password cannot be empty"
//        }
//    }

    fun onPasswordFocusChanged(isFocused: Boolean) {
        if (isFocused) {
            _passwordError.value = null
        } else {
            _passwordInteracted.value = true
            validatePassword()
        }
    }


    // Email validation logic
    private fun validateEmail(): Boolean {
        val emailVal = _email.value.trim()

        return when {
            emailVal.isEmpty() -> {
                _emailError.value = "Email cannot be empty"
                false
            }
            !Patterns.EMAIL_ADDRESS.matcher(emailVal).matches() -> {
                _emailError.value = "Invalid email format"
                false
            }
            else -> {
                _emailError.value = null
                true
            }
        }
    }

    // Password validation logic
    private fun validatePassword(): Boolean {
        val passwordVal = _password.value

        return when {
            passwordVal.isEmpty() -> {
                _passwordError.value = "Password cannot be empty"
                false
            }

            else -> {
                _passwordError.value = null
                true
            }
        }
    }

    // Validate all fields
    private fun validateAllFields(): Boolean {
        // Mark both fields as interacted
        _emailInteracted.value = true
        _passwordInteracted.value = true

        // Run all validations
        val isEmailValid = validateEmail()
        val isPasswordValid = validatePassword()

        return isEmailValid && isPasswordValid
    }

    fun loginUser(authViewModel: AuthViewModel) {
        // Clear previous login error
        _loginError.value = null
        _loginSuccess.value = false

        // Validate all fields
        if (!validateAllFields()) {
            return
        }

        _isLoading.value = true

        viewModelScope.launch {
            try {
                Log.d("LoginViewModel", "===== LOGIN ATTEMPT =====")
                Log.d("LoginViewModel", "Login for email: ${_email.value.trim()}")

                // First, explicitly clear any existing user session
                authViewModel.logout()

                // Short delay to ensure logout completes
                delay(300)

                // Now perform the login
                val response = apiService.login(LoginRequest(_email.value.trim(), _password.value))
                Log.d("LoginViewModel", "Login API response received")

                // Set the new authenticated user
                if (response.user != null) {
                    authViewModel.setNewUser(response.user)
                } else {
                    // If no user in response, check auth status
                    authViewModel.checkAuthStatus()
                }

                // Delay to allow the state to update
                delay(300)

                Log.d("LoginViewModel", "Auth state updated with new user")

                // Set login success to trigger navigation
                _loginSuccess.value = true

            } catch (e: HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                Log.e("LoginViewModel", "HTTP error: ${e.code()} - ${errorBody ?: "No body"}")

                // Show appropriate error message based on status code
                _loginError.value = when (e.code()) {
                    401 -> "Username or password is incorrect"
                    else -> "Server error: ${e.code()} - ${errorBody ?: "Unknown"}"
                }
            } catch (e: IOException) {
                Log.e("LoginViewModel", "Network error: ${e.message}")
                _loginError.value = "Network error: ${e.message}"
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Unknown error: ${e.message}")
                _loginError.value = e.message ?: "Login failed"
            } finally {
                _isLoading.value = false
            }
        }
    }
}