package com.example.birthday

import android.content.Context
import android.telephony.SmsManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.firebase.database.FirebaseDatabase

class WishWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    private val database = FirebaseDatabase.getInstance().reference

    override fun doWork(): Result {
        val name = inputData.getString("name") ?: return Result.failure()
        val mobile = inputData.getString("mobile") ?: return Result.failure()
        val message = inputData.getString("message") ?: return Result.failure()
        val userId = inputData.getString("userId") ?: return Result.failure()
        val category = inputData.getString("category") ?: return Result.failure()
        val wishKey = inputData.getString("wishKey") ?: return Result.failure()

        return try {
            sendSMS(mobile, "Dear $name, $message")

            // Remove the scheduled wish from the database
            database.child("users").child(userId).child("wishes").child(category).child(wishKey)
                .removeValue()
                .addOnSuccessListener { /* Success */ }
                .addOnFailureListener { /* Log or handle error */ }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }

    private fun sendSMS(phoneNumber: String, message: String) {
        val smsManager = SmsManager.getDefault()
        smsManager.sendTextMessage(phoneNumber, null, message, null, null)
    }
}
