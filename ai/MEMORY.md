# MEMORY — Workspace decisions (mutable)

This file holds decisions made during the project. Append only, so the agent can consult it.

## Initial baseline decisions (seed)
- chunk_seconds=30
- chunk_overlap_seconds=2
- persistence=Room (single source of truth)
- background_scheduler=WorkManager
- recording_service=ForegroundService + AudioRecord (implementation chosen after confirmation)
- transcription=mock first → Whisper/Gemini behind interface
- summary_streaming=Flow<String> consumed by Compose UI
- screens=Dashboard, Recording, Summary

## Usage
- Agents should append decisions here after user confirmation.
- Keep entries concise and timestamped when possible.