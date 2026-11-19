package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.PublicProfile
import com.example.data.model.levelTierForId
import com.example.ui.theme.MatchEmerald
import com.example.ui.theme.VipBlackDiamondColor
import com.example.ui.theme.VipGoldenDiamondColor
import com.example.ui.theme.VipMoonDiamondColor
import com.example.ui.theme.VipProGeneralDiamondColor
import com.example.ui.theme.VipRedDiamondColor
import com.example.ui.theme.VipRockDiamondColor
import com.example.ui.theme.VipSilverDiamondColor
import com.example.ui.theme.VipStarDiamondColor
import com.example.ui.theme.VipSuperDiamondColor
import com.example.ui.theme.VipWaterColor
import com.example.ui.util.encodeUriToCompactBase64
import com.example.ui.util.presetDrawableForKey
import com.example.ui.util.rememberDecodedBase64Bitmap
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProfileAvatar(
    name: String,
    avatarKey: String,
    photoBase64: String? = null,
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    val decoded = rememberDecodedBase64Bitmap(photoBase64)
    val presetRes = presetDrawableForKey(avatarKey)
    val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "B"

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        when {
            decoded != null -> {
                Image(
                    bitmap = decoded,
                    contentDescription = "$name avatar",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            presetRes != null -> {
                Image(
                    painter = painterResource(id = presetRes),
                    contentDescription = "$name avatar",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            else -> {
                Text(
                    text = initial,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun MomentMediaView(
    imageKey: String?,
    photoBase64: String?,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val decoded = rememberDecodedBase64Bitmap(photoBase64)
    val presetRes = presetDrawableForKey(imageKey)
    if (decoded != null) {
        Image(
            bitmap = decoded,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else if (presetRes != null) {
        Image(
            painter = painterResource(id = presetRes),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun IntentBadge(lookingFor: String, modifier: Modifier = Modifier) {
    val label = when (lookingFor) {
        "dating" -> "Looking for Dating"
        "friends" -> "Looking for Friends"
        else -> "Dating & Friends"
    }
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
        shape = RoundedCornerShape(50)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun PlusBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(50)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.WorkspacePremium,
                contentDescription = "Bintaface+ Member",
                tint = Color.White,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = "PLUS",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SpotlightBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.secondary,
        shape = RoundedCornerShape(50)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Bolt,
                contentDescription = "Spotlight Boost active",
                tint = Color.White,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = "SPOTLIGHT",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

fun vipLevelAccentColor(levelId: String?): Color = when (levelId?.lowercase()) {
    "water" -> VipWaterColor
    "black_diamond" -> VipBlackDiamondColor
    "red_diamond" -> VipRedDiamondColor
    "super_diamond" -> VipSuperDiamondColor
    "rock_diamond" -> VipRockDiamondColor
    "star_diamond" -> VipStarDiamondColor
    "silver_diamond" -> VipSilverDiamondColor
    "golden_diamond" -> VipGoldenDiamondColor
    "moon_diamond" -> VipMoonDiamondColor
    "professional_general_diamond" -> VipProGeneralDiamondColor
    else -> VipWaterColor
}

@Composable
fun VipLevelBadge(
    vipLevel: String?,
    showActivePill: Boolean = false,
    modifier: Modifier = Modifier
) {
    val tier = levelTierForId(vipLevel) ?: return
    val badgeColor = vipLevelAccentColor(tier.id)
    val shortTitle = if (tier.id == "professional_general_diamond") {
        "L10 • PRO GEN DIAMOND"
    } else {
        "L${tier.rank} • ${tier.name.uppercase()}"
    }
    Surface(
        modifier = modifier.testTag("vip_level_badge_${tier.id}"),
        color = badgeColor,
        shape = RoundedCornerShape(50)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = tier.emoji,
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = shortTitle,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            if (showActivePill) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MatchEmerald
                ) {
                    Text(
                        text = "ACTIVE",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileDetailBottomSheet(
    profile: PublicProfile,
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                val hasMedia = !profile.photoBase64.isNullOrBlank() || presetDrawableForKey(profile.avatarKey) != null
                if (hasMedia) {
                    MomentMediaView(
                        imageKey = profile.avatarKey,
                        photoBase64 = profile.photoBase64,
                        contentDescription = "${profile.displayName} photo",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = profile.displayName.take(1).uppercase(),
                            style = MaterialTheme.typography.displayLarge,
                            color = Color.White
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.68f))
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IntentBadge(lookingFor = profile.lookingFor)
                    Text(
                        text = "${profile.displayName}, ${profile.age}",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White
                    )
                    if (profile.city.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = profile.city,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.92f)
                            )
                        }
                    }
                }
            }

            if (profile.bio.isNotBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "About",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = profile.bio,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (profile.promptAnswer.isNotBlank()) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = profile.promptQuestion,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Text(
                            text = profile.promptAnswer,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (profile.interests.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Interests",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private val SUGGESTED_INTERESTS = listOf(
    "Coffee", "Hiking", "Photography", "Live Music",
    "Foodie", "Travel", "Art", "Fitness", "Books", "Cooking"
)

private val PROMPT_QUESTIONS = listOf(
    "My ideal Sunday looks like...",
    "A spontaneous adventure I'm always down for...",
    "The way to win me over is...",
    "Two truths and a lie..."
)

private val AVATAR_PRESETS = listOf(
    "coffee" to "Cafe Morning",
    "sunset" to "Golden Rooftop",
    "trail" to "Coastal Trail"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileEditorForm(
    initialProfile: PublicProfile?,
    defaultName: String,
    isSaving: Boolean,
    ctaLabel: String,
    onSave: (
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
        isDiscoverable: Boolean
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var displayName by remember(initialProfile) {
        mutableStateOf(initialProfile?.displayName?.ifBlank { defaultName } ?: defaultName)
    }
    var ageText by remember(initialProfile) {
        mutableStateOf((initialProfile?.age ?: 25L).toString())
    }
    var city by remember(initialProfile) {
        mutableStateOf(initialProfile?.city ?: "")
    }
    var bio by remember(initialProfile) {
        mutableStateOf(initialProfile?.bio ?: "")
    }
    var lookingFor by remember(initialProfile) {
        mutableStateOf(initialProfile?.lookingFor ?: "both")
    }
    var promptQuestion by remember(initialProfile) {
        mutableStateOf(initialProfile?.promptQuestion?.ifBlank { PROMPT_QUESTIONS.first() } ?: PROMPT_QUESTIONS.first())
    }
    var promptAnswer by remember(initialProfile) {
        mutableStateOf(initialProfile?.promptAnswer ?: "")
    }
    var avatarKey by remember(initialProfile) {
        mutableStateOf(initialProfile?.avatarKey ?: "sunset")
    }
    var photoBase64 by remember(initialProfile) {
        mutableStateOf(initialProfile?.photoBase64)
    }
    var isDiscoverable by remember(initialProfile) {
        mutableStateOf(initialProfile?.isDiscoverable ?: true)
    }
    val selectedInterests = remember(initialProfile) {
        mutableStateListOf<String>().apply {
            addAll(initialProfile?.interests ?: listOf("Coffee", "Travel"))
        }
    }
    var isProcessingPhoto by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isProcessingPhoto = true
                val encoded = encodeUriToCompactBase64(context, uri)
                if (encoded != null) {
                    photoBase64 = encoded
                }
                isProcessingPhoto = false
            }
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Photo / Theme Picker Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ProfileAvatar(
                name = displayName.ifBlank { "K" },
                avatarKey = avatarKey,
                photoBase64 = photoBase64,
                size = 76.dp
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    enabled = !isProcessingPhoto,
                    colors = ButtonDefaults.filledTonalButtonColors(),
                    modifier = Modifier.testTag("pick_profile_photo_button")
                ) {
                    if (isProcessingPhoto) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    } else {
                        Icon(Icons.Filled.AddAPhoto, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (photoBase64 == null) "Upload Photo" else "Change Photo")
                }
                if (photoBase64 != null) {
                    Text(
                        text = "Remove uploaded photo",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { photoBase64 = null }
                    )
                }
            }
        }

        Text(
            text = "Card Backdrop Theme",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AVATAR_PRESETS.forEach { (key, label) ->
                FilterChip(
                    selected = avatarKey == key,
                    onClick = { avatarKey = key },
                    label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = displayName,
                onValueChange = { if (it.length <= 40) displayName = it },
                label = { Text("Display Name") },
                singleLine = true,
                modifier = Modifier
                    .weight(1.7f)
                    .testTag("profile_name_input")
            )
            OutlinedTextField(
                value = ageText,
                onValueChange = { input ->
                    if (input.length <= 3 && input.all { it.isDigit() }) ageText = input
                },
                label = { Text("Age") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .weight(1f)
                    .testTag("profile_age_input")
            )
        }

        OutlinedTextField(
            value = city,
            onValueChange = { if (it.length <= 60) city = it },
            label = { Text("City / Neighborhood") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("profile_city_input")
        )

        OutlinedTextField(
            value = bio,
            onValueChange = { if (it.length <= 300) bio = it },
            label = { Text("Bio (for Feed & Dating Card)") },
            minLines = 2,
            maxLines = 4,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("profile_bio_input")
        )

        Text(
            text = "I'm here for",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("dating" to "Dating", "friends" to "Friends", "both" to "Both").forEach { (value, label) ->
                FilterChip(
                    selected = lookingFor == value,
                    onClick = { lookingFor = value },
                    label = { Text(label) },
                    leadingIcon = if (lookingFor == value) {
                        { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null
                )
            }
        }

        Text(
            text = "Icebreaker Prompt",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            PROMPT_QUESTIONS.forEach { q ->
                FilterChip(
                    selected = promptQuestion == q,
                    onClick = { promptQuestion = q },
                    label = { Text(q, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                )
            }
        }
        OutlinedTextField(
            value = promptAnswer,
            onValueChange = { if (it.length <= 240) promptAnswer = it },
            label = { Text("Your Answer") },
            minLines = 2,
            maxLines = 3,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("profile_prompt_input")
        )

        Text(
            text = "Interests (pick up to 8)",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SUGGESTED_INTERESTS.forEach { interest ->
                val selected = interest in selectedInterests
                FilterChip(
                    selected = selected,
                    onClick = {
                        if (selected) selectedInterests.remove(interest)
                        else if (selectedInterests.size < 8) selectedInterests.add(interest)
                    },
                    label = { Text(interest) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Show me in Discover",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Let community members match with your card",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isDiscoverable,
                    onCheckedChange = { isDiscoverable = it }
                )
            }
        }

        Button(
            onClick = {
                val parsedAge = (ageText.toIntOrNull() ?: 25).coerceIn(18, 100)
                onSave(
                    displayName.trim().ifBlank { defaultName.ifBlank { "BINTAFACE Member" } },
                    parsedAge,
                    city.trim(),
                    bio.trim(),
                    lookingFor,
                    promptQuestion,
                    promptAnswer.trim(),
                    selectedInterests.toList(),
                    avatarKey,
                    photoBase64,
                    isDiscoverable
                )
            },
            enabled = !isSaving && displayName.trim().length >= 2,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("save_profile_button")
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(ctaLabel, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

fun formatRelativeTimestamp(timestamp: Timestamp?): String {
    val date = timestamp?.toDate() ?: Date()
    val diffSec = ((System.currentTimeMillis() - date.time) / 1000L).coerceAtLeast(0L)
    return when {
        diffSec < 60 -> "Just now"
        diffSec < 3600 -> "${diffSec / 60}m ago"
        diffSec < 86400 -> "${diffSec / 3600}h ago"
        diffSec < 604800 -> "${diffSec / 86400}d ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
    }
}
