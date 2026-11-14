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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.orientation_app.domain.usecase.GetSectionSubjectsUseCase
import com.example.orientation_app.navigation.NavRoutes
import com.example.orientation_app.ui.screens.confirmation.ConfirmationScreen
import com.example.orientation_app.ui.screens.next.NextPageScreen
import com.example.orientation_app.ui.screens.results.ResultsScreen
import com.example.orientation_app.ui.screens.score.ScoreEntryScreen
import com.example.orientation_app.ui.screens.welcome.WelcomeScreen
import com.example.orientation_app.ui.theme.UnicompassTheme
import com.example.orientation_app.viewmodel.ScoreViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
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

// ── Animation constants ───────────────────────────────────────────────────────

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

// ── Navigation host ───────────────────────────────────────────────────────────

@Composable
private fun UnicompassNavHost() {
    val navController = rememberNavController()
    // ScoreViewModel is hoisted at the graph level so ScoreEntry and Confirmation
    // share the same instance and grades are not re-entered.
    val scoreViewModel: ScoreViewModel = hiltViewModel()

    NavHost(
        navController = navController,
        startDestination = NavRoutes.WELCOME,
        enterTransition = { forwardEnterTransition() },
        exitTransition = { forwardExitTransition() },
        popEnterTransition = { backEnterTransition() },
        popExitTransition = { backExitTransition() }
    ) {

        composable(route = NavRoutes.WELCOME) {
            WelcomeScreen(
                viewModel = hiltViewModel(),
                onContinue = { sectionId, optionalSubject, isSportExempt ->
                    val encodedOptional = Uri.encode(optionalSubject)
                    navController.navigate(
                        "score_entry/$sectionId?optionalSubject=$encodedOptional&sportExempt=$isSportExempt" // route built per NavRoutes.SCORE_ENTRY_PATTERN
                    )
                }
            )
        }

        composable(
            route = NavRoutes.SCORE_ENTRY_PATTERN,
            arguments = listOf(
                navArgument("sectionId")       { type = NavType.StringType },
                navArgument("optionalSubject") { type = NavType.StringType; defaultValue = "" },
                navArgument("sportExempt")     { type = NavType.BoolType;   defaultValue = false }
            )
        ) { backStackEntry ->
            val sectionId       = backStackEntry.arguments?.getString("sectionId").orEmpty()
            val optionalSubject = Uri.decode(
                backStackEntry.arguments?.getString("optionalSubject").orEmpty()
            )
            val sportExempt     = backStackEntry.arguments?.getBoolean("sportExempt") ?: false

            ScoreEntryScreen(
                viewModel              = scoreViewModel,
                sectionId              = sectionId,
                selectedOptionalSubject = optionalSubject,
                isSportExempt          = sportExempt,
                onBackClick            = { navController.popBackStack() },
                onFilterClick          = { navController.navigate(NavRoutes.CONFIRMATION) }
            )
        }

        composable(route = NavRoutes.CONFIRMATION) {
            ConfirmationScreen(
                viewModel   = scoreViewModel,
                onBackClick = { navController.popBackStack() },
                onDoneClick = { navController.navigate(NavRoutes.NEXT_PAGE) }
            )
        }

        composable(route = NavRoutes.NEXT_PAGE) {
            val scoreState by scoreViewModel.uiState.collectAsState()
            NextPageScreen(
                onBackClick     = { navController.popBackStack() },
                onContinueClick = { interestText ->
                    if (scoreState.sectionId.isBlank()) return@NextPageScreen
                    val encodedInterest = Uri.encode(interestText)
                    val fgScoreStr = Uri.encode(NavRoutes.formatScore(scoreState.fgScore))
                    navController.navigate(
                        "results/${scoreState.sectionId}/$fgScoreStr/$encodedInterest" // route built per NavRoutes.RESULTS_PATTERN
                    )
                }
            )
        }

        composable(
            route = NavRoutes.RESULTS_PATTERN,
            arguments = listOf(
                navArgument("sectionId")    { type = NavType.StringType },
                navArgument("fgScore")      { type = NavType.StringType },
                navArgument("interestText") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val sectionId    = backStackEntry.arguments?.getString("sectionId").orEmpty()
            val fgScore      = Uri.decode(
                backStackEntry.arguments?.getString("fgScore").orEmpty()
            ).toDoubleOrNull() ?: 0.0
            val interestText = Uri.decode(
                backStackEntry.arguments?.getString("interestText").orEmpty()
            )
            ResultsScreen(
                sectionId    = sectionId,
                sectionName  = GetSectionSubjectsUseCase.displayName(sectionId),
                fgScore      = fgScore,
                interestText = interestText,
                onBackClick  = { navController.popBackStack() },
                viewModel    = hiltViewModel()
            )
        }
    }
}
