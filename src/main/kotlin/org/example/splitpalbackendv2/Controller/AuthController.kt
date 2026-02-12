package org.example.splitpalbackendv2.Controller

import org.bson.types.ObjectId
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

@RestController
@RequestMapping("/auth")
class AuthController(
    private val userService: userService
) {

    @PostMapping("/signup")
    fun signup(@RequestBody request: FirstTimeUserLoginRequest): ResponseEntity<userModel> {
        return if (userService.findUserByPhoneNumber(request.phoneNumber) != null) {
             ResponseEntity(HttpStatus.ALREADY_REPORTED)
        } else {
             val newUser = userService.createNewUser(request)
             ResponseEntity(newUser, HttpStatus.CREATED)
        }
    }

    @PostMapping("/login")
    fun login(@RequestBody request: loginUserRequest): ResponseEntity<loginUserResponse> {
        return try {
            val response = userService.loginUser(request)
            ResponseEntity(response, HttpStatus.OK)
        } catch (e: Exception) {
            println("AuthController: Login failed. Error: ${e.message}")
            e.printStackTrace()
            ResponseEntity(HttpStatus.UNAUTHORIZED)
        }
    }

    @PostMapping("/refresh")
    fun refresh(@RequestBody request: RefreshTokenRequest): ResponseEntity<loginUserResponse> {
        return try {
             val response = userService.refreshToken(request.refreshToken)
             ResponseEntity(response, HttpStatus.OK)
        } catch (e: Exception) {
             ResponseEntity(HttpStatus.UNAUTHORIZED)
        }
    }

    @GetMapping("/validate")
    fun validate(authentication: Authentication): ResponseEntity<loginUserResponse> {
        return try {
            val userId = authentication.principal as ObjectId
            val response = userService.getUserProfile(userId)
            ResponseEntity(response, HttpStatus.OK)
        } catch (e: Exception) {
            ResponseEntity(HttpStatus.UNAUTHORIZED)
        }
    }
}

data class RefreshTokenRequest(val refreshToken: String)
