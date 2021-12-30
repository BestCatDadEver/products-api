package com.productapi.product.common

import java.io.Serializable

interface GenericApi<T, ID : Serializable?> {
    fun save(entity: T): T
    fun delete(id: ID)
    operator fun get(id: ID): T?
    val all: MutableList<T>?
}