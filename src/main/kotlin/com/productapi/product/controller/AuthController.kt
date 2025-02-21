package com.productapi.product.controller

import com.productapi.product.model.Role
import com.productapi.product.model.User
import com.productapi.product.service.api.login.LoginResponse
import com.productapi.product.service.api.signup.SignupResponse
import com.productapi.product.util.ApiGenericResponse
import com.productapi.product.util.AuthTokenFilter
import com.productapi.product.util.JwtUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataAccessException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.jdbc.core.ConnectionCallback
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.web.bind.annotation.*
import java.sql.CallableStatement
import java.sql.ResultSet
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@RestController
@RequestMapping("/api/auth")
class AuthController {

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

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


            val sql = "EXEC RegisterUser ?, ?, ?, ?, ?, ?, ?, ?"
            val result = jdbcTemplate.update(
                sql,
                username,
                password,
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


    @GetMapping("/health")
    fun health(): String {
        return "Service is running"
    }

    @PostMapping("/login")
    fun loginUser(@RequestBody loginRequest: Map<String, String>): ResponseEntity<ApiGenericResponse<LoginResponse>> {
        val username = loginRequest["username"]?.trim() ?: return ResponseEntity.badRequest().body(
            ApiGenericResponse(success = false, message = "Username is required")
        )
        val password = loginRequest["password"]?.trim() ?: return ResponseEntity.badRequest().body(
            ApiGenericResponse(success = false, message = "Password is required")
        )

        try {
            val users = jdbcTemplate.execute(ConnectionCallback<List<User>> { connection ->
                val cs: CallableStatement = connection.prepareCall("{call LoginUser(?, ?)}")
                cs.setString(1, username)
                cs.setString(2, password)
                val rs: ResultSet? = cs.executeQuery()

                val userList = mutableListOf<User>()
                if (rs != null) {
                    while (rs.next()) {
                        logger.info("User found in database: ${rs.getString("Username")}")

                        userList.add(
                            User(
                                id = rs.getLong("UserID"),
                                username = rs.getString("Username"),
                                dateOfBirth = null,
                                email = rs.getString("Email"),
                                firstName = rs.getString("FirstName"),
                                lastName = rs.getString("LastName"),
                                role = Role(roleName = rs.getString("RoleName")),
                                passwordHash = rs.getBytes("PasswordHash"),
                                isActive = rs.getBoolean("IsActive")
                            )
                        )
                    }
                    rs.close()
                }
                cs.close()
                userList
            })

            if (users?.isNotEmpty() == true) {
                val user = users[0]
                logger.info("User found: ${user.username}")

                val token = jwtUtil.generateJwtToken(user)
                val loginResponse = user.role?.let { role: Role ->
                    LoginResponse(
                        token = token,
                        userName = user.username,
                        firstName = user.firstName,
                        lastName = user.lastName,
                        isActive = user.isActive,
                        roleName = role.roleName
                    )
                }
                return ResponseEntity.ok(ApiGenericResponse(success = true, data = loginResponse))
            } else {
                logger.info("Invalid username or password for user: $username")
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    ApiGenericResponse(success = false, message = "Invalid username or password.", errorCode = 401)
                )
            }

        } catch (e: DataAccessException) {
            logger.error("Database error during login for user: $username", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiGenericResponse(success = false, message = "Login failed: ${e.message}", errorCode = 500)
            )
        } catch (e: Exception) {
            logger.error("General error during login for user: $username", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiGenericResponse(success = false, message = "Login failed: ${e.message}", errorCode = 500)
            )
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
