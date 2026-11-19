package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.APP_ADMIN_EMAIL
import com.example.data.model.AdminPayoutAccount
import com.example.data.model.DEFAULT_ADMIN_ACCOUNT_HOLDER
import com.example.data.model.DEFAULT_ADMIN_ACCOUNT_NUMBER
import com.example.data.model.DEFAULT_ADMIN_BANK_NAME
import com.example.data.model.DEFAULT_ADMOB_BANNER_UNIT_ID
import com.example.data.model.DEFAULT_ADMOB_REWARDED_UNIT_ID
import com.example.data.model.DEFAULT_APP_DOWNLOAD_URL
import com.example.data.model.DEFAULT_USD_TO_NGN_RATE
import com.example.data.model.LEGACY_PLAY_APP_DOWNLOAD_URL
import com.example.data.model.PlatformSettings
import com.example.data.model.UNHOSTED_BINTAFACE_URL
import com.example.data.model.maskAccountNumber
import com.example.data.model.normalizeBintafaceAccessUrl
import com.example.ui.theme.MatchEmerald

private val ADMIN_PAYOUT_METHODS = listOf(
    "ngn_bank" to "₦ Naira Bank",
    "usd_bank" to "$ USD Bank",
    "paypal" to "PayPal",
    "upi" to "UPI",
    "mobile_money" to "Mobile Money"
)

