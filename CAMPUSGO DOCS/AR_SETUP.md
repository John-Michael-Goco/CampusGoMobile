# AR Setup (ARCore, Kotlin, no Sceneform)

The app uses **AR Optional**: it runs on all devices; AR is used only on ARCore-supported devices when the user opens the AR test (e.g. from the FAB).

## Stack (current, Kotlin)

- **ARCore SDK only** (`com.google.ar:core`) – no Sceneform, no AppCompat for AR.
- **Camera**: Rendered via OpenGL ES 2.0 in `BackgroundRenderer` (ARCore camera texture).
- **AR entry**: `ArTestActivity` – `ComponentActivity`, camera permission via `ActivityResultContracts.RequestPermission`, then `ArCoreApk.requestInstall()`, `Session` create/configure, `GLSurfaceView` with custom `ArRenderer`.
- **Anchors**: Hit-test (prefer plane + `isPoseInPolygon`), fallback to first hit or camera pose 0.7 m in front. World→screen projection and distance-based scale for the overlay card.

## Manifest

- `android.permission.CAMERA`
- `android.hardware.camera.ar` and `com.google.ar.core` with `android:required="false"`
- `<meta-data android:name="com.google.ar.core" android:value="optional" />`

## Before using AR

1. **Camera permission**: Request at runtime (e.g. before opening AR). `ArTestActivity` uses `registerForActivityResult(ActivityResultContracts.RequestPermission())`.
2. **ARCore installed**: From an Activity, `ArCoreApk.getInstance().requestInstall(activity, true)`. If not `InstallStatus.INSTALLED`, show a message and exit the flow.
3. **Session**: Create `Session(context)`, then `session.configure(config)` (e.g. `LATEST_CAMERA_IMAGE`, `PlaneFindingMode.HORIZONTAL`). **Resume only after** the GL camera texture is set (in the renderer’s `onSurfaceCreated`), then use `session.resume()` on the main thread.

## Anchors and overlay

- **Place anchor**: On first tracking frame, `frame.hitTest(cx, cy)` (screen center). Prefer `Plane` + `isPoseInPolygon`; else first hit; else `session.createAnchor(cameraPose.compose(Pose.makeTranslation(0, 0, -0.7f)))`.
- **World→screen**: View and projection matrices from `camera`, anchor translation from `anchor.pose`, then NDC→screen. Return `(screenX, screenY, depth)` for overlay position and scale.
- **Overlay**: Update `translationX`/`translationY` and scale (e.g. `referenceDepth / depth` clamped) on the UI thread.

## References

- [Enable AR in your Android app](https://developers.google.com/ar/develop/java/enable-arcore)
- [ARCore Anchors](https://developers.google.com/ar/reference/java/com/google/ar/core/Anchor)
- [Session configuration](https://developers.google.com/ar/develop/java/session-config)
