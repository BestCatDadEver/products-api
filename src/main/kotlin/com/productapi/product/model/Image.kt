package com.productapi.product.model

import javax.persistence.*

@Entity
@Table(name = "Image")
@SecondaryTable(name = "Product")
data class Image(

    @Id
    @Column(name = "image_id")
    val imageId : Int,
    @Column(name = "path")
    val path : String,
    @Column(name = "product_code")
    val productCode : Int,
    @ManyToOne
    @JoinColumn(name = "product_code", table = "Product")
    val product : Product
)