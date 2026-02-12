package org.example.splitpalbackendv2.Service

import org.bson.types.ObjectId
import org.example.splitpalbackendv2.Model.TimelineChat
import org.example.splitpalbackendv2.Model.groupModel
import org.example.splitpalbackendv2.Model.splitModel
import org.example.splitpalbackendv2.Model.transactionModel
import org.example.splitpalbackendv2.Repository.groupRepo
import org.example.splitpalbackendv2.Repository.transactionRepo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import java.time.LocalDateTime
import java.util.Optional

class TimelineServiceTest {

    private lateinit var mockTransactionRepo: transactionRepo
    private lateinit var mockGroupRepo: groupRepo
    private lateinit var timelineService: TimelineService

    @BeforeEach
    fun setUp() {
        mockTransactionRepo = Mockito.mock(transactionRepo::class.java)
        mockGroupRepo = Mockito.mock(groupRepo::class.java)
        timelineService = TimelineService(mockTransactionRepo, mockGroupRepo)
    }


    @Test
    fun `test getTimeline combines P2P and Shared transactions sorted by date`() {
        val currentUserId = ObjectId.get()
        val targetUserId = ObjectId.get()
        val groupId = ObjectId.get()

        val p2pTransaction = transactionModel(
            id = ObjectId.get(),
            userID = currentUserId,
            amount = 100,
            transaction_created_at = LocalDateTime.now().minusHours(2),
            to = null,
            involvedUser = null,
            group_id = null
        )

        val split = splitModel(
            id = ObjectId.get(),
            transactionId = ObjectId.get(),
            SplitAmout = 50,
            payed_by = targetUserId,
            owedBy = currentUserId,
            status = true
        )
        
        val sharedTransaction = transactionModel(
            id = ObjectId.get(),
            userID = targetUserId,
            amount = 200,
            transaction_created_at = LocalDateTime.now().minusHours(1),
            to = null,
            involvedUser = mutableListOf(split),
            group_id = groupId
        )

        val group = groupModel(
            id = groupId,
            name = "Test Group",
            member = mutableListOf(currentUserId, targetUserId)
        )

        Mockito.`when`(mockTransactionRepo.findP2PTransactions(currentUserId, targetUserId))
            .thenReturn(listOf(p2pTransaction))
        Mockito.`when`(mockTransactionRepo.findSharedTransactions(currentUserId, targetUserId))
            .thenReturn(listOf(sharedTransaction))
        Mockito.`when`(mockGroupRepo.findById(groupId)).thenReturn(Optional.of(group))

        val result = timelineService.getTimeline(currentUserId, targetUserId)

        assertEquals(2, result.size)
        // Check sorting: p2p (2 hours ago) should be before shared (1 hour ago)
        assertEquals(p2pTransaction.id, result[0].id)
        assertEquals(sharedTransaction.id, result[1].id)
        
        // Check mapping
        assertEquals(100, result[0].total_amount)
        assertEquals(false, result[0].status) // derived from involvedUser null -> default false
        assertEquals(null, result[0].group_name)
        assertEquals(currentUserId, result[0].payed_by)

        assertEquals(200, result[1].total_amount)
        assertEquals(true, result[1].status) // derived from involvedUser split status
        assertEquals("Test Group", result[1].group_name)
        assertEquals(targetUserId, result[1].payed_by)
    }
}
