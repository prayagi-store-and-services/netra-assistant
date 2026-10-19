# Netra One

Netra One is the offline, on-device voice command assistant of the [Netra Ecosystem](https://prayagi-store-and-services.github.io/netra-eco/), by Prayagi Store and Services. Its purpose: one assistant that answers from real phone readings and sends commands - first to the phone itself, then to every Netra family app.

This is a separate app from [Netra Trikaal](https://github.com/prayagi-store-and-services/netra-trikaal), the astrology app. Netra One has no astrology features and holds no birth details.

## Status: rolling beta

Pre-releases are published on the [releases page](https://github.com/prayagi-store-and-services/netra-assistant/releases) only when CI is green. Today it is voice-first: tap the mic and speak (offline recognition where the phone supports it, otherwise Unavailable unless you opt in to online voice), and it answers aloud about battery (level, temperature, rough time left), volume and brightness. Typing is a fallback. Hands-free listening is ON by default once mic permission is granted (notification with Stop, opt-out remembered, no restart after a phone reboot): it answers only after the wake name "Netra One" or "Netra". Phone commands (call, alarm, open app, search, SMS) ask first - say haan or nahi. The Home screen is one status ring: grey off, blue listening, bright heard, amber acting, green done, red Unavailable. Next betas add a command bridge to the other Netra apps. Dates are in the [milestones](https://github.com/prayagi-store-and-services/netra-assistant/milestones). Public release target: 1 April 2027.

## Principles

- Privacy first: everything runs on the phone. No account, no server, no internet permission today (the only permission is the microphone). Any future cloud step will show exactly what is sent and where, and needs your opt-in.
- Truthful answers: anything the phone cannot report shows "Unavailable".
- Confirm before acting: any command that changes something is first read back and runs only after your "haan".
- Free to use.

## Build

`gradle :app:assembleDebug :app:testDebugUnitTest` (JDK 17, Gradle 8.7). Application ID `com.prayagi.netraassistant` (kept unchanged so existing installs keep working).

## Licence

AGPL-3.0, see [LICENSE](LICENSE).
