package br.com.cotista

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import br.com.cotista.ui.AppViewModel
import br.com.cotista.ui.UiState
import br.com.cotista.ui.screens.AddPositionSheet
import br.com.cotista.ui.screens.AgendaScreen
import br.com.cotista.ui.screens.ExploreScreen
import br.com.cotista.ui.screens.FundDetailScreen
import br.com.cotista.ui.screens.HomeScreen
import br.com.cotista.ui.screens.PortfolioScreen
import br.com.cotista.ui.theme.C
import br.com.cotista.ui.theme.CotistaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(C.Bg.toArgb()),
            navigationBarStyle = SystemBarStyle.dark(C.Surface.toArgb()),
        )
        setContent { CotistaTheme { AppRoot() } }
    }
}

private data class Tab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val tabs = listOf(
    Tab("home", "Início", Icons.Rounded.Home),
    Tab("carteira", "Carteira", Icons.Rounded.PieChart),
    Tab("agenda", "Agenda", Icons.Rounded.CalendarMonth),
    Tab("explorar", "Explorar", Icons.Rounded.Explore),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(vm: AppViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: "home"
    val isTab = tabs.any { it.route == route }

    var sheetTicker by rememberSaveable { mutableStateOf<String?>(null) }
    var sheetOpen by rememberSaveable { mutableStateOf(false) }
    val openSheet: (String?) -> Unit = { t -> sheetTicker = t; sheetOpen = true }
    val openFund: (String) -> Unit = { t -> nav.navigate("fii/$t") }

    if (!state.ready) {
        Splash(state)
        return
    }

    Scaffold(
        containerColor = C.Bg,
        bottomBar = {
            if (isTab) {
                NavigationBar(containerColor = C.Surface, tonalElevation = 0.dp) {
                    tabs.forEach { t ->
                        NavigationBarItem(
                            selected = route == t.route,
                            onClick = {
                                nav.navigate(t.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(t.icon, t.label) },
                            label = { Text(t.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = C.Mint, selectedTextColor = C.Mint, indicatorColor = C.MintDeep,
                                unselectedIconColor = C.Text3, unselectedTextColor = C.Text3,
                            ),
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (route == "carteira" && state.positions.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { openSheet(null) }, containerColor = C.Mint, contentColor = C.Bg,
                    shape = RoundedCornerShape(18.dp),
                    icon = { Icon(Icons.Rounded.Add, null) }, text = { Text("Adicionar") },
                )
            }
        },
    ) { inner ->
        val pad = PaddingValues(top = inner.calculateTopPadding(), bottom = inner.calculateBottomPadding())
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.statusBarsPadding())
            Warnings(state)
            PullToRefreshBox(
                isRefreshing = state.manualRefresh,
                onRefresh = { vm.refresh(true) },
                modifier = Modifier.weight(1f),
            ) {
                NavHost(
                    nav, startDestination = "home",
                    enterTransition = { fadeIn() + slideInHorizontally { it / 12 } },
                    exitTransition = { fadeOut() },
                    popEnterTransition = { fadeIn() },
                    popExitTransition = { fadeOut() + slideOutHorizontally { it / 12 } },
                ) {
                    composable("home") {
                        HomeScreen(state, PaddingValues(bottom = pad.calculateBottomPadding()), openFund, openSheet, vm::toggleHide) {
                            nav.navigate("agenda") { popUpTo(nav.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true }
                        }
                    }
                    composable("carteira") { PortfolioScreen(state, PaddingValues(bottom = pad.calculateBottomPadding()), openFund, openSheet, vm::removePosition) }
                    composable("agenda") { AgendaScreen(state, PaddingValues(bottom = pad.calculateBottomPadding()), openFund) }
                    composable("explorar") { ExploreScreen(state, PaddingValues(bottom = pad.calculateBottomPadding()), openFund) }
                    composable("fii/{t}") { e ->
                        val t = e.arguments?.getString("t") ?: ""
                        FundDetailScreen(t, state, onBack = { nav.popBackStack() }, onAdd = { openSheet(it) }, onEnsure = vm::ensureFund)
                    }
                }
            }
        }
    }

    if (sheetOpen) {
        AddPositionSheet(
            funds = state.fundList,
            initialTicker = sheetTicker,
            existing = state.positions.firstOrNull { it.ticker == sheetTicker },
            onDismiss = { sheetOpen = false },
            onSave = { vm.savePosition(it); sheetOpen = false },
        )
    }
}

@Composable
private fun Warnings(state: UiState) {
    AnimatedVisibility(state.warnings.isNotEmpty() || state.sync != null) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
            state.warnings.forEach { w ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(C.AmberDeep).padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.CloudOff, null, tint = C.Amber, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(w, style = MaterialTheme.typography.labelMedium, color = C.Amber)
                }
                Spacer(Modifier.height(4.dp))
            }
            state.sync?.let { (d, t) ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                    CircularProgressIndicator(Modifier.size(12.dp), color = C.Mint, strokeWidth = 1.5.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Atualizando avisos de rendimentos da B3 ($d/$t)", style = MaterialTheme.typography.labelSmall, color = C.Text3)
                }
            }
        }
    }
}

@Composable
private fun Splash(state: UiState) {
    Box(Modifier.fillMaxSize().background(C.Bg), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Box(Modifier.size(84.dp).clip(RoundedCornerShape(26.dp)).background(C.MintDeep), contentAlignment = Alignment.Center) {
                Text("C", style = MaterialTheme.typography.displaySmall, color = C.Mint)
            }
            Spacer(Modifier.height(20.dp))
            Text("Cotista", style = MaterialTheme.typography.headlineMedium, color = C.Text)
            Spacer(Modifier.height(6.dp))
            Text("Dividendos de FIIs com dados oficiais", style = MaterialTheme.typography.bodyMedium, color = C.Text2)
            Spacer(Modifier.height(36.dp))
            CircularProgressIndicator(color = C.Mint, strokeWidth = 2.5.dp, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(14.dp))
            Text(state.status, style = MaterialTheme.typography.bodySmall, color = C.Text3, textAlign = TextAlign.Center)
            Text("Primeira abertura pode levar alguns segundos", style = MaterialTheme.typography.labelSmall, color = C.Text3)
        }
    }
}
