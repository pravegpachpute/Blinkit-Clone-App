package com.example.adminblinkitclone.fragments

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
import com.example.adminblinkitclone.R
import com.example.adminblinkitclone.adapter.AdapterOrders
import com.example.adminblinkitclone.databinding.FragmentOrderBinding
import com.example.adminblinkitclone.models.OrderItem
import com.example.adminblinkitclone.viewModels.AdminViewModel
import kotlinx.coroutines.launch
import kotlin.getValue

class OrderFragment : Fragment() {

    private val viewModel: AdminViewModel by viewModels()
    private lateinit var binding: FragmentOrderBinding
    private lateinit var adapterOrders: AdapterOrders

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?, ): View? {
        binding = FragmentOrderBinding.inflate(layoutInflater)

        getAllOrders()

        changeStatusBarColor() // change color top

        return binding.root
    }

    private fun getAllOrders() {
        binding.shimmerViewContainer.visibility = View.VISIBLE
        lifecycleScope.launch {
            viewModel.getAllOrders().collect { orderList ->
                if (orderList.isNotEmpty()){
                    val orderedList = ArrayList<OrderItem>()

                    for (orders in orderList){
                        val title = StringBuilder()
                        var totalPrice = 0

                        for (product in orders.orderList!!){
                            val price = product.productPrice?.substring(1)?.toInt() // ₹14 remove ₹
                            val itemCount = product.productCount!!
                            totalPrice += (price?.times(itemCount)!!)

                            title.append("${product.productCategory}, ")
                        }
                        val orderedItems = OrderItem(orders.orderId, orders.orderDate, orders.orderStatus, title.toString(), totalPrice, orders.userAddress)
                        orderedList.add(orderedItems)
                    }
                    adapterOrders = AdapterOrders(requireContext(), ::onOrderItemViewClicked)
                    binding.rvOrders.adapter = adapterOrders
                    adapterOrders.differ.submitList(orderedList)
                    binding.shimmerViewContainer.visibility = View.GONE
                }
            }
        }
    }

    fun onOrderItemViewClicked(orderedItems: OrderItem){
        val bundle = Bundle()
        bundle.putInt("status", orderedItems.itemStatus!!)
        bundle.putString("orderId", orderedItems.orderId)
        bundle.putString("userAddress", orderedItems.userAddress)

        findNavController().navigate(R.id.action_orderFragment_to_orderDetailFragment, bundle)
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