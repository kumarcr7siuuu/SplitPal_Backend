package org.example.splitpalbackendv2.Service

import org.bson.types.ObjectId
import org.example.splitpalbackendv2.Model.*
import org.example.splitpalbackendv2.Repository.groupRepo
import org.example.splitpalbackendv2.Repository.transactionRepo
import org.example.splitpalbackendv2.Repository.userRepo
import org.springframework.stereotype.Service

@Service
class DashboardService(
    private val transactionRepo: transactionRepo,
    private val userRepo: userRepo,
    private val groupRepo: groupRepo
) {
    fun getDashboard(currentUserId: ObjectId,userNumber: String): DashboardScreen {
        return DashboardScreen(
            FlatcardList = getFlatCards(currentUserId),
            groupList = getRecentGroups(userNumber),
            individuals = getIndividuals(currentUserId),
            recent_transaction = getRecentTransactions(currentUserId)
        )
    }

    // FlatCard: Recent 5 transactions where user owes (status=false in involvedUser)
    private fun getFlatCards(userId: ObjectId): List<FlatCard> {
        val transactions = transactionRepo.findTransactionsWhereUserOwes(userId)
            .sortedByDescending { it.transaction_created_at }
            .take(5)

        return transactions.mapNotNull { transaction ->
            // Find the split where this user owes
            val userSplit = transaction.involvedUser?.find {
                it.owedBy == userId && !it.status
            } ?: return@mapNotNull null

            // Get initiator name (transaction creator)
            val initiatorName = userRepo.findById(transaction.userID)
                .map { it.userName }
                .orElse("Unknown")

            // Get group name if exists
            val groupName = transaction.group_id?.let { groupId ->
                groupRepo.findById(groupId).map { it.name }.orElse(null)
            }

            FlatCard(
                transaction_id = transaction.id,
                group_name = groupName,
                Initiator_name = initiatorName,
                Amount = userSplit.SplitAmout
            )
        }
    }

    // Groups: Recent 3 groups where user is a member (from groups collection)
    //I need to add phone number to fetch the group for the perticuler user
    private fun getRecentGroups(userNumber: String): List<GroupInfo> {
        return groupRepo.findByMemberContaining(userNumber)
            .sortedByDescending { it.created_at }
            .take(3)
            .map { group ->
                GroupInfo(id = group.id, name = group.name)
            }
    }

    // Individuals: Top 4 users paid to recently/frequently
    private fun getIndividuals(userId: ObjectId): List<IndividualTransactionPeople> {
        println("DEBUG: getIndividuals called for userId: $userId")
        // Find all transactions where user is involved (as payer, recipient, or in split)
        val transactions = transactionRepo.findAllTransactionsInvolvingUser(userId)
        println("DEBUG: Found ${transactions.size} raw transactions involving user")

        // For each transaction, extract the other party:
        // - If user is payer, target is the recipient (to.id)
        // - If user is involved in split, target is the payer (userID)
        val targetUserIds = transactions
            .mapNotNull { transaction ->
                when {
                    transaction.userID == userId -> transaction.to.id // User paid, target is recipient
                    transaction.to.id == userId -> transaction.userID // User received, target is payer
                    else -> transaction.userID // User in split, target is transaction creator
                }
            }
            .filter { it != userId } // Exclude self
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(4)
            .map { it.key }

        println("DEBUG: Found ${targetUserIds.size} unique target users")

        return targetUserIds.mapNotNull { targetId ->
            val targetUser = userRepo.findById(targetId).orElse(null)
            if (targetUser != null) {
                IndividualTransactionPeople(
                    target_user = targetId,
                    target_user_name = targetUser.userName
                )
            } else {
                println("DEBUG: Target user $targetId not found in UserRepo")
                null
            }
        }
    }

    // Recent transactions: 5 most recent involving the user
    private fun getRecentTransactions(userId: ObjectId): List<RecentTransaction> {
        println("DEBUG: getRecentTransactions called for userId: $userId")
        val transactions = transactionRepo.findAllTransactionsInvolvingUser(userId)
            .sortedByDescending { it.transaction_created_at }
            .take(5)

        println("DEBUG: Processing ${transactions.size} recent transactions (after sort & limit)")

        return transactions.map { transaction ->
            val groupName = transaction.group_id?.let { groupId ->
                groupRepo.findById(groupId).map { it.name }.orElse(null)
            }

            // Determine status: DEBITED if user paid, CREDITED if user received
            val status = if (transaction.userID == userId) {
                // User created/paid this transaction
                Status.DEBITED
            } else if (transaction.to?.id == userId) {
                // User received money from this transaction
                Status.CREDITED
            } else {
                // User is in the split (owes money)
                Status.DEBITED
            }

            // Determine who to show the transaction with
            val payedToName = if (transaction.userID == userId) {
                // User paid, show recipient name
                transaction.to.name
            } else {
                // User received or in split, show payer name
                userRepo.findById(transaction.userID).map { it.userName }.orElse("Unknown")
            }

            RecentTransaction(
                payed_to_name = payedToName,
                group_name = groupName,
                amount = transaction.amount,
                status = status
            )
        }
    }
}

