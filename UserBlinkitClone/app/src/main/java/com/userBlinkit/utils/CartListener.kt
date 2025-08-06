package com.userBlinkit.utils

interface CartListener {

    fun showCartLayout(itemCount : Int)

    fun savingCartItemCount(itemCount : Int)

    fun hideCartLayout()
}