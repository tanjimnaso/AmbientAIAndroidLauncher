# Best Practices

## 1. Battery Optimization
- **True Black (#000000):** Use pure black in dark mode to turn off OLED pixels.
- **No Live Blurs:** Android's `RenderEffect` for live blur drains the battery. Use pre-rendered, static blurred PNGs for the background.
- **Lazy Loading AI:** Only instantiate Fireworks API calls/connections when the user interacts with the chat. Limit background polling.

## 2. Compose Performance
- **Derived States:** When filtering the installed apps list in the App Drawer, use `derivedStateOf` to prevent unnecessary UI recompositions.
- **Edge-to-Edge:** Use standard WindowInsets modifiers (`Modifier.imePadding()`, etc.) carefully to place elements above the keyboard without breaking the immersive view.

## 3. System Integration
- **Knox-Safe Modifications:** Do not attempt to root or delete the default One UI launcher. Instead, restrict its battery usage in settings or freeze it via ADB (`adb shell pm disable-user --user 0 com.sec.android.app.launcher`) once the custom launcher is fully stable.
- **Recents/Multitasking:** Disabling One UI Home completely might break standard Samsung recent apps/gestures. Keep it "Restricted" in battery settings as a safer alternative.

## 4. Typography Strictness
- Never hardcode font sizes or weights in individual Composables.
- Always pull from the designated Typography object (e.g., `MaterialTheme.typography.bodyLarge`) to ensure global consistency and avoid visual white noise.