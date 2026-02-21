package com.yannickpulver.perron

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.tooling.preview.devices.WearDevices
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.EdgeButtonSize
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import android.content.Context
import kotlinx.coroutines.launch
import java.util.UUID

class RouteConfigActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                RouteConfigApp()
            }
        }
    }
}

@Composable
private fun RouteConfigApp() {
    val context = LocalContext.current
    val navController = rememberSwipeDismissableNavController()
    var selectedFrom by remember { mutableStateOf<StationInfo?>(null) }
    var selectedTo by remember { mutableStateOf<StationInfo?>(null) }
    var editingRoute by remember { mutableStateOf<Route?>(null) }

    var locationGranted by remember { mutableStateOf(false) }
    val prefs = remember { context.getSharedPreferences("perron", Context.MODE_PRIVATE) }
    var useLocation by remember { mutableStateOf(prefs.getBoolean("use_location", true)) }

    fun checkPermissions() {
        val hasFine = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        val hasBg = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        } else true
        locationGranted = hasFine && hasBg
    }

    val bgPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { checkPermissions() }

    val fgPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val fineGranted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (fineGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            bgPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            checkPermissions()
        }
    }

    LaunchedEffect(Unit) { checkPermissions() }

    SwipeDismissableNavHost(navController = navController, startDestination = "routes") {
        composable("routes") {
            RouteListScreen(
                onAddRoute = {
                    editingRoute = null
                    selectedFrom = null
                    selectedTo = null
                    navController.navigate("search_from")
                },
                onRouteClick = { route ->
                    editingRoute = route
                    selectedFrom = route.fromStation
                    selectedTo = route.toStation
                    navController.navigate("route_detail")
                },
                locationGranted = locationGranted,
                useLocation = useLocation,
                onToggleLocation = {
                    useLocation = !useLocation
                    prefs.edit().putBoolean("use_location", useLocation).apply()
                },
                onRequestPermission = {
                    fgPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            )
        }
        composable("search_from") {
            StationSearchScreen(
                title = "From Station",
                onStationSelected = { station ->
                    selectedFrom = station
                    navController.navigate("search_to")
                }
            )
        }
        composable("search_to") {
            val from = selectedFrom
            if (from != null) {
                StationSearchScreen(
                    title = "To Station",
                    onStationSelected = { toStation ->
                        selectedTo = toStation
                        navController.navigate("pick_icon")
                    }
                )
            }
        }
        composable("route_detail") {
            val route = editingRoute
            if (route != null) {
                RouteDetailScreen(
                    route = route,
                    onEdit = {
                        navController.navigate("search_from")
                    },
                    onDelete = {
                        navController.popBackStack("routes", false)
                    }
                )
            }
        }
        composable("pick_icon") {
            val from = selectedFrom
            val to = selectedTo
            if (from != null && to != null) {
                IconPickerScreen(
                    onIconSelected = { iconKey ->
                        val route = Route(
                            id = editingRoute?.id ?: UUID.randomUUID().toString(),
                            fromStation = from,
                            toStation = to,
                            icon = iconKey
                        )
                        navController.popBackStack("routes", false)
                        route
                    }
                )
            }
        }
    }
}

