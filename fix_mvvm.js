const fs = require('fs');
let code = fs.readFileSync('app/src/main/java/com/example/ui/MainViewModel.kt', 'utf8');

code = code.replace(
    /private val _watermarkText\s*=\s*MutableStateFlow\("Your Watermark"\)/,
    `private val _watermarkStartPage = MutableStateFlow(1)
    val watermarkStartPage = _watermarkStartPage.asStateFlow()
    fun updateWatermarkStartPage(page: Int) { _watermarkStartPage.value = page }

    private val _watermarkEndPage = MutableStateFlow(-1)
    val watermarkEndPage = _watermarkEndPage.asStateFlow()
    fun updateWatermarkEndPage(page: Int) { _watermarkEndPage.value = page }

    private val _watermarkText = MutableStateFlow("Your Watermark")`
);

fs.writeFileSync('app/src/main/java/com/example/ui/MainViewModel.kt', code);
console.log("Fixed main viewmodel");
