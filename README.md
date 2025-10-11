# Brute Morse Code Android App

## Overview
Brute Morse Code is a passive immersion Android training application designed to drill Morse code proficiency through hours-long listening sessions. Users configure a session once, press play, and let the curated audio program cycle through mathematically generated patterns and real-world communication scenarios. The goal is effortless exposure rather than interactive drills or scoring.

## Core Philosophy
- **Passive consumption**: The app acts as an automated instructor that plays structured Morse code sessions for 2–7 hours without requiring user input.
- **Mathematical coverage**: Carefully designed traversal patterns ensure that every character, digraph, and phrase receives repeated exposure without predictable loops.
- **Progressive difficulty**: Training begins with single characters and builds toward realistic amateur radio QSOs with minimal vocal support.
- **Set-and-forget experience**: After initial configuration, autoplay advances through all phases, remembers progress, and resumes playback after interruptions.

## Mathematical Learning Patterns
### 1. NestedID (Nested Incremental–Decremental)
- Forward pass: `A`, `AB`, `ABC`, …, `ABCDEFGHIJKLMNOPQRSTUVWXYZ`
- Reverse pass: `Z`, `ZY`, `ZYX`, …, `ZYXWVUTSRQPONMLKJIHGFEDCBA`
- Provides contextual repetition while covering the full alphabet in both directions.

### 2. BCT (Balanced Coprime Traversal)
The traversal index is calculated with:

```
position = (c + d * a * sign * offset) % n
```

Where:
- `c`: center position
- `d`: direction (`±1`)
- `a`: coprime step value
- `n`: size of the active set
- `offset = ceil(i / 2)`
- `sign = (-1)^(i + 1)`

This generates 26 or more passes through a character set while avoiding habituation by hopping across indices using coprime values.

### 3. ARM1V (Active Recall Morse 1 Vocal)
- Progressive repetition sequences: `[morse×1, speak]`, `[morse×2, speak]`, `[morse×3, speak]`, `[morse×4, speak]`
- Delayed recall: `[morse … 500 ms pause … speak]`
- Text-to-speech (TTS) announces letters/numbers after Morse playback, reinforcing recognition and recall.

### 4. ProgressiveBuild
- Builds words step by step (e.g., `A → AN → ANT → ANTE → ANTEN → ANTENN → ANTENNA`).
- Utilizes ham radio vocabulary, Q-codes, and common digraphs (`TH`, `AN`, `ER`, `ON`, etc.).

### 5. TongueTwister
- Presents rapid sequences of easily confused elements to sharpen discrimination.
- Confusion sets include letters (e.g., `[E, I, S, H]`), numbers/letters (e.g., `[5, S, H]`), and mixed Morse terms (`[CQ, QRZ, QTH]`, `[73, 88, SK]`).
- Vocal support transitions from full → delayed → sparse → none.

## User Interface
### Main Listening Screen
- Header: “≡ Listen” with hamburger menu for settings.
- Status text: `[Phase 1.2, BCT Pattern]` and a session timer (e.g., `2:47:33 / 7:00:00`).
- Central visualizer showing the current Morse pattern (animated dits/dahs) and the character being transmitted.
- Progress bar for the active pattern with pass indicators (e.g., `Phase 1.2 – Pass 14/26`).
- Playback controls: skip backward 30 s, play/pause, skip forward 30 s, skip to next pattern/phase.
- Additional info: current WPM, auto-play indicator, next phase preview.

### Additional Screens
1. **Initial Setup**: Collects call signs, playback preferences (WPM, tone, volume, TTS voice), and phase selection. Defaults to all phases enabled for a 7-hour session.
2. **Main Listening**: The core experience with autoplay, background playback, lock-screen controls, and persistence.
3. **Settings**: Minimal adjustments for WPM, tone, volume, TTS, call signs, progress reset, theme selection, and screen-on toggle.
4. **Scenario Library**: Lists QSO scenarios for Phase 4 (normal QSOs, SKYWARN events, apocalypse situations) and supports custom additions.

