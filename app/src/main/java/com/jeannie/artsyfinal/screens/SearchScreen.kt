package com.jeannie.artsyfinal.screens
import android.util.Log
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.jeannie.artsyfinal.R
import com.jeannie.artsyfinal.models.Artist
import com.jeannie.artsyfinal.viewmodel.SearchViewModel
import com.jeannie.artsyfinal.viewmodel.AuthViewModel
import com.jeannie.artsyfinal.viewmodel.AuthState
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@Composable
fun DebugThemeColor() {
    val primaryColor = MaterialTheme.colorScheme.primary
    LaunchedEffect(Unit) {
        Log.d("ThemeDebug", "Primary color is: $primaryColor")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateBack: () -> Unit,
    onArtistClick: (String) -> Unit = {},
    viewModel: SearchViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val searchQuery by viewModel.query.collectAsState()
    val focusRequester = remember { FocusRequester() }
    val searchResults by viewModel.searchResults.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val favorites by viewModel.favorites.collectAsState()

    // Add state to track if we should show "No Results Found"
    var showNoResults by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    DebugThemeColor()

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Reset showNoResults when query changes or isLoading becomes true
    LaunchedEffect(searchQuery, isLoading) {
        if (isLoading) {
            showNoResults = false
        } else if (!isLoading && searchResults.isEmpty() && searchQuery.length >= 3) {
            // Delay showing "No Results Found" message
            delay(2000) // 2 second delay
            showNoResults = true
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .size(28.dp)
                            )

                            TextField(
                                value = searchQuery,
                                onValueChange = {
                                    viewModel.updateQuery(it)
                                    viewModel.searchArtists(it)
                                },
                                placeholder = {
                                    Text(
                                        "Search artists...",
                                        fontSize = 30.sp,
                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                                    )
                                },
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontSize = 30.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onPrimary
                                ),
                                colors = TextFieldDefaults.colors(
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent,
                                    cursorColor = MaterialTheme.colorScheme.onPrimary,
                                    focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onPrimary,
                                    focusedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                                ),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(
                                    onSearch = {
                                        viewModel.searchArtists(searchQuery.trim())
                                    }
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(focusRequester)
                            )

                            IconButton(
                                onClick = {
                                    if (searchQuery.isNotEmpty()) {
                                        viewModel.updateQuery("")
                                        viewModel.searchArtists("")
                                    }
                                    onNavigateBack()
                                },
                                modifier = Modifier.size(56.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Clear,
                                    contentDescription = if (searchQuery.isNotEmpty()) "Clear and Go Home" else "Go Home",
                                    tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.height(120.dp)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Loading...", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Error: $error", color = MaterialTheme.colorScheme.onBackground)
                }
            }
            // Changed condition here to use the new showNoResults state
            showNoResults && searchResults.isEmpty() && searchQuery.length >= 3 -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "No Results Found",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(searchResults) { artist ->
                        ArtistCard(
                            artist = artist,
                            onClick = { onArtistClick(artist.id) },
                            authViewModel = authViewModel,
                            isFavorite = favorites.contains(artist.id),
                            onFavoriteToggle = { artistToToggle ->
                                scope.launch {
                                    try {
                                        if (favorites.contains(artistToToggle.id)) {
                                            viewModel.removeFavorite(artistToToggle)
                                            snackbarHostState.showSnackbar("Removed from Favorites")
                                        } else {
                                            viewModel.addFavorite(artistToToggle)
                                            snackbarHostState.showSnackbar("Added to Favorites")
                                        }
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar("Error updating favorites")
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ArtistCard(
    artist: Artist,
    onClick: () -> Unit,
    authViewModel: AuthViewModel,
    onFavoriteToggle: (Artist) -> Unit,
    isFavorite: Boolean = false
) {
    val authState by authViewModel.authState.collectAsState()
    val isAuthenticated = authState is AuthState.Authenticated
    val isDarkTheme = isSystemInDarkTheme()

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = artist.imageUrl,
                contentDescription = artist.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                error = painterResource(id = R.drawable.artsy_logo_copy)
            )

            if (isAuthenticated) {
                IconButton(
                    onClick = { onFavoriteToggle(artist) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(48.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                        contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                        tint = if (isFavorite) Color(0xFFFFD700) else MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = artist.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "View details",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}