@Composable
internal fun AdminBankAccountCard(
    adminPayoutAccount: AdminPayoutAccount?,
    platformFeePercent: Long,
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
    ) -> Unit
) {
    var selectedMethod by remember(adminPayoutAccount?.payoutMethod) {
        mutableStateOf(adminPayoutAccount?.payoutMethod?.ifBlank { "ngn_bank" } ?: "ngn_bank")
    }
    var selectedCurrency by remember(adminPayoutAccount?.payoutCurrency) {
        mutableStateOf(
            adminPayoutAccount?.payoutCurrency?.takeIf { it in setOf("USD", "NGN") } ?: "NGN"
        )
    }
    var accountHolderName by remember(adminPayoutAccount?.accountHolderName) {
        mutableStateOf(
            adminPayoutAccount?.accountHolderName?.ifBlank { DEFAULT_ADMIN_ACCOUNT_HOLDER }
                ?: DEFAULT_ADMIN_ACCOUNT_HOLDER
        )
    }
    var bankName by remember(adminPayoutAccount?.bankName) {
        mutableStateOf(
            adminPayoutAccount?.bankName?.ifBlank { DEFAULT_ADMIN_BANK_NAME }
                ?: DEFAULT_ADMIN_BANK_NAME
        )
    }
    var accountNumber by remember(adminPayoutAccount?.accountNumber) {
        mutableStateOf(
            adminPayoutAccount?.accountNumber?.ifBlank { DEFAULT_ADMIN_ACCOUNT_NUMBER }
                ?: DEFAULT_ADMIN_ACCOUNT_NUMBER
        )
    }
    var routingOrSwift by remember(adminPayoutAccount?.routingOrSwift) {
        mutableStateOf(adminPayoutAccount?.routingOrSwift ?: "")
    }
    var payoutContact by remember(adminPayoutAccount?.payoutEmailOrPhone) {
        mutableStateOf(adminPayoutAccount?.payoutEmailOrPhone?.ifBlank { APP_ADMIN_EMAIL } ?: APP_ADMIN_EMAIL)
    }

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                    text = "Admin Receiving Account (USD or Naira)",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (adminPayoutAccount != null) {
                Text(
                    text = "Active (${adminPayoutAccount.payoutCurrency}): ${adminPayoutAccount.bankName} (${maskAccountNumber(adminPayoutAccount.accountNumber)}) • ${adminPayoutAccount.accountHolderName}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MatchEmerald
                )
            } else {
                Text(
                    text = "Add your USD ($) or Nigerian Naira (₦) bank account number to receive 70% AdMob + all admin revenue.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ADMIN_PAYOUT_METHODS.forEach { (key, label) ->
                    FilterChip(
                        selected = selectedMethod == key,
                        onClick = {
                            selectedMethod = key
                            selectedCurrency = if (key == "ngn_bank") "NGN" else "USD"
                        },
                        label = { Text(label) }
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedCurrency == "NGN",
                    onClick = { selectedCurrency = "NGN" },
                    label = { Text("Settle in Naira (₦ NGN)") }
                )
                FilterChip(
                    selected = selectedCurrency == "USD",
                    onClick = { selectedCurrency = "USD" },
                    label = { Text("Settle in USD ($)") }
                )
            }

            OutlinedTextField(
                value = accountHolderName,
                onValueChange = { if (it.length <= 80) accountHolderName = it },
                label = { Text("Account Holder Full Name") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_holder_name_input")
            )

            OutlinedTextField(
                value = bankName,
                onValueChange = { if (it.length <= 80) bankName = it },
                label = {
                    Text(
                        when (selectedMethod) {
                            "ngn_bank" -> "Nigerian Bank (e.g. GTBank, Access, Zenith, OPay)"
                            "usd_bank" -> "USD Bank (e.g. Chase, Bank of America, Mercury)"
                            "paypal" -> "Provider (e.g. PayPal)"
                            "upi" -> "UPI App / Bank Name"
                            "mobile_money" -> "Mobile Money Network"
                            else -> "Bank Name"
                        }
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_bank_name_input")
            )

            OutlinedTextField(
                value = accountNumber,
                onValueChange = { if (it.length <= 64) accountNumber = it },
                label = {
                    Text(
                        when (selectedMethod) {
                            "ngn_bank" -> "10-Digit NUBAN Account Number"
                            "usd_bank" -> "USD Account Number / IBAN"
                            "paypal" -> "PayPal Account / Email"
                            "upi" -> "UPI ID (name@bank)"
                            "mobile_money" -> "Mobile Money Account / Phone Number"
                            else -> "Bank Account Number / IBAN"
                        }
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_account_number_input")
            )

            OutlinedTextField(
                value = routingOrSwift,
                onValueChange = { if (it.length <= 40) routingOrSwift = it },
                label = { Text("Routing / SWIFT / Bank Code (Optional)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_routing_input")
            )

            OutlinedTextField(
                value = payoutContact,
                onValueChange = { if (it.length <= 120) payoutContact = it },
                label = { Text("Admin Settlement Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            val canSaveAccount = accountHolderName.trim().length >= 2 &&
                bankName.trim().length >= 2 &&
                accountNumber.trim().length >= 4 &&
                !isSaving

            Button(
                onClick = {
                    onSaveAdminPayoutAccount(
                        selectedMethod,
                        accountHolderName,
                        bankName,
                        accountNumber,
                        routingOrSwift,
                        payoutContact,
                        platformFeePercent,
                        selectedCurrency
                    )
                },
                enabled = canSaveAccount,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_admin_account_button")
            ) {
                Text(if (adminPayoutAccount == null) "Save Admin $selectedCurrency Account" else "Update Receiving Account ($selectedCurrency)")
            }
        }
    }
}

@Composable
internal fun AdminDownloadAndAdConfigCard(
    platformSettings: PlatformSettings?,
    isSaving: Boolean,
    onSaveConfig: (
        usdToNgnRate: Long,
        autoPayoutEnabled: Boolean,
        appDownloadUrl: String,
        bannerUnitId: String,
        rewardedUnitId: String
    ) -> Unit
) {
    var rateText by remember(platformSettings?.usdToNgnRate) {
        mutableStateOf((platformSettings?.usdToNgnRate ?: DEFAULT_USD_TO_NGN_RATE).toString())
    }
    var downloadUrl by remember(platformSettings?.appDownloadUrl) {
        mutableStateOf(normalizeBintafaceAccessUrl(platformSettings?.appDownloadUrl))
    }
    var bannerUnitId by remember(platformSettings?.admobBannerUnitId) {
        mutableStateOf(platformSettings?.admobBannerUnitId?.ifBlank { DEFAULT_ADMOB_BANNER_UNIT_ID } ?: DEFAULT_ADMOB_BANNER_UNIT_ID)
    }
    var rewardedUnitId by remember(platformSettings?.admobRewardedUnitId) {
        mutableStateOf(platformSettings?.admobRewardedUnitId?.ifBlank { DEFAULT_ADMOB_REWARDED_UNIT_ID } ?: DEFAULT_ADMOB_REWARDED_UNIT_ID)
    }
    var autoPayout by remember(platformSettings?.autoPayoutEnabled) {
        mutableStateOf(platformSettings?.autoPayoutEnabled ?: true)
    }

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Google AdMob, Naira Rate & Download Link",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            OutlinedTextField(
                value = downloadUrl,
                onValueChange = { if (it.length <= 300) downloadUrl = it },
                label = { Text("Public APK / Play Store Download URL") },
                supportingText = {
                    Text("Paste your Google Play, Google Drive APK, or website download link for users.")
                },
                singleLine = true,
                trailingIcon = {
                    if (downloadUrl.trim() != DEFAULT_APP_DOWNLOAD_URL) {
                        TextButton(onClick = { downloadUrl = DEFAULT_APP_DOWNLOAD_URL }) {
                            Text("Reset")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_download_url_input")
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = downloadUrl.trim() == LEGACY_PLAY_APP_DOWNLOAD_URL,
                    onClick = { downloadUrl = LEGACY_PLAY_APP_DOWNLOAD_URL },
                    label = { Text("Play Store URL") }
                )
                FilterChip(
                    selected = downloadUrl.trim().equals(UNHOSTED_BINTAFACE_URL, ignoreCase = true),
                    onClick = { downloadUrl = UNHOSTED_BINTAFACE_URL },
                    label = { Text("bintaface.org") }
                )
                FilterChip(
                    selected = downloadUrl.trim() == DEFAULT_APP_DOWNLOAD_URL,
                    onClick = { downloadUrl = DEFAULT_APP_DOWNLOAD_URL },
                    label = { Text("AI Studio Preview") }
                )
            }

            OutlinedTextField(
                value = rateText,
                onValueChange = { rateText = it.filter { ch -> ch.isDigit() }.take(5) },
                label = { Text("USD → Naira Rate (₦ per $1.00)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_usd_ngn_rate_input")
            )

            OutlinedTextField(
                value = bannerUnitId,
                onValueChange = { if (it.length <= 80) bannerUnitId = it },
                label = { Text("Google AdMob Banner Ad Unit ID") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_banner_unit_input")
            )

            OutlinedTextField(
                value = rewardedUnitId,
                onValueChange = { if (it.length <= 80) rewardedUnitId = it },
                label = { Text("Google AdMob Rewarded Ad Unit ID") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_rewarded_unit_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "YouTube-Style Creator Auto-Payout Queue",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = "Auto-queue 30% Ad + 80% Spark payouts at $1.00 threshold",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = autoPayout,
                    onCheckedChange = { autoPayout = it },
                    modifier = Modifier.testTag("admin_auto_payout_switch")
                )
            }

            Button(
                onClick = {
                    val parsedRate = rateText.toLongOrNull()?.coerceIn(100L, 10000L) ?: DEFAULT_USD_TO_NGN_RATE
                    onSaveConfig(parsedRate, autoPayout, downloadUrl, bannerUnitId, rewardedUnitId)
                },
                enabled = !isSaving && downloadUrl.trim().length >= 8,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_admin_config_button")
            ) {
                Text("Save AdMob, Naira & Link Settings")
            }
        }
    }
}
