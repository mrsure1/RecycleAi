package app.trashai.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle

/** E-순환거버넌스 폐가전 무상 방문 수거 */
const val ECYCLE_DIAL_URI = "tel:15990903"
const val ECYCLE_DISPLAY_NUMBER = "1599-0903"

private val PHONE_IN_TEXT_REGEX =
    Regex("""(1599[-\s]?0903|0\d{1,2}[-\s]?\d{3,4}[-\s]?\d{4})""")

fun dialPhoneNumber(context: android.content.Context, phoneOrUri: String) {
    val uri = when {
        phoneOrUri.startsWith("tel:") -> phoneOrUri
        else -> {
            val digits = phoneOrUri.filter { it.isDigit() }
            if (digits.isEmpty()) return
            "tel:$digits"
        }
    }
    runCatching {
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse(uri)))
    }
}

@Composable
fun TextWithDialablePhones(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    highlightEcycle: Boolean = true,
    onBeforeDial: () -> Unit = {},
) {
    val context = LocalContext.current
    val annotated = remember(text, highlightEcycle) {
        buildDialableAnnotatedString(text, highlightEcycle)
    }
    if (annotated.hasClickablePhones()) {
        ClickableText(
            text = annotated,
            modifier = modifier,
            style = style,
            onClick = { offset ->
                annotated.getStringAnnotations(tag = "phone", start = offset, end = offset)
                    .firstOrNull()
                    ?.let {
                        onBeforeDial()
                        dialPhoneNumber(context, it.item)
                    }
            },
        )
    } else {
        Text(text = text, modifier = modifier, style = style)
    }
}

private fun AnnotatedString.hasClickablePhones(): Boolean =
    getStringAnnotations(tag = "phone", start = 0, end = length).isNotEmpty()

private fun buildDialableAnnotatedString(text: String, highlightEcycle: Boolean): AnnotatedString =
    buildAnnotatedString {
        var lastIndex = 0
        val matches = PHONE_IN_TEXT_REGEX.findAll(text).toList()
        if (matches.isEmpty() && highlightEcycle && text.contains("E-순환", ignoreCase = true)) {
            append(text)
            return@buildAnnotatedString
        }
        for (match in matches) {
            if (match.range.first > lastIndex) {
                append(text.substring(lastIndex, match.range.first))
            }
            val raw = match.value
            val telUri = if (raw.replace(Regex("[^0-9]"), "") == "15990903") {
                ECYCLE_DIAL_URI
            } else {
                "tel:${raw.filter { it.isDigit() }}"
            }
            pushStringAnnotation(tag = "phone", annotation = telUri)
            withStyle(
                SpanStyle(
                    color = Tokens.Primary,
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline,
                ),
            ) {
                append(raw)
            }
            pop()
            lastIndex = match.range.last + 1
        }
        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }
