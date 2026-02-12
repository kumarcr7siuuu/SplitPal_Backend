package org.example.splitpalbackendv2.Model

import org.bson.types.ObjectId

data class tokenModel(
    val id: ObjectId? = null,
    var secureToken: String,
    var refreshToken: String
)
