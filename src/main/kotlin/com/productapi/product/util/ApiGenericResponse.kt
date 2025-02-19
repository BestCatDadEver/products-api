package com.productapi.product.util

import com.fasterxml.jackson.annotation.JsonProperty

open class ApiGenericResponse<T>(
    @JsonProperty("success")
    val success: Boolean = false,
    @JsonProperty("message")
    val message: String? = null,
    @JsonProperty("errorCode")
    val errorCode: Int? = null,
    @JsonProperty("code")
    val code: String? = null,
    @JsonProperty("errors")
    val errors: List<String>? = null,
    @JsonProperty("data")
    val data: T? = null
) {
    fun isSuccessful(): Boolean = success

    override fun toString(): String {
        return "ApiGenericResponse(success=$success, message=$message, errorCode=$errorCode, code=$code, errors=$errors, data=$data)"
    }
}
