package com.userBlinkit.utils

import android.widget.Filter
import com.userBlinkit.adapters.AdapterProduct
import com.userBlinkit.models.Product
import java.util.Locale

class FilteringProducts(
    val adapter : AdapterProduct,
    val filter : ArrayList<Product>
) : Filter() {

    override fun performFiltering(constraint: CharSequence?): FilterResults? {
        val result = FilterResults()

         if (!constraint.isNullOrEmpty()){
           val filteredList = ArrayList<Product>()  //add this getting match query this list for loop search
           val query = constraint.toString().trim().uppercase(Locale.getDefault()).split(" ")  // Capital or space between two words work query

          // present in this 'filter' category, title, type match my query then show output
          for (products in filter){
              if (query.any {
                      products.productTitle?.uppercase(Locale.getDefault())?.contains(it) == true ||
                              products.productCategory?.uppercase(Locale.getDefault())?.contains(it) == true ||
                              products.productPrice?.toString()?.uppercase(Locale.getDefault())?.contains(it) == true ||
                              products.productType?.uppercase(Locale.getDefault())?.contains(it) == true
                  }){
                  // match any product then show
                  filteredList.add(products)
              }
          }
          result.values = filteredList
          result.count = filteredList.size
      }
      else{
          result.values = filter
          result.count = filter.size
      }

        return result
    }

    override fun publishResults(constraint: CharSequence?, results: FilterResults?, ) {
        adapter.differ.submitList(results?.values as ArrayList<Product>)
    }
}