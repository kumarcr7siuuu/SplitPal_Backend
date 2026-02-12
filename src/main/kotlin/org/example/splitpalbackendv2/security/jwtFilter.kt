package org.example.splitpalbackendv2.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.bson.types.ObjectId
import org.example.splitpalbackendv2.Repository.userRepo

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.Collections

@Component
class JwtAuthenticationFilter(
    private val tokenService: TokenService,
    private val userRepo: userRepo
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authHeader = request.getHeader("Authorization")

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response)
            return
        }

        val token = authHeader.substring(7)
        val userIdString = tokenService.extractUserID(token)
        println("JwtFilter: Token extracted. UserId: $userIdString")

        if (userIdString != null && SecurityContextHolder.getContext().authentication == null) {
            val userObjectId = try {
                ObjectId(userIdString)
            } catch (e: Exception) {
                println("JwtFilter: Invalid ObjectId format")
                null
            }

            if (userObjectId != null) {
                val userModel = try {
                     userRepo.findById(userObjectId).orElse(null)
                } catch(e: Exception) {
                    println("JwtFilter: DB Error looking up user: ${e.message}")
                    null
                }
                
                if (userModel != null) {
                    println("JwtFilter: User found: ${userModel.phoneNumber}")
                    val customUserDetails = UserDetails(
                        userId = userModel.id!!,
                        password = userModel.password,
                        initialized_at = java.util.Date(),
                        expires_at = java.util.Date()
                    )

                    if (tokenService.isValid(token, customUserDetails)) {
                        println("JwtFilter: Token is valid. Setting context.")
                        val authToken = UsernamePasswordAuthenticationToken(
                            userModel.id, // Storing ObjectId as principal
                            null,
                            Collections.emptyList()
                        )

                        authToken.details = WebAuthenticationDetailsSource().buildDetails(request)
                        SecurityContextHolder.getContext().authentication = authToken
                    }
                }
            }
        }

        filterChain.doFilter(request, response)
    }
}