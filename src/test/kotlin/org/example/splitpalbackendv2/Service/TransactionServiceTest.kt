package org.example.splitpalbackendv2.Service

import org.bson.types.ObjectId
import org.example.splitpalbackendv2.Model.CreateTransactionRequest
import org.example.splitpalbackendv2.Model.payedTo
import org.example.splitpalbackendv2.Model.transactionModel
import org.example.splitpalbackendv2.Repository.transactionRepo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito

class TransactionServiceTest {

    private lateinit var mockTransactionRepo: transactionRepo
    private lateinit var transactionService: transactionService

    @BeforeEach
    fun setUp() {
        mockTransactionRepo = Mockito.mock(transactionRepo::class.java)
        transactionService = transactionService(mockTransactionRepo)
    }

    @Test
    fun `createTransaction saves transaction with correct data`() {
        val userId = ObjectId.get()
        val targetId = ObjectId.get()
        val request = CreateTransactionRequest(
            amount = 500,
            description = "Dinner",
            to = payedTo(name = "Friend", id = targetId)
        )

        val savedTransaction = transactionModel(
            id = ObjectId.get(),
            userID = userId,
            amount = request.amount,
            description = request.description,
            to = request.to
        )

        Mockito.`when`(mockTransactionRepo.save(any(transactionModel::class.java))).thenReturn(savedTransaction)

        val result = transactionService.createTransaction(userId, request)

        assertEquals(userId, result.userID)
        assertEquals(500, result.amount)
        assertEquals("Dinner", result.description)
        assertEquals(targetId, result.to?.id)
    }
}
