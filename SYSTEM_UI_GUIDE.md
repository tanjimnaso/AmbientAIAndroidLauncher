# System UI & Immersive Mode Configuration

This document outlines the configuration required to maintain a seamless, "borderless" launcher experience on Android 16, specifically ensuring that status/navigation bars remain invisible without triggering Android's default "2-swipe" immersive mode behavior.

## The Goal
*   **Visual:** Status bar and navigation bar must be completely transparent, blending seamlessly into the `WallpaperBackplane`.
*   **Interaction:** A single downward swipe from the top must trigger the notification shade immediately (no 2-swipe requirement).
*   **Cleanliness:** The navigation gesture hint (pill) must be removed.

## 1. Code-Level Configuration (MainActivity.kt)
Do not use `WindowInsetsControllerCompat.hide()` on system bars. While it hides the bars, it forces the system to treat the app as "Immersive," requiring two swipes to interact with the notification shade.

Instead, configure the `Window` directly to remain transparent:

```kotlin
// In MainActivity.onCreate
window.statusBarColor = android.graphics.Color.TRANSPARENT
window.navigationBarColor = android.graphics.Color.TRANSPARENT

// Disable system contrast enforcement to prevent grey/black bars from appearing
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
    window.isNavigationBarContrastEnforced = false
    window.isStatusBarContrastEnforced = false
}

// Keep the bars "visible" so the system doesn't trigger 2-swipe mode
WindowInsetsControllerCompat(window, window.decorView).apply {
    show(WindowInsetsCompat.Type.systemBars())
}
```

## 2. Global ADB Configuration
If the system bar behavior starts requiring two swipes, it means a global Android `policy_control` flag has been set by a previous session or tool.

**To reset and enable 1-swipe behavior:**
```bash
adb shell settings put global policy_control null
```

**To hide the navigation gesture hint (the pill at the bottom):**
```bash
adb shell settings put global navigation_bar_gesture_hint 0
```

## Troubleshooting
*   **If you see black bars:** Ensure `isStatusBarContrastEnforced` is `false` and that the `Window` is explicitly set to `TRANSPARENT`.
*   **If you need 2 swipes to open notifications:** Check if `policy_control` is set to `immersive.full=*`. Reset it to `null`.
*   **If icons are still visible:** Use Samsung "Good Lock" (QuickStar / NavStar) to hide specific system icons within those bars. The code above only handles the container transparency.
