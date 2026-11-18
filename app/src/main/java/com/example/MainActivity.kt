package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Functions
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.AppDatabase
import com.example.data.CalculatorRepository
import com.example.data.PreferencesRepository
import com.example.ui.calculator.CalculatorScreen
import com.example.ui.calculator.CalculatorViewModel
import com.example.ui.converter.ConverterScreen
import com.example.ui.converter.ConverterViewModel
import com.example.ui.graph.GraphScreen
import com.example.ui.graph.GraphViewModel
import com.example.ui.solver.SolverScreen
import com.example.ui.solver.SolverViewModel
import com.example.ui.theme.AxiomCalcTheme
import kotlinx.coroutines.launch

const val ROUTE_COMPUTE = "compute"
const val ROUTE_SOLVE = "solve"
const val ROUTE_GRAPH = "graph"
const val ROUTE_UNITS = "units"

private data class NavDestinationItem(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val appDestinations = listOf(
    NavDestinationItem(
        route = ROUTE_COMPUTE,
        labelRes = R.string.nav_calculator,
        selectedIcon = Icons.Filled.Calculate,
        unselectedIcon = Icons.Outlined.Calculate,
        testTag = "nav_compute"
    ),
    NavDestinationItem(
        route = ROUTE_SOLVE,
        labelRes = R.string.nav_solver,
        selectedIcon = Icons.Filled.Functions,
        unselectedIcon = Icons.Outlined.Functions,
        testTag = "nav_solve"
    ),
    NavDestinationItem(
        route = ROUTE_GRAPH,
        labelRes = R.string.nav_graph,
        selectedIcon = Icons.Filled.AutoGraph,
        unselectedIcon = Icons.Outlined.AutoGraph,
        testTag = "nav_graph"
    ),
    NavDestinationItem(
        route = ROUTE_UNITS,
        labelRes = R.string.nav_converter,
        selectedIcon = Icons.Filled.Straighten,
        unselectedIcon = Icons.Outlined.Straighten,
        testTag = "nav_units"
    )
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = AppDatabase.getInstance(applicationContext)
        val prefsRepo = PreferencesRepository(applicationContext)
        val repository = CalculatorRepository(db.calculationDao(), prefsRepo)

        setContent {
            AxiomCalcRoot(repository = repository)
        }
    }
}

@Composable
fun AxiomCalcRoot(repository: CalculatorRepository) {
    val calcViewModel: CalculatorViewModel = viewModel(
        factory = CalculatorViewModel.Factory(repository)
    )
    val solverViewModel: SolverViewModel = viewModel()
    val graphViewModel: GraphViewModel = viewModel()
    val converterViewModel: ConverterViewModel = viewModel()

    val calcState by calcViewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_COMPUTE

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun showMessage(msg: String) {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(msg)
        }
    }

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    // Keep grapher angle unit synced with calculator angle unit
    LaunchedEffect(calcState.settings.angleUnit) {
        graphViewModel.setAngleUnit(calcState.settings.angleUnit)
    }

    AxiomCalcTheme(themePreference = calcState.settings.themePreference) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 720.dp

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                bottomBar = {
                    if (!useRail) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            appDestinations.forEach { dest ->
                                val selected = currentRoute == dest.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateToTab(dest.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = stringResource(dest.labelRes)
                                        )
                                    },
                                    label = { Text(stringResource(dest.labelRes)) },
                                    modifier = Modifier.testTag(dest.testTag)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useRail) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            appDestinations.forEach { dest ->
                                val selected = currentRoute == dest.route
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateToTab(dest.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = stringResource(dest.labelRes)
                                        )
                                    },
                                    label = { Text(stringResource(dest.labelRes)) },
                                    modifier = Modifier.testTag(dest.testTag)
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        NavHost(
                            navController = navController,
                            startDestination = ROUTE_COMPUTE
                        ) {
                            composable(ROUTE_COMPUTE) {
                                CalculatorScreen(
                                    viewModel = calcViewModel,
                                    onShowMessage = ::showMessage
                                )
                            }
                            composable(ROUTE_SOLVE) {
                                SolverScreen(
                                    viewModel = solverViewModel,
                                    angleUnit = calcState.settings.angleUnit,
                                    onPlotExpression = { expr ->
                                        graphViewModel.loadExternalExpression(expr)
                                        navigateToTab(ROUTE_GRAPH)
                                    },
                                    onSendToCalculator = { value ->
                                        calcViewModel.setExternalValue(value)
                                        navigateToTab(ROUTE_COMPUTE)
                                    },
                                    onShowMessage = ::showMessage
                                )
                            }
                            composable(ROUTE_GRAPH) {
                                GraphScreen(
                                    viewModel = graphViewModel,
                                    angleUnit = calcState.settings.angleUnit
                                )
                            }
                            composable(ROUTE_UNITS) {
                                ConverterScreen(
                                    viewModel = converterViewModel,
                                    onSendToCalculator = { value ->
                                        calcViewModel.setExternalValue(value)
                                        navigateToTab(ROUTE_COMPUTE)
                                    },
                                    onShowMessage = ::showMessage
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
