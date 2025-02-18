package com.productapi.product.model

import javax.persistence.*

@Entity
@Table(name = "roles")
data class Role(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null, // Make it nullable for new roles
    val name: String, // E.g., "ROLE_USER", "ROLE_ADMIN"
    // ... other fields as needed (e.g., description)
)