@Composable
private fun RouteListScreen(
    onAddRoute: () -> Unit,
    onRouteClick: (Route) -> Unit,
    locationGranted: Boolean,
    useLocation: Boolean,
    onToggleLocation: () -> Unit,
    onRequestPermission: () -> Unit
) {
    val routes by RouteRepository.observeRoutes(LocalContext.current).collectAsState(initial = emptyList())
    val listState = rememberScalingLazyListState()

    ScreenScaffold(
        scrollState = listState,
        edgeButton = {
            EdgeButton(
                onClick = onAddRoute,
                buttonSize = EdgeButtonSize.Large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onSurface,
                    contentColor = MaterialTheme.colorScheme.background,
                ),
            ) {
                Text("+")
            }
        }
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
        ) {
            item { ListHeader { Text("Routes") } }

            if (!locationGranted) {
                item {
                    FilledTonalButton(
                        onClick = onRequestPermission,
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Column {
                                Text("Enable Location", fontSize = 12.sp)
                                Text(
                                    "Auto-select nearest route",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    )
                }
            } else {
                item {
                    FilledTonalButton(
                        onClick = onToggleLocation,
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Column {
                                Text(
                                    if (useLocation) "Location: On" else "Location: Off",
                                    fontSize = 12.sp
                                )
                                Text(
                                    if (useLocation) "Auto-selecting nearest" else "Cycling through routes",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    )
                }
            }

            if (routes.isEmpty()) {
                item {
                    Text(
                        "No routes yet",
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            items(routes, key = { it.id }) { route ->
                FilledTonalButton(
                    onClick = { onRouteClick(route) },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RouteIndicator(modifier = Modifier.height(36.dp))
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(route.fromStation.name, fontSize = 12.sp, maxLines = 1)
                                Spacer(Modifier.height(4.dp))
                                Text(route.toStation.name, fontSize = 12.sp, maxLines = 1)
                            }
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                painter = painterResource(RouteIcon.fromKey(route.icon).drawableRes),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
internal fun RouteDetailScreen(
    route: Route,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberScalingLazyListState()

    ScreenScaffold(
        scrollState = listState,
        edgeButton = {
            EdgeButton(
                onClick = onEdit,
                buttonSize = EdgeButtonSize.Large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onSurface,
                    contentColor = MaterialTheme.colorScheme.background,
                ),
            ) {
                Text("Edit")
            }
        }
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
        ) {
            item { ListHeader { Text("Route") } }

            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    RouteIndicator(modifier = Modifier.height(36.dp))
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(route.fromStation.name, fontSize = 12.sp, maxLines = 1)
                        Spacer(Modifier.height(4.dp))
                        Text(route.toStation.name, fontSize = 12.sp, maxLines = 1)
                    }
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        painter = painterResource(RouteIcon.fromKey(route.icon).drawableRes),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            item {
                FilledTonalButton(
                    onClick = {
                        scope.launch {
                            RouteRepository.removeRoute(context, route.id)
                            onDelete()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Delete") }
                )
            }
        }
    }
}

@Composable
private fun StationSearchScreen(
    title: String,
    onStationSelected: (StationInfo) -> Unit
) {
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<StationInfo>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    val listState = rememberScalingLazyListState()

    fun doSearch() {
        keyboardController?.hide()
        scope.launch {
            searching = true
            results = try {
                StationSearchApi.searchStations(query)
            } catch (_: Exception) {
                emptyList()
            }
            searching = false
            if (results.isNotEmpty()) {
                listState.animateScrollToItem(2)
            }
        }
    }

    ScreenScaffold(
        scrollState = listState,
        edgeButton = {
            if (results.isEmpty()) {
                EdgeButton(
                    onClick = { doSearch() },
                    buttonSize = EdgeButtonSize.Large,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onSurface,
                        contentColor = MaterialTheme.colorScheme.background,
                    ),
                ) {
                    Icon(
                        painter = painterResource(android.R.drawable.ic_menu_search),
                        contentDescription = "Search",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState
        ) {
            item { ListHeader { Text(title) } }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.DarkGray, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (query.isEmpty()) {
                        Text("Station...", color = Color.Gray, fontSize = 14.sp)
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = {
                            query = it
                            results = emptyList()
                        },
                        textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                        cursorBrush = SolidColor(Color.White),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { doSearch() }),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (searching) {
                item {
                    Text(
                        "Searching...",
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            items(results, key = { it.id }) { station ->
                FilledTonalButton(
                    onClick = { onStationSelected(station) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(station.name) }
                )
            }
        }
    }
}

@Composable
internal fun RouteIndicator(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.width(8.dp)) {
        val dotRadius = 3.dp.toPx()
        val centerX = size.width / 2
        val topY = dotRadius + 1.dp.toPx()
        val bottomY = size.height - dotRadius - 1.dp.toPx()

        // Top dot
        drawCircle(
            color = Color.White,
            radius = dotRadius,
            center = Offset(centerX, topY)
        )

        // Dashed line between dots
        drawLine(
            color = Color.White.copy(alpha = 0.5f),
            start = Offset(centerX, topY + dotRadius + 2.dp.toPx()),
            end = Offset(centerX, bottomY - dotRadius - 2.dp.toPx()),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))
        )

        // Bottom dot
        drawCircle(
            color = Color.White,
            radius = dotRadius,
            center = Offset(centerX, bottomY)
        )
    }
}

@Composable
private fun IconPickerScreen(onIconSelected: (String) -> Route) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    ScalingLazyColumn(modifier = Modifier.fillMaxSize()) {
        item { ListHeader { Text("Pick Icon") } }

        items(RouteIcon.entries.toList()) { icon ->
            FilledTonalButton(
                onClick = {
                    scope.launch {
                        val route = onIconSelected(icon.key)
                        RouteRepository.saveOrUpdateRoute(context, route)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                icon = {
                    Icon(
                        painter = painterResource(icon.drawableRes),
                        contentDescription = icon.label,
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = { Text(icon.label) }
            )
        }
    }
}

internal val sampleRoutes = listOf(
    Route(
        id = "1",
        fromStation = StationInfo("Bern", "8507000", 46.94, 7.44),
        toStation = StationInfo("Zürich HB", "8503000", 47.38, 8.54),
        icon = "home"
    ),
    Route(
        id = "2",
        fromStation = StationInfo("Zürich HB", "8503000", 47.38, 8.54),
        toStation = StationInfo("Bern", "8507000", 46.94, 7.44),
        icon = "work"
    ),
)

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
private fun RouteListPreview() {
    MaterialTheme {
        RouteListScreen(
            onAddRoute = {},
            onRouteClick = {},
            locationGranted = true,
            useLocation = true,
            onToggleLocation = {},
            onRequestPermission = {},
        )
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
private fun StationSearchPreview() {
    MaterialTheme {
        StationSearchScreen(title = "From Station", onStationSelected = {})
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
private fun RouteDetailPreview() {
    MaterialTheme {
        RouteDetailScreen(
            route = sampleRoutes.first(),
            onEdit = {},
            onDelete = {},
        )
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
private fun RouteDetailBernZurichPreview() {
    MaterialTheme {
        RouteDetailScreen(
            route = sampleRoutes[0],
            onEdit = {},
            onDelete = {},
        )
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
private fun RouteDetailZurichBernPreview() {
    MaterialTheme {
        RouteDetailScreen(
            route = sampleRoutes[1],
            onEdit = {},
            onDelete = {},
        )
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
private fun IconPickerPreview() {
    MaterialTheme {
        IconPickerScreen(onIconSelected = { sampleRoutes.first() })
    }
}
