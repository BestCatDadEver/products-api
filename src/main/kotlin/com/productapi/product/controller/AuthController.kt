package com.productapi.product.controller

import com.productapi.product.model.User
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.jdbc.core.JdbcTemplate  // For calling stored procedures
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*
import java.sql.Types // Import for SQL data types

@RestController
@RequestMapping("/api/auth")
class AuthController {

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate // Inject JdbcTemplate

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

// Ensure this bean is configured in your application (e.g., a @Configuration class, if not already defined)

    @PostMapping("/register")
    fun registerUser(@RequestBody registrationRequest: Map<String, String>): ResponseEntity<*> {
        try {
            val username = registrationRequest["username"]
            val password = registrationRequest["password"]
            val email = registrationRequest["email"]
            val firstName = registrationRequest["firstName"]
            val lastName = registrationRequest["lastName"]
            //... other fields

            if (username.isNullOrBlank() || password.isNullOrBlank() || email.isNullOrBlank() || firstName.isNullOrBlank() || lastName.isNullOrBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("All fields are required.")
            }

            // Call the stored procedure
            val result = jdbcTemplate.update(
                "EXEC RegisterUser?,?,?,?,?,?", // Stored procedure call
                username,
                password, // Password will be hashed inside the stored procedure
                java.sql.Date(java.util.Date().time), // Convert to java.sql.Date for DateOfBirth
                email,
                firstName,
                lastName
                //... other parameters
            )

            if (result > 0) {
                return ResponseEntity.ok("User registered successfully.")
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Registration failed.")
            }


        } catch (e: Exception) {
            e.printStackTrace()
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Registration failed: ${e.message}")
        }
    }

    @PostMapping("/login")
    fun loginUser(@RequestBody loginRequest: Map<String, String>): ResponseEntity<*> {
        try {
            val username = loginRequest["username"]
            val password = loginRequest["password"]

            val userDetails = jdbcTemplate.query(
                "EXEC LoginUser?,?",
                username,
                password,
                { rs, _ -> // RowMapper to extract user details
                    User( // Assuming you have a User data class
                        rs.getInt("UserID"),
                        rs.getString("Username"),
                        rs.getDate("DateOfBirth"),
                        rs.getString("Email"),
                        rs.getString("FirstName"),
                        rs.getString("LastName"),
                        rs.getTimestamp("RegistrationTimestamp"),
                        rs.getBoolean("IsActive")
                    )
                }
            )
        }
    }
}
