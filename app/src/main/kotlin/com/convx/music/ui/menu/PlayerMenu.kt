/**
 * Convx Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.convx.music.ui.menu

import com.convx.music.ui.component.DownloadRingIcon
import com.convx.music.ui.component.sf
import com.convx.music.ui.component.SymbolIcon
import com.convx.music.ui.component.menuBottomInset
import android.content.Context
import android.content.res.Configuration
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.composables.sfsymbols.SfSymbols
import com.composables.sfsymbols.monochrome.SFGaugeWithNeedle
import com.composables.sfsymbols.monochrome.SFSliderHorizontal3
import com.composables.sfsymbols.monochrome.SFBellFill
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.convx.music.ui.component.shapes.ContinuousCapsule
import com.convx.music.ui.component.shapes.ContinuousRoundedRectangle
import com.convx.music.ui.ios.compat.AlertDialog
import com.convx.music.ui.ios.compat.Button
import androidx.compose.material3.ButtonDefaults
import com.convx.music.ui.ios.compat.CircularProgressIndicator
import com.convx.music.ui.ios.compat.FilledTonalButton
import com.convx.music.ui.ios.compat.HorizontalDivider
import com.convx.music.ui.ios.compat.Icon
import com.convx.music.ui.ios.compat.IconButton
import com.convx.music.ui.ios.compat.LinearProgressIndicator
import com.convx.music.ui.ios.compat.AppTheme
import com.convx.music.ui.ios.compat.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import com.convx.music.ui.ios.compat.Surface
import com.convx.music.ui.ios.compat.Text
import com.convx.music.ui.ios.compat.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import com.music.innertube.YouTube
import com.convx.music.LocalDatabase
import com.convx.music.LocalDownloadUtil
import com.convx.music.LocalListenTogetherManager
import com.convx.music.LocalPlayerConnection
import com.convx.music.R
import com.convx.music.constants.ListItemHeight
import com.convx.music.listentogether.ConnectionState
import com.convx.music.listentogether.ListenTogetherEvent
import com.convx.music.listentogether.MAX_ROOM_CODE_LENGTH
import com.convx.music.listentogether.MIN_ROOM_CODE_LENGTH
import com.convx.music.models.MediaMetadata
import com.convx.music.playback.ExoDownloadService
import com.convx.music.core.AudioDeviceMenu
import com.convx.music.ui.component.BottomSheetState
import com.convx.music.ui.component.ListDialog
import com.convx.music.ui.component.IosMenuGroup
import com.convx.music.ui.component.IosMenuItemData
import com.convx.music.ui.component.NewAction
import com.convx.music.ui.component.NewActionGrid
import com.convx.music.constants.EnableSaavnStreamingKey
import com.convx.music.utils.rememberPreference
import com.convx.music.ui.player.menuLabel
import com.convx.music.ui.player.rememberSleepTimerState
import com.convx.music.ui.player.SleepTimerDialogHost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.round
import androidx.lifecycle.compose.collectAsStateWithLifecycle

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
    val ringtoneViewModel = com.convx.music.LocalRingtoneViewModel.current
    val database = LocalDatabase.current.get()
    val playerConnection = LocalPlayerConnection.current ?: return
    val sleepTimer = rememberSleepTimerState(playerConnection)
    SleepTimerDialogHost(playerConnection, sleepTimer)
    var showAudioDevices by remember { mutableStateOf(false) }
    if (showAudioDevices) AudioDeviceMenu(onDismiss = { showAudioDevices = false })
    val librarySong by database.song(mediaMetadata.id).collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()
    val (saavnEnabled) = rememberPreference(EnableSaavnStreamingKey, defaultValue = false)

    val download by LocalDownloadUtil.current.get().getDownload(mediaMetadata.id)
        .collectAsState(initial = null)

    val artists =
        remember(mediaMetadata.artists) {
            mediaMetadata.artists.filter { it.id != null }
        }

    var showChoosePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }
    
    var showListenTogetherDialog by rememberSaveable {
        mutableStateOf(false)
    }

    val listenTogetherManager = LocalListenTogetherManager.current
    val listenTogetherRoleState = listenTogetherManager?.role?.collectAsState(initial = com.convx.music.listentogether.RoomRole.NONE)
    // See Player.kt: gated on control mode, not on role.
    val canControlTogether = listenTogetherManager?.canControl?.collectAsState(initial = true)
    val isListenTogetherGuest = canControlTogether?.value == false
    val pendingSuggestions by listenTogetherManager?.pendingSuggestions?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) }

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

    ListenTogetherDialog(
        visible = showListenTogetherDialog,
        mediaMetadata = mediaMetadata,
        onDismiss = { showListenTogetherDialog = false }
    )

    var showSelectArtistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showSelectArtistDialog) {
        ListDialog(
            onDismiss = { showSelectArtistDialog = false },
        ) {
            items(artists, key = { it.id!! }) { artist ->
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier =
                    Modifier
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

    var showPitchTempoDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showPitchTempoDialog) {
        TempoPitchDialog(
            onDismiss = { showPitchTempoDialog = false },
        )
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
            NewActionGrid(
                actions = listOfNotNull(
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
                                    com.convx.music.utils.ShareUtils.songUrl(mediaMetadata.id)
                                )
                            }
                            context.startActivity(android.content.Intent.createChooser(intent, null))
                            onDismiss()
                        }
                    )
                ),
                columns = if (isListenTogetherGuest) 2 else 3,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 16.dp)
            )
        }

        item {
            IosMenuGroup(
                items = buildList {
                    // Always visible. Re-triggers the full resolution chain.
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
                    add(
                        IosMenuItemData(
                            title = { Text(text = stringResource(R.string.sleep_timer)) },
                            description = sleepTimer.menuLabel?.let { label -> { Text(text = label) } },
                            icon = { SymbolIcon(symbol = sf("moon.zzz.fill"), contentDescription = null, size = 24.dp) },
                            onClick = {
                                onDismiss()
                                sleepTimer.showDialog.value = true
                            },
                        )
                    )
                    add(
                        IosMenuItemData(
                            title = { Text(text = stringResource(R.string.audio_devices)) },
                            icon = { SymbolIcon(symbol = sf("airplayaudio"), contentDescription = null, size = 24.dp) },
                            onClick = {
                                onDismiss()
                                showAudioDevices = true
                            },
                        )
                    )
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

        if (!mediaMetadata.isLocalFile) item {
            IosMenuGroup(
                items = listOf(
                    when (download?.state) {
                        Download.STATE_COMPLETED -> {
                            IosMenuItemData(
                                title = {
                                    Text(
                                        text = stringResource(R.string.remove_download)
                                    )
                                },
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
                                }
                            )
                        }

                        Download.STATE_QUEUED, Download.STATE_DOWNLOADING -> {
                            IosMenuItemData(
                                title = { Text(text = stringResou
