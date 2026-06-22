package com.example

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.HistoryRecord
import com.example.ui.MainViewModel
import com.example.ui.MainViewModelFactory
import com.example.ui.theme.*
import com.example.util.PdfUtils
import com.example.util.SelectableFile
import com.example.util.OrganizePageItem
import com.example.util.PdfOverlay
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.Image
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val context = LocalContext.current
                val app = context.applicationContext as Application
                val viewModel: MainViewModel = viewModel(
                    factory = MainViewModelFactory(app)
                )

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ShopAppContent(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun ShopAppContent(viewModel: MainViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()
    val processStatus by viewModel.processStatus.collectAsStateWithLifecycle()

    BackHandler(enabled = currentScreen != "HOME") {
        when (currentScreen) {
            "MERGE" -> viewModel.clearMergeFiles()
            "SPLIT" -> viewModel.clearSplitFile()
            "PASSWORD_TOOLS" -> viewModel.clearPasswordSource()
            "WATERMARK_PDF" -> viewModel.clearWatermark()
            "ORGANIZE" -> viewModel.clearOrganize()
            "PROTECT" -> viewModel.clearProtectFile()
            "INVERT" -> viewModel.clearInvert()
            "SIGN" -> viewModel.clearSignatureMaker()
            "FILTER" -> viewModel.clearFilterPdf()
            "STAMP_BADGE" -> viewModel.clearStamps()
            "RESIZE" -> viewModel.clearResizeFile()
            "PAGENUMBER" -> viewModel.clearPageNumberSource()
            "CONVERT_PDF" -> viewModel.clearConvertState()
            "CONVERT_ALL_TOOLS" -> viewModel.clearConvertState()
            "REPAIR" -> viewModel.clearRepairFile()
            "PAGES_PER_SHEET" -> viewModel.clearPpsFile()
            "METADATA_REMOVER" -> viewModel.clearMetadataFile()
            "AUTO_SCROLL_PDF" -> viewModel.clearAutoScrollPdfFile()
            "RESTRICTION_REMOVER" -> viewModel.clearRestrictionSource()
            "RESUME_MAKER" -> {} // Placeholder for clear logic when implemented
            "DRAW_PDF" -> {}
            "AUTO_TAG_PDF" -> viewModel.clearAutoTagPdf()
        }
        viewModel.navigateTo("HOME")
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (currentScreen) {
            "HOME" -> HomeScreen(viewModel = viewModel)
            "MERGE" -> MergeFilesScreen(viewModel = viewModel)
            "SPLIT" -> SplitFileScreen(viewModel = viewModel)
            "PASSWORD_TOOLS" -> PasswordToolsScreen(viewModel = viewModel)
            "WATERMARK_PDF" -> WatermarkPdfScreen(viewModel = viewModel)
            "ORGANIZE" -> OrganizeFilesScreen(viewModel = viewModel)
            "PROTECT" -> ProtectFileScreen(viewModel = viewModel)
            "INVERT" -> InvertColorScreen(viewModel = viewModel)
            "SIGN" -> SignatureMakerScreen(viewModel = viewModel)
            "FILTER" -> FilterPdfScreen(viewModel = viewModel)
            "RESIZE" -> ResizePdfScreen(viewModel = viewModel)
            "PAGENUMBER" -> PageNumberScreen(viewModel = viewModel)
            "ROTATE_PDF" -> RotatePdfScreen(viewModel = viewModel)
            "CONVERT_PDF" -> ConvertPdfScreen(viewModel = viewModel)
            "CONVERT_ALL_TOOLS" -> ConvertAllToolsScreen(viewModel = viewModel)
            "REPAIR" -> RepairPdfScreen(viewModel = viewModel)
            "STAMP_BADGE" -> StampBadgeScreen(viewModel = viewModel)
            "PAGES_PER_SHEET" -> PagesPerSheetScreen(viewModel = viewModel)
            "METADATA_REMOVER" -> MetadataRemoverScreen(viewModel = viewModel)
            "AUTO_SCROLL_PDF" -> AutoScrollPdfScreen(viewModel = viewModel)
            "RESTRICTION_REMOVER" -> RestrictionRemoverScreen(viewModel = viewModel)
            "RESUME_MAKER" -> ResumeMakerScreen(viewModel = viewModel)
            "DRAW_PDF" -> DrawPdfScreen(viewModel = viewModel)
            "AUTO_TAG_PDF" -> AutoTagPdfScreen(viewModel = viewModel)
        }

        if (isProcessing) {
            ScannerProcessingOverlay(statusText = processStatus)
        }
    }
}

// ==================== 1. HOME SCREEN ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        val pm = context.getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (!pm.isIgnoringBatteryOptimizations(context.packageName)) {
                try {
                    val intent = android.content.Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    intent.data = android.net.Uri.parse("package:${context.packageName}")
                    context.startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    val historyList by viewModel.allHistory.collectAsStateWithLifecycle()

    val pickMergeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addFilesForMerge(context, uris)
            viewModel.navigateTo("MERGE")
        }
    }

    val pickSplitLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.setSplitSourceFile(context, uri)
            viewModel.navigateTo("SPLIT")
        }
    }

    val pickPasswordFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.setPasswordSourceFile(context, uri)
            viewModel.navigateTo("PASSWORD_TOOLS")
        }
    }

    val pickRestrictionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.setRestrictionSourceFile(context, uri)
            viewModel.navigateTo("RESTRICTION_REMOVER")
        }
    }

    val pickWatermarkLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.setWatermarkSourceFile(context, uri)
            viewModel.navigateTo("WATERMARK_PDF")
        }
    }

    val pickOrganizeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.setOrganizeSourceFile(context, uri)
            viewModel.navigateTo("ORGANIZE")
        }
    }

    val pickProtectLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.setProtectSourceFile(context, uri)
            viewModel.navigateTo("PROTECT")
        }
    }

    val pickInvertLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.setInvertSourceFile(context, uri)
            viewModel.navigateTo("INVERT")
        }
    }

    val pickFilterPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.setFilterSourceFile(context, uri)
            viewModel.navigateTo("FILTER")
        }
    }

    val pickResizePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.setResizeSourceFile(context, uri)
            viewModel.navigateTo("RESIZE")
        }
    }

    val pickRotatePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.setRotatePdfSourceFile(context, uri)
            viewModel.navigateTo("ROTATE_PDF")
        }
    }

    val pickAutoScrollPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.setAutoScrollPdfSourceFile(context, uri)
            viewModel.navigateTo("AUTO_SCROLL_PDF")
        }
    }

    val pickPagesPerSheetLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.setPpsSourceFile(context, uri)
            viewModel.navigateTo("PAGES_PER_SHEET")
        }
    }

    val pickPageNumberLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.setPageNumberSourceFile(context, uri)
            viewModel.navigateTo("PAGENUMBER")
        }
    }

    val pickRepairLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.setRepairSourceFile(context, uri)
            viewModel.navigateTo("REPAIR")
        }
    }

    val pickMetadataLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.setMetadataSourceFile(context, uri)
            viewModel.navigateTo("METADATA_REMOVER")
        }
    }



    Scaffold(
        topBar = {
            val isDark = androidx.compose.foundation.isSystemInDarkTheme()
            val textColor = if (isDark) com.example.ui.theme.TextPrimaryDark else com.example.ui.theme.TextPrimaryLight
            val textSecColor = if (isDark) androidx.compose.ui.graphics.Color(0xFF94A3B8) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(id = R.string.app_name),
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Zero-Cloud Client Document Engine",
                        fontSize = 11.sp,
                        color = textSecColor,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // Omni PDF CS app logo icon
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) androidx.compose.ui.graphics.Color(0xFF1E293B) else androidx.compose.ui.graphics.Color(0xFF1E293B)),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = "Omni PDF Logo",
                        modifier = Modifier.size(56.dp)
                    )
                }
            }
        },

        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "PDF MANIPULATION TOOLS",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                // Dual-column manipulation grid reorganized into a Grid View
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    // Row 1
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ToolCard(
                            title = "Merge PDF",
                            description = "Join PDFs & images",
                            icon = Icons.Default.MergeType,
                            containerColor = SleekMergeContainer,
                            contentColor = SleekMergeOnContainer,
                            modifier = Modifier.weight(1f).testTag("merge_pdf_tool_card"),
                            compact = true,
                            onClick = { pickMergeLauncher.launch("*/*") }
                        )
                        ToolCard(
                            title = "Split PDF",
                            description = "Extract page ranges",
                            icon = Icons.Default.CallSplit,
                            containerColor = SleekSplitContainer,
                            contentColor = SleekSplitOnContainer,
                            modifier = Modifier.weight(1f).testTag("split_pdf_tool_card"),
                            compact = true,
                            onClick = { pickSplitLauncher.launch("application/pdf") }
                        )
                        ToolCard(
                            title = "Delete & Reorder",
                            description = "Delete and drag pages",
                            icon = Icons.Default.SettingsOverscan,
                            containerColor = SleekMergeContainer,
                            contentColor = SleekMergeOnContainer,
                            modifier = Modifier.weight(1f).testTag("delete_reorder_tool_card"),
                            compact = true,
                            onClick = { pickOrganizeLauncher.launch("application/pdf") }
                        )
                    }

                    // Row 2
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ToolCard(
                            title = "Rotate PDF",
                            description = "Turn pages side",
                            icon = Icons.Default.RotateRight,
                            containerColor = SleekMergeContainer,
                            contentColor = SleekMergeOnContainer,
                            modifier = Modifier.weight(1f).testTag("rotate_pdf_tool_card"),
                            compact = true,
                            onClick = { pickRotatePdfLauncher.launch("application/pdf") }
                        )
                        ToolCard(
                            title = "Resize PDF",
                            description = "Compress & Enlarge",
                            icon = Icons.Default.AspectRatio,
                            containerColor = SleekResizeContainer,
                            contentColor = SleekResizeOnContainer,
                            modifier = Modifier.weight(1f).testTag("resize_pdf_tool_card"),
                            compact = true,
                            onClick = { pickResizePdfLauncher.launch("application/pdf") }
                        )
                        ToolCard(
                            title = "Pages per Sheet",
                            description = "Configurable layout grid",
                            icon = Icons.Default.Layers,
                            containerColor = SleekPagesPerSheetContainer,
                            contentColor = SleekPagesPerSheetOnContainer,
                            modifier = Modifier.weight(1f).testTag("pages_per_sheet_tool_card"),
                            compact = true,
                            onClick = { pickPagesPerSheetLauncher.launch("application/pdf") }
                        )
                    }

                    // Row 3
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ToolCard(
                            title = "Color Inverted",
                            description = "Comfort Dark Mode",
                            icon = Icons.Default.InvertColors,
                            containerColor = SleekInvertContainer,
                            contentColor = SleekInvertOnContainer,
                            modifier = Modifier.weight(1f).testTag("invert_pdf_tool_card"),
                            compact = true,
                            onClick = { pickInvertLauncher.launch("*/*") }
                        )
                        ToolCard(
                            title = "Filter PDF",
                            description = "Curves & shadows",
                            icon = Icons.Default.AutoFixHigh,
                            containerColor = SleekFilterContainer,
                            contentColor = SleekFilterOnContainer,
                            modifier = Modifier.weight(1f).testTag("filter_pdf_tool_card"),
                            compact = true,
                            onClick = { pickFilterPdfLauncher.launch("application/pdf") }
                        )
                        ToolCard(
                            title = "Add Page Numbering",
                            description = "Visual numbering",
                            icon = Icons.Default.FormatListNumbered,
                            containerColor = SleekSignatureContainer,
                            contentColor = SleekSignatureOnContainer,
                            modifier = Modifier.weight(1f).testTag("pagenumber_pdf_tool_card"),
                            compact = true,
                            onClick = { pickPageNumberLauncher.launch("application/pdf") }
                        )
                    }

                    // Row 4
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ToolCard(
                            title = "Resume Maker",
                            description = "Create Professional Resumes",
                            icon = Icons.Default.Article,
                            containerColor = SleekSigValidatorContainer,
                            contentColor = SleekSigValidatorOnContainer,
                            modifier = Modifier.weight(1f).testTag("resume_maker_tool_card"),
                            compact = true,
                            onClick = { viewModel.navigateTo("RESUME_MAKER") }
                        )
                        ToolCard(
                            title = "Auto-Scroll PDF",
                            description = "Read with timer",
                            icon = Icons.Default.VerticalAlignBottom,
                            containerColor = SleekOrganizeContainer,
                            contentColor = SleekOrganizeOnContainer,
                            modifier = Modifier.weight(1f).testTag("auto_scroll_pdf_tool_card"),
                            compact = true,
                            onClick = { pickAutoScrollPdfLauncher.launch("application/pdf") }
                        )
                        ToolCard(
                            title = "Draw on PDF",
                            description = "Sketch visually",
                            icon = Icons.Default.Brush,
                            containerColor = SleekFilterContainer,
                            contentColor = SleekFilterOnContainer,
                            modifier = Modifier.weight(1f).testTag("draw_pdf_tool_card"),
                            compact = true,
                            onClick = { viewModel.navigateTo("DRAW_PDF") }
                        )
                    }

                    // Row 5
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ToolCard(
                            title = "Repair PDF",
                            description = "Fix corrupt files",
                            icon = Icons.Default.Build,
                            containerColor = SleekRepairContainer,
                            contentColor = SleekRepairOnContainer,
                            modifier = Modifier.weight(1f).testTag("repair_pdf_tool_card"),
                            compact = true,
                            onClick = { pickRepairLauncher.launch("*/*") }
                        )
                        ToolCard(
                            title = "Auto-Tag PDF",
                            description = "Extract headers & paragraphs",
                            icon = Icons.Default.AutoAwesome,
                            containerColor = SleekUnlockContainer,
                            contentColor = SleekUnlockOnContainer,
                            modifier = Modifier.weight(1f).testTag("auto_tag_pdf_tool_card"),
                            compact = true,
                            onClick = { viewModel.navigateTo("AUTO_TAG_PDF") }
                        )
                        ToolCard(
                            title = "Stamp Badge",
                            description = "Add professional stamps",
                            icon = Icons.Default.Verified,
                            containerColor = SleekFilterContainer,
                            contentColor = SleekFilterOnContainer,
                            modifier = Modifier.weight(1f).testTag("stamp_badge_tool_card"),
                            compact = true,
                            onClick = { viewModel.navigateTo("STAMP_BADGE") }
                        )
                    }

                    // Row 6
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ToolCard(
                            title = "Signature Maker",
                            description = "Sign visual PDF",
                            icon = Icons.Default.Create,
                            containerColor = SleekSignatureContainer,
                            contentColor = SleekSignatureOnContainer,
                            modifier = Modifier.weight(1f).testTag("signature_pdf_tool_card"),
                            compact = true,
                            onClick = { viewModel.navigateTo("SIGN") }
                        )
                        ToolCard(
                            title = "Watermark PDF",
                            description = "Add/Remove marks",
                            icon = Icons.Default.AutoAwesomeMosaic,
                            containerColor = SleekOrganizeContainer,
                            contentColor = SleekOrganizeOnContainer,
                            modifier = Modifier.weight(1f).testTag("watermark_pdf_tool_card"),
                            compact = true,
                            onClick = { pickWatermarkLauncher.launch("application/pdf") }
                        )
                        ToolCard(
                            title = "Lock PDF",
                            description = "Set password",
                            icon = Icons.Default.Lock,
                            containerColor = SleekMultipageContainer,
                            contentColor = SleekMultipageOnContainer,
                            modifier = Modifier.weight(1f).testTag("protect_pdf_tool_card"),
                            compact = true,
                            onClick = { pickProtectLauncher.launch("application/pdf") }
                        )
                    }

                    // Row 7
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ToolCard(
                            title = "Unlock PDF",
                            description = "Remove password",
                            icon = Icons.Default.LockOpen,
                            containerColor = SleekUnlockContainer,
                            contentColor = SleekUnlockOnContainer,
                            modifier = Modifier.weight(1f).testTag("password_pdf_tool_card"),
                            compact = true,
                            onClick = { pickPasswordFileLauncher.launch("application/pdf") }
                        )
                        ToolCard(
                            title = "Remove Restrictions",
                            description = "Enable print, copy, edit",
                            icon = Icons.Default.LockOpen,
                            containerColor = SleekUnlockContainer,
                            contentColor = SleekUnlockOnContainer,
                            modifier = Modifier.weight(1f).testTag("restriction_remover_tool_card"),
                            compact = true,
                            onClick = { pickRestrictionLauncher.launch("application/pdf") }
                        )
                        ToolCard(
                            title = "Metadata Remover",
                            description = "Strip hidden properties",
                            icon = Icons.Default.VisibilityOff,
                            containerColor = SleekMetadataContainer,
                            contentColor = SleekMetadataOnContainer,
                            modifier = Modifier.weight(1f).testTag("metadata_remover_tool_card"),
                            compact = true,
                            onClick = { pickMetadataLauncher.launch("application/pdf") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsTabContent(viewModel: MainViewModel) {
    val context = LocalContext.current
    val historyList by viewModel.allHistory.collectAsStateWithLifecycle()
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Stats Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "TERMINAL PROFILE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Xerox Station #04", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
                            Text("Operational state", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                        }
                        
                        // Status badge (Online)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFE8F5E9))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF4CAF50))
                            )
                            Text("ONLINE", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        // Specs List
        item {
            Text(
                text = "UTILITY CAPABILITIES",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SettingDetailRow(
                        icon = Icons.Default.PictureAsPdf,
                        label = "Core Engine",
                        value = "PdfRenderer (SDK 35)"
                    )
                    HorizontalDivider(color = androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.5f).copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))
                    SettingDetailRow(
                        icon = Icons.Default.Storage,
                        label = "Local Desk Cache",
                        value = "Offline Room SQLite"
                    )
                    HorizontalDivider(color = androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.5f).copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))
                    SettingDetailRow(
                        icon = Icons.Default.Speed,
                        label = "Operation Speed",
                        value = "High (Coroutines)"
                    )
                }
            }
        }

        // Maintenance Action Card
        item {
            Text(
                text = "MAINTENANCE UTILITIES",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Clean Local Storage",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Wipe all conversion logs, records, and cached PDF files permanently from the disk workspace.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = {
                            if (historyList.isNotEmpty()) {
                                viewModel.clearAllHistory()
                                Toast.makeText(context, "Storage space cleared successfully!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Nothing to clear", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("CLEAR HISTORY DESK", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
            Text(text = label, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground)
        }
        Text(text = value, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
    }
}

@Composable
fun LaserHeaderDashboardBanner() {
    val infiniteTransition = rememberInfiniteTransition()
    val laserOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(SleekActiveBannerBg)
            .border(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                RoundedCornerShape(24.dp)
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rounded background icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(SleekPrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Print,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "ACTIVE SESSION",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Ready for new task",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // Draw laser scanning animation bar
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(4.dp)
                .align(Alignment.CenterStart)
                .offset(x = (laserOffset * 300).dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            SleekPrimary,
                            SleekPrimary,
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

@ExperimentalMaterial3Api
@Composable
fun ToolCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color, // Ignored now in favor of Spec
    contentColor: Color,   // Ignored now in favor of Spec
    modifier: Modifier = Modifier,
    compact: Boolean = true,
    onClick: () -> Unit
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val surfaceColor = if (isDark) Color(0xFF1E293B) else Color(0xFFFFFFFF)
    val textColor = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val accentColor = if (isDark) Color(0xFF3B82F6) else Color(0xFF2563EB)
    val borderColor = if (isDark) Color(0xFF334155) else Color.Transparent
    val elevation = if (isDark) 0.dp else 4.dp

    Card(
        onClick = onClick,
        modifier = modifier
            .height(if (compact) 115.dp else 135.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        border = if (isDark) androidx.compose.foundation.BorderStroke(1.dp, borderColor) else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (compact) 10.dp else 20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(if (compact) 32.dp else 44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accentColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(if (compact) 18.dp else 24.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    fontSize = if (compact) 13.sp else 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = description,
                    fontSize = if (compact) 9.sp else 11.sp,
                    lineHeight = if (compact) 11.sp else 14.sp,
                    color = textColor.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun HistoryRecordRow(
    record: HistoryRecord,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val formattedDate = remember(record.createdAt) {
        try {
            val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
            sdf.format(Date(record.createdAt))
        } catch (e: Exception) {
            "Just now"
        }
    }

    val formattedSize = remember(record.fileSize) {
        val kb = record.fileSize / 1024.0
        val mb = kb / 1024.0
        if (mb >= 1.0) {
            String.format(Locale.getDefault(), "%.1f MB", mb)
        } else {
            String.format(Locale.getDefault(), "%.0f KB", kb)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon identifier
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                          when (record.toolType) {
                            "MERGE" -> SleekMergeContainer
                            "SPLIT" -> SleekSplitContainer
                            "UNLOCK" -> SleekUnlockContainer
                            "ORGANIZE" -> SleekOrganizeContainer
                            "MULTIPAGE" -> SleekMultipageContainer
                            "INVERT" -> SleekInvertContainer
                            "RESIZE" -> SleekResizeContainer
                            "PAGENUMBER" -> SleekSignatureContainer
                            "PAGES_PER_SHEET" -> SleekPagesPerSheetContainer
                            "METADATA_REMOVER" -> SleekMetadataContainer
                            "AUTO_SCROLL_PDF" -> SleekOrganizeContainer
                            "RESUME_MAKER" -> SleekSigValidatorContainer
                            "STAMP_BADGE" -> SleekFilterContainer
                            else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (record.toolType) {
                        "MERGE" -> Icons.Default.MergeType
                        "SPLIT" -> Icons.Default.CallSplit
                        "UNLOCK" -> Icons.Default.LockOpen
                        "ORGANIZE" -> Icons.Default.AutoAwesomeMosaic
                        "MULTIPAGE" -> Icons.Default.AutoAwesomeMosaic
                        "INVERT" -> Icons.Default.InvertColors
                        "RESIZE" -> Icons.Default.AspectRatio
                        "PAGENUMBER" -> Icons.Default.FormatListNumbered
                        "PAGES_PER_SHEET" -> Icons.Default.Layers
                        "METADATA_REMOVER" -> Icons.Default.VisibilityOff
                        "AUTO_SCROLL_PDF" -> Icons.Default.VerticalAlignBottom
                        "RESUME_MAKER" -> Icons.Default.Article
                        "STAMP_BADGE" -> Icons.Default.Verified
                        else -> Icons.Default.PictureAsPdf
                    },
                    contentDescription = null,
                    tint = when (record.toolType) {
                        "MERGE" -> SleekMergeOnContainer
                        "SPLIT" -> SleekSplitOnContainer
                        "UNLOCK" -> SleekUnlockOnContainer
                        "ORGANIZE" -> SleekOrganizeOnContainer
                        "MULTIPAGE" -> SleekMultipageOnContainer
                        "INVERT" -> SleekInvertOnContainer
                        "RESIZE" -> SleekResizeOnContainer
                        "PAGENUMBER" -> SleekSignatureOnContainer
                        "PAGES_PER_SHEET" -> SleekPagesPerSheetOnContainer
                        "METADATA_REMOVER" -> SleekMetadataOnContainer
                        "AUTO_SCROLL_PDF" -> SleekOrganizeOnContainer
                        "RESUME_MAKER" -> SleekSigValidatorOnContainer
                        "STAMP_BADGE" -> SleekFilterOnContainer
                        else -> MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Body Detail
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = record.fileName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${record.pagesCount} " + if (record.pagesCount == 1) "page" else "pages",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = formattedSize,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = formattedDate,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // End Actions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onShare,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}


// ==================== 2. MERGE FILES SCREEN ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MergeFilesScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val mergeFiles by viewModel.selectedMergeFiles.collectAsStateWithLifecycle()
    val operationCompleted by viewModel.operationCompleted.collectAsStateWithLifecycle()
    val lastGeneratedRecord by viewModel.lastGeneratedRecord.collectAsStateWithLifecycle()

    val pickMergeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addFilesForMerge(context, uris)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Merge PDF & Images", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.clearMergeFiles()
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            if (operationCompleted && lastGeneratedRecord != null) {
                // Layout showing completed results
                MergeCompletedPanel(
                    record = lastGeneratedRecord!!,
                    onOpen = { viewModel.openFile(context, File(lastGeneratedRecord!!.filePath)) },
                    onShare = { viewModel.shareRecord(context, lastGeneratedRecord!!) },
                    onDone = {
                        viewModel.clearMergeFiles()
                        viewModel.navigateTo("HOME")
                    }
                )
            } else {
                // Upload trigger button styled as the Sleek Dashed Drop Zone
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .clickable { pickMergeLauncher.launch("*/*") }
                        .drawBehind {
                            val stroke = Stroke(
                                width = 1.6f.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f), 0f)
                            )
                            drawRoundRect(
                                color = androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.5f),
                                style = stroke,
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(28.dp.toPx())
                            )
                        }
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                        .testTag("upload_files_trigger"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Add Documents / Images",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Supports PDF files and photo/scanned graphics",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f).copy(alpha = 0.7f)
                        )
                    }
                }

                Text(
                    text = "ARRANGE EXTRACTION ORDER (${mergeFiles.size} of items)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                if (mergeFiles.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No files loaded. Click upload to append first item",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("selected_file_list"),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(mergeFiles, key = { _, v -> v.id }) { index, file ->
                            MergeFileRowItem(
                                index = index,
                                totalSize = mergeFiles.size,
                                file = file,
                                onMoveUp = { viewModel.moveFileUpInMerge(index) },
                                onMoveDown = { viewModel.moveFileDownInMerge(index) },
                                onRemove = { viewModel.removeFileFromMerge(file.id) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.mergeSelectedFiles(context) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .padding(bottom = 8.dp)
                            .testTag("merge_execution_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SleekPrimary
                        ),
                        shape = RoundedCornerShape(24.dp),
                        enabled = mergeFiles.isNotEmpty()
                    ) {
                        Icon(imageVector = Icons.Default.MergeType, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("MERGE PDF & AUTO OPEN", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun MergeFileRowItem(
    index: Int,
    totalSize: Int,
    file: SelectableFile,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit
) {
    val formattedSize = remember(file.size) {
        val kb = file.size / 1024.0
        val mb = kb / 1024.0
        if (mb >= 1.0) {
            String.format(Locale.getDefault(), "%.1f MB", mb)
        } else {
            String.format(Locale.getDefault(), "%.0f KB", kb)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Visual dynamic rendering thumbnail
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.03f))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f),
                        RoundedCornerShape(6.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (file.isPdf) {
                    PdfFallbackThumb(fileName = file.name, pages = file.pageCount)
                } else {
                    AsyncImage(
                        model = file.uri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Badge(
                        containerColor = if (file.isPdf) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = if (file.isPdf) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                    ) {
                        Text(if (file.isPdf) "PDF" else "IMG", fontSize = 9.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formattedSize,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            // Move actions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = onMoveUp,
                    enabled = index > 0,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "Move Up",
                        tint = if (index > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onMoveDown,
                    enabled = index < totalSize - 1,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = "Move Down",
                        tint = if (index < totalSize - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PdfFallbackThumb(fileName: String, pages: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Icon(
            imageVector = Icons.Default.PictureAsPdf,
            contentDescription = null,
            tint = Color(0xFFE53935),
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = "$pages p",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun MergeCompletedPanel(
    record: HistoryRecord,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDone: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4CAF50).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "TASK COMPLETED!",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF4CAF50),
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Your output file was compiled and saved properly on the terminal desk.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            HorizontalDivider(color = androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.5f).copy(alpha = 0.6f))

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = record.fileName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(170.dp)
                    )
                    Text(
                        text = "${record.pagesCount} Compiled pages",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }

                Button(
                    onClick = onOpen,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SleekPrimary
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Auto Open", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onShare,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share Output")
                }

                Button(
                    onClick = onDone,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SleekMergeContainer,
                        contentColor = SleekMergeOnContainer
                    )
                ) {
                    Text("Finish", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}


// ==================== 3. SPLIT FILE SCREEN ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitFileScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val splitSrcFile by viewModel.splitSourceFile.collectAsStateWithLifecycle()
    val manualSelectedPages by viewModel.splitManualSelectedPages.collectAsStateWithLifecycle()
    val rangeExpression by viewModel.splitRangeExpression.collectAsStateWithLifecycle()
    val operationCompleted by viewModel.operationCompleted.collectAsStateWithLifecycle()
    val lastGeneratedRecord by viewModel.lastGeneratedRecord.collectAsStateWithLifecycle()

    var isManualMode by remember { mutableStateOf(true) }

    val pickSplitLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.setSplitSourceFile(context, uri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Split PDF File", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.clearSplitFile()
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            if (operationCompleted && lastGeneratedRecord != null) {
                MergeCompletedPanel(
                    record = lastGeneratedRecord!!,
                    onOpen = { viewModel.openFile(context, File(lastGeneratedRecord!!.filePath)) },
                    onShare = { viewModel.shareRecord(context, lastGeneratedRecord!!) },
                    onDone = {
                        viewModel.clearSplitFile()
                        viewModel.navigateTo("HOME")
                    }
                )
            } else {
                if (splitSrcFile == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                  .fillMaxWidth()
                                  .height(180.dp)
                                  .clip(RoundedCornerShape(28.dp))
                                  .clickable { pickSplitLauncher.launch("application/pdf") }
                                  .drawBehind {
                                      val stroke = Stroke(
                                          width = 1.6f.dp.toPx(),
                                          pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f), 0f)
                                      )
                                      drawRoundRect(
                                          color = androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.5f),
                                          style = stroke,
                                          cornerRadius = androidx.compose.ui.geometry.CornerRadius(28.dp.toPx())
                                      )
                                  }
                                  .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                                  .testTag("select_pdf_source_zone"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Select PDF Source File",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "Extract pages manually or using strict boundaries",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f).copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    val source = splitSrcFile!!

                    // Current File description bar
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = Color(0xFFE53935),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = source.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Contains total ${source.pageCount} pages",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                )
                            }
                            OutlinedButton(
                                onClick = { pickSplitLauncher.launch("application/pdf") },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Text("Change", fontSize = 11.sp)
                            }
                        }
                    }

                    // Tabs select Manual or Extract page range with rounded custom layout
                    TabRow(
                        selectedTabIndex = if (isManualMode) 0 else 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        containerColor = Color.Transparent,
                        divider = {}
                    ) {
                        Tab(
                            selected = isManualMode,
                            onClick = { isManualMode = true },
                            text = { Text("Manual Selection", fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = !isManualMode,
                            onClick = { isManualMode = false },
                            text = { Text("Extract Pages Range", fontWeight = FontWeight.Bold) }
                        )
                    }

                    if (isManualMode) {
                        // Manual grid list showing visual rendering preview pages
                        ManualSplitLayout(
                            uri = source.uri,
                            pageCount = source.pageCount,
                            selectedPages = manualSelectedPages,
                            onToggleSelection = { page -> viewModel.toggleSplitPageSelection(page) },
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { viewModel.splitSourcePdf(context, useManualSelection = true) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .padding(bottom = 8.dp)
                                .testTag("split_manual_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SleekPrimary
                            ),
                            shape = RoundedCornerShape(24.dp),
                            enabled = manualSelectedPages.isNotEmpty()
                        ) {
                            Icon(imageVector = Icons.Default.CallSplit, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "SPLIT CHOSEN PAGES (${manualSelectedPages.size})",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        // Extract Pages Mode range text box input
                        ExpressSplitLayout(
                            maxPages = source.pageCount,
                            expression = rangeExpression,
                            onExpressionChange = { viewModel.updateSplitRangeExpression(it) },
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        val extractedList = remember(rangeExpression) {
                            PdfUtils.parsePageExpression(rangeExpression, source.pageCount)
                        }

                        Button(
                            onClick = { viewModel.splitSourcePdf(context, useManualSelection = false) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .padding(bottom = 8.dp)
                                .testTag("split_range_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SleekPrimary
                            ),
                            shape = RoundedCornerShape(24.dp),
                            enabled = extractedList.isNotEmpty()
                        ) {
                            Icon(imageVector = Icons.Default.CallSplit, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "SPLIT EXTRACTED RANGE (${extractedList.size} p)",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ManualSplitLayout(
    uri: Uri,
    pageCount: Int,
    selectedPages: Set<Int>,
    onToggleSelection: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "TAP CHANNELS / PAGES TO CHOOSE AND SPLIT",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            modifier = Modifier.padding(bottom = 6.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxSize()
                .testTag("manual_page_grid"),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(pageCount) { index ->
                val pageNum = index + 1
                val isSelected = selectedPages.contains(pageNum)

                PdfPageThumbnailCard(
                    uri = uri,
                    pageIndex = index,
                    isSelected = isSelected,
                    onClick = { onToggleSelection(pageNum) }
                )
            }
        }
    }
}

@Composable
fun PdfPageThumbnailCard(
    uri: Uri,
    pageIndex: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    var bitmapState by remember { mutableStateOf<Bitmap?>(null) }
    var hasError by remember { mutableStateOf(false) }

    // Safely asynchronously load first-look of page on IO scope to prevent lag
    LaunchedEffect(uri, pageIndex) {
        withContext(Dispatchers.IO) {
            try {
                val oldBmp = bitmapState
                val bmp = PdfUtils.renderPageToBitmap(context, uri, pageIndex, 150, 210)
                withContext(Dispatchers.Main) {
                    if (bmp != null) {
                        bitmapState = bmp
                        if (oldBmp != null && oldBmp != bmp) {
                            oldBmp.recycle()
                        }
                    } else {
                        hasError = true
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    hasError = true
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            bitmapState?.recycle()
        }
    }

    Card(
        modifier = Modifier
            .aspectRatio(0.72f)
            .clickable { onClick() }
            .border(
                border = if (isSelected) {
                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                } else {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
                },
                shape = RoundedCornerShape(10.dp)
            ),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (bitmapState != null) {
                AsyncImage(
                    model = bitmapState,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (hasError) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.BrokenImage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                }
            }

            // Top overlay badge
            Box(
                modifier = Modifier
                    .padding(6.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(
                            alpha = 0.8f
                        )
                    )
                    .align(Alignment.TopEnd)
                    .border(
                        1.dp,
                        if (isSelected) Color.Transparent else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(12.dp)
                    )
                } else {
                    Text(
                        text = "${pageIndex + 1}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun ExpressSplitLayout(
    maxPages: Int,
    expression: String,
    onExpressionChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val extractedList = remember(expression, maxPages) {
        PdfUtils.parsePageExpression(expression, maxPages)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
    ) {
        Text(
            text = "ENTER MANUAL RANGE VALUE",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            modifier = Modifier.padding(bottom = 6.dp)
        )

        OutlinedTextField(
            value = expression,
            onValueChange = onExpressionChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("split_range_input"),
            placeholder = { Text("Example format: 1-4, 6, 8-10", fontSize = 14.sp) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)
            ),
            trailingIcon = {
                if (expression.isNotEmpty()) {
                    IconButton(onClick = { onExpressionChange("") }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "SYNTAX SUGGESTIONS",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.height(10.dp))
                
                SuggestionBullet(boldLead = "1-4", desc = "extracts pages 1, 2, 3, 4")
                SuggestionBullet(boldLead = "10-1", desc = "reverse range: extracts pages in reverse from 10 down to 1")
                SuggestionBullet(boldLead = "1,1,1", desc = "duplicates: generates 3 identical copies of page 1")
                SuggestionBullet(boldLead = "2,1,6,9,2", desc = "rearrange sequence: outputs pages in precisely typed order")

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.04f))
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "PARSED OUTPUT RESULTS PREVIEW:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (extractedList.isEmpty()) {
                    Text(
                        text = "No pages matched. Enter a range to build split sequence.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Extracting ${extractedList.size} pages: ${extractedList.joinToString(", ")}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SuggestionBullet(boldLead: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = boldLead,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = desc,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}


// ==================== 4. PROCESSING OVERLAY ====================
@Composable
fun ScannerProcessingOverlay(statusText: String) {
    val infiniteTransition = rememberInfiniteTransition()
    val laserYOffset by infiniteTransition.animateFloat(
        initialValue = -50f,
        targetValue = 50f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Dialog(onDismissRequest = {}) {
        Card(
            modifier = Modifier
                .width(280.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f))
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DocumentScanner,
                        contentDescription = "Scanning",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )

                    // Draw green glowing laser sweep bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .align(Alignment.Center)
                            .offset(y = laserYOffset.dp)
                            .background(MaterialTheme.colorScheme.primary)
                            .border(1.dp, Color(0xFF00FF7F).copy(alpha = 0.5f))
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.5.dp,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = statusText,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Compressing pages locally. Please do not close the applet.",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}


// ==================== 5. PDF PASSWORD TOOLS SCREEN ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordToolsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val fileSource by viewModel.passwordSourceFile.collectAsStateWithLifecycle()
    val isProtected by viewModel.isPasswordProtected.collectAsStateWithLifecycle()
    val recoveredPassword by viewModel.recoveredPassword.collectAsStateWithLifecycle()
    val operationCompleted by viewModel.operationCompleted.collectAsStateWithLifecycle()
    val lastGeneratedRecord by viewModel.lastGeneratedRecord.collectAsStateWithLifecycle()

    var manualPasswordInput by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Unlock PDF", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.clearPasswordSource()
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
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f).copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No PDF selected",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    }
                }
                return@LazyColumn
            }

            val source = fileSource!!

            // Section 1: File Specs Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isProtected) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isProtected) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = null,
                                    tint = if (isProtected) Color(0xFFD32F2F) else Color(0xFF388E3C),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = source.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = formatSize(source.size),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                )
                            }
                        }

                        Divider(
                            modifier = Modifier.padding(vertical = 16.dp),
                            color = androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.5f).copy(alpha = 0.3f)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isProtected) Color(0xFFFFCDD2) else Color(0xFFC8E6C9)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isProtected) "PASSWORD ENCRYPTED" else "UNPROTECTED",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isProtected) Color(0xFFB71C1C) else Color(0xFF1B5E20)
                                )
                            }

                            Text(
                                text = if (isProtected) "Requires password key to unlock" else "No password restrictions",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Case A: File is already unprotected
            if (!isProtected) {
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
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF388E3C),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Already Fully Unlocked!",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFF1B5E20)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "This PDF file does not have a password. Since no password exists, no password details are shown.",
                                fontSize = 12.sp,
                                color = Color(0xFF2E7D32),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.clearPasswordSource()
                                        viewModel.navigateTo("HOME")
                                    },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1B5E20)),
                                    border = BorderStroke(1.dp, Color(0xFF1B5E20))
                                ) {
                                    Text("Home", fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = {
                                        viewModel.shareOriginalFile(context, source.uri, source.name)
                                    },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Share PDF", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                return@LazyColumn
            }

            // Case B: Unlocked successfully
            if (operationCompleted && lastGeneratedRecord != null) {
                val record = lastGeneratedRecord!!
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SleekUnlockContainer),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.5.dp, SleekUnlockOnContainer.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.LockOpen,
                                contentDescription = "Unlocked",
                                tint = SleekUnlockOnContainer,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "PDF FILE UNLOCKED!",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = SleekUnlockOnContainer,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Password security has been successfully removed.",
                                fontSize = 12.sp,
                                color = SleekUnlockOnContainer.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Password key display
                            if (recoveredPassword != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(SleekDarkCanvas)
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "ENTERED DECRYPTION PASSWORD",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SleekDarkTextSecondary,
                                            letterSpacing = 1.sp
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = recoveredPassword!!,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color(0xFF00FF7F),
                                            letterSpacing = 1.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Copy and Share Password row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                            val clip = android.content.ClipData.newPlainText("PDF Password", recoveredPassword)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, "Password copied to clipboard", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1f).height(46.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SleekUnlockOnContainer),
                                        border = BorderStroke(1.dp, SleekUnlockOnContainer.copy(alpha = 0.5f))
                                    ) {
                                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Copy Key", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(android.content.Intent.EXTRA_TEXT, "PDF Passkey: ${recoveredPassword}")
                                            }
                                            context.startActivity(android.content.Intent.createChooser(intent, "Share Password Key"))
                                        },
                                        modifier = Modifier.weight(1f).height(46.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SleekUnlockOnContainer),
                                        border = BorderStroke(1.dp, SleekUnlockOnContainer.copy(alpha = 0.5f))
                                    ) {
                                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Share Key", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Divider(
                                    modifier = Modifier.padding(vertical = 16.dp),
                                    color = SleekUnlockOnContainer.copy(alpha = 0.15f)
                                )
                            }

                            // View file / Share decrypted document
                            Button(
                                onClick = {
                                    viewModel.openFile(context, File(record.filePath))
                                },
                                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("view_unlocked_pdf_btn"),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SleekUnlockOnContainer)
                            ) {
                                Icon(imageVector = Icons.Default.Launch, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Open Decrypted PDF File", fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedButton(
                                onClick = {
                                    viewModel.shareRecord(context, record)
                                },
                                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("share_unlocked_pdf_btn"),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = SleekUnlockOnContainer),
                                border = BorderStroke(1.5.dp, SleekUnlockOnContainer)
                            ) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Share Unlocked Document", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                return@LazyColumn
            }

            // Case C: Active selection logic
            item {
                Text(
                    text = "DECRYPTION & SHARING METHODS",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = SleekPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Enter Password to Unlock",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Please enter the password to decrypt the document. This will permanently remove the password requirement so you can open it without passcode entry.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        val visualTransformation = if (isPasswordVisible) {
                            androidx.compose.ui.text.input.VisualTransformation.None
                        } else {
                            androidx.compose.ui.text.input.PasswordVisualTransformation()
                        }

                        OutlinedTextField(
                            value = manualPasswordInput,
                            onValueChange = { manualPasswordInput = it },
                            modifier = Modifier.fillMaxWidth().testTag("manual_password_input_field"),
                            label = { Text("Standard PDF Passkey") },
                            placeholder = { Text("Enter user/owner password") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            visualTransformation = visualTransformation,
                            trailingIcon = {
                                val visibilityIcon = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(imageVector = visibilityIcon, contentDescription = "Toggle Passkey Eye")
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (manualPasswordInput.isNotBlank()) {
                                    viewModel.decryptAndSaveFile(context, manualPasswordInput)
                                } else {
                                    Toast.makeText(context, "Please enter a passcode", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("manual_decrypt_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                            enabled = manualPasswordInput.isNotBlank()
                        ) {
                            Icon(imageVector = Icons.Default.LockOpen, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Decrypt & Remove Password", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Method 2: Share Locked PDF (to request password)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = Color(0xFF00796B),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Request Password",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Don't have the password? You can share this encrypted/locked PDF file directly to others to ask for the correct opening passkey.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                viewModel.shareOriginalFile(context, source.uri, source.name)
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("share_locked_pdf_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00796B))
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Share Locked PDF", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, 2)
    return String.format("%.2f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

// ==================== 6. PDF PAGE ORGANIZER SCREEN ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizeFilesScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val pages by viewModel.organizePages.collectAsStateWithLifecycle()
    val operationCompleted by viewModel.operationCompleted.collectAsStateWithLifecycle()
    val lastGeneratedRecord by viewModel.lastGeneratedRecord.collectAsStateWithLifecycle()

    var activeSelectedIndex by remember { mutableStateOf<Int?>(null) }

    val gridState = rememberLazyGridState()
    val currentPages by rememberUpdatedState(pages)
    val currentViewModel by rememberUpdatedState(viewModel)

    var dragId by remember { mutableStateOf<String?>(null) }
    var dragStartIndex by remember { mutableStateOf<Int?>(null) }
    var currentDragIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    val appendOrganizeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addFilesToOrganize(context, uris)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Delete & Reorder Pages", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
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
                    IconButton(
                        onClick = {
                            appendOrganizeLauncher.launch("application/pdf")
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.NoteAdd, contentDescription = "Add Files", tint = SleekPrimary)
                            Text("Add PDF", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SleekPrimary)
                        }
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
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesomeMosaic,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f).copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No PDF selected",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                viewModel.navigateTo("HOME")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary)
                        ) {
                            Text("Choose PDF from Home", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                return@Column
            }

            // Case A: Finished compiling successfully!
            if (operationCompleted && lastGeneratedRecord != null) {
                val record = lastGeneratedRecord!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SleekOrganizeContainer),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.5.dp, SleekOrganizeOnContainer.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Success",
                                tint = SleekOrganizeOnContainer,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "PDF REORGANIZED SUCCESSFULLY!",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = SleekOrganizeOnContainer,
                                letterSpacing = 0.5.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Sorted pages compiled into a fresh high-quality document.",
                                fontSize = 12.sp,
                                color = SleekOrganizeOnContainer.copy(alpha = 0.70f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Button(
                        onClick = {
                            viewModel.openFile(context, File(record.filePath))
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("view_organized_pdf_btn"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary)
                    ) {
                        Icon(imageVector = Icons.Default.Launch, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Reorganized PDF File", fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.shareRecord(context, record)
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("share_organized_pdf_btn"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SleekPrimary),
                        border = BorderStroke(1.5.dp, SleekPrimary)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share Clean Document", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    TextButton(
                        onClick = {
                            viewModel.clearOrganize()
                            viewModel.navigateTo("HOME")
                        }
                    ) {
                        Text("Return Home Screen", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
                return@Column
            }

            // Central Sandbox workspace
            // Explanatory Sub-Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SANDBOX COMPOSITE PAGE DESK",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = if (activeSelectedIndex == null) "Drag pages to reorder, use arrows, or tap two to swap" else "Tap a second page to swap items...",
                        fontSize = 12.sp,
                        color = if (activeSelectedIndex != null) SleekPrimary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f).copy(alpha = 0.7f),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SleekOrganizeContainer)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${pages.size} Total Pages",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SleekOrganizeOnContainer
                    )
                }
            }

            // Grid of thumbs
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp, top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(pages, key = { _, item -> item.id }) { index, item ->
                    val isBeingDragged = dragId == item.id
                    val isSelectedToSwap = activeSelectedIndex == index
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .zIndex(if (isBeingDragged) 10f else 1f)
                            .graphicsLayer {
                                if (isBeingDragged) {
                                    scaleX = 1.08f
                                    scaleY = 1.08f
                                    shadowElevation = 12.dp.toPx()
                                    translationX = dragOffset.x
                                    translationY = dragOffset.y
                                    alpha = 0.9f
                                }
                            }
                            .pointerInput(item.id) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { _ ->
                                        dragId = item.id
                                        dragStartIndex = index
                                        currentDragIndex = index
                                        dragOffset = Offset.Zero
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffset += dragAmount
                                        
                                        val gridLayoutInfo = gridState.layoutInfo
                                        val dragStartItemInfo = gridLayoutInfo.visibleItemsInfo.find { it.key == item.id }
                                        if (dragStartItemInfo != null) {
                                            val currentAbsoluteX = dragStartItemInfo.offset.x + dragOffset.x + dragStartItemInfo.size.width / 2f
                                            val currentAbsoluteY = dragStartItemInfo.offset.y + dragOffset.y + dragStartItemInfo.size.height / 2f
                                            
                                            val hoveredItem = gridLayoutInfo.visibleItemsInfo.find { visibleItem ->
                                                val left = visibleItem.offset.x
                                                val right = visibleItem.offset.x + visibleItem.size.width
                                                val top = visibleItem.offset.y
                                                val bottom = visibleItem.offset.y + visibleItem.size.height
                                                
                                                currentAbsoluteX >= left && currentAbsoluteX <= right &&
                                                currentAbsoluteY >= top && currentAbsoluteY <= bottom
                                            }
                                            
                                            if (hoveredItem != null) {
                                                val fromIndex = currentDragIndex ?: index
                                                val toIndex = hoveredItem.index
                                                if (toIndex in currentPages.indices && fromIndex != toIndex) {
                                                    currentViewModel.movePageInOrganize(fromIndex, toIndex)
                                                    currentDragIndex = toIndex
                                                    
                                                    val targetOffsetInfo = gridLayoutInfo.visibleItemsInfo.find { it.index == toIndex }
                                                    if (targetOffsetInfo != null) {
                                                        val dx = dragStartItemInfo.offset.x - targetOffsetInfo.offset.x
                                                        val dy = dragStartItemInfo.offset.y - targetOffsetInfo.offset.y
                                                        dragOffset = Offset(dragOffset.x + dx, dragOffset.y + dy)
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        dragId = null
                                        dragStartIndex = null
                                        currentDragIndex = null
                                        dragOffset = Offset.Zero
                                    },
                                    onDragCancel = {
                                        dragId = null
                                        dragStartIndex = null
                                        currentDragIndex = null
                                        dragOffset = Offset.Zero
                                    }
                                )
                            }
                            .testTag("page_card_${index}")
                            .border(
                                width = if (isSelectedToSwap) 3.dp else if (isBeingDragged) 2.dp else 1.dp,
                                color = if (isSelectedToSwap) SleekPrimary else if (isBeingDragged) SleekPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                val currentSelected = activeSelectedIndex
                                if (currentSelected == null) {
                                    activeSelectedIndex = index
                                } else if (currentSelected == index) {
                                    activeSelectedIndex = null // cancel
                                } else {
                                    // SWAP!
                                    viewModel.movePageInOrganize(currentSelected, index)
                                    activeSelectedIndex = null
                                }
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelectedToSwap) SleekActiveBannerBg else MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column {
                            // Top Page Badge line
                            Box(modifier = Modifier.fillMaxWidth()) {
                                // Mini Header
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
                                            .background(
                                                if (isSelectedToSwap) SleekPrimary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f).copy(alpha = 0.1f)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "Page ${index + 1}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelectedToSwap) Color.White else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                        )
                                    }

                                    // Source sub-label
                                    Text(
                                        text = "Ori: p.${item.originalPageIndex + 1}",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f).copy(alpha = 0.7f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.width(60.dp),
                                        textAlign = TextAlign.End
                                    )
                                }
                            }

                            // Visual Thumbnail
                            PDFPageThumbnailImage(
                                context = context,
                                uri = item.sourceUri,
                                pageIndex = item.originalPageIndex,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )

                            // Label of source file
                            Text(
                                text = item.sourceName,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f).copy(alpha = 0.70f),
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                textAlign = TextAlign.Center
                            )

                            Divider(color = androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.5f).copy(alpha = 0.3f))

                            // Custom Sort/Duplicate/Delete Bottom Bar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Left / Right Arrows for micro sorting
                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    IconButton(
                                        onClick = {
                                            if (index > 0) {
                                                viewModel.movePageInOrganize(index, index - 1)
                                            }
                                        },
                                        enabled = index > 0,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowBack,
                                            contentDescription = "Move Left",
                                            tint = if (index > 0) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            if (index < pages.size - 1) {
                                                viewModel.movePageInOrganize(index, index + 1)
                                            }
                                        },
                                        enabled = index < pages.size - 1,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowForward,
                                            contentDescription = "Move Right",
                                            tint = if (index < pages.size - 1) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    // Duplicate page button
                                    IconButton(
                                        onClick = {
                                            viewModel.duplicatePageInOrganize(item.id)
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Duplicate Page",
                                            tint = Color(0xFF00796B),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }

                                    // Trash bin button
                                    IconButton(
                                        onClick = {
                                            viewModel.deletePageFromOrganize(item.id)
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Page",
                                            tint = Color(0xFFD32F2F),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Compile block
            Button(
                onClick = {
                    viewModel.compileOrganizedPdf(context)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .height(52.dp)
                    .testTag("compile_organized_pdf_btn"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary)
            ) {
                Icon(imageVector = Icons.Default.AutoFixHigh, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Compile & Export Reorganized PDF", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
            }
        }
    }
}

@Composable
fun PDFPageThumbnailImage(
    context: android.content.Context,
    uri: Uri,
    pageIndex: Int,
    modifier: Modifier = Modifier
) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(uri, pageIndex) {
        withContext(Dispatchers.IO) {
            val oldBmp = bitmap
            val newBmp = PdfUtils.renderPageToBitmap(context, uri, pageIndex, 300, 420)
            withContext(Dispatchers.Main) {
                bitmap = newBmp
                if (oldBmp != null && oldBmp != newBmp) {
                    oldBmp.recycle()
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            bitmap?.recycle()
        }
    }
    
    Box(
        modifier = modifier
            .background(Color.White)
            .aspectRatio(0.707f),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "PDF page item",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            CircularProgressIndicator(
                color = SleekPrimary.copy(alpha = 0.5f),
                strokeWidth = 2.dp,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ProtectFileScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val sourceFile by viewModel.protectSourceFile.collectAsStateWithLifecycle()
    val password by viewModel.protectPassword.collectAsStateWithLifecycle()
    val confirmPassword by viewModel.protectConfirmPassword.collectAsStateWithLifecycle()
    val operationCompleted by viewModel.operationCompleted.collectAsStateWithLifecycle()
    val lastGeneratedRecord by viewModel.lastGeneratedRecord.collectAsStateWithLifecycle()

    val pickFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.setProtectSourceFile(context, uri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lock PDF", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.clearProtectFile()
                            viewModel.navigateTo("HOME")
                        }
                    ) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (sourceFile == null) {
                // Empty State
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = SleekMultipageOnContainer.copy(alpha = 0.4f),
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No PDF file selected",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "click to upload pdf",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { pickFileLauncher.launch("application/pdf") },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SleekMultipageOnContainer)
                        ) {
                            Icon(imageVector = Icons.Default.FileOpen, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("upload pdf", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                val file = sourceFile!!
                
                // Display File Info Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = Color(0xFFC62828),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = file.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${file.pageCount} Pages • ${formatSize(file.size)}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                        }
                        IconButton(
                            onClick = { viewModel.clearProtectFile() }
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear file", tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                        }
                    }
                }

                // Password configuration
                var passVisible by remember { mutableStateOf(false) }
                var confirmVisible by remember { mutableStateOf(false) }

                OutlinedTextField(
                    value = password,
                    onValueChange = { viewModel.updateProtectPasswords(it, confirmPassword) },
                    label = { Text("Enter Password") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (passVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                    trailingIcon = {
                        IconButton(onClick = { passVisible = !passVisible }) {
                            Icon(
                                imageVector = if (passVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle password visibility"
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SleekMultipageOnContainer,
                        focusedLabelColor = SleekMultipageOnContainer
                    ),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { viewModel.updateProtectPasswords(password, it) },
                    label = { Text("Confirm password") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (confirmVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { viewModel.executeProtectPdf(context) }),
                    trailingIcon = {
                        IconButton(onClick = { confirmVisible = !confirmVisible }) {
                            Icon(
                                imageVector = if (confirmVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle password visibility"
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SleekMultipageOnContainer,
                        focusedLabelColor = SleekMultipageOnContainer
                    ),
                    singleLine = true
                )

                if (operationCompleted && lastGeneratedRecord != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Successfully protected your file. Output: ${lastGeneratedRecord!!.fileName}",
                        fontSize = 12.sp,
                        color = SleekUnlockOnContainer.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.shareRecord(context, lastGeneratedRecord!!) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SleekUnlockOnContainer),
                            border = BorderStroke(1.dp, SleekUnlockOnContainer)
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Button(
                            onClick = { viewModel.openFile(context, java.io.File(lastGeneratedRecord!!.filePath)) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = SleekUnlockOnContainer)
                        ) {
                            Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open PDF", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
                
                Button(
                    onClick = {
                        viewModel.executeProtectPdf(context)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(bottom = 24.dp)
                        .testTag("encrypt_pdf_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SleekMultipageOnContainer)
                ) {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("set password for file", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvertColorScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val sourceFile by viewModel.invertSourceFile.collectAsStateWithLifecycle()
    val operationCompleted by viewModel.operationCompleted.collectAsStateWithLifecycle()
    val lastGeneratedRecord by viewModel.lastGeneratedRecord.collectAsStateWithLifecycle()

    val pickFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.setInvertSourceFile(context, uri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PDF Color Inverter", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.clearInvert()
                            viewModel.navigateTo("HOME")
                        }
                    ) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Introductory Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                colors = CardDefaults.cardColors(containerColor = SleekInvertContainer.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, SleekInvertOnContainer.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.InvertColors,
                            contentDescription = null,
                            tint = SleekInvertOnContainer,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "What Is PDF Color Inversion?",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = SleekInvertOnContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This tool creates a photographic negative of your PDF or image. Every color is replaced with its opposite: white becomes black, and black becomes white. Perfect for comfortable Reading in Dark Mode, reversing high-contrast documents, proofing, design contrast testing, or artistic negative conversion.",
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = SleekInvertOnContainer.copy(alpha = 0.85f)
                    )
                }
            }

            if (sourceFile == null) {
                // Empty state select box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.InvertColors,
                            contentDescription = null,
                            tint = SleekInvertOnContainer.copy(alpha = 0.15f),
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No PDF or Image Selected",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Upload a PDF or Image from your device to invert all colors instantly. Zero software required.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { pickFileLauncher.launch("*/*") },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SleekInvertOnContainer),
                            modifier = Modifier.testTag("select_invert_file_btn")
                        ) {
                            Icon(imageVector = Icons.Default.FileOpen, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Choose PDF or Image", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                val file = sourceFile!!

                // Selected File Info Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = file.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row {
                                    Text(
                                        text = if (file.isPdf) "PDF Document" else "Image File",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SleekInvertOnContainer
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${file.pageCount} ${if (file.pageCount == 1) "page" else "pages"}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            IconButton(onClick = { viewModel.clearInvert() }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear file",
                                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                // File Visual Preview
                Text(
                    text = "VISUAL PREVIEW",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (file.isPdf) {
                        PDFPageThumbnailImage(
                            context = context,
                            uri = file.uri,
                            pageIndex = 0,
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(180.dp)
                                .padding(12.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    } else {
                        AsyncImage(
                            model = file.uri,
                            contentDescription = "Image preview",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Success feedback
                if (operationCompleted && lastGeneratedRecord != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp)
                            .testTag("invert_success_feedback_card"),
                        colors = CardDefaults.cardColors(containerColor = SleekUnlockContainer),
                        border = BorderStroke(1.dp, SleekUnlockOnContainer.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = SleekUnlockOnContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Color Inversion Success!",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SleekUnlockOnContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Your newly inverted PDF file is generated and ready to download! File size: ${String.format(java.util.Locale.US, "%.1f", lastGeneratedRecord!!.fileSize / 1024.0 / 1024.0)} MB",
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                color = SleekUnlockOnContainer.copy(alpha = 0.85f)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.shareRecord(context, lastGeneratedRecord!!) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SleekUnlockOnContainer),
                                    border = BorderStroke(1.dp, SleekUnlockOnContainer),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Share / Export", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Button(
                                    onClick = { viewModel.openFile(context, File(lastGeneratedRecord!!.filePath)) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = SleekUnlockOnContainer),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Open PDF", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // Invert action button
                Button(
                    onClick = {
                        viewModel.invertPdfColors(context)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .padding(bottom = 24.dp)
                        .testTag("apply_color_inversion_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SleekInvertOnContainer)
                ) {
                    Icon(imageVector = Icons.Default.InvertColors, contentDescription = null)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Invert Colors!", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignatureMakerScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val signaturePdfFile by viewModel.signaturePdfSource.collectAsStateWithLifecycle()
    val signatureImageUri by viewModel.signatureImageUri.collectAsStateWithLifecycle()
    val signatureOriginalBitmap by viewModel.signatureOriginalBitmap.collectAsStateWithLifecycle()
    
    val sigCropLeft by viewModel.sigCropLeft.collectAsStateWithLifecycle()
    val sigCropRight by viewModel.sigCropRight.collectAsStateWithLifecycle()
    val sigCropTop by viewModel.sigCropTop.collectAsStateWithLifecycle()
    val sigCropBottom by viewModel.sigCropBottom.collectAsStateWithLifecycle()

    val signaturePageIndex by viewModel.signaturePageIndex.collectAsStateWithLifecycle()
    val signatureRelativeX by viewModel.signatureRelativeX.collectAsStateWithLifecycle()
    val signatureRelativeY by viewModel.signatureRelativeY.collectAsStateWithLifecycle()
    val signatureScaleFactor by viewModel.signatureScaleFactor.collectAsStateWithLifecycle()
    val isSignatureDragOptionEnabled by viewModel.isSignatureDragOptionEnabled.collectAsStateWithLifecycle()

    val useCustomKeystore by viewModel.useCustomKeystore.collectAsStateWithLifecycle()
    val customKeystoreUri by viewModel.customKeystoreUri.collectAsStateWithLifecycle()
    val customKeystorePassword by viewModel.customKeystorePassword.collectAsStateWithLifecycle()
    val commonName by viewModel.commonName.collectAsStateWithLifecycle()
    val organization by viewModel.organization.collectAsStateWithLifecycle()
    val organizationalUnit by viewModel.organizationalUnit.collectAsStateWithLifecycle()
    val country by viewModel.country.collectAsStateWithLifecycle()
    val sigReason by viewModel.sigReason.collectAsStateWithLifecycle()
    val sigLocation by viewModel.sigLocation.collectAsStateWithLifecycle()
    val sigContact by viewModel.sigContact.collectAsStateWithLifecycle()
    val sigGraphicType by viewModel.sigGraphicType.collectAsStateWithLifecycle()
    val sigShowDetailsText by viewModel.sigShowDetailsText.collectAsStateWithLifecycle()
    val sigTimeSource by viewModel.sigTimeSource.collectAsStateWithLifecycle()
    val tsaServerUrl by viewModel.tsaServerUrl.collectAsStateWithLifecycle()

    val operationCompleted by viewModel.operationCompleted.collectAsStateWithLifecycle()
    val lastGeneratedRecord by viewModel.lastGeneratedRecord.collectAsStateWithLifecycle()

    var activeStep by remember { mutableStateOf(1) }

    val pickPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.setSignaturePdfSource(context, uri)
        }
    }

    val pickSigImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.setSignatureImageUri(context, uri)
        }
    }

    val pickKeystoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.setCustomKeystoreUri(uri)
        }
    }

    val croppedSignatureBitmap = remember(signatureOriginalBitmap, sigCropLeft, sigCropRight, sigCropTop, sigCropBottom) {
        viewModel.getCroppedSignatureBitmap()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Signature Maker", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.clearSignatureMaker()
                            viewModel.navigateTo("HOME")
                        }
                    ) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (operationCompleted && lastGeneratedRecord != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    modifier = Modifier.size(90.dp),
                    shape = CircleShape,
                    color = SleekUnlockContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Success tick",
                            tint = SleekUnlockOnContainer,
                            modifier = Modifier.size(46.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "PDF Signed Successfully!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Your signature has been permanently embedded onto the PDF rendering canvas without compressing document vectors.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(30.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = lastGeneratedRecord!!.fileName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row {
                            Text(
                                text = "File Size: ${formatSize(lastGeneratedRecord!!.fileSize)}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "Pages: ${lastGeneratedRecord!!.pagesCount}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.shareRecord(context, lastGeneratedRecord!!) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SleekSignatureOnContainer),
                        border = BorderStroke(1.dp, SleekSignatureOnContainer),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Button(
                        onClick = { viewModel.openFile(context, File(lastGeneratedRecord!!.filePath)) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = SleekSignatureOnContainer),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open file", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                TextButton(
                    onClick = {
                        viewModel.clearSignatureMaker()
                        activeStep = 1
                    }
                ) {
                    Text("Sign Another PDF", fontWeight = FontWeight.Bold, color = SleekPrimary)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                if (activeStep == 1) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            colors = CardDefaults.cardColors(containerColor = SleekSignatureContainer.copy(alpha = 0.5f)),
                            border = BorderStroke(1.dp, SleekSignatureOnContainer.copy(alpha = 0.1f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Create,
                                        contentDescription = null,
                                        tint = SleekSignatureOnContainer,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Professional PDF Signing Tool",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = SleekSignatureOnContainer
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Embed your signature cleanly onto any page of your PDF document. Crop margins, adjust scale, and drag visually into place before saving with absolute precision.",
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    color = SleekSignatureOnContainer.copy(alpha = 0.85f)
                                )
                            }
                        }

                        Text(
                            text = "STEP 1: SELECT PDF DOCUMENT",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )

                        if (signaturePdfFile == null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(115.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .clickable { pickPdfLauncher.launch("application/pdf") }
                                    .drawBehind {
                                        val stroke = Stroke(
                                            width = 1.6f.dp.toPx(),
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f), 0f)
                                        )
                                        drawRoundRect(
                                            color = androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.5f),
                                            style = stroke,
                                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx())
                                        )
                                    }
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.UploadFile,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Choose PDF Document", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground)
                                    Text("Select the document file to be signed", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                                }
                            }
                        } else {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        tint = SleekPrimary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = signaturePdfFile!!.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Text(
                                            text = "Size: ${formatSize(signaturePdfFile!!.size)} • Pages: ${signaturePdfFile!!.pageCount}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.clearSignatureMaker() }
                                    ) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear file", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }

                        Text(
                            text = "STEP 2: CHOOSE SIGNATURE GRAPHIC",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                        )

                        if (sigGraphicType == "IMAGE") {
                            if (signatureImageUri == null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(115.dp)
                                        .clip(RoundedCornerShape(24.dp))
                                        .clickable { pickSigImageLauncher.launch("image/*") }
                                        .drawBehind {
                                            val stroke = Stroke(
                                                width = 1.6f.dp.toPx(),
                                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f), 0f)
                                            )
                                            drawRoundRect(
                                                color = androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.5f),
                                                style = stroke,
                                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx())
                                            )
                                        }
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.Image,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("Choose Signature Image", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground)
                                        Text("Supports transparent PNG, JPEG, scans, etc.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                                    }
                                }
                            } else {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(20.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (signatureOriginalBitmap != null) {
                                            Image(
                                                bitmap = signatureOriginalBitmap!!.asImageBitmap(),
                                                contentDescription = "Signature thumbnail",
                                                modifier = Modifier
                                                    .size(54.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                                    .background(Color.White),
                                                contentScale = ContentScale.Fit
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Image,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Signature Loaded Successfully",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onBackground
                                            )
                                            Text(
                                                text = "Visual scale & cropping fully active",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                            )
                                        }
                                        IconButton(
                                            onClick = { viewModel.clearSignatureMaker() }
                                        ) {
                                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear file", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        } else {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = SleekActiveBannerBg),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, SleekPrimary.copy(alpha = 0.15f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = "Verified stamp enabled",
                                        tint = SleekPrimary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Certified Virtual Stamp Active",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = SleekPrimary
                                        )
                                        Text(
                                            text = "Using automatic visual ${sigGraphicType.lowercase()} stamp. No hand-drawn signature upload required.",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }

                        // 🎨 STEP 3: VISUAL STAMP & DIGITAL BRANDING Accordion / Card-stack
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "STEP 3: VISUAL STAMP & DIGITAL BRANDING",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Select Stamp Aesthetic",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "Choose how the signature representation is rendered on the document.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                val stampOptions = listOf(
                                    "IMAGE" to "Handwritten Ink image",
                                    "CHECKMARK" to "Green Certified Checkmark",
                                    "SEAL" to "Administrative Corporate Seal",
                                    "NONE" to "Cryptographic Metadata text only"
                                )

                                stampOptions.forEach { (type, description) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { viewModel.setSigGraphicType(type) }
                                            .background(if (sigGraphicType == type) SleekActiveBannerBg else Color.Transparent)
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = (sigGraphicType == type),
                                            onClick = { viewModel.setSigGraphicType(type) },
                                            colors = RadioButtonDefaults.colors(selectedColor = SleekPrimary)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = type.replace("_", " "),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (sigGraphicType == type) SleekPrimary else MaterialTheme.colorScheme.onBackground
                                            )
                                            Text(
                                                text = description,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.5f).copy(alpha = 0.4f))
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Show Detailed Validation Overlay",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Text(
                                            text = "Prints Signer Name, Date, and Reason as text alongside graphics",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                        )
                                    }
                                    Switch(
                                        checked = sigShowDetailsText,
                                        onCheckedChange = { viewModel.setSigShowDetailsText(it) },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = SleekPrimary)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "STEP 4: DECRYPT & CERTIFY AUTHORITY (PKCS#12)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Custom P12 / PFX Keystore",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Text(
                                            text = "Load external .p12 config otherwise generate Self-Signed certificate",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                        )
                                    }
                                    Switch(
                                        checked = useCustomKeystore,
                                        onCheckedChange = { viewModel.setUseCustomKeystore(it) },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = SleekPrimary)
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.5f).copy(alpha = 0.4f))
                                Spacer(modifier = Modifier.height(12.dp))

                                if (useCustomKeystore) {
                                    Text(
                                        text = "Load .p12 Keystore File",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Button(
                                        onClick = { pickKeystoreLauncher.launch("application/x-pkcs12") },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.outline),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(imageVector = Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (customKeystoreUri != null) "Keystore Configured! Tap to change" else "Select PKCS#12 (.p12 / .pfx) file",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Keystore Password",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutlinedTextField(
                                        value = customKeystorePassword,
                                        onValueChange = { viewModel.setCustomKeystorePassword(it) },
                                        placeholder = { Text("Enter keystore decrypt password", fontSize = 12.sp) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                } else {
                                    Text(
                                        text = "Dynamic Authority Certificate (Subject DN)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedTextField(
                                        value = commonName,
                                        onValueChange = { viewModel.setCommonName(it) },
                                        label = { Text("Signer Common Name (CN)", fontSize = 11.sp) },
                                        placeholder = { Text("e.g. John Doe", fontSize = 12.sp) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                    )

                                    OutlinedTextField(
                                        value = organization,
                                        onValueChange = { viewModel.setOrganization(it) },
                                        label = { Text("Organization Name (O)", fontSize = 11.sp) },
                                        placeholder = { Text("e.g. Enterprise Solutions Corp", fontSize = 12.sp) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                    )

                                    OutlinedTextField(
                                        value = organizationalUnit,
                                        onValueChange = { viewModel.setOrganizationalUnit(it) },
                                        label = { Text("Organizational Unit (OU)", fontSize = 11.sp) },
                                        placeholder = { Text("e.g. Legal Operations", fontSize = 12.sp) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                    )

                                    OutlinedTextField(
                                        value = country,
                                        onValueChange = { viewModel.setCountry(it) },
                                        label = { Text("Country Code (C)", fontSize = 11.sp) },
                                        placeholder = { Text("e.g. US, IN, FR", fontSize = 12.sp) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "STEP 5: SECURE METADATA & INTEGRITY REASON (CMS)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                OutlinedTextField(
                                    value = sigReason,
                                    onValueChange = { viewModel.setSigReason(it) },
                                    label = { Text("Signature Reason", fontSize = 11.sp) },
                                    placeholder = { Text("e.g. I agree to the terms in this file", fontSize = 12.sp) },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                )

                                OutlinedTextField(
                                    value = sigLocation,
                                    onValueChange = { viewModel.setSigLocation(it) },
                                    label = { Text("Location", fontSize = 11.sp) },
                                    placeholder = { Text("e.g. Boston, MA, US", fontSize = 12.sp) },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                )

                                OutlinedTextField(
                                    value = sigContact,
                                    onValueChange = { viewModel.setSigContact(it) },
                                    label = { Text("Contact Info / email", fontSize = 11.sp) },
                                    placeholder = { Text("e.g. admin@company.com", fontSize = 12.sp) },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "STEP 6: TRUSTED TIME SOURCE",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                val timeSources = listOf(
                                    "LOCAL" to "Local Device Clock (Device time stamp)",
                                    "TSA" to "Trusted RFC-3161 Timestamp Server (Unspoofable time)"
                                )

                                timeSources.forEach { (source, desc) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { viewModel.setSigTimeSource(source) }
                                            .background(if (sigTimeSource == source) SleekActiveBannerBg else Color.Transparent)
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = (sigTimeSource == source),
                                            onClick = { viewModel.setSigTimeSource(source) },
                                            colors = RadioButtonDefaults.colors(selectedColor = SleekPrimary)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = if (source == "TSA") "TSA Timestamp Server" else "Local Device Clock",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (sigTimeSource == source) SleekPrimary else MaterialTheme.colorScheme.onBackground
                                            )
                                            Text(
                                                text = desc,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                }

                                if (sigTimeSource == "TSA") {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "TSA Server Endpoint URL",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutlinedTextField(
                                        value = tsaServerUrl,
                                        onValueChange = { viewModel.setTsaServerUrl(it) },
                                        placeholder = { Text("e.g. http://timestamp.digicert.com", fontSize = 12.sp) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Text(
                                        text = "Must be a globally accessible HTTP/HTTPS timestamping server.",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                                    )
                                }
                            }
                        }

                        val isNextEnabled = signaturePdfFile != null && (sigGraphicType != "IMAGE" || signatureImageUri != null)
                        Spacer(modifier = Modifier.height(36.dp))

                        Button(
                            onClick = { activeStep = 2 },
                            enabled = isNextEnabled,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SleekSignatureOnContainer,
                                disabledContainerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )
                        ) {
                            Text("Next: Edit & Position Signature", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                } else if (activeStep == 2 && signaturePdfFile != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    ) {
                        var editModeTab by remember { mutableStateOf("PLACEMENT") }
                        if (sigGraphicType != "IMAGE") {
                            editModeTab = "PLACEMENT"
                        }

                        if (sigGraphicType == "IMAGE") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Button(
                                    onClick = { editModeTab = "PLACEMENT" },
                                    modifier = Modifier.weight(1f),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (editModeTab == "PLACEMENT") Color.White else Color.Transparent,
                                        contentColor = if (editModeTab == "PLACEMENT") SleekSignatureOnContainer else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                    )
                                ) {
                                    Icon(imageVector = Icons.Default.OpenWith, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Place & Drag", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }

                                Button(
                                    onClick = { editModeTab = "CROP" },
                                    modifier = Modifier.weight(1f),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (editModeTab == "CROP") Color.White else Color.Transparent,
                                        contentColor = if (editModeTab == "CROP") SleekSignatureOnContainer else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                    )
                                ) {
                                    Icon(imageVector = Icons.Default.Crop, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Crop Margins", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }

                        if (editModeTab == "PLACEMENT") {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "🎯 Positioning Settings",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Drag/Drop:",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Switch(
                                                checked = isSignatureDragOptionEnabled,
                                                onCheckedChange = { viewModel.setSignatureDragOptionEnabled(it) },
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = Color.White,
                                                    checkedTrackColor = SleekPrimary
                                                ),
                                                modifier = Modifier.scale(0.81f)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (isSignatureDragOptionEnabled) 
                                            "Option enabled: DRAG signature overlay directly with your finger on the page preview, or use the manual controls below."
                                            else "Option disabled: Scrolling list is active. Toggle 'Drag/Drop' switch or use buttons below to adjust signature position!",
                                        fontSize = 11.sp,
                                        lineHeight = 14.sp,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                    )

                                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.5f).copy(alpha = 0.4f))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "Signature Size: ${(signatureScaleFactor * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Slider(
                                            value = signatureScaleFactor,
                                            onValueChange = { viewModel.setSignatureScaleFactor(it) },
                                            valueRange = 0.3f..2.5f,
                                            modifier = Modifier
                                                .fillMaxWidth(0.72f)
                                                .height(28.dp),
                                            colors = SliderDefaults.colors(
                                                thumbColor = SleekPrimary,
                                                activeTrackColor = SleekPrimary
                                            )
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "🎯 Nudge Position Fine-Tune",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Text(
                                            text = "X: ${(signatureRelativeX * 100).toInt()}% | Y: ${(signatureRelativeY * 100).toInt()}%",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SleekPrimary
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        IconButton(
                                            onClick = { viewModel.setSignatureRelativeX((signatureRelativeX - 0.05f).coerceIn(0f, 1f)) },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowBack,
                                                contentDescription = "Move Left",
                                                modifier = Modifier.size(18.dp),
                                                tint = MaterialTheme.colorScheme.onBackground
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            IconButton(
                                                onClick = { viewModel.setSignatureRelativeY((signatureRelativeY - 0.05f).coerceIn(0f, 1f)) },
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ArrowUpward,
                                                    contentDescription = "Move Up",
                                                    modifier = Modifier.size(18.dp),
                                                    tint = MaterialTheme.colorScheme.onBackground
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))

                                            IconButton(
                                                onClick = { viewModel.setSignatureRelativeY((signatureRelativeY + 0.05f).coerceIn(0f, 1f)) },
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ArrowDownward,
                                                    contentDescription = "Move Down",
                                                    modifier = Modifier.size(18.dp),
                                                    tint = MaterialTheme.colorScheme.onBackground
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        IconButton(
                                            onClick = { viewModel.setSignatureRelativeX((signatureRelativeX + 0.05f).coerceIn(0f, 1f)) },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowForward,
                                                contentDescription = "Move Right",
                                                modifier = Modifier.size(18.dp),
                                                tint = MaterialTheme.colorScheme.onBackground
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Text(
                                        text = "📍 Quick Position Presets",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        val presets = listOf(
                                            "Top L" to (0.15f to 0.15f),
                                            "Top R" to (0.85f to 0.15f),
                                            "Center" to (0.50f to 0.50f),
                                            "Btm L" to (0.15f to 0.85f),
                                            "Btm R" to (0.85f to 0.85f)
                                        )
                                        presets.forEach { (label, coords) ->
                                            val isSelected = (Math.abs(signatureRelativeX - coords.first) < 0.03f && Math.abs(signatureRelativeY - coords.second) < 0.03f)
                                            OutlinedButton(
                                                onClick = {
                                                    viewModel.setSignatureRelativeX(coords.first)
                                                    viewModel.setSignatureRelativeY(coords.second)
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(30.dp),
                                                contentPadding = PaddingValues(0.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    containerColor = if (isSelected) SleekPrimary.copy(alpha = 0.12f) else Color.Transparent,
                                                    contentColor = if (isSelected) SleekPrimary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                                ),
                                                border = BorderStroke(
                                                    width = 1.dp,
                                                    color = if (isSelected) SleekPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                                )
                                            ) {
                                                Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "✂️ Signature Crop Margins",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Adjust sliders to crop blank margins, edges, or optimize image size.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(70.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .border(1.dp, MaterialTheme.colorScheme.outline)
                                                .background(Color.White),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (croppedSignatureBitmap != null) {
                                                Image(
                                                    bitmap = croppedSignatureBitmap.asImageBitmap(),
                                                    contentDescription = "Cropped Preview",
                                                    modifier = Modifier.fillMaxSize(0.92f),
                                                    contentScale = ContentScale.Fit
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            CropSliderRow(label = "L", value = sigCropLeft, onValueChange = { viewModel.setSigCropLeft(it) })
                                            CropSliderRow(label = "R", value = sigCropRight, onValueChange = { viewModel.setSigCropRight(it) })
                                            CropSliderRow(label = "T", value = sigCropTop, onValueChange = { viewModel.setSigCropTop(it) })
                                            CropSliderRow(label = "B", value = sigCropBottom, onValueChange = { viewModel.setSigCropBottom(it) })
                                        }
                                    }
                                }
                            }
                        }

                        Text(
                            text = "SCROLL PAGES & CHOOSE PLACEMENT LOCATION:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )

                        val pagesCount = signaturePdfFile!!.pageCount
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(12.dp)
                        ) {
                            items(pagesCount) { index ->
                                PageSignBox(
                                    context = context,
                                    pdfUri = signaturePdfFile!!.uri,
                                    pageIndex = index,
                                    isCurrentPage = (signaturePageIndex == index),
                                    onSelectPage = { viewModel.setSignaturePageIndex(index) },
                                    signatureBitmap = croppedSignatureBitmap,
                                    sigRelativeX = signatureRelativeX,
                                    sigRelativeY = signatureRelativeY,
                                    sigScale = signatureScaleFactor,
                                    isDragOptionEnabled = isSignatureDragOptionEnabled,
                                    onPositionChanged = { rx, ry ->
                                        viewModel.setSignatureRelativeX(rx)
                                        viewModel.setSignatureRelativeY(ry)
                                    },
                                    graphicType = sigGraphicType,
                                    commonName = commonName,
                                    organization = organization
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { activeStep = 1 },
                                modifier = Modifier
                                    .weight(0.42f)
                                    .height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, SleekSignatureOnContainer),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = SleekSignatureOnContainer)
                            ) {
                                Text("Back", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { viewModel.generateSignedPdf(context) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SleekSignatureOnContainer)
                            ) {
                                Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Create Signed PDF", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PageSignBox(
    context: android.content.Context,
    pdfUri: Uri,
    pageIndex: Int,
    isCurrentPage: Boolean,
    onSelectPage: () -> Unit,
    signatureBitmap: Bitmap?,
    sigRelativeX: Float,
    sigRelativeY: Float,
    sigScale: Float,
    isDragOptionEnabled: Boolean,
    onPositionChanged: (Float, Float) -> Unit,
    graphicType: String = "IMAGE",
    commonName: String = "",
    organization: String = ""
) {
    var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(pdfUri, pageIndex) {
        withContext(Dispatchers.IO) {
            val oldBmp = pageBitmap
            val bmp = PdfUtils.renderPageToBitmap(context, pdfUri, pageIndex, 480, 680)
            withContext(Dispatchers.Main) {
                pageBitmap = bmp
                if (oldBmp != null && oldBmp != bmp) {
                    oldBmp.recycle()
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            pageBitmap?.recycle()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectPage() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentPage) SleekActiveBannerBg else Color.White
        ),
        border = BorderStroke(
            width = if (isCurrentPage) 2.5.dp else 1.dp,
            color = if (isCurrentPage) SleekPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrentPage) 4.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Page ${pageIndex + 1}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (isCurrentPage) {
                    Surface(
                        color = SleekPrimary,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "ACTIVE SIGN PAGE",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    Text(
                        text = "Tap to place here",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f).copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            val pageAspect = pageBitmap?.let { it.width.toFloat() / it.height.toFloat() } ?: 0.707f

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(pageAspect)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White),
                contentAlignment = Alignment.TopStart
            ) {
                val boxWidth = maxWidth
                val boxHeight = maxHeight

                if (pageBitmap != null) {
                    Image(
                        bitmap = pageBitmap!!.asImageBitmap(),
                        contentDescription = "PDF Page Thumbnail",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = SleekPrimary,
                            strokeWidth = 2.dp
                        )
                    }
                }

                val effectiveBitmap = when (graphicType) {
                    "IMAGE" -> signatureBitmap
                    "CHECKMARK" -> {
                        remember {
                            PdfUtils.createGreenCheckmarkBitmap()
                        }
                    }
                    "SEAL" -> {
                        remember(commonName, organization) {
                            PdfUtils.createCorporateSealBitmap(commonName, organization)
                        }
                    }
                    else -> null
                }

                if (isCurrentPage && effectiveBitmap != null) {
                    val sigAspect = effectiveBitmap.width.toFloat() / effectiveBitmap.height.toFloat()
                    val baseWidthDp = boxWidth * 0.30f
                    val currentWidthDp = baseWidthDp * sigScale
                    val currentHeightDp = currentWidthDp / sigAspect

                    val updatedSigRelativeX by rememberUpdatedState(sigRelativeX)
                    val updatedSigRelativeY by rememberUpdatedState(sigRelativeY)
                    val updatedBoxWidth by rememberUpdatedState(boxWidth)
                    val updatedBoxHeight by rememberUpdatedState(boxHeight)
                    val updatedOnPositionChanged by rememberUpdatedState(onPositionChanged)

                    val sigCenterX = sigRelativeX * boxWidth.value
                    val sigCenterY = sigRelativeY * boxHeight.value

                    val leftOffset = (sigCenterX - (currentWidthDp.value / 2f)).coerceIn(0f, boxWidth.value - currentWidthDp.value)
                    val topOffset = (sigCenterY - (currentHeightDp.value / 2f)).coerceIn(0f, boxHeight.value - currentHeightDp.value)

                    Box(
                        modifier = Modifier
                            .offset(x = leftOffset.dp, y = topOffset.dp)
                            .size(width = currentWidthDp, height = currentHeightDp)
                            .background(Color.White.copy(alpha = 0.1f))
                            .border(
                                width = if (isDragOptionEnabled) 1.5.dp else 0.dp,
                                color = if (isDragOptionEnabled) SleekPrimary else Color.Transparent,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .pointerInput(isDragOptionEnabled) {
                                if (isDragOptionEnabled) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        val pxWidth = updatedBoxWidth.toPx()
                                        val pxHeight = updatedBoxHeight.toPx()
                                        if (pxWidth > 0 && pxHeight > 0) {
                                            val newX = (updatedSigRelativeX * pxWidth + dragAmount.x) / pxWidth
                                            val newY = (updatedSigRelativeY * pxHeight + dragAmount.y) / pxHeight
                                            updatedOnPositionChanged(
                                                newX.coerceIn(0f, 1f),
                                                newY.coerceIn(0f, 1f)
                                            )
                                        }
                                    }
                                }
                            }
                    ) {
                        Image(
                            bitmap = effectiveBitmap.asImageBitmap(),
                            contentDescription = "Signature Overlay",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                        
                        if (isDragOptionEnabled) {
                            Icon(
                                imageVector = Icons.Default.OpenWith,
                                contentDescription = "Move handle",
                                tint = SleekPrimary,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(20.dp)
                                    .background(Color.White.copy(alpha = 0.82f), RoundedCornerShape(4.dp))
                                    .padding(2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CropSliderRow(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(16.dp),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..0.45f,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = SleekPrimary,
                activeTrackColor = SleekPrimary.copy(alpha = 0.5f)
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterPdfScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val filterSourceFile by viewModel.filterSourceFile.collectAsStateWithLifecycle()
    val globalFilterMode by viewModel.globalFilterMode.collectAsStateWithLifecycle()
    val pageAdjustments by viewModel.pageAdjustments.collectAsStateWithLifecycle()
    val filterPageOrder by viewModel.filterPageOrder.collectAsStateWithLifecycle()
    val isFilterPageDragEnabled by viewModel.isFilterPageDragEnabled.collectAsStateWithLifecycle()
    val operationCompleted by viewModel.operationCompleted.collectAsStateWithLifecycle()
    val lastGeneratedRecord by viewModel.lastGeneratedRecord.collectAsStateWithLifecycle()

    var activeAdjustingPageIndex by remember { mutableStateOf<Int?>(null) }
    var showAdjustDialog by remember { mutableStateOf(false) }

    val pickPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.setFilterSourceFile(context, uri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Filter & Correct PDF", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.clearFilterPdf()
                            viewModel.navigateTo("HOME")
                        }
                    ) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (operationCompleted && lastGeneratedRecord != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    modifier = Modifier.size(90.dp),
                    shape = CircleShape,
                    color = SleekFilterContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Success",
                            tint = SleekFilterOnContainer,
                            modifier = Modifier.size(46.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Document Corrected Successfully!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Shadows, wrinkles, and illumination gradients have been professionally cleaned per-page. Output has been generated successfully.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(30.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = lastGeneratedRecord!!.fileName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row {
                            Text(
                                text = "File Size: ${formatSize(lastGeneratedRecord!!.fileSize)}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "Pages: ${lastGeneratedRecord!!.pagesCount}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.shareRecord(context, lastGeneratedRecord!!) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SleekFilterOnContainer),
                        border = BorderStroke(1.dp, SleekFilterOnContainer),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share Cleaned", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Button(
                        onClick = { viewModel.openFile(context, File(lastGeneratedRecord!!.filePath)) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = SleekFilterOnContainer),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open Corrected", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                TextButton(
                    onClick = {
                        viewModel.clearFilterPdf()
                    }
                ) {
                    Text("Clean Another Document", fontWeight = FontWeight.Bold, color = SleekPrimary)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                if (filterSourceFile == null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            colors = CardDefaults.cardColors(containerColor = SleekFilterContainer.copy(alpha = 0.5f)),
                            border = BorderStroke(1.dp, SleekFilterOnContainer.copy(alpha = 0.15f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AutoFixHigh,
                                        contentDescription = null,
                                        tint = SleekFilterOnContainer,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Clean & Beautify Scanned Docs",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = SleekFilterOnContainer
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Enhance readability of dark, cast-shadowed, or wrinkled scanned PDFs instantly using adaptive dynamic correction. Adjust filters and manual sliders per-page for 100% perfection.",
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    color = SleekFilterOnContainer.copy(alpha = 0.85f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .clickable { pickPdfLauncher.launch("application/pdf") }
                                .drawBehind {
                                    val stroke = Stroke(
                                        width = 1.6f.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f), 0f)
                                    )
                                    drawRoundRect(
                                        color = androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.5f),
                                        style = stroke,
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx())
                                    )
                                }
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    tint = SleekFilterOnContainer,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Choose Scanned PDF", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground)
                                Text("Select the PDF containing shadows or dark margins", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                            }
                        }
                    }
                } else {
                    val pdfFile = filterSourceFile!!
                    Column(modifier = Modifier.fillMaxSize()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.Description, contentDescription = null, tint = SleekFilterOnContainer, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = pdfFile.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onBackground)
                                    Text(text = "${pdfFile.pageCount} Pages • ${formatSize(pdfFile.size)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                                }
                                TextButton(onClick = { pickPdfLauncher.launch("application/pdf") }) {
                                    Text("Change file", fontSize = 12.sp, color = SleekPrimary)
                                }
                            }
                        }

                        Text(
                            text = "SELECT PRIMARY FILTER MODE:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 8.dp, bottom = 6.dp)
                        )

                        val filters = listOf(
                            "OMNIFIX" to "Omnifix 🌟",
                            "MAGIC_COLOR" to "Magic Color",
                            "BW" to "B&W Text",
                            "GRAY" to "Gray Pres",
                            "LIGHTEN" to "Lighten"
                        )
                        
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            items(filters.size) { i ->
                                val (modeKey, modeName) = filters[i]
                                val isSelected = globalFilterMode == modeKey
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setGlobalFilterMode(modeKey) },
                                    label = { Text(modeName, fontSize = 11.5.sp, fontWeight = FontWeight.Bold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SleekFilterOnContainer,
                                        selectedLabelColor = Color.White,
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        labelColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                    ),
                                    border = BorderStroke(1.dp, if (isSelected) SleekFilterOnContainer else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                )
                            }
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.SwapVert, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Page Rearranging Controls", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onBackground)
                                    }
                                    Switch(
                                        checked = isFilterPageDragEnabled,
                                        onCheckedChange = { viewModel.setFilterPageDragEnabled(it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = SleekPrimary
                                        ),
                                        modifier = Modifier.scale(0.8f)
                                    )
                                }
                                Text(
                                    text = if (isFilterPageDragEnabled) 
                                        "Rearrange options enabled. Click ▲ / ▼ to adjust page positions. If some pages need custom brightness, click 'Manual Adjust'."
                                        else "Rearrange arrows hidden. All pages remain in their natural sequential order. Visual filters remain live.",
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }

                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.2f)),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(10.dp)
                        ) {
                            itemsIndexed(filterPageOrder) { listIndex, pageIndex ->
                                val settings = pageAdjustments[pageIndex] ?: com.example.util.ManualAdjustSettings()
                                FilteredPageCard(
                                    context = context,
                                    pdfUri = pdfFile.uri,
                                    pageIndex = pageIndex,
                                    listIndex = listIndex,
                                    maxCount = filterPageOrder.size,
                                    globalMode = globalFilterMode,
                                    pageSettings = settings,
                                    isDragEnabled = isFilterPageDragEnabled,
                                    onMoveUp = { viewModel.moveFilterPageUp(listIndex) },
                                    onMoveDown = { viewModel.moveFilterPageDown(listIndex) },
                                    onManualAdjustClick = {
                                        activeAdjustingPageIndex = pageIndex
                                        showAdjustDialog = true
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { viewModel.generateFilteredPdf(context) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .padding(bottom = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SleekFilterOnContainer)
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Apply Filters & Save Document", fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }

    if (showAdjustDialog && activeAdjustingPageIndex != null) {
        val pageIndex = activeAdjustingPageIndex!!
        val settings = pageAdjustments[pageIndex] ?: com.example.util.ManualAdjustSettings()

        var localBrightness by remember(pageIndex) { mutableStateOf(settings.brightness) }
        var localContrast by remember(pageIndex) { mutableStateOf(settings.contrast) }
        var localOverrideMode by remember(pageIndex) { mutableStateOf(settings.overrideMode) }

        Dialog(onDismissRequest = { showAdjustDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "🛠️ Manual Page Correct",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Fine-tune page ${pageIndex + 1} individually for 99.999% accurate correction.",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        DialogPreviewThumbnail(
                            context = context,
                            pdfUri = filterSourceFile!!.uri,
                            pageIndex = pageIndex,
                            globalMode = globalFilterMode,
                            pageSettings = com.example.util.ManualAdjustSettings(
                                brightness = localBrightness,
                                contrast = localContrast,
                                overrideMode = localOverrideMode
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Specific Page Mode Override:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                    
                    val overrideOptions = listOf(
                        null to "Default",
                        "OMNIFIX" to "Omni",
                        "MAGIC_COLOR" to "Magic",
                        "BW" to "B&W",
                        "GRAY" to "Gray",
                        "LIGHTEN" to "Light"
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        overrideOptions.forEach { (optionKey, optionLabel) ->
                            val isSelected = localOverrideMode == optionKey
                            OutlinedButton(
                                onClick = { 
                                    localOverrideMode = optionKey
                                    viewModel.updatePageAdjustment(pageIndex, localBrightness, localContrast, optionKey)
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(28.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) SleekFilterOnContainer else Color.Transparent,
                                    contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                ),
                                border = BorderStroke(1.dp, if (isSelected) SleekFilterOnContainer else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(optionLabel, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Shadow Correct: ${localBrightness.toInt()}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        if (localBrightness != 0f) {
                            TextButton(
                                onClick = { 
                                    localBrightness = 0f
                                    viewModel.updatePageAdjustment(pageIndex, 0f, localContrast, localOverrideMode)
                                },
                                modifier = Modifier.height(20.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Reset", fontSize = 10.sp, color = SleekPrimary)
                            }
                        }
                    }
                    Slider(
                        value = localBrightness,
                        onValueChange = { 
                            localBrightness = it
                            viewModel.updatePageAdjustment(pageIndex, it, localContrast, localOverrideMode)
                        },
                        valueRange = -80f..80f,
                        colors = SliderDefaults.colors(thumbColor = SleekPrimary, activeTrackColor = SleekPrimary)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Contrast Level: ${String.format("%.2f", localContrast)}x", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        if (localContrast != 1.0f) {
                            TextButton(
                                onClick = { 
                                    localContrast = 1.0f
                                    viewModel.updatePageAdjustment(pageIndex, localBrightness, 1.0f, localOverrideMode)
                                },
                                modifier = Modifier.height(20.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Reset", fontSize = 10.sp, color = SleekPrimary)
                            }
                        }
                    }
                    Slider(
                        value = localContrast,
                        onValueChange = { 
                            localContrast = it
                            viewModel.updatePageAdjustment(pageIndex, localBrightness, it, localOverrideMode)
                        },
                        valueRange = 0.4f..2.2f,
                        colors = SliderDefaults.colors(thumbColor = SleekPrimary, activeTrackColor = SleekPrimary)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showAdjustDialog = false },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SleekFilterOnContainer)
                    ) {
                        Text("Done Correction", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun FilteredPageCard(
    context: android.content.Context,
    pdfUri: Uri,
    pageIndex: Int,
    listIndex: Int,
    maxCount: Int,
    globalMode: String,
    pageSettings: com.example.util.ManualAdjustSettings,
    isDragEnabled: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onManualAdjustClick: () -> Unit
) {
    var originalBmp by remember { mutableStateOf<Bitmap?>(null) }
    var processedBmp by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(pdfUri, pageIndex) {
        withContext(Dispatchers.IO) {
            val oldBmp = originalBmp
            val newBmp = PdfUtils.renderPageToBitmap(context, pdfUri, pageIndex, 300, 420)
            withContext(Dispatchers.Main) {
                originalBmp = newBmp
                if (oldBmp != null && oldBmp != newBmp) {
                    oldBmp.recycle()
                }
            }
        }
    }

    LaunchedEffect(originalBmp, globalMode, pageSettings) {
        val bmp = originalBmp ?: return@LaunchedEffect
        withContext(Dispatchers.Default) {
            val oldProcBmp = processedBmp
            val newProcBmp = PdfUtils.applyFilterToBitmap(bmp, globalMode, pageSettings)
            withContext(Dispatchers.Main) {
                processedBmp = newProcBmp
                if (oldProcBmp != null && oldProcBmp != newProcBmp && oldProcBmp != originalBmp) {
                    oldProcBmp.recycle()
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            originalBmp?.recycle()
            processedBmp?.recycle()
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(75.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                if (processedBmp != null) {
                    Image(
                        bitmap = processedBmp!!.asImageBitmap(),
                        contentDescription = "Corrected preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp), color = SleekPrimary)
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Sheet ${listIndex + 1}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "(Orig Pg ${pageIndex + 1})",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f).copy(alpha = 0.7f)
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                val customText = buildString {
                    val activeMode = pageSettings.overrideMode ?: globalMode
                    append("Mode: $activeMode")
                    if (pageSettings.brightness != 0f) {
                        append(" • Bright: ${if (pageSettings.brightness > 0) "+" else ""}${pageSettings.brightness.toInt()}%")
                    }
                    if (pageSettings.contrast != 1.0f) {
                        append(" • Scale: ${String.format("%.1f", pageSettings.contrast)}x")
                    }
                }
                Text(
                    text = customText,
                    fontSize = 10.5.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = onManualAdjustClick,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SleekFilterOnContainer),
                        border = BorderStroke(1.dp, SleekFilterOnContainer.copy(alpha = 0.6f))
                    ) {
                        Icon(imageVector = Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Manual Adjust", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    if (pageSettings.brightness != 0f || pageSettings.contrast != 1.0f || pageSettings.overrideMode != null) {
                        Icon(
                            imageVector = Icons.Default.Circle,
                            contentDescription = "Manual active indicator",
                            tint = Color.Red,
                            modifier = Modifier.size(8.dp)
                        )
                    }
                }
            }

            if (isDragEnabled) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onMoveUp,
                        enabled = listIndex > 0,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Move Up",
                            tint = if (listIndex > 0) SleekPrimary else MaterialTheme.colorScheme.outline
                        )
                    }

                    IconButton(
                        onClick = onMoveDown,
                        enabled = listIndex < maxCount - 1,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Move Down",
                            tint = if (listIndex < maxCount - 1) SleekPrimary else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DialogPreviewThumbnail(
    context: android.content.Context,
    pdfUri: Uri,
    pageIndex: Int,
    globalMode: String,
    pageSettings: com.example.util.ManualAdjustSettings
) {
    var originalBmp by remember { mutableStateOf<Bitmap?>(null) }
    var processedBmp by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(pdfUri, pageIndex) {
        withContext(Dispatchers.IO) {
            val oldBmp = originalBmp
            val newBmp = PdfUtils.renderPageToBitmap(context, pdfUri, pageIndex, 250, 350)
            withContext(Dispatchers.Main) {
                originalBmp = newBmp
                if (oldBmp != null && oldBmp != newBmp) {
                    oldBmp.recycle()
                }
            }
        }
    }

    LaunchedEffect(originalBmp, globalMode, pageSettings) {
        val bmp = originalBmp ?: return@LaunchedEffect
        withContext(Dispatchers.Default) {
            val oldProcBmp = processedBmp
            val newProcBmp = PdfUtils.applyFilterToBitmap(bmp, globalMode, pageSettings)
            withContext(Dispatchers.Main) {
                processedBmp = newProcBmp
                if (oldProcBmp != null && oldProcBmp != newProcBmp && oldProcBmp != originalBmp) {
                    oldProcBmp.recycle()
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            originalBmp?.recycle()
            processedBmp?.recycle()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (processedBmp != null) {
            Image(
                bitmap = processedBmp!!.asImageBitmap(),
                contentDescription = "Quick Live Customization Output",
                modifier = Modifier.fillMaxHeight(),
                contentScale = ContentScale.Fit
            )
        } else {
            CircularProgressIndicator(strokeWidth = 2.dp, color = SleekPrimary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResizePdfScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val sourceFile by viewModel.resizeSourceFile.collectAsStateWithLifecycle()
    val targetValue by viewModel.resizeTargetValue.collectAsStateWithLifecycle()
    val targetUnit by viewModel.resizeTargetUnit.collectAsStateWithLifecycle()
    val resizeMode by viewModel.resizeMode.collectAsStateWithLifecycle()
    val operationCompleted by viewModel.operationCompleted.collectAsStateWithLifecycle()
    val lastGeneratedRecord by viewModel.lastGeneratedRecord.collectAsStateWithLifecycle()

    val pickFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.setResizeSourceFile(context, uri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Resize PDF Document", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.clearResizeFile()
                            viewModel.navigateTo("HOME")
                        }
                    ) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Introductory Info Box
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                colors = CardDefaults.cardColors(containerColor = SleekResizeContainer.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, SleekResizeOnContainer.copy(alpha = 0.12f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AspectRatio,
                            contentDescription = null,
                            tint = SleekResizeOnContainer,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "How does PDF Resizer work?",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = SleekResizeOnContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Customize your target file sizes precisely! This utility scales internal imagery dynamically to bring your document size up or down to your manual target, maintaining the highest possible quality for the target size.",
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = SleekResizeOnContainer.copy(alpha = 0.85f)
                    )
                }
            }

            if (operationCompleted && lastGeneratedRecord != null) {
                // Completed UI
                val record = lastGeneratedRecord!!
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    border = BorderStroke(1.dp, Color(0xFF81C784))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Resize Completed Successfully!",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B5E20)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("New File Name:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                                    Text(
                                        text = record.fileName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false).padding(start = 12.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Outcome File Size:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                                    val formattedSize = if (record.fileSize > 1024 * 1024) {
                                        String.format("%.2f MB", record.fileSize / (1024f * 1024f))
                                    } else {
                                        String.format("%.2f KB", record.fileSize / 1024f)
                                    }
                                    Text(formattedSize, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SleekResizeOnContainer)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Total Pages:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                                    Text("${record.pagesCount}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { viewModel.shareRecord(context, record) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SleekResizeOnContainer)
                            ) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Share", fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = {
                                    val f = File(record.filePath)
                                    if (f.exists()) {
                                        viewModel.openFile(context, f)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                            ) {
                                Icon(imageVector = Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Open PDF", fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        TextButton(
                            onClick = {
                                viewModel.clearResizeFile()
                                viewModel.navigateTo("HOME")
                            }
                        ) {
                            Text("Back to Dashboard", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (sourceFile == null) {
                // Empty Picker Slate
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AspectRatio,
                            contentDescription = null,
                            tint = SleekResizeOnContainer.copy(alpha = 0.15f),
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No PDF Selected",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Select a PDF file from your documents storage to scale, compress, or pad to manual outcome size constraints.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { pickFileLauncher.launch("application/pdf") },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SleekResizeOnContainer),
                            modifier = Modifier.testTag("select_resize_file_btn")
                        ) {
                            Icon(imageVector = Icons.Default.FileOpen, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Choose PDF Document", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                val file = sourceFile!!
                
                // Show chosen file info
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = SleekResizeOnContainer,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = file.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row {
                                val sizeFormatted = if (file.size > 1024 * 1024) {
                                    String.format("%.2f MB", file.size / (1024f * 1024f))
                                } else {
                                    String.format("%.2f KB", file.size / 1024f)
                                }
                                Text(
                                    text = "Size: $sizeFormatted",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Pages: ${file.pageCount}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                )
                            }
                        }
                        IconButton(
                            onClick = { pickFileLauncher.launch("application/pdf") }
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Change PDF", tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Mode Selector Header
                Text(
                    text = "RESIZE DIRECTION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    letterSpacing = 1.2.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))

                // Compress / Enlarge mode tabs
                val isCompress = resizeMode == "COMPRESS"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(11.dp))
                            .background(if (isCompress) SleekResizeContainer else Color.Transparent)
                            .clickable { viewModel.setResizeMode("COMPRESS") }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Compress PDF",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCompress) SleekResizeOnContainer else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(11.dp))
                            .background(if (!isCompress) SleekResizeContainer else Color.Transparent)
                            .clickable { viewModel.setResizeMode("ENLARGE") }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Enlarge PDF",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (!isCompress) SleekResizeOnContainer else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Outcome target configuration form
                Text(
                    text = "TARGET OUTCOME FILE SIZE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    letterSpacing = 1.2.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Target size input field
                            OutlinedTextField(
                                value = targetValue,
                                onValueChange = {
                                    val cleaned = it.filter { char -> char.isDigit() || char == '.' }
                                    viewModel.setResizeTargetValue(cleaned)
                                },
                                label = { Text("Enter size") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1.5f)
                                    .testTag("resize_target_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SleekResizeOnContainer,
                                    focusedLabelColor = SleekResizeOnContainer
                                ),
                                shape = RoundedCornerShape(10.dp),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                )
                            )

                            // Unit switch box (KB or MB)
                            val isMB = targetUnit == "MB"
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                                    .padding(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(if (!isMB) SleekResizeContainer else Color.Transparent)
                                        .clickable { viewModel.setResizeTargetUnit("KB") },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("KB", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (!isMB) SleekResizeOnContainer else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(if (isMB) SleekResizeContainer else Color.Transparent)
                                        .clickable { viewModel.setResizeTargetUnit("MB") },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("MB", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isMB) SleekResizeOnContainer else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Quick presets row
                        Text("Quick Target Presets:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val presets = if (isCompress) {
                                listOf("100_KB", "500_KB", "1_MB", "2_MB")
                            } else {
                                listOf("2_MB", "5_MB", "10_MB", "25_MB")
                            }

                            presets.forEach { label ->
                                val parts = label.split("_")
                                val valPart = parts[0]
                                val unitPart = parts[1]

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White)
                                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                        .clickable {
                                            viewModel.setResizeTargetValue(valPart)
                                            viewModel.setResizeTargetUnit(unitPart)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$valPart $unitPart",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SleekResizeOnContainer
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Explanatory summary notes on processing outcome based on choice
                val inputNum = targetValue.toFloatOrNull() ?: 1.0f
                val descriptionText = if (isCompress) {
                    "Your PDF will be compressed using adaptive resolution scaling to fit within $inputNum $targetUnit while keeping all pages sharp and readable."
                } else {
                    "Your PDF will be upscaled. A high-quality render will be performed, and the output file will be padded with clean metadata to precisely match $inputNum $targetUnit as requested."
                }
                Text(
                    text = descriptionText,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.height(30.dp))

                // Final Action Trigger Button
                Button(
                    onClick = {
                        viewModel.generateResizedPdf(context)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("apply_resize_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = SleekResizeOnContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(imageVector = Icons.Default.AspectRatio, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isCompress) "Compress PDF Now" else "Enlarge PDF Now",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConvertPdfScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val tool by viewModel.selectedConvertTool.collectAsStateWithLifecycle()
    val convertSourceUri by viewModel.convertSourceUri.collectAsStateWithLifecycle()
    val convertSourceName by viewModel.convertSourceName.collectAsStateWithLifecycle()
    val multipleConvertUris by viewModel.multipleConvertUris.collectAsStateWithLifecycle()
    val convertHtmlUrl by viewModel.convertHtmlUrl.collectAsStateWithLifecycle()
    val htmlPdfPageSize by viewModel.htmlPdfPageSize.collectAsStateWithLifecycle()
    val convertTypeText by viewModel.convertTypeText.collectAsStateWithLifecycle()
    val jpgPdfOrientation by viewModel.jpgPdfOrientation.collectAsStateWithLifecycle()
    val jpgPdfMarginDp by viewModel.jpgPdfMarginDp.collectAsStateWithLifecycle()
    val convertOperationCompleted by viewModel.convertOperationCompleted.collectAsStateWithLifecycle()
    val convertLastGeneratedRecord by viewModel.convertLastGeneratedRecord.collectAsStateWithLifecycle()

    var txtSourceMode by remember { mutableStateOf("FILE") }

    val standardInputMime = remember(tool) {
        when (tool) {
            "pdf_to_word", "pdf_to_powerpoint", "pdf_to_excel", "pdf_to_jpg", "pdf_to_pdfa" -> "application/pdf"
            "word_to_pdf" -> "*/*"
            "powerpoint_to_pdf" -> "*/*"
            "excel_to_pdf" -> "*/*"
            "epub_to_pdf" -> "*/*"
            "txt_to_pdf" -> "text/plain"
            else -> "*/*"
        }
    }

    val singleFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.setConvertSourceUri(context, uri)
        }
    }

    val multipleImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.setMultipleConvertUris(uris)
        }
    }

    val toolTitle = remember(tool) {
        when (tool) {
            "pdf_to_word" -> "PDF to Word Converter"
            "pdf_to_powerpoint" -> "PDF to PowerPoint"
            "pdf_to_excel" -> "PDF to Excel Puller"
            "word_to_pdf" -> "Word to PDF Layout"
            "powerpoint_to_pdf" -> "PowerPoint to PDF"
            "excel_to_pdf" -> "Excel to PDF Table"
            "pdf_to_jpg" -> "PDF to JPG Extraction"
            "jpg_to_pdf" -> "JPG Images to PDF"
            "html_to_pdf" -> "HTML Webpage to PDF"
            "pdf_to_pdfa" -> "PDF to ISO PDF/A"
            "epub_to_pdf" -> "EPUB Book to PDF"
            "txt_to_pdf" -> "TXT Notes to PDF"
            else -> "Document Converter"
        }
    }

    val toolDescription = remember(tool) {
        when (tool) {
            "pdf_to_word" -> "Easily convert your PDF files into easy to edit DOC and DOCX documents with outstanding layout accuracy."
            "pdf_to_powerpoint" -> "Turn your PDF files into high-quality slideshow presentations for seamless visual display."
            "pdf_to_excel" -> "Pull structured content and tabular cells straight from PDFs into clean Excel spreadsheets."
            "word_to_pdf" -> "Make your DOC and DOCX files clean and easily readable by rendering them into standard PDF formats."
            "powerpoint_to_pdf" -> "Turn your slide decks and PPT presentations into beautifully rendered PDF documents."
            "excel_to_pdf" -> "Turn Excel spreadsheets and raw data tables into perfectly formatted visual PDF layouts."
            "pdf_to_jpg" -> "Extract all embedded images, pages or convert multiple page views into standard JPG frames."
            "jpg_to_pdf" -> "Instantly stitch multiple JPG/PNG photographs into clean PDFs. Customize orientation and margin offsets."
            "html_to_pdf" -> "Convert online articles or live HTML webpages to offline clean PDFs with single layout renders."
            "pdf_to_pdfa" -> "Transform standard PDFs to long-term archiving ISO-standardized PDF/A standard versions."
            "epub_to_pdf" -> "Convert digital EPUB formats and eBooks directly into printable and beautifully visual PDF layouts."
            "txt_to_pdf" -> "Type raw notes or convert clean flat text files directly into formatted PDF documents."
            else -> "Transform documents effortlessly style assets."
        }
    }

    val toolIcon = remember(tool) {
        when (tool) {
            "pdf_to_word" -> Icons.Default.Description
            "pdf_to_powerpoint" -> Icons.Default.Slideshow
            "pdf_to_excel" -> Icons.Default.GridOn
            "word_to_pdf" -> Icons.Default.PictureAsPdf
            "powerpoint_to_pdf" -> Icons.Default.PictureAsPdf
            "excel_to_pdf" -> Icons.Default.PictureAsPdf
            "pdf_to_jpg" -> Icons.Default.Image
            "jpg_to_pdf" -> Icons.Default.PictureAsPdf
            "html_to_pdf" -> Icons.Default.Language
            "pdf_to_pdfa" -> Icons.Default.Archive
            "epub_to_pdf" -> Icons.Default.Book
            "txt_to_pdf" -> Icons.Default.Assignment
            else -> Icons.Default.Description
        }
    }

    val containerColor = remember(tool) {
        when (tool) {
            "pdf_to_word" -> SleekUnlockContainer
            "pdf_to_powerpoint" -> SleekMultipageContainer
            "pdf_to_excel" -> SleekOrganizeContainer
            "word_to_pdf" -> SleekMergeContainer
            "powerpoint_to_pdf" -> SleekSplitContainer
            "excel_to_pdf" -> SleekInvertContainer
            "pdf_to_jpg" -> SleekResizeContainer
            "jpg_to_pdf" -> SleekSignatureContainer
            "html_to_pdf" -> SleekFilterContainer
            "pdf_to_pdfa" -> SleekUnlockContainer
            "epub_to_pdf" -> SleekInvertContainer
            "txt_to_pdf" -> SleekSignatureContainer
            else -> SleekMergeContainer
        }
    }

    val contentColor = remember(tool) {
        when (tool) {
            "pdf_to_word" -> SleekUnlockOnContainer
            "pdf_to_powerpoint" -> SleekMultipageOnContainer
            "pdf_to_excel" -> SleekOrganizeOnContainer
            "word_to_pdf" -> SleekMergeOnContainer
            "powerpoint_to_pdf" -> SleekSplitOnContainer
            "excel_to_pdf" -> SleekInvertOnContainer
            "pdf_to_jpg" -> SleekResizeOnContainer
            "jpg_to_pdf" -> SleekSignatureOnContainer
            "html_to_pdf" -> SleekFilterOnContainer
            "pdf_to_pdfa" -> SleekUnlockOnContainer
            "epub_to_pdf" -> SleekInvertOnContainer
            "txt_to_pdf" -> SleekSignatureOnContainer
            else -> SleekMergeOnContainer
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Document Converter",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.clearConvertState(); viewModel.navigateTo("HOME") }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.navigationBarsPadding()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = containerColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(contentColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = toolIcon,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = toolTitle,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = toolDescription,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = contentColor.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            if (!convertOperationCompleted) {
                when (tool) {
                    "jpg_to_pdf" -> {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 20.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            onClick = { multipleImagePicker.launch("image/*") }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (multipleConvertUris.isEmpty()) "Select Source JPG Images" else "${multipleConvertUris.size} Images Selected",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (multipleConvertUris.isEmpty()) "Tap here to pick pictures" else "Tap here to edit photo selection",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f).copy(alpha = 0.7f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "ORIENTATION ADJUSTMENT",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("PORTRAIT", "LANDSCAPE", "AUTO").forEach { opt ->
                                val active = jpgPdfOrientation == opt
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (active) SleekPrimary else Color.Transparent)
                                        .clickable { viewModel.updateJpgPdfSettings(opt, jpgPdfMarginDp) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = opt,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (active) Color.White else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "CUSTOM MARGIN OFFSET",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(0, 10, 20).forEach { margin ->
                                val active = jpgPdfMarginDp == margin
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (active) SleekPrimary else Color.Transparent)
                                        .clickable { viewModel.updateJpgPdfSettings(jpgPdfOrientation, margin) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (margin == 0) "No Margin (0dp)" else "${margin}dp Margins",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (active) Color.White else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                    "html_to_pdf" -> {
                        OutlinedTextField(
                            value = convertHtmlUrl,
                            onValueChange = { viewModel.updateConvertHtmlUrl(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            label = { Text("Copy & Paste Webpage URL") },
                            placeholder = { Text("www.google.com") },
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SleekPrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Language, contentDescription = null, tint = SleekPrimary)
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "PAGE FORMAT",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(2.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            listOf("A3", "A4", "A5", "Letter").forEach { format ->
                                val active = htmlPdfPageSize == format
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (active) SleekPrimary else Color.Transparent)
                                        .clickable { viewModel.updateHtmlPdfPageSize(format) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = format,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (active) Color.White else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (convertHtmlUrl.isNotBlank()) {
                            val webUrl = if (!convertHtmlUrl.startsWith("http://") && !convertHtmlUrl.startsWith("https://")) "https://$convertHtmlUrl" else convertHtmlUrl
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(280.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White)
                                    .padding(4.dp)
                            ) {
                                androidx.compose.ui.viewinterop.AndroidView(
                                    factory = { ctx ->
                                        android.webkit.WebView(ctx).apply {
                                            settings.javaScriptEnabled = true
                                            settings.domStorageEnabled = true
                                            settings.useWideViewPort = true
                                            settings.loadWithOverviewMode = true
                                            webViewClient = android.webkit.WebViewClient()
                                            loadUrl(webUrl)
                                        }
                                    },
                                    update = { view ->
                                        if (view.url != webUrl && view.url != "$webUrl/") {
                                            view.loadUrl(webUrl)
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    "txt_to_pdf" -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("FILE" to "Select TXT File", "TEXT" to "Type Raw Notes").forEach { (modeCode, labelStr) ->
                                val active = txtSourceMode == modeCode
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (active) SleekPrimary else Color.Transparent)
                                        .clickable { txtSourceMode = modeCode },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = labelStr,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (active) Color.White else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))

                        if (txtSourceMode == "FILE") {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                onClick = { singleFilePicker.launch("text/plain") }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Assignment,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = if (convertSourceUri == null) "Select text/plain File" else convertSourceName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (convertSourceUri == null) "Tap to locate text documents" else "Change text file",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f).copy(alpha = 0.7f)
                                    )
                                }
                            }
                        } else {
                            OutlinedTextField(
                                value = convertTypeText,
                                onValueChange = { viewModel.updateConvertTypeText(it) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .padding(vertical = 6.dp),
                                label = { Text("Write Text Notes Content") },
                                placeholder = { Text("Begin typing your typewriter text file here...") },
                                shape = RoundedCornerShape(16.dp),
                                maxLines = 10,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SleekPrimary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )
                        }
                    }
                    else -> {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            onClick = { singleFilePicker.launch(standardInputMime) }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.UploadFile,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (convertSourceUri == null) "Select Source Document" else convertSourceName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (convertSourceUri == null) "Tap to open device files" else "Change selected document file",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f).copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                val hasInput = remember(
                    tool, convertSourceUri, multipleConvertUris, convertHtmlUrl, convertTypeText, txtSourceMode
                ) {
                    when (tool) {
                        "jpg_to_pdf" -> multipleConvertUris.isNotEmpty()
                        "html_to_pdf" -> convertHtmlUrl.isNotBlank()
                        "txt_to_pdf" -> if (txtSourceMode == "FILE") convertSourceUri != null else convertTypeText.isNotBlank()
                        else -> convertSourceUri != null
                    }
                }

                Button(
                    onClick = { viewModel.runConversion(context) },
                    enabled = hasInput,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = contentColor,
                        contentColor = Color.White,
                        disabledContainerColor = MaterialTheme.colorScheme.surface,
                        disabledContentColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f).copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(imageVector = toolIcon, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = "Start Conversion", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = SleekUnlockContainer.copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, SleekUnlockOnContainer.copy(alpha = 0.12f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(SleekUnlockOnContainer.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Archive,
                                contentDescription = null,
                                tint = SleekUnlockOnContainer,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Conversion Completed!",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = SleekUnlockOnContainer
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Your document has process-styled accurately but will not open automatically as requested. You are free to offline share and save it directly down below.",
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = SleekUnlockOnContainer.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                        
                        Divider(
                            color = SleekUnlockOnContainer.copy(alpha = 0.12f),
                            modifier = Modifier.padding(vertical = 16.dp)
                        )

                        convertLastGeneratedRecord?.let { record ->
                            Text(
                                text = record.fileName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = android.text.format.Formatter.formatShortFileSize(context, record.fileSize),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { convertLastGeneratedRecord?.let { viewModel.saveToDownloads(context, it) } },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Save to Public Downloads", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = { convertLastGeneratedRecord?.let { viewModel.shareRecord(context, it) } },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.2.dp, SleekPrimary),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SleekPrimary)
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Share Out Document", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        TextButton(
                            onClick = { viewModel.clearConvertState() }
                        ) {
                            Text(
                                text = "Convert Another Document",
                                fontWeight = FontWeight.Bold,
                                color = SleekPrimary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConvertAllToolsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Convert files", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(
                        onClick = {
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "DOCUMENT CONVERTERS",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Direct continuous 2-column grid of document converters
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
            ) {
                // Row 1
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ToolCard(
                        title = "PDF to Word",
                        description = "Convert PDF to editable DOC",
                        icon = Icons.Default.Description,
                        containerColor = SleekUnlockContainer,
                        contentColor = SleekUnlockOnContainer,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.selectConvertTool("pdf_to_word") }
                    )
                    ToolCard(
                        title = "PDF to PPT",
                        description = "Convert PDF to slide presentation",
                        icon = Icons.Default.Slideshow,
                        containerColor = SleekMultipageContainer,
                        contentColor = SleekMultipageOnContainer,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.selectConvertTool("pdf_to_powerpoint") }
                    )
                }

                // Row 2
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ToolCard(
                        title = "PDF to Excel",
                        description = "Pull tabular data straight to Excel",
                        icon = Icons.Default.GridOn,
                        containerColor = SleekOrganizeContainer,
                        contentColor = SleekOrganizeOnContainer,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.selectConvertTool("pdf_to_excel") }
                    )
                    ToolCard(
                        title = "Word to PDF",
                        description = "Convert DOC/DOCX files to PDF",
                        icon = Icons.Default.PictureAsPdf,
                        containerColor = SleekMergeContainer,
                        contentColor = SleekMergeOnContainer,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.selectConvertTool("word_to_pdf") }
                    )
                }

                // Row 3
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ToolCard(
                        title = "PPT to PDF",
                        description = "Make presentation slides readable",
                        icon = Icons.Default.PictureAsPdf,
                        containerColor = SleekSplitContainer,
                        contentColor = SleekSplitOnContainer,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.selectConvertTool("powerpoint_to_pdf") }
                    )
                    ToolCard(
                        title = "Excel to PDF",
                        description = "Make spreadsheets layout clean",
                        icon = Icons.Default.PictureAsPdf,
                        containerColor = SleekInvertContainer,
                        contentColor = SleekInvertOnContainer,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.selectConvertTool("excel_to_pdf") }
                    )
                }

                // Row 4
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ToolCard(
                        title = "PDF to JPG",
                        description = "Extract pages or single frames",
                        icon = Icons.Default.Image,
                        containerColor = SleekResizeContainer,
                        contentColor = SleekResizeOnContainer,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.selectConvertTool("pdf_to_jpg") }
                    )
                    ToolCard(
                        title = "JPG to PDF",
                        description = "Convert images with margin presets",
                        icon = Icons.Default.PictureAsPdf,
                        containerColor = SleekSignatureContainer,
                        contentColor = SleekSignatureOnContainer,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.selectConvertTool("jpg_to_pdf") }
                    )
                }

                // Row 5
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ToolCard(
                        title = "HTML to PDF",
                        description = "Save live webpages to PDF file",
                        icon = Icons.Default.Language,
                        containerColor = SleekFilterContainer,
                        contentColor = SleekFilterOnContainer,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.selectConvertTool("html_to_pdf") }
                    )
                    ToolCard(
                        title = "PDF to PDF/A",
                        description = "Archive standards conformance ISO",
                        icon = Icons.Default.Archive,
                        containerColor = SleekUnlockContainer,
                        contentColor = SleekUnlockOnContainer,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.selectConvertTool("pdf_to_pdfa") }
                    )
                }

                // Row 6
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ToolCard(
                        title = "EPUB to PDF",
                        description = "Convert eBooks to readable layout",
                        icon = Icons.Default.Book,
                        containerColor = SleekInvertContainer,
                        contentColor = SleekInvertOnContainer,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.selectConvertTool("epub_to_pdf") }
                    )
                    ToolCard(
                        title = "TXT to PDF",
                        description = "Convert flat files or type directly",
                        icon = Icons.Default.Assignment,
                        containerColor = SleekSignatureContainer,
                        contentColor = SleekSignatureOnContainer,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.selectConvertTool("txt_to_pdf") }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PageNumberScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val pageNumberSourceFile by viewModel.pageNumberSourceFile.collectAsStateWithLifecycle()
    val pageNumberPosition by viewModel.pageNumberPosition.collectAsStateWithLifecycle()
    val pageNumberColor by viewModel.pageNumberColor.collectAsStateWithLifecycle()
    val pageNumberAlpha by viewModel.pageNumberAlpha.collectAsStateWithLifecycle()
    val pageNumberFontSize by viewModel.pageNumberFontSize.collectAsStateWithLifecycle()
    val pageNumberFontFamily by viewModel.pageNumberFontFamily.collectAsStateWithLifecycle()
    val pageNumberRanges by viewModel.pageNumberRanges.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add page number", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.clearPageNumberSource()
                            viewModel.navigateTo("HOME")
                        }
                    ) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.clearPageNumberSource()
                            viewModel.navigateTo("HOME")
                        },
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            viewModel.generateNumberedPdf(context)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                        modifier = Modifier.weight(1f).height(48.dp).testTag("generate_numbered_pdf_button")
                    ) {
                        Text("Save & Render", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // File metadata row
            pageNumberSourceFile?.let { file ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SleekMergeContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = SleekMergeOnContainer,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = file.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = SleekMergeOnContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${file.pageCount} pages • ${String.format("%.1f", file.size / (1024f * 1024f))} MB",
                                fontSize = 11.sp,
                                color = SleekMergeOnContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // 1. Position Grid Config
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.GridOn,
                            contentDescription = null,
                            tint = SleekPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "POSITION ON PAGE",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    val positionsList = listOf(
                        listOf("Top Left", "Top Center", "Top Right"),
                        listOf("Middle Left", "Middle Center", "Middle Right"),
                        listOf("Bottom Left", "Bottom Center", "Bottom Right")
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        positionsList.forEach { rowList ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                rowList.forEach { pos ->
                                    val isSelected = (pageNumberPosition == pos)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(40.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) SleekPrimary else MaterialTheme.colorScheme.surface)
                                            .border(1.dp, if (isSelected) SleekPrimary else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                            .clickable { viewModel.setPageNumberPosition(pos) }
                                            .testTag("pos_chip_${pos.replace(" ", "_").lowercase()}"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = pos,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onBackground
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. TEXT STYLE: Color, Alpha, Font Size, Font Family
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = SleekPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "COLOR, ALPHA & TEXT STYLE",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            letterSpacing = 1.sp
                        )
                    }

                    // Predefined options
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Text Color Presets:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )

                        val colorPresets = listOf(
                            "#000000" to "Black",
                            "#7F7F7F" to "Gray",
                            "#FF0000" to "Red",
                            "#0000FF" to "Blue",
                            "#008000" to "Green",
                            "#FFA500" to "Orange"
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            colorPresets.forEach { (hex, name) ->
                                val isSelected = pageNumberColor.equals(hex, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(android.graphics.Color.parseColor(hex)))
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) SleekPrimary else Color.LightGray,
                                            shape = CircleShape
                                        )
                                        .clickable { viewModel.setPageNumberColor(hex) }
                                        .testTag("color_preset_${name.lowercase()}")
                                )
                            }
                        }

                        // Custom Hex
                        OutlinedTextField(
                            value = pageNumberColor,
                            onValueChange = { input ->
                                if (input.startsWith("#") && input.length <= 9) {
                                    viewModel.setPageNumberColor(input)
                                } else if (!input.startsWith("#") && input.length <= 8) {
                                    viewModel.setPageNumberColor("#$input")
                                }
                            },
                            label = { Text("Custom HEX Color") },
                            modifier = Modifier.fillMaxWidth(),
                            prefix = { Text("#") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SleekPrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            singleLine = true
                        )
                    }

                    // Alpha slide
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Alpha (Transparency/Opacity):",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                            Text(
                                text = String.format("%.2f", pageNumberAlpha),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SleekPrimary
                            )
                        }

                        Slider(
                            value = pageNumberAlpha,
                            onValueChange = { viewModel.setPageNumberAlpha(it) },
                            valueRange = 0.0f..1.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = SleekPrimary,
                                activeTrackColor = SleekPrimary
                            ),
                            modifier = Modifier.testTag("alpha_slider")
                        )
                    }

                    // Font Size
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Font Size:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            IconButton(
                                onClick = { if (pageNumberFontSize > 4) viewModel.setPageNumberFontSize(pageNumberFontSize - 1) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape)
                            ) {
                                Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease Font Size", modifier = Modifier.size(18.dp))
                            }
                            Text(
                                text = pageNumberFontSize.toString(),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            IconButton(
                                onClick = { if (pageNumberFontSize < 72) viewModel.setPageNumberFontSize(pageNumberFontSize + 1) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "Increase Font Size", modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    // Font Family
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Font Family:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("Sans", "Serif", "Mono").forEach { family ->
                                val isSelected = (pageNumberFontFamily == family)
                                AssistChip(
                                    onClick = { viewModel.setPageNumberFontFamily(family) },
                                    label = { Text(family, fontFamily = when(family) {
                                        "Serif" -> androidx.compose.ui.text.font.FontFamily.Serif
                                        "Mono" -> androidx.compose.ui.text.font.FontFamily.Monospace
                                        else -> androidx.compose.ui.text.font.FontFamily.SansSerif
                                    }) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = if (isSelected) SleekPrimary else Color.Transparent,
                                        labelColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                    ),
                                    border = BorderStroke(1.dp, if (isSelected) SleekPrimary else MaterialTheme.colorScheme.outline),
                                    modifier = Modifier.testTag("font_family_${family.lowercase()}")
                                )
                            }
                        }
                    }
                }
            }

            // Option Boxes Card Layout
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PAGE NUMBERING RANGES / OPTION BOXES",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.weight(1f)
                )
                if (pageNumberRanges.size > 1) {
                    TextButton(
                        onClick = { viewModel.autoResolveRanges() },
                        colors = ButtonDefaults.textButtonColors(contentColor = SleekPrimary),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Build, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Auto-Align Ranges", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }

            pageNumberRanges.forEachIndexed { index, range ->
                val conflictIndexes = pageNumberRanges.mapIndexedNotNull { otherIdx, otherRange ->
                    if (otherIdx != index && range.startPage <= otherRange.endPage && range.endPage >= otherRange.startPage) {
                        otherIdx + 1
                    } else {
                        null
                    }
                }
                val hasConflict = conflictIndexes.isNotEmpty()

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(
                        width = if (hasConflict) 1.5.dp else 1.dp,
                        color = if (hasConflict) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (hasConflict) {
                            val activeConflictBox = conflictIndexes.first()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.65f), RoundedCornerShape(10.dp))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Conflict Warning",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Overlap Conflict: Page ${maxOf(range.startPage, pageNumberRanges[activeConflictBox - 1].startPage)} to ${minOf(range.endPage, pageNumberRanges[activeConflictBox - 1].endPage)} overlaps with Option Box $activeConflictBox. Clean up overlapping numbers by using 'Auto-Align Ranges' above.",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 14.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        var startPageValue by remember(range.id, range.startPage) { mutableStateOf(range.startPage.toString()) }
                        var endPageValue by remember(range.id, range.endPage) { mutableStateOf(range.endPage.toString()) }
                        var startingInputValue by remember(range.id, range.startingInput) { mutableStateOf(range.startingInput.toString()) }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Option Box ${index + 1}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = SleekPrimary
                            )
                            if (pageNumberRanges.size > 1) {
                                IconButton(
                                    onClick = { viewModel.removePageNumberRange(range.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Remove Option Box",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }

                        // Start / End Page
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = startPageValue,
                                onValueChange = { input ->
                                    if (input.isEmpty() || input.all { it.isDigit() }) {
                                        startPageValue = input
                                        val parsed = input.toIntOrNull()
                                        if (parsed != null && parsed > 0) {
                                            viewModel.updatePageNumberRange(range.copy(startPage = parsed))
                                        }
                                    }
                                },
                                label = { Text("Start Page") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                )
                            )

                            OutlinedTextField(
                                value = endPageValue,
                                onValueChange = { input ->
                                    if (input.isEmpty() || input.all { it.isDigit() }) {
                                        endPageValue = input
                                        val parsed = input.toIntOrNull()
                                        if (parsed != null && parsed > 0) {
                                            viewModel.updatePageNumberRange(range.copy(endPage = parsed))
                                        }
                                    }
                                },
                                label = { Text("End Page") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                )
                            )
                        }

                        // Page formatting template
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedTextField(
                                value = range.pageTypePattern,
                                onValueChange = { input ->
                                    viewModel.updatePageNumberRange(range.copy(pageTypePattern = input))
                                },
                                label = { Text("Page Type format") },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Page : {NUM}/{CNT}") },
                                singleLine = true
                            )
                            Text(
                                text = "Use {NUM} to insert starting-offset count, or {CNT} to view total pages in selected format.",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f).copy(alpha = 0.8f)
                            )

                            // Quick options
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp).horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("Page : {NUM}/{CNT}", "page : {NUM}", "{NUM}/{CNT}", "- {NUM} -").forEach { preset ->
                                    AssistChip(
                                        onClick = { viewModel.updatePageNumberRange(range.copy(pageTypePattern = preset)) },
                                        label = { Text(preset, fontSize = 10.sp) }
                                    )
                                }
                            }
                        }

                        // Numeral style type
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Numerals Type Setting:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                            val numeralsTypes = listOf(
                                "Numeric",
                                "Roman (Small)",
                                "Roman (Caps)",
                                "Alphabetic (Small)",
                                "Alphabetic (Caps)"
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                numeralsTypes.forEach { typeVal ->
                                    val isSelected = (range.numeralsType == typeVal)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) SleekPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface)
                                            .border(1.dp, if (isSelected) SleekPrimary else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                            .clickable { viewModel.updatePageNumberRange(range.copy(numeralsType = typeVal)) }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = typeVal,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) SleekPrimary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }

                        // Starting input counter value
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = startingInputValue,
                                onValueChange = { input ->
                                    if (input.isEmpty() || input.all { it.isDigit() }) {
                                        startingInputValue = input
                                        val parsed = input.toIntOrNull()
                                        if (parsed != null && parsed >= 0) {
                                            viewModel.updatePageNumberRange(range.copy(startingInput = parsed))
                                        }
                                    }
                                },
                                label = { Text("Starting Input value") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                )
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Start count at:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "The page counter is shifted based on this starting digit.",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f).copy(alpha = 0.6f)
                                )
                            }
                        }

                        // Live preview area
                        val totalPages = pageNumberSourceFile?.pageCount ?: 20
                        val livePreviewStr = getUiRangePreview(range, totalPages)
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SleekPrimary.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                                .border(BorderStroke(1.2.dp, SleekPrimary.copy(alpha = 0.15f)), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = SleekPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Evaluation Live Preview:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SleekPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = livePreviewStr,
                                    fontSize = 12.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }
                }
            }

            // "Add More" configuration card (Dashed shape / visual addition trigger)
            OutlinedButton(
                onClick = { viewModel.addPageNumberRange() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("add_more_range_button"),
                border = BorderStroke(1.5.dp, SleekPrimary.copy(alpha = 0.7f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SleekPrimary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add range config / Option Box", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

// Live-render preview generator
private fun getUiRangePreview(range: com.example.util.PageNumberRange, totalPagesCount: Int): String {
    val listLength = range.endPage - range.startPage + 1
    if (listLength <= 0) return "Invalid range configuration (Start page exceeds End page)"

    val itemsToShow = listLength.coerceAtMost(3)
    val partsList = mutableListOf<String>()

    for (pos in 0 until itemsToShow) {
        val currentNumToFormat = range.startingInput + pos
        val formattedNum = when (range.numeralsType) {
            "Roman (Small)" -> toUiRoman(currentNumToFormat).lowercase()
            "Roman (Caps)" -> toUiRoman(currentNumToFormat).uppercase()
            "Alphabetic (Small)" -> toUiAlphabetic(currentNumToFormat, false)
            "Alphabetic (Caps)" -> toUiAlphabetic(currentNumToFormat, true)
            else -> currentNumToFormat.toString()
        }
        val formattedCount = when (range.numeralsType) {
            "Roman (Small)" -> toUiRoman(totalPagesCount).lowercase()
            "Roman (Caps)" -> toUiRoman(totalPagesCount).uppercase()
            "Alphabetic (Small)" -> toUiAlphabetic(totalPagesCount, false)
            "Alphabetic (Caps)" -> toUiAlphabetic(totalPagesCount, true)
            else -> totalPagesCount.toString()
        }
        val textToDraw = range.pageTypePattern
            .replace("{NUM}", formattedNum)
            .replace("{CNT}", formattedCount)
        partsList.add(textToDraw)
    }

    var result = partsList.joinToString(", ")
    if (listLength > 3) {
        result += ", ..."
    }
    return result
}

private fun toUiRoman(number: Int): String {
    if (number <= 0) return ""
    val values = intArrayOf(1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1)
    val symbols = arrayOf("M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I")
    var num = number
    val sb = java.lang.StringBuilder()
    for (i in values.indices) {
        while (num >= values[i]) {
            num -= values[i]
            sb.append(symbols[i])
        }
    }
    return sb.toString()
}

private fun toUiAlphabetic(value: Int, isCaps: Boolean): String {
    if (value <= 0) return ""
    val charIndex = (value - 1) % 26
    val suffix = (value - 1) / 26
    val baseChar = if (isCaps) ('A' + charIndex) else ('a' + charIndex)
    return if (suffix > 0) {
        "$baseChar$suffix"
    } else {
        "$baseChar"
    }
}

// ==================== REPAIR PDF SCREEN ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepairPdfScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val source by viewModel.repairSourceFile.collectAsStateWithLifecycle()
    val repairStep by viewModel.repairStep.collectAsStateWithLifecycle()
    val repairProgress by viewModel.repairProgress.collectAsStateWithLifecycle()
    val operationCompleted by viewModel.operationCompleted.collectAsStateWithLifecycle()
    val lastGeneratedRecord by viewModel.lastGeneratedRecord.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Repair PDF", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.clearRepairFile()
                            viewModel.navigateTo("HOME")
                        }
                    ) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 40.dp)
        ) {
            val fileSource = source
            if (fileSource == null) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No file selected. Please return home and select a file.", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                    }
                }
                return@LazyColumn
            }

            // Case A: Successfully Repaired & Rebuilt
            if (operationCompleted && lastGeneratedRecord != null) {
                val record = lastGeneratedRecord!!
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("repair_success_card"),
                        colors = CardDefaults.cardColors(containerColor = SleekUnlockContainer),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, SleekUnlockOnContainer.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(SleekUnlockOnContainer.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Success",
                                    tint = SleekUnlockOnContainer,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "PDF FILE REPAIRED!",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = SleekUnlockOnContainer,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Damaged byte blocks, missing dictionary directories and cross-reference offsets have been successfully normalized.",
                                fontSize = 12.sp,
                                color = SleekUnlockOnContainer.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Details block
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White.copy(alpha = 0.6f))
                                    .padding(16.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Rebuilt File:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
                                        Text(record.fileName, fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, maxLines = 1)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Extracted Pages:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
                                        Text("${record.pagesCount} Valid Pages", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Repaired Size:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
                                        val formattedSize = android.text.format.Formatter.formatFileSize(context, record.fileSize)
                                        Text(formattedSize, fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Primary Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.shareRecord(context, record)
                                    },
                                    modifier = Modifier.weight(1f).height(48.dp).testTag("share_repaired_pdf_btn"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SleekUnlockOnContainer),
                                    border = BorderStroke(1.dp, SleekUnlockOnContainer)
                                ) {
                                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Share PDF", fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = {
                                        val file = File(record.filePath)
                                        viewModel.openFile(context, file)
                                    },
                                    modifier = Modifier.weight(1f).height(48.dp).testTag("open_repaired_pdf_btn"),
                                    colors = ButtonDefaults.buttonColors(containerColor = SleekUnlockOnContainer),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Open PDF", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            TextButton(
                                onClick = {
                                    viewModel.clearRepairFile()
                                    viewModel.navigateTo("HOME")
                                }
                            ) {
                                Text("Return to Dashboard", color = SleekUnlockOnContainer, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                return@LazyColumn
            }

            // Case B: Active Repairing Diagnostic Logger
            val currentRepairStep = repairStep
            if (currentRepairStep.isNotEmpty() && !operationCompleted) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = SleekDarkSurface)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFFFFB300),
                                strokeWidth = 4.dp,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "STRUCTURAL REPAIR IN PROGRESS",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFB300),
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Analyzing binaries and extracting document streams...",
                                fontSize = 11.sp,
                                color = SleekDarkTextSecondary,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Progress row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Rebuilding Progress", color = SleekDarkTextSecondary, fontSize = 12.sp)
                                val progressPercent = (repairProgress * 100).toInt()
                                Text("$progressPercent%", color = SleekDarkText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            LinearProgressIndicator(
                                progress = repairProgress,
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                                color = Color(0xFFFFB300),
                                trackColor = SleekDarkBorder.copy(alpha = 0.5f)
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // Console active step logger
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black)
                                    .padding(14.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "> ",
                                        fontSize = 11.sp,
                                        color = Color(0xFFFFB300),
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = currentRepairStep,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
                return@LazyColumn
            }

            // Case C: Idle Screen - Selected file details + repair guide
            item {
                Text(
                    text = "CORRUPT FILE INFORMATION",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = SleekPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Target Corrupted Document",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("File Name:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f), fontWeight = FontWeight.Medium)
                                Text(fileSource.name, fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Damage Size:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f), fontWeight = FontWeight.Medium)
                                val formattedSize = android.text.format.Formatter.formatFileSize(context, fileSource.size)
                                Text(formattedSize, fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Diagnostics Status:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f), fontWeight = FontWeight.Medium)
                                Text("XREF Corrupt / Table Missing", fontSize = 11.sp, color = SleekRepairOnContainer, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.clearRepairFile()
                                    viewModel.navigateTo("HOME")
                                },
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Text("Change File", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "REBUILDING PIPELINE OVERVIEW",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        PipelineStepItem(
                            stepNumber = "1",
                            title = "File Parsing & Header Cleaning",
                            desc = "Strips raw trailing error buffer and prepended text diagnostics before %PDF header bounds to normalize streams."
                        )
                        PipelineStepItem(
                            stepNumber = "2",
                            title = "Cross-Reference Offsets Construction",
                            desc = "Corrects and re-indexes broken object byte boundaries (XREF tables) allowing major PDF readers to open file normally."
                        )
                        PipelineStepItem(
                            stepNumber = "3",
                            title = "Object Stream Data Extraction",
                            desc = "Iterates and extracts remaining healthy visual pages, layout streams, fonts, and embedded images, omitting highly corrupted segments."
                        )
                        PipelineStepItem(
                            stepNumber = "4",
                            title = "Pristine Document Assembly",
                            desc = "Compiles all extracted, healthy layout primitives into a freshly-compiled, fully spec-compliant PDF file."
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        viewModel.repairCorruptPdf(context)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("repair_rebuild_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = SleekRepairOnContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Build, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Repair & Rebuild PDF", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
fun PipelineStepItem(stepNumber: String, title: String, desc: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(SleekRepairContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                fontSize = 12.sp,
                color = SleekRepairOnContainer,
                fontWeight = FontWeight.Bold
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = desc,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                lineHeight = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PagesPerSheetScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val sourceFile by viewModel.ppsSourceFile.collectAsStateWithLifecycle()
    val columns by viewModel.ppsColumns.collectAsStateWithLifecycle()
    val rows by viewModel.ppsRows.collectAsStateWithLifecycle()
    val isPortrait by viewModel.ppsIsPortrait.collectAsStateWithLifecycle()
    val pageSize by viewModel.ppsPageSize.collectAsStateWithLifecycle()
    val marginPercent by viewModel.ppsMarginPercent.collectAsStateWithLifecycle()
    val addBorder by viewModel.ppsAddBorder.collectAsStateWithLifecycle()
    val borderColorHex by viewModel.ppsBorderColor.collectAsStateWithLifecycle()
    val borderWidthPt by viewModel.ppsBorderWidth.collectAsStateWithLifecycle()

    val pickFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.setPpsSourceFile(context, uri)
        }
    }

    val gridOptions = listOf(
        "1x2" to (1 to 2),
        "1x3" to (1 to 3),
        "1x4" to (1 to 4),
        "2x1" to (2 to 1),
        "2x2" to (2 to 2),
        "2x3" to (2 to 3),
        "2x4" to (2 to 4),
        "3x1" to (3 to 1),
        "3x2" to (3 to 2),
        "4x1" to (4 to 1),
        "4x2" to (4 to 2),
        "4x3" to (4 to 3),
        "4x4" to (4 to 4)
    )

    val paperSizes = listOf("A0", "A1", "A2", "A3", "A4", "A5", "A6", "Letter", "Legal")

    val borderColors = listOf(
        "Black" to "#000000",
        "Classic Gray" to "#7F8C8D",
        "Deep Blue" to "#2980B9",
        "Crimson Red" to "#C0392B",
        "Forest Green" to "#27AE60",
        "Golden Yellow" to "#F39C12"
    )

    val borderWidths = listOf(
        "Slim" to 1f,
        "Medium" to 2.5f,
        "Bold" to 4.5f,
        "Thick" to 7f
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pages per sheet", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.clearPpsFile()
                            viewModel.navigateTo("HOME")
                        }
                    ) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (sourceFile == null) {
                // Empty State
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = SleekPagesPerSheetOnContainer.copy(alpha = 0.4f),
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No PDF file selected",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Select a PDF to tile several pages into customizable-sheet layouts.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { pickFileLauncher.launch("application/pdf") },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SleekPagesPerSheetOnContainer)
                        ) {
                            Icon(imageVector = Icons.Default.FileOpen, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Choose PDF Document", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                val file = sourceFile!!

                // Display File Info Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = Color(0xFFC62828),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = file.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${file.pageCount} Pages • ${formatSize(file.size)}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                        }
                        IconButton(
                            onClick = { viewModel.clearPpsFile() }
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear file", tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                        }
                    }
                }

                // Grid configuration
                Text(
                    text = "PAGES PER SHEET LAYOUT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                // Layout choices flow-row
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    gridOptions.forEach { (label, coord) ->
                        val (c, r) = coord
                        val isSelected = columns == c && rows == r
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.updatePpsLayout(c, r) },
                            label = { Text(label, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SleekPagesPerSheetContainer,
                                selectedLabelColor = SleekPagesPerSheetOnContainer,
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                selectedBorderColor = SleekPagesPerSheetOnContainer,
                                borderColor = Color.Transparent
                            )
                        )
                    }
                }

                // Orientation Choose
                Text(
                    text = "PAPER ORIENTATION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Portrait Button
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { viewModel.updatePpsOrientation(true) },
                        color = if (isPortrait) SleekPagesPerSheetContainer else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            width = if (isPortrait) 1.5.dp else 1.dp,
                            color = if (isPortrait) SleekPagesPerSheetOnContainer else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CropPortrait,
                                contentDescription = null,
                                tint = if (isPortrait) SleekPagesPerSheetOnContainer else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Portrait",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isPortrait) SleekPagesPerSheetOnContainer else MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }

                    // Landscape Button
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { viewModel.updatePpsOrientation(false) },
                        color = if (!isPortrait) SleekPagesPerSheetContainer else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            width = if (!isPortrait) 1.5.dp else 1.dp,
                            color = if (!isPortrait) SleekPagesPerSheetOnContainer else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CropLandscape,
                                contentDescription = null,
                                tint = if (!isPortrait) SleekPagesPerSheetOnContainer else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Landscape",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (!isPortrait) SleekPagesPerSheetOnContainer else MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }

                // Paper Size selection
                Text(
                    text = "PAGE SIZE (DEFAULT IS A4)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    paperSizes.forEach { pSize ->
                        val isSelected = pageSize.equals(pSize, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.updatePpsPageSize(pSize) },
                            label = { Text(pSize, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SleekPagesPerSheetContainer,
                                selectedLabelColor = SleekPagesPerSheetOnContainer,
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                selectedBorderColor = SleekPagesPerSheetOnContainer,
                                borderColor = Color.Transparent
                            )
                        )
                    }
                }

                // Margin adjustment
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MARGIN",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "${marginPercent.toInt()}%",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = SleekPagesPerSheetOnContainer
                    )
                }
                Slider(
                    value = marginPercent,
                    onValueChange = { viewModel.updatePpsMarginPercent(it) },
                    valueRange = 0f..20f,
                    steps = 20,
                    colors = SliderDefaults.colors(
                        thumbColor = SleekPagesPerSheetOnContainer,
                        activeTrackColor = SleekPagesPerSheetOnContainer,
                        inactiveTrackColor = SleekPagesPerSheetContainer
                    ),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Add Border Switch
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Add Cell Borders",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "Outline individual pages on the sheet",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                )
                            }
                            Switch(
                                checked = addBorder,
                                onCheckedChange = { viewModel.updatePpsAddBorder(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = SleekPagesPerSheetOnContainer,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                    uncheckedTrackColor = MaterialTheme.colorScheme.surface
                                )
                            )
                        }

                        if (addBorder) {
                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.5f).copy(alpha = 0.4f))
                            Spacer(modifier = Modifier.height(12.dp))

                            // Border Color
                            Text(
                                text = "BORDER COLOR",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            FlowRow(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                borderColors.forEach { (name, hex) ->
                                    val isSelected = borderColorHex.equals(hex, ignoreCase = true)
                                    val colorObj = Color(android.graphics.Color.parseColor(hex))
                                    Surface(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .clickable { viewModel.updatePpsBorderColor(hex) },
                                        color = if (isSelected) SleekPagesPerSheetContainer else Color.Transparent,
                                        border = BorderStroke(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) SleekPagesPerSheetOnContainer else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clip(CircleShape)
                                                    .background(colorObj)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                        }
                                    }
                                }
                            }

                            // Border Width
                            Text(
                                text = "BORDER WIDTH",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                borderWidths.forEach { (name, width) ->
                                    val isSelected = borderWidthPt == width
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.updatePpsBorderWidth(width) },
                                        label = { Text(name, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = SleekPagesPerSheetContainer,
                                            selectedLabelColor = SleekPagesPerSheetOnContainer,
                                            containerColor = MaterialTheme.colorScheme.surface,
                                            labelColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = isSelected,
                                            selectedBorderColor = SleekPagesPerSheetOnContainer,
                                            borderColor = Color.Transparent
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Dynamic live compiler preview container!
                Text(
                    text = "LIVE SHEET COMPILER PREVIEW",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val pagesPerSheet = columns * rows
                        val simulatedSheets = Math.ceil(file.pageCount.toDouble() / pagesPerSheet).toInt().coerceAtMost(3)

                        Text(
                            text = "Tiles layout: $columns x $rows grid ($pagesPerSheet pages per sheet)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Size: $pageSize | Orientation: ${if (isPortrait) "Portrait" else "Landscape"}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "Will compile into approx. ${Math.ceil(file.pageCount.toDouble() / pagesPerSheet).toInt()} output sheets.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Render preview of sheets
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
                        ) {
                            for (sheetIndex in 0 until simulatedSheets) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    // Sheet container
                                    val sw = if (isPortrait) 100.dp else 142.dp
                                    val sh = if (isPortrait) 142.dp else 100.dp

                                    Card(
                                        modifier = Modifier
                                            .size(width = sw, height = sh)
                                            .padding(4.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        border = BorderStroke(1.dp, Color.LightGray),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding((10 * marginPercent / 100).dp), // simulated margin
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            for (r in 0 until rows) {
                                                Row(
                                                    modifier = Modifier.weight(1f),
                                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                                ) {
                                                    for (c in 0 until columns) {
                                                        val gridCellNum = sheetIndex * pagesPerSheet + r * columns + c
                                                        val hasPage = gridCellNum < file.pageCount
                                                        Box(
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .fillMaxHeight()
                                                                .clip(RoundedCornerShape(2.dp))
                                                                .background(
                                                                    if (hasPage) SleekPagesPerSheetContainer.copy(alpha = 0.6f) else Color(0xFFF0F0F0)
                                                                )
                                                                .border(
                                                                    width = if (addBorder && hasPage) (borderWidthPt / 3).dp else 0.dp,
                                                                    color = if (addBorder && hasPage) Color(android.graphics.Color.parseColor(borderColorHex)) else Color.Transparent,
                                                                    shape = RoundedCornerShape(2.dp)
                                                                ),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            if (hasPage) {
                                                                Text(
                                                                    text = "${gridCellNum + 1}",
                                                                    fontSize = 8.sp,
                                                                    color = SleekPagesPerSheetOnContainer,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Sheet ${sheetIndex + 1}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }

                // Compile Action Button
                Button(
                    onClick = { viewModel.compilePpsPdf(context) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .padding(bottom = 16.dp)
                        .testTag("pages_per_sheet_compile_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SleekPagesPerSheetOnContainer)
                ) {
                    Icon(imageVector = Icons.Default.Transform, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Compile Pages Per Sheet", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MetadataRemoverScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val sourceFile by viewModel.metadataSourceFile.collectAsStateWithLifecycle()
    val extractedMetadata by viewModel.extractedMetadata.collectAsStateWithLifecycle()

    val metaTitle by viewModel.metaTitle.collectAsStateWithLifecycle()
    val metaAuthor by viewModel.metaAuthor.collectAsStateWithLifecycle()
    val metaSubject by viewModel.metaSubject.collectAsStateWithLifecycle()
    val metaKeywords by viewModel.metaKeywords.collectAsStateWithLifecycle()
    val metaCreator by viewModel.metaCreator.collectAsStateWithLifecycle()
    val metaProducer by viewModel.metaProducer.collectAsStateWithLifecycle()

    val pickFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.setMetadataSourceFile(context, uri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Metadata Remover", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.clearMetadataFile()
                            viewModel.navigateTo("HOME")
                        }
                    ) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (sourceFile == null) {
                // Empty State
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = SleekMetadataOnContainer.copy(alpha = 0.4f),
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No PDF file selected",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Select a PDF to inspect, edit, or strip hidden author, software, and creation metadata.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { pickFileLauncher.launch("application/pdf") },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SleekMetadataOnContainer)
                        ) {
                            Icon(imageVector = Icons.Default.FileOpen, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Choose PDF Document", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                val file = sourceFile!!

                // Display File Info Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = Color(0xFFC62828),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = file.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${file.pageCount} Pages • ${formatSize(file.size)}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                        }
                        IconButton(
                            onClick = { viewModel.clearMetadataFile() }
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear file", tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                        }
                    }
                }

                // Educational / Informational privacy banner description as requested
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    colors = CardDefaults.cardColors(containerColor = SleekMetadataContainer.copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, SleekMetadataOnContainer.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.PrivacyTip,
                            contentDescription = null,
                            tint = SleekMetadataOnContainer,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "What is PDF Metadata?",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = SleekMetadataOnContainer
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "PDF metadata is invisible background information attached to a file. It often includes details like the author's name, the creator/company, creation and modification dates, and the software used to create the document. This hidden data can unintentionally leak your privacy or internal company information when shared. Removing metadata does not change the visible text, formatting, or layout of your PDF.",
                                fontSize = 12.sp,
                                color = SleekMetadataOnContainer.copy(alpha = 0.85f),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                // Metadata Form Editing Fields
                Text(
                    text = "CHANGE FILE INFORMATION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Modify properties suggestion:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        MetadataTextField(
                            label = "Title",
                            value = metaTitle,
                            onValueChange = { viewModel.setMetaTitle(it) },
                            placeholder = "e.g., Project Proposal, Whitepaper",
                            leadingIcon = { Icon(imageVector = Icons.Default.Edit, contentDescription = "Title", tint = SleekMetadataOnContainer) }
                        )

                        MetadataTextField(
                            label = "Author",
                            value = metaAuthor,
                            onValueChange = { viewModel.setMetaAuthor(it) },
                            placeholder = "e.g., Creator Name, Company Dept",
                            leadingIcon = { Icon(imageVector = Icons.Default.Person, contentDescription = "Author", tint = SleekMetadataOnContainer) }
                        )

                        MetadataTextField(
                            label = "Subject",
                            value = metaSubject,
                            onValueChange = { viewModel.setMetaSubject(it) },
                            placeholder = "e.g., Marketing, Legal, Finance",
                            leadingIcon = { Icon(imageVector = Icons.Default.Info, contentDescription = "Subject", tint = SleekMetadataOnContainer) }
                        )

                        MetadataTextField(
                            label = "Keywords (Comma separated)",
                            value = metaKeywords,
                            onValueChange = { viewModel.setMetaKeywords(it) },
                            placeholder = "e.g., proposal, confidential, Q3",
                            leadingIcon = { Icon(imageVector = Icons.Default.Label, contentDescription = "Keywords", tint = SleekMetadataOnContainer) }
                        )

                        MetadataTextField(
                            label = "Creator / Application",
                            value = metaCreator,
                            onValueChange = { viewModel.setMetaCreator(it) },
                            placeholder = "e.g., Microsoft Word, Adobe InDesign",
                            leadingIcon = { Icon(imageVector = Icons.Default.Settings, contentDescription = "Creator", tint = SleekMetadataOnContainer) }
                        )

                        MetadataTextField(
                            label = "Producer",
                            value = metaProducer,
                            onValueChange = { viewModel.setMetaProducer(it) },
                            placeholder = "e.g., macOS Version 10.15.7 (Build 19H15) Quartz PDFContext",
                            leadingIcon = { Icon(imageVector = Icons.Default.Build, contentDescription = "Producer", tint = SleekMetadataOnContainer) }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    viewModel.setMetaTitle("")
                                    viewModel.setMetaAuthor("")
                                    viewModel.setMetaSubject("")
                                    viewModel.setMetaKeywords("")
                                    viewModel.setMetaCreator("")
                                    viewModel.setMetaProducer("")
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = SleekMetadataOnContainer)
                            ) {
                                Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = "Clear fields")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear Fields", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                // Extracted Original Properties container
                Text(
                    text = "ORIGINAL PROPERTIES FOUND",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (extractedMetadata.isEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = SleekMetadataOnContainer,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Ready to share!",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "No custom metadata or creator details were originally found.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Found ${extractedMetadata.size} hidden properties in this file:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            // Display metadata keys and values beautifully
                            extractedMetadata.forEach { (key, value) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = key,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        modifier = Modifier.weight(0.4f)
                                    )
                                    Text(
                                        text = value,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                        modifier = Modifier.weight(0.6f),
                                        textAlign = TextAlign.End
                                    )
                                }
                                HorizontalDivider(color = androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.5f).copy(alpha = 0.2f))
                            }
                        }
                    }
                }

                // Action choices buttons
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Update Action Button
                    Button(
                        onClick = { viewModel.saveModifiedMetadata(context, stripAllOthers = true) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("metadata_remover_update_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SleekMetadataOnContainer)
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save with Custom Metadata", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    // Direct Strip Action Button
                    OutlinedButton(
                        onClick = { viewModel.stripPdfMetadata(context) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("metadata_remover_strip_button"),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.5.dp, SleekMetadataOnContainer),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SleekMetadataOnContainer)
                    ) {
                        Icon(imageVector = Icons.Default.VisibilityOff, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Strip ALL Metadata & Save", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun MetadataTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        leadingIcon = leadingIcon,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        singleLine = true,
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SleekMetadataOnContainer,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            focusedLabelColor = SleekMetadataOnContainer,
            cursorColor = SleekMetadataOnContainer
        )
    )
}

@Composable
fun ResumeMakerScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    var data by remember { mutableStateOf(com.example.ui.ResumeData()) }
    var exportedFile by remember { mutableStateOf<java.io.File?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            data = data.copy(personalInfo = data.personalInfo.copy(photoUri = uri.toString()))
        }
    }

    if (exportedFile != null) {
        AlertDialog(
            onDismissRequest = { exportedFile = null },
            title = { Text("PDF Generated") },
            text = { Text("Your resume has been generated successfully. Would you like to view it or share it?") },
            confirmButton = {
                TextButton(onClick = {
                    try {
                        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", exportedFile!!)
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "application/pdf")
                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(intent)
                    } catch (e: java.lang.IllegalArgumentException) {
                        e.printStackTrace()
                        android.widget.Toast.makeText(context, "Error reading file", android.widget.Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        android.widget.Toast.makeText(context, "No PDF viewer found", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    exportedFile = null
                }) {
                    Text("View")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        try {
                            val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", exportedFile!!)
                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(android.content.Intent.createChooser(intent, "Share Resume"))
                        } catch (e: Exception) {
                            e.printStackTrace()
                            android.widget.Toast.makeText(context, "Error sharing file", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        exportedFile = null
                    }) {
                        Text("Share")
                    }
                    TextButton(onClick = { exportedFile = null }) {
                        Text("Close")
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.navigateTo("HOME") },
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "Resume Maker",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Build a professional resume",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { 
                    val file = com.example.util.ResumeExportUtils.generateResumePdf(context, data)
                    if (file != null) {
                        exportedFile = file
                    } else {
                        android.widget.Toast.makeText(context, "Error generating PDF", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                icon = { Icon(Icons.Default.Share, null) },
                text = { Text("Export Document") },
                containerColor = SleekPrimary,
                contentColor = Color.White
            )
        }
    ) { paddingValues ->
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 0. Template Selection
            item {
                SectionCard(title = "Template Selection", icon = Icons.Default.Description) {
                    var expanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Text(data.selectedTemplate.name + " TEMPLATE")
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.fillMaxWidth(0.8f) // Optional width constraint
                        ) {
                            com.example.ui.ResumeTemplate.values().forEach { tmpl ->
                                DropdownMenuItem(
                                    text = { Text(tmpl.name + " TEMPLATE") },
                                    onClick = { 
                                        data = data.copy(selectedTemplate = tmpl)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 1. Personal Info
            item {
                SectionCard(title = "Personal Information", icon = Icons.Default.Person) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (data.personalInfo.photoUri.isNotBlank()) {
                                AsyncImage(
                                    model = data.personalInfo.photoUri,
                                    contentDescription = "Profile Photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "No Photo",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Button(
                                onClick = { photoPickerLauncher.launch("image/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary)
                            ) {
                                Text("Upload Photo", color = Color.White)
                            }
                            if (data.personalInfo.photoUri.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                TextButton(
                                    onClick = { data = data.copy(personalInfo = data.personalInfo.copy(photoUri = "")) }
                                ) {
                                    Text("Remove Photo", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = data.personalInfo.fullName,
                        onValueChange = { data = data.copy(personalInfo = data.personalInfo.copy(fullName = it.uppercase())) },
                        label = { Text("Full Name (CAPS)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = data.personalInfo.professionalTitle,
                        onValueChange = { data = data.copy(personalInfo = data.personalInfo.copy(professionalTitle = it)) },
                        label = { Text("Professional Title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = data.personalInfo.email,
                        onValueChange = { data = data.copy(personalInfo = data.personalInfo.copy(email = it)) },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = data.personalInfo.phone,
                        onValueChange = { data = data.copy(personalInfo = data.personalInfo.copy(phone = it)) },
                        label = { Text("Phone") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = data.personalInfo.location,
                        onValueChange = { data = data.copy(personalInfo = data.personalInfo.copy(location = it)) },
                        label = { Text("Location") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = data.personalInfo.socialMedia,
                        onValueChange = { data = data.copy(personalInfo = data.personalInfo.copy(socialMedia = it)) },
                        label = { Text("LinkedIn / Social Media") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            // 2. Summary
            item {
                SectionCard(title = "Professional Summary", icon = Icons.Default.Assignment) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = data.summaryInfo.type == com.example.ui.SummaryType.MANUAL,
                            onClick = { data = data.copy(summaryInfo = data.summaryInfo.copy(type = com.example.ui.SummaryType.MANUAL)) }
                        )
                        Text("Manual")
                        Spacer(Modifier.width(16.dp))
                        RadioButton(
                            selected = data.summaryInfo.type == com.example.ui.SummaryType.AUTO,
                            onClick = { data = data.copy(summaryInfo = data.summaryInfo.copy(type = com.example.ui.SummaryType.AUTO)) }
                        )
                        Text("Auto generate")
                    }
                    if (data.summaryInfo.type == com.example.ui.SummaryType.MANUAL) {
                        OutlinedTextField(
                            value = data.summaryInfo.manualText,
                            onValueChange = { data = data.copy(summaryInfo = data.summaryInfo.copy(manualText = it)) },
                            label = { Text("Summary") },
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            maxLines = 5
                        )
                    } else {
                        OutlinedTextField(
                            value = data.summaryInfo.autoJobTitle,
                            onValueChange = { data = data.copy(summaryInfo = data.summaryInfo.copy(autoJobTitle = it)) },
                            label = { Text("Job Title for Summary") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Tone:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        val tones = listOf(com.example.ui.AutoSummaryTone.ENTRY_LEVEL to "Entry Level", com.example.ui.AutoSummaryTone.BALANCED to "Balanced", com.example.ui.AutoSummaryTone.ACTION to "Action")
                        tones.forEach { (tone, name) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = data.summaryInfo.autoTone == tone,
                                    onClick = { data = data.copy(summaryInfo = data.summaryInfo.copy(autoTone = tone)) }
                                )
                                Text(name)
                            }
                        }
                    }
                }
            }

            // 3. Work Experience
            item {
                SectionCard(title = "Work Experience", icon = Icons.Default.Work) {
                    data.workExperiences.forEachIndexed { index, exp ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Experience #${index + 1}", fontWeight = FontWeight.Bold)
                                    IconButton(onClick = { 
                                        val m = data.workExperiences.toMutableList()
                                        m.removeAt(index)
                                        data = data.copy(workExperiences = m)
                                    }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Delete, "Remove")
                                    }
                                }
                                OutlinedTextField(value = exp.jobTitle, onValueChange = { 
                                    var m = data.workExperiences.toMutableList()
                                    m[index] = exp.copy(jobTitle = it)
                                    data = data.copy(workExperiences = m)
                                }, label = { Text("Job Title") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(value = exp.company, onValueChange = { 
                                    var m = data.workExperiences.toMutableList()
                                    m[index] = exp.copy(company = it)
                                    data = data.copy(workExperiences = m)
                                }, label = { Text("Company/Institute") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(value = exp.startDate, onValueChange = { 
                                        var m = data.workExperiences.toMutableList()
                                        m[index] = exp.copy(startDate = it)
                                        data = data.copy(workExperiences = m)
                                    }, label = { Text("Start Date") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(value = exp.endDate, onValueChange = { 
                                        var m = data.workExperiences.toMutableList()
                                        m[index] = exp.copy(endDate = it)
                                        data = data.copy(workExperiences = m)
                                    }, label = { Text("End Date") }, modifier = Modifier.weight(1f), singleLine = true)
                                }
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(value = exp.duration, onValueChange = { 
                                    var m = data.workExperiences.toMutableList()
                                    m[index] = exp.copy(duration = it)
                                    data = data.copy(workExperiences = m)
                                }, label = { Text("Duration (e.g. 2 Years)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                                Spacer(Modifier.height(8.dp))
                                Text("Responsibilities:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                exp.responsibilities.forEachIndexed { rIndex, resp ->
                                    OutlinedTextField(value = resp, onValueChange = { 
                                        var m = data.workExperiences.toMutableList()
                                        val newResps = exp.responsibilities.toMutableList()
                                        newResps[rIndex] = it
                                        m[index] = exp.copy(responsibilities = newResps)
                                        data = data.copy(workExperiences = m)
                                    }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp), placeholder = { Text("Add bullet point") })
                                }
                                TextButton(onClick = { 
                                    var m = data.workExperiences.toMutableList()
                                    val newResps = exp.responsibilities.toMutableList().apply { add("") }
                                    m[index] = exp.copy(responsibilities = newResps)
                                    data = data.copy(workExperiences = m)
                                }) {
                                    Icon(Icons.Default.Add, null)
                                    Text("Add Point")
                                }
                            }
                        }
                    }
                    Button(onClick = { 
                        data = data.copy(workExperiences = data.workExperiences + com.example.ui.WorkExperience())
                    }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onBackground)) {
                        Icon(Icons.Default.Add, null)
                        Text("Add Experience")
                    }
                }
            }

            // 4. Education
            item {
                SectionCard(title = "Education Details", icon = Icons.Default.School) {
                    data.educations.forEachIndexed { index, edu ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Education #${index + 1}", fontWeight = FontWeight.Bold)
                                    IconButton(onClick = { 
                                        val m = data.educations.toMutableList()
                                        m.removeAt(index)
                                        data = data.copy(educations = m)
                                    }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Delete, "Remove")
                                    }
                                }
                                OutlinedTextField(value = edu.qualification, onValueChange = { 
                                    var m = data.educations.toMutableList()
                                    m[index] = edu.copy(qualification = it)
                                    data = data.copy(educations = m)
                                }, label = { Text("Qualification") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(value = edu.institute, onValueChange = { 
                                    var m = data.educations.toMutableList()
                                    m[index] = edu.copy(institute = it)
                                    data = data.copy(educations = m)
                                }, label = { Text("Institute Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(value = edu.board, onValueChange = { 
                                    var m = data.educations.toMutableList()
                                    m[index] = edu.copy(board = it)
                                    data = data.copy(educations = m)
                                }, label = { Text("University / Board") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(value = edu.yearOfCompletion, onValueChange = { 
                                        var m = data.educations.toMutableList()
                                        m[index] = edu.copy(yearOfCompletion = it)
                                        data = data.copy(educations = m)
                                    }, label = { Text("Year (e.g. 2024)") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(value = edu.percentage, onValueChange = { 
                                        var m = data.educations.toMutableList()
                                        m[index] = edu.copy(percentage = it)
                                        data = data.copy(educations = m)
                                    }, label = { Text("Percentage / CGPA") }, modifier = Modifier.weight(1f), singleLine = true)
                                }
                            }
                        }
                    }
                    Button(onClick = { 
                        data = data.copy(educations = data.educations + com.example.ui.Education())
                    }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onBackground)) {
                        Icon(Icons.Default.Add, null)
                        Text("Add Education")
                    }
                }
            }

            // 5. Skills
            item {
                SectionCard(title = "Skills & Certifications", icon = Icons.Default.Star) {
                    data.skills.forEachIndexed { index, skill ->
                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = skill.name, onValueChange = { 
                                var m = data.skills.toMutableList()
                                m[index] = skill.copy(name = it)
                                data = data.copy(skills = m)
                            }, label = { Text("Skill/Cert") }, modifier = Modifier.weight(1.5f), singleLine = true)
                            // Menu implementation using Box + OutlinedButton
                            var exp by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedButton(onClick = { exp = true }) { Text(skill.level) }
                                DropdownMenu(expanded = exp, onDismissRequest = { exp = false }) {
                                    listOf("Foundational", "Intermediate", "Proficiency", "Expert").forEach { lvl ->
                                        DropdownMenuItem(text = { Text(lvl) }, onClick = { 
                                            var m = data.skills.toMutableList()
                                            m[index] = skill.copy(level = lvl)
                                            data = data.copy(skills = m)
                                            exp = false
                                        })
                                    }
                                }
                            }
                            IconButton(onClick = { 
                                val m = data.skills.toMutableList()
                                m.removeAt(index)
                                data = data.copy(skills = m)
                            }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, null)
                            }
                        }
                    }
                    Button(onClick = { 
                        data = data.copy(skills = data.skills + com.example.ui.Skill())
                    }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onBackground)) {
                        Icon(Icons.Default.Add, null)
                        Text("Add Skill")
                    }
                }
            }

            // 6. Additional Info
            item {
                SectionCard(title = "Credentials & Profile Details", icon = Icons.Default.MoreHoriz) {
                    OutlinedTextField(
                        value = data.additionalInfo.dob,
                        onValueChange = { data = data.copy(additionalInfo = data.additionalInfo.copy(dob = it)) },
                        label = { Text("Date of Birth (DD-MM-YYYY)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = data.additionalInfo.gender,
                        onValueChange = { data = data.copy(additionalInfo = data.additionalInfo.copy(gender = it)) },
                        label = { Text("Gender") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = data.additionalInfo.maritalStatus,
                        onValueChange = { data = data.copy(additionalInfo = data.additionalInfo.copy(maritalStatus = it)) },
                        label = { Text("Marital Status") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = data.additionalInfo.hobby,
                        onValueChange = { data = data.copy(additionalInfo = data.additionalInfo.copy(hobby = it)) },
                        label = { Text("Hobbies") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("Languages Known:", fontWeight = FontWeight.Bold)
                    data.additionalInfo.languages.forEachIndexed { index, lang ->
                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = lang.name, onValueChange = { 
                                var m = data.additionalInfo.languages.toMutableList()
                                m[index] = lang.copy(name = it)
                                data = data.copy(additionalInfo = data.additionalInfo.copy(languages = m))
                            }, label = { Text("Language") }, modifier = Modifier.weight(1.5f), singleLine = true)
                            var exp by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedButton(onClick = { exp = true }) { Text(lang.level) }
                                DropdownMenu(expanded = exp, onDismissRequest = { exp = false }) {
                                    listOf("Beginner", "Conversational", "Fluent", "Native").forEach { lvl ->
                                        DropdownMenuItem(text = { Text(lvl) }, onClick = { 
                                            var m = data.additionalInfo.languages.toMutableList()
                                            m[index] = lang.copy(level = lvl)
                                            data = data.copy(additionalInfo = data.additionalInfo.copy(languages = m))
                                            exp = false
                                        })
                                    }
                                }
                            }
                            IconButton(onClick = { 
                                val m = data.additionalInfo.languages.toMutableList()
                                m.removeAt(index)
                                data = data.copy(additionalInfo = data.additionalInfo.copy(languages = m))
                            }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, null)
                            }
                        }
                    }
                    Button(onClick = { 
                         val m = data.additionalInfo.languages.toMutableList()
                         m.add(com.example.ui.Language())
                         data = data.copy(additionalInfo = data.additionalInfo.copy(languages = m))
                    }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onBackground)) {
                        Icon(Icons.Default.Add, null)
                        Text("Add Language")
                    }
                }
            }

            // 7. Declaration
            item {
                SectionCard(title = "Declaration", icon = Icons.Default.Gavel) {
                    OutlinedTextField(
                        value = data.declaration.role,
                        onValueChange = { data = data.copy(declaration = data.declaration.copy(role = it)) },
                        label = { Text("Role requested (e.g. Software Engineer)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "I hereby declare that the above information is true and correct to the best of my knowledge and belief. I take full responsibility for its accuracy and affirm my commitment to the ${data.declaration.role.ifBlank{"[Role]"}} profession with sincerity and integrity.",
                        fontSize = 13.sp, color = Color.Gray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
        }
    }
}

@Composable
fun SectionCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = SleekPrimary)
                Spacer(Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
            }
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}






