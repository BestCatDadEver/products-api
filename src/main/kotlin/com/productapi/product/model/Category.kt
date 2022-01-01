package com.productapi.product.model

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "Category")
data class Category(

    @Id
    @Column(name = "category_id")
    val categoryId : Int,
    @Column(name = "category_name")
    val categoryName : String)