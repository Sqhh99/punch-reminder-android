# Repository Guidelines

## Project Structure & Module Organization

This repository is an Android app built with a single Gradle module, `:app`. Run project-level commands from the repository root, one directory above `icons/`.

- `app/src/main/java/com/sqhh99/punchreminder/`: Kotlin source code.
- `app/src/main/java/com/sqhh99/punchreminder/ui/`: Jetpack Compose screens and theme code.
- `app/src/main/java/com/sqhh99/punchreminder/domain/`: business models, validation, scheduling, and use cases.
- `app/src/main/java/com/sqhh99/punchreminder/data/`: persistence DTOs, mappers, and repositories.
- `app/src/main/java/com/sqhh99/punchreminder/system/`: Android platform integrations such as alarms, notifications, receivers, permissions, and app launching.
- `app/src/test/java/com/sqhh99/punchreminder/`: local JVM tests.
- `app/src/main/res/` and `icons/android/`: Android resources and launcher icon assets.
- `.github/workflows/`: PR, main, and release CI workflows.

## Build, Test, and Development Commands

- `./gradlew assembleDebug`: builds the debug APK.
- `./gradlew testDebugUnitTest`: runs local unit tests.
- `./gradlew lint`: runs Android Lint and fails on lint errors.
- `./gradlew lint testDebugUnitTest assembleDebug --stacktrace`: mirrors the PR validation path.

Use JDK 17 and Android SDK platform 35, matching the Gradle configuration.

## Coding Style & Naming Conventions

Use Kotlin with 4-space indentation and idiomatic Android naming. Keep Compose UI in `ui`, state holders in `viewmodel`, business rules in `domain`, storage in `data`, and direct Android API wrappers in `system`. UI code should not call `AlarmManager`, `PackageManager`, or `NotificationManager` directly; route those interactions through `system` abstractions.

## Testing Guidelines

Tests use JUnit under `app/src/test`. Name test files after the unit under test, for example `TaskValidatorTest.kt` or `NextTriggerTimeCalculatorTest.kt`. Prefer deterministic tests for domain logic, scheduling calculations, validation, repository mapping, and use-case decisions. Run `./gradlew testDebugUnitTest` before opening a PR.

## Commit & Pull Request Guidelines

Use branch names such as `feature/*`, `fix/*`, `test/*`, or `ci/*`. Commit prefixes observed in this project include `feat`, `fix`, `test`, `ci`, `docs`, `refactor`, and `chore`. PRs should describe the change, mention linked issues when applicable, include screenshots for UI changes, and pass CI before merge.

## Security & Configuration Tips

Do not commit signing keys, credentials, or local SDK paths. Release signing material belongs in GitHub Secrets; local debug builds should use the standard debug signing flow.
