const fs = require('fs');
const path = require('path');

const candidatePaths = [
    'app/build/outputs/apk/debug/app-debug.apk',
    '.build-outputs/app-debug.apk'
];

let foundSrc = null;
for (const p of candidatePaths) {
    if (fs.existsSync(p)) {
        foundSrc = p;
        break;
    }
}

if (!foundSrc) {
    console.error('Could not find app-debug.apk in candidate paths:', candidatePaths);
    process.exit(1);
}

console.log('Found source APK at:', foundSrc);

try {
    fs.copyFileSync(foundSrc, 'Omnipdf.apk');
    fs.copyFileSync(foundSrc, 'Omnipdf.zip');
    
    // Ensure parent dir of destination exists
    const appSrcDir = 'app/src';
    if (!fs.existsSync(appSrcDir)) {
        fs.mkdirSync(appSrcDir, { recursive: true });
    }
    fs.copyFileSync(foundSrc, 'app/src/Omnipdf.apk');
    
    console.log('APK copied successfully to explorer outputs!');
} catch (e) {
    console.error('Failed to copy APK:', e);
    process.exit(1);
}
