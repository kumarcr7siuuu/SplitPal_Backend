package org.example.splitpalbackendv2.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.bson.types.ObjectId
import org.example.splitpalbackendv2.config.JwtProperties
import org.springframework.stereotype.Service
import java.util.Date




data class UserDetails(
    val userId: ObjectId,
    val password: String,
    var initialized_at: Date,
    var expires_at: Date
)

@Service
class TokenService(
    private val jwtProperties: JwtProperties
) {

    private val secretKey = Keys.hmacShaKeyFor(
        jwtProperties.key.toByteArray()
    )

    fun generate(
        userDetails: UserDetails,
        expirationDate: Date,
        additionalClaims: Map<String, Any> = emptyMap()
    ): String =
        Jwts.builder()
            .claims()
            .subject(userDetails.userId.toString())
            .issuedAt(Date(System.currentTimeMillis()))
            .expiration(expirationDate)
            .add(additionalClaims)
            .and()
            .signWith(secretKey)
            .compact()

    fun generateAccessToken(userDetails: UserDetails): String {
        return generate(
            userDetails,
            Date(System.currentTimeMillis() + jwtProperties.accessTokenExpiration)
        )
    }

    fun generateRefreshToken(userDetails: UserDetails): String {
        return generate(
            userDetails,
            Date(System.currentTimeMillis() + jwtProperties.refreshTokenExpiration)
        )
    }

    fun isValid(token: String, userDetails: UserDetails): Boolean {
        val userID = extractUserID(token)

        return userDetails.userId.toString() == userID && !isExpired(token)
    }

    fun extractUserID(token: String): String? =
        try {
            getAllClaims(token).subject
        } catch (e: Exception) {
            null
        }

    fun isExpired(token: String): Boolean =
        try {
            getAllClaims(token)
                .expiration
                .before(Date(System.currentTimeMillis()))
        } catch (e: Exception) {
            true
        }

    private fun getAllClaims(token: String): Claims {
        val parser = Jwts.parser()
            .verifyWith(secretKey)
            .build()

        return parser
            .parseSignedClaims(token)
            .payload
    }
}