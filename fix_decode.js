const fs = require('fs');

function replaceDecodeStream(code) {
    const rx = /val (\w+) = BitmapFactory\.decodeStream\(([^)]+)\)/g;
    return code.replace(rx, (match, varName, streamVar) => {
        return `val opts = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    android.graphics.BitmapFactory.decodeStream(${streamVar}, null, opts)
                    var scaleSize = 1
                    while (opts.outWidth / scaleSize > 2048 || opts.outHeight / scaleSize > 2048) { scaleSize *= 2 }
                    opts.inJustDecodeBounds = false
                    opts.inSampleSize = scaleSize
                    ${streamVar}?.close()
                    // Need a new stream instance since previous was consumed
                    // But wait, the stream variable is usually local or we can't reopen it easily here!
               `;
    });
}
