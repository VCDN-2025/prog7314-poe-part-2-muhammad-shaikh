[![Review Assignment Due Date](https://classroom.github.com/assets/deadline-readme-button-22041afd0340ce965d47ae6ef1cefeee28c7c493a6346c4f15d667ab976d596c.svg)](https://classroom.github.com/a/I9lhFGyg)

# StudySync — Student Planner (Android + .NET API)

> Plan smarter. Sync safely. Study with confidence.

**StudySync** is an Android app that helps students capture tasks fast, see what’s due, and stay organised — even with spotty connectivity. The prototype for Part 2 demonstrates Google Sign-In (SSO), core task CRUD, due dates (with local fallback), a simple calendar view, language toggle (English/Afrikaans), a light/dark theme toggle, and rotating study tips. A minimal ASP.NET Core API with JWT auth backs the app.

---

## Table of contents
- [1. Purpose of the app](#1-purpose-of-the-app)
- [2. Design considerations](#2-design-considerations)
- [3. Features implemented in the prototype](#3-features-implemented-in-the-prototype)
- [4. Architecture overview](#4-architecture-overview)
- [5. Local setup & run](#5-local-setup--run)
- [6. API quick reference](#6-api-quick-reference)
- [7. Testing & CI with GitHub Actions](#7-testing--ci-with-github-actions)
- [8. Version control workflow](#8-version-control-workflow)
- [9. Demo video](#9-demo-video)
- [10. How AI was used (short report)](#10-how-ai-was-used-short-report)
- [11. References (Harvard style)](#11-references-harvard-style)

---

## 1. Purpose of the app
Students often deal with patchy internet, limited data, and a mix of languages. Many planners are online-only, ad-heavy, or weak on privacy. **StudySync** focuses on:
- **Fast capture** of tasks in ≤ 3 taps.
- **Clarity** on what’s due and when.
- **Privacy & control** (sign-in on every launch, no sensitive data stored long-term on device).
- **Localisation** (English/Afrikaans now; extensible later).
- **Low-friction UX** with optional light/dark theme and gentle guidance via rotating study tips.

---

## 2. Design considerations
**Offline-first thinking (prototype-friendly):**  
The final design targets offline-first with background sync; in the prototype we keep it simple: tasks are stored on the API, and due dates are also cached locally.

**Simple, dependable UI:**  
- Material toolbar + two FABs (Add Task, Settings) and a Calendar FAB.  
- A **tip card header** on the task list (motivation/study tips with “Next” and “Share”).  
- **Dialogs** for add/edit actions to reduce screen count and complexity.  

**Internationalisation and theming:**  
- Runtime **language toggle** (English/Afrikaans) via `LocaleManager`.  
- **Light/Dark** theme toggle applied at Activity start via `ThemeManager`.

**Security and sign-in:**  
- **Google SSO** for identity; the API validates Google ID tokens and returns a short-lived JWT.  
- The prototype **forces sign-in on every app open** (no long-term token storage) to match the brief.

---

## 3. Features implemented in the prototype
- **Google SSO**: “Continue with Google” → server verifies ID token → client gets JWT.
- **Tasks CRUD**: Create, list, toggle done/open, delete.
- **Due dates**: Optional date/time at task creation.
- **Calendar view**: Pick a date and see due tasks for that day.
- **Settings**:
  - **Language**: English / Afrikaans.
  - **Theme**: Light / Dark / System default.
  - Example local toggle: “Weekly digest” (prototype only).
- **Tips header**: Rotating study tips with “Next” + “Share”.

---

## 4. Architecture overview
**Mobile (Android / Kotlin)**  
- UI: Material components, two FABs, list with header.  
- Networking: Retrofit + Moshi + OkHttp logging.  
- Auth: Google Sign-In → exchange ID token for API JWT.  
- Localisation: `LocaleManager` (persisted in SharedPreferences).  
- Theme: `ThemeManager` (persisted in SharedPreferences).  
- Local fallback due-date store: `LocalDueStore` (SharedPreferences per task ID).

**API (ASP.NET Core Minimal API)**  
- Auth endpoint validates Google ID token (`Google.Apis.Auth`) and issues JWT (HS256).  
- Tasks CRUD (with `DateTimeOffset?` `DueDateTime` column in final design).  
- DB: PostgreSQL via EF Core.  
- Swagger UI available in development.

**Data flow (prototype)**  
Android (SSO) → Google → returns ID token
Android → API /auth/google (ID token) → returns JWT
Android → API /tasks (JWT) → list/create/update/delete
Android ↔ LocalDueStore (fallback due date)

markdown
Copy code

---

## 5. Local setup & run

### Android app
1. **Open in Android Studio** (Giraffe+ recommended).  
2. **Configure SSO**  
   - Put your **Web client ID** in `app/src/main/res/values/strings.xml` as `server_client_id`.  
3. **Point the app to your API**  
   - In `Api.kt`, use `http://10.0.2.2:<PORT>/` for the emulator (replace `<PORT>` with your API port).  
4. **Run** the app on the emulator/phone.

### API (ASP.NET Core)
1. Set environment variables (PowerShell example):
   ```powershell
   $env:GOOGLE_WEB_CLIENT_ID = "<your-web-client-id>.apps.googleusercontent.com"
   $env:JWT_SIGNING_KEY = "<a-very-long-random-string-at-least-32-bytes>"
   $env:DB_CONNECTION = "Host=...;Port=5432;Database=...;Username=...;Password=...;SSL Mode=Require;Trust Server Certificate=true"
   $env:ASPNETCORE_URLS = "http://localhost:5041"
Apply migrations (if you’re persisting due dates):

powershell
Copy code
dotnet ef migrations add init
dotnet ef database update
Run:

powershell
Copy code
dotnet run
Swagger: http://localhost:5041/swagger.

6. API quick reference
POST /api/v1/auth/google
Exchange Google ID token → app JWT.

json
Copy code
Request: { "idToken": "..." }
Response: { "access_token": "JWT...", "user": { "sub": "...", "email": "...", "name": "..." } }
GET /api/v1/tasks
Return list of tasks (id, title, status, createdAt, updatedAt, and dueDateTime if enabled on server).

POST /api/v1/tasks
Create a task.

json
Copy code
{ "title": "POE", "dueDateTime": "2025-10-09T13:35:00Z" }   // dueDateTime optional
PUT /api/v1/tasks/{id}
Update title/status/dueDateTime (any subset).

DELETE /api/v1/tasks/{id}
Remove a task.

7. Testing & CI with GitHub Actions
This repo uses Git + GitHub for version control and introduces CI with GitHub Actions to ensure the Android app can build on a clean runner (and optionally run tests):

Android build workflow (example)
Create .github/workflows/android.yml:

yaml
Copy code
name: Android CI

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up JDK
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'

      - name: Gradle cache
        uses: gradle/actions/setup-gradle@v3

      - name: Build Debug APK
        run: ./gradlew assembleDebug

      - name: Run Unit Tests
        run: ./gradlew testDebugUnitTest

      - name: Upload APK (artifact)
        uses: actions/upload-artifact@v4
        with:
          name: app-debug
          path: app/build/outputs/apk/debug/*.apk
Why CI?

Proves the project compiles outside your machine.

Surfaces missing files or misconfigured dependencies early.

Produces a downloadable APK artifact for testers.

8. Version control workflow
Main branch is protected; use feature branches: feature/sso, feature/calendar, fix/due-label.

Frequent, small commits with meaningful messages.

Pull Requests with CI checks.

Tag releases (e.g., v0.1.0-prototype) before demo.


9. video link:


10. How AI was used (short report)
AI tooling (ChatGPT) was used to:

Draft core sections of the design (requirements, API surface, and data model) aligned with the assignment brief.

Bootstrap code snippets for Android (Retrofit service, Activities, locale/theme managers) and the .NET API (minimal endpoints, JWT issuance).

Debug specific issues rapidly (emulator base URL 10.0.2.2, Retrofit/Moshi converter error messages, JWT key length, Google SSO audience mismatch).

Explain build/CI steps and produce a working GitHub Actions workflow.

Create a small tips feature (local array + header view) to add demonstrable functionality without extra backend work.

All AI-generated content was reviewed and tested by the developer. Sensitive credentials were kept out of source control. The final code reflects iterative refinement, with manual adjustments for the project’s exact package names, dependencies, and runtime behavior.

11. References 
Android Developers. (2024) Guide to app architecture (ViewModel, lifecycle). Available at: https://developer.android.com/topic/libraries/architecture (Accessed: date).

Android Developers. (2024) Credentials and Google Sign-In on Android. Available at: https://developers.google.com/identity/sign-in/android (Accessed: date).

Square. (2024) Retrofit & OkHttp logging. Available at: https://square.github.io/retrofit/ (Accessed: date).

Google. (2024) Moshi JSON library. Available at: https://github.com/square/moshi (Accessed: date).

Microsoft. (2024) ASP.NET Core minimal APIs. Available at: https://learn.microsoft.com/aspnet/core/fundamentals/minimal-apis (Accessed: date).

Microsoft. (2024) Entity Framework Core with PostgreSQL (Npgsql). Available at: https://learn.microsoft.com/ef/core/ (Accessed: date).

Google. (2024) Google.Apis.Auth (ID token validation). Available at: https://www.nuget.org/packages/Google.Apis.Auth (Accessed: date).

GitHub Marketplace. (2024) Automated build Android app with GitHub Action. Available at: https://github.com/marketplace/actions/automated-build-android-app-with-github-action (Accessed: date).
