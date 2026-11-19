package com.example.ui.screens

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ReceiptLong
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
import com.example.data.model.DEFAULT_USD_TO_NGN_RATE
import com.example.data.model.SHOP_CATEGORIES
import com.example.data.model.Shop
import com.example.data.model.ShopOrder
import com.example.data.model.formatDualCurrency
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.MomentMediaView
import com.example.ui.components.ProfileAvatar
import com.example.ui.theme.MatchEmerald
import com.example.ui.util.encodeUriToCompactBase64
import com.example.ui.viewmodel.UiState
import kotlinx.coroutines.launch

private val SHOP_COVERS = listOf(
    "studio" to "Studio",
    "coffee" to "Cafe",
    "sunset" to "Gold",
    "trail" to "Coastal"
)

fun LazyListScope.shopsDirectoryItems(
    currentUserId: String,
    shopsState: UiState<List<Shop>>,
    buyerOrdersCount: Int,
    sellerOrdersCount: Int,
    usdToNgnRate: Long = DEFAULT_USD_TO_NGN_RATE,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit,
    onOpenCreateShop: () -> Unit,
    onOpenOrdersSheet: () -> Unit,
    onOpenShop: (Shop) -> Unit
) {
    item {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Storefront,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Column {
                            Text(
                                text = "BINTAFACE Shops",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Open your store, list products & keep 80% per sale",
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
                    Button(
                        onClick = onOpenCreateShop,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("open_create_shop_button")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Open a Shop")
                    }
                    OutlinedButton(
                        onClick = onOpenOrdersSheet,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("open_shop_orders_button")
                    ) {
                        Icon(Icons.Filled.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        val totalOrders = buyerOrdersCount + sellerOrdersCount
                        Text(if (totalOrders > 0) "Orders ($totalOrders)" else "Orders")
                    }
                }
            }
        }
    }

    item {
        val cats = listOf("All") + SHOP_CATEGORIES
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            cats.forEach { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { onSelectCategory(cat) },
                    label = { Text(cat) }
                )
            }
        }
    }

    when (shopsState) {
        is UiState.Loading -> {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
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
                    title = "Couldn't load Shops",
                    subtitle = shopsState.message
                )
            }
        }
        is UiState.Success -> {
            val filtered = shopsState.data.filter {
                selectedCategory == "All" || it.category.equals(selectedCategory, ignoreCase = true)
            }
            if (filtered.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Filled.Storefront,
                        title = "No Shops here yet",
                        subtitle = "Launch the first BINTAFACE Shop in this category and start selling.",
                        actionLabel = "Open Your Shop",
                        onAction = onOpenCreateShop
                    )
                }
            } else {
                items(filtered, key = { it.id }) { shop ->
                    ShopDirectoryCard(
                        shop = shop,
                        isMine = shop.ownerId == currentUserId,
                        usdToNgnRate = usdToNgnRate,
                        onClick = { onOpenShop(shop) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ShopDirectoryCard(
    shop: Shop,
    isMine: Boolean,
    usdToNgnRate: Long,
    onClick: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("shop_card_${shop.id}")
    ) {
        Column {
            if (!shop.photoBase64.isNullOrBlank() || shop.coverKey.isNotBlank()) {
                MomentMediaView(
                    imageKey = shop.coverKey,
                    photoBase64 = shop.photoBase64,
                    contentDescription = "${shop.name} storefront banner",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                )
            }
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ProfileAvatar(
                        name = shop.name,
                        avatarKey = shop.ownerAvatarKey,
                        size = 42.dp
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = shop.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isMine) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MatchEmerald.copy(alpha = 0.16f)
                                ) {
                                    Text(
                                        text = "YOURS",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MatchEmerald,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "${shop.handle} • ${shop.category}${if (shop.city.isNotBlank()) " • ${shop.city}" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = shop.currency,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                if (shop.description.isNotBlank()) {
                    Text(
                        text = shop.description,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Inventory2,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${shop.productsCount} products • ${shop.ordersCount} orders",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (shop.totalSalesCents > 0L) {
                        Text(
                            text = formatDualCurrency(shop.totalSalesCents, usdToNgnRate),
                            style = MaterialTheme.typography.labelMedium,
                            color = MatchEmerald,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "Visit Shop →",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOrEditShopSheet(
    existingShop: Shop?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (
        name: String,
        handle: String,
        category: String,
        description: String,
        currency: String,
        city: String,
        coverKey: String,
        photoBase64: String?
    ) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember(existingShop?.id) { mutableStateOf(existingShop?.name ?: "") }
    var handle by remember(existingShop?.id) { mutableStateOf(existingShop?.handle ?: "") }
    var category by remember(existingShop?.id) { mutableStateOf(existingShop?.category ?: "Fashion") }
    var description by remember(existingShop?.id) { mutableStateOf(existingShop?.description ?: "") }
    var currency by remember(existingShop?.id) { mutableStateOf(existingShop?.currency ?: "NGN") }
    var city by remember(existingShop?.id) { mutableStateOf(existingShop?.city ?: "") }
    var coverKey by remember(existingShop?.id) { mutableStateOf(existingShop?.coverKey ?: "studio") }
    var photoBase64 by remember(existingShop?.id) { mutableStateOf(existingShop?.photoBase64) }
    var isProcessingPhoto by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isProcessingPhoto = true
                encodeUriToCompactBase64(context, uri)?.let { photoBase64 = it }
                isProcessingPhoto = false
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
                    text = if (existingShop == null) "Open a BINTAFACE Shop" else "Edit Shop Details",
                    style = MaterialTheme.typography.headlineMedium
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 80) name = it },
                label = { Text("Shop Name") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("shop_name_input")
            )
            if (existingShop == null) {
                OutlinedTextField(
                    value = handle,
                    onValueChange = { if (it.length <= 36) handle = it },
                    label = { Text("@shophandle") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            OutlinedTextField(
                value = city,
                onValueChange = { if (it.length <= 60) city = it },
                label = { Text("City / Delivery Area") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = description,
                onValueChange = { if (it.length <= 400) description = it },
                label = { Text("What do you sell?") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            Text("Storefront Category", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SHOP_CATEGORIES.forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(cat) }
                    )
                }
            }

            Text("Primary Currency", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("NGN" to "₦ Naira (NGN)", "USD" to "$ US Dollar (USD)").forEach { (code, label) ->
                    FilterChip(
                        selected = currency == code,
                        onClick = { currency = code },
                        label = { Text(label) }
                    )
                }
            }

            Text("Banner Artwork", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    enabled = !isProcessingPhoto
                ) {
                    Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (photoBase64 != null) "Custom Photo Ready" else "Upload Banner")
                }
                SHOP_COVERS.forEach { (key, label) ->
                    FilterChip(
                        selected = photoBase64 == null && coverKey == key,
                        onClick = {
                            photoBase64 = null
                            coverKey = key
                        },
                        label = { Text(label) }
                    )
                }
            }

            Button(
                onClick = {
                    onSubmit(name, handle, category, description, currency, city, coverKey, photoBase64)
                },
                enabled = !isSaving && !isProcessingPhoto && name.trim().length >= 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("submit_shop_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (existingShop == null) "Launch Shop" else "Save Shop",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyShopOrdersSheet(
    buyerOrders: List<ShopOrder>,
    sellerOrders: List<ShopOrder>,
    usdToNgnRate: Long,
    onUpdateOrderStatus: (orderId: String, newStatus: String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tab by remember { mutableStateOf("buying") }

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
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Shop Orders", style = MaterialTheme.typography.headlineMedium)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = tab == "buying",
                    onClick = { tab = "buying" },
                    label = { Text("My Purchases (${buyerOrders.size})") }
                )
                FilterChip(
                    selected = tab == "selling",
                    onClick = { tab = "selling" },
                    label = { Text("Shop Sales (${sellerOrders.size})") }
                )
            }

            val list = if (tab == "buying") buyerOrders else sellerOrders
            if (list.isEmpty()) {
                Text(
                    text = if (tab == "buying") "You haven't placed any shop orders yet."
                    else "No customer orders on your shops yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 20.dp)
                )
            } else {
                list.forEach { order ->
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${order.productTitle} x${order.quantity}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                StatusPill(
                                    status = when (order.status) {
                                        "fulfilled" -> "paid"
                                        "cancelled" -> "rejected"
                                        else -> "pending"
                                    }
                                )
                            }
                            Text(
                                text = "${order.shopName} • ${formatDualCurrency(order.totalCents, usdToNgnRate)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (tab == "selling") "Buyer: ${order.buyerName} • Your 80%: ${formatDualCurrency(order.sellerShareCents, usdToNgnRate)}"
                                else "Seller: ${order.sellerName} • Status: ${order.status.uppercase()}",
                                style = MaterialTheme.typography.labelMedium
                            )
                            if (!order.deliveryNote.isNullOrBlank()) {
                                Text(
                                    text = "Note: ${order.deliveryNote}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (tab == "selling" && order.status == "placed") {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { onUpdateOrderStatus(order.id, "fulfilled") },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Mark Fulfilled")
                                    }
                                    OutlinedButton(
                                        onClick = { onUpdateOrderStatus(order.id, "cancelled") },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Cancel")
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
