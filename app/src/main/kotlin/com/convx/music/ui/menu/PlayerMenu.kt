/**
 * Convx Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.convx.music.ui.menu

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.convx.music.LocalDatabase
import com.convx.music.LocalListenTogetherManager
import com.convx.music.LocalPlayerConnection
import com.convx.music.R
import com.convx.music.models.MediaMetadata
import com.convx.music.ui.component.BottomSheetState
import com.convx.music.ui.component.NewAction
import com.convx.music.ui.component.NewActionGrid
import com.convx.music.ui.component.SymbolIcon
import com.convx.music.ui.component.menuBottomInset
import com.convx.music.ui.component.sf
import com.convx.music.ui.ios.compat.HorizontalDivider
import com.convx.music.utils.ShareUtils
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
    val coroutineScope = rememberCoroutineScope()

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
    }
}
