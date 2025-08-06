package com.userBlinkit.api

import com.userBlinkit.models.CheckStatus
import com.userBlinkit.models.Notification
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiInterface {

    //phonepe.
    @GET("apis/pg-sandbox/pg/v1/status/{merchantId}/{transactionId}")
    suspend fun checkStatus(
        @HeaderMap headers: Map<String, String>,
        @Path("merchantId") merchantId: String,
        @Path("transactionId") transactionId: String
        ) : Response<CheckStatus>

    //Cloud Messaging.
    @Headers("Content-Type: application/json", "Authorization: key=firebase{Server_key}")
    @POST("fcm/send")
    fun sendNotification(@Body notification : Notification) : Call<Notification>
}