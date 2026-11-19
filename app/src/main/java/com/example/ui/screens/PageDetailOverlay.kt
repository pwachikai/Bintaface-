package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.PagePost
import com.example.data.model.SocialPage
import com.example.data.model.formatUsdCents
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.GoogleAdBannerCard
import com.example.ui.components.MomentMediaView
import com.example.ui.components.ProfileAvatar
import com.example.ui.components.formatRelativeTimestamp
import com.example.ui.theme.SuperlikeGold
import com.example.ui.util.encodeUriToCompactBase64
import com.example.ui.viewmodel.UiState
import kotlinx.coroutines.launch

@Composable
fun PageDetailOverlay(
    page: SocialPage,
    currentUserId: String,
    isFollowing: Boolean,
    postsState: UiState<List<PagePost>>,
    likedPagePostIds: Set<String>,
    isSaving: Boolean,
    admobBannerUnitId: String,
    onBack: () -> Unit,
    onToggleFollow: () -> Unit,
    onEditPage: () -> Unit,
    onSendSparkGift: () -> Unit,
    onPublishPagePost: (text: String, imageKey: String?, photoBase64: String?, onDone: () -> Unit) -> Unit,
    onToggleLikePost: (postId: String, currentlyLiked: Boolean) -> Unit,
    onDeletePost: (postId: String) -> Unit,
    onGoogleAdPaid: (
        adFormat: String,
        adUnitId: String,
        valueMicros: Long,
        currencyCode: String,
        creatorId: String,
        creatorName: String,
        postId: String
    ) -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val isOwner = page.ownerId == currentUserId
    var showPostComposer by remember { mutableStateOf(false) }
    var pendingDeletePagePostId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (isOwner) {
                FloatingActionButton(
                    onClick = { showPostComposer = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("create_page_post_fab")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Publish Page post")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        MomentMediaView(
                            imageKey = page.coverKey,
                            photoBase64 = null,
                            contentDescription = "${page.name} cover banner",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(155.dp)
                        )
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                            modifier = Modifier
                                .padding(12.dp)
                                .align(Alignment.TopStart)
                        ) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("page_detail_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back to Pages"
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(3.dp, MaterialTheme.colorScheme.surface),
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 16.dp)
                                .offset(y = 26.dp)
                        ) {
                            ProfileAvatar(
                                name = page.name,
                                avatarKey = page.ownerAvatarKey,
                                photoBase64 = page.photoBase64,
                                size = 76.dp
                            )
                        }
                    }
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp, start = 16.dp, end = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = page.name,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = page.category,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = "${page.handle} • ${page.followersCount} followers • ${page.postsCount} posts • Managed by ${page.ownerName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isOwner) {
                                Button(
                                    onClick = { showPostComposer = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .minimumInteractiveComponentSize()
                                        .testTag("page_owner_post_button")
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Write Post")
                                }
                                OutlinedButton(
                                    onClick = onEditPage,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("edit_page_button")
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Edit Page")
                                }
                            } else {
                                Button(
                                    onClick = onToggleFollow,
                                    modifier = Modifier
                                        .weight(1f)
                                        .minimumInteractiveComponentSize()
                                        .testTag("page_detail_follow_button")
                                ) {
                                    Text(if (isFollowing) "Following" else "Follow Page")
                                }
                                OutlinedButton(
                                    onClick = onSendSparkGift,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("page_spark_gift_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AutoAwesome,
                                        contentDescription = null,
                                        tint = SuperlikeGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("$1 Spark")
                                }
                            }
                        }

                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "About Page",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = page.description.ifBlank { "Official ${page.category} Page on BINTAFACE." },
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (page.websiteUrl.isNotBlank()) {
                                    val cleanLink = page.websiteUrl.trim().let { url ->
                                        if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                runCatching {
                                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(cleanLink)))
                                                }
                                            }
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Language,
                                            contentDescription = "Website",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = page.websiteUrl,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                if (page.totalGiftsCents > 0L) {
                                    Text(
                                        text = "Creator Spark support received: ${formatUsdCents(page.totalGiftsCents)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = SuperlikeGold,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        GoogleAdBannerCard(
                            adUnitId = admobBannerUnitId,
                            creatorId = page.ownerId,
                            creatorName = page.name,
                            postId = "page_${page.id}",
                            onPaidMicros = { micros, currency, unitId, cId, cName, pId ->
                                onGoogleAdPaid("banner", unitId, micros, currency, cId, cName, pId)
                            }
                        )
                    }
                }

                item {
                    Text(
                        text = "Page Posts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                when (postsState) {
                    is UiState.Loading -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                    is UiState.Error -> {
                        item {
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                EmptyStateCard(
                                    icon = Icons.Filled.Flag,
                                    title = "Couldn't load Page posts",
                                    subtitle = postsState.message
                                )
                            }
                        }
                    }
                    is UiState.Success -> {
                        if (postsState.data.isEmpty()) {
                            item {
                                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    EmptyStateCard(
                                        icon = Icons.Filled.Flag,
                                        title = "No Page updates yet",
                                        subtitle = if (isOwner) "Publish announcements, product drops, and updates to grow your followers."
                                        else "${page.name} hasn't posted updates yet.",
                                        actionLabel = if (isOwner) "Write First Page Post" else null,
                                        onAction = if (isOwner) { { showPostComposer = true } } else null
                                    )
                                }
                            }
                        } else {
                            items(postsState.data, key = { "pagepost_${it.id}" }) { post ->
                                val isLiked = post.id in likedPagePostIds
                                Surface(
                                    shape = MaterialTheme.shapes.large,
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .testTag("page_post_card_${post.id}")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                ProfileAvatar(
                                                    name = page.name,
                                                    avatarKey = page.ownerAvatarKey,
                                                    photoBase64 = page.photoBase64,
                                                    size = 38.dp
                                                )
                                                Column {
                                                    Text(
                                                        text = post.pageName,
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = formatRelativeTimestamp(post.createdAt),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            if (isOwner) {
                                                IconButton(
                                                    onClick = { pendingDeletePagePostId = post.id },
                                                    modifier = Modifier.minimumInteractiveComponentSize()
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.DeleteOutline,
                                                        contentDescription = "Delete Page post",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }

                                        Text(
                                            text = post.text,
                                            style = MaterialTheme.typography.bodyLarge
                                        )

                                        if (!post.postPhotoBase64.isNullOrBlank() || !post.imageKey.isNullOrBlank()) {
                                            MomentMediaView(
                                                imageKey = post.imageKey,
                                                photoBase64 = post.postPhotoBase64,
                                                contentDescription = "Page post attachment",
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(190.dp)
                                                    .clip(MaterialTheme.shapes.medium)
                                            )
                                        }

                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(50))
                                                .minimumInteractiveComponentSize()
                                                .clickable { onToggleLikePost(post.id, isLiked) }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                                .testTag("like_page_post_${post.id}"),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                                contentDescription = if (isLiked) "Unlike" else "Like",
                                                tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(19.dp)
                                            )
                                            Text(
                                                text = "${post.likesCount} Likes",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
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

    if (showPostComposer) {
        CreatePagePostSheet(
            pageName = page.name,
            isSaving = isSaving,
            onDismiss = { showPostComposer = false },
            onPublish = { text, imageKey, photo ->
                onPublishPagePost(text, imageKey, photo) {
                    showPostComposer = false
                }
            }
        )
    }

    pendingDeletePagePostId?.let { targetPostId ->
        AlertDialog(
            onDismissRequest = { pendingDeletePagePostId = null },
            title = { Text("Delete this Page post?") },
            text = { Text("This update will be permanently removed from ${page.name}.") },
            confirmButton = {
                Button(
                    onClick = {
                        pendingDeletePagePostId = null
                        onDeletePost(targetPostId)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { pendingDeletePagePostId = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreatePagePostSheet(
    pageName: String,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onPublish: (text: String, imageKey: String?, photoBase64: String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var text by remember { mutableStateOf("") }
    var imageKey by remember { mutableStateOf<String?>(null) }
    var photoBase64 by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isProcessing = true
                val encoded = encodeUriToCompactBase64(context, uri)
                if (encoded != null) {
                    photoBase64 = encoded
                    imageKey = null
                }
                isProcessing = false
            }
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
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Post as $pageName",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }

            OutlinedTextField(
                value = text,
                onValueChange = { if (it.length <= 1000) text = it },
                placeholder = { Text("Share an update, promo, or announcement on your Page…") },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("page_post_text_input")
            )

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        picker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    enabled = !isProcessing
                ) {
                    Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (photoBase64 != null) "Photo Ready" else "Add Photo")
                }
                listOf(null to "Text Only", "studio" to "Studio", "sunset" to "Rooftop", "coffee" to "Cafe").forEach { (k, label) ->
                    FilterChip(
                        selected = photoBase64 == null && imageKey == k,
                        onClick = {
                            photoBase64 = null
                            imageKey = k
                        },
                        label = { Text(label) }
                    )
                }
            }

            Button(
                onClick = { onPublish(text.trim(), imageKey, photoBase64) },
                enabled = !isSaving && !isProcessing && text.trim().isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("publish_page_post_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Publish to Page", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
