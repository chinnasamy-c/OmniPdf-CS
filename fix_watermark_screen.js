const fs = require('fs');
let code = fs.readFileSync('app/src/main/java/com/example/WatermarkPdfScreen.kt', 'utf8');

// I need to add state for start and end page
code = code.replace(
    /val opacity by viewModel\.watermarkOpacity\.collectAsStateWithLifecycle\(\)/,
    `val opacity by viewModel.watermarkOpacity.collectAsStateWithLifecycle()
    val startPage by viewModel.watermarkStartPage.collectAsStateWithLifecycle()
    val endPage by viewModel.watermarkEndPage.collectAsStateWithLifecycle()`
);

code = code.replace(
    /Text\("Rotation: \$\{rotation\.toInt\(\)\}°", fontWeight = FontWeight\.Bold\)\n                    Slider\(\n                        value = rotation,\n                        onValueChange = \{ viewModel\.updateWatermarkRotation\(it\) \},\n                        valueRange = 0f\.\.360f\n                    \)/,
    `Text("Rotation: \${rotation.toInt()}°", fontWeight = FontWeight.Bold)
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
                    }`
);

fs.writeFileSync('app/src/main/java/com/example/WatermarkPdfScreen.kt', code);
console.log("Updated WatermarkPdfScreen");
