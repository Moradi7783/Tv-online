package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AppDatabase
import com.example.data.Channel
import com.example.data.ChannelRepository
import com.example.ui.components.CustomVideoPlayer
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.ChannelViewModel
import com.example.viewmodel.ChannelViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize SQLite database and Repository pattern
        val database = AppDatabase.getDatabase(this)
        val repository = ChannelRepository(database.channelDao())
        val factory = ChannelViewModelFactory(repository)
        val viewModel = ViewModelProvider(this, factory)[ChannelViewModel::class.java]

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppMainScreen(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppMainScreen(viewModel: ChannelViewModel) {
    val activeChannel by viewModel.activeChannel.collectAsStateWithLifecycle()
    val channels by viewModel.channelsState.collectAsStateWithLifecycle()
    val categories by viewModel.categoriesState.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }

    // If a channel is selected for active live playback, we swap to full-screen video player layout
    if (activeChannel != null) {
        CustomVideoPlayer(
            channel = activeChannel!!,
            onClose = { viewModel.selectChannel(null) }
        )
    } else {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Tv,
                                contentDescription = "تله پلاس",
                                tint = Color(0xFF00F0FF),
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = "تله‌پلاس پخش‌زنده",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color(0xFF0F172A)
                    ),
                    actions = {
                        IconButton(
                            onClick = { showAddDialog = true }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "افزودن شبکه",
                                tint = Color(0xFF00F0FF)
                            )
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = Color(0xFF00F0FF),
                    contentColor = Color.Black,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = "افزودن")
                        Text("شبکه جدید", fontWeight = FontWeight.Bold)
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF0F172A), Color(0xFF090D16))
                        )
                    )
            ) {
                // Slogan/Welcome banner
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFF00F0FF).copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.SignalCellularAlt, contentDescription = "سرعت", tint = Color(0xFF00F0FF))
                        }
                        Column {
                            Text(
                                text = "موتور بهینه‌ساز بافر سنگین ⚡",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "اتصال خودکار به کیفیتهای مختلف جهت پخش روان با اینترنت ضعیف",
                                color = Color.LightGray,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = {
                        Text(
                            "جستجوی شبکه یا دسته بندی (مثلا ورزشی، فرانس)...",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "جستجو",
                            tint = Color.Gray
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(
                                    imageVector = Icons.Filled.Clear,
                                    contentDescription = "پاک کردن",
                                    tint = Color.Gray
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00F0FF),
                        unfocusedBorderColor = Color(0xFF1E293B),
                        focusedContainerColor = Color(0xFF111827),
                        unfocusedContainerColor = Color(0xFF111827)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    singleLine = true
                )

                // Horizontal list of categories
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { category ->
                        val isSelected = selectedCategory == category
                        val farsiCategory = when (category) {
                            "All" -> "همه شبکه‌ها 📺"
                            "Favorites" -> "لیست محبوب ⭐"
                            "Persian 🇮🇷 & News" -> "ماهواره فارسی 🇮🇷"
                            "Sports ⚽ & World Cup" -> "ورزشی و فوتبال ⚽"
                            "News 📰" -> "اخبار جهانی 📰"
                            "Entertainment & Science 🚀" -> "علمی و سرگرمی 🚀"
                            else -> category
                        }

                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectCategory(category) },
                            label = {
                                Text(
                                    text = farsiCategory,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF00F0FF),
                                selectedLabelColor = Color.Black,
                                containerColor = Color(0xFF1E293B),
                                labelColor = Color.White
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                selectedBorderColor = Color(0xFF00F0FF),
                                borderColor = Color.Transparent
                            )
                        )
                    }
                }

                // Header for Channels Section
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "شبکه‌های موجود (${channels.size})",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (selectedCategory == "Favorites" && channels.isEmpty()) {
                        Text(
                            text = "موردی ذخیره نشده",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                }

                // Vertical List of Available TV Channels
                if (channels.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.TvOff,
                                contentDescription = "خالی",
                                tint = Color.Gray,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "هیچ شبکه‌ای یافت نشد!",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "می‌توانید با دکمه بالا سمت راست یا FAB پایین، به طور دلخواه آدرس مانیتورینگ M3U8 را دستی اضافه کنید.",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(channels, key = { it.id }) { channel ->
                            ChannelListItem(
                                channel = channel,
                                onPlay = { viewModel.selectChannel(channel) },
                                onToggleFavorite = { viewModel.toggleFavorite(channel) },
                                onDelete = { viewModel.deleteChannel(channel) }
                            )
                        }
                    }
                }
            }

            // Beautiful custom material 3 dialog to add manually entered live screen streams
            if (showAddDialog) {
                AddChannelDialog(
                    onDismiss = { showAddDialog = false },
                    onConfirm = { name, url, cat ->
                        viewModel.addChannel(name, url, cat)
                        showAddDialog = false
                    }
                )
            }
        }
    }
}

