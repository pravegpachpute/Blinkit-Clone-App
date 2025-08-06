package com.userBlinkit.fragments

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.userBlinkit.utils.CartListener
import com.userBlinkit.R
import com.userBlinkit.adapters.AdapterProduct
import com.userBlinkit.databinding.FragmentSearchBinding
import com.userBlinkit.databinding.ItemViewProductBinding
import com.userBlinkit.models.Product
import com.userBlinkit.roomDB.CartProducts
import com.userBlinkit.utils.Utils
import com.userBlinkit.viewModels.UserViewModel
import kotlinx.coroutines.launch

class SearchFragment : Fragment() {

    val viewModel : UserViewModel by viewModels()
    private lateinit var binding : FragmentSearchBinding
    private lateinit var adapterProduct : AdapterProduct
    private var cartListener : CartListener? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?, ): View? {
        binding = FragmentSearchBinding.inflate(layoutInflater)

        changeStatusBarColor()

        getAllTheProducts() // pass the new list Adapter set product list // 1 call this fuc -> 2 in ViewModel class fetchAllTheProducts()

        backToHomeFragment()

        searchProducts()

        return binding.root
    }

    private fun searchProducts() {
        binding.searchEt.addTextChangedListener(object : TextWatcher{

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                if (this@SearchFragment::adapterProduct.isInitialized) {
                    adapterProduct.filter.filter(query)
                }
            }

            override fun afterTextChanged(s: Editable?) {}

        })
    }

    private fun backToHomeFragment() {
        binding.searchEt.setOnClickListener {
            findNavController().navigate(R.id.action_searchFragment_to_homeFragment)
        }
    }

    private fun getAllTheProducts() {
        binding.shimmerViewContainer.visibility = View.VISIBLE

        lifecycleScope.launch {
            viewModel.fetchAllTheProducts().collect {

                if (it.isEmpty()){
                    binding.rvProducts.visibility = View.GONE
                    binding.tvText.visibility = View.VISIBLE
                }
                else{
                    binding.rvProducts.visibility = View.VISIBLE
                    binding.tvText.visibility = View.GONE
                }
                adapterProduct = AdapterProduct(
                    ::onAddButtonClicked,
                    ::onIncreamentButtonClicked,
                    ::onDecreamentButtonClicked
                )
                binding.rvProducts.adapter = adapterProduct
                adapterProduct.differ.submitList(it)
                adapterProduct.originalList = it as ArrayList<Product>   //Search
                binding.shimmerViewContainer.visibility = View.GONE
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is CartListener){
            cartListener = context
        }
        else{
            throw ClassCastException("Please implement cart listener")
        }
    }

    //add Button set view gone or visible bottom cart item
    private fun onAddButtonClicked(product: Product, productBinding: ItemViewProductBinding){
        productBinding.tvAdd.visibility = View.GONE
        productBinding.llProductCount.visibility = View.VISIBLE

        //step 1. Extract values
        var itemCount = productBinding.tvProductCount.text.toString().toInt()
        itemCount++
        productBinding.tvProductCount.text = itemCount.toString()

        cartListener?.showCartLayout(1)

        //step 2. Save values in DB firebase
        product.itemCount = itemCount
        lifecycleScope.launch {
            cartListener?.savingCartItemCount(1)

            saveProductInRoom(product)

            viewModel.updateItemCount(product, itemCount) // save count in firebase
        }
    }

    // + button
    private fun onIncreamentButtonClicked(product: Product, productBinding: ItemViewProductBinding){
        var itemCountInc = productBinding.tvProductCount.text.toString().toInt()
        itemCountInc++

        if (product.productStock!! + 1 > itemCountInc){
            productBinding.tvProductCount.text = itemCountInc.toString()

            cartListener?.showCartLayout(1)

            //step 2.
            product.itemCount = itemCountInc
            lifecycleScope.launch {
                cartListener?.savingCartItemCount(1)

                saveProductInRoom(product)

                viewModel.updateItemCount(product, itemCountInc) // save count in firebase
            }
        }
        else{
            Utils.showToast(requireContext(), "Can't add more item of this")
        }
    }

    // - button
    private fun onDecreamentButtonClicked(product: Product, productBinding: ItemViewProductBinding){
        var itemCountDec = productBinding.tvProductCount.text.toString().toInt()
        itemCountDec--

        //step 2.
        product.itemCount = itemCountDec
        lifecycleScope.launch {
            cartListener?.savingCartItemCount(-1)

            saveProductInRoom(product)

            viewModel.updateItemCount(product, itemCountDec) // save count in firebase
        }

        if (itemCountDec > 0){
            productBinding.tvProductCount.text = itemCountDec.toString()
        }
        else{
            lifecycleScope.launch { viewModel.deleteCartProduct(product.productRandomId!!) } // remove cart in Room db
            productBinding.tvAdd.visibility = View.VISIBLE
            productBinding.llProductCount.visibility = View.GONE
            productBinding.tvProductCount.text = "0"
        }

        cartListener?.showCartLayout(-1)
    }

    private fun saveProductInRoom(product: Product) {

        val cartProduct = CartProducts(
            productId = product.productRandomId!!,
            productTitle = product.productTitle,
            productQuantity = product.productQuantity.toString() + product.productUnit.toString(),
            productPrice = "₹" + "${product.productPrice}",
            productCount = product.itemCount,
            productStock = product.productStock,
            productImage = product.productImageUris?.get(0)!!,
            productCategory = product.productCategory,
            adminUid = product.adminUid,
            productType = product.productType
        )

        lifecycleScope.launch {
            viewModel.insertCartProduct(cartProduct)
        }
    } //call onAddButtonClicked()

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