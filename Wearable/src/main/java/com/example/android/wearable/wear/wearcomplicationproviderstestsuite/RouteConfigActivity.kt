package com.example.android.wearable.wear.wearcomplicationproviderstestsuite

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
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
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

    val bgPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* best effort */ }

    val fgPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val fineGranted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (fineGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            bgPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }

    LaunchedEffect(Unit) {
        val hasFine = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (!hasFine) {
            fgPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val hasBg = context.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
            if (!hasBg) {
                bgPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
        }
    }

    SwipeDismissableNavHost(navController = navController, startDestination = "routes") {
        composable("routes") {
            RouteListScreen(
                onAddRoute = {
                    selectedFrom = null
                    selectedTo = null
                    navController.navigate("search_from")
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
        composable("pick_icon") {
            val from = selectedFrom
            val to = selectedTo
            if (from != null && to != null) {
                IconPickerScreen(
                    onIconSelected = { iconKey ->
                        val route = Route(
                            id = UUID.randomUUID().toString(),
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
private fun RouteListScreen(onAddRoute: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val routes = remember { mutableStateListOf<Route>() }

    LaunchedEffect(Unit) {
        routes.clear()
        routes.addAll(RouteRepository.getRoutes(context))
    }

    ScalingLazyColumn(modifier = Modifier.fillMaxSize()) {
        item { ListHeader { Text("Routes") } }

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
                onClick = {
                    scope.launch {
                        RouteRepository.removeRoute(context, route.id)
                        routes.remove(route)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Dots and dashed line
                        RouteIndicator(modifier = Modifier.height(36.dp))
                        Spacer(Modifier.width(8.dp))
                        // Station names
                        Column(modifier = Modifier.weight(1f)) {
                            Text(route.fromStation.name, fontSize = 12.sp, maxLines = 1)
                            Spacer(Modifier.height(4.dp))
                            Text(route.toStation.name, fontSize = 12.sp, maxLines = 1)
                        }
                        Spacer(Modifier.width(8.dp))
                        // Icon on the right
                        Icon(
                            painter = painterResource(RouteIcon.fromKey(route.icon).drawableRes),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            )
        }

        item {
            Button(
                onClick = onAddRoute,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("+ Add Route") }
            )
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
    val listState = androidx.wear.compose.foundation.lazy.rememberScalingLazyListState()

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
            // Scroll to results (item 0 = header, 1 = search row, 2+ = results)
            if (results.isNotEmpty()) {
                listState.animateScrollToItem(2)
            }
        }
    }

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState
    ) {
        item { ListHeader { Text(title) } }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color.DarkGray, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (query.isEmpty()) {
                        Text("Station...", color = Color.Gray, fontSize = 14.sp)
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                        cursorBrush = SolidColor(Color.White),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { doSearch() }),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Button(
                    onClick = { doSearch() },
                    modifier = Modifier.size(36.dp),
                    label = {
                        Icon(
                            painter = painterResource(android.R.drawable.ic_menu_search),
                            contentDescription = "Search",
                            modifier = Modifier.size(16.dp)
                        )
                    }
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

@Composable
private fun RouteIndicator(modifier: Modifier = Modifier) {
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
                        RouteRepository.addRoute(context, route)
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