@Composable
fun ChannelListItem(
    channel: Channel,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onPlay() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Icon + Channel info (Left/Center)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Category emblem
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF00F0FF).copy(alpha = 0.25f), Color.Transparent)
                            ),
                            shape = CircleShape
                        )
                        .border(1.dp, Color(0xFF00F0FF).copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    val icon = when (channel.category) {
                        "Persian 🇮🇷 & News" -> Icons.Filled.Tv
                        "Sports ⚽ & World Cup" -> Icons.Filled.SportsSoccer
                        "News 📰" -> Icons.Filled.Language
                        "Entertainment & Science 🚀" -> Icons.Filled.Science
                        else -> Icons.Filled.Link
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = "آیکون",
                        tint = Color(0xFF00F0FF),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    Text(
                        text = channel.name,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = channel.category,
                            color = Color(0xFF00F0FF),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .background(Color(0xFF00F0FF).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                        if (channel.isPreset) {
                            Text(
                                text = "سیستم ✔",
                                color = Color.LightGray,
                                fontSize = 10.sp,
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        } else {
                            Text(
                                text = "دستی ✨",
                                color = Color(0xFFFB923C),
                                fontSize = 10.sp,
                                modifier = Modifier
                                    .background(Color(0xFFFB923C).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = channel.url,
                        color = Color.Gray,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Controls: Favorite star, Play button, Delete manual
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Favorite Button
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (channel.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = "علاقه‌مندی",
                        tint = if (channel.isFavorite) Color(0xFFFB923C) else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Delete Custom Channel (Only if NOT preset)
                if (!channel.isPreset) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "حذف کانال دستی",
                            tint = Color.Red.copy(alpha = 0.8f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Round Play overlay
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFF00F0FF), CircleShape)
                        .clickable { onPlay() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "پخش",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddChannelDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, url: String, category: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Manual Streams 🔗") }
    var errorText by remember { mutableStateOf("") }

    val presetCategories = listOf(
        "Persian 🇮🇷 & News",
        "Sports ⚽ & World Cup",
        "News 📰",
        "Entertainment & Science 🚀",
        "Manual Streams 🔗"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "افزودن شبکه تلویزیونی جدید 📺",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "مشخصات شبکه زنده را با دقت وارد کنید (ترجیحا لینک با پسوند .m3u8 یا .mp4 باشد)",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )

                // Channel Name input
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (it.isNotEmpty()) errorText = ""
                    },
                    label = { Text("نام شبکه تلویزیونی/ماهواره") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00F0FF),
                        unfocusedBorderColor = Color.Gray
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // URL input
                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        url = it
                        if (it.isNotEmpty()) errorText = ""
                    },
                    label = { Text("لینک استریم پخش آنلاین (M3U8 / MP4)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00F0FF),
                        unfocusedBorderColor = Color.Gray
                    ),
                    placeholder = { Text("http://example.com/live.m3u8", fontSize = 11.sp, color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Category options
                Text(
                    text = "دسته بندی شبکه:",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(presetCategories) { cat ->
                        val isSel = selectedCategory == cat
                        val catLabel = when (cat) {
                            "Persian 🇮🇷 & News" -> "ایرانی/فارسی"
                            "Sports ⚽ & World Cup" -> "ورزشی"
                            "News 📰" -> "اخباری"
                            "Entertainment & Science 🚀" -> "علمی"
                            else -> "متفرقه ⛓"
                        }
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isSel) Color(0xFF00F0FF) else Color(0xFF1E293B),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = catLabel,
                                color = if (isSel) Color.Black else Color.White,
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                if (errorText.isNotEmpty()) {
                    Text(
                        text = errorText,
                        color = Color.Red,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.trim().isEmpty()) {
                        errorText = "لطفا نام شبکه را وارد کنید!"
                    } else if (url.trim().isEmpty() || (!url.trim().startsWith("http://", ignoreCase = true) && !url.trim().startsWith("https://", ignoreCase = true))) {
                        errorText = "لطفا یک لینک استریم معتبر با http یا https وارد کنید!"
                    } else {
                        onConfirm(name, url, selectedCategory)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF))
            ) {
                Text("افزودن شبکه", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text("انصراف")
            }
        },
        containerColor = Color(0xFF131B2E),
        shape = RoundedCornerShape(24.dp)
    )
}
