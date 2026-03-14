# ARCore & ARCore Anchor Implementation Checklist

Use this list to replicate the AR + anchor behavior in another app or with another AI. Reference: [Working with Anchors](https://developers.google.com/ar/develop/anchors).

**CampusGo app (Kotlin, current):** The app uses **ARCore SDK only** (no Sceneform). See `ArTestActivity` and `ar/BackgroundRenderer.kt`: `ComponentActivity`, `GLSurfaceView` + custom `ArRenderer`, session resume after `setCameraTextureName`, hit-test anchor, world→screen projection, overlay position/scale on main thread. Same concepts below apply; implementation is Kotlin and ARCore-only.

---

## 1. Dependencies & manifest

- **Gradle (Kotlin):**
  - **Option A (current app):** `implementation("com.google.ar:core:1.52.0")` — ARCore only; you implement camera with OpenGL (see `BackgroundRenderer.kt`) and Session lifecycle.
  - **Option B (legacy):** Sceneform fork `implementation("com.gorisse.thomas.sceneform:sceneform:1.23.0")` — provides ArFragment and ARCore (Java-era API).
- **AndroidManifest.xml:**
  - `<uses-permission android:name="android.permission.CAMERA" />`
  - `<uses-feature android:name="android.hardware.camera.ar" android:required="false" />`
  - Inside `<application>`: `<meta-data android:name="com.google.ar.core" android:value="optional" />`

---

## 2. Layout: AR camera from ARCore

- **Option A (current app):** Full-screen `GLSurfaceView` in the layout; camera is drawn by a custom `GLSurfaceView.Renderer` that uses ARCore’s camera texture (see `BackgroundRenderer`). Overlays are siblings **above** the `GLSurfaceView` in the same root. Use a **transparent window** so the GL surface (behind the window) shows through.
- **Option B (Sceneform):** Full-screen `ArFragment` (`com.google.ar.sceneform.ux.ArFragment`); overlays as siblings above the fragment.

---

## 3. Get the AR frame every frame

- **Option A (current app):** Per-frame work runs on the **GL thread** inside `GLSurfaceView.Renderer.onDrawFrame`: `session.update()`, draw background, then hit-test / anchor / world→screen; post overlay position to main thread (e.g. `runOnUiThread` or `Handler`).
- **Option B (Sceneform):** Use **Choreographer** on the main thread: get `frame = sceneView.arFrame` in a `Choreographer.FrameCallback`, run every frame, schedule next with `Choreographer.getInstance().postFrameCallback(this)`.
- **Important:** Use the frame on one thread; if you do heavy work (e.g. QR decode), copy what you need then do work off-thread; **always** call `image.close()` if you used `frame.acquireCameraImage()`.

---

## 4. Create an ARCore anchor (hit-test on plane)

- **When to create:** When you have a 2D position where you want the “AR object” (e.g. when the user first sees a QR at that screen position). Store that as “pending anchor” (e.g. `pendingAnchorX`, `pendingAnchorY`).
- **How to create (each frame until done):**
  - If you have a pending position and a valid `Frame`:
    - `val hitResults = frame.hitTest(pendingX, pendingY)` — (x, y) in **screen/view coordinates** (same as your overlay root).
    - Prefer a **plane** hit so the anchor stays on a real surface:
      - Loop over `hitResults`; if `hit.trackable is Plane` and `(hit.trackable as Plane).isPoseInPolygon(hit.hitPose)`, then:
        - `arAnchor?.detach()` (if you had an old one).
        - `arAnchor = hit.createAnchor()`.
        - Clear pending (e.g. `pendingAnchorX = null`, `pendingAnchorY = null`) and break.
    - If no plane-in-polygon hit, you can fallback to the first hit: `arAnchor = hitResults[0].createAnchor()` and clear pending.
  - **Imports:** `com.google.ar.core.Anchor`, `com.google.ar.core.Frame`, `com.google.ar.core.HitResult`, `com.google.ar.core.Plane`.
- **Cleanup:** When the user leaves the AR experience or you “reset”, call `arAnchor?.detach()` and set `arAnchor = null`.

---

## 5. Position 2D overlay from the anchor (world → screen)

- **Every frame** when `arAnchor != null` and you have a valid `Frame`:
  - Get **screen position** and **depth** from the anchor (see below).
  - Use (screenX, screenY) to position your overlay (e.g. set `translationX`, `translationY` so the overlay is centered or bottom-aligned at that point).
- **World → screen projection (with depth):**
  - Get camera: `val camera = frame.camera ?: return`
  - Get matrices: `camera.getViewMatrix(viewMatrix, 0)`, `camera.getProjectionMatrix(projMatrix, 0, 0.1f, 100f)` (float[16] each).
  - Get anchor position in world: `anchor.pose.getTranslation(translation, 0)` → float[3] (x,y,z in meters).
  - Transform to view space: `point = [tx, ty, tz, 1]`; `viewXpoint = viewMatrix * point` (4D). Use `android.opengl.Matrix.multiplyMV(viewXpoint, 0, viewMatrix, 0, point, 0)`.
  - If `viewXpoint[2] >= 0` the point is behind the camera → skip or return null.
  - **Depth for scaling:** `depth = -viewXpoint[2]` (positive = in front).
  - Transform to clip space: `clip = projMatrix * viewXpoint`; if `clip[3] <= 0` skip.
  - NDC: `ndcX = clip[0]/clip[3]`, `ndcY = clip[1]/clip[3]`.
  - Screen: `screenX = (ndcX + 1f) * 0.5f * viewWidth`, `screenY = (1f - ndcY) * 0.5f * viewHeight`.
  - Return (screenX, screenY, depth) so you can also use depth for scale (see below).

---

## 6. Distance-based scale (panel smaller when user moves away)

- Store a **reference depth** the first time you successfully project the anchor: e.g. `if (referenceAnchorDepth <= 0f) referenceAnchorDepth = depth`.
- Each frame: `scale = referenceAnchorDepth / depth`, then clamp (e.g. between 0.35f and 1.05f).
- Apply to overlay: `overlay.scaleX = scale`, `overlay.scaleY = scale`.
- When you detach the anchor / reset, set `referenceAnchorDepth = 0f` so the next anchor gets a new reference.

---

## 7. Keep the overlay visible when “target” is not visible

- If the “target” (e.g. QR) is only used to **create** the anchor, then once the anchor exists, **do not** hide the overlay just because the target is no longer detected.
- Only run “target lost” logic (e.g. hide overlay after N ms with no detection) when **there is no anchor** (`arAnchor == null`). When `arAnchor != null`, keep updating overlay position from the anchor every frame and leave it visible (optionally hide only when the anchor is behind the camera, or keep last position).

---

## 8. Fallback when there is no anchor

- Before an anchor is created (or if hit-test never succeeds), you can position the overlay from something else (e.g. QR decode in the camera image). Use the same overlay root dimensions for coordinate mapping (e.g. imageToView using root width/height). When you later create an anchor, switch to anchor-based positioning only and ignore this fallback for position (but you can still use detection for “pending anchor” position or state).

---

## 9. Threading and cleanup

- **Choreographer** runs on the main thread; get `frame` and do hit-test / anchor creation / projection on main. Only offload heavy work (e.g. QR decode) to an executor; then post results back to main to update UI.
- In **onDestroy**: remove the Choreographer callback (`Choreographer.getInstance().removeFrameCallback(arFrameCallback)`), shut down any executor, and call `arAnchor?.detach()`.

---

## 10. Summary checklist

| Step | What to do |
|------|------------|
| 1 | Add Sceneform dependency + manifest CAMERA, camera.ar (optional), meta-data ar.core optional. |
| 2 | Layout: full-screen ArFragment; overlays as siblings on top (2D only). |
| 3 | Use Choreographer (not Scene.setOnUpdateListener) to get `sceneView.arFrame` every frame. |
| 4 | When you have a desired screen position: `frame.hitTest(x,y)` → prefer Plane + isPoseInPolygon → `hit.createAnchor()`; store anchor, clear pending. |
| 5 | Every frame with anchor: get camera view + projection, anchor.pose translation → view space → clip → NDC → screen (screenX, screenY, depth). |
| 6 | Position overlay with translationX/Y from (screenX, screenY); set scale from referenceDepth/depth (clamped). |
| 7 | Only hide overlay on “target lost” when arAnchor == null; when anchor exists, keep overlay and update from anchor. |
| 8 | Fallback: position from QR/detection when no anchor; use same root dimensions for coordinates. |
| 9 | Cleanup: remove frame callback, detach anchor, shutdown executor in onDestroy. |

---

## Quest QR format

**How the QR works is defined in the API documentation.** See **[api-docs/quests.md](api-docs/quests.md)** — section **"Quests — Resolve from QR (step 1.3)"**.

- **Canonical format (per API):** QR codes encode a **full URL** whose path is `/quests/{quest_id}/stages/{stage_id}`. Example: `https://your-domain.com/quests/5/stages/12`. The app sends that URL as the `qr` query parameter; the backend parses the path and returns `quest_id`, `stage_id`, `can_join`, `can_play`, etc.
- **Resolve API:** `GET /api/quests/resolve` accepts either `qr=<url>` or `quest_id` + `stage_id`. So the app can send the raw scanned string as `qr`, or parse the URL (or a short form) and call resolve with ids.

The app uses **ZXing** (no ML Kit) for decoding. Same decoder in the standalone scanner (CameraX `ImageProxy`) and in AR (grayscale bitmap from ARCore frame). See `com.campusgomobile.scanner.QrDecoder` and `parseQuestQrPayload()`; the parser supports the canonical URL path and an optional short form `questId:stageId`.

---

## Optional: Getting camera image for QR (or other CV) from ARCore

- Inside the Choreographer callback (throttle, e.g. every 25–33 ms): `val image = frame.acquireCameraImage()`.
- **Must** call `image.close()` as soon as possible (same thread is safest). Copy the Y plane (or whatever you need) into a byte array, then close the image, then run decode on a background thread.
- Image format is typically `ImageFormat.YUV_420_888`; plane 0 is Y; respect `rowStride` and `pixelStride` when copying. You can downsample (e.g. every 2nd row/column) to reduce work and improve performance.
- Map decoded coordinates from image space to **overlay (root) view** size so (x,y) match the same coordinate system as hit-test and overlay positioning.
- **CampusGo:** Use `QrDecoder.decodeFromBitmap(bitmap)` with a grayscale bitmap built from the Y plane; no ML Kit dependency. Same ZXing decoder as the scanner screen.

---

## Anchor at QR code (CampusGo implementation)

When the user opens AR from the scanner (with a scanned QR), the app can place the AR anchor **at the QR code** instead of at screen center:

1. **Launch with QR**: Pass `EXTRA_SCANNED_QR` so `ArTestActivity` sets `useQrAnchor = true` and shows: "Point camera at the QR code to place the object."
2. **On the GL thread** (in the renderer): Throttle (e.g. every 20 frames), call `frame.acquireCameraImage()`, copy the Y plane to a byte array, then `image.close()`. Post the buffer to a single-thread executor.
3. **On the executor**: Build a grayscale bitmap from the Y plane and call **`QrDecoder.decodeFromBitmap(bitmap)`** (ZXing, lightweight). Optionally use the decoded text to confirm quest/stage; for anchor placement you need the barcode’s **bounding box center** in image coordinates — either use ZXing’s `Result` result points (if available) or run a simple detector for QR finder pattern center. Store as `(centerX, centerY, imageWidth, imageHeight)`.
4. **Store pending**: Set an `AtomicReference<QrDetection>` with the center and dimensions.
5. **Next frame**: If `qrPending` is set, convert image coordinates to **view coordinates** using display rotation (0°/90°/180°/270°), then `frame.hitTest(viewX, viewY)` and create the anchor from the hit (prefer plane + `isPoseInPolygon`, else first hit). Clear the pending. The AR overlay then tracks the anchor at the QR’s position.
6. **Coordinate conversion**: Map image (sensor) → view (display) using the same rotation as `session.setDisplayGeometry(rotation, width, height)` so hit-test and overlay use the same coordinate system.
