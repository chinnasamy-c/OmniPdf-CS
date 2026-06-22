package com.example

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.util.DrawnPage
import com.example.util.DrawnStroke
import com.example.util.DrawnText
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle

enum class ToolMode { PEN, HIGHLIGHT, ERASER, TEXT }

data class DrawStrokeLocal(
    val path: Path,
    val color: Color,
    val width: Float,
    val isHighlight: Boolean = false,
    val isEraser: Boolean = false
)

data class DrawTextLocal(
    var text: String,
    var x: Float,
    var y: Float,
    var size: Float,
    var color: Color
)

data class DrawPageLocal(
    val strokes: MutableList<DrawStrokeLocal> = mutableStateListOf(),
    val texts: MutableList<DrawTextLocal> = mutableStateListOf()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawPdfScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    var pages by remember { mutableStateOf(listOf(DrawPageLocal())) }
    var currentPageIndex by remember { mutableStateOf(0) }
    
    var currentColor by remember { mutableStateOf(Color.Black) }
    var currentStrokeWidth by remember { mutableStateOf(5f) }
    var currentTextSize by remember { mutableStateOf(20f) }
    
    
    var currentMode by remember { mutableStateOf(ToolMode.PEN) }
    
    var currentPath by remember { mutableStateOf<Path?>(null) }
    var canvasSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }

    // Text box state
    var showTextDialog by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }
    var textTapPosition by remember { mutableStateOf(Offset.Zero) }

    val textMeasurer = rememberTextMeasurer()

    val presetColors = listOf(
        Color.Black, Color.Red, Color.Blue, Color.Green, Color(0xFFFFA500), Color(0xFFFFFF00)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Draw PDF", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo("HOME") }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val page = pages[currentPageIndex]
                        if (page.strokes.isNotEmpty() || page.texts.isNotEmpty()) {
                            if (page.strokes.isNotEmpty() && page.texts.isEmpty()) page.strokes.removeLast()
                            else if (page.texts.isNotEmpty() && page.strokes.isEmpty()) page.texts.removeLast()
                            else {
                                // removing whatever was added last would require unified timeline,
                                // we'll prioritize stroke undo over text undo unless requested
                                page.strokes.removeLast()
                            }
                        }
                    }) {
                        Icon(Icons.Default.Undo, contentDescription = "Undo")
                    }
                    IconButton(onClick = {
                        val finalPages = pages.map { page ->
                            val sX = if (canvasSize != androidx.compose.ui.geometry.Size.Zero) (595f / canvasSize.width) else 1f
                            val sY = if (canvasSize != androidx.compose.ui.geometry.Size.Zero) (842f / canvasSize.height) else 1f
                            DrawnPage(
                                strokes = page.strokes.map {
                                    val androidPath = android.graphics.Path(it.path.asAndroidPath())
                                    if (canvasSize != androidx.compose.ui.geometry.Size.Zero) {
                                        val matrix = android.graphics.Matrix()
                                        matrix.postScale(sX, sY)
                                        androidPath.transform(matrix)
                                    }
                                    DrawnStroke(
                                        path = androidPath,
                                        color = if (it.isEraser) Color.White.toArgb() else it.color.toArgb(),
                                        width = it.width * sX,
                                        isHighlight = it.isHighlight
                                    )
                                },
                                texts = page.texts.map {
                                    DrawnText(it.text, it.x * sX, it.y * sY, it.size * sX, it.color.toArgb())
                                }
                            )
                        }
                        viewModel.saveDrawPdf(context, finalPages)
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { currentMode = ToolMode.PEN },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = if (currentMode == ToolMode.PEN) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                    ) { Icon(Icons.Default.Brush, contentDescription = "Pen") }
                    
                    IconButton(
                        onClick = { currentMode = ToolMode.HIGHLIGHT },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = if (currentMode == ToolMode.HIGHLIGHT) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                    ) { Icon(Icons.Default.Highlight, contentDescription = "Highlight") }

                    IconButton(
                        onClick = { currentMode = ToolMode.ERASER },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = if (currentMode == ToolMode.ERASER) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                    ) { Icon(Icons.Default.AutoFixHigh, contentDescription = "Eraser") }

                    IconButton(
                        onClick = { currentMode = ToolMode.TEXT },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = if (currentMode == ToolMode.TEXT) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                    ) { Icon(Icons.Default.Title, contentDescription = "Text") }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                presetColors.forEach { color ->
                    Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(color).border(2.dp, if (currentColor == color) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape).clickable { currentColor = color })
                }
            }

            if (currentMode != ToolMode.TEXT) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Text("Thickness: ")
                    Slider(value = currentStrokeWidth, onValueChange = { currentStrokeWidth = it }, valueRange = 1f..40f, modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("${currentStrokeWidth.toInt()}pt")
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Text("Text Size: ")
                    Slider(value = currentTextSize, onValueChange = { currentTextSize = it }, valueRange = 10f..100f, modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("${currentTextSize.toInt()}pt")
                }
            }

            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { if (currentPageIndex > 0) currentPageIndex-- }, enabled = currentPageIndex > 0) { Text("Prev") }
                Text("Page ${currentPageIndex + 1} of ${pages.size}", fontWeight = FontWeight.Bold)
                Row {
                    IconButton(onClick = { pages = pages + DrawPageLocal(); currentPageIndex = pages.size - 1 }) { Icon(Icons.Default.Add, "Add Page") }
                    Button(onClick = { if (currentPageIndex < pages.size - 1) currentPageIndex++ }, enabled = currentPageIndex < pages.size - 1) { Text("Next") }
                }
            }

            BoxWithConstraints(modifier = Modifier.fillMaxWidth().weight(1f).padding(16.dp), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f / 1.414f).clip(RoundedCornerShape(4.dp)).background(Color.White).border(1.dp, Color.LightGray, RoundedCornerShape(4.dp))) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(currentMode) {
                                if (currentMode == ToolMode.TEXT) {
                                    detectTapGestures(onTap = { offset ->
                                        textTapPosition = offset
                                        inputText = ""
                                        showTextDialog = true
                                    })
                                } else {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            currentPath = Path().apply { moveTo(offset.x, offset.y) }
                                        },
                                        onDrag = { change, _ ->
                                            change.consume()
                                            currentPath?.lineTo(change.position.x, change.position.y)
                                        },
                                        onDragEnd = {
                                            currentPath?.let { path ->
                                                pages[currentPageIndex].strokes.add(
                                                    DrawStrokeLocal(path, if (currentMode == ToolMode.ERASER) Color.White else currentColor, currentStrokeWidth, currentMode == ToolMode.HIGHLIGHT, currentMode == ToolMode.ERASER)
                                                )
                                            }
                                            currentPath = null
                                        },
                                        onDragCancel = {
                                            currentPath = null
                                        }
                                    )
                                }
                            }
                    ) {
                        canvasSize = size
                        val page = pages[currentPageIndex]
                        
                        page.strokes.forEach { stroke ->
                            drawPath(
                                path = stroke.path,
                                color = if (stroke.isEraser) Color.White else stroke.color.copy(alpha = if (stroke.isHighlight) 0.4f else 1f),
                                style = Stroke(width = stroke.width, cap = StrokeCap.Round, join = StrokeJoin.Round),
                                blendMode = BlendMode.SrcOver
                            )
                        }

                        currentPath?.let { path ->
                            drawPath(
                                path = path,
                                color = if (currentMode == ToolMode.ERASER) Color.White else currentColor.copy(alpha = if (currentMode == ToolMode.HIGHLIGHT) 0.4f else 1f),
                                style = Stroke(width = currentStrokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }
                        
                        page.texts.forEach { dt ->
                            drawText(
                                textMeasurer = textMeasurer,
                                text = dt.text,
                                topLeft = Offset(dt.x, dt.y - dt.size), // Adjust because iText uses bottom left text anchoring
                                style = TextStyle(color = dt.color, fontSize = dt.size.sp, fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
        
        if (showTextDialog) {
            AlertDialog(
                onDismissRequest = { showTextDialog = false },
                title = { Text("Add Text") },
                text = {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (inputText.isNotBlank()) {
                            pages[currentPageIndex].texts.add(
                                DrawTextLocal(inputText, textTapPosition.x, textTapPosition.y, currentTextSize, currentColor)
                            )
                        }
                        showTextDialog = false
                    }) { Text("Add") }
                },
                dismissButton = {
                    TextButton(onClick = { showTextDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}
