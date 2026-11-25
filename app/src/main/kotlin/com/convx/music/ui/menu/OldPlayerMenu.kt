/**
 * Convx Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.convx.music.ui.menu

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.convx.music.LocalDatabase
import com.convx.music.LocalListenTogetherManager
import com.convx.music.LocalPlayerConnection
import com.convx.music.R
import com.convx.music.core.AudioDeviceMenu
import com.convx.music.models.MediaMetadata
import com.convx.music.ui.component.BottomSheetState
import com.convx.music.ui.component.IosMenuGroup
import com.convx.music.ui.component.IosMenuItemData
import com.convx.music.ui.component.NewAction
import com.convx.music.ui.component.NewActionGrid
import com.convx.music.ui.component.SymbolIcon
import com.convx.music.ui.component.VolumeSlider
import com.convx.music.ui.component.menuBottomInset
import com.convx.music.ui.component.sf
import com.convx.music.ui.ios.compat.AppTheme
import com.convx.music.ui.ios.compat.HorizontalDivider
import com.convx.music.ui.ios.compat.Text
import com.convx.music.ui.player.SleepTimerDialogHost
import com.convx.music.ui.player.rememberSleepTimerState
import com.convx.music.ui.theme.rememberGlobalAccentColors
import com.music.innertube.YouTube
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun OldPlayerMenu(
    mediaMetadata: MediaMetadata?,
    navController: NavController,
    playerBottomSheetState: BottomSheetState,
    onShowDetailsDialog: () -> Unit,
    onDismiss: () -> Unit,
) {
    mediaMetadata ?: return
    val context = LocalContext.current
    val database = LocalDatabase.current.get()
    val playerConnection = LocalPlayerConnection.current ?: return
    val coroutineScope = rememberCoroutineScope()
    val playerVolume = playerConnection.service.playerVolume.collectAsStateWithLifecycle()

    val sleepTimer = rememberSleepTimerState(playerConnection)
    SleepTimerDialogHost(playerConnection, sleepTimer)
    var showAudioDeviceBottomSheet by remember { mutableStateOf(false) }

    val castHandler = remember(playerConnection) {
        try {
            playerConnection.service.castConnectionHandler
        } catch (e: Exception) {
            null
        }
    }
    val isCasting by castHandler?.isCasting?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(false) }
    val castVolume by castHandler?.castVolume?.collectAsStateWithLifecycle() ?: remember { mutableFloatStateOf(1f) }
    val castDeviceName by castHandler?.castDeviceName?.collectAsStateWithLifecycle() ?: remember { mutableStateOf<String?>(null) }

    val listenTogetherManager = LocalListenTogetherManager.current
    val canControlTogether = listenTogetherManager?.canControl?.collectAsState(initial = true)
    val isListenTogetherGuest = canControlTogether?.value == false

    var showChoosePlaylistDialog by rememberSaveable { mutableStateOf(false) }

    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        onGetSong = { playlist ->
            database.transaction { insert(mediaMetadata) }
            coroutineScope.launch(Dispatchers.IO) {
                playlist.playlist.browseId?.let { YouTube.addToPlaylist(it, mediaMetadata.id) }
            }
            onDismiss()
            listOf(mediaMetadata.id)
        },
        onDismiss = { showChoosePlaylistDialog = false }
    )

    if (showAudioDeviceBottomSheet) {
        AudioDeviceMenu(onDismiss = { showAudioDeviceBottomSheet = false })
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 24.dp, bottom = 6.dp),
    ) {
        if (isCasting && castDeviceName != null) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                SymbolIcon(symbol = sf("airplayvideo"), contentDescription = null, size = 24.dp, tint = AppTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.casting_to, castDeviceName ?: ""),
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colorScheme.primary
                )
            }
        }

        VolumeSlider(
            value = if (isCasting) castVolume else playerVolume.value,
            onValueChange = { volume ->
                if (isCasting) {
                    castHandler?.setVolume(volume)
                } else {
                    playerConnection.service.playerVolume.value = volume
                }
            },
            modifier = Modifier.fillMaxWidth(),
            accentColor = rememberGlobalAccentColors().first
        )
    }

    Spacer(modifier = Modifier.height(20.dp))
    HorizontalDivider()
    Spacer(modifier = Modifier.height(12.dp))

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
            val actions = buildList {
                if (!isListenTogetherGuest) {
                    add(
                        NewAction(
                            icon = {
                                SymbolIcon(symbol = sf("dot.radiowaves.left.and.right"), contentDescription = null, size = 32.dp, tint = AppTheme.colorScheme.onSurfaceVariant)
                            },
                            text = stringResource(R.string.start_an_radio),
                            onClick = {
                                Toast.makeText(context, startingRadioText, Toast.LENGTH_SHORT).show()
                                playerConnection.startRadioSeamlessly()
                                onDismiss()
                            }
                        )
                    )
                }
                add(
                    NewAction(
                        icon = {
                            SymbolIcon(symbol = sf("text.badge.plus"), contentDescription = null, size = 32.dp, tint = AppTheme.colorScheme.onSurfaceVariant)
                        },
                        text = stringResource(R.string.add_to_an_playlist),
                        onClick = { showChoosePlaylistDialog = true }
                    )
                )
            }
            NewActionGrid(
                actions = actions,
                columns = actions.size.coerceAtLeast(1),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
            )
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        item {
            IosMenuGroup(
                items = buildList {
                    add(
                        IosMenuItemData(
                            title = { Text(text = stringResource(R.string.sleep_timer)) },
                            description = sleepTimer.menuLabel?.let { label -> { Text(text = label) } },
                            icon = {
                                SymbolIcon(symbol = sf("moon.zzz.fill"), contentDescription = null, size = 24.dp)
                            },
                            onClick = {
                                sleepTimer.showDialog.value = true
                            }
                        )
                    )

                    add(
                        IosMenuItemData(
                            title = { Text(text = stringResource(R.string.audio_devices)) },
                            icon = {
                                SymbolIcon(symbol = sf("airplayaudio"), contentDescription = null, size = 24.dp)
                            },
                            onClick = {
                                showAudioDeviceBottomSheet = true
                            }
                        )
                    )

                    add(
                        IosMenuItemData(
                            title = { Text(text = stringResource(R.string.details)) },
                            description = { Text(text = stringResource(R.string.details_desc)) },
                            icon = {
                                SymbolIcon(symbol = sf("info.circle"), contentDescription = null, size = 24.dp)
                            },
                            onClick = {
                                onShowDetailsDialog()
                                onDismiss()
                            }
                        )
                    )

                    add(
                        IosMenuItemData(
                            title = { Text(text = stringResource(R.string.equalizer)) },
                            description = { Text(text = stringResource(R.string.equalizer_desc)) },
                            icon = {
                                SymbolIcon(symbol = sf("slider.vertical.3"), contentDescription = null, size = 24.dp)
                            },
                            onClick = {
                                navController.navigate("settings/equalizer")
                                onDismiss()
                            }
                        )
                    )
                }
            )
        }
    }
}
