package com.application.messengerforbusiness.models

data class ModelTask(
    var timeStamp: String = "",
    var creator: String = "",
    var receiver: String = "",
    var taskName: String = "",
    var description: String = "",
    var deadline: String = ""
)