package com.productapi.product.controller

import com.productapi.product.model.User
import com.productapi.product.service.api.signup.SignupResponse
import com.productapi.product.util.ApiGenericResponse
import com.productapi.product.util.AuthTokenFilter
import com.productapi.product.util.JwtUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataAccessException
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.crypto.bcrypt.BCrypt
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.*

@RestController
@RequestMapping("/api/auth")
class AuthController {

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate // Inject JdbcTemplate

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var jwtUtil: JwtUtil

    private val logger = LoggerFactory.getLogger(AuthTokenFilter::class.java)


    @PostMapping("/register")
    fun registerUser(@RequestBody registrationRequest: Map<String, String>): ResponseEntity<ApiGenericResponse<SignupResponse>> {
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
                LocalDate.parse(dateOfBirthStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            } catch (e: DateTimeParseException) {
                return badRequest("Invalid date format. Use YYYY-MM-DD.")
            }

            val isActive = try {
                isActiveStr.toBoolean()
            } catch (e: IllegalArgumentException) {
                return badRequest("Invalid isActive value. Use true or false.")
            }

            val hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt()).toByteArray()

            val sql = "EXEC RegisterUser ?, ?, ?, ?, ?, ?, ?, ?" // Assuming stored procedure
            val result = jdbcTemplate.update(
                sql,
                username,
                hashedPassword,
                java.sql.Date.valueOf(dateOfBirth),
                email,
                firstName,
                lastName,
                roleName,
                isActive
            )

            return if (result == -1) {
                val registrationData = SignupResponse(userName = username)
                val response = ApiGenericResponse(success = true, message = "User registered successfully.", data = registrationData)
                ResponseEntity.ok(response)
            } else {
                val response = ApiGenericResponse<SignupResponse>(success = false, message = "Registration failed.", errorCode = 1002, code = "REG_FAIL")
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response)
            }

        } catch (e: Exception) {
            e.printStackTrace()
            return internalServerError("An unexpected error occurred during registration.")
        }
    }

    @PostMapping("/login")
    fun loginUser(@RequestBody loginRequest: Map<String, String>): ResponseEntity<*> {
        try {
            val username = loginRequest["username"] ?: return ResponseEntity.badRequest().body("Username is required")
            val password = loginRequest["password"] ?: return ResponseEntity.badRequest().body("Password is required")

            val hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt()).toByteArray()

            val user = jdbcTemplate.queryForObject(
                "EXEC LoginUser ?, ?", // Your stored procedure call
                arrayOf(username, hashedPassword),
                { rs, _ ->
                    val storedPasswordHash = rs.getBytes("PasswordHash")

                    if (storedPasswordHash != null && Arrays.equals(hashedPassword, storedPasswordHash)) {
                        User( // Assuming User is your data class
                            id = rs.getLong("UserID"),
                            username = rs.getString("Username"),
                            passwordHash = storedPasswordHash,
                            dateOfBirth = rs.getDate("DateOfBirth")?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate(),
                            email = rs.getString("Email"),
                            firstName = rs.getString("FirstName"),
                            lastName = rs.getString("LastName"),
                            roles = mutableListOf() // Initialize roles if needed
                        )
                    } else {
                        null
                    }
                }
            )

            if (user != null) {
                val token = jwtUtil.generateJwtToken(user)
                return ResponseEntity.ok(mapOf("token" to token))
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password.")
            }

        } catch (e: EmptyResultDataAccessException) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password.")
        } catch (e: DataAccessException) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Login failed: ${e.message}")
        } catch (e: Exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Login failed: ${e.message}")
        }
    }

    private fun badRequest(message: String): ResponseEntity<ApiGenericResponse<SignupResponse>> {
        val response = ApiGenericResponse<SignupResponse>(success = false, message = message, errorCode = 400, code = "BAD_REQUEST")
        return ResponseEntity.badRequest().body(response)
    }

    private fun internalServerError(message: String): ResponseEntity<ApiGenericResponse<SignupResponse>> {
        val response = ApiGenericResponse<SignupResponse>(success = false, message = message, errorCode = 500, code = "INTERNAL_SERVER_ERROR")
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response)
    }

    private fun isValidEmail(email: String): Boolean {
        return email.matches(Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"))
    }
}
