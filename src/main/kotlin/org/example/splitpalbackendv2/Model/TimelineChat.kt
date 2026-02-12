package org.example.splitpalbackendv2.Model

import org.bson.types.ObjectId
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer

data class TimelineChat(
    val id: String,
    val total_amount: Int,
    val owend_amount: Int,
    val status: Boolean,
    val group_name: String? = null,
    val group_id: String? = null,
    val payed_by: String,
    val payer_phoneNumber: String? = null,  // Phone number of payer for frontend differentiation
    val payer_name: String? = null         // Name of the payer for display
)

