package org.example.splitpalbackendv2.Service

import org.bson.types.ObjectId
import org.example.splitpalbackendv2.Repository.groupRepo
import org.example.splitpalbackendv2.Repository.userRepo
import org.example.splitpalbackendv2.Model.groupModel
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer

data class Group(val name: String, val description: String? = null)
//this string is phone number
data class CreateGroupRequest(val group: Group, val members: MutableList<String>)
data class groupResponse(val Admin: String, val name: String, @JsonSerialize(contentUsing = ToStringSerializer::class) val members: List<ObjectId>)

// Paginated response for groups
data class PaginatedGroupResponse(
    val content: List<groupModel>,
    val currentPage: Int,
    val totalPages: Int,
    val totalItems: Long,
    val hasMore: Boolean
)

// Custom exception for insufficient credits
class InsufficientCreditsException(message: String) : RuntimeException(message)

// Response wrapper for group creation
data class CreateGroupResponse(
    val group: groupModel,
    val remainingCredits: Int
)

@Service
class groupService(
    private val groupRepo: groupRepo,
    private val userRepo: userRepo
) {
    
    /**
     * Create a new group if user has sufficient credits.
     * Each user starts with 3 free credits.
     * @throws InsufficientCreditsException if user has 0 credits
     */
    fun createGroup(adminId: ObjectId, request: CreateGroupRequest): CreateGroupResponse {
        // Check if user has credits
        val user = userRepo.findById(adminId).orElseThrow { 
            RuntimeException("User not found") 
        }
        
        if (user.groupCredits <= 0) {
            throw InsufficientCreditsException(
                "No group credits remaining. You have used all 3 free group creations. " +
                "Please purchase more credits to create additional groups."
            )
        }
        
        // Ensure admin is in the members list
        val adminPhone = user.phoneNumber
        if (!request.members.contains(adminPhone)) {
            request.members.add(adminPhone)
        }

        // Create the group
        val group = groupRepo.save(
            groupModel(
                Admin = adminId,
                name = request.group.name,
                description = request.group.description,
                member = request.members
            )
        )
        
        // Decrement user's credits
        user.groupCredits -= 1
        userRepo.save(user)
        
        return CreateGroupResponse(
            group = group,
            remainingCredits = user.groupCredits
        )
    }
    
    /**
     * Get remaining group credits for a user
     */
    fun getRemainingCredits(userId: ObjectId): Int {
        val user = userRepo.findById(userId).orElseThrow {
            RuntimeException("User not found")
        }
        return user.groupCredits
    }

    fun findGroupById(id: ObjectId) = groupRepo.findById(id).orElse(null)

    // Paginated method to get user's groups
    fun getUserGroupsPaginated(userId: ObjectId, page: Int, size: Int): PaginatedGroupResponse {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "created_at"))
        val groupPage = groupRepo.findByMemberContaining(userId, pageable)
        
        return PaginatedGroupResponse(
            content = groupPage.content,
            currentPage = groupPage.number,
            totalPages = groupPage.totalPages,
            totalItems = groupPage.totalElements,
            hasMore = groupPage.hasNext()
        )
    }

    fun addMember(groupid: ObjectId, member: String): List<String>? {
        val group = groupRepo.findById(groupid).orElse(null)
        if (group != null) {
            if (userRepo.findByPhoneNumber(member) != null) {
                val groupList = group.member
                groupList.add(member)
                return groupList
            }
        } else {
            throw Exception("Group not found")
        }
        return null
    }
}