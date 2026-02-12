//package org.example.splitpalbackendv2.Repository
//import org.bson.types.ObjectId
//import org.example.splitpalbackendv2.Model.splitModel
//import org.springframework.data.mongodb.repository.MongoRepository
//
//import org.springframework.data.mongodb.repository.Query
//
//interface splitRepo: MongoRepository<splitModel,ObjectId> {
//    @Query("{ 'owedBy.?0': { \$exists: true } }")
//    fun findByOwedBy(id: String): List<splitModel>
//
//
//    fun deleteByTransactionId(transactionId: ObjectId)
//}