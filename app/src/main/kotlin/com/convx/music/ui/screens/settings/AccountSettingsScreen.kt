/**
 * Convx Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.convx.music.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.composables.sfsymbols.SfSymbols
import com.composables.sfsymbols.monochrome.SFArrowClockwise
import com.composables.sfsymbols.monochrome.SFPlusCircle
import com.composables.sfsymbols.monochrome.SFRectanglePortraitAndArrowRight
import com.convx.music.R
import com.convx.music.constants.InnerTubeCookieKey
import com.convx.music.constants.UseLoginForBrowse
import com.convx.music.constants.YtmSyncKey
import com.convx.music.ui.component.DefaultDialog
import com.convx.music.ui.component.IosSettingsGroup
import com.convx.music.ui.component.IosSettingsItem
import com.convx.music.ui.component.SettingsPage
import com.convx.music.ui.ios.compat.AppTheme
import com.convx.music.ui.ios.compat.Switch
import com.convx.music.ui.ios.compat.Text
import com.convx.music.ui.ios.foundation.IosTheme
import com.convx.music.utils.rememberPreference
import com.convx.music.viewmodels.AccountSettingsViewModel
import com.convx.music.viewmodels.HomeViewModel
import com.music.innertube.YouTube
import com.music.innertube.utils.parseCookieString
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AccountSettingsScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior
) {
    val context = LocalContext.current

    val (innerTubeCookie, onInnerTubeCookieChange) = rememberPreference(InnerTubeCookieKey, "")
    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in parseCookieString(innerTubeCookie)
    }

    val (useLoginForBrowse, onUseLoginForBrowseChange) = rememberPreference(UseLoginForBrowse, true)
    val (ytmSync, onYtmSyncChange) = rememberPreference(YtmSyncKey, true)

    val homeViewModel: HomeViewModel = koinViewModel()
    val accountSettingsViewModel: AccountSettingsViewModel = koinViewModel()
    val accountName by homeViewModel.accountName.collectAsStateWithLifecycle()
    val accountImageUrl by homeViewModel.accountImageUrl.collectAsStateWithLifecycle()

    var showLogoutDialog by remember { mutableStateOf(false) }

    SettingsPage(
        navController = navController,
        title = stringResource(R.string.account),
        pageColor = AppTheme.colorScheme.background,
        horizontalPadding = 16.dp,
    ) {
        // Account Profile Section
        IosSettingsGroup(
            title = "YouTube Music",
            items = listOf(
                IosSettingsItem(
                    icon = if (isLoggedIn && !accountImageUrl.isNullOrBlank()) {
                        null
                    } else {
                        rememberVectorPainter(SfSymbols.Monochrome.SFRectanglePortraitAndArrowRight)
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isLoggedIn && !accountImageUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = accountImageUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                            Text(
                                text = if (isLoggedIn) accountName else stringResource(R.string.login),
                                color = if (isLoggedIn) IosTheme.colors.label else IosTheme.tint,
                                style = IosTheme.typography.body.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    },
                    trailingContent = if (isLoggedIn) {
                        {
                            Text(
                                text = stringResource(R.string.action_logout),
                                color = IosTheme.colors.systemRed,
                                style = IosTheme.typography.subheadline.copy(fontWeight = FontWeight.SemiBold),
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(IosTheme.colors.systemRed.copy(alpha = 0.14f))
                                    .clickable { showLogoutDialog = true }
                                    .padding(horizontal = 14.dp, vertical = 7.dp),
                            )
                        }
                    } else {
                        null
                    },
                    onClick = {
                        if (isLoggedIn) navController.navigate("account")
                        else navController.navigate("login")
                    }
                )
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Sync & Content Section
        if (isLoggedIn) {
            IosSettingsGroup(
                title = stringResource(R.string.settings_section_player_content),
                items = listOf(
                    IosSettingsItem(
                        icon = rememberVectorPainter(SfSymbols.Monochrome.SFPlusCircle),
                        title = { Text(stringResource(R.string.more_content)) },
                        trailingContent = {
                            Switch(
                                checked = useLoginForBrowse,
                                onCheckedChange = {
                                    YouTube.useLoginForBrowse = it
                                    onUseLoginForBrowseChange(it)
                                }
                            )
                        },
                        onClick = {
                            val newValue = !useLoginForBrowse
                            YouTube.useLoginForBrowse = newValue
                            onUseLoginForBrowseChange(newValue)
                        }
                    ),
                    IosSettingsItem(
                        icon = rememberVectorPainter(SfSymbols.Monochrome.SFArrowClockwise),
                        title = { Text(stringResource(R.string.yt_sync)) },
                        trailingContent = {
                            Switch(
                                checked = ytmSync,
                                onCheckedChange = onYtmSyncChange
                            )
                        },
                        onClick = { onYtmSyncChange(!ytmSync) }
                    )
                )
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (showLogoutDialog) {
            DefaultDialog(
                onDismiss = { showLogoutDialog = false },
                title = { Text(stringResource(R.string.logout_dialog_title)) },
                buttons = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        ToggleButton(
                            checked = false,
                            onCheckedChange = { showLogoutDialog = false },
                            modifier = Modifier.weight(1f),
                            shapes = ButtonGroupDefaults.connectedLeadingButtonShapes()
                        ) {
                            Text(stringResource(android.R.string.cancel))
                        }

                        ToggleButton(
                            checked = false,
                            onCheckedChange = {
                                accountSettingsViewModel.logoutAndClearSyncedContent(context, onInnerTubeCookieChange)
                                showLogoutDialog = false
                            },
                            modifier = Modifier.weight(1f),
                            shapes = ButtonGroupDefaults.connectedMiddleButtonShapes(),
                            colors = ToggleButtonDefaults.toggleButtonColors(
                                contentColor = AppTheme.colorScheme.error
                            )
                        ) {
                            Text(stringResource(R.string.logout_clear_data))
                        }

                        ToggleButton(
                            checked = true,
                            onCheckedChange = {
                                accountSettingsViewModel.logoutKeepData(context, onInnerTubeCookieChange)
                                showLogoutDialog = false
                                navController.navigateUp()
                            },
                            modifier = Modifier.weight(1f),
                            shapes = ButtonGroupDefaults.connectedTrailingButtonShapes()
                        ) {
                            Text(stringResource(R.string.logout_keep_data))
                        }
                    }
                }
            ) {
                Text(
                    text = stringResource(R.string.logout_dialog_message),
                    style = AppTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
    }
}
