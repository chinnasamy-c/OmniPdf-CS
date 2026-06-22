package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.Canvas

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StampBadgeScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var pdfUri by remember { mutableStateOf<Uri?>(null) }
    var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var fileDescriptor by remember { mutableStateOf<ParcelFileDescriptor?>(null) }
    var pageCount by remember { mutableStateOf(0) }
    
    // Pages rendered as bitmaps
    val pageBitmaps = remember { mutableStateMapOf<Int, Bitmap>() }
    
    val presets = listOf("Approved", "Confidential", "Draft", "Verified", "Rejected", "Okay", "Trusted", "Avoid", "Certified", "Wanted", "Patented", "Final", "Official", "Terminated", "CUSTOM")
    var selectedPreset by remember { mutableStateOf(presets[0]) }
    var customImageUri by remember { mutableStateOf<Uri?>(null) }

    val pageStamps = viewModel.stampData
    val globalStampSize by viewModel.globalStampSize.collectAsStateWithLifecycle()

    val pickPdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            pdfUri = uri
            coroutineScope.launch {
                try {
                    fileDescriptor?.close()
                    pdfRenderer?.close()
                    val fd = context.contentResolver.openFileDescriptor(uri, "r")
                    if (fd != null) {
                        fileDescriptor = fd
                        pdfRenderer = PdfRenderer(fd)
                        pageCount = pdfRenderer?.pageCount ?: 0
                        pageBitmaps.clear()
                        viewModel.clearStamps()
                        
                        // Preheat first few pages
                        withContext(Dispatchers.IO) {
                            for (i in 0 until minOf(pageCount, 3)) {
                                renderPage(pdfRenderer!!, i)?.let { bmp ->
                                    pageBitmaps[i] = bmp
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            customImageUri = uri
            selectedPreset = "CUSTOM"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stamp Badge") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo("HOME") }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (pdfUri != null) {
                        TextButton(onClick = {
                            viewModel.processStampsOnPdf(context, pdfUri!!)
                        }) {
                            Text("Save", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (pdfUri == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        onClick = { pickPdfLauncher.launch("application/pdf") },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.LocalOffer, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Upload PDF to Add Stamps", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            } else {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(presets) { preset ->
                        val isSelected = selectedPreset == preset
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (preset == "CUSTOM") {
                                    pickImageLauncher.launch("image/*")
                                } else {
                                    selectedPreset = preset
                                }
                            },
                            label = { Text(preset) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }

                if (selectedPreset == "CUSTOM" && customImageUri != null) {
                    Text("Custom Image Selected", modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }
                
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = "Stamp Size Modifer: ${(globalStampSize * 100).toInt()}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Slider(
                        value = globalStampSize,
                        onValueChange = { viewModel.globalStampSize.value = it },
                        valueRange = 0.3f..3.0f,
                        steps = 54
                    )
                }
                Text(
                    "Tap on document to place stamp. Use two fingers to scroll. Tap stamp to remove.",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.DarkGray)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        for (i in 0 until pageCount) {
                            LaunchedEffect(i) {
                                if (pageBitmaps[i] == null) {
                                    withContext(Dispatchers.IO) {
                                        renderPage(pdfRenderer!!, i)?.let { bmp ->
                                            pageBitmaps[i] = bmp
                                        }
                                    }
                                }
                            }

                            val bmp = pageBitmaps[i]
                            if (bmp != null) {
                                Box(modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight()
                                ) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = "Page ${i + 1}",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .pointerInput(selectedPreset, customImageUri) {
                                                detectTapGestures(
                                                    onTap = { offset ->
                                                        val width = size.width
                                                        val height = size.height
                                                        val rx = offset.x / width.toFloat()
                                                        val ry = offset.y / height.toFloat()
                                                        
                                                        viewModel.addStamp(i, selectedPreset, customImageUri, rx, ry, globalStampSize)
                                                    }
                                                )
                                            },
                                        contentScale = ContentScale.FillWidth
                                    )
                                    
                                    val stampsForPage = pageStamps[i] ?: emptyList()
                                    BoxWithConstraints(modifier = Modifier.matchParentSize()) {
                                        val w = maxWidth
                                        val h = maxHeight
                                        stampsForPage.forEachIndexed { idx, stamp ->
                                            Box(
                                                modifier = Modifier
                                                    .offset(
                                                        x = (w.value * stamp.x).dp - (75.dp * stamp.size), 
                                                        y = (h.value * stamp.y).dp - (75.dp * stamp.size)
                                                    )
                                                    .size(150.dp * stamp.size)
                                                    .clickable {
                                                        viewModel.removeStamp(i, idx)
                                                    }
                                            ) {
                                                if (stamp.text == "CUSTOM" && stamp.customImageUri != null) {
                                                    coil.compose.AsyncImage(
                                                        model = stamp.customImageUri,
                                                        contentDescription = null,
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Fit
                                                    )
                                                } else {
                                                    ProfessionalStamp(text = stamp.text)
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                Box(modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(0.7f)
                                    .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun renderPage(renderer: PdfRenderer, pageIndex: Int): Bitmap? {
    try {
        val page = renderer.openPage(pageIndex)
        val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.WHITE)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()
        return bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}

@Composable
fun ProfessionalStamp(text: String) {
    val bitmap = remember(text) {
        com.example.util.StampGenerator.generateRealisticStamp(text).asImageBitmap()
    }
    androidx.compose.foundation.Image(
        bitmap = bitmap,
        contentDescription = "Stamp",
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Fit
    )
}
