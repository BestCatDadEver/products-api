package com.productapi.product.model

import java.util.*
import javax.persistence.*


@Entity
@Table(name = "Product")
data class Product(

    @Id
    @Column(name = "product_code")
    val productCode: String,
    @Column(name = "product_name")
    val productName: String,
    @Column(name = "product_description")
    val productDescription: String?,
    @Column(name = "product_price")
    val productPrice: String?,
    @Column(name = "product_discount_price")
    val discountPrice: String?,
    @Column(name = "product_link")
    val productLink : String?,
    @Column(name = "category_name")
    val categoryName : String,
    @Column(name = "inserted_on")
    val insertedOn: Date,
    @OneToMany(mappedBy = "productCode")
    val images : List<Image>

)
