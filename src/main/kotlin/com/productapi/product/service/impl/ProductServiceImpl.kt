package com.productapi.product.service.impl

import com.productapi.product.common.GenericApiImpl
import com.productapi.product.model.Product
import com.productapi.product.repository.ProductRepository
import com.productapi.product.service.api.ProductService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Service

@Service
class ProductServiceImpl : GenericApiImpl<Product, String>(), ProductService{

    @Autowired
    lateinit var productRepository: ProductRepository

    override val dao: CrudRepository<Product, String>
        get() = productRepository
}