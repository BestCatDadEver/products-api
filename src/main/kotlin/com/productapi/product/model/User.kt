package com.productapi.product.model

import java.time.LocalDate

import javax.persistence.*

@Entity
@Table(name = "users")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    val username: String,

    val passwordHash: ByteArray,

    val dateOfBirth: LocalDate?,

    val email: String,

    val firstName: String,

    val lastName: String,

    val isActive: Boolean = true,

    @ManyToOne  // One-to-many relationship (one role per user)
    @JoinColumn(name = "role_id") // Name of the foreign key column in the Users table
    val role: Role? = null // User can have one role or no role (nullable)
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as User

        if (id != other.id) return false
        if (username != other.username) return false
        if (!passwordHash.contentEquals(other.passwordHash)) return false
        if (dateOfBirth != other.dateOfBirth) return false
        if (email != other.email) return false
        if (firstName != other.firstName) return false
        if (lastName != other.lastName) return false
        if (isActive != other.isActive) return false
        if (role != other.role) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + username.hashCode()
        result = 31 * result + passwordHash.contentHashCode()
        result = 31 * result + (dateOfBirth?.hashCode() ?: 0)
        result = 31 * result + email.hashCode()
        result = 31 * result + firstName.hashCode()
        result = 31 * result + lastName.hashCode()
        result = 31 * result + isActive.hashCode()
        result = 31 * result + (role?.hashCode() ?: 0)
        return result
    }
}