package com.productapi.product.service.api

import com.productapi.product.model.User
import com.productapi.product.repository.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class UserService {

    @Autowired
    private lateinit var userRepository: UserRepository

    fun createUser(user: User): User {
        return userRepository.save(user)  // Save the user to the database
    }

    fun findUserByUsername(username: String): User? {
        return userRepository.findByUsername(username) // Find user by username
    }



}