package pl.rosstracker.ui.common

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import pl.rosstracker.ui.calm

/** Set around the NavHost so a product photo can glide from the grid into the detail screen. */
@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

/** The navigation destination's enter/exit scope, set per screen. */
val LocalNavAnimatedScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

/** Marks the product photo as the same element on both screens; no-op outside a shared transition. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedProductImage(productId: Long): Modifier {
    val shared = LocalSharedTransitionScope.current ?: return this
    val animated = LocalNavAnimatedScope.current ?: return this
    return with(shared) {
        this@sharedProductImage.sharedElement(
            sharedContentState = rememberSharedContentState("product-image-$productId"),
            animatedVisibilityScope = animated,
            boundsTransform = { _, _ -> calm(550) },
        )
    }
}

/** Gentle shrink while pressed. */
@Composable
fun Modifier.pressScale(interaction: MutableInteractionSource): Modifier {
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, calm(220), label = "pressScale")
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
