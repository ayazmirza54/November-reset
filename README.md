# November Reset

Native, offline Android app for the **November Challenge** (Nov 1–30, 2026): 13 daily habits, 390 possible points, win at 80%.

See **[PLAN.md](PLAN.md)** for the full challenge plan (habit rules, 4-week ramp, daily blueprint, weekly check-in).

## Features
- **Material 3 × terminal UI** — [JetBrains Mono](https://github.com/JetBrains/JetBrainsMono) font (OFL 1.1, bundled), shell-prompt header with blinking cursor, `## section` headers, `// comment` hints, ASCII progress bars, M3 cards/chips/switches/text fields/navigation bar. Accent themes: phosphor green, amber, cyan, or **Material You** wallpaper colors (Android 12+). Themed monochrome launcher icon.
- **Today** — progress ring, 13 habit cards. Number habits (steps, sleep, pages, skill minutes, meditation, expenses) auto-tick when you hit the target; goals & gratitude auto-tick when written; workout vs. planned rest day with a 3-rest-days-per-week cap. Browse any past day with ‹ ›.
- **Month** — heat-map calendar of November, plus a weekly review (points, strong days, workouts).
- **Stats** — total score vs. the 312-point goal, current/best streak, strong & perfect days, totals (steps, km, sleep average, pages, skill hours, meditation, money spent) and per-habit consistency. Share your progress.
- **Plan** — rules, ramp, daily blueprint and settings: skill name, ramp-up targets, morning/evening reminder times, reset.
- Morning and evening reminder notifications (survive reboots).
- No accounts, no network, only the Material Components library — data stays on the device.

## Build the APK
GitHub Actions builds `app-debug.apk` on every push (any branch), on pull requests, or via **Actions → Build Android APK → Run workflow**. Download the `November30-debug-apk` artifact and install it on your phone (allow "install unknown apps").

Locally: Java 17, Android SDK 35, Gradle 8.9 → `gradle assembleDebug`.
