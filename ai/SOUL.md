# SOUL — Project purpose, constraints, and high-level goals

Project: TwinMind — Android take-home (replica/minimal)

## Purpose
Provide clear, minimal product + engineering context so the agent understands *what* we are building and *how success will be measured*. This file should not lock in implementation details — it defines goals, constraints, and acceptance criteria.

## Summary (one line)
Build a minimal, robust Android app that records conversations in background, transcribes audio into ordered transcripts, and produces a streamed LLM summary — resilient to real-world interruptions.

## Primary goals
- Robust foreground/background audio recording (30s chunking with ~2s overlap).
- Transcription pipeline that preserves chunk order and retries on failure.
- Streaming structured summary UI (title, summary, action items, key points) that continues even if the app is killed.
- Demonstrate platform-level skills: services, audio focus, telephony handling, WorkManager, Room, Compose, and Hilt.

## Constraints (do not exceed these for the 48-hour scope)
- Native Android implementation only (single-platform) for the first pass. Keep module layout KMP-ready but do not implement KMP now.
- Room as single source of truth.
- Use WorkManager for durable background work.
- If time-constrained, use a mock transcription provider first and swap in a real API later.

## Success criteria (acceptance)
1. The recording service continues in background and handles phone calls, audio-focus loss, source changes, low-storage, and silence detection.
2. 30s audio chunks are saved reliably with ~2s overlap and never lost.
3. Transcripts are stored in Room in correct order; failed transcriptions retry reliably.
4. Summary generation streams to UI and continues after app process death (worker-based).
5. Code is organized, DI-enabled, and easy to explain in a short demo video.

## Operational rules for the agent
- Propose architecture options, explain tradeoffs, then ask for confirmation.
- Ask clarifying question(s) before implementing. Limit to one focused question at a time.
- Only proceed after the user explicitly confirms the proposed plan.

## Reference
- Assignment spec (detailed): see `ai/context/assignment.md`. :contentReference[oaicite:0]{index=0}
- Product reference: https://twinmind.com/

---