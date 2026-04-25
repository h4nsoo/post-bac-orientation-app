package com.example.orientation_app

import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.orientation_app.ui.screens.confirmation.ConfirmationScreen
import com.example.orientation_app.ui.screens.next.NextPageScreen
import com.example.orientation_app.ui.screens.score.ScoreEntryScreen
import com.example.orientation_app.ui.screens.welcome.WelcomeScreen
import com.example.orientation_app.ui.theme.UnicompassTheme
import com.example.orientation_app.viewmodel.ScoreViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        val splashStart = SystemClock.uptimeMillis()
        splashScreen.setKeepOnScreenCondition {
            SystemClock.uptimeMillis() - splashStart < 650
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UnicompassTheme {
                UnicompassNavHost()
            }
        }
    }
}

private const val ANIM_DURATION = 380

private fun forwardEnterTransition(): EnterTransition =
    slideInHorizontally(
        initialOffsetX = { it },
        animationSpec = tween(ANIM_DURATION, easing = FastOutSlowInEasing)
    ) + fadeIn(tween(ANIM_DURATION, easing = FastOutSlowInEasing))

private fun forwardExitTransition(): ExitTransition =
    slideOutHorizontally(
        targetOffsetX = { -(it / 6) },
        animationSpec = tween(ANIM_DURATION, easing = FastOutLinearInEasing)
    ) + fadeOut(tween(ANIM_DURATION / 2, easing = FastOutLinearInEasing))

private fun backEnterTransition(): EnterTransition =
    slideInHorizontally(
        initialOffsetX = { -(it / 6) },
        animationSpec = tween(ANIM_DURATION, easing = FastOutSlowInEasing)
    ) + fadeIn(tween(ANIM_DURATION / 2, easing = FastOutSlowInEasing))

private fun backExitTransition(): ExitTransition =
    slideOutHorizontally(
        targetOffsetX = { it },
        animationSpec = tween(ANIM_DURATION, easing = FastOutLinearInEasing)
    ) + fadeOut(tween(ANIM_DURATION, easing = FastOutLinearInEasing))

@Composable
private fun UnicompassNavHost() {
    val navController = rememberNavController()
    val scoreViewModel: ScoreViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = "welcome",
        enterTransition = { forwardEnterTransition() },
        exitTransition = { forwardExitTransition() },
        popEnterTransition = { backEnterTransition() },
        popExitTransition = { backExitTransition() }
    ) {
        composable(route = "welcome") {
            WelcomeScreen(
                onContinue = { sectionId, optionalSubject, isSportExempt ->
                    val encodedOptional = Uri.encode(optionalSubject)
                    navController.navigate("score_entry/$sectionId?optionalSubject=$encodedOptional&sportExempt=$isSportExempt")
                }
            )
        }

        composable(
            route = "score_entry/{sectionId}?optionalSubject={optionalSubject}&sportExempt={sportExempt}",
            arguments = listOf(
                navArgument("sectionId") { type = NavType.StringType },
                navArgument("optionalSubject") { type = NavType.StringType; defaultValue = "" },
                navArgument("sportExempt") { type = NavType.BoolType; defaultValue = false }
            )
        ) { backStackEntry ->
            val sectionId = backStackEntry.arguments?.getString("sectionId").orEmpty()
            val optionalSubject = Uri.decode(backStackEntry.arguments?.getString("optionalSubject").orEmpty())
            val sportExempt = backStackEntry.arguments?.getBoolean("sportExempt") ?: false

            ScoreEntryScreen(
                viewModel = scoreViewModel,
                sectionId = sectionId,
                selectedOptionalSubject = optionalSubject,
                isSportExempt = sportExempt,
                onBackClick = { navController.popBackStack() },
                onFilterClick = { navController.navigate("confirmation") }
            )
        }

        composable(route = "confirmation") {
            ConfirmationScreen(
                viewModel = scoreViewModel,
                onBackClick = { navController.popBackStack() },
                onDoneClick = { navController.navigate("next_page") }
            )
        }

        composable(route = "next_page") {
            NextPageScreen(
                onBackClick = { navController.popBackStack() },
                onContinueClick = { }
            )
        }
    }
}
