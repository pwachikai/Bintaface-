package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.APP_ADMIN_EMAIL
import com.example.data.model.AdminPayoutAccount
import com.example.data.model.AdminWithdrawal
import com.example.data.model.DEFAULT_USD_TO_NGN_RATE
import com.example.data.model.PayoutRequest
import com.example.data.model.PlatformSettings
import com.example.data.model.RevenueLedgerEntry
import com.example.data.model.formatDualCurrency
import com.example.data.model.formatNgnFromUsdCents
import com.example.data.model.formatUsdCents
import com.example.data.model.maskAccountNumber
import com.example.ui.components.formatRelativeTimestamp
import com.example.ui.theme.MatchEmerald

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminRevenueSheet(
    userEmail: String?,
    platformSettings: PlatformSettings?,
    adminPayoutAccount: AdminPayoutAccount?,
    adminWithdrawals: List<AdminWithdrawal>,
    adminTotalEarnedCents: Long,
    adminWithdrawnCents: Long,
    adminAvailableCents: Long,
    usdToNgnRate: Long = DEFAULT_USD_TO_NGN_RATE,
    adImpressionsCount: Int = 0,
    lastAdEventStatus: String? = null,
    isAdmin: Boolean,
    revenueLedger: List<RevenueLedgerEntry>,
    payoutRequests: List<PayoutRequest>,
    isSaving: Boolean,
    onSaveAdminPayoutAccount: (
        payoutMethod: String,
        accountHolderName: String,
        bankName: String,
        accountNumber: String,
        routingOrSwift: String,
        payoutEmailOrPhone: String,
        platformFeePercent: Long,
        payoutCurrency: String
    ) -> Unit,
    onUpdateAdminConfig: (
        usdToNgnRate: Long,
        autoPayoutEnabled: Boolean,
        appDownloadUrl: String,
        bannerUnitId: String,
        rewardedUnitId: String
    ) -> Unit,
    onWithdrawAdminRevenue: () -> Unit,
    onProcessAutoPayoutBatch: () -> Unit,
    onSettlePayoutRequest: (requestId: String, approved: Boolean, reference: String, note: String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AdminPanelSettings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "BINTAFACE App Admin",
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close admin console")
                }
            }

            if (!isAdmin) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Text("Admin Access Restricted", style = MaterialTheme.typography.titleMedium)
                        }
                        Text(
                            text = "All BINTAFACE revenue, Google AdMob configuration, and creator payout approvals are restricted exclusively to $APP_ADMIN_EMAIL.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.VerifiedUser,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Verified Admin: ${userEmail ?: APP_ADMIN_EMAIL}",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "70% Google Advertising Revenue + 100% Subs & Packs + 20% Spark Fees. Settle in USD ($) or Nigerian Naira (₦).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                val adAdminCents = revenueLedger.filter { it.source == "ad_revenue" }.sumOf { it.adminShareCents }
                val adCreatorCents = revenueLedger.filter { it.source == "ad_revenue" }.sumOf { it.creatorShareCents }
                val sparkCommissionCents = revenueLedger.filter { it.source == "spark_gift" }.sumOf { it.adminShareCents }
                val directAppSalesCents = revenueLedger
                    .filter { it.source != "spark_gift" && it.source != "ad_revenue" }
                    .sumOf { it.adminShareCents }
                val pendingList = payoutRequests.filter { it.status == "pending" }
                val pendingCreatorPayouts = pendingList.sumOf { it.amountCents }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard(
                        title = "Admin Available\n${formatNgnFromUsdCents(adminAvailableCents, usdToNgnRate)}",
                        value = formatUsdCents(adminAvailableCents),
                        highlight = true,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Total Admin Rev\n${formatNgnFromUsdCents(adminTotalEarnedCents, usdToNgnRate)}",
                        value = formatUsdCents(adminTotalEarnedCents),
                        highlight = false,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard(
                        title = "AdMob 70% Admin\n($adImpressionsCount impressions)",
                        value = formatUsdCents(adAdminCents),
                        highlight = false,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "AdMob 30% Users\n${formatNgnFromUsdCents(adCreatorCents, usdToNgnRate)}",
                        value = formatUsdCents(adCreatorCents),
                        highlight = false,
                        modifier = Modifier.weight(1f)
                    )
                }
                val levelUpgradesCents = revenueLedger
                    .filter { it.source == "level_upgrade" }
                    .sumOf { it.adminShareCents }
                val radioAccessCents = revenueLedger
                    .filter { it.source == "radio_access" }
                    .sumOf { it.adminShareCents }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard(
                        title = "Levels ${formatUsdCents(levelUpgradesCents)} • Radio ${formatUsdCents(radioAccessCents)} + Subs",
                        value = formatUsdCents(directAppSalesCents + sparkCommissionCents),
                        highlight = false,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Withdrawn to Bank\n${formatNgnFromUsdCents(adminWithdrawnCents, usdToNgnRate)}",
                        value = formatUsdCents(adminWithdrawnCents),
                        highlight = false,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (!lastAdEventStatus.isNullOrBlank()) {
                    Text(
                        text = "AdMob SDK: $lastAdEventStatus",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                AdminBankAccountCard(
                    adminPayoutAccount = adminPayoutAccount,
                    platformFeePercent = platformSettings?.platformFeePercent ?: 20L,
                    isSaving = isSaving,
                    onSaveAdminPayoutAccount = onSaveAdminPayoutAccount
                )

                // Withdraw Accumulated App Revenue in USD or Naira
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Withdraw 70% Ad & App Revenue", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = if (adminPayoutAccount != null) {
                                "Ready: ${formatDualCurrency(adminAvailableCents, usdToNgnRate)} → ${adminPayoutAccount.bankName} (${maskAccountNumber(adminPayoutAccount.accountNumber)}) [${adminPayoutAccount.payoutCurrency}]."
                            } else {
                                "Save your USD or Nigerian Naira receiving account above to withdraw revenue."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = onWithdrawAdminRevenue,
                            enabled = !isSaving && adminPayoutAccount != null && adminAvailableCents >= 100L,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("withdraw_admin_revenue_button")
                        ) {
                            Icon(Icons.Filled.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Withdraw ${formatDualCurrency(adminAvailableCents, usdToNgnRate)}")
                        }
                    }
                }

                AdminDownloadAndAdConfigCard(
                    platformSettings = platformSettings,
                    isSaving = isSaving,
                    onSaveConfig = onUpdateAdminConfig
                )

                Text(
                    text = "AdSense/AdMob pays the publisher automatically on Google's ~21st monthly schedule. Automated user bank/crypto transfers require a connected payout processor (Paystack/Flutterwave/Wise/Coinbase).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (adminWithdrawals.isNotEmpty()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Text("Admin Revenue Withdrawal History", style = MaterialTheme.typography.titleMedium)
                    adminWithdrawals.take(10).forEach { w ->
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
                                        text = "${formatDualCurrency(w.amountCents, usdToNgnRate)} [${w.payoutCurrency}] → ${w.bankName} ${w.maskedAccountNumber}",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "Ref: ${w.referenceCode} • ${formatRelativeTimestamp(w.createdAt)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                StatusPill(if (w.status == "completed") "paid" else w.status)
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Creator Payouts (30% Ads + 80% Sparks)", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Queued: ${formatDualCurrency(pendingCreatorPayouts, usdToNgnRate)} (${pendingList.size} pending)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (pendingList.isNotEmpty()) {
                    Button(
                        onClick = onProcessAutoPayoutBatch,
                        enabled = !isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("process_auto_payout_batch_button")
                    ) {
                        Icon(Icons.Filled.AutoMode, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Process ${pendingList.size} Auto-Payouts (YouTube Style)")
                    }
                }

                if (payoutRequests.isEmpty()) {
                    Text(
                        text = "No creator payout requests queued yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    payoutRequests.take(20).forEach { req ->
                        var refInput by remember(req.id) { mutableStateOf("TX-${req.id.take(6).uppercase()}") }
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
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
                                    Column(modifier = Modifier.weight(1f)) {
                                        val autoLabel = if (req.isAutoPayout) " [YT AUTO]" else ""
                                        Text(
                                            text = "${req.userName} • ${formatDualCurrency(req.amountCents, usdToNgnRate)}$autoLabel",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = "${req.payoutCurrency} • ${req.payoutBankOrNetwork.ifBlank { req.payoutMethod.uppercase() }}: ${req.payoutAccount}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    StatusPill(req.status)
                                }
                                if (req.status == "pending") {
                                    OutlinedTextField(
                                        value = refInput,
                                        onValueChange = { if (it.length <= 120) refInput = it },
                                        label = { Text("Transfer / Receipt Reference") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { onSettlePayoutRequest(req.id, false, "", "Declined by admin") },
                                            enabled = !isSaving,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Decline")
                                        }
                                        Button(
                                            onClick = { onSettlePayoutRequest(req.id, true, refInput, "Settled by admin") },
                                            enabled = !isSaving && refInput.isNotBlank(),
                                            modifier = Modifier
                                                .weight(1.3f)
                                                .testTag("approve_payout_${req.id}")
                                        ) {
                                            Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(5.dp))
                                            Text("Mark Paid")
                                        }
                                    }
                                } else if (!req.payoutReference.isNullOrBlank()) {
                                    Text(
                                        text = "Settlement Ref: ${req.payoutReference}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MatchEmerald
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text("All App Revenue Ledger", style = MaterialTheme.typography.titleMedium)
                if (revenueLedger.isEmpty()) {
                    Text(
                        text = "No revenue transactions recorded yet. AdMob impressions (70/30), Subscriptions, Boosts & Spark Gifts appear here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    revenueLedger.take(25).forEach { entry ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${entry.source.replace('_', ' ').uppercase()} • ${entry.productId}",
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Text(
                                    text = "Admin: ${formatUsdCents(entry.adminShareCents)} • Creator: ${formatUsdCents(entry.creatorShareCents)} • ${formatRelativeTimestamp(entry.createdAt)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = formatDualCurrency(entry.grossCents, usdToNgnRate),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
