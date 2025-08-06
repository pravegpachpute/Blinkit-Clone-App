package com.userBlinkit.viewModels

import android.app.Application
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.userBlinkit.api.ApiUtilities
import com.userBlinkit.models.Bestseller
import com.userBlinkit.utils.Constants
import com.userBlinkit.models.Notification
import com.userBlinkit.models.NotificationData
import com.userBlinkit.models.Orders
import com.userBlinkit.models.Product
import com.userBlinkit.roomDB.CartProducts
import com.userBlinkit.roomDB.CartProductsDao
import com.userBlinkit.roomDB.CartProductsDatabase
import com.userBlinkit.utils.Utils
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UserViewModel(application: Application) : AndroidViewModel(application) {

    //Initializations, get data for DB
    val sharedPreferences : SharedPreferences = application.getSharedPreferences("My_Pref", MODE_PRIVATE)
    val cartProductDao : CartProductsDao = CartProductsDatabase.getDatabaseInstance(application).cartProductsDao()

    private val _paymentStatus = MutableStateFlow<Boolean>(false)
    val paymentStatus = _paymentStatus

    //Room DB
    suspend fun insertCartProduct(products: CartProducts){
        cartProductDao.insertCartProduct(products)
    }
    fun getAll(): LiveData<List<CartProducts>>{
        return cartProductDao.getAllCartProducts()
    }
    suspend fun updateCartProduct(products: CartProducts){
        cartProductDao.updateCartProduct(products)
    }
    suspend fun deleteCartProduct(productId: String){
        cartProductDao.deleteCartProduct(productId)
    } //1.
    suspend fun deleteCartProducts(){
        cartProductDao.deleteCartProducts()
    }//All.

    //Firebase call
    fun updateItemCount(product : Product, itemCount: Int){
        FirebaseDatabase.getInstance().getReference("Admins").child("AllProducts/${product.productRandomId}").child("itemCount").setValue(itemCount)
        FirebaseDatabase.getInstance().getReference("Admins").child("ProductCategory/${product.productCategory}/${product.productRandomId}").child("itemCount").setValue(itemCount)
        FirebaseDatabase.getInstance().getReference("Admins").child("ProductType/${product.productType}/${product.productRandomId}").child("itemCount").setValue(itemCount)
    }   // save Realtime Database
    fun fetchAllTheProducts(): Flow<List<Product>> = callbackFlow {
        val db = FirebaseDatabase.getInstance().getReference("Admins").child("AllProducts")

        val eventListener = object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val products = ArrayList<Product>()
                for (product in snapshot.children){
                    val prod = product.getValue(Product::class.java)    // typecast
                    products.add(prod!!)
                }
                trySend(products)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        db.addValueEventListener(eventListener)

        awaitClose{
            db.removeEventListener(eventListener)
        }
    }   // fetch data DB to UI
    fun getAllOrders() : Flow<List<Orders>> = callbackFlow {
        val db = FirebaseDatabase.getInstance().getReference("Admins").child("Orders").orderByChild("orderStatus")

        val eventListener = object : ValueEventListener{

            override fun onDataChange(snapshot: DataSnapshot) {
                val orderList = ArrayList<Orders>()
                for(orders in snapshot.children){
                    val order = orders.getValue(Orders::class.java)

                    if (order?.orderingUserId == Utils.getCurrentUserId()){
                        orderList.add(order)
                    }
                }
                trySend(orderList)
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        db.addValueEventListener(eventListener)
        awaitClose{ db.removeEventListener(eventListener) }
    }
    fun getCategoryProduct(category: String) : Flow<List<Product>> = callbackFlow{
        val db = FirebaseDatabase.getInstance().getReference("Admins").child("ProductCategory/${category}") //Path database

        val eventListener = object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val products = ArrayList<Product>()
                for (product in snapshot.children){
                    val prod = product.getValue(Product::class.java)    // typecast
                    products.add(prod!!)
                }
                trySend(products)
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        //remove callback event
        db.addValueEventListener(eventListener)

        awaitClose{
            db.removeEventListener(eventListener)
        }
    }
    fun getOrderedProducts(orderId : String): Flow<List<CartProducts>> = callbackFlow{
        val db = FirebaseDatabase.getInstance().getReference("Admins").child("Orders").child(orderId)

        val eventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val order = snapshot.getValue(Orders::class.java)
                trySend(order?.orderList!!)
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        db.addValueEventListener(eventListener)
        awaitClose{ db.removeEventListener(eventListener) }
    }
    fun saveUserAddress(address : String){
        FirebaseDatabase.getInstance().getReference("AllUsers").child("Users").child(Utils.getCurrentUserId()).child("userAddress").setValue(address)
    }//save address in DB
    fun getUserAddress(callback : (String?) -> Unit){
        val db = FirebaseDatabase.getInstance().getReference("AllUsers").child("Users").child(Utils.getCurrentUserId()).child("userAddress")

        db.addListenerForSingleValueEvent(object : ValueEventListener{

            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()){
                    val address = snapshot.getValue(String::class.java)
                    callback(address)
                }
                else{
                    callback(null)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                callback(null)
            }

        })
    } //get user address.
    fun saveOrderedProducts(orders : Orders){
        FirebaseDatabase.getInstance().getReference("Admins").child("Orders").child(orders.orderId!!).setValue(orders)
    }
    fun saveProductsAfterOrder(stock : Int, product: CartProducts){
        FirebaseDatabase.getInstance().getReference("Admins").child("AllProducts/${product.productId}").child("itemCount").setValue(0)
        FirebaseDatabase.getInstance().getReference("Admins").child("ProductCategory/${product.productCategory}/${product.productId}").child("itemCount").setValue(0)
        FirebaseDatabase.getInstance().getReference("Admins").child("ProductType/${product.productType}/${product.productId}").child("itemCount").setValue(0)

        FirebaseDatabase.getInstance().getReference("Admins").child("AllProducts/${product.productId}").child("productStock").setValue(stock)
        FirebaseDatabase.getInstance().getReference("Admins").child("ProductCategory/${product.productCategory}/${product.productId}").child("productStock").setValue(stock)
        FirebaseDatabase.getInstance().getReference("Admins").child("ProductType/${product.productType}/${product.productId}").child("productStock").setValue(stock)
    }
    fun fetchProductTypes(): Flow<List<Bestseller>> = callbackFlow{
        val db = FirebaseDatabase.getInstance().getReference("Admins/ProductType")

        val eventListener = object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val productTypeList = ArrayList<Bestseller>()

                for(productType in snapshot.children){
                    val productTypeName = productType.key //Noodles, Bread, ICream -> ICream

                    val productList = ArrayList<Product>() //ICream

                    for(products in productType.children){ //ICream -> Chocolate, butterscotch
                        val product = products.getValue(Product::class.java)
                        productList.add(product!!)
                    }

                    val bestseller = Bestseller(productType = productTypeName, products = productList)
                    productTypeList.add(bestseller)
                }
                trySend(productTypeList)
            }

            override fun onCancelled(error: DatabaseError) {}
        }

        db.addValueEventListener(eventListener)
        awaitClose{ db.removeEventListener(eventListener) }
    }

    fun saveAddress(address: String){
        FirebaseDatabase.getInstance().getReference("AllUsers").child("Users").child(Utils.getCurrentUserId()).child("userAddress").setValue(address)
    }

    fun logout(){
        FirebaseAuth.getInstance().signOut()
    } //logout User

    //SharedPreferences, save in DB item count
    fun savingCartItemCount(itemCount: Int){
        sharedPreferences.edit().putInt("itemCount", itemCount).apply()
    }
    fun fetchTotalCartItemCount() : MutableLiveData<Int>{
        val totalItemCount = MutableLiveData<Int>()
        totalItemCount.value = sharedPreferences.getInt("itemCount", 0)
        return totalItemCount
    }
    fun saveAddressStatus(){
        sharedPreferences.edit().putBoolean("addressStatus", true).apply()
    }
    fun getAddressStatus() : MutableLiveData<Boolean>{
        val status = MutableLiveData<Boolean>()
        status.value = sharedPreferences.getBoolean("addressStatus", false)
        return status
    }

    //Retrofit
    suspend fun checkPayment(headers : Map<String, String>){
        val res = ApiUtilities.statusApi.checkStatus(headers, Constants.MERCHANTID, Constants.merchantTransactionId)
        _paymentStatus.value = res.body() != null && res.body()!!.success
    }

    //Cloud notification
    suspend fun sendNotification(adminUid : String, title : String, message : String){
        val getToken = FirebaseDatabase.getInstance().getReference("Admins").child("AdminInfo").child(adminUid).child("adminToken").get()

        getToken.addOnCompleteListener { task ->
            val token = task.result.getValue(String::class.java)

            val notification = Notification(token, NotificationData(title, message))

            ApiUtilities.notificationApi.sendNotification(notification)
                .enqueue(object : Callback<Notification>{
                    override fun onResponse(call: Call<Notification?>, response: Response<Notification?>, ) {
                       if (response.isSuccessful){
                           Log.d("DDD", "Send Notification...")
                           Log.d("DDD", token.toString())
                       }
                    }

                    override fun onFailure(call: Call<Notification?>, t: Throwable, ) {}
                })
        }
    }
}


//suspend fun sendNotification(adminUid: String, title: String, message: String) {
//    val getToken = FirebaseDatabase.getInstance().getReference("Admins").child("AdminInfo").child(adminUid).child("adminToken").get()
//
//    getToken.addOnCompleteListener { task ->
//        val token = task.result.getValue(String::class.java)
//
//        val notification = Notification(token, NotificationData(title, message))
//
//        ApiUtilities.notificationApi.sendNotification(notification)
//            .enqueue(object : Callback<Notification> {
//                override fun onResponse(
//                    call: Call<Notification>,
//                    response: Response<Notification>
//                ) {
//                    if (response.isSuccessful) {
//                        Log.d("Notification", "Notification sent successfully")
//                    } else {
//                        Log.e("Notification", "Failed to send: ${response.errorBody()?.string()}")
//                    }
//                }
//
//                override fun onFailure(call: Call<Notification>, t: Throwable) {
//                    Log.e("Notification", "Error sending notification", t)
//                }
//            })
//    }
//}
