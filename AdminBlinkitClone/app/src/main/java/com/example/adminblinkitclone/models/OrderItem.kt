package com.example.adminblinkitclone.models

data class OrderItem(
    val orderId : String? = null,
    val itemDate : String? = null,
    val itemStatus : Int? = null,
    val itemTitle : String? = null,
    val itemPrice : Int? = null,
    val userAddress : String? = null
)