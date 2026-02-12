package org.example.splitpalbackendv2.Controller

import org.bson.types.ObjectId
import org.example.splitpalbackendv2.Model.groupModel
import org.example.splitpalbackendv2.Model.userModel
import org.example.splitpalbackendv2.Service.FirstTimeUserLoginRequest
import org.example.splitpalbackendv2.Service.loginUserRequest
import org.example.splitpalbackendv2.Service.loginUserResponse
import org.example.splitpalbackendv2.Service.userService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

// DTO for group response with String IDs
data class GroupDTO(
    val id: String?,
    val name: String,
    val description: String?,
    val Admin: String?,
    val created_at: LocalDateTime?,
    val member: List<String>
)

// Extension function to convert groupModel to GroupDTO
fun groupModel.toDTO(): GroupDTO = GroupDTO(
    id = this.id?.toHexString(),
    name = this.name,
    description = this.description,
    Admin = this.Admin?.toHexString(),
    created_at = this.created_at,
    member = this.member
)

@RestController
@RequestMapping("/users")
class userController(
    private val userService: userService,
//    private val splitService: splitService
) {

    @GetMapping("/demo")
    fun demo(): ResponseEntity<String> {
        return ResponseEntity("woring BC", HttpStatus.CREATED)
    }

//    @PostMapping("/signup")
//    fun createUser(@RequestBody user: FirstTimeUserLoginRequest): ResponseEntity<userModel> {
//        if (userService.findUserByPhoneNumber(user.phoneNumber) != null) {
//            return ResponseEntity(HttpStatus.ALREADY_REPORTED)
//        } else {
//            val newUser = userService.createNewUser(user)
//            return ResponseEntity(newUser, HttpStatus.CREATED)
//        }
//    }

    @PostMapping("/login")
    fun login(@RequestBody user: loginUserRequest): ResponseEntity<loginUserResponse> {
        val response = userService.loginUser(user)
        return ResponseEntity(response, HttpStatus.OK)
    }

//    @GetMapping("/involSplit")
//    fun invSplit(authentication: Authentication): ResponseEntity<List<flatcardModel>> {
//        val userId = authentication.principal as ObjectId
//        val response = splitService.flatCardItems(userId)
//        return ResponseEntity(response, HttpStatus.OK)
//    }

//    @GetMapping("/allGroups")
//    fun allGroups(authentication: Authentication): ResponseEntity<List<GroupDTO>> {
//        val userId = authentication.principal as ObjectId
//        val response = userService.getUserGroups(userId)
//        if (response == null) {
//            return ResponseEntity(HttpStatus.NOT_FOUND)
//        } else {
//            // Convert to DTO with String IDs
//            val dtoList = response.map { it.toDTO() }
//            return ResponseEntity(dtoList, HttpStatus.OK)
//        }
//    }
    @GetMapping("/contacts")
    fun getContacts(authentication: Authentication): ResponseEntity<List<org.example.splitpalbackendv2.Service.UserContact>> {
        val userId = authentication.principal as ObjectId
        val contacts = userService.getUserContacts(userId)
        return ResponseEntity.ok(contacts)
    }
}
