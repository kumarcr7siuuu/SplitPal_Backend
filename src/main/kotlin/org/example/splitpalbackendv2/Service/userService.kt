package org.example.splitpalbackendv2.Service

import org.bson.types.ObjectId
import org.example.splitpalbackendv2.Model.groupModel
//import org.example.splitpalbackendv2.Model.tokenModel
import org.example.splitpalbackendv2.Model.userModel
import org.example.splitpalbackendv2.Repository.groupRepo
import org.example.splitpalbackendv2.Repository.userRepo
import org.example.splitpalbackendv2.security.TokenService
//import org.example.backendsp.security.UserDetails
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer

data class FirstTimeUserLoginRequest(var userName: String, var phoneNumber: String, var password: String)

//abhi ka liya acces and refresh token null hai because in futere jwt sai replace hoga
data class FirstTimeUserLoginResponse(
    val userName: String,
    var phoneNumber: String,
    var created_at: LocalDateTime,
    var secureToken: String? = null,
    var refreshToken: String? = null
)

data class GroupData(
    @JsonSerialize(using = ToStringSerializer::class)
    val id: ObjectId,
    val name: String,
)

fun groupModel.toGroupData(): GroupData {
    return GroupData(
        id = this.id!!,
        name = this.name,
    )
}

data class loginUserRequest (var phoneNumber: String, var password: String)
data class loginUserResponse(
    val userName: String,
    var phoneNumber: String,
    var created_at: LocalDateTime,
    var secureToken: String? = null,
    var refreshToken: String? = null
)

data class UserContact(
    @JsonSerialize(using = ToStringSerializer::class)
    val id: ObjectId,
    val name: String
)

