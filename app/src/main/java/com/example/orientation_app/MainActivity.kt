package com.example.orientation_app

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.orientation_app.ui.screens.score.ScoreEntryScreen
import com.example.orientation_app.ui.screens.welcome.WelcomeScreen
import com.example.orientation_app.ui.theme.UnicompassTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UnicompassTheme {
                UnicompassNavHost()
            }
        }
    }
}

private const val ANIM_DURATION = 450

@Composable
private fun UnicompassNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "welcome") {

        composable(
            route = "welcome",
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(ANIM_DURATION, easing = EaseInOut)
                ) + fadeOut(tween(ANIM_DURATION / 2))
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(ANIM_DURATION, easing = EaseInOut)
                ) + fadeIn(tween(ANIM_DURATION / 2))
            }
        ) {
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
            ),
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> -fullWidth },
                    animationSpec = tween(ANIM_DURATION, easing = EaseInOut)
                ) + fadeIn(tween(ANIM_DURATION / 2))
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> -fullWidth },
                    animationSpec = tween(ANIM_DURATION, easing = EaseInOut)
                ) + fadeOut(tween(ANIM_DURATION / 2))
            }
        ) { backStackEntry ->
            val sectionId = backStackEntry.arguments?.getString("sectionId").orEmpty()
            val optionalSubject = Uri.decode(backStackEntry.arguments?.getString("optionalSubject").orEmpty())
            val sportExempt = backStackEntry.arguments?.getBoolean("sportExempt") ?: false

            ScoreEntryScreen(
                sectionId = sectionId,
                selectedOptionalSubject = optionalSubject,
                isSportExempt = sportExempt,
                onBackClick = { navController.popBackStack() },
                onFilterClick = { /* TODO: navigate to results screen */ }
            )
        }
    }
}