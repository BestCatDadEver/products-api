package com.productapi.product.controller

import com.productapi.product.model.Category
import com.productapi.product.service.api.CategoryService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1")
@CrossOrigin("*")
class CategoryController {

    @Autowired
    lateinit var categoryService: CategoryService

    @GetMapping("/all")
    fun getAll() : MutableList<Category>? {
        return categoryService.all
    }

    @PostMapping("/save")
    fun save(@RequestBody category: Category) : ResponseEntity<Category> {
        var obj = categoryService.save(category)
        return ResponseEntity<Category>(category, HttpStatus.OK)
    }

    @GetMapping("/delete/{id}")
    fun delete(@PathVariable id: Int) : ResponseEntity<Category> {
        val category = categoryService.get(id)
        if (category != null) {
            categoryService.delete(id)
        } else {
            return ResponseEntity<Category>(HttpStatus.NO_CONTENT)
        }

        return ResponseEntity<Category>(category, HttpStatus.OK)
    }
}