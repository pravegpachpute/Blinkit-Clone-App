package com.example.adminblinkitclone.auth

import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.adminblinkitclone.R
import com.example.adminblinkitclone.databinding.FragmentSigninBinding
import com.example.adminblinkitclone.utils.Utils


class signinFragment : Fragment() {

    private lateinit var binding: FragmentSigninBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {

        binding = FragmentSigninBinding.inflate(layoutInflater)
        changeStatusBarColor()
        getUserNumber() // 10 digit no

        onContinueButtonClick()
        return binding.root
    }

    // click then redirect OTP Screen
    private fun onContinueButtonClick() {
        binding.btnContinue.setOnClickListener {
            val number = binding.etUserNumber.text.toString()

            if (number.isEmpty() || number.length != 10){
                Utils.showToast(requireContext(), "Please enter valid phone number")
            }else{
                val bundle = Bundle()
                bundle.putString("number", number)
                findNavController().navigate(R.id.action_signinFragment_to_OTPFragment, bundle)     // Navigate(switch)
            }
        }
    }

    // Check No length 10
    private fun getUserNumber() {
        binding.etUserNumber.addTextChangedListener (object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }

            override fun onTextChanged(number: CharSequence?, start: Int, before: Int, count: Int) {
                val len = number?.length

                if(len == 10){
                    binding.btnContinue.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.green))
                }else{
                    binding.btnContinue.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.grayish_blue))
                }
            }

            override fun afterTextChanged(s: Editable?) { }

        } )
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