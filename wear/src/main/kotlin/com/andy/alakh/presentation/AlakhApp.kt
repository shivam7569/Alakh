package com.andy.alakh.presentation

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.andy.alakh.presentation.breathing.BreathingScreen
import com.andy.alakh.presentation.exercises.CatalogNav
import com.andy.alakh.presentation.exercises.ExerciseDetailHolder
import com.andy.alakh.presentation.exercises.ExerciseDetailScreen
import com.andy.alakh.presentation.exercises.ExercisesScreen
import com.andy.alakh.presentation.exercises.MuscleExercisesScreen
import com.andy.alakh.presentation.home.HomeScreen
import com.andy.alakh.presentation.theme.AlakhTheme
import com.andy.alakh.presentation.workout.ActiveWorkout
import com.andy.alakh.presentation.workout.SetEntryScreen
import com.andy.alakh.presentation.workout.WorkoutScreen
import com.andy.alakh.shared.data.ExerciseListItem
import com.andy.alakh.shared.model.MuscleGroup

/** Navigation route keys. */
object Routes {
    const val HOME = "home"
    const val BREATHING = "breathing"
    const val WORKOUT = "workout"
    const val WORKOUT_PICK = "workout_pick"
    const val SET_ENTRY = "set_entry"
    const val EXERCISES = "exercises"
    const val MUSCLE_EXERCISES = "muscle_exercises"
    const val EXERCISE_DETAIL = "exercise_detail"
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
                        onExercises = { navController.navigate(Routes.EXERCISES) },
                    )
                }
                composable(Routes.BREATHING) { BreathingScreen() }
                composable(Routes.EXERCISES) {
                    ExercisesScreen(
                        onOpenGroup = { g ->
                            CatalogNav.group = g
                            CatalogNav.picking = false
                            navController.navigate(Routes.MUSCLE_EXERCISES)
                        },
                        onSelectExercise = { item ->
                            ExerciseDetailHolder.item = item
                            navController.navigate(Routes.EXERCISE_DETAIL)
                        },
                    )
                }
                composable(Routes.EXERCISE_DETAIL) { ExerciseDetailScreen() }
                composable(Routes.MUSCLE_EXERCISES) {
                    val pick: (ExerciseListItem) -> Unit = { ex ->
                        ActiveWorkout.editingIndex = ActiveWorkout.addExercise(ex)
                        navController.navigate(Routes.SET_ENTRY) {
                            popUpTo(Routes.WORKOUT_PICK) { inclusive = true }
                        }
                    }
                    val browse: (ExerciseListItem) -> Unit = { ex ->
                        ExerciseDetailHolder.item = ex
                        navController.navigate(Routes.EXERCISE_DETAIL)
                    }
                    MuscleExercisesScreen(
                        group = CatalogNav.group ?: MuscleGroup.CHEST,
                        onSelectExercise = if (CatalogNav.picking) pick else browse,
                    )
                }
                composable(Routes.WORKOUT) {
                    WorkoutScreen(
                        onAddExercise = { navController.navigate(Routes.WORKOUT_PICK) },
                        onOpenExercise = { index ->
                            ActiveWorkout.editingIndex = index
                            navController.navigate(Routes.SET_ENTRY)
                        },
                        onFinished = { navController.popBackStack(Routes.HOME, false) },
                    )
                }
                composable(Routes.WORKOUT_PICK) {
                    ExercisesScreen(
                        onOpenGroup = { g ->
                            CatalogNav.group = g
                            CatalogNav.picking = true
                            navController.navigate(Routes.MUSCLE_EXERCISES)
                        },
                        onSelectExercise = { exercise ->
                            ActiveWorkout.editingIndex = ActiveWorkout.addExercise(exercise)
                            navController.navigate(Routes.SET_ENTRY) {
                                popUpTo(Routes.WORKOUT_PICK) { inclusive = true }
                            }
                        },
                    )
                }
                composable(Routes.SET_ENTRY) {
                    SetEntryScreen(onLogged = { navController.popBackStack() })
                }
            }
        }
    }
}
