package com.userBlinkit.auth

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.userBlinkit.R
import com.userBlinkit.activity.UsersMainActivity
import com.userBlinkit.databinding.FragmentOTPBinding
import com.userBlinkit.models.Users
import com.userBlinkit.utils.Utils
import com.userBlinkit.viewModels.AuthViewModal
import kotlinx.coroutines.launch

class OTPFragment : Fragment() {

    private val viewModel: AuthViewModal by viewModels()
    private lateinit var binding: FragmentOTPBinding
    private lateinit var userNumber: String

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
       binding = FragmentOTPBinding.inflate(layoutInflater)

        getUserNumber()// render no
        customizeEnterOTPNo() // move focus
        onBackButtonClicked() // back icon
        sendOTP() // OTP send dialog
        onLogginButtonClicked() // on click
        return binding.root
    }

    private fun onLogginButtonClicked() {
        binding.btnLogin.setOnClickListener {
            Utils.showDialog(requireContext(), "Signing you...")
            val editTexts = arrayOf(binding.etOtp1, binding.etOtp2, binding.etOtp3, binding.etOtp4, binding.etOtp5, binding.etOtp6)
            val otp = editTexts.joinToString("") { it.text.toString() }
            if (otp.length < editTexts.size){
                Utils.showToast(requireContext(), "Please enter right OTP")
            }
            else{
                editTexts.forEach { it.text?.clear(); it.clearFocus() }
                verifyOTP(otp)
            }
        }
    }

    private fun verifyOTP(otp: String) {

        val user = Users(uId = null, userPhoneNumber = userNumber, userAddress = " ")

        viewModel.signInWithPhoneAuthCredential(otp, userNumber, user)

        lifecycleScope.launch {
            viewModel.isSignedInSuccessfully.collect {
                if (it) {
                    Utils.hideDialog()
                    Utils.showToast(requireContext(), "Logged In!")
                    // Optional: Navigate to next screen
                    startActivity(Intent(requireActivity(), UsersMainActivity::class.java))
                    requireActivity().finish()
                }
            }
        }
    }

    private fun sendOTP() {
        Utils.showDialog(requireContext(), "Sending OTP...")
        viewModel.apply {
            sendOTP(userNumber, requireActivity())
            lifecycleScope.launch {
                otpSend.collect {
                    if (it) {
                        Utils.hideDialog()
                        Utils.showToast(requireContext(), "OTP send...")
                    }
                }    // this is suspend function then coroutine function inside declare
            }
        }
    }

    private fun onBackButtonClicked(){
        binding.tbOtpFragment.setNavigationOnClickListener{
            findNavController().navigate(R.id.action_OTPFragment_to_signinFragment )
        }
    }

    private fun customizeEnterOTPNo() {
        val editTexts = arrayOf(binding.etOtp1, binding.etOtp2, binding.etOtp3, binding.etOtp4, binding.etOtp5, binding.etOtp6)
        for (i in editTexts.indices){
            editTexts[i].addTextChangedListener(object: TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { }

                override fun afterTextChanged(s: Editable?) {
                    if (s?.length == 1){
                        if (i < editTexts.size -1){
                            editTexts[i + 1].requestFocus()
                        }
                    } else if (s?.length == 0){
                        if (i > 0){
                            editTexts[i -1].requestFocus()
                        }
                    }
                }
            })
        }
    }

    private fun getUserNumber() {
        val bundle = arguments
        userNumber = bundle?.getString("number").toString() // pass key SigningFragment
        binding.tvUserNumber.text = userNumber
    }
}