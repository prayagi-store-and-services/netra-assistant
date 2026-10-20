package com.prayagi.netraassistant.bridge

import java.util.UUID

/**
 * Netra Family Bridge contract, version 1 (outbound).
 * Netra One sends a confirmed command to one other Netra family app as an explicit
 * broadcast intent. Family apps register a BroadcastReceiver for ACTION in their
 * manifest to receive it. The full spec for family apps is in BRIDGE.md.
 * Everything stays on the phone: this is a local Android broadcast, not a network call.
 */
object BridgeContract {
    const val ACTION = "com.prayagi.netra.BRIDGE_COMMAND"
    const val EXTRA_COMMAND = "com.prayagi.netra.extra.COMMAND"
    const val EXTRA_ARGS = "com.prayagi.netra.extra.ARGS"
    const val EXTRA_REQUEST_ID = "com.prayagi.netra.extra.REQUEST_ID"
    const val EXTRA_SOURCE = "com.prayagi.netra.extra.SOURCE"
    const val EXTRA_VERSION = "com.prayagi.netra.extra.CONTRACT_VERSION"
    const val CONTRACT_VERSION = 1
    const val SOURCE_PACKAGE = "com.prayagi.netraassistant"

    /** Command names are lowercase letters, digits and dashes, e.g. "battery-status". */
    private val commandName = Regex("^[a-z0-9][a-z0-9-]{0,63}$")

    fun isValidCommand(cmd: String): Boolean = commandName.matches(cmd)

    fun newRequestId(): String = "nb-" + UUID.randomUUID().toString()
}
