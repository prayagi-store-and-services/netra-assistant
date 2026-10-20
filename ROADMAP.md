# Netra One - Roadmap / Future Plans

v0.5.0-beta.1 | Netra Ecosystem

## Vision (binding, Deepak 18 Oct 2026)

Netra One is the universal voice command center of the Netra Ecosystem. Every Netra app sends commands and announcement tasks to Netra One when it is installed; Netra One collects results, executes, and issues commands back. Long-term, step by step, all apps merge into Netra One so one app does every task and separate apps become unnecessary. Not immediate - no app merging has started. Every design choice keeps the Family Bridge in mind.

## Design principles (binding, Deepak 18 Oct 2026)

1. Hands-free first. No manual button for the core flow: wake, listen, execute all run by voice. Once mic permission is granted, hands-free listening is on by default, with a clear visible state at all times.
2. Assistant-grade behavior, learned from Google Assistant / Siri / Alexa: always-listening wake word, quick spoken confirmation, conversational flow. Designs are reasoned from how good assistants behave, not only from literal specs.
3. Pretty, execution-focused UI. No fake or decorative buttons - every control does real work. States are honest (Unavailable means unavailable, with the reason).
4. Confirm-before-sensitive-action stays: calls, SMS, alarms and app commands always ask first, by voice.

## Stages

### Stage 0 - done (v0.4.0 / v0.5.0)
Rename to Netra One (single voice, wake names "Netra One" / "Netra", incl. Devanagari), command layer: Call, SetAlarm, OpenApp (incl. Netra family), WebSearch, SMS - all confirm-before-act, with an in-app Action Log.

### Stage 1 - next PR: hands-free-first redesign
- Mic permission granted => hands-free listening starts by default; persistent notification with Stop; a big visible listening state in the app.
- Wake word "Netra One" (or "Netra") triggers listening; answer and confirmations are spoken; "haan" confirms, "nahi" cancels.
- UI: one status view (listening / heard / acting / done), live transcript, last action with undo where possible, Action Log tab. No button that only pretends to work.
- Feasibility: continuous SpeechRecognizer with a name filter is free and offline-capable but costs battery (honest estimate: noticeable drain, this is why it is opt-out and stops on reboot); a true low-power wake-word engine (e.g. Porcupine free tier) is a later option and adds native binary size.

### Stage 2 - Family Bridge outbound
Netra One sends commands to other Netra apps (broadcast intent contract, then content provider; accessibility as last-resort fallback). Each family app needs a small receiver - per-app work, owned per app.

### Stage 3 - Family Bridge inbound
Apps send announcement/result tasks to Netra One (battery alerts, sensor events) and Netra One speaks them and acts on replies.

### Stage 4 - gradual merge
Features move into Netra One app by app, only at parity. Honest constraints: APK size growth, Play policy per permission, migration risk per app, and keeping each original app working until its users have moved.

## Feasibility notes
- Permissions: RECORD_AUDIO (foreground service), POST_NOTIFICATIONS, SET_ALARM, package visibility via <queries> - no INTERNET, no QUERY_ALL_PACKAGES.
- Battery: always-listening via SpeechRecognizer is the main cost; Stage 1 ships it with clear status and an easy off switch.
- Size: command layer adds <100 KB; a wake-word engine would add MBs and is deferred.
- Risk: per-app bridge receivers are the long pole; each lands behind green CI like everything else.
