package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.APP_ADMIN_EMAIL
import com.example.data.model.DEFAULT_ADMIN_ACCOUNT_HOLDER
import com.example.data.model.DEFAULT_ADMIN_ACCOUNT_NUMBER
import com.example.data.model.DEFAULT_ADMIN_BANK_NAME
import com.example.data.model.DEFAULT_USD_TO_NGN_RATE
import com.example.data.model.LevelUpgradeOrder
import com.example.data.model.MEMBERSHIP_LEVEL_TIERS
import com.example.data.model.PlatformSettings
import com.example.data.model.formatNgnWhole
import com.example.data.model.formatUsdCents
import com.example.data.model.levelTierForId
import com.example.data.model.nextLevelTierForRank
import com.example.ui.components.VipLevelBadge
import com.example.ui.components.formatRelativeTimestamp
import com.example.ui.components.vipLevelAccentColor
import com.example.ui.theme.MatchEmerald
import com.example.ui.theme.VipProGeneralDiamondColor
import com.example.ui.theme.VipWaterColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelUpgradeSheet(
    userEmail: String? = null,
    currentVipLevel: String,
    currentVipRank: Int,
    usdToNgnRate: Long = DEFAULT_USD_TO_NGN_RATE,
    platformSettings: PlatformSettings?,
    myUpgrades: List<LevelUpgradeOrder>,
    isSaving: Boolean,
    isPlayStoreReady: Boolean = false,
    isLevelPlaySkuReady: Boolean = false,
    onLaunchGooglePlayLevel: (Int) -> Boolean = { false },
    onConfirmUpgrade: (
        levelId: String,
        currency: String,
        paymentMethod: String,
        paymentReference: String,
        googleConfirmed: Boolean
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val clipboardManager = LocalClipboardManager.current
    val nextTier = remember(currentVipRank) { nextLevelTierForRank(currentVipRank) }
    var selectedCurrency by remember { mutableStateOf("NGN") }
    var selectedMethod by remember { mutableStateOf("google_play") }
    var paymentReference by remember { mutableStateOf("") }
    var copiedNotice by remember { mutableStateOf(false) }
    var showGoogleConfirmDialog by remember { mutableStateOf(false) }
    var googlePrePaymentChecked by remember { mutableStateOf(false) }

    val adminBankName = remember(platformSettings?.bankName) {
        platformSettings?.bankName?.trim()?.takeIf { it.isNotBlank() } ?: DEFAULT_ADMIN_BANK_NAME
    }
    val adminAccountNumber = remember(platformSettings?.adminAccountNumber) {
        platformSettings?.adminAccountNumber?.trim()?.takeIf { it.isNotBlank() }
            ?: DEFAULT_ADMIN_ACCOUNT_NUMBER
    }
    val adminAccountHolder = remember(platformSettings?.adminAccountHolder) {
        platformSettings?.adminAccountHolder?.trim()?.takeIf { it.isNotBlank() }
            ?: DEFAULT_ADMIN_ACCOUNT_HOLDER
    }

    val currentTier = remember(currentVipLevel) { levelTierForId(currentVipLevel) }
    val activePayTier = nextTier ?: MEMBERSHIP_LEVEL_TIERS.last()

    val safeRate = usdToNgnRate.coerceIn(100L, 10000L)
    val nextNgnLabel = remember(activePayTier, selectedCurrency, safeRate) {
        if (selectedCurrency == "NGN") {
            formatNgnWhole(activePayTier.ngnAmount)
        } else {
            formatNgnWhole(((activePayTier.usdCents * safeRate) / 100L).coerceAtLeast(5000L))
        }
    }
    val nextUsdLabel = remember(activePayTier, selectedCurrency, safeRate) {
        if (selectedCurrency == "USD") {
            formatUsdCents(activePayTier.usdCents)
        } else {
            formatUsdCents(((activePayTier.ngnAmount * 100L) / safeRate).coerceAtLeast(100L))
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Gradient Hero Banner: Level 1 Water Diamond -> Level 10 Professional General Diamond
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(VipWaterColor, VipProGeneralDiamondColor)
                        )
                    )
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.2f)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Diamond,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "10-Step Level Upgrade",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "₦5,000 each step • No jumping levels",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.92f)
                                )
                            }
                        }

                        LinearProgressIndicator(
                            progress = { (currentVipRank.coerceIn(0, 10) / 10f) },
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.28f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(50))
                        )

                        if (currentTier != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Current: Level ${currentTier.rank}/10",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.94f),
                                    fontWeight = FontWeight.SemiBold
                                )
                                VipLevelBadge(
                                    vipLevel = currentTier.id,
                                    showActivePill = true
                                )
                            }
                        } else {
                            Text(
                                text = "Current: Level 0/10 • Pay via Google & Admin FairMoney ($adminAccountNumber) to activate Level 1 Water Diamond.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.94f)
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close level upgrade",
                            tint = Color.White
                        )
                    }
                }
            }

            // Admin FairMoney Account Card — always visible on users' level screen
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_level_bank_card")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                imageVector = Icons.Filled.AccountBalance,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Admin Payment Account",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MatchEmerald.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = "Google Verified",
                                style = MaterialTheme.typography.labelSmall,
                                color = MatchEmerald,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Bank: $adminBankName",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Account Number: $adminAccountNumber",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.testTag("admin_level_account_number")
                            )
                            Text(
                                text = "Account Name: $adminAccountHolder • Admin: $APP_ADMIN_EMAIL",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(adminAccountNumber))
                                copiedNotice = true
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("copy_admin_account_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = "Copy Admin Account Number",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(if (copiedNotice) "Copied" else "Copy")
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.VerifiedUser,
                            contentDescription = null,
                            tint = MatchEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Google confirms payment before activating your badge as ACTIVE on your profile.",
                            style = MaterialTheme.typography.labelMedium,
                            color = MatchEmerald,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Currency Selector: Naira (NGN) or USD ($)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Pay Direct to App Admin In",
                    style = MaterialTheme.typography.titleMedium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = selectedCurrency == "NGN",
                        onClick = {
                            selectedCurrency = "NGN"
                            if (selectedMethod in setOf("usd_bank", "card_usd")) {
                                selectedMethod = "ngn_bank"
                            }
                        },
                        label = { Text("₦ Nigerian Naira (₦5,000/step)") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("level_currency_ngn")
                    )
                    FilterChip(
                        selected = selectedCurrency == "USD",
                        onClick = {
                            selectedCurrency = "USD"
                            if (selectedMethod in setOf("ngn_bank", "card_ngn")) {
                                selectedMethod = "usd_bank"
                            }
                        },
                        label = { Text("$ US Dollars ($3.50/step)") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("level_currency_usd")
                    )
                }
            }

            // Sequential 10-Level Ladder (Step-by-Step Only, No Jumping)
            Text(
                text = "Sequential Ladder (No Jumping Levels)",
                style = MaterialTheme.typography.titleMedium
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MEMBERSHIP_LEVEL_TIERS.forEach { tier ->
                    val isUnlocked = currentVipRank >= tier.rank
                    val isNextStep = tier.rank == currentVipRank + 1
                    val isLockedFuture = tier.rank > currentVipRank + 1
                    val tierColor = vipLevelAccentColor(tier.id)
                    val primaryPrice = if (selectedCurrency == "NGN") {
                        formatNgnWhole(tier.ngnAmount)
                    } else {
                        formatUsdCents(tier.usdCents)
                    }
                    val secondaryPrice = if (selectedCurrency == "NGN") {
                        formatUsdCents(((tier.ngnAmount * 100L) / safeRate).coerceAtLeast(100L))
                    } else {
                        formatNgnWhole(((tier.usdCents * safeRate) / 100L).coerceAtLeast(5000L))
                    }

                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = when {
                            isUnlocked -> MatchEmerald.copy(alpha = 0.08f)
                            isNextStep -> tierColor.copy(alpha = 0.14f)
                            else -> MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.65f)
                        },
                        border = BorderStroke(
                            width = if (isNextStep) 2.dp else 1.dp,
                            color = when {
                                isUnlocked -> MatchEmerald.copy(alpha = 0.55f)
                                isNextStep -> tierColor
                                else -> MaterialTheme.colorScheme.outlineVariant
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.large)
                            .clickable(enabled = false) { }
                            .testTag("level_tier_${tier.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            when {
                                isUnlocked -> Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "Level ${tier.rank} unlocked",
                                    tint = MatchEmerald,
                                    modifier = Modifier.size(24.dp)
                                )
                                isNextStep -> Surface(
                                    shape = CircleShape,
                                    color = tierColor
                                ) {
                                    Text(
                                        text = "${tier.rank}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                else -> Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = "Level ${tier.rank} locked",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = "${tier.emoji} Level ${tier.rank}: ${tier.name}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        isNextStep -> tierColor
                                        isLockedFuture -> MaterialTheme.colorScheme.onSurfaceVariant
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (currentVipLevel.equals(tier.id, ignoreCase = true)) {
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = MatchEmerald
                                        ) {
                                            Text(
                                                text = "ACTIVE ON PROFILE",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    } else if (isUnlocked) {
                                        Text(
                                            text = "Completed",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MatchEmerald,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    } else if (isNextStep) {
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = tierColor
                                        ) {
                                            Text(
                                                text = "NEXT STEP",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = "Locked • Complete Level ${tier.rank - 1} first",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                                Text(
                                    text = tier.perks,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = primaryPrice,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isLockedFuture) MaterialTheme.colorScheme.onSurfaceVariant else tierColor
                                )
                                Text(
                                    text = "≈ $secondaryPrice",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            if (nextTier != null) {
                // Google Payment System + FairMoney Method Picker
                val methodOptions = if (selectedCurrency == "NGN") {
                    listOf(
                        "google_play" to "Google Pay / Billing",
                        "ngn_bank" to "₦ FairMoney ($adminAccountNumber)",
                        "card_ngn" to "₦ Naira Card"
                    )
                } else {
                    listOf(
                        "google_play" to "Google Pay / Billing",
                        "usd_bank" to "$ USD Bank / Wire",
                        "card_usd" to "$ USD Card"
                    )
                }
                Text(
                    text = "Select Payment System (Google Confirmed)",
                    style = MaterialTheme.typography.titleMedium
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    methodOptions.forEach { (key, label) ->
                        FilterChip(
                            selected = selectedMethod == key,
                            onClick = { selectedMethod = key },
                            label = { Text(label) },
                            modifier = Modifier.testTag("level_method_$key")
                        )
                    }
                }

                if (selectedMethod == "google_play") {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Bolt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (isLevelPlaySkuReady) {
                                    "Google Play Billing ready • Settlement routes to $adminBankName $adminAccountNumber after Google confirms."
                                } else if (isPlayStoreReady) {
                                    "Google Account verified (${userEmail ?: "Signed in"}) • Confirm before FairMoney $adminAccountNumber settlement."
                                } else {
                                    "Google Account confirmation required before FairMoney ($adminAccountNumber) payment activates your badge."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = paymentReference,
                    onValueChange = { if (it.length <= 120) paymentReference = it },
                    label = {
                        Text(
                            if (selectedMethod == "ngn_bank") {
                                "FairMoney Transfer Ref ($adminAccountNumber)"
                            } else {
                                "Google / Bank Payment Reference (Optional)"
                            }
                        )
                    },
                    placeholder = { Text("Enter transfer/receipt ref or leave blank for Google Ref ID") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("level_payment_reference_input")
                )

                val payLabel = if (selectedCurrency == "NGN") nextNgnLabel else nextUsdLabel
                Button(
                    onClick = {
                        googlePrePaymentChecked = false
                        showGoogleConfirmDialog = true
                    },
                    enabled = !isSaving,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = vipLevelAccentColor(nextTier.id)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("confirm_level_upgrade_button")
                ) {
                    Text(
                        text = if (isSaving) {
                            "Waiting for Google Confirmation…"
                        } else {
                            "Review & Confirm Google Payment ($payLabel) • L${nextTier.rank}"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MatchEmerald.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, MatchEmerald.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MatchEmerald)
                        Text(
                            text = "💎 Level 10: Professional General Diamond is ACTIVE on your profile — the highest tier on BINTAFACE!",
                            style = MaterialTheme.typography.titleSmall,
                            color = MatchEmerald,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (myUpgrades.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text("Your Level Upgrade Receipts", style = MaterialTheme.typography.titleMedium)
                myUpgrades.take(12).forEach { receipt ->
                    val amountText = if (receipt.currency == "NGN") {
                        "${formatNgnWhole(receipt.ngnAmount)} (${formatUsdCents(receipt.amountCents)})"
                    } else {
                        "${formatUsdCents(receipt.amountCents)} (${formatNgnWhole(receipt.ngnAmount)})"
                    }
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Level ${receipt.levelRank}: ${receipt.levelName} • $amountText",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${receipt.adminBankLabel.ifBlank { "$adminBankName $adminAccountNumber" }} • Ref: ${receipt.paymentReference} • ${formatRelativeTimestamp(receipt.createdAt)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            VipLevelBadge(
                                vipLevel = receipt.levelId,
                                showActivePill = currentVipLevel.equals(receipt.levelId, ignoreCase = true)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showGoogleConfirmDialog && nextTier != null) {
        val payLabel = if (selectedCurrency == "NGN") nextNgnLabel else nextUsdLabel
        AlertDialog(
            onDismissRequest = { showGoogleConfirmDialog = false },
            modifier = Modifier.testTag("google_payment_confirm_dialog"),
            icon = {
                Icon(
                    imageVector = Icons.Filled.VerifiedUser,
                    contentDescription = null,
                    tint = MatchEmerald
                )
            },
            title = { Text("Google Pre-Payment Confirmation") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Please confirm via your Google account before payment processes and activates your badge as ACTIVE on your profile:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Google Account: ${userEmail ?: "Verified Google User"}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Upgrade Badge: ${nextTier.emoji} Level ${nextTier.rank} — ${nextTier.name}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Amount: $payLabel ($selectedCurrency)",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = "App Admin Bank: $adminBankName • $adminAccountNumber ($adminAccountHolder)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .clickable { googlePrePaymentChecked = !googlePrePaymentChecked }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = googlePrePaymentChecked,
                            onCheckedChange = { googlePrePaymentChecked = it },
                            modifier = Modifier.testTag("google_confirm_checkbox")
                        )
                        Text(
                            text = "Confirm Google payment authorization to $adminBankName $adminAccountNumber before activating Level ${nextTier.rank} badge.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showGoogleConfirmDialog = false
                        val launchedLivePlay = if (selectedMethod == "google_play" && isLevelPlaySkuReady) {
                            onLaunchGooglePlayLevel(nextTier.rank)
                        } else {
                            false
                        }
                        if (!launchedLivePlay) {
                            onConfirmUpgrade(
                                nextTier.id,
                                selectedCurrency,
                                selectedMethod,
                                paymentReference,
                                true
                            )
                        }
                    },
                    enabled = googlePrePaymentChecked && !isSaving,
                    modifier = Modifier.testTag("google_confirm_pay_button")
                ) {
                    Text("Confirm & Pay $payLabel")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoogleConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
