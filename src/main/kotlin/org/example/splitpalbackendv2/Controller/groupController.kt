package org.example.splitpalbackendv2.Controller

import org.bson.types.ObjectId
import org.example.splitpalbackendv2.Service.CreateGroupRequest
import org.example.splitpalbackendv2.Service.CreateGroupResponse
import org.example.splitpalbackendv2.Service.InsufficientCreditsException
import org.example.splitpalbackendv2.Service.groupService

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/groups")
class groupController(
    private val groupService: groupService
) {
    
    @PostMapping("/newgroup")
    fun createGroup(
        @RequestBody request: CreateGroupRequest, 
        authentication: Authentication
    ): ResponseEntity<CreateGroupResponse> {
        val userId = authentication.principal as ObjectId
        val response = groupService.createGroup(userId, request)
        return ResponseEntity(response, HttpStatus.CREATED)
    }
    
    @GetMapping("/credits")
    fun getRemainingCredits(authentication: Authentication): ResponseEntity<Map<String, Int>> {
        val userId = authentication.principal as ObjectId
        val credits = groupService.getRemainingCredits(userId)
        return ResponseEntity.ok(mapOf("remainingCredits" to credits))
    }
    
    // Exception handler for insufficient credits
    @ExceptionHandler(InsufficientCreditsException::class)
    fun handleInsufficientCredits(ex: InsufficientCreditsException): ResponseEntity<Map<String, Any>> {
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(mapOf(
                "error" to "INSUFFICIENT_CREDITS",
                "message" to (ex.message ?: "No credits remaining"),
                "remainingCredits" to 0
            ))
    }
}