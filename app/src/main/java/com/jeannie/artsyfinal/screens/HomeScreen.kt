package com.jeannie.artsyfinal.screens
import androidx.compose.ui.text.style.TextAlign
import android.util.Log
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.jeannie.artsyfinal.models.Artist
import com.jeannie.artsyfinal.models.FavoriteArtistEntry
import com.jeannie.artsyfinal.viewmodel.AuthState
import com.jeannie.artsyfinal.viewmodel.AuthViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    authViewModel: AuthViewModel = viewModel(),
    onSearchClick: () -> Unit,
    onLoginClick: () -> Unit,
    onArtistClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val authState by authViewModel.authState.collectAsState()
    // Collect favorites from authViewModel
    val favoriteEntries by authViewModel.favoriteEntries.collectAsState()
    var showDropdown by remember { mutableStateOf(false) }

    // Add a loading state
    var isRefreshing by remember { mutableStateOf(false) }

    // For triggering UI updates for relative timestamps
    var tickTrigger by remember { mutableStateOf(0L) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Create a unique screen instance ID that changes each time the screen appears
    val screenInstanceId = remember { System.currentTimeMillis() }

    // Function to handle refreshing favorites
    fun refreshFavorites() {
        Log.d("HomeScreen", "Refreshing favorites (instance: $screenInstanceId)")
        if (authState is AuthState.Authenticated) {
            isRefreshing = true
            scope.launch {
                try {
                    // Load favorites from ViewModel
                    authViewModel.loadFavorites()
                } finally {
                    isRefreshing = false
                }
            }
        }
    }

    // This effect runs every time HomeScreen appears with a new instance ID
    DisposableEffect(screenInstanceId) {
        Log.d("HomeScreen", "Screen appeared with instance ID: $screenInstanceId")
        refreshFavorites()

        onDispose {
            Log.d("HomeScreen", "Screen disposed: $screenInstanceId")
        }
    }

    // This runs when auth state changes
    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            Log.d("HomeScreen", "Auth state changed to Authenticated, refreshing")
            refreshFavorites()
        }
    }

    // Start a timer to update relative timestamps every second
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)  // Update every second
            tickTrigger = System.currentTimeMillis()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier.fillMaxHeight(),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Text(
                            "Artist Search",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier.fillMaxHeight(),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        Row(
                            modifier = Modifier.padding(bottom = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onSearchClick) {
                                Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray)
                            }
                            when (val state = authState) {
                                is AuthState.Authenticated -> {
                                    if (state.avatarUrl != null) {
                                        Box {
                                            AsyncImage(
                                                model = state.avatarUrl,
                                                contentDescription = "User Avatar",
                                                modifier = Modifier
                                                    .size(30.dp)
                                                    .clip(CircleShape)
                                                    .clickable { showDropdown = true }
                                            )
                                            DropdownMenu(
                                                expanded = showDropdown,
                                                onDismissRequest = { showDropdown = false }
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("Log Out",
                                                        color = Color(0xFF324185)
                                                    ) },
                                                    onClick = {
                                                        showDropdown = false
                                                        authViewModel.logout()
                                                        scope.launch {
                                                            snackbarHostState.showSnackbar("Logged out successfully")
                                                        }
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text(
                                                        "Delete Account",
                                                        color = Color.Red
                                                    ) },
                                                    onClick = {
                                                        showDropdown = false
                                                        authViewModel.deleteAccount()
                                                        scope.launch {
                                                            snackbarHostState.showSnackbar("Deleted user successfully")
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    } else {
                                        IconButton(onClick = onLoginClick) {
                                            Icon(Icons.Default.Person, contentDescription = "User", tint = Color.Gray)
                                        }
                                    }
                                }
                                else -> {
                                    IconButton(onClick = onLoginClick) {
                                        Icon(Icons.Default.Person, contentDescription = "User", tint = Color.Gray)
                                    }
                                }
                            }
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
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 24.dp)
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = getCurrentDate(),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(top = 10.dp, bottom = 10.dp)
            )

            Text(
                text = "Favorites",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF5F5F5))
                    .align(Alignment.CenterHorizontally),
                textAlign = TextAlign.Center
            )

            // Show loading indicator when refreshing
            if (isRefreshing) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            }

            when (authState) {
                is AuthState.Authenticated -> {
                    // Add debug logging
                    Log.d("HomeScreen", "Rendering authenticated state with ${favoriteEntries.size} favorites")

                    if (favoriteEntries.isEmpty() && !isRefreshing) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp)
                                .background(
                                    color = Color(0xFFE8E8FF).copy(alpha = 0.8f),
                                    shape = RoundedCornerShape(size = 12.dp)
                                )
                                .padding(vertical = 16.dp, horizontal = 24.dp)
                        ) {
                            Text(
                                text = "No Favorites",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Black,
                                fontSize = 18.sp
                            )
                        }
                    } else {
                        // Pass the tick trigger to force recomposition for timestamp updates
                        FavoritesSection(
                            favoriteEntries = favoriteEntries,
                            onArtistClick = { artistId ->
                                // Just navigate to the artist detail screen
                                onArtistClick(artistId)
                            },
                            tickTrigger = tickTrigger
                        )
                    }
                }
                is AuthState.Loading -> {
                    CircularProgressIndicator()
                }
                is AuthState.Unauthenticated -> {
                    Button(
                        onClick = onLoginClick,
                        modifier = Modifier.padding(vertical = 13.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4A5FAF),
                            contentColor = Color.White
                        ),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text("Log in to see favorites", modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                    }
                }
            }

            TextButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.artsy.net/"))
                    context.startActivity(intent)
                },
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Text(
                    text = "Powered by Artsy",
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun FavoritesSection(
    favoriteEntries: List<FavoriteArtistEntry>,
    onArtistClick: (String) -> Unit,
    tickTrigger: Long = 0 // Parameter to force recomposition
) {
    Column {
        favoriteEntries.forEach { favoriteEntry ->
            val artist = favoriteEntry.artist
            val artistId = favoriteEntry.artistId
            val timestamp = favoriteEntry.timestamp

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onArtistClick(artistId) }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = artist.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    if (!artist.nationality.isNullOrBlank() || artist.birthday != null) {
                        Text(
                            text = listOfNotNull(artist.nationality, artist.birthday).joinToString(", "),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Calculate relative time - recomputed every time the tick trigger changes
                    val relativeTime = getRelativeTime(timestamp, tickTrigger)

                    // The relativeTime value will change every second due to the tickTrigger
                    Text(relativeTime, color = Color.Gray, fontSize = 12.sp)
                    Icon(
                        Icons.Default.KeyboardArrowRight,
                        contentDescription = "Go to artist",
                        tint = Color.Gray,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .size(14.dp)
                    )
                }
            }
        }
    }
}

// Function to get the current date
fun getCurrentDate(): String {
    val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
    return sdf.format(Date())
}

// Function to calculate relative time
fun getRelativeTime(timestamp: Long, currentTick: Long = System.currentTimeMillis()): String {
    // Calculate the difference in milliseconds
    val diffInMillis = currentTick - timestamp
    // Convert to seconds
    val seconds = Math.floor(diffInMillis / 1000.0).toInt()

    return when {
        seconds < 60 -> "${seconds} second${if (seconds == 1) "" else "s"} ago"
        else -> {
            val minutes = Math.floor(seconds / 60.0).toInt()
            when {
                minutes < 60 -> "${minutes} minute${if (minutes == 1) "" else "s"} ago"
                else -> {
                    val hours = Math.floor(minutes / 60.0).toInt()
                    when {
                        hours < 24 -> "${hours} hour${if (hours == 1) "" else "s"} ago"
                        else -> {
                            val days = Math.floor(hours / 24.0).toInt()
                            "${days} day${if (days == 1) "" else "s"} ago"
                        }
                    }
                }
            }
        }
    }
}