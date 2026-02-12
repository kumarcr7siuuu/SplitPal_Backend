package org.example.splitpalbackendv2.Controller

import org.bson.types.ObjectId
import org.example.splitpalbackendv2.Model.DashboardScreen
import org.example.splitpalbackendv2.Model.TimelineChat
import org.example.splitpalbackendv2.Service.DashboardService
import org.example.splitpalbackendv2.Service.PaginatedTimelineResponse
import org.example.splitpalbackendv2.Service.TimelineService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.stereotype.Controller

@RestController
@RequestMapping("/timeline")
class TimelineRestController(
    private val timelineService: TimelineService
) {
    
    // User-to-User timeline (P2P chat history)
    @GetMapping("/{targetUserId}")
    fun getTimelinePaginated(
        @PathVariable targetUserId: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        authentication: Authentication
    ): ResponseEntity<PaginatedTimelineResponse> {
        val currentUserId = authentication.principal as ObjectId
        val response = timelineService.getTimelinePaginated(
            currentUserId, 
            ObjectId(targetUserId), 
            page, 
            size
        )
        return ResponseEntity.ok(response)
    }
    
    // Group timeline (Group chat history)
    @GetMapping("/group/{groupId}")
    fun getGroupTimelinePaginated(
        @PathVariable groupId: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        authentication: Authentication
    ): ResponseEntity<PaginatedTimelineResponse> {
        val currentUserId = authentication.principal as ObjectId
        val response = timelineService.getGroupTimelinePaginated(
            ObjectId(groupId),
            currentUserId,
            page, 
            size
        )
        return ResponseEntity.ok(response)
    }
}
@Controller
class TimelineController(
    private val timelineService: TimelineService,
    private val dashboardService: DashboardService
) {

    @QueryMapping
    fun timeline(@Argument targetUserId: String): List<TimelineChat> {
        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication == null) {
            throw RuntimeException("User not authenticated")
        }

        val currentUserId = authentication.principal as ObjectId
        val targetId = ObjectId(targetUserId)
        return timelineService.getTimeline(currentUserId, targetId)
    }

    @QueryMapping
    fun dashboard(@Argument userNumber: String): DashboardScreen {
        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication == null) {
            throw RuntimeException("User not authenticated")
        }

        val currentUserId = authentication.principal as ObjectId
        return dashboardService.getDashboard(currentUserId,userNumber)
    }
}
