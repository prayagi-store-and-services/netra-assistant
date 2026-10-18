package com.prayagi.netraassistant.commands

import android.provider.AlarmClock
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.prayagi.netraassistant.actions.Result

/** Runs a confirmed PhoneCommand by firing an Android intent. Nothing leaves this app. */
class CommandExecutor(private val context: Context) {

    private fun start(i: Intent): Boolean = try {
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(i)
        true
    } catch (e: ActivityNotFoundException) {
        false
    } catch (e: Exception) {
        false
    }

    fun run(cmd: PhoneCommand): Result<String> = when (cmd) {
        is PhoneCommand.Call -> {
            val ok = start(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${cmd.number}")))
            if (ok) Result.Value("Dialer khul gaya: ${cmd.number}. Call dabana aapka hai.")
            else Result.Unavailable("is phone mein dialer app nahi mila")
        }
        is PhoneCommand.SetAlarm -> {
            val i = Intent(AlarmClock.ACTION_SET_ALARM)
                .putExtra(AlarmClock.EXTRA_HOUR, cmd.hour)
                .putExtra(AlarmClock.EXTRA_MINUTES, cmd.minutes)
                .putExtra(AlarmClock.EXTRA_MESSAGE, cmd.label.ifBlank { "Netra One" })
                .putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            if (start(i)) Result.Value("Clock app mein alarm set karne ke liye khul gaya.")
            else Result.Unavailable("is phone mein clock app alarm intent nahi leti")
        }
        is PhoneCommand.OpenApp -> {
            val i = context.packageManager.getLaunchIntentForPackage(cmd.packageId)
            if (i == null) {
                Result.Unavailable("${cmd.appName} is phone par installed nahi hai")
            } else if (start(i)) {
                Result.Value("${cmd.appName} khul gaya.")
            } else {
                Result.Unavailable("${cmd.appName} nahi khul paya")
            }
        }
        is PhoneCommand.WebSearch -> {
            val url = "https://www.google.com/search?q=" + Uri.encode(cmd.query)
            if (start(Intent(Intent.ACTION_VIEW, Uri.parse(url)))) {
                Result.Value("Browser mein search khul gaya: ${cmd.query}")
            } else {
                Result.Unavailable("is phone mein browser nahi mila")
            }
        }
        is PhoneCommand.Sms -> {
            val i = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${cmd.number}"))
                .putExtra("sms_body", cmd.body)
            if (start(i)) Result.Value("SMS khul gaya: ${cmd.number} ko. Send dabana aapka hai.")
            else Result.Unavailable("is phone mein SMS app nahi mila")
        }
    }
}
