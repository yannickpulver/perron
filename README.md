# 🚂 Perron

**Next departures on your wrist.** A Wear OS complication that shows upcoming public transport departures based on your location.

## Features

- 📍 **Auto-detect** — Finds the closest station to you
- ⏱️ **Live departures** — Shows next 2 departure times on your watch face
- 🔄 **Auto-refresh** — Updates every 10 minutes
- 🛤️ **Configurable routes** — Choose which lines to display
- 🇨🇭 **Swiss transport data** — Powered by [transport.opendata.ch](https://transport.opendata.ch/)

## Setup

1. Install on your Wear OS device
2. Grant location permissions
3. Add the **Perron** complication to your watch face
4. (Optional) Open the app to configure preferred routes

## Permissions

| Permission | Purpose |
|---|---|
| Location | Detect nearest station |
| Internet | Fetch departure data |

## Build

```bash
./gradlew :Wearable:installDebug
```

## License

Apache 2.0
