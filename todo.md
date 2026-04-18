# AmbientAIAndroidLauncher TODO


---

## Operational Rules for AI Agents
- **Rule 1: No Synchronous Loading.** Never load icons or network data on the main thread.
- **Rule 2: Flat Layouts.** No cards within cards. Edge-to-edge bleed on wallpaper.
- **Rule 3: State Management.** Push transformations to ViewModels. No `derivedStateOf` thrashing.
- **Rule 4: Theme Consistency.** Always use `AmbientTheme.palette` tokens (Earthy/Muted palette).
- **Rule 5: Keep it Ambient.** High data density, low visual noise.
- **Rule 6: Grid Discipline.** 90-degree corners only. Analytical, modular spacing (2dp).
