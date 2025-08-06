package com.userBlinkit.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.userBlinkit.utils.CartListener
import com.userBlinkit.adapters.AdaptersCartProducts
import com.userBlinkit.databinding.ActivityUsersMainBinding
import com.userBlinkit.databinding.BsCartProductsBinding
import com.userBlinkit.roomDB.CartProducts
import com.userBlinkit.viewModels.UserViewModel

class UsersMainActivity : AppCompatActivity(), CartListener {

    private lateinit var binding : ActivityUsersMainBinding
    private val viewModel : UserViewModel by viewModels()
    private lateinit var cartProductList : List<CartProducts>
    private lateinit var adaptersCartProducts: AdaptersCartProducts

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUsersMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        getTotalItemCountInCart()

        onCartClicked() // open cart

        getAllCartProducts()

        onNextButtonClicked()
    }

    private fun onNextButtonClicked() {
        binding.btnNext.setOnClickListener {
            startActivity(Intent(this, OrderPlaceActivity::class.java))
        }
    }

    private fun getAllCartProducts(){
        viewModel.getAll().observe(this) {
                cartProductList = it
        }
    }

    private fun onCartClicked() {
        binding.llItemCart.setOnClickListener {
            val bsCartProductsBinding = BsCartProductsBinding.inflate(LayoutInflater.from(this))

            val bs = BottomSheetDialog(this)
            bs.setContentView(bsCartProductsBinding.root)

            bsCartProductsBinding.tvNumberOfProductCount.text = binding.tvNumberOfProductCount.text
            bsCartProductsBinding.btnNext.setOnClickListener {
                startActivity(Intent(this, OrderPlaceActivity::class.java))
            }

            adaptersCartProducts = AdaptersCartProducts()
            bsCartProductsBinding.rvProductsItem.adapter = adaptersCartProducts
            adaptersCartProducts.differ.submitList(cartProductList)

            bs.show()
        }
    }

    private fun getTotalItemCountInCart() {
        viewModel.fetchTotalCartItemCount().observe(this) {
            if (it > 0){
                binding.llCart.visibility = View.VISIBLE
                binding.tvNumberOfProductCount.text = it.toString()
            }
            else{
                binding.llCart.visibility = View.GONE
            }
        }
    }

    // Cart visibility, item count real time +, -
    override fun showCartLayout(itemCount : Int) {
        val previousCount = binding.tvNumberOfProductCount.text.toString().toInt()
        val updatedCount = previousCount + itemCount

        if (updatedCount > 0){
            binding.llCart.visibility = View.VISIBLE
            binding.tvNumberOfProductCount.text = updatedCount.toString()
        }
        else{
            binding.llCart.visibility = View.GONE
            binding.tvNumberOfProductCount.text = "0"
        }
    }

    override fun savingCartItemCount(itemCount: Int) {
        viewModel.fetchTotalCartItemCount().observe(this) {
            viewModel.savingCartItemCount(it + itemCount)
        }
    }

    override fun hideCartLayout() {
        binding.llCart.visibility = View.GONE
        binding.tvNumberOfProductCount.text = "0"
    }//all products delete then hide cart.
}