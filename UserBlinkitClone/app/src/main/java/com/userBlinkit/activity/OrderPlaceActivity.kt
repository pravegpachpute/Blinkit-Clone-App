package com.userBlinkit.activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.phonepe.intent.sdk.api.B2BPGRequest
import com.phonepe.intent.sdk.api.B2BPGRequestBuilder
import com.phonepe.intent.sdk.api.PhonePe
import com.phonepe.intent.sdk.api.PhonePeInitException
import com.phonepe.intent.sdk.api.models.PhonePeEnvironment
import com.userBlinkit.utils.CartListener
import com.userBlinkit.R
import com.userBlinkit.adapters.AdaptersCartProducts
import com.userBlinkit.databinding.ActivityOrderPlaceBinding
import com.userBlinkit.databinding.AddressLayoutBinding
import com.userBlinkit.utils.Constants
import com.userBlinkit.models.Orders
import com.userBlinkit.utils.Utils
import com.userBlinkit.viewModels.UserViewModel
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.nio.charset.Charset
import java.security.MessageDigest


class OrderPlaceActivity : AppCompatActivity() {

    private lateinit var binding : ActivityOrderPlaceBinding
    private val viewModel : UserViewModel by viewModels()
    private lateinit var adaptersCartProducts: AdaptersCartProducts
    private lateinit var b2BPGRequest: B2BPGRequest
    private var cartListener : CartListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderPlaceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        changeStatusBarColor()  // change top color
        getAllCartProducts()    // get all products
        backToUserMainActivity()    //back icon click
        onPlaceOrderClicked() // render address page
        initializePhonepe() //Phonepe integration
    }

    private fun initializePhonepe() {
        val data = JSONObject()
        PhonePe.init(this, PhonePeEnvironment.UAT, Constants.MERCHANTID, "")
        data.put("merchantId", Constants.MERCHANTID)
        data.put("merchantTransactionId", Constants.merchantTransactionId)
        data.put("amount", 200)//Long, mandatory
        data.put("mobileNumber", "9999999999")//string optional
        data.put("callbackUrl", "https://webhook.site/callback-url")//string optional

        val paymentInstrument = JSONObject()
        paymentInstrument.put("type", "UPI_INTENT")
        paymentInstrument.put("targetApp", "com.phonepe.simulator")

        data.put("paymentInstrument", paymentInstrument)

        val deviceContext = JSONObject()
        deviceContext.put("deviceOS","ANDROID")

        data.put("deviceContext", deviceContext)

        val payloadBase64 = Base64.encodeToString(
            data.toString().toByteArray(Charset.defaultCharset()), Base64.NO_WRAP
        )

        val checksum = sha256(payloadBase64 + Constants.apiEndPoint + Constants.SALT_KEY) + "###1";

        b2BPGRequest = B2BPGRequestBuilder()
            .setData(payloadBase64)
            .setChecksum(checksum)
            .setUrl(Constants.apiEndPoint)
            .build()
    }

    private fun sha256(input: String): String {
        val bytes = input.toByteArray(Charsets.UTF_8)
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    private fun onPlaceOrderClicked() { // check user 1st time order or not 1st time then enter add address and save in firebase either not 1st time then payment page
        binding.btnNext.setOnClickListener {
            viewModel.getAddressStatus().observe(this) { status ->
                if (status){
                    // payment work
                    getPaymentView()
                }
                else{
                    //alert dialog to add your address
                    val addressLayoutBinding = AddressLayoutBinding.inflate(LayoutInflater.from(this))

                    val alertDialog = AlertDialog.Builder(this).setView(addressLayoutBinding.root).create()
                    alertDialog.show()

                    addressLayoutBinding.btnAdd.setOnClickListener {
                        saveAddress(alertDialog, addressLayoutBinding)
                    }
                }
            }
        }
    }

    val phonePayView = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if(it.resultCode == RESULT_OK){ //meaning we have enter 1 UPI pin
            checkStatus()
        }
    }

    private fun checkStatus() {
        val xVerify = sha256("/pg/v1/status/${Constants.MERCHANTID}/${Constants.merchantTransactionId}${Constants.SALT_KEY}") + "###1"
        val headers = mapOf(
            "Content-Type" to "application/json",
            "X-VERIFY" to xVerify,
            "X-MERCHANT-ID" to Constants.MERCHANTID,
        )
        lifecycleScope.launch {
            viewModel.checkPayment(headers)
            viewModel.paymentStatus.collect { status->
                if (status){
                    Utils.showToast(this@OrderPlaceActivity, "Payment Done...")

                    //order save, delete products
                    saveOrder()

                    viewModel.deleteCartProducts()
                    viewModel.savingCartItemCount(0)
                    cartListener?.hideCartLayout()

                    Utils.hideDialog()
                    startActivity(Intent(this@OrderPlaceActivity, UsersMainActivity::class.java))
                    finish()
                }else{
                    Utils.showToast(this@OrderPlaceActivity, "Payment not Done...")
                }
            }
        }
    }

    private fun saveOrder() {
        viewModel.getAll().observe(this) {cartProductsList->
            if(cartProductsList.isNotEmpty()){
                viewModel.getUserAddress {address ->
                    val order = Orders(
                        orderId = Utils.getRandomId(),
                        orderList = cartProductsList,
                        userAddress = address,
                        orderStatus = 0,
                        orderDate = Utils.getCurrentDate(),
                        orderingUserId = Utils.getCurrentUserId()
                    )
                    viewModel.saveOrderedProducts(order)

                    //Notification.
                   lifecycleScope.launch {
                       viewModel.sendNotification(cartProductsList[0].adminUid!!, "Ordered", "Some Products has been ordered")
                   }
                }
                for(products in cartProductsList){
                    val count = products.productCount
                    val stock = products.productStock?.minus(count!!)
                    if(stock != null){
                        viewModel.saveProductsAfterOrder(stock, products)
                    }
                }
            }
        }
    }

    private fun getPaymentView() {
        try {
            PhonePe.getImplicitIntent(this, b2BPGRequest, "com.phonepe.simulator")?.let { phonePayView.launch(it) }
        }
        catch (e : PhonePeInitException){
            Utils.showToast(this, e.message.toString())
        }
    }

    private fun saveAddress(alertDialog: AlertDialog, addressLayoutBinding: AddressLayoutBinding) {
        Utils.showDialog(this, "Processing...")
        val userPicCode = addressLayoutBinding.etPicCode.text.toString()
        val userPhoneNumber = addressLayoutBinding.etPhoneNumber.text.toString()
        val userState = addressLayoutBinding.etState.text.toString()
        val userDistrict = addressLayoutBinding.etDistrict.text.toString()
        val userAddress = addressLayoutBinding.etDescriptiveAddress.text.toString()

        val address = "$userPicCode, $userDistrict($userState), $userAddress, $userPhoneNumber"

        lifecycleScope.launch {
            viewModel.saveUserAddress(address)
            viewModel.getAddressStatus()
        }
        Utils.showToast(this, "Saved...")
        alertDialog.dismiss()

        //payment work
        getPaymentView()
    }

    private fun backToUserMainActivity() {
        binding.tbOrderFragment.setNavigationOnClickListener {
            startActivity(Intent(this, UsersMainActivity::class.java))
            finish()
        }
    }

    //fetch database products
    private fun getAllCartProducts() {
        viewModel.getAll().observe(this) { cartProductList ->
            adaptersCartProducts = AdaptersCartProducts()
            binding.rvProductsItem.adapter  = adaptersCartProducts
            adaptersCartProducts.differ.submitList(cartProductList)

            var totalPrice = 0
            for (product in cartProductList){
                val price = product.productPrice?.substring(1)?.toInt() // ₹14 remove ₹
                val itemCount = product.productCount!!
                totalPrice += (price?.times(itemCount)!!)
            }

            binding.tvSubTotal.text = totalPrice.toString()
            if (totalPrice < 200){
                binding.tvDeliveryCharge.text = "₹15"
                totalPrice += 15
            }

            binding.tvGrandTotal.text = totalPrice.toString()
        }
    }

    //change notification status color
    private fun changeStatusBarColor(){
        window?.apply {
            val statusBarColors = ContextCompat.getColor(this@OrderPlaceActivity, R.color.orange)
            statusBarColor = statusBarColors
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        }
    }
}