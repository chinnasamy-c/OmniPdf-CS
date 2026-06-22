package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.theme.SleekActiveBannerBg
import com.example.ui.theme.SleekLightBorder
import com.example.ui.theme.SleekLightSurface
import com.example.ui.theme.SleekLightTextSecondary
import com.example.ui.theme.SleekPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RotatePdfScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val pages by viewModel.organizePages.collectAsStateWithLifecycle()
    val operationCompleted by viewModel.operationCompleted.collectAsStateWithLifecycle()
    val lastGeneratedRecord by viewModel.lastGeneratedRecord.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rotate PDF", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.clearOrganize()
                            viewModel.navigateTo("HOME")
                        }
                    ) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.rotateAllPages(-90f) }) {
                        Icon(Icons.Default.RotateLeft, contentDescription = "Rotate Left All", tint = SleekPrimary)
                    }
                    IconButton(onClick = { viewModel.rotateAllPages(90f) }) {
                        Icon(Icons.Default.RotateRight, contentDescription = "Rotate Right All", tint = SleekPrimary)
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            if (pages.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No PDF selected")
                }
                return@Column
            }

            if (operationCompleted && lastGeneratedRecord != null) {
                val record = lastGeneratedRecord!!
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Rotation Complete!", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.openFile(context, java.io.File(record.filePath)) }) {
                        Text("Open Document")
                    }
                    Button(onClick = { viewModel.shareRecord(context, record) }) {
                        Text("Share Document")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(
                        onClick = {
                            viewModel.clearOrganize()
                            viewModel.navigateTo("HOME")
                        }
                    ) {
                        Text("Return Home")
                    }
                }
                return@Column
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SleekPrimary.copy(alpha = 0.1f))
                    .padding(12.dp)
            ) {
                Text(
                    text = "Tap any page to rotate it individually, or use the header buttons to rotate all pages. Click 'Apply' when done.",
                    fontSize = 12.sp,
                    color = SleekPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp, top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(pages, key = { _, item -> item.id }) { index, item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SleekLightBorder.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            .clickable { viewModel.rotatePage(item.id, 90f) },
                        colors = CardDefaults.cardColors(containerColor = SleekLightSurface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(SleekLightTextSecondary.copy(alpha = 0.1f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Page ${index + 1}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SleekLightTextSecondary
                                    )
                                }
                            }

                            // Thumbnail
                            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
                                PDFPageThumbnailImage(
                                    context = context,
                                    uri = item.sourceUri,
                                    pageIndex = item.originalPageIndex,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .rotate(item.rotationDegrees)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                // overlay rotate icon
                                Box(
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(Color.White.copy(alpha = 0.7f))
                                        .padding(8.dp).align(Alignment.Center)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.RotateRight,
                                        contentDescription = "Rotate",
                                        tint = SleekPrimary
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            Button(
                onClick = { viewModel.compileOrganizedPdf(context) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Apply Rotations & Export", modifier = Modifier.padding(vertical = 6.dp))
            }
        }
    }
}
