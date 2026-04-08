# Architecture

## Technology Stack
- **UI Framework:** Jetpack Compose + Material 3.
- **Language:** Kotlin.
- **Local Storage:** Room Database (for offline Notes).
- **Network/API:** Retrofit / OkHttp (for RSS fetching and Fireworks API integration).
- **Dependency Injection:** Hilt (recommended) or manual DI.

## High-Level Components
1. **MainActivity:**
   - Sets `WindowCompat.setDecorFitsSystemWindows(window, false)` for edge-to-edge.
   - Hosts the single Compose navigation graph or main state holder.
2. **State Management (ViewModels):**
   - `LauncherViewModel`: Manages the list of installed packages (`PackageManager`) and the search filter logic (`StateFlow`).
   - `DashboardViewModel`: Manages RSS feed polling and local Room DB Notes.
   - `AgenticAIViewModel`: Manages the chat session history and streaming responses from the Fireworks API.
3. **Theme Engine:**
   - A custom Compose Theme that observes the current system time to provide shifting Color Palettes (e.g., Ocean vs OLED Black).
4. **Backend/AI Integration:**
   - Connects to the Agentic AI system API (Fireworks).
   - Prioritizes web technologies, specifically relying on a Chrome PWA setup for app handling.

## Data Flow
- App queries use standard Android `PackageManager` APIs, cached in memory.
- Fireworks API queries are executed asynchronously; responses update the Compose UI seamlessly.
- RSS feeds are parsed locally after fetching via network client.