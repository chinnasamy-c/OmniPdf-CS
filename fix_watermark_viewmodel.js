const fs = require('fs');
let code = fs.readFileSync('app/src/main/java/com/example/ui/MainViewModel.kt', 'utf8');

code = code.replace(
    /private val _watermarkText = MutableStateFlow\("Your Watermark"\)/,
    `private val _watermarkStartPage = MutableStateFlow(1)
    val watermarkStartPage: StateFlow<Int> = _watermarkStartPage.asStateFlow()
    fun updateWatermarkStartPage(page: Int) { _watermarkStartPage.value = page }

    private val _watermarkEndPage = MutableStateFlow(-1)
    val watermarkEndPage: StateFlow<Int> = _watermarkEndPage.asStateFlow()
    fun updateWatermarkEndPage(page: Int) { _watermarkEndPage.value = page }

    private val _watermarkText = MutableStateFlow("Your Watermark")`
);

code = code.replace(
    /private val _watermarkRotation = MutableStateFlow\(45f\)/,
    'private val _watermarkRotation = MutableStateFlow(0f)'
);

code = code.replace(
    /val result = PdfUtils\.processWatermark\([\s\S]*?outputFile = outFile\n                    \)/,
    `val result = PdfUtils.processWatermark(
                        context = context,
                        sourceUri = uri,
                        mode = _watermarkMode.value,
                        type = _watermarkType.value,
                        text = _watermarkText.value,
                        position = _watermarkPosition.value,
                        textSize = _watermarkTextSize.value,
                        font = _watermarkFont.value,
                        colorHex = _watermarkColor.value,
                        foreground = _watermarkForeground.value,
                        rotation = _watermarkRotation.value,
                        imageUri = _watermarkImageUri.value,
                        opacity = _watermarkOpacity.value,
                        cropEnabled = _watermarkCropEnable.value,
                        startPage = _watermarkStartPage.value,
                        endPage = _watermarkEndPage.value,
                        outputFile = outFile
                    )`
);

// We need to initialize the end page when file is selected.
// In check `fun setWatermarkSourceFile(context: Context, uri: Uri)`
code = code.replace(
    /fun setWatermarkSourceFile\(context: Context, uri: Uri\) \{[\s\S]*?\n    \}/,
    `fun setWatermarkSourceFile(context: Context, uri: Uri) {
        _watermarkSourceUri.value = uri
        _watermarkEndPage.value = PdfUtils.getPdfPageCount(context, uri)
        _watermarkStartPage.value = 1
        generateWatermarkPreview(context)
    }`
);

fs.writeFileSync('app/src/main/java/com/example/ui/MainViewModel.kt', code);
console.log("Updated Watermark in MainViewModel");
