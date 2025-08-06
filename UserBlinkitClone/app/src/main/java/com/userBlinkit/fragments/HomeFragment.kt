package com.userBlinkit.fragments

import android.content.Context
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
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.userBlinkit.utils.CartListener
import com.userBlinkit.R
import com.userBlinkit.adapters.AdapterBestSellers
import com.userBlinkit.adapters.AdapterCategory
import com.userBlinkit.adapters.AdapterProduct
import com.userBlinkit.databinding.BsSeeAllBinding
import com.userBlinkit.databinding.FragmentHomeBinding
import com.userBlinkit.databinding.ItemViewProductBinding
import com.userBlinkit.models.Bestseller
import com.userBlinkit.models.Category
import com.userBlinkit.utils.Constants
import com.userBlinkit.models.Product
import com.userBlinkit.roomDB.CartProducts
import com.userBlinkit.utils.Utils
import com.userBlinkit.viewModels.UserViewModel
import kotlinx.coroutines.launch
import kotlin.getValue

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private val viewModel : UserViewModel by viewModels()
    private lateinit var adapterBestSellers: AdapterBestSellers
    private lateinit var adapterProduct: AdapterProduct
    private var cartListener : CartListener? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?, ): View? {

        binding = FragmentHomeBinding.inflate(layoutInflater)

        changeStatusBarColor() // change notification status color
        setAllCategories() // render list

        navigationToSearchFragment() //Home -> Search

//        get()//No Use   // get all cart products

        onProfileClicked()  //open profile screen

        fetchBestsellers() //top trending products

        return binding.root // Inflate the layout for this fragment
    }

    private fun fetchBestsellers() {
        binding.shimmerViewContainer.visibility = View.VISIBLE
        lifecycleScope.launch {
            viewModel.fetchProductTypes().collect {
                adapterBestSellers = AdapterBestSellers(::onSeeAllButtonClicked) //Open all types of product page
                binding.rvBestSellers.adapter = adapterBestSellers
                adapterBestSellers.differ.submitList(it)
                binding.shimmerViewContainer.visibility = View.GONE
            }
        }
    }

    private fun onProfileClicked() {
        binding.ivProfile.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_profileFragment)
        }
    }

    private fun navigationToSearchFragment() {
        binding.serachCV.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_searchFragment)
        }
    }

    private fun setAllCategories() {
        val categoryList = ArrayList<Category>()

        for (i in 0 until Constants.allProductsCategoryIcon.size){ // create for loop using title , image
            categoryList.add(
                Category(
                    Constants.allProductCategory[i],
                    Constants.allProductsCategoryIcon[i]
                )
            )
        }

        binding.rvCategories.layoutManager = GridLayoutManager(requireContext(), 4)
        binding.rvCategories.adapter = AdapterCategory(categoryList, ::onCategoryIconClicked) // recycler view receive list
    }

    fun onCategoryIconClicked(category: Category){
        val bundle = Bundle()
        bundle.putString("category", category.title)    //key
        findNavController().navigate(R.id.action_homeFragment_to_categoryFragment, bundle)
    }

    fun onSeeAllButtonClicked(productType : Bestseller){
        val bsSeeAllBinding = BsSeeAllBinding.inflate(LayoutInflater.from(requireContext()))
        val bs = BottomSheetDialog(requireContext())
            bs.setContentView(bsSeeAllBinding.root)

        adapterProduct = AdapterProduct(::onAddButtonClicked, ::onIncreamentButtonClicked, ::onDecreamentButtonClicked)

        bsSeeAllBinding.rvProducts.adapter = adapterProduct

        adapterProduct.differ.submitList(productType.products)

        bs.show()
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

//    private fun get(){
//        viewModel.getAll().observe(viewLifecycleOwner) {
//            for (i in it){
//                Log.d("vvv", i.productTitle.toString())
//                Log.d("vvv", i.productCount.toString())
//            }
//        }
//    }