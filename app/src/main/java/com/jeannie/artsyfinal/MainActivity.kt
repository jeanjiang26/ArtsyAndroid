package com.jeannie.artsyfinal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jeannie.artsyfinal.screens.*
import com.jeannie.artsyfinal.ui.theme.ArtsyFinalTheme
import com.jeannie.artsyfinal.viewmodel.AuthViewModel
import android.util.Log
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import android.app.Application

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ArtsyFinalTheme(dynamicColor = false) { // Explicitly disable dynamic color
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val authViewModel: AuthViewModel = viewModel()

                    LaunchedEffect(Unit) {
                        Log.d("MainActivity", "AuthViewModel instance: ${authViewModel.hashCode()}")
                    }

                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        // Define animations at the NavHost level
                        enterTransition = {
                            slideIntoContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                                animationSpec = tween(300)
                            )
                        },
                        exitTransition = {
                            slideOutOfContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                                animationSpec = tween(300)
                            )
                        },
                        popEnterTransition = {
                            slideIntoContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                                animationSpec = tween(300)
                            )
                        },
                        popExitTransition = {
                            slideOutOfContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                                animationSpec = tween(300)
                            )
                        }
                    ) {
                        composable("home") {
                            HomeScreen(
                                authViewModel = authViewModel,
                                onSearchClick = { navController.navigate("search") },
                                onLoginClick = { navController.navigate("login") },
                                onArtistClick = { artistId: String ->
                                    // Navigate to artist detail with timestamp to force refresh
                                    val timestamp = System.currentTimeMillis()
                                    navController.navigate("artist_detail/$artistId?t=$timestamp")
                                }
                            )
                        }

                        composable("login") {
                            LoginScreen(
                                authViewModel = authViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onLoginSuccess = {
                                    navController.navigate("home") {
                                        popUpTo("home") { inclusive = true }
                                    }
                                },
                                onRegisterClick = { navController.navigate("register") }
                            )
                        }

                        composable("register") {
                            RegisterScreen(
                                authViewModel = authViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onLoginClick = { navController.navigate("login") },
                                onRegisterSuccess = {
                                    navController.navigate("home") {
                                        popUpTo("home") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("search") {
                            SearchScreen(
                                authViewModel = authViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onArtistClick = { artistId ->
                                    val timestamp = System.currentTimeMillis()
                                    navController.navigate("artist_detail/$artistId?t=$timestamp")
                                }
                            )
                        }

                        composable(
                            route = "artist_detail/{artistId}?t={t}",
                            arguments = listOf(
                                navArgument("artistId") { type = NavType.StringType },
                                navArgument("t") {
                                    type = NavType.StringType
                                    defaultValue = ""
                                }
                            )
                        ) { backStackEntry ->
                            val artistId = backStackEntry.arguments?.getString("artistId") ?: return@composable
                            ArtistDetailScreen(
                                artistId = artistId,
                                onNavigateBack = { navController.popBackStack() },
                                onSimilarArtistClick = { similarArtistId ->
                                    // Navigate to the new artist detail without removing the current one from back stack
                                    val timestamp = System.currentTimeMillis()
                                    navController.navigate("artist_detail/$similarArtistId?t=$timestamp")
                                    // Removed the popUpTo block to preserve back navigation
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}