## Training Session Flow
1. **Phase 1: Alphabet Mastery (~90 min)**
   - 1.1 NestedID alphabet repetitions
   - 1.2 BCT traversal with ARM1V (26 passes, 4 repetitions per letter)
   - 1.3 Common digraphs with ProgressiveBuild
   - 1.4 Letter confusion TongueTwisters with varying vocal support

2. **Phase 2: Expanded Set (~120 min)**
   - 2.1 Numbers 0–9 with NestedID + ARM1V
   - 2.2 Full BCT across letters, numbers, and radio phrases
   - 2.3 Number/letter confusion TongueTwisters
   - 2.4 Number sequences (signal reports, frequencies)

3. **Phase 3: Words & Abbreviations (~90 min)**
   - 3.1 Ham vocabulary introduction (50+ terms)
   - 3.2 Q-code TongueTwisters
   - 3.3 Reduced vocal support (2:1 Morse-to-vocal ratio)
   - 3.4 Mixed confusion sets

4. **Phase 4: Real-World QSOs (~120 min)**
   - 4.0 Vocabulary review (80 % Morse-only, 20 % vocal)
   - 4.1–4.4 Normal rag-chew QSOs
   - 4.5–4.8 SKYWARN emergency scenarios
   - 4.9–4.12 Apocalypse scenarios

Each phase transition includes a 2-second chime and a TTS announcement (e.g., “Phase 2 complete. Beginning Phase 3: Words and Abbreviations”). Sessions can loop back to Phase 1 after completion.

## Audio & Timing Requirements
- Pure sine wave tone, default 800 Hz (configurable 400–1200 Hz).
- Paris standard timing:
  - `unit = 1200 ms / WPM`
  - dit = 1 unit, dah = 3 units
  - intra-character gap = 1 unit, inter-character gap = 3 units, inter-word gap = 7 units
- Android TTS provides delayed vocalizations for ARM1V.
- Audio assets (e.g., individual letters, numbers, magnitudes) can reside under an `audio/` directory.

## Technical Implementation Notes
- Foreground service ensures uninterrupted background playback with MediaSession integration for lock-screen controls.
- Partial wake lock prevents sleep during active playback.
- Audio focus handling pauses playback during interruptions (e.g., phone calls) and resumes automatically.
- Session state persists via `SharedPreferences`, storing call signs, WPM, phase selections, and current playback position (e.g., `{ phase: 2, subphase: 3, pattern_pass: 14, element_index: 127 }`).
- State is saved every 60 seconds and restored on launch with a resume prompt.
- Optional dark/light theme support and a “keep screen on” setting.

## Pattern Generation Example (Kotlin)
```kotlin
fun generateCompleteTrainingSession(): List<AudioSegment> {
    val session = mutableListOf<AudioSegment>()

    if (phase1Selected) session.addAll(generatePhase1())
    if (phase2Selected) session.addAll(generatePhase2())
    if (phase3Selected) session.addAll(generatePhase3())
    if (phase4Selected) session.addAll(generatePhase4())

    return session
}

fun bctTraversal(center: Int, size: Int, direction: Int, coprime: Int): List<Int> {
    val sequence = mutableListOf<Int>()
    for (i in 0 until size) {
        val offset = ceil(i / 2.0).toInt()
        val sign = if ((i + 1) % 2 == 0) -1 else 1
        val position = (center + direction * coprime * sign * offset).mod(size)
        sequence.add(position)
    }
    return sequence
}
```

## Scenario Template System
- Variables support runtime substitution during Phase 4 playback:
  - `{MY_CALL}`: user call sign
  - `{FRIEND_CALL}`: rotates through friend call signs
  - `{MY_QTH}`: random city/state
  - `{SIGNAL}`: random signal report (`599`, `579`, etc.)
  - `{WX}`: random weather condition
  - `{FREQ}`: random operating frequency
  - `{RIG}`: random radio model
  - `{PWR}`: random transmit power

