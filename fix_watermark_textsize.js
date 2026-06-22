const fs = require('fs');
let code = fs.readFileSync('app/src/main/java/com/example/WatermarkPdfScreen.kt', 'utf8');

const targetContent = `Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = colorHex,
                                onValueChange = { viewModel.updateWatermarkColor(it) },
                                label = { Text("Color Hex") },
                                modifier = Modifier.weight(1f)
                            )`;

const replacement = `Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = colorHex,
                                onValueChange = { viewModel.updateWatermarkColor(it) },
                                label = { Text("Color Hex") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        Text("Text Size: \${textSize.toInt()}pt", fontWeight = FontWeight.Bold)
                        Slider(
                            value = textSize,
                            onValueChange = { viewModel.updateWatermarkTextSize(it) },
                            valueRange = 10f..150f
                        )
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {`;

code = code.replace(targetContent, replacement);
fs.writeFileSync('app/src/main/java/com/example/WatermarkPdfScreen.kt', code);
console.log("Updated WatermarkPdfScreen text size");
