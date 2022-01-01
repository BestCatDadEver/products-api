package com.productapi.product.service.impl

import com.productapi.product.common.GenericApiImpl
import com.productapi.product.model.Category
import com.productapi.product.repository.CategoryRepository
import com.productapi.product.service.api.CategoryService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Service

@Service
class CategoryServiceImpl : GenericApiImpl<Category, Int>(), CategoryService {

    @Autowired
    lateinit var categoryRepository: CategoryRepository

    override val dao: CrudRepository<Category, Int>
        get() = categoryRepository


}