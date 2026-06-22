const fs = require('fs');
let code = fs.readFileSync('app/src/main/java/com/example/WatermarkPdfScreen.kt', 'utf8');

code = code.replace(
    /import androidx\.compose\.runtime\.\*/,
    "import androidx.compose.runtime.*\nimport androidx.compose.material3.ExposedDropdownMenuBox\nimport androidx.compose.material3.ExposedDropdownMenuDefaults\nimport androidx.compose.material3.DropdownMenuItem"
);

const targetContent = `Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = colorHex,
                                onValueChange = { viewModel.updateWatermarkColor(it) },
                                label = { Text("Color Hex") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = font,
                                onValueChange = { viewModel.updateWatermarkFont(it) },
                                label = { Text("Font") },
                                modifier = Modifier.weight(1f)
                            )
                        }`;

const replacement = `Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = colorHex,
                                onValueChange = { viewModel.updateWatermarkColor(it) },
                                label = { Text("Color Hex") },
                                modifier = Modifier.weight(1f)
                            )
                            // Dropdown for Font
                            var fontExpanded by remember { mutableStateOf(false) }
                            val fonts = listOf("Helvetica", "Times New Roman", "Courier", "Sans Serif")
                            
                            @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
                            ExposedDropdownMenuBox(
                                expanded = fontExpanded,
                                onExpandedChange = { fontExpanded = !fontExpanded },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = font,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Font") },
                                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fontExpanded) },
                                    modifier = Modifier.menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = fontExpanded,
                                    onDismissRequest = { fontExpanded = false }
                                ) {
                                    fonts.forEach { item ->
                                        DropdownMenuItem(
                                            text = { Text(item) },
                                            onClick = {
                                                viewModel.updateWatermarkFont(item)
                                                fontExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }`;

code = code.replace(targetContent, replacement);
fs.writeFileSync('app/src/main/java/com/example/WatermarkPdfScreen.kt', code);
console.log("Updated WatermarkPdfScreen font selection");
