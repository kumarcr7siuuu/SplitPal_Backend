package org.example.splitpalbackendv2.Repository

import org.bson.types.ObjectId
import org.example.splitpalbackendv2.Model.groupModel
import org.example.splitpalbackendv2.Model.splitModel
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.graphql.data.method.annotation.Argument

interface groupRepo : MongoRepository<groupModel, ObjectId> {
    //    fun findNameByid(id: ObjectId?): String
//    fun findMemberByGroupId(group_id: ObjectId): List<String>

    @Query("{ 'member': ?0 }")
    fun findByMemberContaining(@Argument("userNumber") id: String): List<groupModel>

    // Paginated version
    @Query("{ 'member': ?0 }")
    fun findByMemberContaining(memberId: ObjectId, pageable: Pageable): Page<groupModel>
}
