const fs = require('fs');
let code = fs.readFileSync('app/src/main/java/com/example/ui/MainViewModel.kt', 'utf8');

code = code.replace(/private val _sigCropRight = MutableStateFlow\(0f\)/g, 'private val _sigCropRight = MutableStateFlow(1f)');
code = code.replace(/private val _sigCropBottom = MutableStateFlow\(0f\)/g, 'private val _sigCropBottom = MutableStateFlow(1f)');

// We need to replace `_sigCropRight.value = 0f` and `_sigCropBottom.value = 0f` lines inside clearSignatureMaker and setSignatureImageUri
code = code.replace(/_sigCropLeft\.value = 0f\s*\n\s*_sigCropRight\.value = 0f\s*\n\s*_sigCropTop\.value = 0f\s*\n\s*_sigCropBottom\.value = 0f/g, 
`_sigCropLeft.value = 0f
            _sigCropRight.value = 1f
            _sigCropTop.value = 0f
            _sigCropBottom.value = 1f`);

// Update getCroppedSignatureBitmap to check 1f instead of 0f for rightFrac and bottomFrac
code = code.replace(/if \(leftFrac == 0f && rightFrac == 0f && topFrac == 0f && bottomFrac == 0f\)/, 
`if (leftFrac == 0f && rightFrac == 1f && topFrac == 0f && bottomFrac == 1f)`);

fs.writeFileSync('app/src/main/java/com/example/ui/MainViewModel.kt', code);
console.log("Fixed sigCrop defaults!");
