package org.example.splitpalbackendv2.Model

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer

@Document(collection = "groups")
data class groupModel (
    @JsonSerialize(using = ToStringSerializer::class)
    @Id val id: ObjectId?=null,
    var name: String,
    var description: String? = null,
    @JsonSerialize(using = ToStringSerializer::class)
    var Admin : ObjectId? = null,
    var created_at: LocalDateTime?=LocalDateTime.now(),
    @JsonSerialize(contentUsing = ToStringSerializer::class)
    var member: MutableList<String>,
)