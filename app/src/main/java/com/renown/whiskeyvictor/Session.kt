package com.renown.whiskeyvictor

import java.io.Serializable

data class Session(
    val userName: String,
    val url: String,
    val cookiesJson: String
): Serializable
