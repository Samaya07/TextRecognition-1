package com.example.textrecognition4.api

import com.google.gson.annotations.SerializedName

data class ProductResponse(
    @SerializedName("products")
    val products: List<Product>
    // Add other fields as needed
)