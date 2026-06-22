package com.example

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.theme.SleekLightBorder
import com.example.ui.theme.SleekLightSurface
import com.example.ui.theme.SleekLightText
import com.example.ui.theme.SleekLightTextSecondary

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format("%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestrictionRemoverScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val fileSource by viewModel.restrictionSourceFile.collectAsStateWithLifecycle()
    val restrictionInfo by viewModel.restrictionInfo.collectAsStateWithLifecycle()
    val operationCompleted by viewModel.operationCompleted.collectAsStateWithLifecycle()
    val lastGeneratedRecord by viewModel.lastGeneratedRecord.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Remove Restrictions", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.clearRestrictionSource()
                            viewModel.navigateTo("HOME")
                        }
                    ) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            if (fileSource == null) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No PDF selected", color = SleekLightTextSecondary)
                    }
                }
                return@LazyColumn
            }

            val source = fileSource!!

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = SleekLightSurface),
                    border = BorderStroke(1.dp, SleekLightBorder.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFE8EAF6)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.LockOpen, contentDescription = null, tint = Color(0xFF3F51B5))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = source.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = SleekLightText
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = formatSize(source.size),
                                    fontSize = 12.sp,
                                    color = SleekLightTextSecondary
                                )
                            }
                        }
                    }
                }
            }

            if (operationCompleted && lastGeneratedRecord != null) {
                val record = lastGeneratedRecord!!
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF388E3C), modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Restrictions Removed!", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1B5E20))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("This PDF is now fully unlocked. You can print, edit, copy, and sign freely.", fontSize = 12.sp, color = Color(0xFF2E7D32), textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.openFile(context, java.io.File(record.filePath)) },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1B5E20)),
                                    border = BorderStroke(1.dp, Color(0xFF1B5E20))
                                ) {
                                    Text("Open File", fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { viewModel.shareRecord(context, record) },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Share", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            } else if (restrictionInfo?.isEncrypted == true && restrictionInfo?.canOpenWithEmptyPassword == false) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("User Password Required", fontWeight = FontWeight.Bold, color = Color(0xFFB71C1C))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("This PDF requires a User Password to open, not just an Owner Password. Please use the 'Unlock PDF' tool to decrypt it first.", color = Color(0xFFC62828), fontSize = 13.sp)
                        }
                    }
                }
            } else {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SleekLightSurface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Ready to Remove Restrictions", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("This tool safely strips all owner restrictions (printing, copying, commenting, editing) while preserving the original layout and contents.", fontSize = 13.sp, color = SleekLightTextSecondary)
                        }
                    }
                }
                
                item {
                    Button(
                        onClick = { viewModel.removeRestrictions(context) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Enable Free Permissions", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
