const fs = require('fs');
let code = fs.readFileSync('app/src/main/java/com/example/StampBadgeScreen.kt', 'utf8');

// Replace viewmodel global call
code = code.replace(
    /val pageStamps = viewModel\.stampData/,
    "val pageStamps = viewModel.stampData\n    val globalStampSize by viewModel.globalStampSize.collectAsStateWithLifecycle()"
);

// We need to make sure collectAsStateWithLifecycle works here.
code = code.replace(
    /import androidx\.compose\.runtime\.\*/,
    "import androidx.compose.runtime.*\nimport androidx.lifecycle.compose.collectAsStateWithLifecycle"
);

// Add slider before the box
code = code.replace(
    /Text\(\n\s*"Tap on document to place stamp\. Use two fingers to scroll\. Tap stamp to remove\.",/,
    `Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = "Stamp Size Modifer: \${(globalStampSize * 100).toInt()}%",
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
                    "Tap on document to place stamp. Use two fingers to scroll. Tap stamp to remove.",`
);

// update `viewModel.addStamp(i, selectedPreset, customImageUri, rx, ry)`
code = code.replace(
    /viewModel\.addStamp\(i, selectedPreset, customImageUri, rx, ry\)/g,
    "viewModel.addStamp(i, selectedPreset, customImageUri, rx, ry, globalStampSize)"
);

// update UI size `size(150.dp)`
code = code.replace(
    /\.size\(150\.dp\)/g,
    ".size(150.dp * stamp.size)"
);

// update offset x/y 
code = code.replace(
    /x = \(w\.value \* stamp\.x\)\.dp - 75\.dp,/,
    "x = (w.value * stamp.x).dp - (75.dp * stamp.size),"
);
code = code.replace(
    /y = \(h\.value \* stamp\.y\)\.dp - 75\.dp/,
    "y = (h.value * stamp.y).dp - (75.dp * stamp.size)"
);

fs.writeFileSync('app/src/main/java/com/example/StampBadgeScreen.kt', code);
console.log("Updated StampBadgeScreen.kt ui");
