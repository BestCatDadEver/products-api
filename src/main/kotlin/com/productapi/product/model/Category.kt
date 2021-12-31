package com.productapi.product.model


import org.springframework.boot.context.properties.bind.Name
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "Categories" )
data class Category(

    @Id
    @Column(name = "category_id")
    private val categoryId : Int,
    @Column(name = "category_name")
    private val categoryName : String)