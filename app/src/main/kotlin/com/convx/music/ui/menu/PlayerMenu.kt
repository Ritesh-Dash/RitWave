/**
 * Convx Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.convx.music.ui.menu

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import com.convx.music.LocalDatabase
import com.convx.music.LocalDownloadUtil
import com.convx.music.LocalListenTogetherManager
import com.convx.music.LocalPlayerConnection
import com.convx.music.R
import com.convx.music.constants.EnableSaavnStreamingKey
import com.convx.music.constants.ListItemHeight
import com.convx.music.models.MediaMetadata
import com.convx.music.playback.ExoDownloadService
import com.convx.music.ui.component.BottomSheetState
import com.convx.music.ui.component.DownloadRingIcon
import com.convx.music.ui.component.IosMenuGroup
import com.convx.music.ui.component.IosMenuItemData
import com.convx.music.ui.component.ListDialog
import com.convx.music.ui.component.NewAction
import com.convx.music.ui.component.NewActionGrid
import com.convx.music.ui.component.SymbolIcon
import com.convx.music.ui.component.menuBottomInset
import com.convx.music.ui.component.sf
import com.convx.music.ui.ios.compat.HorizontalDivider
import com.convx.music.ui.ios.compat.Text
import com.convx.music.utils.ShareUtils
import com.convx.music.utils.rememberPreference
import com.music.innertube.YouTube
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun PlayerMenu(
    mediaMetadata: MediaMetadata?,
    navController: NavController,
    playerBottomSheetState: BottomSheetState,
    isQueueTrigger: Boolean? = false,
    onShowDetailsDialog: () -> Unit,
    onDismiss: () -> Unit,
) {
    mediaMetadata ?: return
    val context = LocalContext.current
    val database = LocalDatabase.current.get()
    val playerConnection = LocalPlayerConnection.current ?: return
    val librarySong by database.song(mediaMetadata.id).collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()
    val (saavnEnabled) = rememberPreference(EnableSaavnStreamingKey, defaultValue = false)

    val download by LocalDownloadUtil.current.get().getDownload(mediaMetadata.id)
        .collectAsState(initial = null)

    val artists = remember(mediaMetadata.artists) {
        mediaMetadata.artists.filter { it.id != null }
    }

    var showChoosePlaylistDialog by rememberSaveable { mutableStateOf(false) }

    val listenTogetherManager = LocalListenTogetherManager.current
    val canControlTogether = listenTogetherManager?.canControl?.collectAsState(initial = true)
    val isListenTogetherGuest = canControlTogether?.value == false

    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        onGetSong = { playlist ->
            database.transaction {
                insert(mediaMetadata)
            }
            coroutineScope.launch(Dispatchers.IO) {
                playlist.playlist.browseId?.let { YouTube.addToPlaylist(it, mediaMetadata.id) }
            }
            listOf(mediaMetadata.id)
        },
        onDismiss = {
            showChoosePlaylistDialog = false
        }
    )

    var showSelectArtistDialog by rememberSaveable { mutableStateOf(false) }

    if (showSelectArtistDialog) {
        ListDialog(
            onDismiss = { showSelectArtistDialog = false },
        ) {
            items(artists, key = { it.id!! }) { artist ->
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier
                        .fillParentMaxWidth()
                        .height(ListItemHeight)
                        .clickable {
                            navController.navigate("artist/${artist.id}")
                            showSelectArtistDialog = false
                            playerBottomSheetState.collapseSoft()
                            onDismiss()
                        }
                        .padding(horizontal = 24.dp),
                ) {
                    Text(
                        text = artist.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(20.dp))
    HorizontalDivider()
    Spacer(modifier = Modifier.height(12.dp))

    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    LazyColumn(
        contentPadding = PaddingValues(
            start = 0.dp,
            top = 0.dp,
            end = 0.dp,
            bottom = 8.dp + menuBottomInset(),
        ),
    ) {
        item {
            val startingRadioText = stringResource(R.string.starting_radio)
            val actions = listOfNotNull(
                if (!isListenTogetherGuest && !mediaMetadata.isLocalFile) {
                    NewAction(
                        icon = {
                            SymbolIcon(symbol = sf("dot.radiowaves.left.and.right"), contentDescription = null, size = 32.dp)
                        },
                        text = stringResource(R.string.start_an_radio),
                        onClick = {
                            Toast.makeText(context, startingRadioText, Toast.LENGTH_SHORT).show()
                            playerConnection.startRadioSeamlessly()
                            onDismiss()
                        }
                    )
                } else null,
                NewAction(
                    icon = {
                        SymbolIcon(symbol = sf("text.badge.plus"), contentDescription = null, size = 32.dp)
                    },
                    text = stringResource(R.string.add_to_an_playlist),
                    onClick = { showChoosePlaylistDialog = true }
                ),
                if (mediaMetadata.isLocalFile) null else NewAction(
                    icon = {
                        SymbolIcon(symbol = sf("square.and.arrow.up"), contentDescription = null, size = 32.dp)
                    },
                    text = stringResource(R.string.share),
                    onClick = {
                        val intent = android.content.Intent().apply {
                            action = android.content.Intent.ACTION_SEND
                            type = "text/plain"
                            putExtra(
                                android.content.Intent.EXTRA_TEXT,
                                ShareUtils.songUrl(mediaMetadata.id)
                            )
                        }
                        context.startActivity(android.content.Intent.createChooser(intent, null))
                        onDismiss()
                    }
                )
            )
            NewActionGrid(
                actions = actions,
                columns = actions.size.coerceAtLeast(1),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = if (isPortrait) 16.dp else 8.dp)
            )
        }

        item {
            IosMenuGroup(
                items = buildList {
                    add(
                        IosMenuItemData(
                            title = { Text(text = stringResource(R.string.retry_stream)) },
                            description = {
                                Text(
                                    text = if (saavnEnabled)
                                        stringResource(R.string.retry_stream_desc_saavn)
                                    else
                                        stringResource(R.string.retry_stream_desc_yt)
                                )
                            },
                            icon = {
                                SymbolIcon(symbol = sf("arrow.counterclockwise"), contentDescription = null, size = 24.dp)
                            },
                            onClick = {
                                val player = playerConnection.player
                                player.seekTo(0)
                                player.prepare()
                                player.play()
                                onDismiss()
                            }
                        )
                    )

                    if (artists.isNotEmpty()) {
                        add(
                            IosMenuItemData(
                                title = { Text(text = stringResource(R.string.view_artist)) },
                                description = {
                                    Text(
                                        text = mediaMetadata.artists.joinToString { it.name },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                icon = {
                                    SymbolIcon(symbol = sf("music.mic"), contentDescription = null, size = 24.dp)
                                },
                                onClick = {
                                    if (mediaMetadata.artists.size == 1) {
                                        navController.navigate("artist/${mediaMetadata.artists[0].id}")
                                        playerBottomSheetState.collapseSoft()
                                        onDismiss()
                                    } else {
                                        showSelectArtistDialog = true
                                    }
                                }
                            )
                        )
                    }

                    if (mediaMetadata.album != null) {
                        add(
                            IosMenuItemData(
                                title = { Text(text = stringResource(R.string.view_album)) },
                                description = {
                                    Text(
                                        text = mediaMetadata.album.title,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                icon = {
                                    SymbolIcon(symbol = sf("square.stack"), contentDescription = null, size = 24.dp)
                                },
                                onClick = {
                                    navController.navigate("album/${mediaMetadata.album.id}")
                                    playerBottomSheetState.collapseSoft()
                                    onDismiss()
                                }
                            )
                        )
                    }

                    val isInLibrary = librarySong?.song?.inLibrary != null
                    add(
                        IosMenuItemData(
                            title = {
                                Text(
                                    text = stringResource(
                                        if (isInLibrary) R.string.remove_from_library
                                        else R.string.add_to_library
                                    )
                                )
                            },
                            icon = {
                                SymbolIcon(
                                    symbol = if (isInLibrary) sf("text.badge.checkmark") else sf("text.badge.plus"),
                                    contentDescription = null,
                                    size = 24.dp,
                                )
                            },
                            onClick = {
                                playerConnection.toggleLibrary()
                                onDismiss()
                            }
                        )
                    )
                }
            )
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        if (!mediaMetadata.isLocalFile) {
            item {
                IosMenuGroup(
                    items = listOf(
                        when (download?.state) {
                            Download.STATE_COMPLETED -> {
                                IosMenuItemData(
                                    title = { Text(text = stringResource(R.string.remove_download)) },
                                    icon = {
                                        SymbolIcon(symbol = sf("arrow.down.circle"), contentDescription = null, size = 24.dp)
                                    },
                                    onClick = {
                                        DownloadService.sendRemoveDownload(
                                            context,
                                            ExoDownloadService::class.java,
                                            mediaMetadata.id,
                                            false,
                                        )
                                        onDismiss()
                                    }
                                )
                            }

                            Download.STATE_QUEUED, Download.STATE_DOWNLOADING -> {
                                IosMenuItemData(
                                    title = { Text(text = stringResource(R.string.downloading)) },
                                    icon = { DownloadRingIcon(download) },
                                    onClick = {
                                        DownloadService.sendRemoveDownload(
                                            context,
                                            ExoDownloadService::class.java,
                                            mediaMetadata.id,
                                            false,
                                        )
                                        onDismiss()
                                    }
                                )
                            }

                            else -> {
                                IosMenuItemData(
                                    title = { Text(text = stringResource(R.string.action_download)) },
                                    icon = {
                                        SymbolIcon(symbol = sf("arrow.down.circle"), contentDescription = null, size = 24.dp)
                                    },
                                    onClick = {
                                        database.transaction { insert(mediaMetadata) }
                                        val downloadRequest = DownloadRequest.Builder(mediaMetadata.id, mediaMetadata.id.toUri())
                                            .setCustomCacheKey(mediaMetadata.id)
                                            .setData(mediaMetadata.title.toByteArray())
                                            .build()
                                        DownloadService.sendAddDownload(context, ExoDownloadService::class.java, downloadRequest, false)
                                        onDismiss()
                                    }
                                )
                            }
                        }
                    )
                )
            }
        }
    }
}
