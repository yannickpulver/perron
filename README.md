# Wear OS Transport Complication

A Wear OS complication that shows next public transport departures based on your location.

## Features

- Automatically detects closest station (Wyleregg, Schönegg, or Bern Bahnhof)
- Shows next 2 departure times on your watch face
- Updates every 10 minutes
- Uses [Swiss public transport API](https://transport.opendata.ch/)

## Permissions

- Location (for station selection)
- Internet (for fetching departures)

## Setup

1. Install on Wear OS device
2. Grant location permissions
3. Add "Transport Data" complication to your watch face

## Build

```bash
./gradlew :Wearable:installDebug
```
