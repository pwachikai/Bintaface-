package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.example.data.model.PAGE_CATEGORIES
import com.example.data.model.SocialPage
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.MomentMediaView
import com.example.ui.components.ProfileAvatar
import com.example.ui.util.encodeUriToCompactBase64
import com.example.ui.viewmodel.UiState
import kotlinx.coroutines.launch

private val PAGE_COVER_OPTIONS = listOf(
    "studio" to "Studio",
    "sunset" to "Rooftop",
    "coffee" to "Cafe",
    "trail" to "Nature"
)

fun LazyListScope.pagesDirectoryItems(
    currentUserId: String,
    pagesState: UiState<List<SocialPage>>,
    followedPageIds: Set<String>,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit,
    onOpenCreatePage: () -> Unit,
    onOpenPage: (SocialPage) -> Unit,
    onToggleFollowPage: (SocialPage) -> Unit
) {
    item {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.primaryContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("pages_hero_card")
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Flag,
                            contentDescription = "BINTAFACE Pages",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "BINTAFACE Pages",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Create brand, business, creator & fan pages. Earn 30% AdMob + 80% Sparks.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                }
                Button(
                    onClick = onOpenCreatePage,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("create_page_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Create", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    item {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            (listOf("All") + PAGE_CATEGORIES).forEach { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { onSelectCategory(cat) },
                    label = { Text(cat) }
                )
            }
        }
    }

    when (pagesState) {
        is UiState.Loading -> {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
        is UiState.Error -> {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.Storefront,
                    title = "Couldn't load Pages",
                    subtitle = pagesState.message
                )
            }
        }
        is UiState.Success -> {
            val filtered = pagesState.data.filter {
                selectedCategory == "All" || it.category == selectedCategory
            }
            if (filtered.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Filled.Flag,
                        title = "No Pages in $selectedCategory yet",
                        subtitle = "Launch the first Facebook-style Page on BINTAFACE for your brand, business, or community.",
                        actionLabel = "Create a Page",
                        onAction = onOpenCreatePage
                    )
                }
            } else {
                val ownedPages = filtered.filter { it.ownerId == currentUserId }
                if (ownedPages.isNotEmpty()) {
                    item {
                        Text(
                            text = "Your Pages (${ownedPages.size})",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                items(filtered, key = { "page_${it.id}" }) { page ->
                    PageDirectoryCard(
                        page = page,
                        isOwner = page.ownerId == currentUserId,
                        isFollowing = page.id in followedPageIds,
                        onOpen = { onOpenPage(page) },
                        onToggleFollow = { onToggleFollowPage(page) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PageDirectoryCard(
    page: SocialPage,
    isOwner: Boolean,
    isFollowing: Boolean,
    onOpen: () -> Unit,
    onToggleFollow: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onOpen)
            .testTag("page_card_${page.id}")
    ) {
        Column {
            MomentMediaView(
                imageKey = page.coverKey,
                photoBase64 = null,
                contentDescription = "${page.name} cover banner",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)
            )
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ProfileAvatar(
                        name = page.name,
                        avatarKey = page.ownerAvatarKey,
                        photoBase64 = page.photoBase64,
                        size = 48.dp
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = page.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
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
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "${page.handle} • Managed by ${page.ownerName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (isOwner) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "Your Page",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    } else {
                        Button(
                            onClick = onToggleFollow,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("follow_page_button_${page.id}")
                        ) {
                            Text(
                                text = if (isFollowing) "Following" else "Follow",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                if (page.description.isNotBlank()) {
                    Text(
                        text = page.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.People,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${page.followersCount} followers",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "•",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${page.postsCount} posts",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOrEditPageSheet(
    existingPage: SocialPage?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (
        name: String,
        handle: String,
        category: String,
        description: String,
        websiteUrl: String,
        coverKey: String,
        photoBase64: String?
    ) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember(existingPage?.id) { mutableStateOf(existingPage?.name ?: "") }
    var handle by remember(existingPage?.id) { mutableStateOf(existingPage?.handle ?: "") }
    var category by remember(existingPage?.id) { mutableStateOf(existingPage?.category ?: "Creator") }
    var description by remember(existingPage?.id) { mutableStateOf(existingPage?.description ?: "") }
    var websiteUrl by remember(existingPage?.id) { mutableStateOf(existingPage?.websiteUrl ?: "") }
    var coverKey by remember(existingPage?.id) { mutableStateOf(existingPage?.coverKey ?: "studio") }
    var photoBase64 by remember(existingPage?.id) { mutableStateOf(existingPage?.photoBase64) }
    var isProcessingImage by remember { mutableStateOf(false) }

    val logoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isProcessingImage = true
                val encoded = encodeUriToCompactBase64(context, uri)
                if (encoded != null) photoBase64 = encoded
                isProcessingImage = false
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
                .verticalScroll(rememberScrollState())
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
                    text = if (existingPage == null) "Create a Page" else "Edit Page Details",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 80) name = it },
                label = { Text("Page Name") },
                placeholder = { Text("e.g. Gregory Media Studio") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("page_name_input")
            )

            if (existingPage == null) {
                OutlinedTextField(
                    value = handle,
                    onValueChange = { if (it.length <= 40) handle = it },
                    label = { Text("Page @handle") },
                    placeholder = { Text("@gregorymedia") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("page_handle_input")
                )
            }

            Text(
                text = "Page Category",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PAGE_CATEGORIES.forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(cat) }
                    )
                }
            }

            OutlinedTextField(
                value = description,
                onValueChange = { if (it.length <= 400) description = it },
                label = { Text("About / Bio") },
                placeholder = { Text("Describe what your Page shares with followers…") },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("page_description_input")
            )

            OutlinedTextField(
                value = websiteUrl,
                onValueChange = { if (it.length <= 200) websiteUrl = it },
                label = { Text("Website or Store Link (optional)") },
                placeholder = { Text("https://example.com") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "Cover Theme & Page Photo",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        logoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    enabled = !isProcessingImage
                ) {
                    Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (photoBase64 != null) "Logo Ready" else "Upload Logo")
                }
                PAGE_COVER_OPTIONS.forEach { (key, label) ->
                    FilterChip(
                        selected = coverKey == key,
                        onClick = { coverKey = key },
                        label = { Text(label) }
                    )
                }
            }

            Button(
                onClick = {
                    onSubmit(
                        name.trim(),
                        handle.trim(),
                        category,
                        description.trim(),
                        websiteUrl.trim(),
                        coverKey,
                        photoBase64
                    )
                },
                enabled = !isSaving && !isProcessingImage && name.trim().length >= 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_page_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (existingPage == null) "Publish Page" else "Save Page Changes",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
