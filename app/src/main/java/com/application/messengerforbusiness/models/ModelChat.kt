package com.application.messengerforbusiness.models

    data class ModelChat(
        var message: String = "", var receiver: String = "", var sender: String = "",
        var timeStamp: String = "", var seen: Boolean = false, var type: String = ""
    )