package com.theantsbot

import android.graphics.Bitmap
import android.os.Environment
import com.googlecode.tesseract.android.TessBaseAPI


class ExtractText {
    fun run(bitmap: Bitmap?): String {
        val tessBaseApi = TessBaseAPI()

        val path = Environment.getExternalStoragePublicDirectory(
            "TheAntsBot"
        ).toString()

        tessBaseApi.init(path, "eng")
        tessBaseApi.setImage(bitmap)
        val extractedText = tessBaseApi.utF8Text
        tessBaseApi.end()
        return extractedText
    }
}
