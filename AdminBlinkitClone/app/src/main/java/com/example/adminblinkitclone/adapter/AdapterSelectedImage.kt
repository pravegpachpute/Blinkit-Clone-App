package com.example.adminblinkitclone.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.example.adminblinkitclone.databinding.ItemViewImageSelectionBinding

class AdapterSelectedImage(val imageUris : ArrayList<Uri>) : RecyclerView.Adapter<AdapterSelectedImage.SelectedImageHolder>() {

    class SelectedImageHolder(val binding: ItemViewImageSelectionBinding) : ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int, ): SelectedImageHolder {
        return SelectedImageHolder(ItemViewImageSelectionBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int {
        return imageUris.size
    }

    override fun onBindViewHolder(holder: SelectedImageHolder, position: Int,) {    // set our image
        val image = imageUris[position]
        holder.binding.apply {
            ivImage.setImageURI(image)
        }

        holder.binding.closeButton.setOnClickListener {
            if(position < imageUris.size){
                imageUris.removeAt(position)
                notifyItemRemoved(position) // update adapter
            }
        }
    }
}