package com.example.adminblinkitclone.viewModels

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.adminblinkitclone.models.Admins
import com.example.adminblinkitclone.utils.Utils
import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.TimeUnit

class AuthViewModal : ViewModel() {


    private val _verificationId = MutableStateFlow<String?>(null)

    private val _otpSend = MutableStateFlow(false)
    val otpSend = _otpSend
    private val _isSignedInSuccessfully = MutableStateFlow(false)
    val isSignedInSuccessfully = _isSignedInSuccessfully

    private val _isACurrentUser = MutableStateFlow(false)
    val isACurrentUser = _isACurrentUser

    init {
        Utils.getAuthInstance().currentUser?.let {
            _isACurrentUser.value = true
        }
    }

    fun sendOTP(userNumber: String, activity: Activity){
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {}

            override fun onVerificationFailed(e: FirebaseException) {}

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken,
            ) {
                _verificationId.value = verificationId
                _otpSend.value = true
            }
        }
        val options = PhoneAuthOptions.newBuilder(Utils.getAuthInstance())
            .setPhoneNumber("+91$userNumber") // Phone number to verify
            .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
            .setActivity(activity) // Activity (for callback binding)
            .setCallbacks(callbacks) // OnVerificationStateChangedCallbacks
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

         fun signInWithPhoneAuthCredential(otp: String, userNumber: String, user: Admins) {
        val credential = PhoneAuthProvider.getCredential(_verificationId.value.toString(), otp)

        //cloud messaging.
             FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                 val token = task.result
                 user.adminToken = token
                 Utils.getAuthInstance().signInWithCredential(credential)
                     .addOnCompleteListener { task ->
                         user.uId = Utils.getCurrentUserId()     // add this line
                         if (task.isSuccessful) {
                             FirebaseDatabase.getInstance().getReference("Admins").child("AdminInfo").child(user.uId!!).setValue(user)
//                             adminToken
                             Log.d("DEBUG_UID", "✅ ADMIN_ID is: $user.uId")
                             _isSignedInSuccessfully.value = true
                         }
                     }
             }

    // comment code here
    }
}

//    fun signInWithPhoneAuthCredential(otp: String, userNumber: String) {
//        val credential = PhoneAuthProvider.getCredential(_verificationId.value ?: "", otp)
//
//        Utils.getAuthInstance().signInWithCredential(credential)
//            .addOnCompleteListener { task ->
//                if (task.isSuccessful) {
//                    val firebaseUser = Utils.getAuthInstance().currentUser
//                    val userId = firebaseUser?.uid
//
//                    if (!userId.isNullOrEmpty()) {
//                        val user = Users(uId = userId, userPhoneNumber = userNumber, userAddress = null)
//
//                        FirebaseDatabase.getInstance().getReference("AllUsers").child("Users").child(userId).setValue(user)
//                            .addOnSuccessListener {
//                                _isSignedInSuccessfully.value = true
//                            }
//                            .addOnFailureListener {
//                                _isSignedInSuccessfully.value = false
//                            }
//                        Log.d("DEBUG_UID", "UID is: $userId")
//
//                    } else {
//                        // UID is null, handle error
//                        _isSignedInSuccessfully.value = false
//                    }
//                } else {
//                    // Sign-in failed
//                    _isSignedInSuccessfully.value = false
//                }
//            }