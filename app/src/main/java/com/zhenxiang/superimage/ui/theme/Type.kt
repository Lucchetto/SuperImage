package com.zhenxiang.superimage.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.zhenxiang.superimage.R

val Typography: Typography
    @Composable get() {
        val fontFamily = stringResource(R.string.font_family).let {
            when (it) {
                "source" -> FontFamily(
                    Font(R.font.source_serif_pro_regular),
                    Font(R.font.source_serif_pro_semibold, FontWeight.W600),
                    Font(R.font.source_serif_pro_bold, FontWeight.Bold)
                )
                "crimson" -> FontFamily(
                    Font(R.font.crimson_text_regular),
                    Font(R.font.crimson_text_semibold, FontWeight.W600),
                    Font(R.font.crimson_text_bold, FontWeight.Bold)
                )
                else -> throw IllegalStateException("Invalid font family for locale")
            }
        }
        
        return Typography(
            displayMedium = TextStyle(
                fontWeight = FontWeight.Normal,
                fontFamily = fontFamily,
                fontSize = 42.sp,
                lineHeight = 48.sp,
                letterSpacing = 0.sp
            ),
            headlineLarge = TextStyle(
                fontWeight = FontWeight.SemiBold,
                fontFamily = fontFamily,
                fontSize = 32.sp,
                lineHeight = 40.sp,
                letterSpacing = 0.sp
            ),
            headlineMedium = TextStyle(
                fontWeight = FontWeight.SemiBold,
                fontFamily = fontFamily,
                fontSize = 28.sp,
                lineHeight = 36.sp,
                letterSpacing = 0.sp
            ),
            headlineSmall = TextStyle(
                fontWeight = FontWeight.Normal,
                fontFamily = fontFamily,
                fontSize = 22.sp,
                lineHeight = 30.sp,
                letterSpacing = 0.sp
            ),
            titleLarge = TextStyle(
                fontWeight = FontWeight.SemiBold,
                fontFamily = fontFamily,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                letterSpacing = 0.sp
            ),
            titleMedium = TextStyle(
                fontWeight = FontWeight.SemiBold,
                fontFamily = fontFamily,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.15.sp
            ),
            titleSmall = TextStyle(
                fontWeight = FontWeight.Bold,
                fontFamily = fontFamily,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.1.sp
            ),
            bodyLarge = TextStyle(
                fontWeight = FontWeight.Normal,
                fontFamily = fontFamily,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.15.sp
            ),
            bodyMedium = TextStyle(
                fontWeight = FontWeight.Normal,
                fontFamily = fontFamily,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.5.sp
            ),
            bodySmall = TextStyle(
                fontWeight = FontWeight.Normal,
                fontFamily = fontFamily,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.15.sp
            ),
            labelLarge = TextStyle(
                fontWeight = FontWeight.Normal,
                fontFamily = fontFamily,
                fontSize = 18.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.1.sp
            ),
            labelMedium = TextStyle(
                fontWeight = FontWeight.SemiBold,
                fontFamily = fontFamily,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.5.sp
            ),
            labelSmall = TextStyle(
                fontWeight = FontWeight.SemiBold,
                fontFamily = fontFamily,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.5.sp
            )
        )
    }