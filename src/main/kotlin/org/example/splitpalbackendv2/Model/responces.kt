package org.example.splitpalbackendv2.Model

import org.bson.types.ObjectId
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer

// Main dashboard response
data class DashboardScreen(
    val FlatcardList: List<FlatCard>,
    val groupList: List<GroupInfo>,
    val individuals: List<IndividualTransactionPeople>,
    val recent_transaction: List<RecentTransaction>
)

// Recent 5 transactions where user owes (from involvedUser splits)
data class FlatCard(
    @JsonSerialize(using = ToStringSerializer::class)
    val transaction_id: ObjectId?,
    val group_name: String?,
    val Initiator_name: String,
    val Amount: Int
)

// Group info (simplified)
data class GroupInfo(
    @JsonSerialize(using = ToStringSerializer::class)
    val id: ObjectId?,
    val name: String?
)

// Top 4 users paid to
data class IndividualTransactionPeople(
    @JsonSerialize(using = ToStringSerializer::class)
    val target_user: ObjectId?,
    val target_user_name: String?
)

// Recent 5 transactions by user
data class RecentTransaction(
    val payed_to_name: String?,
    val group_name: String?,
    val amount: Int,
    val status: Status
)

enum class Status {
    DEBITED,
    CREDITED
}
