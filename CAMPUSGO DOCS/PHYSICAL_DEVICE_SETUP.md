# Using the app on a physical Android phone

When you run the app on a **physical phone**, it cannot reach your backend at `10.0.2.2` (that address only works on the emulator). Sign-in and all API calls will fail until the app is pointed at your PC’s IP.

## Steps

1. **Find your PC’s IP address** (on the same Wi‑Fi as the phone):
   - **Windows:** `ipconfig` → look for “IPv4 Address” (e.g. `192.168.1.100`).
   - **Mac/Linux:** `ifconfig` or `ip addr` → look for your Wi‑Fi interface (e.g. `192.168.1.100`).

2. **Set the API base URL** in `app/build.gradle.kts`:
   - In `defaultConfig`, find:
     ```kotlin
     buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8000/\"")
     ```
   - Replace with your PC’s IP and port (Laravel default port is 8000):
     ```kotlin
     buildConfigField("String", "API_BASE_URL", "\"http://192.168.1.100:8000/\"")
     ```
   - Use your actual IP and a trailing slash.

3. **Run your backend** on the same machine (e.g. `php artisan serve`). It will listen on `0.0.0.0:8000` or `127.0.0.1:8000`. For the phone to reach it, the server must listen on the LAN interface:
   - Laravel: `php artisan serve --host=0.0.0.0` (so it accepts connections from the network, not only localhost).

4. **Rebuild and install** the app on your phone. Sign-in and API calls should work.

## Switching back to emulator

Change `API_BASE_URL` back to:

```kotlin
buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8000/\"")
```

Then rebuild.
