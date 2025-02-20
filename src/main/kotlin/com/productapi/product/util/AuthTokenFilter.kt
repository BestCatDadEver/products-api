package com.productapi.product.util

import io.jsonwebtoken.ExpiredJwtException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.security.web.util.matcher.AntPathRequestMatcher
import org.springframework.stereotype.Component
import org.springframework.util.StringUtils
import org.springframework.web.filter.OncePerRequestFilter
import javax.naming.AuthenticationException
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
        val requestURI = request.requestURI

        // Skip filtering for permitted endpoints (adjust paths as needed)
        if (requestURI.startsWith("/api/auth/")) { // Include /api/login
            filterChain.doFilter(request, response)
            return // VERY IMPORTANT: Exit the filter early
        }

        logger.info("AuthTokenFilter is being executed for URI: $requestURI") // Log the URI

        try {
            val jwt = parseJwt(request)  // Extract the JWT

            if (jwt!= null && jwtUtil.validateToken(jwt)) {
                val username = jwtUtil.getUsernameFromToken(jwt)

                val userDetails: UserDetails = userDetailsService.loadUserByUsername(username)
                val authentication = UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.authorities
                )
                authentication.details = WebAuthenticationDetailsSource().buildDetails(request)

                SecurityContextHolder.getContext().authentication = authentication
                logger.info("Authentication successful for user: $username") // Log success

            } else {
                // Handle invalid or missing token
                logger.warn("Invalid or missing token for URI: $requestURI") // Log the error
                response.status = HttpServletResponse.SC_UNAUTHORIZED
                response.contentType = "application/json"
                response.writer.write("""{"message": "Invalid or missing token"}""")
                return // Stop the filter chain
            }

        } catch (e: ExpiredJwtException) {
            logger.warn("Token expired for URI: $requestURI", e) // Log with exception details
            response.status = HttpServletResponse.SC_UNAUTHORIZED
            response.contentType = "application/json"
            response.writer.write("""{"message": "Token expired"}""")
            return
        } catch (e: UsernameNotFoundException) {
            logger.warn("User not found for URI: $requestURI", e)
            response.status = HttpServletResponse.SC_UNAUTHORIZED
            response.contentType = "application/json"
            response.writer.write("""{"message": "User not found"}""")
            return
        } catch (e: AuthenticationException) {
            logger.warn("Authentication failed for URI: $requestURI", e)
            response.status = HttpServletResponse.SC_UNAUTHORIZED
            response.contentType = "application/json"
            response.writer.write("""{"message": "Authentication failed"}""")
            return
        } catch (e: Exception) {
            logger.error("Authentication error for URI: $requestURI", e) // Log the full exception
            response.status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR
            response.contentType = "application/json"
            response.writer.write("""{"message": "Authentication error"}""")
            return
        }

        filterChain.doFilter(request, response) // Continue if authentication is successful
    }

    private fun parseJwt(request: HttpServletRequest): String? {
        val authHeader = request.getHeader("Authorization")

        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7)
        }

        return null
    }
}


@Component // Make sure it's a component
class TestFilter : OncePerRequestFilter() {
    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        logger.info("TestFilter is being executed") // Log at the very start

        filterChain.doFilter(request, response) // Allow the request to continue *unconditionally*
    }
}