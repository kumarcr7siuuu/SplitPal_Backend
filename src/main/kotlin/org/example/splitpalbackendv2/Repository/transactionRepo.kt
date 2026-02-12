package org.example.splitpalbackendv2.Repository
import org.bson.types.ObjectId
import org.example.splitpalbackendv2.Model.groupModel
import org.example.splitpalbackendv2.Model.transactionModel
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query


interface transactionRepo: MongoRepository<transactionModel, ObjectId> {
    fun findAllTransactionByUserID(userId: ObjectId): List<transactionModel>
    @Query("{ 'group_id' : ?0 }")
    fun findByGroup_id(groupId: ObjectId): transactionModel?

    @Query("{ '\$or': [ { 'userID': ?0, 'to.id': ?1, 'group_id': null }, { 'userID': ?1, 'to.id': ?0, 'group_id': null } ] }")
    fun findP2PTransactions(currentUserId: ObjectId, targetUserId: ObjectId): List<transactionModel>

    @Query("{ '\$or': [ { 'userID': ?0, 'involvedUser.owedBy': ?1 }, { 'userID': ?1, 'involvedUser.owedBy': ?0 } ] }")
    fun findSharedTransactions(currentUserId: ObjectId, targetUserId: ObjectId): List<transactionModel>

    // Paginated: Combined query for both P2P and Shared transactions between two users
    @Query("{ '\$or': [ { 'userID': ?0, 'to.id': ?1 }, { 'userID': ?1, 'to.id': ?0 }, { 'userID': ?0, 'involvedUser.owedBy': ?1 }, { 'userID': ?1, 'involvedUser.owedBy': ?0 } ] }")
    fun findTimelineTransactions(currentUserId: ObjectId, targetUserId: ObjectId, pageable: Pageable): Page<transactionModel>

    // Dashboard queries
    @Query("{ 'involvedUser': { '\$elemMatch': { 'owedBy': ?0, 'status': false } } }")
    fun findTransactionsWhereUserOwes(userId: ObjectId): List<transactionModel>

    fun findTransactionByUserID(userId: ObjectId): List<transactionModel>
    
    // Paginated: Get transactions by group
    @Query("{ 'group_id': ?0 }")
    fun findByGroupIdPaginated(groupId: ObjectId, pageable: Pageable): Page<transactionModel>

    // Dashboard: Find all transactions where user is involved (as payer, recipient, or in involvedUser)
    @Query("{ '\$or': [ { 'userID': ?0 }, { 'to.id': ?0 }, { 'involvedUser.owedBy': ?0 } ] }")
    fun findAllTransactionsInvolvingUser(userId: ObjectId): List<transactionModel>
}

