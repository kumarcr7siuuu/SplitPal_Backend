package org.example.splitpalbackendv2.Repository
//import org.example.splitpalbackendv2.Controller.OtpVerification
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.repository.MongoRepository
import java.time.LocalDateTime

data class otpModel(
    @Id val email: String,
    val code: String, val expiry: LocalDateTime
)

interface otpRepo : MongoRepository<otpModel, String> {
    fun findByEmail(email: String): otpModel?
}