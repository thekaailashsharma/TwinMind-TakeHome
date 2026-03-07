# TOOLS — Allowed libraries, APIs, and patterns

This file lists recommended tools the agent may use. The agent may propose alternatives, but must explain tradeoffs.

## Android platform
- Jetpack Compose (UI)
- Hilt (DI)
- Room (local persistence — single source of truth)
- WorkManager (durable background work)
- ForegroundService (audio recording)
- AudioRecord / MediaRecorder (capture audio; AudioRecord preferred for control)
- AudioManager / AudioFocus (audio focus handling)
- TelephonyManager / PhoneStateListener (call interruptions)
- BroadcastReceiver (headset and source changes)
- StatFs (storage checks)
- Timber or Android Log (logging)

## Networking / LLM / ASR
- Retrofit + OkHttp (HTTP clients)
- WebSocket or SSE (optional for streaming token APIs)
- OpenAI Whisper (cloud ASR) OR a mock ASR for first pass
- OpenAI / Google Gemini / other LLMs for summary (mockable for the demo)

## Dev & testing
- adb (shell / stop/start service tests)
- Emulator + physical device testing for audio
- Gradle build variants / buildConfig flags for MOCK=true

## Security & config
- API keys in `local.properties` or encrypted env; do not commit secrets.

## Recommended pattern
1. Implement a mock provider interface (e.g., `AsrProvider`) — get E2E working.
2. Replace mock with real provider behind the same interface.
3. Use WorkManager + DAO patterns so workers can be re-run safely.