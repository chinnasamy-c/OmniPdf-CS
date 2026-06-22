package com.example

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.theme.SleekLightBorder
import com.example.ui.theme.SleekPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoScrollPdfScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val uri by viewModel.autoScrollPdfSourceUri.collectAsStateWithLifecycle()

    var totalDurationMinutes by remember { mutableStateOf("1") }
    var totalDurationSeconds by remember { mutableStateOf("0") }
    var isScrolling by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(true) }

    var pdfPageCount by remember { mutableStateOf(0) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val currentPage by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex + 1
        }
    }

    LaunchedEffect(uri) {
        if (uri != null) {
            withContext(Dispatchers.IO) {
                try {
                    val pfd = context.contentResolver.openFileDescriptor(uri!!, "r")
                    if (pfd != null) {
                        val renderer = PdfRenderer(pfd)
                        pdfPageCount = renderer.pageCount
                        renderer.close()
                        pfd.close()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Auto-Scroll PDF", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.clearAutoScrollPdfFile()
                            viewModel.navigateTo("HOME")
                        }
                    ) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!showSettings && uri != null) {
                        IconButton(onClick = { showSettings = true; isScrolling = false }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = SleekPrimary)
                        }
                        IconButton(onClick = { isScrolling = !isScrolling }) {
                            Icon(
                                if (isScrolling) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isScrolling) "Pause" else "Play",
                                tint = SleekPrimary
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!showSettings && pdfPageCount > 0) {
                Column(horizontalAlignment = Alignment.End) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.padding(bottom = 8.dp).shadow(4.dp, RoundedCornerShape(20.dp))
                    ) {
                        Text(
                            text = "Page $currentPage / $pdfPageCount",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SmallFloatingActionButton(
                            onClick = {
                                coroutineScope.launch {
                                    val target = maxOf(0, currentPage - 2)
                                    listState.animateScrollToItem(target)
                                }
                            },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Previous Page")
                        }
                        SmallFloatingActionButton(
                            onClick = {
                                coroutineScope.launch {
                                    val target = minOf(pdfPageCount - 1, currentPage)
                                    listState.animateScrollToItem(target)
                                }
                            },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next Page")
                        }
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        if (uri == null) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("No PDF selected")
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (showSettings) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Timer Settings", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Set the total time to read the entire document. The PDF will scroll smoothly based on this timer.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            OutlinedTextField(
                                value = totalDurationMinutes,
                                onValueChange = { if (it.all { char -> char.isDigit() }) totalDurationMinutes = it },
                                label = { Text("Minutes") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = totalDurationSeconds,
                                onValueChange = { if (it.all { char -> char.isDigit() }) totalDurationSeconds = it },
                                label = { Text("Seconds") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                showSettings = false
                                isScrolling = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Start Reading", modifier = Modifier.padding(vertical = 6.dp))
                        }
                    }
                }
            }

            if (!showSettings && pdfPageCount > 0) {
                val density = LocalDensity.current
                val configuration = LocalConfiguration.current
                // A4 ratio is 1:1.414. We estimate the height of each item precisely so the total height is reliable.
                // horizontal padding = 8.dp * 2 = 16.dp
                // vertical padding = 8.dp per item (4 top, 4 bottom maybe)
                val itemWidthDp = configuration.screenWidthDp.dp - 16.dp
                val itemHeightDp = itemWidthDp * 1.414f
                val totalItemHeightPx = with(density) { (itemHeightDp + 16.dp).toPx() } // plus vertical padding

                val scrollPixelsPerSecond = remember(totalDurationMinutes, totalDurationSeconds, pdfPageCount) {
                    val m = totalDurationMinutes.toIntOrNull() ?: 0
                    val s = totalDurationSeconds.toIntOrNull() ?: 0
                    val totalSecs = (m * 60) + s
                    if (totalSecs <= 0) 10f else {
                        val estTotalPixels = pdfPageCount * totalItemHeightPx
                        estTotalPixels / totalSecs.toFloat()
                    }
                }

                LaunchedEffect(isScrolling) {
                    if (isScrolling) {
                        while (isActive) {
                            listState.animateScrollBy(
                                value = scrollPixelsPerSecond,
                                animationSpec = tween(durationMillis = 1000, easing = LinearEasing)
                            )
                        }
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(pdfPageCount) { pageIndex ->
                        var bitmap by remember { mutableStateOf<Bitmap?>(null) }

                        LaunchedEffect(context, uri, pageIndex) {
                            withContext(Dispatchers.IO) {
                                try {
                                    val newBmp = com.example.util.PdfUtils.renderPageToBitmap(context, uri!!, pageIndex, 1000, 1414)
                                    withContext(Dispatchers.Main) {
                                        val oldBmp = bitmap
                                        bitmap = newBmp
                                        if (oldBmp != null && oldBmp != newBmp) {
                                            oldBmp.recycle()
                                        }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }

                        DisposableEffect(Unit) {
                            onDispose {
                                bitmap?.recycle()
                                bitmap = null
                            }
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.707f) // Keep fixed aspect ratio to prevent jumpy scroll
                                .padding(vertical = 8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, SleekLightBorder, RoundedCornerShape(8.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap!!.asImageBitmap(),
                                    contentDescription = "Page ${pageIndex + 1}",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.FillWidth
                                )
                            } else {
                                Box(modifier = Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = SleekPrimary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
