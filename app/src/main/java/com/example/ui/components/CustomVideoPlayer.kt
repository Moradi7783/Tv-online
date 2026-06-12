package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.ui.PlayerView
import com.example.data.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class Quality(val label: String) {
    AUTO("کیفیت خودکار (Auto)"),
    MEDIUM_480("کیفیت متوسط (480p)"),
    LOW_240("کیفیت ضعیف (240p)")
}

@OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun CustomVideoPlayer(
    channel: Channel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val coroutineScope = rememberCoroutineScope()

    // Screen states
    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isControlsVisible by remember { mutableStateOf(true) }
    var isFullscreen by remember { mutableStateOf(false) }

    // Selected Quality
    var currentQuality by remember { mutableStateOf(Quality.AUTO) }

    // Brightness and Volume Swipe Overlays
    var activeGestureVolume by remember { mutableStateOf<Float?>(null) } // 0f to 1f
    var activeGestureBrightness by remember { mutableStateOf<Float?>(null) } // 0f to 1f
    var dismissGestureJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    // Audio manager
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat() }

    // Configure ExoPlayer with heavy buffering for weak connections
    val player = remember(channel.url) {
        ExoPlayer.Builder(context)
            .build().apply {
                playWhenReady = true

                // Construct HLS/M3U8 Source dynamically if applicable
                val dataSourceFactory = DefaultHttpDataSource.Factory()
                    .setAllowCrossProtocolRedirects(true)
                    .setUserAgent("exoplayer_iptv_streamer")
                    .setConnectTimeoutMs(15000)
                    .setReadTimeoutMs(15000)

                val mediaItem = MediaItem.fromUri(channel.url)
                val mediaSource = if (channel.url.contains(".m3u8", ignoreCase = true)) {
                    HlsMediaSource.Factory(dataSourceFactory)
                        .setAllowChunklessPreparation(true)
                        .createMediaSource(mediaItem)
                } else {
                    // Default generic media source
                    null
                }

                if (mediaSource != null) {
                    setMediaSource(mediaSource)
                } else {
                    setMediaItem(mediaItem)
                }

                prepare()
            }
    }

    // Toggle Landscape Fullscreen on state change
    LaunchedEffect(isFullscreen) {
        if (activity != null) {
            if (isFullscreen) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                // Hide system UI
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    activity.window.insetsController?.let { controller ->
                        controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                        controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    }
                } else {
                    @Suppress("DEPRECATION")
                    activity.window.decorView.systemUiVisibility = (
                            View.SYSTEM_UI_FLAG_FULLSCREEN
                                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            )
                }
            } else {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER
                // Show system UI
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    activity.window.insetsController?.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                } else {
                    @Suppress("DEPRECATION")
                    activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                }
            }
        }
    }

    // Apply track quality limitation when currentQuality changes
    LaunchedEffect(currentQuality) {
        val parametersBuilder = player.trackSelectionParameters.buildUpon()
        when (currentQuality) {
            Quality.AUTO -> {
                // Clear any video dimension limits
                parametersBuilder.setMaxVideoSize(Int.MAX_VALUE, Int.MAX_VALUE)
            }
            Quality.MEDIUM_480 -> {
                parametersBuilder.setMaxVideoSize(854, 480)
            }
            Quality.LOW_240 -> {
                parametersBuilder.setMaxVideoSize(426, 240)
            }
        }
        player.trackSelectionParameters = parametersBuilder.build()
    }

    // Auto fade controls after 5 seconds
    LaunchedEffect(isControlsVisible) {
        if (isControlsVisible) {
            delay(5000)
            isControlsVisible = false
        }
    }

    // Listener
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isPlaying = player.playWhenReady
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    errorMessage = null
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                errorMessage = when (error.errorCode) {
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> {
                        "خطای شبکه: اتصال اینترنت ضعیف است و یا قطع شده است 🌐"
                    }
                    else -> "خطا در بارگذاری شبکه. لطفا لینک پخش را بررسی کنید ⚠️"
                }
                isBuffering = false
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
            // Reset screen orientation on exits
            if (activity != null) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    activity.window.insetsController?.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                } else {
                    @Suppress("DEPRECATION")
                    activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        // Show controls briefly on touch start
                        isControlsVisible = true
                    },
                    onDrag = { change, dragAmount ->
                        // Measure width and height of the gesture region
                        val widthPx = size.width.toFloat()
                        val isLeftSide = change.position.x < (widthPx / 2f)

                        dismissGestureJob?.cancel()

                        if (isLeftSide) {
                            // Adjust Brightness
                            val layoutParams = activity?.window?.attributes
                            var currentBrightness = layoutParams?.screenBrightness ?: 0.5f
                            if (currentBrightness < 0f) currentBrightness = 0.5f // system auto default

                            // Invert dragAmount.y (negative is swipe up, positive is swipe down)
                            val changeRatio = -dragAmount.y / 800f
                            val nextBrightness = (currentBrightness + changeRatio).coerceIn(0.01f, 1f)

                            layoutParams?.screenBrightness = nextBrightness
                            activity?.window?.attributes = layoutParams
                            activeGestureBrightness = nextBrightness
                        } else {
                            // Adjust Volume
                            val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                            val changeValue = -dragAmount.y / 200f
                            val maxVolIndex = maxVolume.toInt()

                            val step = (changeValue * maxVolIndex).toInt()
                            if (step != 0) {
                                val nextVolIndex = (currentVol + step).coerceIn(0, maxVolIndex)
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, nextVolIndex, 0)
                                activeGestureVolume = nextVolIndex.toFloat() / maxVolume
                            }
                        }
                    },
                    onDragEnd = {
                        dismissGestureJob = coroutineScope.launch {
                            delay(1200)
                            activeGestureBrightness = null
                            activeGestureVolume = null
                        }
                    }
                )
            }
    ) {
        // Android PlayerView
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = false // Custom Compose UI controller for ultra luxury look
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { playerView ->
                playerView.player = player
            }
        )

        // Custom Overlay for Left/Right swipe gestures HUD
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Brightness Overlay HUD (Left)
                AnimatedVisibility(
                    visible = activeGestureBrightness != null,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LightMode,
                            contentDescription = "روشنایی",
                            tint = Color(0xFFFB923C),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${((activeGestureBrightness ?: 1f) * 100).toInt()}%",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                // Volume Overlay HUD (Right)
                AnimatedVisibility(
                    visible = activeGestureVolume != null,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        val isMuted = (activeGestureVolume ?: 0f) == 0f
                        Icon(
                            imageVector = if (isMuted) Icons.Filled.VolumeMute else Icons.Filled.VolumeUp,
                            contentDescription = "صدا",
                            tint = Color(0xFF00F0FF),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${((activeGestureVolume ?: 0f) * 100).toInt()}%",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // Tap to toggle controls visibility
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { isControlsVisible = !isControlsVisible }
        )

        // Buffering Dialog
        if (isBuffering && errorMessage == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = Color(0xFF00F0FF),
                        strokeWidth = 4.dp,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "درحال اتصال و پر کردن بافر کیفیت بالا... 🔄",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // Error message view with restart capability
        if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2C))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ErrorOutline,
                            contentDescription = "قرمز",
                            tint = Color.Red,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = errorMessage ?: "خطای ناشناخته رخ داده است",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "لینک پخش: ${channel.url}",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Button(
                                onClick = {
                                    errorMessage = null
                                    isBuffering = true
                                    player.seekTo(0)
                                    player.prepare()
                                    player.play()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF))
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = "تلاش مجدد")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("تلاش مجدد", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = onClose,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) {
                                Text("انصراف / خروج")
                            }
                        }
                    }
                }
            }
        }

        // Beautiful cinematic overlays for Back buttons, Title, Control center and Bottom slider
        AnimatedVisibility(
            visible = isControlsVisible,
            enter = fadeIn(animationSpec = tween(300)) + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut(animationSpec = tween(300)) + slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.background(Color.White.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "خروج", tint = Color.White)
                    }
                    Column {
                        Text(
                            text = channel.name,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "شبکه پخش آنلاین • دسته بندی ${channel.category}",
                            color = Color(0xFF00F0FF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Light
                        )
                    }
                }

                // Header right: Adaptive Badge for quality indicator
                Box(
                    modifier = Modifier
                        .background(Color(0xFF00F0FF).copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = when (currentQuality) {
                            Quality.AUTO -> "کیفیت هوشمند (Auto)"
                            Quality.MEDIUM_480 -> "480p"
                            Quality.LOW_240 -> "240p • ضعیف"
                        },
                        color = Color(0xFF00F0FF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Central Play / Pause Controller
        AnimatedVisibility(
            visible = isControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(32.dp))
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                // Return 5 sec button
                IconButton(onClick = { player.seekTo((player.currentPosition - 10000).coerceAtLeast(0)) }) {
                    Icon(
                        imageVector = Icons.Filled.Replay10,
                        contentDescription = "سازده ثانیه عقب",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Play / Pause button
                IconButton(
                    onClick = {
                        if (player.isPlaying) {
                            player.pause()
                        } else {
                            player.play()
                        }
                    },
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color(0xFF00F0FF), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "مکث",
                        tint = Color.Black,
                        modifier = Modifier.size(40.dp)
                    )
                }

                // Forward 5 sec button
                IconButton(onClick = { player.seekTo(player.currentPosition + 10000) }) {
                    Icon(
                        imageVector = Icons.Filled.Forward10,
                        contentDescription = "ده ثانیه جلو",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        // Bottom Controls Overlay
        AnimatedVisibility(
            visible = isControlsVisible,
            enter = fadeIn(animationSpec = tween(300)) + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut(animationSpec = tween(300)) + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left element: Web connection quality setting
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(
                            onClick = {
                                // Toggle buffer to LOW/MEDIUM/AUTO to adapt with weak internet
                                currentQuality = when (currentQuality) {
                                    Quality.AUTO -> Quality.MEDIUM_480
                                    Quality.MEDIUM_480 -> Quality.LOW_240
                                    Quality.LOW_240 -> Quality.AUTO
                                }
                            },
                            modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.NetworkCheck,
                                contentDescription = "تنظیم کیفیت با کیفیت اینترنت",
                                tint = Color(0xFF00F0FF)
                            )
                        }

                        Column {
                            Text(
                                text = "حالت سرعت اینترنت شما:",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                            Text(
                                text = currentQuality.label,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Right element: Refresh and Screen fullscreen toggler
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                // Re-prepare stream to clear buffer
                                errorMessage = null
                                isBuffering = true
                                player.seekTo(0)
                                player.prepare()
                                player.play()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Color.White.copy(alpha = 0.3f)))
                        ) {
                            Icon(Icons.Filled.Speed, contentDescription = "بهینه سازی", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("رفع فریز / بوفه", fontSize = 11.sp)
                        }

                        IconButton(
                            onClick = { isFullscreen = !isFullscreen },
                            modifier = Modifier.background(Color(0xFF00F0FF), CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                                contentDescription = "تمام صفحه تلویزیون",
                                tint = Color.Black
                            )
                        }
                    }
                }
            }
        }
    }
}
