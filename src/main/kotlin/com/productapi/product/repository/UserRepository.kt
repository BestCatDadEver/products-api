package com.productapi.product.repository

import com.productapi.product.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : JpaRepository<User, Long> { // <Entity, ID Type>
    fun findByUsername(username: String): User?  // Add custom queries as needed
    fun existsByUsername(username: String): Boolean
    fun existsByEmail(email: String): Boolean

}