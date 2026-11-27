package com.kairo.agent.ui.theme

import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kairo.agent.R

/** Same variable One UI Sans face used by Twidget, with real weight-axis instances. */
@OptIn(ExperimentalTextApi::class)
private val OneUiSans = FontFamily(
    listOf(300, 400, 500, 600, 700).map { weight ->
        Font(R.font.one_ui_sans, FontWeight(weight),
            variationSettings = FontVariation.Settings(FontVariation.weight(weight)))
    }
)

val OneUiTypography = Typography().let { base ->
    base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = OneUiSans, fontWeight = FontWeight.Bold),
        displayMedium = base.displayMedium.copy(fontFamily = OneUiSans),
        displaySmall = base.displaySmall.copy(fontFamily = OneUiSans),
        headlineLarge = base.headlineLarge.copy(fontFamily = OneUiSans, fontWeight = FontWeight.Bold),
        headlineMedium = base.headlineMedium.copy(fontFamily = OneUiSans, fontWeight = FontWeight.SemiBold),
        headlineSmall = base.headlineSmall.copy(fontFamily = OneUiSans, fontWeight = FontWeight.SemiBold),
        titleLarge = base.titleLarge.copy(fontFamily = OneUiSans, fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontFamily = OneUiSans, fontWeight = FontWeight.Medium),
        titleSmall = base.titleSmall.copy(fontFamily = OneUiSans, fontWeight = FontWeight.Medium),
        bodyLarge = base.bodyLarge.copy(fontFamily = OneUiSans, fontSize = 16.sp, letterSpacing = 0.sp),
        bodyMedium = base.bodyMedium.copy(fontFamily = OneUiSans, letterSpacing = 0.sp),
        bodySmall = base.bodySmall.copy(fontFamily = OneUiSans, letterSpacing = 0.sp),
        labelLarge = base.labelLarge.copy(fontFamily = OneUiSans, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
        labelMedium = base.labelMedium.copy(fontFamily = OneUiSans, letterSpacing = 0.sp),
        labelSmall = base.labelSmall.copy(fontFamily = OneUiSans, letterSpacing = 0.sp)
    )
}

val OneUiShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp), large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)
