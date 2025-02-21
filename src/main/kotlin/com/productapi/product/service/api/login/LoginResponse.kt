package com.productapi.product.service.api.login

import com.productapi.product.model.Role

data class LoginResponse(
    val token: String,
    val userName: String,
    val firstName: String,
    val lastName: String,
    val isActive: Boolean,
    val roleName: String
)