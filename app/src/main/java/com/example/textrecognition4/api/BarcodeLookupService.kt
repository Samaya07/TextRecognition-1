package com.example.textrecognition4.api

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface BarcodeLookupService {
    @GET("v3/products")
    fun getProductDetails(
        @Query("barcode") barcode: String,
        @Query("key") apiKey: String,
        @Query("formatted") formatted: String
    ): Call<ProductResponse>
}