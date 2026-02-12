package org.example.splitpalbackendv2.Repository

import org.bson.types.ObjectId
import org.example.splitpalbackendv2.Model.groupModel
import org.example.splitpalbackendv2.Model.userModel
import org.springframework.data.mongodb.repository.MongoRepository
import java.util.Optional

interface userRepo: MongoRepository<userModel, ObjectId> {
    fun findByPhoneNumber(phoneNumber: String): userModel?
    fun findByPhoneNumberIn(phoneNumbers: List<String>): List<userModel>
    override fun findById(id: ObjectId): Optional<userModel>
//    fun findgroupsByid(id: ObjectId): List<groupModel>?
}