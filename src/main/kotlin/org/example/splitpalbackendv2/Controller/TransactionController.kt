package org.example.splitpalbackendv2.Controller

import org.bson.types.ObjectId
import org.example.splitpalbackendv2.Model.CreateTransactionRequest
import org.example.splitpalbackendv2.Model.EditTransactionRequest
import org.example.splitpalbackendv2.Model.EditSplitRequest
import org.example.splitpalbackendv2.Model.TransactionDetailResponse
import org.example.splitpalbackendv2.Model.TransactionResponseDTO
import org.example.splitpalbackendv2.Model.transactionModel
import org.example.splitpalbackendv2.Service.transactionService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/transaction")
class TransactionController(
    private val transactionService: transactionService
) {

    @GetMapping("/{transactionId}")
    fun getTransactionDetails(
        @PathVariable transactionId: String,
//        authentication: Authentication
    ): ResponseEntity<TransactionDetailResponse> {
        val response = transactionService.getTransactionDetails(ObjectId(transactionId))
        return ResponseEntity.ok(response)
    }

    @PostMapping("/create")
    fun createTransaction(@RequestBody request: CreateTransactionRequest, authentication: Authentication): ResponseEntity<TransactionResponseDTO> {
        val userId: ObjectId = authentication.principal as ObjectId
        val transaction = transactionService.createTransaction(userId, request)
        return ResponseEntity.ok(transaction)
    }

    @PatchMapping("/edit")
    fun editTransaction(@RequestBody request: EditTransactionRequest, authentication: Authentication): ResponseEntity<TransactionResponseDTO> {
        val userId: ObjectId = authentication.principal as ObjectId
        val response = transactionService.editTransaction(userId, request)
        return ResponseEntity.ok(response)
    }

    @PatchMapping("/split")
    fun editSplit(@RequestBody request: EditSplitRequest, authentication: Authentication): ResponseEntity<TransactionResponseDTO> {
        val userId: ObjectId = authentication.principal as ObjectId
        val response = transactionService.editSplit(userId, request)
        return ResponseEntity.ok(response)
    }

}
