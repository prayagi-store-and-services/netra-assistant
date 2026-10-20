# Netra Family Bridge - receiver spec (contract version 1)

v0.7.0-beta.1 | Netra Ecosystem

Netra One (com.prayagi.netraassistant) sends user-confirmed commands to other Netra
family apps as explicit Android broadcasts. Everything stays on the phone: no internet,
no server, no account. This document is the contract a family app implements to receive
those commands.

## What Netra One sends

- Action: `com.prayagi.netra.BRIDGE_COMMAND`
- Targeting: explicit (`setPackage(<your package>)`) - only your app can receive it.
- Extras:
  - `com.prayagi.netra.extra.COMMAND` (String): the command name. Lowercase letters,
    digits and dashes only, 1-64 chars, e.g. `battery-status`.
  - `com.prayagi.netra.extra.ARGS` (String): free-form argument text the user spoke,
    may be empty.
  - `com.prayagi.netra.extra.REQUEST_ID` (String): unique id per command, prefix `nb-`.
    Use it to deduplicate and to correlate a future reply.
  - `com.prayagi.netra.extra.SOURCE` (String): the sender package,
    `com.prayagi.netraassistant`.
  - `com.prayagi.netra.extra.CONTRACT_VERSION` (int): currently `1`.

## What a family app must do

1. Register a manifest BroadcastReceiver for the action. Because the broadcast comes
   from another app, the receiver must be exported:

   ```xml
   <receiver
       android:name=".bridge.NetraBridgeReceiver"
       android:exported="true">
       <intent-filter>
           <action android:name="com.prayagi.netra.BRIDGE_COMMAND" />
       </intent-filter>
   </receiver>
   ```

2. In `onReceive`, verify before acting:
   - `SOURCE` equals `com.prayagi.netraassistant`. Any app on the phone can send an
     explicit broadcast to an exported receiver, so check the source on every intent.
   - `CONTRACT_VERSION` equals `1`; ignore other versions.
   - `COMMAND` matches `^[a-z0-9][a-z0-9-]{0,63}$`; ignore anything else.
3. Deduplicate on `REQUEST_ID` (broadcasts can be redelivered).
4. Handle only the commands your app knows; ignore unknown ones quietly.
5. Do any slow work off the main thread (`goAsync()`).

## Honest behavior

- Netra One checks before sending whether a receiver for the action is registered in
  the target package. If none is found it tells the user "bridge receiver abhi nahi hai"
  instead of pretending. Until your app ships its receiver, that is the expected state.
- The broadcast is fire-and-forget: delivery is not a promise that the command ran.
  Spoken results and replies to Netra One are Stage 3 (inbound) work.

## Hardening planned for a later contract version

- A signature-level custom permission so only Netra-signed apps can send.
- Content-provider channel for larger payloads.
