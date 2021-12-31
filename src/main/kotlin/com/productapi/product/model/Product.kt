package com.productapi.product.model

import javax.persistence.Column
import javax.persistence.Entity


data class Product(

    @Column
    private val productName: String = "",
    @Column
    private val productDescription: String = "",
    @Column
    private val productPrice: Double = 0.00,
    @Column
    private val discountPrice: Double = 0.00,
    @Column
    private val categoryName : String = "",
)
