package com.userBlinkit.fragments

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.userBlinkit.R
import com.userBlinkit.adapters.AdaptersCartProducts
import com.userBlinkit.databinding.FragmentOrderDetailBinding
import com.userBlinkit.viewModels.UserViewModel
import kotlinx.coroutines.launch
import kotlin.getValue

class OrderDetailFragment : Fragment() {

    private val viewModel: UserViewModel by viewModels()
    private lateinit var binding: FragmentOrderDetailBinding
    private lateinit var adaptersCartProducts: AdaptersCartProducts
    private var status = 0
    private var orderId = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?, ): View? {
        binding = FragmentOrderDetailBinding.inflate(layoutInflater)

        getValues() //pass bundle in OrdersFragment and receive here with this function.

        settingStatus() //change color status pack -> loading delivery -> complete.

        lifecycleScope.launch { getOrderedProducts() } //User show Products fetch use userViewModel class method.

        onBackButtonClicked() //back on ordersFragment screen.

        changeStatusBarColor() // change color top
        return binding.root
    }

    private fun onBackButtonClicked() {
        binding.tbOrderDetailFragment.setNavigationOnClickListener {
            findNavController().navigate(R.id.action_orderDetailFragment_to_odersFragment)
        }
    }

    suspend fun getOrderedProducts() {
        viewModel.getOrderedProducts(orderId).collect { cartList ->
            adaptersCartProducts = AdaptersCartProducts()
            binding.rvProductsItems.adapter = adaptersCartProducts
            adaptersCartProducts.differ.submitList(cartList)
        }
    }

    private fun settingStatus() {

        val statusToViews = mapOf(
            0 to listOf(binding.iv1),
            1 to listOf(binding.iv1, binding.iv2, binding.view1),
            2 to listOf(binding.iv1, binding.iv2, binding.iv3, binding.view1, binding.view2,),
            3 to listOf(binding.iv1, binding.iv2, binding.iv3, binding.iv4, binding.view1, binding.view2, binding.view3),
        )

        val viewsToTint = statusToViews.getOrDefault(status, emptyList())

        for (view in viewsToTint){
            view.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.blue)
        }
    }

    private fun getValues() {
        val bundle = arguments
        status = bundle?.getInt("status")!!
        orderId = bundle.getString("orderId").toString()
    }

    // change notification status color
    private fun changeStatusBarColor(){
        activity?.window?.apply {
            val statusBarColors = ContextCompat.getColor(requireContext(), R.color.orange)
            statusBarColor = statusBarColors
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        }
    }
}

//*** compact and concise reduce code
//private fun settingStatus() {
//    when(status){
//        0 ->{
//            binding.iv1.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.blue)
//        }
//        1 ->{
//            binding.iv1.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.blue)
//            binding.iv2.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.blue)
//            binding.view1.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.blue)
//        }
//        2 ->{
//            binding.iv1.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.blue)
//            binding.iv2.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.blue)
//            binding.view1.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.blue)
//            binding.iv3.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.blue)
//            binding.view2.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.blue)
//        }
//        3 ->{
//            binding.iv1.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.blue)
//            binding.iv2.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.blue)
//            binding.view1.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.blue)
//            binding.iv3.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.blue)
//            binding.view2.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.blue)
//            binding.iv4.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.blue)
//            binding.view3.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.blue)
//        }
//    }
//}
