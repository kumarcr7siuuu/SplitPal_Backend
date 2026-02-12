package org.example.splitpalbackendv2.Service

import org.bson.types.ObjectId
import org.example.splitpalbackendv2.Model.*
import org.example.splitpalbackendv2.Repository.transactionRepo
import org.example.splitpalbackendv2.Repository.userRepo
import org.example.splitpalbackendv2.Repository.groupRepo
import org.springframework.stereotype.Service

@Service
class transactionService(
    private val transactionRepo: transactionRepo,
    private val userRepo: userRepo,
    private val groupRepo: groupRepo
) {
    fun createTransaction(
        userId: ObjectId,
        request: CreateTransactionRequest
    ): TransactionResponseDTO {
        
        val groupId = request.group_id?.let {
            if (it.isNotEmpty()) ObjectId(it) else null
        }
        
        val involvedUsersList = if (groupId != null) {
            // 1. Fetch Group
            val group = groupRepo.findById(groupId)
                .orElseThrow { RuntimeException("Group not found with id: ${request.group_id}") }

            // 2. Fetch all members by phone number
            val members = userRepo.findByPhoneNumberIn(group.member)
            if (members.isEmpty()) {
                throw RuntimeException("No valid members found for group ${group.name}")
            }

            // 3. Calculate Split amount (Total / Count)
            val splitAmount = request.amount / members.size
            
            // 4. Create split entries for EACH member
            members.map { member ->
                splitModel(
                    id = ObjectId.get(),
                    // logic: "payed_by" is the transaction creator (userId)
                    // "owedBy" is the member
                    payed_by = userId,
                    owedBy = member.id!!,
                    SplitAmout = splitAmount,
                    // If member IS the payer, status is true (settled/paid), else false
                    status = (member.id == userId),
                    settled_date = if (member.id == userId) java.time.LocalDateTime.now() else null
                )
            }.toMutableList()

        } else {
            // Non-group transaction: use provided list or empty
             null
        }

        val transaction = transactionModel(
            id = ObjectId.get(),
            userID = userId,
            amount = request.amount,
            description = request.description,
            to = request.to,
            group_id = groupId,
            involvedUser = involvedUsersList, // Use the calculated list
            transaction_created_at = java.time.LocalDateTime.now()
        )
        val savedTransaction = transactionRepo.save(transaction)
        return mapToTransactionResponseDTO(savedTransaction)
    }

    fun editTransaction(userId: ObjectId, request: EditTransactionRequest): TransactionResponseDTO {
        val transactionId = try {
            ObjectId(request.transaction_id)
        } catch (e: Exception) {
            throw RuntimeException("Invalid transaction ID format")
        }
        
        val existingTransaction = transactionRepo.findById(transactionId)
            .orElseThrow { RuntimeException("Transaction not found with id: ${request.transaction_id}") }
        
        // Validate ownership - only the creator can edit
        if (existingTransaction.userID != userId) {
            throw RuntimeException("User $userId is not authorized to edit this transaction")
        }
        
        val groupId = request.group_id?.let {
            if (it.isNotEmpty()) ObjectId(it) else null
        }
        
        val updatedTransaction = existingTransaction.copy(
            amount = request.amount ?: existingTransaction.amount,
            description = request.description ?: existingTransaction.description,
            to = request.to ?: existingTransaction.to,
            group_id = groupId ?: existingTransaction.group_id,
            involvedUser = request.involvedUser ?: existingTransaction.involvedUser
        )
        val saved = transactionRepo.save(updatedTransaction)
        return mapToTransactionResponseDTO(saved)
    }

    fun getTransactionDetails(transactionId: ObjectId): TransactionDetailResponse {
        val txn = transactionRepo.findById(transactionId)
            .orElseThrow { RuntimeException("Transaction not found with id: $transactionId") }
        
        // Get group name if exists
        val groupName = txn.group_id?.let { 
            groupRepo.findById(it).orElse(null)?.name 
        }
        
        // Get payer info
        val payer = userRepo.findById(txn.userID).orElse(null)
        
        // Map splits with user names (sorted by amount DESC)
        val splitDetails = txn.involvedUser?.map { split ->
            val user = userRepo.findById(split.owedBy).orElse(null)
            SplitDetail(
                split_id = split.id!!.toHexString(), // Use Hex String for robustness
                user_id = split.owedBy.toString(),
                user_name = user?.userName ?: "Unknown User",
                phoneNumber = user?.phoneNumber,
                amount = split.SplitAmout,
                status = split.status,
                settled_date = split.settled_date
            )
        }?.sortedByDescending { it.amount } ?: emptyList()
        
        return TransactionDetailResponse(
            id = txn.id.toString(),
            amount = txn.amount,
            description = txn.description,
            payed_by = PayerInfo(
                id = txn.userID.toString(),
                name = payer?.userName ?: txn.to?.name ?: "Unknown",
                phoneNumber = txn.to.phoneNumber
            ),
            date = txn.transaction_created_at,
            group_name = groupName,
            splits = splitDetails
        )
    }

    fun editSplit(userId: ObjectId, request: EditSplitRequest): TransactionResponseDTO {
        val transactionId = try {
            ObjectId(request.transaction_id)
        } catch (e: Exception) {
            throw RuntimeException("Invalid transaction ID format")
        }

        val transaction = transactionRepo.findById(transactionId)
            .orElseThrow { RuntimeException("Transaction not found with id: ${request.transaction_id}") }
        
        // Validate ownership - only the creator can edit splits
        // NOTE: For payments, the payer needs to confirm payment. 
        // If current logic allows ANYONE involved to settle, we should relax this check.
        // Assuming current requirement is strict:
        if (transaction.userID != userId) {
            // Check if user is involved in the split (self-settlement?)
            // For now, keep strict creator check or expand logic if needed.
             throw RuntimeException("User $userId is not authorized to edit splits in this transaction")
        }
        
        val splitId = try {
            ObjectId(request.split_id)
        } catch (e: Exception) {
            throw RuntimeException("Invalid split ID format")
        }
        
        // Find the split to edit
        val splitToEdit = transaction.involvedUser?.find { it.id == splitId }
            ?: throw RuntimeException("Split not found with id: ${request.split_id}")
        
        // Update the split fields
        request.amount?.let { splitToEdit.SplitAmout = it }
        request.status?.let { splitToEdit.status = it }
        
        val saved = transactionRepo.save(transaction)
        return mapToTransactionResponseDTO(saved)
    }

    private fun mapToTransactionResponseDTO(txn: transactionModel): TransactionResponseDTO {
        return TransactionResponseDTO(
            id = txn.id!!.toHexString(),
            group_id = txn.group_id?.toHexString(),
            userID = txn.userID.toHexString(),
            amount = txn.amount,
            transaction_created_at = txn.transaction_created_at,
            description = txn.description,
            to = PayedToDTO(
                name = txn.to.name,
                id = txn.to.id?.toHexString(),
                phoneNumber = txn.to.phoneNumber
            ),
            involvedUser = txn.involvedUser?.map { split ->
                SplitResponseDTO(
                    id = split.id?.toHexString(),
                    SplitAmout = split.SplitAmout,
                    payed_by = split.payed_by.toHexString(),
                    owedBy = split.owedBy.toHexString(),
                    status = split.status,
                    settled_date = split.settled_date
                )
            }
        )
    }
}
