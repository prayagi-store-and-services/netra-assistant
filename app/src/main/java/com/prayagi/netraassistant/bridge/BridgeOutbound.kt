package com.prayagi.netraassistant.bridge

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.prayagi.netraassistant.actions.Result

/**
 * Sends one confirmed command to one Netra family app via the BridgeContract broadcast.
 * Honest status: it reports how many receivers are registered for the action in the
 * target package before sending, and never pretends the command ran - the family app
 * does the work and answers inside itself (Stage 3 adds spoken replies back).
 */
class BridgeOutbound(private val context: Context) {

    fun send(targetPackage: String, targetName: String, command: String, args: String): Result<String> {
        if (!BridgeContract.isValidCommand(command)) {
            return Result.Unavailable("bridge command ka naam theek nahi hai: $command")
        }
        if (!isInstalled(targetPackage)) {
            return Result.Unavailable("$targetName is phone par installed nahi hai")
        }
        val i = Intent(BridgeContract.ACTION)
            .setPackage(targetPackage)
            .putExtra(BridgeContract.EXTRA_COMMAND, command)
            .putExtra(BridgeContract.EXTRA_ARGS, args)
            .putExtra(BridgeContract.EXTRA_REQUEST_ID, BridgeContract.newRequestId())
            .putExtra(BridgeContract.EXTRA_SOURCE, BridgeContract.SOURCE_PACKAGE)
            .putExtra(BridgeContract.EXTRA_VERSION, BridgeContract.CONTRACT_VERSION)
        val receivers = context.packageManager.queryBroadcastReceivers(i, 0)
        if (receivers.isEmpty()) {
            return Result.Unavailable("$targetName mein bridge receiver abhi nahi hai (us app ke update ka intezaar)")
        }
        return try {
            context.sendBroadcast(i)
            Result.Value("Bridge command '$command' $targetName ko bhej diya (${receivers.size} receiver mila). Kaam wo app apne andar karega.")
        } catch (e: SecurityException) {
            Result.Unavailable("$targetName ko bhejne ki anumati nahi mili")
        } catch (e: Exception) {
            Result.Unavailable("$targetName ko bhejte waqt dikkat aayi")
        }
    }

    private fun isInstalled(pkg: String): Boolean = try {
        context.packageManager.getPackageInfo(pkg, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }
}
