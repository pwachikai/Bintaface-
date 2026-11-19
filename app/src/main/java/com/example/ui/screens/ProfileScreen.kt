package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DEFAULT_ADMIN_ACCOUNT_NUMBER
import com.example.data.model.DEFAULT_ADMIN_BANK_NAME
import com.example.data.model.DEFAULT_APP_DOWNLOAD_URL
import com.example.data.model.DEFAULT_USD_TO_NGN_RATE
import com.example.data.model.PublicProfile
import com.example.data.model.RADIO_ACCESS_FEE_NGN
import com.example.data.model.formatDualCurrency
import com.example.data.model.formatNgnWhole
import com.example.data.model.levelTierForId
import com.example.data.model.nextLevelTierForRank
import com.example.ui.components.AppDownloadShareCard
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.IntentBadge
import com.example.ui.components.PlusBadge
import com.example.ui.components.ProfileAvatar
import com.example.ui.components.ProfileDetailBottomSheet
import com.example.ui.components.ProfileEditorForm
import com.example.ui.components.SpotlightBadge
import com.example.ui.components.VipLevelBadge
import com.example.ui.components.vipLevelAccentColor
import com.example.ui.theme.AppDarkMode
import com.example.ui.theme.AppThemePreset
import com.example.ui.theme.LocalBintafaceAccents
import com.example.ui.theme.MatchEmerald
import com.example.ui.theme.RadioAccentColor
import com.example.ui.theme.ThemeController
import com.example.ui.theme.previewSwatchesForPreset
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    defaultName: String,
    userEmail: String?,
    profileState: UiState<PublicProfile?>,
    postsCount: Int,
    matchesCount: Int,
    isSaving: Boolean,
    isPlusMember: Boolean,
    currentVipLevel: String = "none",
    currentVipRank: Int = 0,
    hasRadioAccess: Boolean = false,
    superlikesBalance: Int,
    boostsBalance: Int,
    sparksBalance: Int,
    creatorAvailableCents: Long,
    usdToNgnRate: Long = DEFAULT_USD_TO_NGN_RATE,
    appDownloadUrl: String = DEFAULT_APP_DOWNLOAD_URL,
    isAdmin: Boolean,
    adminAvailableCents: Long,
    adminAccountSummary: String?,
    pendingAdminPayoutsCount: Int,
    onSaveProfile: (
        displayName: String,
        age: Int,
        city: String,
        bio: String,
        lookingFor: String,
        promptQuestion: String,
        promptAnswer: String,
        interests: List<String>,
        avatarKey: String,
        photoBase64: String?,
        isDiscoverable: Boolean,
        onSaved: () -> Unit
    ) -> Unit,
    onActivateBoost: () -> Unit,
    onOpenLevelUpgrade: () -> Unit = {},
    onOpenRadioAccess: () -> Unit = {},
    onOpenRadioHub: () -> Unit = {},
    onOpenPaywall: () -> Unit,
    onOpenWallet: () -> Unit,
    onOpenAdminConsole: () -> Unit,
    onShowMessage: (String) -> Unit = {},
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isEditing by remember { mutableStateOf(false) }
    var showPreviewSheet by remember { mutableStateOf(false) }
    val accents = LocalBintafaceAccents.current

    BackHandler(enabled = isEditing) {
        isEditing = false
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your Profile",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(
                    onClick = onSignOut,
                    modifier = Modifier.testTag("sign_out_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Sign Out")
                }
            }

            when (profileState) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is UiState.Error -> {
                    EmptyStateCard(
                        icon = Icons.Filled.Edit,
                        title = "Couldn't load profile",
                        subtitle = profileState.message
                    )
                }
                is UiState.Success -> {
                    val profile = profileState.data
                    if (profile == null || isEditing) {
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (profile == null) "Set Up Your BINTAFACE Card" else "Edit Profile",
                                        style = MaterialTheme.typography.headlineMedium
                                    )
                                    if (profile != null) {
                                        TextButton(onClick = { isEditing = false }) {
                                            Text("Cancel")
                                        }
                                    }
                                }
                                ProfileEditorForm(
                                    initialProfile = profile,
                                    defaultName = defaultName,
                                    isSaving = isSaving,
                                    ctaLabel = if (profile == null) "Publish My Card" else "Save Changes",
                                    onSave = { name, age, city, bio, lookingFor, q, a, interests, avatarKey, photo, discoverable ->
                                        onSaveProfile(name, age, city, bio, lookingFor, q, a, interests, avatarKey, photo, discoverable) {
                                            isEditing = false
                                        }
                                    }
                                )
                            }
                        }
                    } else {
                        // Profile Summary Hero Card
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    ProfileAvatar(
                                        name = profile.displayName,
                                        avatarKey = profile.avatarKey,
                                        photoBase64 = profile.photoBase64,
                                        size = 76.dp
                                    )
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "${profile.displayName}, ${profile.age}",
                                                style = MaterialTheme.typography.headlineMedium
                                            )
                                            VipLevelBadge(
                                                vipLevel = currentVipLevel,
                                                showActivePill = true
                                            )
                                            if (isPlusMember) {
                                                PlusBadge()
                                            }
                                            if (profile.isBoosted) {
                                                SpotlightBadge()
                                            }
                                        }
                                        if (profile.city.isNotBlank()) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.LocationOn,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Text(
                                                    text = profile.city,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        IntentBadge(lookingFor = profile.lookingFor)
                                    }
                                }

                                if (!userEmail.isNullOrBlank()) {
                                    Text(
                                        text = "Private account: $userEmail",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (profile.bio.isNotBlank()) {
                                    Text(
                                        text = profile.bio,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }

                                if (profile.interests.isNotEmpty()) {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        profile.interests.forEach { interest ->
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = MaterialTheme.colorScheme.secondaryContainer
                                            ) {
                                                Text(
                                                    text = interest,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { showPreviewSheet = true },
                                        modifier = Modifier
                                            .weight(1f)
                                            .minimumInteractiveComponentSize()
                                    ) {
                                        Icon(Icons.Filled.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Preview Card")
                                    }
                                    OutlinedButton(
                                        onClick = { isEditing = true },
                                        modifier = Modifier
                                            .weight(1f)
                                            .minimumInteractiveComponentSize()
                                            .testTag("edit_profile_button")
                                    ) {
                                        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Edit")
                                    }
                                }
                            }
                        }

                        // Stats Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatTile(
                                value = postsCount.toString(),
                                label = "My Posts",
                                modifier = Modifier.weight(1f)
                            )
                            StatTile(
                                value = matchesCount.toString(),
                                label = "Matches",
                                modifier = Modifier.weight(1f)
                            )
                            StatTile(
                                value = if (profile.isDiscoverable) "Visible" else "Paused",
                                label = "Discovery",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Colourful Theme & Appearance Selector
                        ThemeAppearanceCard()

                        // VIP Level Upgrade Card (Level 1 Water Diamond -> Level 10 Professional General Diamond, NGN 5,000 each step)
                        val activeTier = levelTierForId(currentVipLevel)
                        val nextTier = nextLevelTierForRank(currentVipRank)
                        val levelAccent = vipLevelAccentColor(activeTier?.id ?: "water")
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = levelAccent.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, levelAccent.copy(alpha = 0.4f))
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Diamond,
                                        contentDescription = null,
                                        tint = levelAccent
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = if (activeTier != null) {
                                                    "${activeTier.emoji} Level ${activeTier.rank}/10: ${activeTier.name}"
                                                } else {
                                                    "10-Step Level Upgrade (₦5,000/step)"
                                                },
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            VipLevelBadge(
                                                vipLevel = currentVipLevel,
                                                showActivePill = true
                                            )
                                        }
                                        Text(
                                            text = if (nextTier != null) {
                                                "Next step: ${nextTier.emoji} Level ${nextTier.rank} ${nextTier.name} (${formatNgnWhole(nextTier.ngnAmount)} / USD) • Google Pay & FairMoney 9038006221"
                                            } else {
                                                "💎 Level 10 Professional General Diamond ACTIVE! Highest tier on BINTAFACE."
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Button(
                                    onClick = onOpenLevelUpgrade,
                                    colors = ButtonDefaults.buttonColors(containerColor = levelAccent),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("profile_open_level_upgrade_button")
                                ) {
                                    Text(
                                        if (activeTier == null) {
                                            "Start Level 1 Water Diamond (₦5,000)"
                                        } else if (nextTier != null) {
                                            "Upgrade to L${nextTier.rank} ${nextTier.name} (${formatNgnWhole(nextTier.ngnAmount)})"
                                        } else {
                                            "View Level 10 Receipts"
                                        }
                                    )
                                }
                            }
                        }

                        // BINTAFACE Global Radio Access Pass Card (NGN 2,000 -> FairMoney 9038006221)
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = RadioAccentColor.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, RadioAccentColor.copy(alpha = 0.4f))
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Radio,
                                        contentDescription = null,
                                        tint = RadioAccentColor
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = if (hasRadioAccess) {
                                                    "Global Radio Broadcaster Pass"
                                                } else {
                                                    "Global Radio Access (${formatNgnWhole(RADIO_ACCESS_FEE_NGN)})"
                                                },
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            if (hasRadioAccess) {
                                                Surface(
                                                    shape = RoundedCornerShape(50),
                                                    color = MatchEmerald
                                                ) {
                                                    Text(
                                                        text = "ACTIVE",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = if (hasRadioAccess) {
                                                "Your Radio Pass is ACTIVE. Create global channels for billions of listeners & broadcast live voice!"
                                            } else {
                                                "Pay ${formatNgnWhole(RADIO_ACCESS_FEE_NGN)} to Francis Gregory • $DEFAULT_ADMIN_BANK_NAME $DEFAULT_ADMIN_ACCOUNT_NUMBER (Google confirmed) to create & listen to global stations."
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    if (hasRadioAccess) {
                                        Button(
                                            onClick = onOpenRadioHub,
                                            colors = ButtonDefaults.buttonColors(containerColor = RadioAccentColor),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp)
                                                .testTag("profile_open_radio_hub_button")
                                        ) {
                                            Text("Open Radio Studio")
                                        }
                                        OutlinedButton(
                                            onClick = onOpenRadioAccess,
                                            modifier = Modifier
                                                .height(48.dp)
                                                .testTag("profile_view_radio_receipt_button")
                                        ) {
                                            Text("Receipt")
                                        }
                                    } else {
                                        Button(
                                            onClick = onOpenRadioAccess,
                                            colors = ButtonDefaults.buttonColors(containerColor = RadioAccentColor),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp)
                                                .testTag("profile_open_radio_pass_button")
                                        ) {
                                            Text("Unlock Global Radio (${formatNgnWhole(RADIO_ACCESS_FEE_NGN)})")
                                        }
                                    }
                                }
                            }
                        }

                        // Kindred+ Membership & Boosts Card
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            border = BorderStroke(
                                1.dp,
                                if (isPlusMember) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                                else MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.WorkspacePremium,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Column {
                                            Text(
                                                text = if (isPlusMember) "Bintaface+ Membership Active" else "BINTAFACE Free Plan",
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Text(
                                                text = "$superlikesBalance Superlikes • $boostsBalance Spotlight Boosts",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    if (boostsBalance > 0 && !profile.isBoosted) {
                                        OutlinedButton(
                                            onClick = onActivateBoost,
                                            enabled = !isSaving,
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("activate_boost_button")
                                        ) {
                                            Icon(Icons.Filled.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Use Boost")
                                        }
                                    }
                                    Button(
                                        onClick = onOpenPaywall,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("profile_open_paywall_button")
                                    ) {
                                        Text(if (isPlusMember) "Top Up Boosts" else "Upgrade to Bintaface+")
                                    }
                                }
                            }
                        }

                        // Creator Earnings & Admin Payouts Row
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Creator Wallet & Payouts",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = "Available: ${formatDualCurrency(creatorAvailableCents, usdToNgnRate)} • $sparksBalance Sparks",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = onOpenWallet,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("open_creator_wallet_button")
                                    ) {
                                        Text("Wallet & Payouts")
                                    }
                                    if (isAdmin) {
                                        Button(
                                            onClick = onOpenAdminConsole,
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("open_admin_console_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.AdminPanelSettings,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(Modifier.width(5.dp))
                                            val label = if (pendingAdminPayoutsCount > 0) {
                                                "Admin ($pendingAdminPayoutsCount)"
                                            } else {
                                                "Admin Revenue"
                                            }
                                            Text(label)
                                        }
                                    }
                                }
                            }
                        }

                        if (isAdmin) {
                            Surface(
                                shape = MaterialTheme.shapes.large,
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.AdminPanelSettings,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "BINTAFACE App Admin Revenue",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "Available: ${formatDualCurrency(adminAvailableCents, usdToNgnRate)} • ${adminAccountSummary ?: "Add your USD or Naira account"}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = onOpenAdminConsole,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            if (adminAccountSummary == null) {
                                                "Add Receiving Account Number"
                                            } else {
                                                "Manage Admin Bank & Withdraw"
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        AppDownloadShareCard(
                            downloadUrl = appDownloadUrl,
                            userAccessHandleOrId = profile.handle.ifBlank { profile.userId },
                            onCopiedMessage = onShowMessage
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    val currentProfile = (profileState as? UiState.Success)?.data
    if (showPreviewSheet && currentProfile != null) {
        ProfileDetailBottomSheet(
            profile = currentProfile,
            onDismiss = { showPreviewSheet = false }
        )
    }
}

@Composable
private fun StatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ThemeAppearanceCard() {
    val context = LocalContext.current
    val settings by ThemeController.settings.collectAsStateWithLifecycle()
    val systemDark = isSystemInDarkTheme()

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("theme_appearance_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Theme & Appearance",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${settings.preset.displayName} • ${
                            when (settings.darkMode) {
                                AppDarkMode.SYSTEM -> if (systemDark) "Auto (Dark)" else "Auto (Light)"
                                AppDarkMode.LIGHT -> "Light Mode"
                                AppDarkMode.DARK -> "Dark Mode"
                            }
                        }",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                AppDarkMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = settings.darkMode == mode,
                        onClick = { ThemeController.setDarkMode(context, mode) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = AppDarkMode.entries.size
                        ),
                        modifier = Modifier.testTag("theme_mode_${mode.id}")
                    ) {
                        Text(mode.label, maxLines = 1)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppThemePreset.entries.chunked(2).forEach { rowPresets ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowPresets.forEach { preset ->
                            val isSelected = settings.preset == preset
                            val accentSwatch = previewSwatchesForPreset(preset).first()
                            val borderColor by animateColorAsState(
                                targetValue = if (isSelected) accentSwatch else MaterialTheme.colorScheme.outlineVariant,
                                label = "presetBorder"
                            )
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = if (isSelected) accentSwatch.copy(alpha = 0.08f)
                                else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor),
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .clip(MaterialTheme.shapes.medium)
                                    .clickable { ThemeController.setPreset(context, preset) }
                                    .testTag("theme_preset_${preset.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(accentSwatch)
                                            .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = preset.displayName,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = preset.subtitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = "Active theme",
                                            tint = accentSwatch,
                                            modifier = Modifier.size(18.dp)
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
}
