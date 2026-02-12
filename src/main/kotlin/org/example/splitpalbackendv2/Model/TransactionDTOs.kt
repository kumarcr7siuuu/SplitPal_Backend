package org.example.splitpalbackendv2.Model

import java.time.LocalDateTime

// Response DTO for transaction list view (lightweight, no splits)
data class TransactionListItem(
    val id: String,
    val amount: Int,
    val description: String?,
    val payed_by: String,
    val date: LocalDateTime?,
    val splitCount: Int,
    val group_name: String?
)

// Response DTO for transaction detail view (full data with splits)
data class TransactionDetailResponse(
    val id: String,
    val amount: Int,
    val description: String?,
    val payed_by: PayerInfo,
    val date: LocalDateTime?,
    val group_name: String?,
    val splits: List<SplitDetail>
)

data class PayerInfo(
    val id: String,
    val name: String,
    val phoneNumber: String?
)

data class SplitDetail(
    val split_id: String,
    val user_id: String,
    val user_name: String,
    val phoneNumber: String?,
    val amount: Int,
    val status: Boolean,
    val settled_date: LocalDateTime?
)
// Response DTO for create/edit operations (mirrors Entity but with String IDs)
data class TransactionResponseDTO(
    val id: String,
    val group_id: String?,
    val userID: String,
    val amount: Int,
    val transaction_created_at: LocalDateTime?,
    val description: String?,
    val to: PayedToDTO?,
    val involvedUser: List<SplitResponseDTO>?
)

data class PayedToDTO(
    val name: String,
    val id: String?,
    val phoneNumber: String
)

data class SplitResponseDTO(
    val id: String?,
    val SplitAmout: Int,
    val payed_by: String,
    val owedBy: String,
    val status: Boolean,
    val settled_date: LocalDateTime?
)
