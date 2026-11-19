package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.DEFAULT_USD_TO_NGN_RATE
import com.example.data.model.Shop
import com.example.data.model.ShopProduct
import com.example.data.model.computeShop8020Split
import com.example.data.model.formatDualCurrency
import com.example.data.model.formatNgnWhole
import com.example.data.model.formatUsdCents
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.MomentMediaView
import com.example.ui.components.ProfileAvatar
import com.example.ui.theme.MatchEmerald
import com.example.ui.util.encodeUriToCompactBase64
import com.example.ui.viewmodel.UiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopDetailOverlay(
    currentUserId: String,
    shop: Shop,
    productsState: UiState<List<ShopProduct>>,
    usdToNgnRate: Long = DEFAULT_USD_TO_NGN_RATE,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onEditShop: () -> Unit,
    onAddProduct: (
        title: String,
        description: String,
        category: String,
        priceUsdCents: Long,
        priceNgn: Long,
        stockCount: Long,
        imageKey: String?,
        photoBase64: String?
    ) -> Unit,
    onPlaceOrder: (
        product: ShopProduct,
        quantity: Long,
        currency: String,
        deliveryNote: String,
        onDone: () -> Unit
    ) -> Unit
) {
    BackHandler { onDismiss() }

    val isOwner = shop.ownerId == currentUserId
    var showAddProductSheet by remember { mutableStateOf(false) }
    var checkoutProduct by remember { mutableStateOf<ShopProduct?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to shops")
                        }
                    },
                    title = {
                        Text(
                            text = shop.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    actions = {
                        if (isOwner) {
                            IconButton(onClick = onEditShop) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit shop")
                            }
                        }
                    }
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column {
                            if (!shop.photoBase64.isNullOrBlank() || shop.coverKey.isNotBlank()) {
                                MomentMediaView(
                                    imageKey = shop.coverKey,
                                    photoBase64 = shop.photoBase64,
                                    contentDescription = "${shop.name} banner",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(145.dp)
                                )
                            }
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    ProfileAvatar(
                                        name = shop.ownerName,
                                        avatarKey = shop.ownerAvatarKey,
                                        size = 44.dp
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = shop.name,
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${shop.handle} • Owned by ${shop.ownerName}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (shop.description.isNotBlank()) {
                                    Text(shop.description, style = MaterialTheme.typography.bodyMedium)
                                }
                                Text(
                                    text = "${shop.productsCount} products • ${shop.ordersCount} orders • Total Sales ${formatDualCurrency(shop.totalSalesCents, usdToNgnRate)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MatchEmerald,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (isOwner) {
                                    Button(
                                        onClick = { showAddProductSheet = true },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("add_shop_product_button")
                                    ) {
                                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Add Product")
                                    }
                                }
                            }
                        }
                    }
                }

                when (productsState) {
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
                                title = "Couldn't load products",
                                subtitle = productsState.message
                            )
                        }
                    }
                    is UiState.Success -> {
                        if (productsState.data.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    icon = Icons.Filled.ShoppingBag,
                                    title = "No products listed yet",
                                    subtitle = if (isOwner) "Add your first product so shoppers can order in USD or Naira."
                                    else "Check back soon — ${shop.ownerName} hasn't listed items yet.",
                                    actionLabel = if (isOwner) "Add Product" else null,
                                    onAction = if (isOwner) ({ showAddProductSheet = true }) else null
                                )
                            }
                        } else {
                            items(productsState.data, key = { it.id }) { product ->
                                ProductCatalogCard(
                                    product = product,
                                    isOwner = isOwner,
                                    onBuyClick = { checkoutProduct = product }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddProductSheet) {
        AddProductSheet(
            defaultCategory = shop.category,
            usdToNgnRate = usdToNgnRate,
            isSaving = isSaving,
            onDismiss = { showAddProductSheet = false },
            onAdd = { title, desc, cat, usdCents, ngn, stock, imgKey, photo ->
                onAddProduct(title, desc, cat, usdCents, ngn, stock, imgKey, photo)
                showAddProductSheet = false
            }
        )
    }

    checkoutProduct?.let { prod ->
        ProductCheckoutSheet(
            shop = shop,
            product = prod,
            usdToNgnRate = usdToNgnRate,
            isSaving = isSaving,
            onDismiss = { checkoutProduct = null },
            onConfirmOrder = { qty, currency, note ->
                onPlaceOrder(prod, qty, currency, note) {
                    checkoutProduct = null
                }
            }
        )
    }
}

@Composable
private fun ProductCatalogCard(
    product: ShopProduct,
    isOwner: Boolean,
    onBuyClick: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            if (!product.photoBase64.isNullOrBlank() || !product.imageKey.isNullOrBlank()) {
                MomentMediaView(
                    imageKey = product.imageKey,
                    photoBase64 = product.photoBase64,
                    contentDescription = product.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                )
            }
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = product.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${formatNgnWhole(product.priceNgn)} • ${formatUsdCents(product.priceUsdCents)}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (product.description.isNotBlank()) {
                    Text(
                        text = product.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (product.stockCount > 0L && product.isAvailable) "${product.stockCount} in stock"
                        else "Out of stock",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (product.stockCount > 0L) MatchEmerald else MaterialTheme.colorScheme.error
                    )
                    if (!isOwner) {
                        Button(
                            onClick = onBuyClick,
                            enabled = product.stockCount > 0L && product.isAvailable,
                            modifier = Modifier.testTag("buy_product_${product.id}")
                        ) {
                            Icon(Icons.Filled.ShoppingBag, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (product.stockCount > 0L) "Buy Now" else "Sold Out")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddProductSheet(
    defaultCategory: String,
    usdToNgnRate: Long,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onAdd: (
        title: String,
        description: String,
        category: String,
        priceUsdCents: Long,
        priceNgn: Long,
        stockCount: Long,
        imageKey: String?,
        photoBase64: String?
    ) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priceUsdStr by remember { mutableStateOf("5.00") }
    var priceNgnStr by remember { mutableStateOf((5L * usdToNgnRate).toString()) }
    var stockStr by remember { mutableStateOf("10") }
    var imageKey by remember { mutableStateOf<String?>("studio") }
    var photoBase64 by remember { mutableStateOf<String?>(null) }
    var isProcessingPhoto by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isProcessingPhoto = true
                encodeUriToCompactBase64(context, uri)?.let { photoBase64 = it; imageKey = null }
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
                Text("Add Product", style = MaterialTheme.typography.headlineMedium)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }
            OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= 100) title = it },
                label = { Text("Product Name") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("product_title_input")
            )
            OutlinedTextField(
                value = description,
                onValueChange = { if (it.length <= 500) description = it },
                label = { Text("Description & Delivery Details") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = priceNgnStr,
                    onValueChange = { input ->
                        priceNgnStr = input.filter { it.isDigit() }.take(8)
                    },
                    label = { Text("Price (₦ NGN)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = priceUsdStr,
                    onValueChange = { priceUsdStr = it.take(8) },
                    label = { Text("Price ($ USD)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            OutlinedTextField(
                value = stockStr,
                onValueChange = { stockStr = it.filter { c -> c.isDigit() }.take(5) },
                label = { Text("Available Stock Quantity") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedButton(
                onClick = {
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                enabled = !isProcessingPhoto,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (photoBase64 != null) "Product Photo Ready" else "Upload Product Photo")
            }

            val parsedUsdCents = ((priceUsdStr.toDoubleOrNull() ?: 5.0) * 100.0).toLong().coerceIn(50L, 100000L)
            val parsedNgn = (priceNgnStr.toLongOrNull() ?: (5L * usdToNgnRate)).coerceIn(100L, 10000000L)
            val parsedStock = (stockStr.toLongOrNull() ?: 1L).coerceIn(1L, 100000L)

            Button(
                onClick = {
                    onAdd(title, description, defaultCategory, parsedUsdCents, parsedNgn, parsedStock, imageKey, photoBase64)
                },
                enabled = !isSaving && !isProcessingPhoto && title.trim().length >= 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("publish_product_button")
            ) {
                Text("List Product")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductCheckoutSheet(
    shop: Shop,
    product: ShopProduct,
    usdToNgnRate: Long,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirmOrder: (quantity: Long, currency: String, deliveryNote: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var qty by remember { mutableIntStateOf(1) }
    var currency by remember { mutableStateOf(shop.currency) }
    var deliveryNote by remember { mutableStateOf("") }

    val maxQty = product.stockCount.coerceIn(1L, 20L).toInt()
    val unitCents = if (currency == "NGN") {
        ((product.priceNgn * 100L) / usdToNgnRate.coerceIn(100L, 10000L)).coerceIn(50L, 100000L)
    } else {
        product.priceUsdCents.coerceIn(50L, 100000L)
    }
    val totalCents = unitCents * qty.toLong()
    val ngnTotal = if (currency == "NGN") product.priceNgn * qty.toLong() else (totalCents * usdToNgnRate) / 100L
    val (sellerShare, adminFee) = computeShop8020Split(totalCents)

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
                Text("Checkout • ${product.title}", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close checkout")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("NGN" to "Pay in Naira (₦)", "USD" to "Pay in USD ($)").forEach { (code, label) ->
                    FilterChip(
                        selected = currency == code,
                        onClick = { currency = code },
                        label = { Text(label) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Quantity (max $maxQty)", style = MaterialTheme.typography.titleMedium)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = { if (qty > 1) qty -= 1 }, enabled = qty > 1) { Text("-") }
                    Text("$qty", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedButton(onClick = { if (qty < maxQty) qty += 1 }, enabled = qty < maxQty) { Text("+") }
                }
            }

            OutlinedTextField(
                value = deliveryNote,
                onValueChange = { if (it.length <= 240) deliveryNote = it },
                label = { Text("Delivery Address or Contact Note") },
                minLines = 2,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Text(
                text = "Order Total: ${formatNgnWhole(ngnTotal)} (${formatUsdCents(totalCents)})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "80% (${formatUsdCents(sellerShare)}) goes directly to ${shop.ownerName} • 20% (${formatUsdCents(adminFee)}) BINTAFACE marketplace fee",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = { onConfirmOrder(qty.toLong(), currency, deliveryNote) },
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("confirm_shop_order_button")
            ) {
                Text("Place Order • ${formatNgnWhole(ngnTotal)}")
            }
        }
    }
}
