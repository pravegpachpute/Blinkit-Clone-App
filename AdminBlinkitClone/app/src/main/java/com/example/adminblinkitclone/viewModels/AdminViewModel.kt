package com.example.adminblinkitclone.viewModels

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.adminblinkitclone.api.ApiUtilities
import com.example.adminblinkitclone.models.CartProducts
import com.example.adminblinkitclone.models.Notification
import com.example.adminblinkitclone.models.NotificationData
import com.example.adminblinkitclone.models.Orders
import com.example.adminblinkitclone.models.Product
import com.example.adminblinkitclone.utils.Utils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.UUID

class AdminViewModel : ViewModel(){

    private val _isImagesUploaded = MutableStateFlow(false)
    var isImagesUploaded : StateFlow<Boolean> = _isImagesUploaded

    private val _downloadedUrls = MutableStateFlow<ArrayList<String?>>(arrayListOf())
    var downloadedUrls : StateFlow<ArrayList<String?>> = _downloadedUrls

    private val _isProductSaved = MutableStateFlow(false)
    val isProductSaved : StateFlow<Boolean> = _isProductSaved

    fun saveImageInDB(imageUri : ArrayList<Uri>){   // 1 create array
        val downloadUrls = ArrayList<String?>()

        imageUri.forEach { uri ->
            val imageRef = FirebaseStorage.getInstance()
                .reference
                .child(Utils.getCurrentUserId())
                .child("images")
                .child(UUID.randomUUID().toString())

            imageRef.putFile(uri).continueWithTask {
                imageRef.downloadUrl                    // 2 download urls
            }.addOnCompleteListener { task ->
                val url = task.result
                downloadUrls.add(url.toString())        // 3 add urls

                if (downloadUrls.size == imageUri.size){
                    _isImagesUploaded.value = true
                    _downloadedUrls.value = downloadUrls
                }
            }
        }
    }   // cant use storage Paid

    fun saveProduct(product : Product){
        FirebaseDatabase.getInstance().getReference("Admins").child("AllProducts/${product.productRandomId}").setValue(product)
            .addOnSuccessListener {
                FirebaseDatabase.getInstance().getReference("Admins").child("ProductCategory/${product.productCategory}/${product.productRandomId}").setValue(product)
                    .addOnSuccessListener {
                        FirebaseDatabase.getInstance().getReference("Admins").child("ProductType/${product.productType}/${product.productRandomId}").setValue(product)
                            .addOnSuccessListener {
                                _isProductSaved.value = true
                            }
                    }
            }
    }   // save Realtime Database

    fun fetchAllTheProducts(category: String): Flow<List<Product>> = callbackFlow {
        val db = FirebaseDatabase.getInstance().getReference("Admins").child("AllProducts")

        val eventListner = object : ValueEventListener{

            override fun onDataChange(snapshot: DataSnapshot) {
                val products = ArrayList<Product>()
                for (product in snapshot.children){
                    val prod = product.getValue(Product::class.java)    // typecast
                    if (category == "All" || prod?.productCategory == category){
                        products.add(prod!!)
                    }

                }
                trySend(products)
            }

            override fun onCancelled(error: DatabaseError) {}
        }

        db.addValueEventListener(eventListner)

        awaitClose{
            db.removeEventListener(eventListner)
        }
    }   // fetch data DB to UI

    //Path save data clicked save btn HomeFragment()
    fun savingUpdateProduct(product: Product){
        FirebaseDatabase.getInstance().getReference("Admins").child("AllProducts/${product.productRandomId}").setValue(product)
        FirebaseDatabase.getInstance().getReference("Admins").child("ProductCategory/${product.productCategory}/${product.productRandomId}").setValue(product)
        FirebaseDatabase.getInstance().getReference("Admins").child("ProductType/${product.productType}/${product.productRandomId}").setValue(product)
    }

    fun getAllOrders() : Flow<List<Orders>> = callbackFlow {
        val db = FirebaseDatabase.getInstance().getReference("Admins").child("Orders").orderByChild("orderStatus")

        val eventListener = object : ValueEventListener{

            override fun onDataChange(snapshot: DataSnapshot) {
                val orderList = ArrayList<Orders>()
                for(orders in snapshot.children){
                    val order = orders.getValue(Orders::class.java)
                    if (order != null) {
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

    fun updateOrderStatus(orderId: String, status : Int){
        FirebaseDatabase.getInstance().getReference("Admins").child("Orders").child(orderId).child("orderStatus").setValue(status)
    }

    //Cloud notification
    suspend fun sendNotification(orderId : String, title : String, message : String){
        val getToken = FirebaseDatabase.getInstance().getReference("Admins").child("Orders").child(orderId).child("orderingUserId").get()
        getToken.addOnCompleteListener { task ->

            val userId = task.result.getValue(String::class.java)
            val userToken = FirebaseDatabase.getInstance().getReference("AllUsers").child("Users").child(userId!!).child("userToken").get()

            userToken.addOnCompleteListener {

                val notification = Notification(it.result.getValue(String::class.java), NotificationData(title, message))

                ApiUtilities.notificationApi.sendNotification(notification)
                    .enqueue(object : Callback<Notification>{
                        override fun onResponse(call: Call<Notification?>, response: Response<Notification?>,) {
                            if (response.isSuccessful){
                                Log.d("DDD", "Send Notification...")
                                Log.d("GGG", it.result.getValue(String::class.java).toString())
                            }
                        }

                        override fun onFailure(call: Call<Notification?>, t: Throwable, ) {}
                    })
            }

        }
    }

    fun logout(){
        FirebaseAuth.getInstance().signOut()
    } //logout User

}


/* fun saveImageInDB(imageUri: ArrayList<Uri>) {
        val downloadUrls = ArrayList<String?>()

        imageUri.forEach { uri ->
            val imageRef = FirebaseStorage.getInstance()
                .reference
                .child(Utils.getCurrentUserId())
                .child("images")
                .child(UUID.randomUUID().toString())

            imageRef.putFile(uri)
                .addOnSuccessListener {
                    // ✅ File upload succeeded
                    imageRef.downloadUrl.addOnSuccessListener { url ->
                        downloadUrls.add(url.toString())

                        if (downloadUrls.size == imageUri.size) {
                            _isImagesUploaded.value = true
                            _downloadedUrls.value = downloadUrls
                        }

                    }.addOnFailureListener { e ->
                        Log.e("DownloadURL", "Failed to get download URL", e)
                    }

                }.addOnFailureListener { e ->
                    Log.e("Upload", "Image upload failed", e)
                }
        }
}*/