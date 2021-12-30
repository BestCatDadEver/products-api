package com.productapi.product.model

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id

@Entity(name = "Categories")
data class Category(

    @Id
    private val categoryId : Int = 0,
    @Column
    private val categoryName : String = "")