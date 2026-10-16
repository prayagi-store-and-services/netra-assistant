# Netra Assistant

Offline, on-device personal assistant for Android by Prayagi Store and Services. Two assistants in one app: NETRA (warm) and TRIKAL (brief). Part of the [Netra Eco](https://prayagi-store-and-services.github.io/netra-eco/) family.

This is a separate app from [Netra Trikaal](https://github.com/prayagi-store-and-services/netra-trikaal), the astrology app. This assistant has no astrology features and holds no birth details.

## Status: rolling beta

Pre-releases are published on the [releases page](https://github.com/prayagi-store-and-services/netra-assistant/releases) only when CI is green. Today it is voice-first: tap the mic and speak (offline recognition where the phone supports it, otherwise Unavailable unless you opt in to online voice), and it answers aloud about battery (level, temperature, rough time left), volume and brightness, with persona-specific replies. Typing is a fallback. Hands-free mode (one-tap switch, screen off, notification with Stop) is in 0.3.1. Saved memory, Family Bridge and on-device AI are planned. Dates are in the [milestones](https://github.com/prayagi-store-and-services/netra-assistant/milestones). Public release target: 1 April 2027.

## Principles

- Privacy first: everything runs on the phone. No account, no server, no internet permission today (the only permission is the microphone). Any future cloud step will show exactly what is sent and where, and needs your opt-in.
- Truthful answers: anything the phone cannot report shows "Unavailable".
- Free to use.

## Build

`gradle :app:assembleDebug :app:testDebugUnitTest` (JDK 17, Gradle 8.7). Application ID `com.prayagi.netraassistant`.

## Licence

AGPL-3.0, see [LICENSE](LICENSE).
