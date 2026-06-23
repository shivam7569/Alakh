package com.andy.alakh.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.andy.alakh.health.PermissionHelper
import com.andy.alakh.health.WorkoutSensors
import com.andy.alakh.presentation.breathing.BreathingCategoriesScreen
import com.andy.alakh.presentation.breathing.BreathingCategoryScreen
import com.andy.alakh.presentation.breathing.BreathingDetailScreen
import com.andy.alakh.presentation.breathing.BreathingNav
import com.andy.alakh.presentation.breathing.BreathingRunScreen
import com.andy.alakh.presentation.exercises.CatalogNav
import com.andy.alakh.presentation.exercises.ExerciseDetailHolder
import com.andy.alakh.presentation.exercises.ExerciseDetailScreen
import com.andy.alakh.presentation.exercises.ExercisesScreen
import com.andy.alakh.presentation.exercises.MuscleExercisesScreen
import com.andy.alakh.presentation.home.HomeScreen
import com.andy.alakh.presentation.theme.AlakhTheme
import com.andy.alakh.presentation.workout.ActiveWorkout
import com.andy.alakh.presentation.workout.RoutinesScreen
import com.andy.alakh.presentation.workout.SetEntryScreen
import com.andy.alakh.presentation.workout.WorkoutMonitorScreen
import com.andy.alakh.presentation.workout.WorkoutScreen
import com.andy.alakh.shared.data.ExerciseListItem
import com.andy.alakh.shared.model.MuscleGroup
import com.andy.alakh.shared.rules.BreathingCatalog

/** Navigation route keys. */
object Routes {
    const val HOME = "home"
    const val BREATHING_CATEGORIES = "breathing_categories"
    const val BREATHING_CATEGORY = "breathing_category"
    const val BREATHING_DETAIL = "breathing_detail"
    const val BREATHING_RUN = "breathing_run"
    const val WORKOUT = "workout"
    const val ROUTINES = "routines"
    const val WORKOUT_MONITOR = "workout_monitor"
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
        val context = LocalContext.current

        // Centralized workout start: request HR/notification permissions, kick off the sensor
        // service, create the in-memory session, then open the exercise picker. Used by both the
        // ad-hoc path and the (placeholder) routine path. Logging still works if permission is denied.
        val sensorPermLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) {
            ActiveWorkout.startIfNeeded()
            WorkoutSensors.start(context)
            navController.navigate(Routes.WORKOUT_PICK)
        }
        val beginWorkout: () -> Unit = {
            val missing = PermissionHelper.missingSensorPermissions(context)
            if (missing.isEmpty()) {
                ActiveWorkout.startIfNeeded()
                WorkoutSensors.start(context)
                navController.navigate(Routes.WORKOUT_PICK)
            } else {
                sensorPermLauncher.launch(missing.toTypedArray())
            }
        }

        AppScaffold {
            SwipeDismissableNavHost(
                navController = navController,
                startDestination = Routes.HOME,
            ) {
                composable(Routes.HOME) {
                    HomeScreen(
                        onWorkout = { navController.navigate(Routes.WORKOUT) },
                        onBreathing = { navController.navigate(Routes.BREATHING_CATEGORIES) },
                        onExercises = { navController.navigate(Routes.EXERCISES) },
                    )
                }
                composable(Routes.BREATHING_CATEGORIES) {
                    BreathingCategoriesScreen(onSelectCategory = { category ->
                        BreathingNav.category = category
                        navController.navigate(Routes.BREATHING_CATEGORY)
                    })
                }
                composable(Routes.BREATHING_CATEGORY) {
                    val category = BreathingNav.category ?: BreathingCatalog.categories().first()
                    BreathingCategoryScreen(
                        category = category,
                        onSelectTechnique = { technique ->
                            BreathingNav.selected = technique
                            navController.navigate(Routes.BREATHING_DETAIL)
                        },
                    )
                }
                composable(Routes.BREATHING_DETAIL) {
                    val technique = BreathingNav.selected ?: BreathingCatalog.all.first()
                    BreathingDetailScreen(
                        technique = technique,
                        onStart = { navController.navigate(Routes.BREATHING_RUN) },
                    )
                }
                composable(Routes.BREATHING_RUN) {
                    val technique = BreathingNav.selected ?: BreathingCatalog.all.first()
                    BreathingRunScreen(technique = technique, onExit = { navController.popBackStack() })
                }
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
                        onStartAdHoc = beginWorkout,
                        onFromRoutine = { navController.navigate(Routes.ROUTINES) },
                        onAddExercise = { navController.navigate(Routes.WORKOUT_PICK) },
                        onOpenExercise = { index ->
                            ActiveWorkout.editingIndex = index
                            navController.navigate(Routes.SET_ENTRY)
                        },
                        onOpenMonitor = { navController.navigate(Routes.WORKOUT_MONITOR) },
                        onFinished = { navController.popBackStack(Routes.HOME, false) },
                    )
                }
                composable(Routes.ROUTINES) {
                    RoutinesScreen(onAddExercises = beginWorkout)
                }
                composable(Routes.WORKOUT_MONITOR) { WorkoutMonitorScreen() }
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
