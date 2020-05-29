package com.application.messengerforbusiness.models

data class ModelUser(
    var name: String = "",
    var email: String = "",
    var phone: String = "",
    var position: String = "",
    var image: String = "",
    var cover: String = "",
    var search: String = "",
    var uid: String = "",
    var surname: String = "",
    var onlineStatus: String = "",
    var typingTo: String = "",
    var deleted: Boolean = false
)