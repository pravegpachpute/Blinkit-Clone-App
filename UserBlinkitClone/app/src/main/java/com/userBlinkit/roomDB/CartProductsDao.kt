package com.userBlinkit.roomDB

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface CartProductsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCartProduct(products: CartProducts)

    @Update
    fun updateCartProduct(products: CartProducts)

    @Query("SELECT * FROM CartProducts")    //If should be here if we want to see the data in app inspection.
    fun getAllCartProducts(): LiveData<List<CartProducts>>

    @Query("DELETE FROM CartProducts WHERE productId = :productId") //1 delete.
    fun deleteCartProduct(productId: String)

    @Query("DELETE FROM CartProducts")  //All delete.
    suspend fun deleteCartProducts()
}