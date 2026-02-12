package org.example.splitpalbackendv2.Model

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
//import tools.jackson.databind.annotation.JsonSerialize

@Document(collection = "users")
data class userModel (
    @JsonSerialize(using = ToStringSerializer::class)
    val id: ObjectId?=null,
    var userName: String,
    var password: String,
    var phoneNumber: String,
    var secureToken: String?=null,
    var refreshToken: String?=null,
    var groups: MutableList<groupModel>?=null,
    var groupCredits: Int = 3,  // Free credits to create groups (default: 3)
    var updated_at:LocalDateTime=LocalDateTime.now(),
    var created_at:LocalDateTime=LocalDateTime.now(),
)
