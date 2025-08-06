package com.example.adminblinkitclone.api

import com.example.adminblinkitclone.models.Notification
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface ApiInterface {

    //Cloud Messaging
    @Headers("Content-Type: application/json", "Authorization: key=firebase{Server_key}")
    @POST("fcm/send")
    fun sendNotification(@Body notification : Notification) : Call<Notification>
}

//Notification -> NotificationData -> ApiInterface -> ApiUtilities -> AdminViewModel -> Orders(orderingUserId) -> OrderDetailFragment