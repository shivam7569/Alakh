package com.andy.alakh.presentation

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.andy.alakh.presentation.breathing.BreathingScreen
import com.andy.alakh.presentation.home.HomeScreen
import com.andy.alakh.presentation.theme.AlakhTheme
import com.andy.alakh.presentation.workout.WorkoutScreen

/** Navigation route keys. */
object Routes {
    const val HOME = "home"
    const val BREATHING = "breathing"
    const val WORKOUT = "workout"
}

/** Root composable: theme + Wear navigation host wiring the three screens. */
@Composable
fun AlakhApp() {
    AlakhTheme {
        val navController = rememberSwipeDismissableNavController()
        AppScaffold {
            SwipeDismissableNavHost(
                navController = navController,
                startDestination = Routes.HOME,
            ) {
                composable(Routes.HOME) {
                    HomeScreen(
                        onWorkout = { navController.navigate(Routes.WORKOUT) },
                        onBreathing = { navController.navigate(Routes.BREATHING) },
                    )
                }
                composable(Routes.BREATHING) { BreathingScreen() }
                composable(Routes.WORKOUT) { WorkoutScreen() }
            }
        }
    }
}
