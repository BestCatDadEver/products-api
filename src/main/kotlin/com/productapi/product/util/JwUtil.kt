package com.productapi.product.util

import com.productapi.product.model.User
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Date

@Component
class JwtUtil {

    @Value("\${jwt.secret}")
    private lateinit var jwtSecret: String

    @Value("\${jwt.expiration}")
    private val jwtExpirationMs: Long = 86400000 // 1 day default

    fun generateJwtToken(user: User): String { // Assuming you have a User class
        val now = Date()
        val expiryDate = Date(now.time + jwtExpirationMs)

        return Jwts.builder()
            .setSubject(user.username) // Use username as subject
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .claim("userId", user.id) // Add user ID as a claim (optional)
            .signWith(SignatureAlgorithm.HS512, jwtSecret) // Sign with HS512
            .compact()
    }

    // Optional: Add a function to validate the token
    fun validateToken(token: String): Boolean {
        try {
            Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token)
            return true // Token is valid
        } catch (e: Exception) { // Catch various exceptions (expired, invalid signature, etc.)
            return false // Token is invalid
        }
    }


    // Optional: Add a function to extract claims from the token
    fun getClaimsFromToken(token: String): io.jsonwebtoken.Claims? {
        try {
            return Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token).body
        } catch (e: Exception) {
            return null
        }
    }


    // Optional: Add a function to get username from the token
    fun getUsernameFromToken(token: String): String? {
        val claims = getClaimsFromToken(token)
        return claims?.subject
    }

}