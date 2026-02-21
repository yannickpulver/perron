package com.yannickpulver.perron

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.tooling.preview.devices.WearDevices
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(device = WearDevices.SMALL_ROUND, showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun ScreenshotRouteList() {
    MaterialTheme {
        val listState = rememberScalingLazyListState()
        ScreenScaffold(scrollState = listState) {
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
            ) {
                item { ListHeader { Text("Routes") } }
                items(sampleRoutes) { route ->
                    FilledTonalButton(
                        onClick = {},
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
}

@PreviewTest
@Preview(device = WearDevices.SMALL_ROUND, showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun ScreenshotBernZurich() {
    MaterialTheme {
        RouteDetailScreen(
            route = sampleRoutes[0],
            onEdit = {},
            onDelete = {},
        )
    }
}

@PreviewTest
@Preview(device = WearDevices.SMALL_ROUND, showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun ScreenshotZurichBern() {
    MaterialTheme {
        RouteDetailScreen(
            route = sampleRoutes[1],
            onEdit = {},
            onDelete = {},
        )
    }
}
