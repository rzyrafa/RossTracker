package pl.rosstracker.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.rosstracker.R

// ---- Fonts: Playfair Display for headings, Inter for everything else (both variable, bundled). ----

private fun playfair(weight: Int) = Font(
    R.font.playfair_display,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

private fun inter(weight: Int) = Font(
    R.font.inter,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val Serif = FontFamily(playfair(400), playfair(500), playfair(600))
val Sans = FontFamily(inter(400), inter(500), inter(600), inter(700))

private val AppTypography = Typography(
    displaySmall = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Normal, fontSize = 34.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Normal, fontSize = 28.sp, lineHeight = 34.sp),
    headlineSmall = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Normal, fontSize = 24.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Medium, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Medium, fontSize = 17.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = Sans, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Sans, fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = Sans, fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp),
)

/** Small uppercase, letter-spaced label ("JAPANDI STYLE" in the reference design). */
val Eyebrow = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 1.4.sp)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

// ---- Colours: warm cream + espresso, terracotta accent. Night mode is "dark roast". ----

/** Colours outside the Material scheme. */
@Immutable
data class CoffeeColors(
    val accent: Color,
    val onAccent: Color,
    val accentSoft: Color,
    val muted: Color,
    val chip: Color,
    val imageTile: Color,
    /** Label palette, indexed by `LabelEntity.color`; muted so labels sit with the coffee tones. */
    val labels: List<Color>,
    /** Text on a filled label chip. */
    val onLabel: Color,
) {
    fun label(index: Int): Color = labels[index.mod(labels.size)]
}

/** Names match the order of both palettes; shown as swatch descriptions for accessibility. */
val LabelColorNames = listOf("terakota", "szałwia", "ochra", "niebieski", "śliwka", "róż", "oliwka", "grafit")

private val LightCoffee = CoffeeColors(
    accent = Color(0xFFB4583F),
    onAccent = Color(0xFFFFF8F2),
    accentSoft = Color(0xFFF3DED4),
    muted = Color(0xFF8A7E72),
    chip = Color(0xFFEAE3D8),
    // Product photos come on white; a white tile keeps them seamless.
    imageTile = Color(0xFFFFFFFF),
    labels = listOf(
        Color(0xFFB4583F), Color(0xFF6E8B6A), Color(0xFFB8862F), Color(0xFF5B7A99),
        Color(0xFF7D5A7A), Color(0xFFB5676F), Color(0xFF7F7F3E), Color(0xFF5F6670),
    ),
    onLabel = Color(0xFFFFF8F2),
)

private val DarkCoffee = CoffeeColors(
    accent = Color(0xFFDB8E70),
    onAccent = Color(0xFF2A1A14),
    accentSoft = Color(0xFF4A2F25),
    muted = Color(0xFFA5998C),
    chip = Color(0xFF332C26),
    // Slightly dimmed white so photos don't glare at night.
    imageTile = Color(0xFFEDE7DF),
    labels = listOf(
        Color(0xFFDB8E70), Color(0xFF9DB896), Color(0xFFD9AE5F), Color(0xFF8EADCB),
        Color(0xFFB08DAD), Color(0xFFDE939A), Color(0xFFB3B36B), Color(0xFF9AA1AB),
    ),
    onLabel = Color(0xFF1B1714),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF2B2420),
    onPrimary = Color(0xFFF7F2EA),
    secondary = Color(0xFFB4583F),
    onSecondary = Color(0xFFFFF8F2),
    background = Color(0xFFF4EFE8),
    onBackground = Color(0xFF2B2420),
    surface = Color(0xFFF4EFE8),
    onSurface = Color(0xFF2B2420),
    surfaceVariant = Color(0xFFEAE3D8),
    onSurfaceVariant = Color(0xFF6F645A),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFBF8F3),
    surfaceContainer = Color(0xFFF8F4EE),
    surfaceContainerHigh = Color(0xFFFBF8F3),
    surfaceContainerHighest = Color(0xFFEFE8DE),
    outline = Color(0xFFD8CEC1),
    outlineVariant = Color(0xFFE6DED3),
    error = Color(0xFFA5402D),
    errorContainer = Color(0xFFF6DED6),
    onErrorContainer = Color(0xFF5C1E12),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFF1EAE0),
    onPrimary = Color(0xFF1B1714),
    secondary = Color(0xFFDB8E70),
    onSecondary = Color(0xFF2A1A14),
    background = Color(0xFF1B1714),
    onBackground = Color(0xFFF1EAE0),
    surface = Color(0xFF1B1714),
    onSurface = Color(0xFFF1EAE0),
    surfaceVariant = Color(0xFF332C26),
    onSurfaceVariant = Color(0xFFBDB1A4),
    surfaceContainerLowest = Color(0xFF15120F),
    surfaceContainerLow = Color(0xFF25201C),
    surfaceContainer = Color(0xFF28221E),
    surfaceContainerHigh = Color(0xFF2E2823),
    surfaceContainerHighest = Color(0xFF38312B),
    outline = Color(0xFF4E453E),
    outlineVariant = Color(0xFF3B342E),
    error = Color(0xFFF0A28C),
    errorContainer = Color(0xFF4A2A21),
    onErrorContainer = Color(0xFFFBDCD2),
)

private val LocalCoffee = staticCompositionLocalOf { LightCoffee }

val coffee: CoffeeColors
    @Composable @ReadOnlyComposable
    get() = LocalCoffee.current

// ---- Motion: one calm easing used everywhere. ----

/** Soft ease-out, similar to iOS' default curve. */
val CalmEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

fun <T> calm(durationMillis: Int = 450, delayMillis: Int = 0): FiniteAnimationSpec<T> =
    tween(durationMillis, delayMillis, CalmEasing)

@Composable
fun isDarkTheme(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun RossTrackerTheme(dark: Boolean, content: @Composable () -> Unit) {
    // No Material You dynamic colours on purpose: the coffee palette is the look.
    CompositionLocalProvider(LocalCoffee provides if (dark) DarkCoffee else LightCoffee) {
        val colors = if (dark) DarkColors else LightColors
        MaterialTheme(colorScheme = colors, typography = AppTypography, shapes = AppShapes) {
            // Screens draw their own background instead of sitting in a Surface, so un-coloured
            // Text would otherwise fall back to black and vanish in night mode.
            CompositionLocalProvider(LocalContentColor provides colors.onBackground, content = content)
        }
    }
}