@Service
class userService(
    private val userRepository: userRepo,
    private val groupRepo: groupRepo,
    private val transactionRepo: org.example.splitpalbackendv2.Repository.transactionRepo,
    private val passwordEncoder: PasswordEncoder,
    private val tokenService: TokenService
) {
    fun groupModel.togroupName(): String {
        return this.name
    }

//    fun findGropsByid(id: ObjectId): List<groupModel>? =
//        if (userRepository.findgroupsByid(id)==null){
//            throw Exception("no group present in database")
//        }
//    else userRepository.findgroupsByid(id)


    fun findUserByPhoneNumber(phoneNumber: String) = userRepository.findByPhoneNumber(phoneNumber)

    fun createNewUser(firstTimeUserRequest: FirstTimeUserLoginRequest): userModel {
        // Input validation
        require(firstTimeUserRequest.userName.isNotBlank()) { "Username cannot be blank" }
        require(firstTimeUserRequest.phoneNumber.matches(Regex("^\\+\\d{1,3}-\\d{10}$"))) { "Invalid phone number format (must be 10 digits)" }
        require(firstTimeUserRequest.password.length >= 6) { "Password must be at least 6 characters" }
        
        // 1. Create User with Encoded Password
        val newUser = userModel(
            userName = firstTimeUserRequest.userName.trim(),
            phoneNumber = firstTimeUserRequest.phoneNumber.trim(),
            password = passwordEncoder.encode(firstTimeUserRequest.password)!!,
        )
        // Save first to get ID
        val savedUser = userRepository.save(newUser)

        // 2. Generate Tokens
        val userDetails = org.example.splitpalbackendv2.security.UserDetails(
            userId = savedUser.id!!,
            password = savedUser.password,
            initialized_at = java.util.Date(),
            expires_at = java.util.Date()
        )
        val accessToken = tokenService.generateAccessToken(userDetails)
        val refreshToken = tokenService.generateRefreshToken(userDetails)

        // 3. Save Tokens to User
        savedUser.secureToken = accessToken
        savedUser.refreshToken = refreshToken
        
        return userRepository.save(savedUser)
    }

    fun loginUser(loginUserRequest: loginUserRequest): loginUserResponse? {
        println("UserService: Attempting login for ${loginUserRequest.phoneNumber}")
        val user = userRepository.findByPhoneNumber(loginUserRequest.phoneNumber)
        if (user == null) {
            println("UserService: User not found for phone number: ${loginUserRequest.phoneNumber}")
            throw Exception("User with phone number ${loginUserRequest.phoneNumber} not found.")
        }

        // Verify Password
        if (passwordEncoder.matches(loginUserRequest.password, user.password)) {
            println("UserService: Password matched for ${loginUserRequest.phoneNumber}")
            // Generate New Tokens
            val userDetails = org.example.splitpalbackendv2.security.UserDetails(
                userId = user.id!!,
                password = user.password,
                initialized_at = java.util.Date(),
                expires_at = java.util.Date()
            )
            val accessToken = tokenService.generateAccessToken(userDetails)
            val refreshToken = tokenService.generateRefreshToken(userDetails)

            // Save tokens to DB
            user.secureToken = accessToken
            user.refreshToken = refreshToken
            userRepository.save(user)

            return loginUserResponse(
                userName = user.userName,
                phoneNumber = user.phoneNumber,
                created_at = user.created_at,
                secureToken = accessToken,
                refreshToken = refreshToken
            )
        } else {
            println("UserService: Password mismatch for ${loginUserRequest.phoneNumber}")
            throw Exception("Invalid credentials.") // Generic message for security
        }
    }

    fun refreshToken(token: String): loginUserResponse {
        val userIdString = tokenService.extractUserID(token) ?: throw Exception("Invalid token")
        val userId = ObjectId(userIdString)
        val user = userRepository.findById(userId).orElseThrow { Exception("User not found") }

        // Validate token matches DB
        if (user.refreshToken != token) {
            throw Exception("Invalid refresh token")
        }

        val userDetails = org.example.splitpalbackendv2.security.UserDetails(
            userId = user.id!!,
            password = user.password,
            initialized_at = java.util.Date(),
            expires_at = java.util.Date()
        )

        if (!tokenService.isValid(token, userDetails)) {
             throw Exception("Token expired or invalid")
        }

        // Generate new tokens
        val newAccessToken = tokenService.generateAccessToken(userDetails)
        val newRefreshToken = tokenService.generateRefreshToken(userDetails) // Rotate refresh token too

        // Save
        user.secureToken = newAccessToken
        user.refreshToken = newRefreshToken
        userRepository.save(user)

        return loginUserResponse(
            userName = user.userName,
            phoneNumber = user.phoneNumber,
            created_at = user.created_at,
            secureToken = newAccessToken,
            refreshToken = newRefreshToken
        )
//    }
//
//    fun getUserGroups(userId: ObjectId): List<
//            groupModel>? {
//        // Query the groups collection directly instead of relying on the user object
//        return groupRepo.findByMemberContaining(userId)
    }

    fun getUserProfile(userId: ObjectId): loginUserResponse {
        val user = userRepository.findById(userId).orElseThrow { Exception("User not found") }
        return loginUserResponse(
            userName = user.userName,
            phoneNumber = user.phoneNumber,
            created_at = user.created_at,
            secureToken = user.secureToken,
            refreshToken = user.refreshToken
        )
    }

    // Get list of users the current user has interacted with
    fun getUserContacts(userId: ObjectId): List<UserContact> {
        // 1. Find all transactions involving the user
        val transactions = transactionRepo.findAllTransactionsInvolvingUser(userId)
        
        // 2. Extract unique IDs of OTHER users
        val contactIds = transactions
            .mapNotNull { transaction ->
                // Who is the "other" person in this transaction?
                when {
                    transaction.userID == userId -> transaction.to?.id // I paid -> returns Recipient
                    transaction.to?.id == userId -> transaction.userID // I received -> returns Payer
                    else -> transaction.userID // I'm split participant -> returns Transaction Creator
                }
            }
            .filter { it != userId } // Exclude myself
            .distinct()
            
        // 3. Fetch user details (name) for these IDs
        // Note: filtered by non-null to exclude deleted users or invalid IDs
        return contactIds.mapNotNull { contactId ->
            userRepository.findById(contactId).map { 
                UserContact(
                    id = it.id!!,
                    name = it.userName
                )
            }.orElse(null)
        }
    }
}
