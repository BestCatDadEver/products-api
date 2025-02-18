package com.productapi.product.controller

import com.productapi.product.model.User
import com.productapi.product.util.JwtUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataAccessException
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.jdbc.core.JdbcTemplate  // For calling stored procedures
import org.springframework.security.crypto.bcrypt.BCrypt
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@RestController
@RequestMapping("/api/auth")
class AuthController {

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate // Inject JdbcTemplate

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var jwtUtil: JwtUtil


    @PostMapping("/register")
    fun registerUser(@RequestBody registrationRequest: Map<String, String>): ResponseEntity<*> {
        try {
            val username = registrationRequest["username"]?.trim() ?: return badRequest("Username is required")
            val password = registrationRequest["password"] ?: return badRequest("Password is required")
            val dateOfBirthStr = registrationRequest["dateOfBirth"] ?: return badRequest("Date of Birth is required")
            val email = registrationRequest["email"]?.trim() ?: return badRequest("Email is required")
            val firstName = registrationRequest["firstName"]?.trim() ?: return badRequest("First Name is required")
            val lastName = registrationRequest["lastName"]?.trim() ?: return badRequest("Last Name is required")
            val roleName = registrationRequest["roleName"] ?: return badRequest("Role Name is required")
            val isActiveStr = registrationRequest["isActive"] ?: "true" // Default to true

            if (username.isBlank() || password.isBlank() || email.isBlank() || roleName.isBlank()) {
                return badRequest("All required fields are required.")
            }

            if (!isValidEmail(email)) {
                return badRequest("Invalid email format.")
            }

            val dateOfBirth: LocalDate = try {
                LocalDate.parse(dateOfBirthStr, DateTimeFormatter.ofPattern("yyyy-MM-dd")) // ISO 8601 format
            } catch (e: DateTimeParseException) {
                return badRequest("Invalid date format. Use YYYY-MM-DD.")
            }

            val isActive = try {
                isActiveStr.toBoolean()
            } catch (e: IllegalArgumentException) {
                return badRequest("Invalid isActive value. Use true or false.")
            }


            val hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt()).toByteArray()

            val sql = "EXEC RegisterUser ?, ?, ?, ?, ?, ?, ?, ?"
            val result = jdbcTemplate.update(
                sql,
                username,
                hashedPassword,
                java.sql.Date.valueOf(dateOfBirth),
                email,
                firstName,
                lastName,
                isActive,
                roleName
            )

            println("Executing SQL: $sql with parameters: $username, " +
                    "$email, $hashedPassword, $dateOfBirth, $firstName, $lastName, $isActive, $roleName")


            if (result > 0) {
                return ResponseEntity.ok("User registered successfully.")
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Registration failed.")
            }

        } catch (e: Exception) {
            e.printStackTrace() // Log the exception for debugging
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Registration failed: ${e.message}")
        }
    }

    @PostMapping("/login")
    fun loginUser(@RequestBody loginRequest: Map<String, String>): ResponseEntity<*> {
        try {
            val username = loginRequest["username"]
            val password = loginRequest["password"] // Get plaintext password

            // 1. Hash the password using BCrypt
            val hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt())

            // 2. Call the stored procedure, passing the hashed password
            val user = jdbcTemplate.queryForObject(
                "EXEC LoginUser ?, ?",
                arrayOf(username, hashedPassword.toByteArray()), // Pass the hashed password as a byte array
                { rs, _ ->
                    User( // Assuming 'User' is your data class
                        id = rs.getLong("UserID"),
                        username = rs.getString("Username"),
                        dateOfBirth = rs.getDate("DateOfBirth")?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate(),
                        email = rs.getString("Email"),
                        firstName = rs.getString("FirstName"),
                        lastName = rs.getString("LastName"),
                        passwordHash = rs.getBytes("passwordHash"),
                        roles = mutableListOf() // Initialize roles if needed
                    )
                }
            )

            val token = user?.let { jwtUtil.generateJwtToken(it) } // Generate JWT token

            return ResponseEntity.ok(mapOf("token" to token))

        } catch (e: EmptyResultDataAccessException) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password.")
        } catch (e: DataAccessException) {
            e.printStackTrace()
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Login failed: ${e.message}")
        } catch (e: Exception) {
            e.printStackTrace()
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Login failed: ${e.message}")
        }
    }


    private fun badRequest(message: String): ResponseEntity<String> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(message)
    }

    private fun isValidEmail(email: String): Boolean {
        return email.matches(Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"))
    }
}
