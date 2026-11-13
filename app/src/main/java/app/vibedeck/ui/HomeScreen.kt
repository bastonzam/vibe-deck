package app.vibedeck.ui

import android.app.Activity
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import app.vibedeck.RecentStore
import app.vibedeck.data.Game
import app.vibedeck.auth.SignInSheet
import app.vibedeck.auth.ProfileSyncBanner
import app.vibedeck.ui.add.*
import app.vibedeck.ui.components.DisplaySettingsPanel
import app.vibedeck.ui.detail.*
import app.vibedeck.ui.library.*
import app.vibedeck.ui.search.*
import app.vibedeck.ui.store.*
import app.vibedeck.ui.theme.VibeDeckTheme
import kotlinx.coroutines.launch

private const val SEARCH = "search?collection={collection}&category={category}"
private const val ADD = "add?url={url}"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(store: RecentStore, onPlay: (String) -> Boolean) {
    val context = LocalContext.current
    val app: AppViewModel = viewModel(factory = ScreenViewModelFactory { AppViewModel(context.applicationContext, store) })
    val display by app.display.collectAsState()
    val repository = app.repository
    val gate: AuthGateViewModel = viewModel(factory = ScreenViewModelFactory { AuthGateViewModel(app.auth) })
    val authState by gate.state.collectAsState()
    val user by app.auth.user.collectAsState()
    val profileSync by app.auth.profileSync.collectAsState()
    val catalogStatus by repository.catalogStatus.collectAsState()
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: "store"
    val showTabs = route == "store" || route == SEARCH || route == "library"
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var settingsOpen by remember { mutableStateOf(false) }
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(catalogStatus.error) { catalogStatus.error?.let { snackbar.showSnackbar(it) } }
    LaunchedEffect(authState.notice) { authState.notice?.let { snackbar.showSnackbar(it); gate.clearNotice() } }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) repository.refreshLibrary() }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    SideEffect {
        (context as? Activity)?.let { activity ->
            WindowCompat.getInsetsController(activity.window, activity.window.decorView).apply {
                isAppearanceLightStatusBars = !display.dark
                isAppearanceLightNavigationBars = !display.dark
            }
        }
    }
    fun play(game: Game) {
        if (!onPlay(game.url)) scope.launch { snackbar.showSnackbar("ลิงก์เกมไม่ถูกต้อง") }
        repository.refreshLibrary()
    }
    fun detail(game: Game) { nav.navigate("detail/${Uri.encode(game.id)}") { launchSingleTop = true } }
    fun tab(destination: String) {
        nav.navigate(destination) {
            popUpTo("store") { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    VibeDeckTheme(darkTheme = display.dark) {
        Scaffold(contentWindowInsets = WindowInsets.safeDrawing,
            topBar = { if (user?.uid == profileSync.uid) ProfileSyncBanner(profileSync, gate::retryProfile) },
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                if (showTabs) NavigationBar {
                    listOf(Triple("store", "Store", Icons.Default.SportsEsports), Triple("search", "ค้นหา", Icons.Default.Search),
                        Triple("library", "คลังของฉัน", Icons.Default.CollectionsBookmark)).forEach { (target, label, icon) ->
                        NavigationBarItem(selected = if (target == "search") route == SEARCH else route == target,
                            onClick = { tab(target) }, icon = { Icon(icon, null) }, label = { Text(label) })
                    }
                }
            }) { padding ->
            NavHost(navController = nav, startDestination = "store", modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
                composable("store") {
                    val vm: StoreViewModel = viewModel(factory = ScreenViewModelFactory { StoreViewModel(repository) })
                    val state by vm.state.collectAsState()
                    StoreScreen(state, vm::selectCategory, ::detail, ::play, { tab("search") }, { nav.navigate("add") }, { collection ->
                        nav.navigate("search?collection=${Uri.encode(collection)}&category=${Uri.encode(state.category)}")
                    }, vm::refresh)
                }
                composable(SEARCH, arguments = listOf(
                    navArgument("collection") { type = NavType.StringType; defaultValue = "" },
                    navArgument("category") { type = NavType.StringType; defaultValue = "ทั้งหมด" },
                )) { backStack ->
                    val vm: SearchViewModel = viewModel(factory = ScreenViewModelFactory {
                        SearchViewModel(repository, backStack.arguments?.getString("collection").orEmpty(), backStack.arguments?.getString("category") ?: "ทั้งหมด")
                    })
                    val state by vm.state.collectAsState()
                    SearchScreen(state, vm::query, vm::category, vm::tool, vm::orientation, vm::saveSearch, vm::clearHistory, vm::clearFilters, ::detail, vm::refresh)
                }
                composable("library") {
                    val vm: LibraryViewModel = viewModel(factory = ScreenViewModelFactory { LibraryViewModel(repository) })
                    val state by vm.state.collectAsState()
                    LaunchedEffect(Unit) { vm.refresh() }
                    LibraryScreen(state, vm::selectTab, ::play, ::detail, vm::toggleFavorite, { tab("store") }, { settingsOpen = true }, vm::refresh)
                }
                composable("detail/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { backStack ->
                    val id = backStack.arguments?.getString("id").orEmpty()
                    val vm: DetailViewModel = viewModel(factory = ScreenViewModelFactory { DetailViewModel(repository, id, app.auth) })
                    val state by vm.state.collectAsState()
                    val games by repository.games.collectAsState()
                    val similar = games.filter { it.id != id && it.tags.any { tag -> tag in state.game?.tags.orEmpty() } }.take(6)
                    DetailScreen(state, similar, { nav.popBackStack() }, ::play, vm::toggleFavorite, ::detail,
                        onRate = { value -> gate.requireLogin { vm.rate(value) } }, onCommentText = vm::commentText,
                        onSendComment = { gate.requireLogin(vm::sendComment) }, onDeleteComment = vm::deleteComment,
                        onReport = { reason, commentId -> gate.requireLogin { vm.report(reason, commentId) } },
                        onLoadMore = { vm.loadComments() }, onRefreshComments = { vm.loadComments(true) },
                        onRetry = vm::listen, onMessageShown = vm::clearMessage,
                        onAdd = { url -> nav.navigate("add?url=${Uri.encode(url)}") })
                }
                composable(ADD, arguments = listOf(navArgument("url") { type = NavType.StringType; defaultValue = "" })) { backStack ->
                    val vm: AddGameViewModel = viewModel(factory = ScreenViewModelFactory { AddGameViewModel(repository) })
                    val state by vm.state.collectAsState()
                    LaunchedEffect(Unit) {
                        val url = backStack.arguments?.getString("url").orEmpty()
                        if (url.isNotBlank() && state.input.isBlank()) { vm.input(url); vm.preview() }
                    }
                    AddGameScreen(state, vm::input, vm::preview, ::play, { gate.requireLogin(vm::submit) }, { nav.popBackStack() },
                        vm::title, vm::description, vm::image, vm::tags, vm::orientation,
                        onSaved = { id -> nav.navigate("detail/${Uri.encode(id)}") })
                }
            }
        }
        if (settingsOpen) ModalBottomSheet(onDismissRequest = { settingsOpen = false }) {
            DisplaySettingsPanel(display, app::safeArea, app::orientation, app::dark,
                userName = user?.displayName, onAccount = { settingsOpen = false; gate.open() },
                onSignOut = { gate.signOut() })
        }
        SignInSheet(authState, gate::dismiss) { (context as? Activity)?.let(gate::signIn) }
    }
}
