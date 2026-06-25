package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.Channel
import com.example.data.Playlist
import com.example.data.EpgManager
import com.example.data.EpgProgram

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IptvApp(
    viewModel: IptvViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val selectedPlaylistId by viewModel.selectedPlaylistId.collectAsStateWithLifecycle()
    val filteredChannels by viewModel.filteredChannels.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val playingChannel by viewModel.playingChannel.collectAsStateWithLifecycle()
    
    val isImporting by viewModel.isImporting.collectAsStateWithLifecycle()
    val importError by viewModel.importError.collectAsStateWithLifecycle()
    val importSuccess by viewModel.importSuccess.collectAsStateWithLifecycle()

    var showImportDialog by remember { mutableStateOf(false) }
    var showPlaylistsManager by remember { mutableStateOf(false) }

    // Configuration adaptative (Tablette vs Téléphone / Paysage vs Portrait)
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isExpanded = configuration.screenWidthDp >= 600

    // Gestion du bouton "Retour" physique ou virtuel sur Android
    BackHandler(enabled = selectedPlaylistId != null || playingChannel != null) {
        if (playingChannel != null) {
            viewModel.playChannel(null)
        } else if (selectedPlaylistId != null) {
            viewModel.selectPlaylist(null)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Lecteur IPTV",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    if (selectedPlaylistId != null) {
                        IconButton(onClick = { viewModel.selectPlaylist(null) }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Retour aux playlists")
                        }
                    } else {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_tv),
                            contentDescription = "Logo Lecteur",
                            modifier = Modifier.padding(start = 16.dp, end = 8.dp).size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showPlaylistsManager = true },
                        modifier = Modifier.testTag("manage_playlists_button")
                    ) {
                        Icon(Icons.Default.List, contentDescription = "Gérer les listes")
                    }
                    IconButton(
                        onClick = { showImportDialog = true },
                        modifier = Modifier.testTag("add_playlist_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Ajouter une liste")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            if (playlists.isEmpty()) {
                // Écran de démarrage s'il n'y a aucune playlist
                EmptyStateScreen(
                    onImportClick = { showImportDialog = true },
                    onLoadDemoClick = {
                        val demoText = getDemoM3u()
                        viewModel.importPlaylistText("Liste Démo Gratuite", demoText)
                    },
                    isImporting = isImporting
                )
            } else {
                if (selectedPlaylistId == null) {
                    // Sélection de la playlist
                    PlaylistSelectionScreen(
                        playlists = playlists,
                        onPlaylistSelect = { viewModel.selectPlaylist(it.id) },
                        onDeletePlaylist = { viewModel.deletePlaylist(it) }
                    )
                } else {
                    // Affichage des chaînes d'une playlist sélectionnée
                    val currentPlaylistName = playlists.find { it.id == selectedPlaylistId }?.name ?: "Chaînes"
                    
                    if (isLandscape || isExpanded) {
                        // Layout 2 colonnes pour tablette ou mode paysage
                        Row(modifier = Modifier.fillMaxSize()) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(end = 8.dp)
                            ) {
                                ChannelHeaderSection(
                                    playlistName = currentPlaylistName,
                                    searchQuery = searchQuery,
                                    onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                                    categories = categories,
                                    selectedCategory = selectedCategory,
                                    onCategorySelect = { viewModel.selectCategory(it) }
                                )
                                
                                ChannelsListGrid(
                                    channels = filteredChannels,
                                    playingChannel = playingChannel,
                                    onChannelSelect = { viewModel.playChannel(it) },
                                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                                    columnsCount = if (isLandscape) 2 else 3
                                )
                            }
                            
                            // Panneau latéral pour le lecteur vidéo
                            Column(
                                modifier = Modifier
                                    .weight(1.2f)
                                    .fillMaxHeight()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                if (playingChannel != null) {
                                    Text(
                                        text = "Lecture en cours",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp),
                                        textAlign = TextAlign.Start
                                    )
                                    IptvPlayer(
                                        streamUrl = playingChannel!!.streamUrl,
                                        channelName = playingChannel!!.name,
                                        onClose = { viewModel.playChannel(null) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    ChannelDetailCard(channel = playingChannel!!)
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.size(80.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Sélectionnez une chaîne pour démarrer la lecture",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 24.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        // Layout vertical standard pour mobile portrait
                        Column(modifier = Modifier.fillMaxSize()) {
                            if (playingChannel != null) {
                                IptvPlayer(
                                    streamUrl = playingChannel!!.streamUrl,
                                    channelName = playingChannel!!.name,
                                    onClose = { viewModel.playChannel(null) }
                                )
                                ChannelDetailCard(
                                    channel = playingChannel!!,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            
                            ChannelHeaderSection(
                                playlistName = currentPlaylistName,
                                searchQuery = searchQuery,
                                onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                                categories = categories,
                                selectedCategory = selectedCategory,
                                onCategorySelect = { viewModel.selectCategory(it) }
                            )
                            
                            ChannelsListGrid(
                                channels = filteredChannels,
                                playingChannel = playingChannel,
                                onChannelSelect = { viewModel.playChannel(it) },
                                onToggleFavorite = { viewModel.toggleFavorite(it) },
                                columnsCount = 2
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialogue d'importation de listes de lecture
    if (showImportDialog) {
        ImportPlaylistDialog(
            isImporting = isImporting,
            importError = importError,
            importSuccess = importSuccess,
            onImportUrl = { name, url -> viewModel.importPlaylistUrl(name, url) },
            onImportText = { name, text -> viewModel.importPlaylistText(name, text) },
            onDismiss = {
                showImportDialog = false
                viewModel.resetImportStatus()
            }
        )
    }

    // Gestionnaire de listes de lecture (suppression / aperçu)
    if (showPlaylistsManager) {
        PlaylistsManagerDialog(
            playlists = playlists,
            onDeletePlaylist = { viewModel.deletePlaylist(it) },
            onDismiss = { showPlaylistsManager = false }
        )
    }
}

@Composable
fun EmptyStateScreen(
    onImportClick: () -> Unit,
    onLoadDemoClick: () -> Unit,
    isImporting: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_tv),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(96.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Bienvenue dans votre Lecteur IPTV",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "Ajoutez une liste de lecture M3U ou M3U8 pour commencer à regarder vos chaînes de télévision en direct.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(36.dp))
        
        if (isImporting) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(8.dp))
            Text("Importation des chaînes en cours...", style = MaterialTheme.typography.bodyMedium)
        } else {
            Button(
                onClick = onImportClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("empty_state_import_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Importer une Playlist", fontWeight = FontWeight.SemiBold)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedButton(
                onClick = onLoadDemoClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Charger la démo gratuite & légale", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun PlaylistSelectionScreen(
    playlists: List<Playlist>,
    onPlaylistSelect: (Playlist) -> Unit,
    onDeletePlaylist: (Playlist) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Mes Listes de Lecture",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 280.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(playlists) { playlist ->
                PlaylistCard(
                    playlist = playlist,
                    onSelect = { onPlaylistSelect(playlist) },
                    onDelete = { onDeletePlaylist(playlist) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaylistCard(
    playlist: Playlist,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardDefaults.shape)
            .combinedClickable(
                onClick = onSelect
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Text(
                        text = playlist.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${playlist.channelCount} chaînes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Supprimer la playlist",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun ChannelHeaderSection(
    playlistName: String,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    categories: List<String>,
    selectedCategory: String,
    onCategorySelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = playlistName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Barre de recherche
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("channel_search_bar"),
            placeholder = { Text("Rechercher une chaîne...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Effacer")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Search
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Liste horizontale de catégories
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { category ->
                val isSelected = category == selectedCategory
                FilterChip(
                    selected = isSelected,
                    onClick = { onCategorySelect(category) },
                    label = { Text(category) },
                    leadingIcon = if (category == "Favoris") {
                        { Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
    }
}

@Composable
fun ChannelsListGrid(
    channels: List<Channel>,
    playingChannel: Channel?,
    onChannelSelect: (Channel) -> Unit,
    onToggleFavorite: (Channel) -> Unit,
    columnsCount: Int,
    modifier: Modifier = Modifier
) {
    if (channels.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Aucune chaîne trouvée",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Essayez un autre mot clé ou filtre de catégorie.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columnsCount),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = modifier.fillMaxSize()
        ) {
            items(channels, key = { it.id }) { channel ->
                ChannelCard(
                    channel = channel,
                    isPlaying = playingChannel?.id == channel.id,
                    onSelect = { onChannelSelect(channel) },
                    onToggleFavorite = { onToggleFavorite(channel) }
                )
            }
        }
    }
}

@Composable
fun ChannelCard(
    channel: Channel,
    isPlaying: Boolean,
    onSelect: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentProgram = remember(channel.id) {
        EpgManager.getCurrentProgram(channel.name, channel.groupTitle)
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isPlaying) 4.dp else 1.dp
        ),
        border = if (isPlaying) {
            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Logo de la chaîne
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(channel.logoUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Logo ${channel.name}",
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.8f))
                        .padding(4.dp),
                    contentScale = ContentScale.Fit,
                    error = null // On ignore les erreurs pour afficher le fallback ci-dessous
                )
                
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (channel.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favori",
                        tint = if (channel.isFavorite) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = channel.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isPlaying) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = currentProgram.title,
                style = MaterialTheme.typography.bodySmall,
                color = if (isPlaying) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f) else MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            LinearProgressIndicator(
                progress = currentProgram.progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(1.5.dp)),
                color = if (isPlaying) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                trackColor = (if (isPlaying) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary).copy(alpha = 0.2f)
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = channel.groupTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isPlaying) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                Text(
                    text = currentProgram.formattedTimeRange.substringBefore(" -"),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isPlaying) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun ChannelDetailCard(channel: Channel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val currentProgram = remember(channel.id) {
        EpgManager.getCurrentProgram(channel.name, channel.groupTitle)
    }
    val upcomingPrograms = remember(channel.id) {
        EpgManager.getUpcomingPrograms(channel.name, channel.groupTitle, count = 3)
    }
    
    var isExpanded by remember { mutableStateOf(false) }
    var showFullGrid by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Ligne d'en-tête de la chaîne
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(Color.White, shape = RoundedCornerShape(8.dp))
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = channel.logoUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = channel.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Catégorie : ${channel.groupTitle}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
            )
            
            // Section Programme en cours
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "EN DIRECT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = currentProgram.category,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                
                Text(
                    text = currentProgram.formattedTimeRange,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = currentProgram.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = currentProgram.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (isExpanded) 10 else 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = currentProgram.progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Bouton d'extension et bouton de grille complète
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { isExpanded = !isExpanded },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isExpanded) "Masquer les détails" else "Voir la suite & détails",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                OutlinedButton(
                    onClick = { showFullGrid = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Grille 24h", style = MaterialTheme.typography.labelMedium)
                }
            }
            
            // Section extensible pour les programmes à venir
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
                    )
                    
                    Text(
                        text = "À venir ensuite :",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    if (upcomingPrograms.isEmpty()) {
                        Text(
                            text = "Aucun programme supplémentaire de disponible.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            upcomingPrograms.forEach { prog ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = prog.formattedTimeRange.substringBefore(" -"),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.width(64.dp)
                                    )
                                    
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = prog.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
                                                        shape = RoundedCornerShape(4.dp)
                                                    )
                                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    text = prog.category,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = prog.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    if (showFullGrid) {
        FullDayEpgDialog(
            channelName = channel.name,
            groupTitle = channel.groupTitle,
            onDismiss = { showFullGrid = false }
        )
    }
}

@Composable
fun FullDayEpgDialog(
    channelName: String,
    groupTitle: String,
    onDismiss: () -> Unit
) {
    val programs = remember(channelName) {
        EpgManager.generateEpgForChannel(channelName, groupTitle)
    }
    val now = System.currentTimeMillis()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Grille des programmes - $channelName",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 450.dp)) {
                Text(
                    text = "Aujourd'hui",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    items(programs) { prog ->
                        val isCurrent = now >= prog.startTime && now < prog.endTime
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrent) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                }
                            ),
                            border = if (isCurrent) {
                                BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                            } else null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = prog.formattedTimeRange,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                                    )
                                    
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = (if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.secondaryContainer).copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = prog.category,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                Text(
                                    text = prog.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                
                                Spacer(modifier = Modifier.height(2.dp))
                                
                                Text(
                                    text = prog.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = (if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.8f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                
                                if (isCurrent) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = prog.progress,
                                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportPlaylistDialog(
    isImporting: Boolean,
    importError: String?,
    importSuccess: Boolean,
    onImportUrl: (String, String) -> Unit,
    onImportText: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var playlistName by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = URL, 1 = Fichier, 2 = Copier-coller
    var m3uUrl by remember { mutableStateOf("") }
    var m3uContent by remember { mutableStateOf("") }
    var fileSelectedName by remember { mutableStateOf<String?>(null) }
    
    val context = LocalContext.current
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val contentResolver = context.contentResolver
                var nameFromUri = "Ma playlist importée"
                
                val cursor = contentResolver.query(it, null, null, null, null)
                cursor?.use { c ->
                    if (c.moveToFirst()) {
                        val nameIndex = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            nameFromUri = c.getString(nameIndex).substringBeforeLast(".")
                        }
                    }
                }
                
                if (playlistName.isEmpty()) {
                    playlistName = nameFromUri
                }
                
                val inputStream = contentResolver.openInputStream(it)
                val content = inputStream?.bufferedReader()?.use { reader -> reader.readText() } ?: ""
                m3uContent = content
                fileSelectedName = nameFromUri
            } catch (e: Exception) {
                // Erreur lors du chargement du fichier
            }
        }
    }
    
    AlertDialog(
        onDismissRequest = { if (!isImporting) onDismiss() },
        confirmButton = {
            if (importSuccess) {
                Button(onClick = onDismiss) {
                    Text("OK")
                }
            } else if (!isImporting) {
                Button(
                    onClick = {
                        val finalName = if (playlistName.trim().isEmpty()) "Ma Liste" else playlistName
                        if (selectedTab == 0) {
                            onImportUrl(finalName, m3uUrl)
                        } else {
                            onImportText(finalName, m3uContent)
                        }
                    },
                    enabled = playlistName.isNotEmpty() && (
                        (selectedTab == 0 && m3uUrl.isNotEmpty()) || 
                        (selectedTab == 1 && m3uContent.isNotEmpty()) || 
                        (selectedTab == 2 && m3uContent.isNotEmpty())
                    ),
                    modifier = Modifier.testTag("dialog_confirm_import")
                ) {
                    Text("Importer")
                }
            }
        },
        dismissButton = {
            if (!isImporting && !importSuccess) {
                TextButton(onClick = onDismiss) {
                    Text("Annuler")
                }
            }
        },
        title = {
            Text(
                text = if (importSuccess) "Importation réussie" else "Importer une playlist",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            if (isImporting) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Lecture et analyse de la playlist M3U...")
                }
            } else if (importSuccess) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier
                            .size(64.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "La playlist a été configurée avec succès ! Les chaînes ont été ajoutées à votre base de données locale.",
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    if (importError != null) {
                        Text(
                            text = importError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
                                .padding(8.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    
                    OutlinedTextField(
                        value = playlistName,
                        onValueChange = { playlistName = it },
                        label = { Text("Nom de la playlist") },
                        placeholder = { Text("Ex: Chaînes Françaises") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_playlist_name")
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    TabRow(selectedTabIndex = selectedTab) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Lien URL") }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Fichier Local") }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("Copier-coller") }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (selectedTab == 0) {
                        OutlinedTextField(
                            value = m3uUrl,
                            onValueChange = { m3uUrl = it },
                            label = { Text("URL du fichier .m3u ou .m3u8") },
                            placeholder = { Text("https://example.com/playlist.m3u") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Done
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_playlist_url")
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Suggestion de démo légale
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Besoin d'un test ?",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Cliquez ci-dessous pour charger une liste de flux d'actualité publics et entièrement légaux.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(
                                    onClick = {
                                        playlistName = "Démo Info Publique"
                                        m3uContent = getDemoM3u()
                                        selectedTab = 2
                                    },
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text("Charger l'exemple de démonstration")
                                }
                            }
                        }
                    } else if (selectedTab == 1) {
                        Card(
                            onClick = { filePickerLauncher.launch("*/*") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_folder),
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (fileSelectedName != null) "Fichier : $fileSelectedName" else "Sélectionner un fichier .m3u ou .m3u8",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                if (m3uContent.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "${m3uContent.lines().size} lignes chargées",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = m3uContent,
                            onValueChange = { m3uContent = it },
                            label = { Text("Contenu texte M3U brut") },
                            placeholder = { Text("#EXTM3U\n#EXTINF:-1,Nom\nhttp://...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .testTag("input_playlist_raw_text"),
                            maxLines = 80
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun PlaylistsManagerDialog(
    playlists: List<Playlist>,
    onDeletePlaylist: (Playlist) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Fermer")
            }
        },
        title = {
            Text("Gérer mes playlists", fontWeight = FontWeight.Bold)
        },
        text = {
            if (playlists.isEmpty()) {
                Text("Aucune playlist importée.")
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    playlists.forEach { playlist ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(playlist.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${playlist.channelCount} chaînes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { onDeletePlaylist(playlist) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    )
}

// Fonction utilitaire pour obtenir une playlist de test publique de haute qualité
fun getDemoM3u(): String {
    return """
        #EXTM3U
        #EXTINF:-1 tvg-id="France24" tvg-logo="https://upload.wikimedia.org/wikipedia/commons/e/e3/FRANCE_24_Logo.svg" group-title="Actualités",France 24 Info
        https://static.france24.com/live/F24_FR_LO_HLS/live_tv.m3u8
        #EXTINF:-1 tvg-id="Euronews" tvg-logo="https://upload.wikimedia.org/wikipedia/commons/3/3b/Euronews_logo_2016.svg" group-title="Actualités",Euronews Français
        https://euronews-euronews-french-1-fr.samsung.wurl.com/manifest/playlist.m3u8
        #EXTINF:-1 tvg-id="NASA" tvg-logo="https://upload.wikimedia.org/wikipedia/commons/e/e5/NASA_logo.svg" group-title="Science & Espace",NASA TV Public
        https://ntvpublic.akamaized.net/hls/live/2014088/NTV-Public/master.m3u8
        #EXTINF:-1 tvg-id="CGTNDoc" tvg-logo="https://upload.wikimedia.org/wikipedia/commons/6/64/CGTN_Documentary_Logo.svg" group-title="Documentaires",CGTN Doc
        https://cgtn-doc.live.cgtn.com/1000/index.m3u8
    """.trimIndent()
}
