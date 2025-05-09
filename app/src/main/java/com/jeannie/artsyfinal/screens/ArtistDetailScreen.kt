package com.jeannie.artsyfinal.screens
import androidx.compose.material.icons.filled.AccountBox
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.jeannie.artsyfinal.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.jeannie.artsyfinal.models.Artist
import com.jeannie.artsyfinal.models.ArtistDetail
import com.jeannie.artsyfinal.models.Artwork
import com.jeannie.artsyfinal.models.Category
import com.jeannie.artsyfinal.viewmodel.ArtistDetailViewModel
import com.jeannie.artsyfinal.viewmodel.AuthState
import com.jeannie.artsyfinal.viewmodel.AuthViewModel
import com.jeannie.artsyfinal.viewmodel.Result
import com.jeannie.artsyfinal.viewmodel.SearchViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistDetailScreen(
    artistId: String,
    onNavigateBack: () -> Unit,
    viewModel: ArtistDetailViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel(),
    searchViewModel: SearchViewModel = viewModel(),
    onSimilarArtistClick: (String) -> Unit,
) {
    val artistDetails by viewModel.artistDetails.collectAsState()
    val artworks by viewModel.artworks.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val similarArtists by viewModel.similarArtists.collectAsState()

    LaunchedEffect(Unit) {
        // Force immediate auth check when screen loads
        authViewModel.checkAuthStatus()
        Log.d("ArtistDetailScreen", "Initial auth check: ${authViewModel.authState.value}")
    }

    // Get favorite state from SearchViewModel
    val favorites by searchViewModel.favorites.collectAsState()
    val isFavorite = favorites.contains(artistId)

    // Then add more aggressive state observation
    val authState by authViewModel.authState.collectAsState()
    // Add a log statement to track real-time changes
    LaunchedEffect(authState) {
        Log.d("ArtistDetailScreen", "Auth state updated: $authState")
    }

    // Adjust the isAuthenticated tracking to update more aggressively
    var isAuthenticated by remember { mutableStateOf(false) }
    LaunchedEffect(authState) {
        isAuthenticated = authState is AuthState.Authenticated
        Log.d("ArtistDetailScreen", "Updated isAuthenticated: $isAuthenticated")
    }

    // Reset tab selection when auth state changes
    var selectedTabIndex by remember(isAuthenticated) {
        mutableStateOf(0)
    }

    // Determine which tabs to show based on authentication status
    val tabs = if (isAuthenticated) {
        listOf("Details", "Artworks", "Similar")
    } else {
        listOf("Details", "Artworks")
    }

    // For snackbar messages
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Re-run the effect when authentication state changes to refresh data appropriately
    LaunchedEffect(artistId, isAuthenticated) {
        viewModel.loadArtistDetails(artistId)
        // Only load similar artists if user is authenticated
        if (isAuthenticated) {
            viewModel.loadSimilarArtists(artistId)
        }
        // We don't need to explicitly clear similarArtists - the view will
        // just not show the Similar tab when unauthenticated
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
                            text = artistDetails?.name ?: "Artist Detail",
                            style = MaterialTheme.typography.titleLarge,
                            fontSize = 30.sp,
                            modifier = Modifier.padding(bottom = 16.dp),
                            color = MaterialTheme.colorScheme.onPrimary
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
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier.fillMaxHeight(),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        // Only show star icon when authenticated
                        if (isAuthenticated) {
                            IconButton(
                                onClick = {
                                    Log.e("ArtistDetailScreen", "Starting favorite toggle operation")
                                    Log.e("ArtistDetailScreen", "artistDetails: $artistDetails")
                                    Log.e("ArtistDetailScreen", "searchViewModel: $searchViewModel")
                                    Log.e("ArtistDetailScreen", "isFavorite current state: $isFavorite")

                                    scope.launch {
                                        try {
                                            val artist = artistDetails?.let {
                                                Artist(
                                                    id = it.artistId ?: return@let null,
                                                    name = it.name ?: return@let null,
                                                    imageUrl = it.imageUrl,
                                                    birthday = it.birthday,
                                                    nationality = it.nationality
                                                )
                                            }

                                            if (artist != null) {
                                                if (isFavorite) {
                                                    searchViewModel.removeFavorite(artist)
                                                    snackbarHostState.showSnackbar("Removed from Favorites")
                                                } else {
                                                    searchViewModel.addFavorite(artist)
                                                    snackbarHostState.showSnackbar("Added to Favorites")
                                                }
                                            }
                                        } catch (e: Exception) {
                                            Log.e("ArtistDetailScreen", "Error updating favorites: ${e.javaClass.simpleName}: ${e.message}", e)
                                            snackbarHostState.showSnackbar("Error: ${e.javaClass.simpleName}")
                                        }
                                    }
                                },
                                modifier = Modifier.padding(bottom = 12.dp)
                            ) {
//                                Icon(
//                                    imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
//                                    contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
//                                    tint = if (isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onPrimary
//                                )

                                Icon(
                                    imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                                    contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                                    tint = if (isFavorite) Color(0xFFFFD700) else MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(32.dp)
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
        Column(modifier = Modifier.padding(paddingValues)) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.background, // Background will be white in light mode, black in dark mode
                contentColor = MaterialTheme.colorScheme.primary // Text color will be dark blue in light mode, light blue in dark mode
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        icon = {
                            Icon(
                                imageVector = when (index) {
                                    0 -> Icons.Default.Info
                                    1 -> Icons.Default.AccountBox
                                    else -> Icons.Default.Person
                                },
                                contentDescription = when (index) {
                                    0 -> "Details"
                                    1 -> "Artworks"
                                    else -> "Similar"
                                },
                                tint = MaterialTheme.colorScheme.tertiary // Always dark blue in light mode, light blue in dark mode
                            )
                        },
                        text = {
                            Text(
                                text = title,
                                color = MaterialTheme.colorScheme.tertiary // Always dark blue in light mode, light blue in dark mode
                            )
                        }
                    )
                }
            }

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Loading...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                error != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Text(
                            "Error: $error",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                else -> {
                    // Determine which content to show based on selected tab and authentication
                    when {
                        selectedTabIndex == 0 -> DetailsTab(artistDetails)
                        selectedTabIndex == 1 -> ArtworksTab(artworks)
                        selectedTabIndex == 2 && isAuthenticated -> SimilarArtistsTab(
                            similarArtists = similarArtists,
                            onSimilarArtistClick = onSimilarArtistClick,
                            authViewModel = authViewModel,
                            searchViewModel = searchViewModel
                        )
                        else -> Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Please log in to access this feature",
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailsTab(artist: ArtistDetail?) {
    if (artist == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No artist details available",
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Display name if it exists
            artist.name?.let { name ->
                if (name.isNotBlank()) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            // Combine nationality and birth/death years
            val nationality = artist.nationality?.takeIf { it.isNotBlank() }
            val birthYear = artist.birthday?.takeIf { it.isNotBlank() }?.let { extractYear(it) }
            val deathYear = artist.deathday?.takeIf { it.isNotBlank() }?.let { extractYear(it) }

            // Create combined info text
            val hasYears = birthYear != null || deathYear != null
            val yearsText = when {
                birthYear != null && deathYear != null -> "$birthYear – $deathYear" // Use proper en-dash
                birthYear != null -> "b. $birthYear"
                deathYear != null -> "d. $deathYear"
                else -> ""
            }

            val combinedText = when {
                nationality != null && hasYears -> "$nationality, $yearsText"
                nationality != null -> nationality
                hasYears -> yearsText
                else -> ""
            }

            // Display combined information if available
            if (combinedText.isNotBlank()) {
                Text(
                    text = combinedText,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Display biography with improved formatting
            artist.biography?.takeIf { it.isNotBlank() }?.let { rawBio ->
                // Clean up the biography text
                val cleanedBio = rawBio
                    .replace("  ", "–") // Replace corrupt character with en dash
                    .replace(" )", ")") // Fix spacing issues
                    .replace("( ", "(") // Fix spacing issues
                    .replace("\n\n", "@@PARAGRAPH@@") // Preserve paragraph breaks
                    .replace("\n", " ") // Replace single line breaks with spaces
                    .replace("@@PARAGRAPH@@", "\n\n") // Restore paragraph breaks
                    .replace("&nbsp;", " ") // Fix common HTML entities
                    .replace("&amp;", "&")
                    .replace("&lt;", "<")
                    .replace("&gt;", ">")
                    .replace("&quot;", "\"")
                    .replace("&#39;", "'")

                // Display the biography with improved formatting
                Text(
                    text = cleanedBio,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    textAlign = TextAlign.Start, // Ensure left alignment for readability
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

// Helper function to extract year from a date string
private fun extractYear(dateString: String): String? {
    // Common pattern: extract 4 consecutive digits that would represent a year
    val yearRegex = "\\d{4}".toRegex()
    val matchResult = yearRegex.find(dateString)
    return matchResult?.value
}

@Composable
fun ArtworksTab(artworks: List<Artwork>?) {
    if (artworks.isNullOrEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(),
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 2.dp
            ) {
                Box(
                    modifier = Modifier
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No Artworks",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // The rest of the screen remains empty
            Spacer(modifier = Modifier.weight(1f))
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(artworks) { artwork ->
                ArtworkCard(artwork)
            }
        }
    }
}

@Composable
fun ArtworkCard(artwork: Artwork) {
    var showDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column {
            artwork.imageUrl?.let {
                AsyncImage(
                    model = it,
                    contentDescription = artwork.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.5f, matchHeightConstraintsFirst = false),
                    contentScale = ContentScale.Crop
                )
            }

            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally // Center all items in column
            ) {
                Text(
                    text = artwork.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 12.dp),
                    textAlign = TextAlign.Center, // Center the text alignment
                    color = MaterialTheme.colorScheme.onSurface
                )

                Button(
                    onClick = { showDialog = true },
                    // Content-width button
                    modifier = Modifier.wrapContentWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    )
                ) {
                    Text("View categories")
                }
            }
        }
    }

    if (showDialog) {
        CategoriesDialog(
            artwork = artwork,
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
fun CategoriesDialog(
    artwork: Artwork,
    onDismiss: () -> Unit,
    viewModel: ArtistDetailViewModel = viewModel()
) {
    var categoriesState by remember { mutableStateOf<Result<List<Category>>>(Result.Loading) }

    LaunchedEffect(artwork.id) {
        viewModel.getArtworkCategories(artwork.id).collect { result ->
            categoriesState = result
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Categories",
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            // Use a Box with heightIn to control the minimum height
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    // Set minimum height for the dialog content
                    .heightIn(min = 400.dp)
            ) {
                when (categoriesState) {
                    is Result.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Loading...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    is Result.Error -> {
                        Text(
                            "Error: ${(categoriesState as Result.Error).message}",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    is Result.Success -> {
                        val categories = (categoriesState as Result.Success).data
                        if (categories.isEmpty()) {
                            Text(
                                "No categories available",
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        } else {
                            // The carousel can now take more vertical space
                            CategoriesCarouselWithButtons(categories)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                )
            ) {
                Text("Close")
            }
        },
        containerColor = MaterialTheme.colorScheme.outline,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
fun CategoriesCarouselWithButtons(categories: List<Category>) {
    var currentIndex by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Category Card with navigation buttons
        Box(
            modifier = Modifier
                .width(340.dp)
                .height(600.dp)
        ) {
            CategoryCard(categories[currentIndex])

            // Navigation buttons overlay
            Row(
                modifier = Modifier
                    .width(400.dp)
                    .align(Alignment.Center)
                    .padding(horizontal = 1.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left button
                IconButton(
                    onClick = {
                        if (currentIndex > 0) currentIndex--
                    },
                    enabled = currentIndex > 0
                ) {
                    Text(
                        text = "<",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.size(32.dp),
                        textAlign = TextAlign.Center,
                        color = if (currentIndex > 0)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }

                // Right button
                IconButton(
                    onClick = {
                        if (currentIndex < categories.size - 1) currentIndex++
                    },
                    enabled = currentIndex < categories.size - 1
                ) {
                    Text(
                        text = ">",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.size(32.dp),
                        textAlign = TextAlign.Center,
                        color = if (currentIndex < categories.size - 1)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryCard(category: Category) {
    // Log the category name
    Log.d("CategoryCard", "Category name: ${category.name}")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .heightIn(min = 800.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            category.imageUrl?.let {
                AsyncImage(
                    model = it,
                    contentDescription = category.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = category.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Create a scrollable text area for longer descriptions
            val scrollState = rememberScrollState()

            // Special descriptions for specific categories
            val descriptionText = when (category.name) {
                "1860–1969" -> "All art, design, decorative art, and architecture produced from roughly 1860 to 1970."

                "19th Century" -> "In the West, the 19th century witnessed major social and technological upheavals, spurred in large part by the Industrial Revolution and correlating trends: urbanization, frequently poor working and living conditions, and territorial expansion by emerging global superpowers. In this period, artistic patronage shifted increasingly towards the capitalist bourgeoisie and national academies, with a rising profile for art dealers and critics. The hub of Western artistic activity was Paris, and the French Academy and Salon represented the establishment, favoring Neoclassicism at the beginning of the century and later Romanticism. Social and political commentary appeared in the history paintings of Théodore Géricault and Francisco Goya as well as the Social Realist works of Gustave Courbet and Honoré Daumier. Broad defiance of the Salon system swelled in the second half of the century with provocative works by the likes of Édouard Manet, the new styles of Impressionism and Post-Impressionism, and the birth of the concepts of modernism and the avant-garde. Japan's opening to global trade and the subsequent arrival of Japanese objects in Europe were a significant influence. At the same time, photography emerged as a means of documenting social realities and an art form accessible to non-elites. Architecture was characterized by the use of new industrial materials and techniques, on the the one hand, and the revival of Classical and Gothic styles, on the other. In design and decorative arts, the Arts and Crafts Movement and Art Nouveau both responded to industrialization and mass production through their emphasis on handmade craft and organic forms."

                "En plein air" -> "French for \"in the open air,\" referring to painting done outdoors rather than in a studio. Impressionists were the first to champion this practice, which they used primarily to capture the transient effects of light on a landscape."

                "Figurative Painting" -> "Paintings that represent recognizable things in the visible world, as opposed to abstract painting. The variety of approaches to figurative painting is enormous, from Photorealism (like Chuck Close) to nearly abstract figuration (like Picasso's Analytic Cubism). While ignored by the Western avant-garde for much of the 20th century, figurative painting saw renewed interest with artists like Basquiat and Kiefer in the 1980s."

                "Figurative Art" -> "A general category for artworks that represent recognizable material in the visible world, as opposed to abstract art. The variety of approaches to figurative art is enormous and almost as diverse as the history of art itself. In painting, figuration ranges from Photorealism (like that of Chuck Close) at one end of the spectrum to nearly abstract (like Pablo Picasso's Analytic Cubism) at the other. For much of the 20th century, figurative painting was largely ignored by the Western avant-garde, but the course changed with the 1980s emergence of figurative painters like Anselm Kiefer, Jean-Michel Basquiat, and David Salle."

                "Attenuated Figure" -> "Representations of the human form in which a figure is stylized so as to look elongated or thinned out. As far back as the 16th century, the mannerist painter El Greco departed from the idealized, classical figures of Renaissance art in his drawn-out depictions of the human figure. Egon Schiele later adopted a similar style, often rendering grotesquely thin figures with a long wavering line, as if to express the precarious state of the individual in modern society. In response to the atrocities of World War II, Alberto Giacometti's textured bronze sculptures of gaunt human figures echoed the emaciated bodies of Jews persecuted during the Holocaust."

                "Black and White" -> "Artworks, including photographs, that are either all black, all white, or a combination of the two. In the West in the 20th century various reasons accounted for painters' use of this simplified palette. For example, Kazimir Malevich's *Black Square* was one of the first expressions of the artist's Suprematist theories. Additionally, Robert Rauschenberg made his modular panel \"White Paintings\" in 1951 with the aim of creating something that did not look as if it were done by hand. In doing so, he paved the way for later Minimalist art. In 1961, John Cage famously referred to Rauschenberg's works as \"airports for the lights, shadows, and particles,\" proposing a reading of them as reflections (and recipients) of the life occurring in front of them."

                "Etching/Engraving" -> "Most likely derived from the decoration of metal armor in Northern Europe during the 15th century, etching and engraving are the two major intaglio printmaking techniques. Though they produce visually similar results, etching involves incising a wax-coated metal plate with a sharp tool and placing the plate into an acid bath. The acid corrodes the exposed lines, leaving the waxy ground unaffected; the plate is then inked and printed, forcing the paper into the incised lines. Considered one of the great masters of the technique, Rembrandt van Rijn harnessed the medium's similarity to drawing with a pencil or pen. Engraving, the oldest intaglio technique and the most common method seen in Old Master prints, involves directly incising a metal matrix and then printing. Engraving arguably reached its pinnacle with Albrecht Dürer, who mastered the technique's precise and tapered lines to achieve great tonality as seen in this print."

                "Focus on the Social Margins" -> "\"I really believe there are things which nobody would see unless I photographed them.\" —Diane Arbus\nWorks that take social difference as their theme, documenting individuals or groups deemed to be marginal by society. Like Jacob A. Riis's seminal social documentary book *How the Other Half Lives*, which captured immigrants and poor people with compassionate directness, or Honoré Daumier's political prints of 19th-century France's disenfranchised, such works may destabilize our perception of what is \"mainstream\" or central to a culture, versus what is \"different\" or peripheral. Some of the most famous photographers of the past century, like Arbus and Nan Goldin, have drawn attention to racial, ethnic, or religious minorities, the disabled, bohemians, and individuals marginalized due to their sexual orientation."

                else -> "Category: ${category.name}"
            }

            // Use Column with scrollable modifier for longer texts
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .heightIn(min = 550.dp), // Limit height and make scrollable
            ) {
                Text(
                    text = descriptionText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun SimilarArtistsTab(
    similarArtists: List<Artist>?,
    onSimilarArtistClick: (String) -> Unit,
    authViewModel: AuthViewModel = viewModel(),
    searchViewModel: SearchViewModel = viewModel()
) {
    val favorites by searchViewModel.favorites.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Add Scaffold to display the snackbar
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (similarArtists.isNullOrEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 2.dp
                ) {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No Similar Artists Found",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // The rest of the screen remains empty
                Spacer(modifier = Modifier.weight(1f))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(similarArtists) { artist ->
                    ArtistCard(
                        artist = artist,
                        onClick = { onSimilarArtistClick(artist.id) },
                        authViewModel = authViewModel,
                        isFavorite = favorites.contains(artist.id),
                        onFavoriteToggle = { artistToToggle ->
                            scope.launch {
                                try {
                                    if (favorites.contains(artistToToggle.id)) {
                                        searchViewModel.removeFavorite(artistToToggle)
                                        snackbarHostState.showSnackbar("Removed from Favorites")
                                    } else {
                                        searchViewModel.addFavorite(artistToToggle)
                                        snackbarHostState.showSnackbar("Added to Favorites")
                                    }
                                } catch (e: Exception) {
                                    Log.e("SimilarArtistsTab", "Error updating favorites: ${e.javaClass.simpleName}: ${e.message}", e)
                                    snackbarHostState.showSnackbar("Error: ${e.javaClass.simpleName}")
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}