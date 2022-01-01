package com.productapi.product.controller

import com.productapi.product.model.Category
import com.productapi.product.model.Product
import com.productapi.product.service.api.ProductService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/product")
@CrossOrigin("*")
class ProductController {

    @Autowired
    lateinit var productService: ProductService

    @GetMapping("/all")
    fun getAll() : MutableList<Product>? {
        return productService.all
    }
}