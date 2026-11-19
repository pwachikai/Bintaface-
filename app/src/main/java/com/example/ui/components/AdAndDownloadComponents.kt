package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.FrameLayout
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.DEFAULT_ADMOB_BANNER_UNIT_ID
import com.example.data.model.DEFAULT_APP_DOWNLOAD_URL
import com.example.data.model.buildBintafaceInAppDeepLink
import com.example.data.model.buildBintafaceUserAccessUrl
import com.example.data.model.normalizeBintafaceAccessUrl
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.OnPaidEventListener

@Composable
fun GoogleAdBannerCard(
    adUnitId: String = DEFAULT_ADMOB_BANNER_UNIT_ID,
    creatorId: String = "",
    creatorName: String = "",
    postId: String = "feed_banner",
    onPaidMicros: (
        valueMicros: Long,
        currencyCode: String,
        adUnitId: String,
        creatorId: String,
        creatorName: String,
        postId: String
    ) -> Unit,
    onBannerLoaded: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cleanUnit = adUnitId.trim().ifBlank { DEFAULT_ADMOB_BANNER_UNIT_ID }

    val latestCreatorId by rememberUpdatedState(creatorId)
    val latestCreatorName by rememberUpdatedState(creatorName)
    val latestPostId by rememberUpdatedState(postId)
    val latestOnPaidMicros by rememberUpdatedState(onPaidMicros)
    val latestOnBannerLoaded by rememberUpdatedState(onBannerLoaded)

    val adView = remember(context, cleanUnit) {
        runCatching {
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                this.adUnitId = cleanUnit
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
                onPaidEventListener = OnPaidEventListener { adValue ->
                    latestOnPaidMicros(
                        adValue.valueMicros,
                        adValue.currencyCode.ifBlank { "USD" },
                        cleanUnit,
                        latestCreatorId,
                        latestCreatorName,
                        latestPostId
                    )
                }
                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        latestOnBannerLoaded()
                    }
                }
            }
        }.getOrNull()
    }

    LaunchedEffect(adView) {
        if (adView != null) {
            runCatching { adView.loadAd(AdRequest.Builder().build()) }
        }
    }

    DisposableEffect(lifecycleOwner, adView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> runCatching { adView?.resume() }
                Lifecycle.Event.ON_PAUSE -> runCatching { adView?.pause() }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            runCatching { adView?.destroy() }
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("google_ad_banner_card"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.AdsClick,
                    contentDescription = "Google Ad",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Sponsored • Google Ad (70% App / 30% Creators)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
            if (adView != null) {
                AndroidView(
                    factory = { adView },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                )
            } else {
                Text(
                    text = "Google AdMob unit ready ($cleanUnit)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AppDownloadShareCard(
    downloadUrl: String,
    userAccessHandleOrId: String? = null,
    onCopiedMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val baseAccessUrl = normalizeBintafaceAccessUrl(downloadUrl)
    val personalAccessUrl = remember(baseAccessUrl, userAccessHandleOrId) {
        if (!userAccessHandleOrId.isNullOrBlank()) {
            buildBintafaceUserAccessUrl(baseAccessUrl, userAccessHandleOrId)
        } else {
            null
        }
    }
    val inAppDeepLink = remember(userAccessHandleOrId) {
        buildBintafaceInAppDeepLink(userAccessHandleOrId)
    }
    // 0 = Live App Link, 1 = Personal Invite Link, 2 = App Deep Link (bintaface://)
    var selectedMode by remember(personalAccessUrl) { mutableStateOf(0) }
    val activeUrl = when (selectedMode) {
        1 -> personalAccessUrl ?: baseAccessUrl
        2 -> inAppDeepLink
        else -> baseAccessUrl
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("app_download_share_card"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
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
                    imageVector = Icons.Filled.Link,
                    contentDescription = "Share BINTAFACE App",
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Share BINTAFACE App",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Share your APK / Play Store download link or bintaface:// app invite.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedMode == 0,
                    onClick = { selectedMode = 0 },
                    label = { Text("Download Link") },
                    modifier = Modifier.testTag("access_link_main_chip")
                )
                if (personalAccessUrl != null) {
                    FilterChip(
                        selected = selectedMode == 1,
                        onClick = { selectedMode = 1 },
                        label = { Text("Invite") },
                        modifier = Modifier.testTag("access_link_personal_chip")
                    )
                }
                FilterChip(
                    selected = selectedMode == 2,
                    onClick = { selectedMode = 2 },
                    label = { Text("App Link") },
                    modifier = Modifier.testTag("access_link_deeplink_chip")
                )
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Text(
                    text = activeUrl,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("app_download_link_text")
                )
            }

            if (selectedMode != 2 && activeUrl.contains("ais-pre-", ignoreCase = true)) {
                Text(
                    text = "Note: This AI Studio preview URL is only for you/invited testers. To let anyone join, export an APK or publish to Google Play and save that link in Admin Console.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        clipboard?.setPrimaryClip(ClipData.newPlainText("BINTAFACE Users Access Link", activeUrl))
                        onCopiedMessage("BINTAFACE access link copied: $activeUrl")
                    },
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("copy_download_link_button")
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Copy")
                }

                OutlinedButton(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Access BINTAFACE on bintaface.org")
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Join me on BINTAFACE — social feed, reels, live streams, pages & creator earnings: $activeUrl"
                            )
                        }
                        runCatching {
                            context.startActivity(Intent.createChooser(shareIntent, "Share BINTAFACE Access Link"))
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("share_download_link_button")
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Share")
                }

                Button(
                    onClick = {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(activeUrl)))
                        }.onFailure {
                            onCopiedMessage("Copy link to open in browser: $activeUrl")
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("open_download_link_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Open")
                }
            }
        }
    }
}
