package io.github.v2compose.ui.common

import android.util.Log
import android.util.Size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import io.github.cooaer.htmltext.HtmlText
import io.github.v2compose.Constants
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import kotlin.experimental.xor

private const val TAG = "HtmlComposables"

@Composable
fun HtmlContent(
    content: String,
    modifier: Modifier = Modifier,
    htmlImageSizes: Map<String, Size> = emptyMap(),
    selectable: Boolean = false,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium.copy(
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.3.sp,
    ),
    baseUrl: String = Constants.baseUrl,
    onUriClick: ((uri: String) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val document = remember(content) { Jsoup.parse(content) }

    val encodedEmails = remember(document) { document.select("a.__cf_email__") }

    val imgElements = remember(document) { document.select("img") }
    val changedImgElements by remember(htmlImageSizes) {
        derivedStateOf {
            imgElements.associateBy { it.attr("src") }
                .filter { htmlImageSizes.containsKey(it.key) }
        }
    }

    val newHtml = if (changedImgElements.isNotEmpty() || encodedEmails.isNotEmpty()) {
        changedImgElements.forEach { (src, ele) ->
            htmlImageSizes[src]?.let { resetImgSize(ele, it) }
        }
        encodedEmails.forEach { fixEmailProtected(it) }
        document.outerHtml()
    } else content

    HtmlText(
        html = newHtml,
        modifier = modifier,
        selectable = selectable,
        textStyle = textStyle,
        baseUrl = baseUrl,
        onLinkClick = onUriClick,
        onClick = onClick
    )
}

private fun resetImgSize(ele: Element, it: Size) {
    ele.attr("width", it.width.toString())
    ele.attr("height", it.height.toString())
    Log.d(TAG, "resetImage, newImg = $ele")
}

private fun fixEmailProtected(ele: Element) {
    val encodedEmail = ele.attr("data-cfemail")
    try {
        val email = cfDecodeEmail(encodedEmail)
        if (email.isNotEmpty()) {
            val parent = ele.parent()
            val siblingIndex = ele.siblingIndex()
            ele.remove()
            parent?.insertChildren(siblingIndex, TextNode(email))
        }
    } catch (e: IllegalArgumentException) {
        e.printStackTrace()
    }
}

private fun cfDecodeEmail(encodedString: String): String {
    val stringBuilder = StringBuilder()
    val bytes = encodedString.decodeHex()
    val r = bytes[0]
    for (index in 1 until bytes.size) {
        val byte = bytes[index] xor r
        stringBuilder.append(byte.toInt().toChar())
    }
    return stringBuilder.toString()
}

internal fun String.decodeHex(): ByteArray {
    require(length % 2 == 0) { "Unexpected hex string: $this" }

    val result = ByteArray(length / 2)
    for (i in result.indices) {
        val d1 = decodeHexDigit(this[i * 2]) shl 4
        val d2 = decodeHexDigit(this[i * 2 + 1])
        result[i] = (d1 + d2).toByte()
    }
    return result
}

private fun decodeHexDigit(c: Char): Int {
    return when (c) {
        in '0'..'9' -> c - '0'
        in 'a'..'f' -> c - 'a' + 10
        in 'A'..'F' -> c - 'A' + 10
        else -> throw IllegalArgumentException("Unexpected hex digit: $c")
    }
}