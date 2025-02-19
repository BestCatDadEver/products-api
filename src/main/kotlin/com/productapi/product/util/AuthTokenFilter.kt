package com.productapi.product.util

import io.jsonwebtoken.ExpiredJwtException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.security.web.util.matcher.AntPathRequestMatcher
import org.springframework.stereotype.Component
import org.springframework.util.StringUtils
import org.springframework.web.filter.OncePerRequestFilter
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
class AuthTokenFilter : OncePerRequestFilter() {

    private val logger = LoggerFactory.getLogger(AuthTokenFilter::class.java)

    @Autowired
    private lateinit var jwtUtil: JwtUtil

    @Autowired
    private lateinit var userDetailsService: UserDetailsService

    private val excludedMatchers = arrayOf(
        AntPathRequestMatcher("/api/auth/**", "POST") // Exclude POST requests to /api/auth/** (registration)
        // Add other paths to exclude as needed
    )

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {

        // Check if the request matches any excluded paths
        for (matcher in excludedMatchers) {
            if (matcher.matches(request)) {
                filterChain.doFilter(request, response) // Skip filtering for excluded paths
                return // Important: Exit the filter early
            }
        }

        // If the request is not excluded, then apply the JWT logic:
        try {
            val jwt = parseJwt(request)
            if (jwt != null && jwtUtil.validateToken(jwt)) {
                val username = jwtUtil.getUsernameFromToken(jwt)

                val userDetails: UserDetails = userDetailsService.loadUserByUsername(username)
                val authentication = UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.authorities
                )
                authentication.details = WebAuthenticationDetailsSource().buildDetails(request)

                SecurityContextHolder.getContext().authentication = authentication
            } else {
                // Handle invalid token or missing token
                response.status = HttpServletResponse.SC_UNAUTHORIZED  // 401 Unauthorized
                response.contentType = "application/json"
                response.writer.write("""{"message": "Invalid or missing token"}""") // Or a more structured error response
                return // Stop the filter chain
            }
        } catch (e: ExpiredJwtException) { // Example: Catch specific JWT exception
            response.status = HttpServletResponse.SC_UNAUTHORIZED
            response.contentType = "application/json"
            response.writer.write("""{"message": "Token expired"}""")
            return
        } catch (e: org.springframework.security.core.userdetails.UsernameNotFoundException) { // Catch user not found
            response.status = HttpServletResponse.SC_UNAUTHORIZED
            response.contentType = "application/json"
            response.writer.write("""{"message": "User not found"}""")
            return
        } catch (e: org.springframework.security.core.AuthenticationException) { // Generic Authentication Exception
            response.status = HttpServletResponse.SC_UNAUTHORIZED
            response.contentType = "application/json"
            response.writer.write("""{"message": "Authentication failed"}""")
            return
        } catch (e: Exception) { // Catch any other exceptions
            logger.error("Authentication error: {}", e) // Log the full exception
            response.status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR // 500 Internal Server Error
            response.contentType = "application/json"
            response.writer.write("""{"message": "Authentication error"}""")
            return
        }

        filterChain.doFilter(request, response)
    }

    private fun parseJwt(request: HttpServletRequest): String? {
        val authHeader = request.getHeader("Authorization")

        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7)
        }

        return null
    }
}