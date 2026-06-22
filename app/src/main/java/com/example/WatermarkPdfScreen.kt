package com.example

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatermarkPdfScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val mode by viewModel.watermarkMode.collectAsStateWithLifecycle()
    val sourceUri by viewModel.watermarkSourceUri.collectAsStateWithLifecycle()
    val type by viewModel.watermarkType.collectAsStateWithLifecycle()
    val text by viewModel.watermarkText.collectAsStateWithLifecycle()
    val position by viewModel.watermarkPosition.collectAsStateWithLifecycle()
    val textSize by viewModel.watermarkTextSize.collectAsStateWithLifecycle()
    val font by viewModel.watermarkFont.collectAsStateWithLifecycle()
    val colorHex by viewModel.watermarkColor.collectAsStateWithLifecycle()
    val foreground by viewModel.watermarkForeground.collectAsStateWithLifecycle()
    val rotation by viewModel.watermarkRotation.collectAsStateWithLifecycle()
    val imageUri by viewModel.watermarkImageUri.collectAsStateWithLifecycle()
    val opacity by viewModel.watermarkOpacity.collectAsStateWithLifecycle()
    val startPage by viewModel.watermarkStartPage.collectAsStateWithLifecycle()
    val endPage by viewModel.watermarkEndPage.collectAsStateWithLifecycle()
    val cropEnabled by viewModel.watermarkCropEnable.collectAsStateWithLifecycle()
    val previewBmp by viewModel.watermarkPreviewBmp.collectAsStateWithLifecycle()
    val operationCompleted by viewModel.operationCompleted.collectAsStateWithLifecycle()
    val lastRecord by viewModel.lastGeneratedRecord.collectAsStateWithLifecycle()

    val pickPdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            viewModel.setWatermarkSourceFile(context, uri)
        }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            viewModel.updateWatermarkImage(uri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Watermark PDF", fontWeight = FontWeight.SemiBold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.clearWatermark()
                        viewModel.navigateTo("HOME")
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black
                )
            )
        },
        containerColor = Color(0xFFF5F5F5)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mode Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("ADD", "REMOVE").forEach { m ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (mode == m) Color(0xFF2E7D32) else Color.Transparent)
                            .clickable { viewModel.updateWatermarkMode(m) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = m,
                            color = if (mode == m) Color.White else Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            if (sourceUri == null) {
                Card(
                    onClick = { pickPdfLauncher.launch("application/pdf") },
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth().height(150.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Click to Upload PDF", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    }
                }
            } else if (operationCompleted && lastRecord != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Watermark Operation Successful!", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { 
                                val f = java.io.File(lastRecord!!.filePath)
                                viewModel.openFile(context, f) 
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                        ) {
                            Text("Open Processed PDF")
                        }
                    }
                }
            } else {
                if (previewBmp != null) {
                    androidx.compose.foundation.Image(
                        bitmap = previewBmp!!.asImageBitmap(),
                        contentDescription = "PDF Preview",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )
                }
                androidx.compose.material3.Text("PDF Selected for operation.", fontWeight = FontWeight.Bold)

                if (mode == "ADD") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.LightGray)
                            .padding(2.dp)
                    ) {
                        listOf("TEXT", "IMAGE").forEach { t ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (type == t) Color.White else Color.Transparent)
                                    .clickable { viewModel.updateWatermarkType(t) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(t, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }
                    }

                    if (type == "TEXT") {
                        OutlinedTextField(
                            value = text,
                            onValueChange = { viewModel.updateWatermarkText(it) },
                            label = { Text("Watermark Text") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = colorHex,
                                onValueChange = { viewModel.updateWatermarkColor(it) },
                                label = { Text("Color Hex") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        Text("Text Size: ${textSize.toInt()}pt", fontWeight = FontWeight.Bold)
                        Slider(
                            value = textSize,
                            onValueChange = { viewModel.updateWatermarkTextSize(it) },
                            valueRange = 10f..150f
                        )
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = font,
                                onValueChange = { viewModel.updateWatermarkFont(it) },
                                label = { Text("Custom Font Name (e.g. Helvetica)") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        Button(
                            onClick = { pickImageLauncher.launch("image/*") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                        ) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (imageUri != null) "Image Selected - Change" else "Upload Image")
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = cropEnabled,
                                onCheckedChange = { viewModel.updateWatermarkCropEnable(it) }
                            )
                            Text("Enable Crop / Scale Fit Bounds")
                        }
                    }

                    // Shared Params
                    Text("Opacity: ${(opacity * 100).toInt()}%", fontWeight = FontWeight.Bold)
                    Slider(
                        value = opacity,
                        onValueChange = { viewModel.updateWatermarkOpacity(it) },
                        valueRange = 0.05f..1f
                    )

                    Text("Rotation: ${rotation.toInt()}°", fontWeight = FontWeight.Bold)
                    Slider(
                        value = rotation,
                        onValueChange = { viewModel.updateWatermarkRotation(it) },
                        valueRange = 0f..360f
                    )
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = startPage.toString(),
                            onValueChange = { it.toIntOrNull()?.let { v -> viewModel.updateWatermarkStartPage(v) } },
                            label = { Text("Start Page") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = if (endPage == -1) "All" else endPage.toString(),
                            onValueChange = { if (it == "All" || it.isBlank()) viewModel.updateWatermarkEndPage(-1) else it.toIntOrNull()?.let { v -> viewModel.updateWatermarkEndPage(v) } },
                            label = { Text("End Page") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (type == "TEXT") {
                        Text("Text Size / Scale: ${textSize.toInt()}", fontWeight = FontWeight.Bold)
                        Slider(
                            value = textSize,
                            onValueChange = { viewModel.updateWatermarkTextSize(it) },
                            valueRange = 10f..200f
                        )
                    } else {
                         Text("Image Scale: ${textSize.toInt()}%", fontWeight = FontWeight.Bold)
                         Slider(
                             value = textSize,
                             onValueChange = { viewModel.updateWatermarkTextSize(it) },
                             valueRange = 10f..200f
                         )
                    }

                    Text("Position", fontWeight = FontWeight.Bold)
                    val positions = listOf("TL", "TC", "TR", "ML", "MC", "MR", "BL", "BC", "BR")
                    val names = listOf("Top Left", "Top Cent", "Top Right", "Mid Left", "Mid Cent", "Mid Right", "Bot Left", "Bot Cent", "Bot Right")
                    Column(modifier = Modifier.fillMaxWidth()) {
                        for (i in 0..2) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                for (j in 0..2) {
                                    val idx = i * 3 + j
                                    val p = positions[idx]
                                    val n = names[idx]
                                    FilterChip(
                                        selected = position == p,
                                        onClick = { viewModel.updateWatermarkPosition(p) },
                                        label = { Text(n, fontSize = 10.sp) }
                                    )
                                }
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = foreground,
                            onCheckedChange = { viewModel.updateWatermarkForeground(it) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (foreground) "Foreground (Over Content)" else "Background (Behind Content)")
                    }
                }

                Button(
                    onClick = { viewModel.applyWatermarkOperation(context) },
                    modifier = Modifier.fillMaxWidth().height(50.dp).padding(top = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Text(if (mode == "REMOVE") "Remove Watermark & Apply" else "Apply Watermark", fontSize = 16.sp)
                }
            }
        }
    }
}
