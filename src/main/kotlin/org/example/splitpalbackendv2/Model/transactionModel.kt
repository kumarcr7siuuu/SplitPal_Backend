package org.example.splitpalbackendv2.Model

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import java.time.LocalDateTime
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer

data class transactionModel (
    @JsonSerialize(using = ToStringSerializer::class)
    @Id val id: ObjectId?=null,
//    @JsonSerialize(using = ToStringSerializer::class)
//    var splitId: ObjectId?=null,
    @JsonSerialize(using = ToStringSerializer::class)
    val group_id: ObjectId?=null,
    @JsonSerialize(using = ToStringSerializer::class)
    val userID: ObjectId,
    val amount: Int,
    val transaction_created_at: LocalDateTime?= LocalDateTime.now(),
    var description: String?=null,
    var to: payedTo,
    var involvedUser: MutableList<splitModel>? = null
    )

data class payedTo(
//    main for interface of ui
    val name: String,
//    this id will of of those uer which have our app , mean both side have our app ,
//    otherwse theywill only able to see the name and upi_id
    val id: ObjectId?=null,
    val phoneNumber:String
)

data class CreateTransactionRequest(
    val amount: Int,
    val description: String? = null,
//    to is the phone number of the reciver
    val to: payedTo,
    val group_id: String? = null,     // Changed to String
    val involvedUser: MutableList<splitModel>? = null
)

data class EditTransactionRequest(
    val transaction_id: String,       // Changed to String
    val amount: Int? = null,
    val description: String? = null,
    val to: payedTo? = null,
    val group_id: String? = null,     // Changed to String
    val involvedUser: MutableList<splitModel>? = null
)

// Request to edit an individual split within a transaction
data class EditSplitRequest(
    val transaction_id: String,       // Changed to String
    val split_id: String,             // Changed to String
    val amount: Int? = null,
    val status: Boolean? = null
)
