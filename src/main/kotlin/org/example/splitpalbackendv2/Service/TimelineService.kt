package org.example.splitpalbackendv2.Service

import org.bson.types.ObjectId
import org.example.splitpalbackendv2.Model.TimelineChat
import org.example.splitpalbackendv2.Model.transactionModel
import org.example.splitpalbackendv2.Repository.groupRepo
import org.example.splitpalbackendv2.Repository.transactionRepo
import org.example.splitpalbackendv2.Repository.userRepo
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service

// Paginated response wrapper
data class PaginatedTimelineResponse(
    val content: List<TimelineChat>,
    val currentPage: Int,
    val totalPages: Int,
    val totalItems: Long,
    val hasMore: Boolean
)

@Service
class TimelineService(
    private val transactionRepo: transactionRepo,
    private val groupRepo: groupRepo,
    private val userRepo: userRepo
) {

    // Original method (non-paginated) - kept for backward compatibility
    fun getTimeline(currentUserId: ObjectId, targetUserId: ObjectId): List<TimelineChat> {
        val p2pTransactions = transactionRepo.findP2PTransactions(currentUserId, targetUserId)
        val sharedTransactions = transactionRepo.findSharedTransactions(currentUserId, targetUserId)

        val allTransactions = (p2pTransactions + sharedTransactions)
            .distinctBy { it.id }
            .sortedByDescending { it.transaction_created_at }

        return allTransactions.map { transaction ->
            mapToTimelineChat(transaction, currentUserId)
        }
    }

    // NEW: Paginated timeline method
    fun getTimelinePaginated(
        currentUserId: ObjectId, 
        targetUserId: ObjectId, 
        page: Int, 
        size: Int
    ): PaginatedTimelineResponse {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "transaction_created_at"))
        val transactionPage = transactionRepo.findTimelineTransactions(currentUserId, targetUserId, pageable)
        
        val timelineChats = transactionPage.content.map { transaction ->
            mapToTimelineChat(transaction, currentUserId)
        }
        
        return PaginatedTimelineResponse(
            content = timelineChats,
            currentPage = transactionPage.number,
            totalPages = transactionPage.totalPages,
            totalItems = transactionPage.totalElements,
            hasMore = transactionPage.hasNext()
        )
    }

    // NEW: Paginated group timeline method
    fun getGroupTimelinePaginated(
        groupId: ObjectId,
        currentUserId: ObjectId,
        page: Int,
        size: Int
    ): PaginatedTimelineResponse {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "transaction_created_at"))
        val transactionPage = transactionRepo.findByGroupIdPaginated(groupId, pageable)
        
        val timelineChats = transactionPage.content.map { transaction ->
            mapToTimelineChat(transaction, currentUserId)
        }
        
        return PaginatedTimelineResponse(
            content = timelineChats,
            currentPage = transactionPage.number,
            totalPages = transactionPage.totalPages,
            totalItems = transactionPage.totalElements,
            hasMore = transactionPage.hasNext()
        )
    }

    private fun mapToTimelineChat(transaction: transactionModel, currentUserId: ObjectId): TimelineChat {
        val groupName = transaction.group_id?.let {
            groupRepo.findById(it).map { g -> g.name }.orElse(null)
        }

        // Fix: Check status specifically for the current user
        // If current user is the payer, they are always "settled" (true)
        // If current user is a borrower, check their specific split status
        val status = if (transaction.userID == currentUserId) {
            true
        } else {
            transaction.involvedUser?.find { it.owedBy == currentUserId }?.status ?: false
        }
        val payer = transaction.userID
        
        // Find the current user's split amount from involvedUser array
        // If user is the payer, show total amount; if user owes, show their split amount
        val owedAmount = if (payer == currentUserId) {
            // Current user is the payer - they are owed the total from others
            transaction.amount
        } else {
            // Current user owes - find their specific split amount
            transaction.involvedUser?.find { it.owedBy == currentUserId }?.SplitAmout ?: 0
        }
        
        // Fetch payer's phone number and name for frontend differentiation
        val payerUser = userRepo.findById(payer).orElse(null)
        val payerPhoneNumber = payerUser?.phoneNumber
        val payerName = payerUser?.userName

        return TimelineChat(
            id = transaction.id?.toHexString() ?: "",
            total_amount = transaction.amount,
            owend_amount = owedAmount,
            status = status,
            group_name = groupName,
            group_id = transaction.group_id?.toHexString(),
            payed_by = payer.toHexString(),
            payer_phoneNumber = payerPhoneNumber,
            payer_name = payerName
        )
    }
}


