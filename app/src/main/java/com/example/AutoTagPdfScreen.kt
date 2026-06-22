package com.example

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoTagPdfScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val sourceUri by viewModel.autoTagSourceUri.collectAsStateWithLifecycle()
    val nodes by viewModel.autoTagNodes.collectAsStateWithLifecycle()
    val isCompleted by viewModel.operationCompleted.collectAsStateWithLifecycle()

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.setAutoTagSource(uri)
            viewModel.analyzeAutoTagPdf(context)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Auto-Tagging PDF", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo("HOME") }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            if (sourceUri == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Style, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Select a PDF to analyze its structure.")
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { filePicker.launch("application/pdf") }) {
                            Text("Select PDF")
                        }
                    }
                }
            } else if (nodes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No tags found or analyzing...")
                }
            } else {
                Text("Structure Preview", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(nodes) { node ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val color = when {
                                node.type.startsWith("Heading") -> Color(0xFFE3F2FD)
                                node.type == "List Item" -> Color(0xFFF1F8E9)
                                else -> Color(0xFFFAFAFA)
                            }
                            Box(
                                modifier = Modifier
                                    .background(color, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(node.type, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(node.content, modifier = Modifier.weight(1f), maxLines = 2, fontSize = 14.sp)
                            Text("Page ${node.pageNumber}", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.saveAutoTagPdf(context) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Save to PDF (with Bookmarks)")
                }
            }
            
            if (isCompleted) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.navigateTo("HOME") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Go Home")
                }
            }
        }
    }
}
