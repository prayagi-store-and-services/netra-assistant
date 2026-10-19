package com.prayagi.netraassistant.commands

/** Spoken yes/no for confirming a sensitive command. English + Hindi/Hinglish + Devanagari. */
object ConfirmWords {
    private val confirm = setOf(
        "haan", "ha", "han", "yes", "yeah", "haan haan", "zaroor", "bilkul",
        "हाँ", "हां", "ज़रूर", "बिल्कुल", "ठीक है", "theek hai", "theek"
    )
    private val cancel = setOf(
        "nahi", "nahin", "no", "nope", "cancel", "ruk jao", "ruko", "band karo", "rehne do",
        "नहीं", "नही", "रुक जाओ", "रुको", "बंद करो", "रहने दो"
    )

    fun isConfirm(t: String): Boolean = t.trim().lowercase() in confirm
    fun isCancel(t: String): Boolean = t.trim().lowercase() in cancel
}
