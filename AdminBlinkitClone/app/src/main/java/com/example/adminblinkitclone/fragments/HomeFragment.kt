package com.example.adminblinkitclone.fragments

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.adminblinkitclone.R
import com.example.adminblinkitclone.activity.AuthMainActivity
import com.example.adminblinkitclone.adapter.AdapterProduct
import com.example.adminblinkitclone.adapter.CategoriesAdapter
import com.example.adminblinkitclone.databinding.EditProductLayoutBinding
import com.example.adminblinkitclone.databinding.FragmentHomeBinding
import com.example.adminblinkitclone.models.Categories
import com.example.adminblinkitclone.utils.Constants
import com.example.adminblinkitclone.models.Product
import com.example.adminblinkitclone.utils.Utils
import com.example.adminblinkitclone.viewModels.AdminViewModel
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    val viewModel : AdminViewModel by viewModels()
    private lateinit var binding : FragmentHomeBinding
    private lateinit var adapterProduct: AdapterProduct

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?, ): View? {

        binding = FragmentHomeBinding.inflate(layoutInflater)

        changeStatusBarColor()  // change color

        setCategories() // horizontal RecyclerView image + icon

        searchProducts() // Search

        getAllTheProducts("All") // pass the new list Adapter set product list // 1 call this fuc -> 2 in ViewModel class fetchAllTheProducts()

        onLogout() //logout admin

        return binding.root
    }

    private fun onLogout() {
        binding.tbHomeFragment.setOnMenuItemClickListener {
            when(it.itemId){
                R.id.menuLogout ->{
                    logoutUser()
                    true
                }
                else -> { false }
            }
        }
    }

    private fun logoutUser(){
            val builder = AlertDialog.Builder(requireContext())
            val alertDialog = builder.create()
            builder.setTitle("Log out")
                .setMessage("Do you want to log out ?")
                .setPositiveButton("Yes"){_,_ -> //(_,_ = nothing pass aplyala kahi pass nasel karaych tevha )
                    viewModel.logout()
                    startActivity(Intent(requireContext(), AuthMainActivity::class.java))
                    requireActivity().finish()
                }
                .setNegativeButton("No"){_,_ ->
                    alertDialog.dismiss()
                }
                .show()
                .setCancelable(false)
    }

    private fun searchProducts() {
        binding.searchEt.addTextChangedListener(object : TextWatcher{

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
//                adapterProduct.filter.filter(query)
                // FIX: Only call filter if adapterProduct is initialized
                if (this@HomeFragment::adapterProduct.isInitialized) {
                    adapterProduct.filter.filter(query)
                }
            }

            override fun afterTextChanged(s: Editable?) {}

        })
    }

    private fun getAllTheProducts(category: String) {
        binding.shimmerViewContainer.visibility = View.VISIBLE

        lifecycleScope.launch {
            viewModel.fetchAllTheProducts(category).collect {

                if (it.isEmpty()){
                    binding.rvProducts.visibility = View.GONE
                    binding.tvText.visibility = View.VISIBLE
                }
                else{
                    binding.rvProducts.visibility = View.VISIBLE
                    binding.tvText.visibility = View.GONE
                }

                adapterProduct = AdapterProduct(::onEditButtonClicked)
                binding.rvProducts.adapter = adapterProduct
                adapterProduct.differ.submitList(it)

                //Search
                adapterProduct.originalList = it as ArrayList<Product>

                binding.shimmerViewContainer.visibility = View.GONE
            }
        }
    }

    //set cat dropdown
    private fun setCategories() {
        val categoryList = ArrayList<Categories>()

        for (i in 0 until Constants.allProductsCategoryIcon.size){
            categoryList.add(Categories(Constants.allProductsCategory[i], Constants.allProductsCategoryIcon[i]))
        }

        // set this list in recycler view
        binding.rvCategories.adapter = CategoriesAdapter(categoryList, ::onCategoryClicked)
    }

    //get cat Dropdown
    private fun onCategoryClicked(categories: Categories){
        getAllTheProducts(categories.category)
    }

    //Edit product details
    private fun onEditButtonClicked(product: Product){
        val editProduct = EditProductLayoutBinding.inflate(LayoutInflater.from(requireContext()))
        editProduct.apply {
            etProductTitle.setText(product.productTitle)
            etProductQuantity.setText(product.productQuantity.toString())
            etProductUnit.setText(product.productUnit)
            etProductPrice.setText(product.productPrice.toString())
            etProductStock.setText(product.productStock.toString())
            etProductCategory.setText(product.productCategory)
            etProductType.setText(product.productType)
        }

        //open edit layout in alert view
        val alertDialog = AlertDialog.Builder(requireContext())
            .setView(editProduct.root)
            .create()
        alertDialog.show()

        // enable typing
        editProduct.btnEdit.setOnClickListener {
            editProduct.apply {
                etProductTitle.isEnabled = true
                etProductQuantity.isEnabled = true
                etProductUnit.isEnabled = true
                etProductPrice.isEnabled = true
                etProductStock.isEnabled = true
                etProductCategory.isEnabled = true
                etProductType.isEnabled = true
            }

            setAutoCompleteTextViews(editProduct)

            //Save button clicked
            editProduct.btnSave.setOnClickListener {
               lifecycleScope.launch {
                   product.productTitle = editProduct.etProductTitle.text.toString()
                   product.productQuantity = editProduct.etProductQuantity.text.toString().toInt()
                   product.productUnit = editProduct.etProductUnit.text.toString()
                   product.productPrice = editProduct.etProductPrice.text.toString().toInt()
                   product.productStock = editProduct.etProductStock.text.toString().toInt()
                   product.productCategory = editProduct.etProductCategory.text.toString()
                   product.productType = editProduct.etProductType.text.toString()

                   //Save product
                   viewModel.savingUpdateProduct(product)
               }

                Utils.showToast(requireContext(), "Saved Changes!")
                alertDialog.dismiss()
            }
        }
    }

    // list dropdown
    private fun setAutoCompleteTextViews(editProduct: EditProductLayoutBinding) {
        val units = ArrayAdapter(requireContext(), R.layout.show_list, Constants.allUnitsOfProducts)
        val category = ArrayAdapter(requireContext(), R.layout.show_list, Constants.allProductsCategory)
        val productType = ArrayAdapter(requireContext(), R.layout.show_list, Constants.productType)

        // set this xml
        editProduct.apply {
            etProductUnit.setAdapter(units)
            etProductCategory.setAdapter(category)
            etProductType.setAdapter(productType)
        }
    }

    // change notification status color
    private fun changeStatusBarColor(){
        activity?.window?.apply {
            val statusBarColors = ContextCompat.getColor(requireContext(), R.color.yellow)
            @Suppress("DEPRECATION")
            statusBarColor = statusBarColors
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                @Suppress("DEPRECATION")
                decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        }
    }
}