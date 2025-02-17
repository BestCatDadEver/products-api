package com.productapi.product.model

data class User(
    val userId: Int,
    val username: String,
    val dateOfBirth: java.sql.Date?, // Make it nullable if needed
    val email: String,
    val firstName: String,
    val lastName: String,
    val registrationTimestamp: java.sql.Timestamp,
    val isActive: Boolean
    // ... other fields
)
