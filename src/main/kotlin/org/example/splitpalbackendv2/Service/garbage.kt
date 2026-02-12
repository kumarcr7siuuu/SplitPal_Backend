////import org.bson.types.ObjectId
////import org.example.splitpalbackendv2.Model.transactionModel
////import org.example.splitpalbackendv2.Repository.transactionRepo
////import org.example.splitpalbackendv2.Repository.userRepo
////import org.springframework.stereotype.Service
////
//////import org.example.splitpalbackendv2.Model.amountStatus
////
////@Service
////class transactionService(
////    private val transactionRepository: transactionRepo,
////    private val userRepo: userRepo,
////) {
////    fun FindAllTransactionByUserID(userId: ObjectId): List<TransactionResponse> {
////        val transactions = transactionRepository.findAllTransactionByUserID(userId) ?: return emptyList()
////
////        return transactions.map { transaction ->
////            val user = userRepo.findById(transaction.userID).orElse(null)
////            val userName = user?.userName ?: "Unknown"
////
////            TransactionResponse(
////                id = transaction.id.toString(),
////                splitId = transaction.splitId?.toString(),
////                group_id = transaction.group_id?.toString(),
////                userID = transaction.userID.toString(),
////                userName = userName,
////                to = transaction.to,
////                amount = transaction.amount,
////                transaction_created_at = transaction.transaction_created_at,
////                description = transaction.description,
////                interactionId = transaction.interactionId
////            )
////        }
////    }
////
////    fun createTransaction(request: CreateTransactionRequest): transactionModel {
////        return when {
////            request.group_id != null -> createGroupTransaction(request)
////            !request.involvedUserIds.isNullOrEmpty() -> createIndividualSplitTransaction(request)
////            else -> createDirectTransaction(request)
////        }
////    }
////
////    // 1. Service for making a transaction without groupId and involveduserIds (Direct Payment)
////    private fun createDirectTransaction(request: CreateTransactionRequest): transactionModel {
////        val transaction = transactionModel(
////            userID = request.payer_id,
////            amount = request.amount,
////            description = request.description,
////            to = request.to
////        )
////        return transactionRepository.save(transaction)
////    }
////
////    // 2. Service for creating transaction using groupId but no involveduserIds (Group Split)
////    private fun createGroupTransaction(request: CreateTransactionRequest): transactionModel {
////        val transaction = transactionModel(
////            group_id = request.group_id,
////            userID = request.payer_id,
////            amount = request.amount,
////            description = request.description,
////            to = request.to
////        )
////        val savedTransaction = transactionRepository.save(transaction)
////
////        // Calculate split for the whole group (or selective if involvedUserIds provided)
////        splitService.calculateAndSaveSplits(savedTransaction, request.group_id, request.involvedUserIds)
////
////        return savedTransaction
////    }
////
////    // 3. Service for creating transaction using involveduserIds and interactionId but no group id (Individual Split)
////    private fun createIndividualSplitTransaction(request: CreateTransactionRequest): transactionModel {
////        val transaction = transactionModel(
////            userID = request.payer_id,
////            amount = request.amount,
////            description = request.description,
////            to = request.to,
////            interactionId = request.interactionId // Map of amounts provided by client
////        )
////        val savedTransaction = transactionRepository.save(transaction)
////
////        // Also create a Split document for settlement tracking, relying on involvedUserIds
////        splitService.calculateAndSaveSplits(savedTransaction, null, request.involvedUserIds)
////
////        return savedTransaction
////    }
////}
////
////data class CreateTransactionRequest(
////    val group_id: ObjectId? = null,
////    val payer_id: ObjectId,
////    val to : String,
////    val amount: Int,
////    val description: String? = null,
////    val involvedUserIds: List<ObjectId>? = null,
////    val interactionId: MutableMap<String, amountStatus>? = null
////)
////
////data class TransactionResponse(
////    val id: String,
////    val splitId: String?,
////    val group_id: String?,
////    val userID: String,
////    val userName: String,
////    val to: String?,
////    val amount: Int,
////    val transaction_created_at: java.time.LocalDateTime?,
////    val description: String?,
////    val interactionId: Map<String, amountStatus>?
////)//import org.example.splitpalbackendv2.Model.amountStatus
////
////@Service
////class transactionService(
////    private val transactionRepository: transactionRepo,
////    private val userRepo: userRepo,
////) {
////    fun FindAllTransactionByUserID(userId: ObjectId): List<TransactionResponse> {
////        val transactions = transactionRepository.findAllTransactionByUserID(userId) ?: return emptyList()
////
////        return transactions.map { transaction ->
////            val user = userRepo.findById(transaction.userID).orElse(null)
////            val userName = user?.userName ?: "Unknown"
////
////            TransactionResponse(
////                id = transaction.id.toString(),
////                splitId = transaction.splitId?.toString(),
////                group_id = transaction.group_id?.toString(),
////                userID = transaction.userID.toString(),
////                userName = userName,
////                to = transaction.to,
////                amount = transaction.amount,
////                transaction_created_at = transaction.transaction_created_at,
////                description = transaction.description,
////                interactionId = transaction.interactionId
////            )
////        }
////    }
////
////    fun createTransaction(request: CreateTransactionRequest): transactionModel {
////        return when {
////            request.group_id != null -> createGroupTransaction(request)
////            !request.involvedUserIds.isNullOrEmpty() -> createIndividualSplitTransaction(request)
////            else -> createDirectTransaction(request)
////        }
////    }
////
////    // 1. Service for making a transaction without groupId and involveduserIds (Direct Payment)
////    private fun createDirectTransaction(request: CreateTransactionRequest): transactionModel {
////        val transaction = transactionModel(
////            userID = request.payer_id,
////            amount = request.amount,
////            description = request.description,
////            to = request.to
////        )
////        return transactionRepository.save(transaction)
////    }
////
////    // 2. Service for creating transaction using groupId but no involveduserIds (Group Split)
////    private fun createGroupTransaction(request: CreateTransactionRequest): transactionModel {
////        val transaction = transactionModel(
////            group_id = request.group_id,
////            userID = request.payer_id,
////            amount = request.amount,
////            description = request.description,
////            to = request.to
////        )
////        val savedTransaction = transactionRepository.save(transaction)
////
////        // Calculate split for the whole group (or selective if involvedUserIds provided)
////        splitService.calculateAndSaveSplits(savedTransaction, request.group_id, request.involvedUserIds)
////
////        return savedTransaction
////    }
////
////    // 3. Service for creating transaction using involveduserIds and interactionId but no group id (Individual Split)
////    private fun createIndividualSplitTransaction(request: CreateTransactionRequest): transactionModel {
////        val transaction = transactionModel(
////            userID = request.payer_id,
////            amount = request.amount,
////            description = request.description,
////            to = request.to,
////            interactionId = request.interactionId // Map of amounts provided by client
////        )
////        val savedTransaction = transactionRepository.save(transaction)
////
////        // Also create a Split document for settlement tracking, relying on involvedUserIds
////        splitService.calculateAndSaveSplits(savedTransaction, null, request.involvedUserIds)
////
////        return savedTransaction
////    }
////}
////
////data class CreateTransactionRequest(
////    val group_id: ObjectId? = null,
////    val payer_id: ObjectId,
////    val to : String,
////    val amount: Int,
////    val description: String? = null,
////    val involvedUserIds: List<ObjectId>? = null,
////    val interactionId: MutableMap<String, amountStatus>? = null
////)
////
////data class TransactionResponse(
////    val id: String,
////    val splitId: String?,
////    val group_id: String?,
////    val userID: String,
////    val userName: String,
////    val to: String?,
////    val amount: Int,
////    val transaction_created_at: java.time.LocalDateTime?,
////    val description: String?,
////    val interactionId: Map<String, amountStatus>?
////)
//
//
//
//[
//{
//    "_id": { "$oid": "65b900000000000000000001" },
//    "userID": { "$oid": "6970ee460ba5c1301f3d6129" },
//    "amount": 100,
//    "description": "Lunch (P2P You -> Target)",
//    "to": {
//    "name": "Target User",
//    "id": { "$oid": "6970ee460ba5c1301f3d6129" },
//    "upi_id": null
//},
//    "group_id": null,
//    "involvedUser": null,
//    "transaction_created_at": { "$date": "2026-01-21T10:00:00.000Z" },
//    "_class": "org.example.splitpalbackendv2.Model.transactionModel"
//},
//{
//    "_id": { "$oid": "65b900000000000000000002" },
//    "userID": { "$oid": "6970ee460ba5c1301f3d6129" },
//    "amount": 50,
//    "description": "Coffee (P2P Target -> You)",
//    "to": {
//    "name": "You",
//    "id": { "$oid": "6970ee460ba5c1301f3d6129" },
//    "upi_id": null
//},
//    "group_id": null,
//    "involvedUser": null,
//    "transaction_created_at": { "$date": "2026-01-21T11:00:00.000Z" },
//    "_class": "org.example.splitpalbackendv2.Model.transactionModel"
//},
//{
//    "_id": { "$oid": "65b900000000000000000003" },
//    "userID": { "$oid": "REPLACE_WITH_YOUR_USER_ID" },
//    "amount": 300,
//    "description": "Shared Dinner with Demo & Demo2",
//    "to": {
//    "name": "demo",
//    "id": { "$oid": "6947f4e1a693e44a1ff0bb56" },
//    "upi_id": null
//},
//    "group_id": null,
//    "involvedUser": [
//    {
//        "_id": { "$oid": "65b900000000000000000101" },
//        "transactionId": { "$oid": "65b900000000000000000003" },
//        "SplitAmout": 150,
//        "payed_by": { "$oid": "REPLACE_WITH_YOUR_USER_ID" },
//        "owedBy": { "$oid": "REPLACE_WITH_YOUR_USER_ID" },
//        "status": true,
//        "settled_date": null
//    },
//    {
//        "_id": { "$oid": "65b900000000000000000102" },
//        "transactionId": { "$oid": "65b900000000000000000003" },
//        "SplitAmout": 150,
//        "payed_by": { "$oid": "REPLACE_WITH_YOUR_USER_ID" },
//        "owedBy": { "$oid": "6947e847a693e44a1ff0bb55" },
//        "status": false,
//        "settled_date": null
//    }
//    ],
//    "transaction_created_at": { "$date": "2026-01-21T20:00:00.000Z" },
//    "_class": "org.example.splitpalbackendv2.Model.transactionModel"
//}
//]