package pl.rosstracker

import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import pl.rosstracker.notify.Notifier
import pl.rosstracker.ui.RossTrackerTheme
import pl.rosstracker.ui.calm
import pl.rosstracker.ui.common.LocalNavAnimatedScope
import pl.rosstracker.ui.common.LocalSharedTransitionScope
import pl.rosstracker.ui.isDarkTheme
import pl.rosstracker.ui.add.AddProductDialog
import pl.rosstracker.ui.detail.ProductDetailScreen
import pl.rosstracker.ui.list.ProductListScreen

class MainActivity : ComponentActivity() {

    /** Text shared from another app; non-null opens the add dialog. */
    private var sharedText by mutableStateOf<String?>(null)

    /** Labels pre-ticked in the add dialog: the open label tab when adding with +, none from the share sheet. */
    private var addToLabels by mutableStateOf(emptySet<Long>())

    /** Product to open, set when the user taps a notification. */
    private var pendingProductId by mutableStateOf<Long?>(null)

    @OptIn(ExperimentalSharedTransitionApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) handleIntent(intent)

        setContent {
            val themeMode by ServiceLocator.theme.mode.collectAsStateWithLifecycle()
            val dark = isDarkTheme(themeMode)
            // Status/navigation bar icons follow the app theme, not only the system one.
            LaunchedEffect(dark) {
                val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            RossTrackerTheme(dark) {
                val nav = rememberNavController()
                val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

                LaunchedEffect(pendingProductId) {
                    pendingProductId?.let {
                        nav.navigate("product/$it") { launchSingleTop = true }
                        pendingProductId = null
                    }
                }

                // Shared layout lets the product photo glide between the grid card and the detail hero.
                SharedTransitionLayout {
                    CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                        NavHost(
                            navController = nav,
                            startDestination = "list",
                            enterTransition = { fadeIn(calm(450)) },
                            exitTransition = { fadeOut(calm(350)) },
                            popEnterTransition = { fadeIn(calm(450)) },
                            popExitTransition = { fadeOut(calm(350)) },
                            // The default back-gesture animation shrinks the page to 70%, which fights
                            // the photo gliding back into its card; use the same fade as the back arrow.
                            predictivePopEnterTransition = { fadeIn(calm(450)) },
                            predictivePopExitTransition = { fadeOut(calm(350)) },
                        ) {
                            composable("list") {
                                CompositionLocalProvider(LocalNavAnimatedScope provides this) {
                                    ProductListScreen(
                                        onOpenProduct = { nav.navigate("product/$it") },
                                        onAddClick = { labelId ->
                                            addToLabels = setOfNotNull(labelId)
                                            sharedText = ""
                                        },
                                    )
                                }
                            }
                            composable(
                                "product/{id}",
                                arguments = listOf(navArgument("id") { type = NavType.LongType }),
                            ) { entry ->
                                CompositionLocalProvider(LocalNavAnimatedScope provides this) {
                                    ProductDetailScreen(
                                        productId = entry.arguments!!.getLong("id"),
                                        onBack = { nav.popBackStack() },
                                    )
                                }
                            }
                        }
                    }
                }

                sharedText?.let { text ->
                    AddProductDialog(
                        initialText = text,
                        initialLabels = addToLabels,
                        onDismiss = { sharedText = null },
                        onAdded = {
                            sharedText = null
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !Notifier.canNotify(this)) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        when {
            intent.action == Intent.ACTION_SEND && intent.type == "text/plain" -> {
                addToLabels = emptySet()
                sharedText = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
            }
            intent.hasExtra(EXTRA_PRODUCT_ID) ->
                pendingProductId = intent.getLongExtra(EXTRA_PRODUCT_ID, -1).takeIf { it >= 0 }
        }
    }

    companion object {
        const val EXTRA_PRODUCT_ID = "product_id"
    }
}
