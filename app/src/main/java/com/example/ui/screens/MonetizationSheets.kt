package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Button
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AdRevenueEvent
import com.example.data.model.DEFAULT_USD_TO_NGN_RATE
import com.example.data.model.PayoutRequest
import com.example.data.model.SparkGift
import com.example.data.model.formatDualCurrency
import com.example.data.model.formatNgnFromUsdCents
import com.example.data.model.formatUsdCents
import com.example.ui.components.formatRelativeTimestamp
import com.example.ui.theme.MatchEmerald
import com.example.ui.theme.SuperlikeGold

private val CREATOR_PAYOUT_METHODS = listOf(
    "ngn_bank" to "₦ Naira Bank",
    "usd_bank" to "$ USD Bank",
    "crypto" to "Crypto Wallet",
    "paypal" to "PayPal"
)

private val CRYPTO_NETWORKS = listOf("USDT TRC20", "USDT ERC20", "USDC", "BTC", "ETH")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatorWalletSheet(
    sparksBalance: Int,
    sparkEarnedCents: Long,
    adEarnedCents: Long,
    shopEarnedCents: Long = 0L,
    totalEarnedCents: Long,
    paidOutCents: Long,
    pendingCents: Long,
    availableCents: Long,
    usdToNgnRate: Long = DEFAULT_USD_TO_NGN_RATE,
    adImpressionsCount: Int = 0,
    lastAdEventStatus: String? = null,
    savedMethod: String?,
    savedAccount: String?,
    savedBankOrNetwork: String? = null,
    savedCurrency: String? = null,
    autoPayoutEnabled: Boolean = true,
    receivedGifts: List<SparkGift>,
    adRevenueEvents: List<AdRevenueEvent> = emptyList(),
    myPayoutRequests: List<PayoutRequest>,
    isSaving: Boolean,
    onBuySparks: () -> Unit,
    onSavePayoutMethod: (
        method: String,
        account: String,
        bankOrNetwork: String,
        currency: String,
        autoPayoutEnabled: Boolean
    ) -> Unit,
    onRequestPayout: (
        method: String,
        account: String,
        currency: String,
        bankOrNetwork: String
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedMethod by remember(savedMethod) {
        mutableStateOf(
            savedMethod?.takeIf { it in listOf("ngn_bank", "usd_bank", "crypto", "paypal", "bank", "upi") }
                ?: "ngn_bank"
        )
    }
    var selectedCurrency by remember(savedCurrency, savedMethod) {
        mutableStateOf(
            savedCurrency?.takeIf { it in setOf("USD", "NGN", "CRYPTO") }
                ?: when (selectedMethod) {
                    "crypto" -> "CRYPTO"
                    "ngn_bank" -> "NGN"
                    else -> "USD"
                }
        )
    }
    var bankOrNetwork by remember(savedBankOrNetwork) {
        mutableStateOf(savedBankOrNetwork ?: "")
    }
    var payoutAccount by remember(savedAccount) { mutableStateOf(savedAccount ?: "") }
    var autoPayout by remember(autoPayoutEnabled) { mutableStateOf(autoPayoutEnabled) }

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
            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                        imageVector = Icons.Filled.AccountBalanceWallet,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Creator Wallet & Auto-Payout",
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close wallet")
                }
            }

            // 30% Google AdMob + 80% Spark Breakdown Card
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AdsClick,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "You Earn 30% of Google Ads + 80% of Sparks & Shops",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "AdMob 30%: ${formatDualCurrency(adEarnedCents, usdToNgnRate)} • Sparks 80%: ${formatDualCurrency(sparkEarnedCents, usdToNgnRate)} • Shops 80%: ${formatDualCurrency(shopEarnedCents, usdToNgnRate)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Session Ad Impressions: $adImpressionsCount • Rate: $1 = ₦$usdToNgnRate",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!lastAdEventStatus.isNullOrBlank()) {
                        Text(
                            text = lastAdEventStatus,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Spark Coins Balance + Top Up
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = SuperlikeGold
                        )
                        Column {
                            Text(
                                text = "$sparksBalance Spark Coins Ready",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Tip Posts, Reels, Videos, Live & Channels",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    OutlinedButton(onClick = {
                        onDismiss()
                        onBuySparks()
                    }) {
                        Text("Get Sparks")
                    }
                }
            }

            // Earnings Summary Grid (USD + Naira)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "Available\n${formatNgnFromUsdCents(availableCents, usdToNgnRate)}",
                    value = formatUsdCents(availableCents),
                    highlight = true,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Total Earned\n${formatNgnFromUsdCents(totalEarnedCents, usdToNgnRate)}",
                    value = formatUsdCents(totalEarnedCents),
                    highlight = false,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Paid Out\n${formatNgnFromUsdCents(paidOutCents, usdToNgnRate)}",
                    value = formatUsdCents(paidOutCents),
                    highlight = false,
                    modifier = Modifier.weight(1f)
                )
            }

            if (pendingCents > 0L) {
                Text(
                    text = "Auto-Payout Queue Pending Settlement: ${formatDualCurrency(pendingCents, usdToNgnRate)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Payout Destination: Naira Bank, USD Bank, Crypto Wallet, PayPal
            Text(
                text = "Payout Destination (Crypto, USD or Naira)",
                style = MaterialTheme.typography.titleMedium
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CREATOR_PAYOUT_METHODS.forEach { (key, label) ->
                    FilterChip(
                        selected = selectedMethod == key,
                        onClick = {
                            selectedMethod = key
                            selectedCurrency = when (key) {
                                "crypto" -> "CRYPTO"
                                "ngn_bank" -> "NGN"
                                else -> "USD"
                            }
                            if (key == "crypto" && bankOrNetwork.isBlank()) {
                                bankOrNetwork = "USDT TRC20"
                            }
                        },
                        label = { Text(label) }
                    )
                }
            }

            if (selectedMethod == "crypto") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CRYPTO_NETWORKS.forEach { net ->
                        FilterChip(
                            selected = bankOrNetwork.equals(net, ignoreCase = true),
                            onClick = { bankOrNetwork = net },
                            label = { Text(net) }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = bankOrNetwork,
                onValueChange = { if (it.length <= 80) bankOrNetwork = it },
                label = {
                    Text(
                        when (selectedMethod) {
                            "crypto" -> "Crypto Network (e.g. USDT TRC20, BTC, ETH)"
                            "ngn_bank" -> "Nigerian Bank Name (e.g. GTBank, OPay, Access)"
                            "usd_bank" -> "USD Bank Name (e.g. Chase, Wise, Mercury)"
                            else -> "Payment Provider"
                        }
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("payout_bank_or_network_input")
            )

            OutlinedTextField(
                value = payoutAccount,
                onValueChange = { if (it.length <= 120) payoutAccount = it },
                label = {
                    Text(
                        when (selectedMethod) {
                            "crypto" -> "Crypto Wallet Address"
                            "ngn_bank" -> "10-Digit Naira Account Number"
                            "usd_bank" -> "USD Account / Routing / IBAN"
                            "paypal" -> "PayPal Email or @handle"
                            else -> "Account Number or Handle"
                        }
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("payout_account_input")
            )

            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Payout Like YouTube",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Auto-queue payouts at $1.00 (${formatNgnFromUsdCents(100L, usdToNgnRate)}) threshold.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoPayout,
                        onCheckedChange = { autoPayout = it },
                        modifier = Modifier.testTag("creator_auto_payout_switch")
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onSavePayoutMethod(
                            selectedMethod,
                            payoutAccount,
                            bankOrNetwork,
                            selectedCurrency,
                            autoPayout
                        )
                    },
                    enabled = !isSaving && payoutAccount.trim().length >= 3,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("save_payout_method_button")
                ) {
                    Text("Save Method")
                }
                Button(
                    onClick = {
                        onRequestPayout(
                            selectedMethod,
                            payoutAccount,
                            selectedCurrency,
                            bankOrNetwork
                        )
                    },
                    enabled = !isSaving && availableCents >= 100L && payoutAccount.trim().length >= 3,
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("request_payout_button")
                ) {
                    Icon(Icons.Filled.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Payout ${formatUsdCents(availableCents)}")
                }
            }

            Text(
                text = "Note: Google AdMob pays publishers on Google's monthly cycle (~21st). Live automated Naira, USD bank & Crypto wallet disbursements require a server-side payout processor (Paystack/Flutterwave/Wise/Coinbase) connected to settled ad funds.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (myPayoutRequests.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text("Payout History", style = MaterialTheme.typography.titleMedium)
                myPayoutRequests.take(10).forEach { req ->
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
                                val autoTag = if (req.isAutoPayout) " • YT AUTO" else ""
                                Text(
                                    text = "${formatDualCurrency(req.amountCents, usdToNgnRate)} • ${req.payoutCurrency}$autoTag",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "${req.payoutBankOrNetwork.ifBlank { req.payoutMethod.uppercase() }}: ${req.payoutAccount} • ${formatRelativeTimestamp(req.createdAt)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (!req.payoutReference.isNullOrBlank()) {
                                    Text(
                                        text = "Ref: ${req.payoutReference}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MatchEmerald
                                    )
                                }
                            }
                            StatusPill(req.status)
                        }
                    }
                }
            }

            if (adRevenueEvents.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text("Google AdMob 30% Creator Share", style = MaterialTheme.typography.titleMedium)
                adRevenueEvents.take(8).forEach { ev ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "AdMob ${ev.adFormat.uppercase()} • ${ev.postId.take(24)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = formatRelativeTimestamp(ev.createdAt),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "+${formatDualCurrency(ev.creatorShareCents, usdToNgnRate)}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MatchEmerald,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (receivedGifts.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text("Recent Spark Gifts Received (80%)", style = MaterialTheme.typography.titleMedium)
                receivedGifts.take(8).forEach { gift ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${gift.senderName} sent a Spark",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = formatRelativeTimestamp(gift.createdAt),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "+${formatDualCurrency(gift.creatorShareCents, usdToNgnRate)}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MatchEmerald,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun MetricCard(
    title: String,
    value: String,
    highlight: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = if (highlight) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
        else MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(
            1.dp,
            if (highlight) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun StatusPill(status: String) {
    val bg = when (status) {
        "paid" -> MatchEmerald
        "rejected" -> MaterialTheme.colorScheme.error
        else -> SuperlikeGold
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = bg
    ) {
        Text(
            text = status.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
        )
    }
}