### Example Templates
**Normal Rag-Chew**
```
CQ CQ CQ DE {MY_CALL} {MY_CALL} K
{MY_CALL} DE {FRIEND_CALL} UR {SIGNAL} QTH {MY_QTH} K
{FRIEND_CALL} DE {MY_CALL} R R UR {SIGNAL} QTH TEXAS WX {WX} RIG {RIG} K
{MY_CALL} DE {FRIEND_CALL} R R FB OM PWR {PWR} 73 SK
```

**SKYWARN**
```
NET CONTROL DE {MY_CALL} TORNADO SPOTTED 5 MILES WEST OF {MY_QTH} MOVING NORTHEAST K
{MY_CALL} DE NET CONTROL ROGER RELAY TO NWS CONTINUE REPORTS K
```

**Apocalypse**
```
ANY STATION DE {MY_CALL} EMERGENCY TRAFFIC GRID DOWN NEED SUPPLY INFO K
{MY_CALL} DE {FRIEND_CALL} COPY EMERGENCY SAFE ZONE AT {MY_QTH} FOOD WATER AVAILABLE K
```

## Exclusions (Intentional)
The initial release deliberately omits features such as feedback, scoring, gamification, social sharing, progress analytics, or active Morse sending. The experience is entirely passive listening.

## Example User Journey
1. Download the app and complete initial setup.
2. Listen to Phase 1 in bed for ~2 hours; session auto-saves when stopping.
3. Resume during a commute to progress through Phase 2.
4. Finish remaining phases over subsequent sessions, ultimately achieving 25 WPM comprehension through sustained immersion.

## Roadmap Considerations
- Automated audio generation pipeline for all patterns.
- Downloadable scenario packs or community submissions.
- Advanced analytics (time spent per phase, repetition counts).
- Optional interactive mode for active recall once immersion mastery is achieved.


## Current Implementation
The repository now contains a Jetpack Compose Android application that models the complete brute-force training session described above. Highlights:

- **Autogenerated training queue**: `SessionRepository` deterministically builds every phase, sub-phase, and pass using the NestedID, BCT, ProgressiveBuild, and TongueTwister patterns.
- **Scenario templating**: Phase 4 scripts substitute `{MY_CALL}`, `{FRIEND_CALL}`, `{MY_QTH}`, and related tokens with rotating context drawn from user preferences and curated data pools.
- **Composable UI surfaces**: Setup, Listening, Settings, and Scenario Library screens match the passive-immersion workflow with Material Design 3 styling.
- **State management**: `PlaybackViewModel` coordinates DataStore-backed settings, generates long-form sessions, and simulates long-running playback progress that can auto-advance across passes and phases.
- **Foreground playback scaffolding**: `PlaybackService` establishes a MediaSession-ready foreground service for future real audio rendering while keeping notification controls alive during background playback.

## Developer Quickstart
1. Open the project in Android Studio Hedgehog or newer.
2. Let Gradle sync; if the wrapper JAR is missing, run `gradle wrapper` locally to regenerate it for your platform.
3. Select the `app` configuration and deploy to an emulator or device running Android 8.0 (API 26) or later.
4. On first launch, enter your call sign, optional friends, confirm the default phase selection, and tap **START TRAINING** to generate a 7-hour passive session.
5. Use the Listen screen’s controls to simulate playback, skip passes, and inspect future phases or scenario scripts.

## Next Steps
- Replace the simulated playback ticker with synthesized Morse audio using the configured WPM and tone frequency.
- Drive the `PlaybackService` with actual ExoPlayer media items per `SessionStep`, piping tone generation and TTS into the audio timeline.
- Persist fine-grained resume points (phase, pass, element) so sessions can resume exactly where users paused.
- Expand the scenario library and allow custom user-defined templates stored in DataStore or a lightweight database.
