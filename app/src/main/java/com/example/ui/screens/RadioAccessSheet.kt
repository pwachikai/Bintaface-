package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Radio
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
import com.example.data.model.PlatformSettings
import com.example.data.model.RADIO_ACCESS_FEE_NGN
import com.example.data.model.RADIO_ACCESS_USD_CENTS
import com.example.data.model.RadioPass
import com.example.data.model.formatNgnWhole
import com.example.data.model.formatUsdCents
import com.example.ui.components.formatRelativeTimestamp
import com.example.ui.theme.MatchEmerald
import com.example.ui.theme.RadioAccentColor
import com.example.ui.theme.VipMoonDiamondColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadioAccessSheet(
    userEmail: String? = null,
    hasRadioAccess: Boolean,
    usdToNgnRate: Long = DEFAULT_USD_TO_NGN_RATE,
    platformSettings: PlatformSettings?,
    myRadioPasses: List<RadioPass>,
    isSaving: Boolean,
    isPlayStoreReady: Boolean = false,
    isRadioPlaySkuReady: Boolean = false,
    onLaunchGooglePlayRadio: () -> Boolean = { false },
    onConfirmPurchase: (
        currency: String,
        paymentMethod: String,
        paymentReference: String,
        googleConfirmed: Boolean
    ) -> Unit,
    onOpenCreateStation: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val clipboardManager = LocalClipboardManager.current

    var selectedCurrency by remember { mutableStateOf("NGN") }
    var selectedMethod by remember { mutableStateOf("ngn_bank") }
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

    val safeRate = usdToNgnRate.coerceIn(100L, 10000L)
    val ngnLabel = formatNgnWhole(RADIO_ACCESS_FEE_NGN)
    val usdLabel = remember(selectedCurrency, safeRate) {
        if (selectedCurrency == "USD") {
            formatUsdCents(RADIO_ACCESS_USD_CENTS)
        } else {
            formatUsdCents(((RADIO_ACCESS_FEE_NGN * 100L) / safeRate).coerceAtLeast(100L))
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
            // Gradient Hero Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(RadioAccentColor, VipMoonDiamondColor)
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
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.2f)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Radio,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "BINTAFACE Global Radio",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$ngnLabel one-time pass • Reach billions of listeners",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.92f)
                                )
                            }
                        }

                        if (hasRadioAccess) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MatchEmerald
                            ) {
                                Text(
                                    text = "📻 GLOBAL RADIO ACCESS ACTIVE",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close radio pass sheet",
                            tint = Color.White
                        )
                    }
                }
            }

            // What Global Radio Unlocks
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "What Your $ngnLabel Radio Pass Unlocks",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    RadioBenefitRow(
                        icon = Icons.Filled.Public,
                        text = "Broadcast to billions of BINTAFACE listeners across Talk, Music, News, Knowledge & Faith."
                    )
                    RadioBenefitRow(
                        icon = Icons.Filled.Podcasts,
                        text = "Create your own Global FM Radio Channel with custom frequency, @handle & voice broadcasts."
                    )
                    RadioBenefitRow(
                        icon = Icons.Filled.GraphicEq,
                        text = "Listen to live global stations & earn 80% of listener $1 Spark Gifts."
                    )
                }
            }

            // Admin FairMoney 9038006221 Payment Account Card
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("radio_admin_bank_card")
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
                                text = "Pay Admin ($ngnLabel)",
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
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Account Number: $adminAccountNumber",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.testTag("radio_admin_account_number")
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
                                .testTag("copy_radio_account_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = "Copy FairMoney Account Number",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(if (copiedNotice) "Copied" else "Copy")
                        }
                    }
                }
            }

            if (hasRadioAccess) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MatchEmerald.copy(alpha = 0.12f),
                    border = BorderStroke(1.5.dp, MatchEmerald.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MatchEmerald
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Radio Pass Activated!",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MatchEmerald
                                )
                                Text(
                                    text = "You can now listen to every station and create your own Global Radio Channel.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Button(
                            onClick = {
                                onDismiss()
                                onOpenCreateStation()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RadioAccentColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("radio_unlocked_create_station_button")
                        ) {
                            Icon(Icons.Filled.Podcasts, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Create My Global Radio Channel")
                        }
                    }
                }
            } else {
                // Currency Selector
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Select Payment Currency",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
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
                            label = { Text("₦ Nigerian Naira ($ngnLabel)") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("radio_currency_ngn")
                        )
                        FilterChip(
                            selected = selectedCurrency == "USD",
                            onClick = {
                                selectedCurrency = "USD"
                                if (selectedMethod in setOf("ngn_bank", "card_ngn")) {
                                    selectedMethod = "usd_bank"
                                }
                            },
                            label = { Text("$ USD (${formatUsdCents(RADIO_ACCESS_USD_CENTS)})") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("radio_currency_usd")
                        )
                    }
                }

                // Payment Method
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Payment Method (Google Verified)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedMethod == "ngn_bank" || selectedMethod == "usd_bank",
                            onClick = {
                                selectedMethod = if (selectedCurrency == "NGN") "ngn_bank" else "usd_bank"
                            },
                            label = {
                                Text(
                                    if (selectedCurrency == "NGN") "₦ FairMoney ($adminAccountNumber)"
                                    else "Bank Transfer"
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedMethod == "google_play",
                            onClick = { selectedMethod = "google_play" },
                            label = { Text("Google Pay") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedMethod == "card_ngn" || selectedMethod == "card_usd",
                            onClick = {
                                selectedMethod = if (selectedCurrency == "NGN") "card_ngn" else "card_usd"
                            },
                            label = { Text("Card") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                OutlinedTextField(
                    value = paymentReference,
                    onValueChange = { paymentReference = it },
                    label = {
                        Text(
                            if (selectedMethod == "google_play") "Google Transaction / Order Note (optional)"
                            else "FairMoney / Transfer Receipt Reference (optional)"
                        )
                    },
                    placeholder = { Text("e.g. FM-$adminAccountNumber-2000") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("radio_payment_ref_input")
                )

                val payAmountLabel = if (selectedCurrency == "NGN") "$ngnLabel (~$usdLabel)" else "$usdLabel ($ngnLabel+)"
                Button(
                    onClick = {
                        if (selectedMethod == "google_play" && isPlayStoreReady && isRadioPlaySkuReady) {
                            val launched = onLaunchGooglePlayRadio()
                            if (!launched) {
                                googlePrePaymentChecked = false
                                showGoogleConfirmDialog = true
                            }
                        } else {
                            googlePrePaymentChecked = false
                            showGoogleConfirmDialog = true
                        }
                    },
                    enabled = !isSaving,
                    colors = ButtonDefaults.buttonColors(containerColor = RadioAccentColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("pay_radio_access_button")
                ) {
                    Icon(Icons.Filled.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isSaving) "Activating Global Radio…"
                        else "Pay $payAmountLabel → $adminBankName ($adminAccountNumber)"
                    )
                }
            }

            if (myRadioPasses.isNotEmpty()) {
                HorizontalDivider()
                Text(
                    text = "Your Global Radio Pass Receipts",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                myRadioPasses.forEach { pass ->
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        border = BorderStroke(1.dp, MatchEmerald.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MatchEmerald
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Global Radio Pass • ACTIVE",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${formatNgnWhole(pass.ngnAmount)} (${formatUsdCents(pass.amountCents)}) • ${pass.adminBankLabel.ifBlank { "$adminBankName $adminAccountNumber" }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Ref: ${pass.paymentReference} • ${formatRelativeTimestamp(pass.createdAt)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showGoogleConfirmDialog) {
        val confirmAmount = if (selectedCurrency == "NGN") ngnLabel else formatUsdCents(RADIO_ACCESS_USD_CENTS)
        AlertDialog(
            onDismissRequest = { if (!isSaving) showGoogleConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.VerifiedUser,
                    contentDescription = null,
                    tint = MatchEmerald,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text("Google Payment Confirmation")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Google must confirm your Global Radio payment before your account unlocks Global Radio and Channel Creation.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                text = "BINTAFACE Global Radio Access Pass",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Amount: $confirmAmount",
                                style = MaterialTheme.typography.bodySmall,
                                color = RadioAccentColor,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Admin Bank: $adminBankName • $adminAccountNumber",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Account Name: $adminAccountHolder",
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (!userEmail.isNullOrBlank()) {
                                Text(
                                    text = "Google Account: $userEmail",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = googlePrePaymentChecked,
                            onCheckedChange = { googlePrePaymentChecked = it },
                            modifier = Modifier.testTag("radio_google_confirm_checkbox")
                        )
                        Text(
                            text = "I confirm $confirmAmount payment to $adminAccountHolder ($adminBankName $adminAccountNumber) to activate Global Radio.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showGoogleConfirmDialog = false
                        onConfirmPurchase(
                            selectedCurrency,
                            selectedMethod,
                            paymentReference,
                            true
                        )
                    },
                    enabled = googlePrePaymentChecked && !isSaving,
                    colors = ButtonDefaults.buttonColors(containerColor = MatchEmerald),
                    modifier = Modifier.testTag("confirm_radio_google_button")
                ) {
                    Text("Confirm & Unlock Radio")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showGoogleConfirmDialog = false },
                    enabled = !isSaving
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun RadioBenefitRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = RadioAccentColor,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
