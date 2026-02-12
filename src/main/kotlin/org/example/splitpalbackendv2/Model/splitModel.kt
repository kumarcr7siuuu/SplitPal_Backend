package org.example.splitpalbackendv2.Model

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import java.time.LocalDateTime
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer

data class splitModel (
    @JsonSerialize(using = ToStringSerializer::class)
    @Id val id: ObjectId? = null,
//    @JsonSerialize(using = ToStringSerializer::class)
//    val transactionId:ObjectId,
//    @JsonSerialize(using = ToStringSerializer::class)
//    val group_id:ObjectId?=null,
    var SplitAmout: Int,
//    var description: String?=null,
    @JsonSerialize(using = ToStringSerializer::class)
    var payed_by:ObjectId,
    @JsonSerialize(using = ToStringSerializer::class)
    var owedBy: ObjectId,
    var status:Boolean=false,
//    update it after setulment
    val settled_date: LocalDateTime? = null
    )