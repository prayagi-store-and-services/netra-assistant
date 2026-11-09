# ATIF v3.0 - applicable parts for Netra One (assistant)

ATIF v3.0 (Absolute Truth & Intelligence Framework) is the owner's permanent master rule, issued 9 Nov 2026. Canonical full text: private governance copy (`netra-intruder`, `docs/ATIF-v3.0.md`). This file lists only the parts that apply to THIS app.

## Applicable parts and why

- Part 01, 02: spoken answers must be true before pleasant.
- Part 04, 06: answers come only from real phone readings (battery level/temperature/rough time left, volume, brightness); anything the phone cannot report is answered as Unavailable.
- Part 09: time-sensitive answers (battery, time left) are read live at ask time, not cached silently.
- Part 14: offline recognition depends on the phone; online voice is a separate labelled opt-in. "Rough time left" is an estimate and is worded as one.
- Part 16: everything on-device; no account, no server; the only permission is the microphone. Any future cloud step requires opt-in with exactly what is sent and where.
- Part 17, 20: confirm-before-act (haan/nahi) for anything that changes something.

## Data this app shows/speaks, and its status

- Battery, volume, brightness answers: live device readings - VERIFIED source.
- Family Bridge: sends commands only after explicit confirmation and says honestly when a family app has no bridge receiver yet.
- Status ring colours: grey/blue/bright/amber/green/red map to real states, red = Unavailable.

## Standing rule

No answer may be synthesized beyond device readings. Beta status and the 1 April 2027 public target stay labelled as targets, not achievements.

## Source labels (owner-approved 9 Nov 2026)

Every user-visible data claim carries a short source label. UI strings stay short; the label for each claim lives in this file's "Data this app shows" list and in WHATS_NEW/release notes (e.g. "Source: Android BatteryManager"). In-app visible source labels are planned and ship only after the owner's UI preview.

## Stale data: OLD marking (owner-approved 9 Nov 2026)

Any dated or versioned claim that stops matching reality is marked "OLD (as of <date>)" at the next check and corrected in the same or a following PR. Stale claims are never silently edited and never left unmarked.

## Weekly audit (owner-approved 9 Nov 2026)

A weekly audit records each material claim as VERIFIED or NOT VERIFIED in `docs/ATIF-AUDIT.md`, newest first. Failures and NOT VERIFIED items are listed first in every audit and every report.
