package com.example.textrecognition4.api

import com.google.gson.annotations.SerializedName

data class Product(
    @SerializedName("title")
    val title: String,
    @SerializedName("stores")
    val stores: List<Store>
    // Add other fields as needed
)

data class Store(
    @SerializedName("price")
    val price: String?
    // Add other fields as needed
)
