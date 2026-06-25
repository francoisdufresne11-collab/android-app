package com.example.ui

import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.*
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

@OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun IptvPlayer(
    streamUrl: String,
    channelName: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }
    
    LaunchedEffect(streamUrl) {
        val mediaItem = MediaItem.Builder()
            .setUri(streamUrl)
            .apply {
                if (streamUrl.contains(".m3u8", ignoreCase = true)) {
                    setMimeType(MimeTypes.APPLICATION_M3U8)
                }
            }
            .build()
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
    }
    
    var isRecording by remember { mutableStateOf(false) }
    var recordJob by remember { mutableStateOf<Job?>(null) }
    var bytesRecorded by remember { mutableLongStateOf(0L) }
    var recordingError by remember { mutableStateOf<String?>(null) }
    
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("video/mp4")
    ) { uri ->
        uri?.let { fileUri ->
            isRecording = true
            bytesRecorded = 0L
            recordingError = null
            Toast.makeText(context, "Enregistrement démarré. Appuyez sur Stop pour sauvegarder.", Toast.LENGTH_SHORT).show()
            
            recordJob = coroutineScope.launch {
                recordStream(
                    context = context,
                    streamUrl = streamUrl,
                    outputUri = fileUri,
                    onProgress = { bytes ->
                        bytesRecorded = bytes
                    },
                    onError = { err ->
                        recordingError = err
                        isRecording = false
                        coroutineScope.launch(Dispatchers.Main) {
                            Toast.makeText(context, "Erreur d'enregistrement : $err", Toast.LENGTH_LONG).show()
                        }
                    }
                )
            }
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
            recordJob?.cancel()
        }
    }
    
    var isFullscreen by remember { mutableStateOf(false) }

    if (isFullscreen) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { isFullscreen = false },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = false
            )
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black
            ) {
                PlayerContent(
                    exoPlayer = exoPlayer,
                    channelName = channelName,
                    isFullscreen = true,
                    isRecording = isRecording,
                    bytesRecorded = bytesRecorded,
                    onStartRecording = {
                        val sanitizedName = channelName.replace(Regex("[^a-zA-Z0-9]"), "_")
                        createDocumentLauncher.launch("${sanitizedName}_enregistrement")
                    },
                    onStopRecording = {
                        recordJob?.cancel()
                        isRecording = false
                        Toast.makeText(context, "Enregistrement terminé et sauvegardé !", Toast.LENGTH_LONG).show()
                    },
                    onToggleFullscreen = { isFullscreen = false },
                    onClose = {
                        isFullscreen = false
                        onClose()
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f),
        colors = CardDefaults.cardColors(containerColor = Color.Black),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        if (!isFullscreen) {
            PlayerContent(
                exoPlayer = exoPlayer,
                channelName = channelName,
                isFullscreen = false,
                isRecording = isRecording,
                bytesRecorded = bytesRecorded,
                onStartRecording = {
                    val sanitizedName = channelName.replace(Regex("[^a-zA-Z0-9]"), "_")
                    createDocumentLauncher.launch("${sanitizedName}_enregistrement")
                },
                onStopRecording = {
                    recordJob?.cancel()
                    isRecording = false
                    Toast.makeText(context, "Enregistrement terminé et sauvegardé !", Toast.LENGTH_LONG).show()
                },
                onToggleFullscreen = { isFullscreen = true },
                onClose = onClose,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Lecture en plein écran",
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun PlayerContent(
    exoPlayer: ExoPlayer,
    channelName: String,
    isFullscreen: Boolean,
    isRecording: Boolean,
    bytesRecorded: Long,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = channelName,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isRecording) {
                    Text(
                        text = "REC: ${formatSize(bytesRecorded)}",
                        color = Color.Red,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
                
                IconButton(
                    onClick = {
                        if (isRecording) {
                            onStopRecording()
                        } else {
                            onStartRecording()
                        }
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(
                            id = if (isRecording) com.example.R.drawable.ic_stop else com.example.R.drawable.ic_record
                        ),
                        contentDescription = if (isRecording) "Arrêter l'enregistrement" else "Enregistrer",
                        tint = if (isRecording) Color.Red else Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(
                    onClick = onToggleFullscreen,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(
                            id = if (isFullscreen) com.example.R.drawable.ic_fullscreen_exit else com.example.R.drawable.ic_fullscreen
                        ),
                        contentDescription = if (isFullscreen) "Quitter le plein écran" else "Plein écran",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fermer le lecteur",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kib = bytes / 1024.0
    if (kib < 1024) return String.format("%.2f KB", kib)
    val mib = kib / 1024.0
    if (mib < 1024) return String.format("%.2f MB", mib)
    val gib = mib / 1024.0
    return String.format("%.2f GB", gib)
}

private suspend fun recordStream(
    context: android.content.Context,
    streamUrl: String,
    outputUri: android.net.Uri,
    onProgress: (Long) -> Unit,
    onError: (String) -> Unit
) {
    withContext(Dispatchers.IO) {
        var outputStream: OutputStream? = null
        try {
            outputStream = context.contentResolver.openOutputStream(outputUri)
            if (outputStream == null) {
                onError("Impossible d'accéder au dossier de destination")
                return@withContext
            }
            
            var totalBytes = 0L
            
            if (streamUrl.contains(".m3u8", ignoreCase = true)) {
                val downloadedSegments = mutableSetOf<String>()
                var currentPlaylistUrl = streamUrl
                
                try {
                    val initialContent = fetchTextUrl(currentPlaylistUrl)
                    if (initialContent.contains("#EXT-X-STREAM-INF")) {
                        val resolved = parseFirstMediaPlaylistUrl(currentPlaylistUrl, initialContent)
                        if (resolved != null) {
                            currentPlaylistUrl = resolved
                        }
                    }
                } catch (e: Exception) {
                    // Fail gracefully and attempt to parse raw
                }
                
                while (coroutineContext.isActive) {
                    try {
                        val playlistContent = fetchTextUrl(currentPlaylistUrl)
                        val segments = parseSegments(currentPlaylistUrl, playlistContent)
                        
                        for (segmentUrl in segments) {
                            if (!coroutineContext.isActive) break
                            if (!downloadedSegments.contains(segmentUrl)) {
                                try {
                                    val bytes = downloadSegment(segmentUrl)
                                    if (bytes.isNotEmpty()) {
                                        outputStream.write(bytes)
                                        outputStream.flush()
                                        totalBytes += bytes.size
                                        withContext(Dispatchers.Main) {
                                            onProgress(totalBytes)
                                        }
                                    }
                                    downloadedSegments.add(segmentUrl)
                                } catch (e: Exception) {
                                    // Continuer malgré l'erreur sur un segment particulier
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // Ignorer les erreurs temporaires de playlist
                    }
                    
                    delay(3000)
                }
            } else {
                val connection = URL(streamUrl).openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.inputStream.use { inputStream ->
                    val buffer = ByteArray(16384)
                    var bytesRead: Int
                    while (coroutineContext.isActive) {
                        bytesRead = inputStream.read(buffer)
                        if (bytesRead == -1) break
                        if (bytesRead > 0) {
                            outputStream.write(buffer, 0, bytesRead)
                            outputStream.flush()
                            totalBytes += bytesRead
                            withContext(Dispatchers.Main) {
                                onProgress(totalBytes)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            onError(e.localizedMessage ?: "Erreur d'enregistrement")
        } finally {
            try {
                outputStream?.close()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}

private fun fetchTextUrl(urlStr: String): String {
    val connection = URL(urlStr).openConnection() as HttpURLConnection
    connection.connectTimeout = 8000
    connection.readTimeout = 8000
    return connection.inputStream.bufferedReader().use { it.readText() }
}

private fun parseFirstMediaPlaylistUrl(masterUrl: String, content: String): String? {
    val lines = content.lines().map { it.trim() }
    for (i in lines.indices) {
        if (lines[i].startsWith("#EXT-X-STREAM-INF")) {
            for (j in i + 1 until lines.size) {
                val line = lines[j]
                if (line.isNotEmpty() && !line.startsWith("#")) {
                    return resolveAbsoluteUrl(masterUrl, line)
                }
            }
        }
    }
    return null
}

private fun resolveAbsoluteUrl(baseUrl: String, relativePath: String): String {
    return try {
        val base = URI(baseUrl)
        base.resolve(relativePath).toString()
    } catch (e: Exception) {
        if (relativePath.startsWith("http://") || relativePath.startsWith("https://")) {
            relativePath
        } else {
            val baseWithoutQuery = baseUrl.substringBefore("?")
            val lastSlash = baseWithoutQuery.lastIndexOf('/')
            if (lastSlash != -1) {
                baseWithoutQuery.substring(0, lastSlash + 1) + relativePath
            } else {
                relativePath
            }
        }
    }
}

private fun parseSegments(playlistUrl: String, content: String): List<String> {
    val segments = mutableListOf<String>()
    val lines = content.lines().map { it.trim() }
    for (i in lines.indices) {
        val line = lines[i]
        if (line.startsWith("#EXTINF")) {
            for (j in i + 1 until lines.size) {
                val nextLine = lines[j]
                if (nextLine.isNotEmpty()) {
                    if (!nextLine.startsWith("#")) {
                        segments.add(resolveAbsoluteUrl(playlistUrl, nextLine))
                    }
                    break
                }
            }
        }
    }
    return segments
}

private fun downloadSegment(urlStr: String): ByteArray {
    val connection = URL(urlStr).openConnection() as HttpURLConnection
    connection.connectTimeout = 8000
    connection.readTimeout = 8000
    return connection.inputStream.use { it.readBytes() }
}
