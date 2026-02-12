# Ktor Client Instruction Manual for SplitPal Backend

This document provides complete instructions for building a **Ktor client** that interacts with the SplitPal Spring Boot backend API.

---

## Table of Contents
1. [Project Setup](#1-project-setup)
2. [Data Models](#2-data-models)
3. [Authentication Flow](#3-authentication-flow)
4. [API Endpoints Reference](#4-api-endpoints-reference)
5. [Client Implementation](#5-client-implementation)
6. [Error Handling](#6-error-handling)
7. [Complete Example](#7-complete-example)
8. [Timeline & Dashboard API](#8-timeline--dashboard-api)

---

## ⚠️ Important Clarifications

> [!CAUTION]
> **Read this section first before implementing the client!**

### Q1: Which is the correct Transaction endpoint?
**Answer: `POST /transaction/create`** ✅

There is **no `/alltransac` endpoint**. The `TransactionController` is mapped to `/transaction`.

### Q2: Is `splitId` still used?
**Answer: NO - `splitId` has been REMOVED**

Use the **embedded `involvedUser: List<SplitModel>`** within each transaction. Do not use `splitId`.

### Q3: How to get transaction history?
**Answer: Use GraphQL, not REST**

| Purpose | Method |
|---------|--------|
| Single transaction details | REST: `GET /transaction/{id}` |
| User transaction history | GraphQL: `timeline(targetUserId)` query |
| Dashboard data | GraphQL: `dashboard()` query |

### Q4: Active Transaction REST Endpoints

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `POST` | `/transaction/create` | Create new transaction |
| `GET` | `/transaction/{id}` | Get transaction details |
| `PATCH` | `/transaction/edit` | Edit transaction metadata |
| `PATCH` | `/transaction/split` | Edit individual split |

### Q5: How to fetch user groups for dashboard?
**Answer: Use `GET /users/allGroups` (REST endpoint)**

> [!IMPORTANT]
> The groups are stored in a **separate `groups` collection**, NOT embedded in the user document. You must call this endpoint to get groups the user is a member of.

#### Endpoint
| Method | Endpoint | Auth | Response |
|--------|----------|------|----------|
| `GET` | `/users/allGroups` | ✅ Bearer Token | `List<GroupModel>` |

#### Response Format
The response is a **JSON array** (not wrapped in an object):
```json
[
  {
    "id": "6947f71111430368d38499d6",
    "name": "Weekend Football",
    "description": "Weekly football games",
    "Admin": "6970ee460ba5c1301f3d6129",
    "created_at": "2025-12-21T13:35:00.000+00:00",
    "member": ["6947e847a693e44a1ff0bb55", "68e8e1009ca981f613153ca2", "6970ee460ba5c1301f3d6129"]
  },
  {
    "id": "6947f71111430368d38499d7",
    "name": "Family Expenses",
    "description": "Groceries and bills",
    "Admin": "6970ee460ba5c1301f3d6129",
    "created_at": "2025-12-21T13:40:00.000+00:00",
    "member": ["6947e847a693e44a1ff0bb55", "68e8e1009ca981f613153ca9", "6970ee460ba5c1301f3d6129"]
  }
]
```

#### Common Client-Side Bugs

> [!CAUTION]
> **Bug #1: Expecting wrapped response**
> ❌ Wrong: `{ "groups": [...] }` or `{ "data": [...] }`
> ✅ Correct: The response IS the array directly `[...]`

> [!CAUTION]
> **Bug #2: Wrong field names**
> ❌ Wrong: `group.groupId`, `group.groupName`
> ✅ Correct: `group.id`, `group.name`

> [!CAUTION]
> **Bug #3: Not displaying fetched data**
> If API returns groups but UI shows empty:
> 1. Check console for parsing errors
> 2. Verify state management is updating correctly
> 3. Ensure UI component is bound to the correct state variable

#### Client Implementation
```kotlin
suspend fun getAllGroups(): List<GroupModel> {
    val response = ApiClient.client.get("/users/allGroups") {
        bearerAuth(ApiClient.getAccessToken() ?: throw UnauthorizedException())
    }
    return response.body()  // Returns List<GroupModel> directly
}
```

---

## 1. Project Setup

### Dependencies (build.gradle.kts)

```kotlin
plugins {
    kotlin("jvm") version "1.9.21"
    kotlin("plugin.serialization") version "1.9.21"
}

dependencies {
    // Ktor Client
    implementation("io.ktor:ktor-client-core:2.3.7")
    implementation("io.ktor:ktor-client-cio:2.3.7")  // CIO engine for JVM
    implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
    implementation("io.ktor:ktor-client-auth:2.3.7")
    implementation("io.ktor:ktor-client-logging:2.3.7")
    
    // Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
}
```

---

## 2. Data Models

### 2.1 User Models

```kotlin
import kotlinx.serialization.Serializable
import kotlinx.datetime.LocalDateTime

// === AUTH REQUESTS ===

@Serializable
data class FirstTimeUserLoginRequest(
    val userName: String,
    val phoneNumber: String,
    val password: String
)

@Serializable
data class LoginUserRequest(
    val userName: String, // Kept for backwards compatibility if needed, else remove
    val phoneNumber: String,
    val password: String
)

@Serializable
data class RefreshTokenRequest(
    val refreshToken: String
)

// === AUTH RESPONSES ===

@Serializable
data class LoginUserResponse(
    val userName: String,
    val phoneNumber: String,
    val created_at: String,  // ISO-8601 datetime string
    val secureToken: String? = null,
    val refreshToken: String? = null
)

@Serializable
data class UserContact(
    val id: String,
    val name: String
)

@Serializable
data class UserModel(
    val id: String? = null,
    val userName: String,
    val password: String,
    val phoneNumber: String,
    val secureToken: String? = null,
    val refreshToken: String? = null,
    val groups: List<GroupModel>? = null,
    val updated_at: String,
    val created_at: String
)
```

### 2.2 Group Models

```kotlin
@Serializable
data class GroupModel(
    val id: String? = null,
    val name: String,
    val description: String? = null,
    val Admin: String? = null,
    val created_at: String? = null,
    val member: List<String>  // List of ObjectId strings
)

@Serializable
data class Group(
    val name: String,
    val description: String? = null
)

@Serializable
data class CreateGroupRequest(
    val group: Group,
    val members: List<String>  // List of ObjectId strings
)

@Serializable
data class GroupData(
    val id: String,
    val name: String
)
```

### 2.3 Transaction Models

```kotlin
@Serializable
data class TransactionModel(
    val id: String? = null,
    val group_id: String? = null,
    val userID: String,
    val amount: Int,
    val transaction_created_at: String? = null,
    val description: String? = null,
    val to: PayedTo? = null,
    val involvedUser: List<SplitModel>? = null
)

@Serializable
data class PayedTo(
    val name: String,
    val id: String? = null,
    val upi_Id: String? = null
)

@Serializable
data class CreateTransactionRequest(
    val amount: Int,
    val description: String? = null,
    val to: PayedTo? = null,
    val group_id: String? = null,
    val involvedUser: List<SplitModel>? = null
)

@Serializable
data class EditTransactionRequest(
    val transaction_id: String,
    val amount: Int? = null,
    val description: String? = null,
    val to: PayedTo? = null,
    val group_id: String? = null,
    val involvedUser: List<SplitModel>? = null
)

@Serializable
data class EditSplitRequest(
    val transaction_id: String,
    val split_id: String,
    val amount: Int? = null,
    val status: Boolean? = null
)
```

### 2.4 Split Models

```kotlin
@Serializable
data class SplitModel(
    val id: String? = null,
    val SplitAmout: Int,
    val payed_by: String,  // ObjectId as string
    val owedBy: String,    // ObjectId as string
    val status: Boolean = false,
    val settled_date: String? = null
)
```

### 2.5 Transaction DTOs (Response Models)

```kotlin
@Serializable
data class TransactionListItem(
    val id: String,
    val amount: Int,
    val description: String?,
    val payed_by: String,
    val date: String?,
    val splitCount: Int,
    val group_name: String?
)

@Serializable
data class TransactionDetailResponse(
    val id: String,
    val amount: Int,
    val description: String?,
    val payed_by: PayerInfo,
    val date: String?,
    val group_name: String?,
    val splits: List<SplitDetail>
)

@Serializable
data class PayerInfo(
    val id: String,
    val name: String,
    val upi_id: String?
)

@Serializable
data class SplitDetail(
    val user_id: String,
    val user_name: String,
    val amount: Int,
    val status: Boolean,
    val settled_date: String?
)
```

---

## 3. Authentication Flow

The backend uses **JWT Bearer Token** authentication:

```
┌─────────────┐     POST /auth/signup      ┌─────────────┐
│   Client    │ ───────────────────────────▶│   Backend   │
│             │◀─────────────────────────── │             │
│             │    UserModel (with tokens)  │             │
├─────────────┤                             ├─────────────┤
│             │     POST /auth/login        │             │
│             │ ───────────────────────────▶│             │
│             │◀─────────────────────────── │             │
│             │    LoginUserResponse        │             │
│             │    (secureToken,            │             │
│             │     refreshToken)           │             │
├─────────────┤                             ├─────────────┤
│             │    GET /protected-route     │             │
│             │    Authorization: Bearer    │             │
│             │    <secureToken>            │             │
│             │ ───────────────────────────▶│             │
│             │◀─────────────────────────── │             │
└─────────────┘                             └─────────────┘
```

### Token Usage

| Token | Purpose | Header Format |
|-------|---------|---------------|
| `secureToken` | Access Token for API calls | `Authorization: Bearer <token>` |
| `refreshToken` | Refresh expired access tokens | POST body to `/auth/refresh` |

---

## 4. API Endpoints Reference

### 4.1 Auth Endpoints (`/auth`)

| Method | Endpoint | Auth | Request Body | Response |
|--------|----------|------|--------------|----------|
| `POST` | `/auth/signup` | ❌ | `FirstTimeUserLoginRequest` | `UserModel` |
| `POST` | `/auth/login` | ❌ | `LoginUserRequest` | `LoginUserResponse` |
| `POST` | `/auth/refresh` | ❌ | `RefreshTokenRequest` | `LoginUserResponse` |
| `GET` | `/auth/validate` | ✅ | - | `LoginUserResponse` |

### 4.2 User Endpoints (`/users`)

| Method | Endpoint | Auth | Request Body | Response |
|--------|----------|------|--------------|----------|
| `GET` | `/users/demo` | ❌ | - | `String` |
| `POST` | `/users/signup` | ❌ | `FirstTimeUserLoginRequest` | `UserModel` |
| `POST` | `/users/login` | ❌ | `LoginUserRequest` | `LoginUserResponse` |
| `GET` | `/users/contacts` | ✅ | - | `List<UserContact>` |
| `GET` | `/users/allGroups` | ✅ | - | `List<GroupModel>` |

### 4.3 Group Endpoints (`/groups`)

| Method | Endpoint | Auth | Request Body | Response |
|--------|----------|------|--------------|----------|
| `POST` | `/groups/newgroup` | ✅ | `CreateGroupRequest` | `GroupModel` |

### 4.4 Transaction Endpoints (`/transaction`)

| Method | Endpoint | Auth | Request Body | Response |
|--------|----------|------|--------------|----------|
| `GET` | `/transaction/{transactionId}` | ✅ | - | `TransactionDetailResponse` |
| `POST` | `/transaction/create` | ✅ | `CreateTransactionRequest` | `TransactionModel` |
| `PATCH` | `/transaction/edit` | ✅ | `EditTransactionRequest` | `TransactionModel` |
| `PATCH` | `/transaction/split` | ✅ | `EditSplitRequest` | `TransactionModel` |

### 4.5 Timeline Endpoints (`/timeline`)

| Method | Endpoint | Auth | Query Params | Response |
|--------|----------|------|--------------|----------|
| `GET` | `/timeline/{targetUserId}` | ✅ | `page` (0), `size` (20) | `PaginatedTimelineResponse` |
| `GET` | `/timeline/group/{groupId}` | ✅ | `page` (0), `size` (20) | `PaginatedTimelineResponse` |

> [!TIP]
> Both endpoints return **20 transactions per page** by default. Use `page` for infinite scroll.

---

## 5. Client Implementation

### 5.1 HttpClient Configuration

```kotlin
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object ApiClient {
    private const val BASE_URL = "http://localhost:8080"  // Change for production
    
    private var accessToken: String? = null
    private var refreshTokenValue: String? = null
    
    val client = HttpClient(CIO) {
        // JSON Serialization
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
                encodeDefaults = true
            })
        }
        
        // Logging (optional, for debugging)
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.HEADERS
        }
        
        // Default Request Configuration
        defaultRequest {
            url(BASE_URL)
            contentType(ContentType.Application.Json)
        }
        
        // Response Validation
        expectSuccess = true
        
        HttpResponseValidator {
            handleResponseExceptionWithRequest { exception, _ ->
                when (exception) {
                    is ClientRequestException -> {
                        val response = exception.response
                        when (response.status) {
                            HttpStatusCode.Unauthorized -> throw UnauthorizedException()
                            HttpStatusCode.NotFound -> throw NotFoundException()
                            else -> throw ApiException(response.status.value)
                        }
                    }
                }
            }
        }
    }
    
    fun setTokens(access: String?, refresh: String?) {
        accessToken = access
        refreshTokenValue = refresh
    }
    
    fun getAccessToken() = accessToken
    fun getRefreshToken() = refreshTokenValue
    fun clearTokens() {
        accessToken = null
        refreshTokenValue = null
    }
}

// Custom Exceptions
class UnauthorizedException : Exception("Unauthorized")
class NotFoundException : Exception("Resource not found")
class ApiException(code: Int) : Exception("API Error: $code")
```

### 5.2 Auth Service

```kotlin
import io.ktor.client.call.*
import io.ktor.client.request.*

object AuthService {
    
    suspend fun signup(userName: String, phoneNumber: String, password: String): UserModel {
        val response = ApiClient.client.post("/auth/signup") {
            setBody(FirstTimeUserLoginRequest(userName, phoneNumber, password))
        }
        val user = response.body<UserModel>()
        ApiClient.setTokens(user.secureToken, user.refreshToken)
        return user
    }
    
    suspend fun login(userName: String, phoneNumber: String, password: String): LoginUserResponse {
        val response = ApiClient.client.post("/auth/login") {
            setBody(LoginUserRequest(userName, phoneNumber, password))
        }
        val loginResponse = response.body<LoginUserResponse>()
        ApiClient.setTokens(loginResponse.secureToken, loginResponse.refreshToken)
        return loginResponse
    }
    
    suspend fun refreshToken(): LoginUserResponse {
        val currentRefreshToken = ApiClient.getRefreshToken()
            ?: throw IllegalStateException("No refresh token available")
        
        val response = ApiClient.client.post("/auth/refresh") {
            setBody(RefreshTokenRequest(currentRefreshToken))
        }
        val loginResponse = response.body<LoginUserResponse>()
        ApiClient.setTokens(loginResponse.secureToken, loginResponse.refreshToken)
        return loginResponse
    }
    
    suspend fun validate(): LoginUserResponse {
        val response = ApiClient.client.get("/auth/validate") {
            bearerAuth(ApiClient.getAccessToken() ?: throw UnauthorizedException())
        }
        return response.body()
    }
    
    fun logout() {
        ApiClient.clearTokens()
    }
}
```

### 5.3 User Service

```kotlin
object UserService {
    
    suspend fun getAllGroups(): List<GroupModel> {
        val response = ApiClient.client.get("/users/allGroups") {
            bearerAuth(ApiClient.getAccessToken() ?: throw UnauthorizedException())
        }
        return response.body()
    }
    
    suspend fun getContacts(): List<UserContact> {
        val response = ApiClient.client.get("/users/contacts") {
            bearerAuth(ApiClient.getAccessToken() ?: throw UnauthorizedException())
        }
        return response.body()
    }
}
```

### 5.4 Group Service

```kotlin
object GroupService {
    
    suspend fun createGroup(
        name: String, 
        description: String? = null, 
        members: List<String>
    ): GroupModel {
        val response = ApiClient.client.post("/groups/newgroup") {
            bearerAuth(ApiClient.getAccessToken() ?: throw UnauthorizedException())
            setBody(CreateGroupRequest(
                group = Group(name, description),
                members = members
            ))
        }
        return response.body()
    }
}
```

### 5.5 Transaction Service

```kotlin
object TransactionService {
    
    suspend fun getTransactionDetails(transactionId: String): TransactionDetailResponse {
        val response = ApiClient.client.get("/transaction/$transactionId") {
            bearerAuth(ApiClient.getAccessToken() ?: throw UnauthorizedException())
        }
        return response.body()
    }
    
    suspend fun createTransaction(request: CreateTransactionRequest): TransactionModel {
        val response = ApiClient.client.post("/transaction/create") {
            bearerAuth(ApiClient.getAccessToken() ?: throw UnauthorizedException())
            setBody(request)
        }
        return response.body()
    }
    
    suspend fun editTransaction(request: EditTransactionRequest): TransactionModel {
        val response = ApiClient.client.patch("/transaction/edit") {
            bearerAuth(ApiClient.getAccessToken() ?: throw UnauthorizedException())
            setBody(request)
        }
        return response.body()
    }
    
    suspend fun editSplit(request: EditSplitRequest): TransactionModel {
        val response = ApiClient.client.patch("/transaction/split") {
            bearerAuth(ApiClient.getAccessToken() ?: throw UnauthorizedException())
            setBody(request)
        }
        return response.body()
    }
}
```

---

## 6. Error Handling

### 6.1 Result Wrapper Pattern

```kotlin
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: Int? = null) : ApiResult<Nothing>()
    data object Loading : ApiResult<Nothing>()
}

suspend fun <T> safeApiCall(apiCall: suspend () -> T): ApiResult<T> {
    return try {
        ApiResult.Success(apiCall())
    } catch (e: UnauthorizedException) {
        ApiResult.Error("Unauthorized - Please login again", 401)
    } catch (e: NotFoundException) {
        ApiResult.Error("Resource not found", 404)
    } catch (e: ApiException) {
        ApiResult.Error("API Error", null)
    } catch (e: Exception) {
        ApiResult.Error(e.message ?: "Unknown error occurred")
    }
}
```

### 6.2 Automatic Token Refresh (Advanced)

```kotlin
suspend fun <T> authenticatedCall(
    retryOnUnauthorized: Boolean = true,
    apiCall: suspend () -> T
): T {
    return try {
        apiCall()
    } catch (e: UnauthorizedException) {
        if (retryOnUnauthorized) {
            // Try to refresh token
            AuthService.refreshToken()
            // Retry the call
            apiCall()
        } else {
            throw e
        }
    }
}
```

---

## 7. Complete Example

```kotlin
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    // 1. Login
    val loginResult = safeApiCall {
        AuthService.login(
            userName = "testuser",
            phoneNumber = "1234567890",
            password = "password123"
        )
    }
    
    when (loginResult) {
        is ApiResult.Success -> {
            println("✅ Logged in as: ${loginResult.data.userName}")
            println("   Access Token: ${loginResult.data.secureToken?.take(20)}...")
        }
        is ApiResult.Error -> {
            println("❌ Login failed: ${loginResult.message}")
            return@runBlocking
        }
        ApiResult.Loading -> {}
    }
    
    // 2. Get User Groups
    val groupsResult = safeApiCall {
        UserService.getAllGroups()
    }
    
    when (groupsResult) {
        is ApiResult.Success -> {
            println("📁 User Groups (${groupsResult.data.size}):")
            groupsResult.data.forEach { group ->
                println("   - ${group.name} (${group.member.size} members)")
            }
        }
        is ApiResult.Error -> println("❌ Failed to fetch groups: ${groupsResult.message}")
        ApiResult.Loading -> {}
    }
    
    // 3. Create a Transaction
    val transactionResult = safeApiCall {
        TransactionService.createTransaction(
            CreateTransactionRequest(
                amount = 300,
                description = "Dinner at Restaurant",
                to = PayedTo(name = "Restaurant XYZ"),
                involvedUser = listOf(
                    SplitModel(
                        SplitAmout = 150,
                        payed_by = "your-user-id",
                        owedBy = "friend-user-id",
                        status = false
                    )
                )
            )
        )
    }
    
    when (transactionResult) {
        is ApiResult.Success -> {
            println("💰 Transaction created: ${transactionResult.data.id}")
        }
        is ApiResult.Error -> println("❌ Failed: ${transactionResult.message}")
        ApiResult.Loading -> {}
    }
    
    // 4. Cleanup
    ApiClient.client.close()
}
```

---

## 8. Timeline & Dashboard API

The backend provides **two ways** to access timeline/chat data:
1. **REST API** (Recommended) - Paginated, fetches 20 items at a time
2. **GraphQL** - For dashboard data

---

### 8.1 Timeline REST API (Paginated)

> [!IMPORTANT]
> **Use the REST endpoint for timeline/chat history** - it supports pagination with 20 items per page by default.

#### Endpoint

| Method | Endpoint | Auth | Query Params |
|--------|----------|------|--------------|
| `GET` | `/timeline/{targetUserId}` | ✅ | `page` (default: 0), `size` (default: 20) |

#### Response Model

```kotlin
@Serializable
data class PaginatedTimelineResponse(
    val content: List<TimelineChat>,
    val currentPage: Int,
    val totalPages: Int,
    val totalItems: Long,
    val hasMore: Boolean
)

@Serializable
data class TimelineChat(
    val id: String? = null,
    val total_amount: Int,
    val status: Boolean,
    val group_name: String? = null,
    val group_id: String? = null,
    val payed_by: String
)
```

#### Timeline Service Implementation

```kotlin
object TimelineService {
    
    /**
     * Fetch paginated chat/transaction history with a target user
     * @param targetUserId - The user to get chat history with
     * @param page - Page number (0-indexed), default 0
     * @param size - Items per page, default 20
     */
    suspend fun getTimeline(
        targetUserId: String,
        page: Int = 0,
        size: Int = 20
    ): PaginatedTimelineResponse {
        val response = ApiClient.client.get("/timeline/$targetUserId") {
            bearerAuth(ApiClient.getAccessToken() ?: throw UnauthorizedException())
            parameter("page", page)
            parameter("size", size)
        }
        return response.body()
    }
    
    /**
     * Fetch first page of timeline (20 most recent transactions)
     */
    suspend fun getTimelineFirstPage(targetUserId: String): PaginatedTimelineResponse {
        return getTimeline(targetUserId, page = 0, size = 20)
    }
    
    /**
     * Load more timeline items (next page)
     */
    suspend fun loadMoreTimeline(
        targetUserId: String, 
        currentPage: Int
    ): PaginatedTimelineResponse {
        return getTimeline(targetUserId, page = currentPage + 1, size = 20)
    }
}
```

#### Usage Example - Infinite Scroll Pattern

```kotlin
class TimelineViewModel {
    private var currentPage = 0
    private var hasMore = true
    private val _timeline = mutableListOf<TimelineChat>()
    
    // Initial load - fetch first 20 transactions
    suspend fun loadInitialTimeline(targetUserId: String) {
        val result = safeApiCall {
            TimelineService.getTimelineFirstPage(targetUserId)
        }
        
        when (result) {
            is ApiResult.Success -> {
                _timeline.clear()
                _timeline.addAll(result.data.content)
                currentPage = result.data.currentPage
                hasMore = result.data.hasMore
                
                println("📱 Loaded ${result.data.content.size} chats")
                println("   Total: ${result.data.totalItems} | Pages: ${result.data.totalPages}")
            }
            is ApiResult.Error -> println("❌ Error: ${result.message}")
            ApiResult.Loading -> {}
        }
    }
    
    // Load more when user scrolls to bottom
    suspend fun loadMore(targetUserId: String) {
        if (!hasMore) {
            println("No more items to load")
            return
        }
        
        val result = safeApiCall {
            TimelineService.loadMoreTimeline(targetUserId, currentPage)
        }
        
        when (result) {
            is ApiResult.Success -> {
                _timeline.addAll(result.data.content)
                currentPage = result.data.currentPage
                hasMore = result.data.hasMore
                
                println("📱 Loaded ${result.data.content.size} more chats")
            }
            is ApiResult.Error -> println("❌ Error: ${result.message}")
            ApiResult.Loading -> {}
        }
    }
}
```

---

### 8.2 Group Timeline REST API (Paginated)

> [!NOTE]
> Use this endpoint to fetch transaction history for a specific **group**.

#### Endpoint

| Method | Endpoint | Auth | Query Params |
|--------|----------|------|--------------|
| `GET` | `/timeline/group/{groupId}` | ✅ | `page` (default: 0), `size` (default: 20) |

#### Group Timeline Service

```kotlin
object GroupTimelineService {
    
    /**
     * Fetch paginated group transaction history
     * @param groupId - The group to get transaction history for
     * @param page - Page number (0-indexed), default 0
     * @param size - Items per page, default 20
     */
    suspend fun getGroupTimeline(
        groupId: String,
        page: Int = 0,
        size: Int = 20
    ): PaginatedTimelineResponse {
        val response = ApiClient.client.get("/timeline/group/$groupId") {
            bearerAuth(ApiClient.getAccessToken() ?: throw UnauthorizedException())
            parameter("page", page)
            parameter("size", size)
        }
        return response.body()
    }
    
    suspend fun getGroupTimelineFirstPage(groupId: String): PaginatedTimelineResponse {
        return getGroupTimeline(groupId, page = 0, size = 20)
    }
    
    suspend fun loadMoreGroupTimeline(groupId: String, currentPage: Int): PaginatedTimelineResponse {
        return getGroupTimeline(groupId, page = currentPage + 1, size = 20)
    }
}
```

---

### 8.3 Dashboard (GraphQL)

The dashboard data is accessed via **GraphQL** at `/graphql`.

#### Dashboard Models

```kotlin
@Serializable
data class DashboardScreen(
    val FlatcardList: List<FlatCard>,
    val groupList: List<GroupInfo>,
    val individuals: List<IndividualTransactionPeople>,
    val recent_transaction: List<RecentTransaction>
)

@Serializable
data class FlatCard(
    val transaction_id: String?,
    val group_name: String?,
    val Initiator_name: String,
    val Amount: Int
)

@Serializable
data class GroupInfo(
    val id: String?,
    val name: String?
)

@Serializable
data class IndividualTransactionPeople(
    val target_user: String?,
    val target_user_name: String?
)

@Serializable
data class RecentTransaction(
    val transaction_id: String?,
    val payed_to_name: String?,
    val group_name: String?,
    val amount: Int,
    val status: Status
)

@Serializable
enum class Status {
    DEBITED,
    CREDITED
}

// GraphQL helpers
@Serializable
data class GraphQLRequest(
    val query: String,
    val variables: Map<String, String>? = null
)

@Serializable
data class GraphQLResponse<T>(
    val data: T?,
    val errors: List<GraphQLError>? = null
)

@Serializable
data class GraphQLError(val message: String)

@Serializable
data class DashboardData(val dashboard: DashboardScreen)
```

#### Dashboard Service

```kotlin
object DashboardService {
    
    private val DASHBOARD_QUERY = """
        query Dashboard {
            dashboard {
                FlatcardList { transaction_id, group_name, Initiator_name, Amount }
                groupList { id, name }
                individuals { target_user, target_user_name }
                recent_transaction { transaction_id, payed_to_name, group_name, amount, status }
            }
        }
    """.trimIndent()
    
    suspend fun getDashboard(): DashboardScreen {
        val response = ApiClient.client.post("/graphql") {
            bearerAuth(ApiClient.getAccessToken() ?: throw UnauthorizedException())
            setBody(GraphQLRequest(query = DASHBOARD_QUERY))
        }
        
        val result = response.body<GraphQLResponse<DashboardData>>()
        
        if (result.errors?.isNotEmpty() == true) {
            throw Exception("GraphQL Error: ${result.errors.first().message}")
        }
        
        return result.data?.dashboard ?: throw Exception("No dashboard data")
    }
}
```

---

### 8.4 API Quick Reference

| Endpoint | Method | Purpose | Pagination |
|----------|--------|---------|------------|
| `/timeline/{targetUserId}` | REST GET | P2P chat history | ✅ 20/page |
| `/timeline/group/{groupId}` | REST GET | Group chat history | ✅ 20/page |
| `/graphql` (dashboard) | GraphQL | Dashboard data | ❌ |

| Dashboard Field | Count | Description |
|-----------------|-------|-------------|
| `FlatcardList` | 5 | Transactions where user owes money |
| `groupList` | 3 | Most recent groups |
| `individuals` | 4 | Frequently paid users |
| `recent_transaction` | 5 | Most recent transactions |

---

## Important Notes

> [!IMPORTANT]
> - All ObjectId fields from the backend are serialized as **strings** in JSON
> - Date/time fields are returned as **ISO-8601 formatted strings**
> - Always handle `401 Unauthorized` errors by refreshing the token

> [!WARNING]
> - Store tokens securely (e.g., encrypted shared preferences on Android, Keychain on iOS)
> - Never log or expose tokens in production

> [!TIP]
> For Android/KMM projects, consider using `ktor-client-okhttp` instead of CIO for better Android compatibility:
> ```kotlin
> implementation("io.ktor:ktor-client-okhttp:2.3.7")
> ```

---

## File Structure Recommendation

```
src/
├── api/
│   ├── ApiClient.kt          # HttpClient configuration
│   ├── models/
│   │   ├── AuthModels.kt     # User, Login requests/responses
│   │   ├── GroupModels.kt    # Group-related models
│   │   ├── TransactionModels.kt
│   │   ├── SplitModels.kt
│   │   ├── GraphQLModels.kt  # Timeline, Dashboard models
│   │   └── DashboardModels.kt
│   └── services/
│       ├── AuthService.kt
│       ├── UserService.kt
│       ├── GroupService.kt
│       ├── TransactionService.kt
│       └── GraphQLService.kt  # Timeline & Dashboard queries
└── utils/
    ├── ApiResult.kt          # Result wrapper
    └── TokenManager.kt       # Token storage/refresh logic
```

