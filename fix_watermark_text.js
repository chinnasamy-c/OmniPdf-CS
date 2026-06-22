const fs = require('fs');
let code = fs.readFileSync('app/src/main/java/com/example/WatermarkPdfScreen.kt', 'utf8');

const targetContent = `Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

const replacement = `Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = font,
                                onValueChange = { viewModel.updateWatermarkFont(it) },
                                label = { Text("Custom Font Name (e.g. Helvetica)") },
                                modifier = Modifier.weight(1f)
                            )
                        }`;

code = code.replace(targetContent, replacement);
fs.writeFileSync('app/src/main/java/com/example/WatermarkPdfScreen.kt', code);
console.log("Updated WatermarkPdfScreen to allow typing custom font");
