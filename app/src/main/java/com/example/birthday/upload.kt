package com.example.birthday

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class UploadActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    private val SMS_PERMISSION_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload)

        // Handle edge-to-edge layout
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize views
        val nameInput: EditText = findViewById(R.id.nameInput)
        val mobileInput: EditText = findViewById(R.id.mobileInput)
        val dateLabel: TextView = findViewById(R.id.dateLabel)
        val timeLabel: TextView = findViewById(R.id.timeLabel)
        val categorySpinner: Spinner = findViewById(R.id.categorySpinner)
        val messageInput: EditText = findViewById(R.id.messageInput)
        val uploadButton: Button = findViewById(R.id.uploadButton)
        val backButton: ImageButton = findViewById(R.id.backButton)

        // Check and request SMS permission
        checkAndRequestPermissions()

        // Date Picker
        dateLabel.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = "$selectedDay/${selectedMonth + 1}/$selectedYear"
                dateLabel.text = selectedDate
            }, year, month, day)

            datePickerDialog.show()
        }
        backButton.setOnClickListener {
            val intent = Intent(this@UploadActivity, HomeActivity::class.java)
            startActivity(intent)
            finish() // Optional: Finish the current activity to remove it from the back stack
        }

        // Time Picker
        timeLabel.setOnClickListener {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            val timePickerDialog = TimePickerDialog(this, { _, selectedHour, selectedMinute ->
                val formattedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                timeLabel.text = formattedTime
            }, hour, minute, true)

            timePickerDialog.show()
        }

        // Category Spinner
        val categories = arrayOf("Select Any", "Birthday", "Anniversary")
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = categoryAdapter

        // Upload Button
        uploadButton.setOnClickListener {
            val name = nameInput.text.toString()
            val mobile = mobileInput.text.toString()
            val date = dateLabel.text.toString()
            val time = timeLabel.text.toString()
            val category = categorySpinner.selectedItem.toString()
            val message = messageInput.text.toString()

            if (name.isEmpty() || mobile.isEmpty() || date == "Choose Date" || time == "Choose Time" || category == "Select Any" || message.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            } else {
                val userId = auth.currentUser?.uid

                if (userId != null) {
                    val wishKey = database.child("users").child(userId).child("wishes").child(category).push().key
                    if (wishKey != null) {
                        val wishData = WishData(name, mobile, date, time, message)

                        // Store the data under the user's ID
                        database.child("users").child(userId).child("wishes").child(category).child(wishKey)
                            .setValue(wishData)
                            .addOnSuccessListener {
                                scheduleWish(wishData, userId, category, wishKey) // Schedule the SMS
                                Toast.makeText(this, "Data Uploaded and SMS Scheduled!", Toast.LENGTH_SHORT).show()
                                // Navigate to HomeActivity after successful upload
                                val intent = Intent(this@UploadActivity, HomeActivity::class.java)
                                startActivity(intent)
                                finish()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Failed to upload data", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun calculateDelay(date: String, time: String): Long {
        val dateTime = "$date $time"
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val targetTime = sdf.parse(dateTime)?.time ?: return 0L
        val currentTime = System.currentTimeMillis()
        return if (targetTime > currentTime) targetTime - currentTime else 0L
    }

    private fun scheduleWish(wishData: WishData, userId: String, category: String, wishKey: String) {
        val delay = calculateDelay(wishData.date, wishData.time)
        val data = Data.Builder()
            .putString("name", wishData.name)
            .putString("mobile", wishData.mobileNumber)
            .putString("message", wishData.message)
            .putString("userId", userId)
            .putString("category", category)
            .putString("wishKey", wishKey)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<WishWorker>()
            .setInputData(data)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(this).enqueue(workRequest)
    }

    private fun checkAndRequestPermissions() {
        if (checkSelfPermission(android.Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(android.Manifest.permission.SEND_SMS),
                SMS_PERMISSION_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "SMS Permission Granted!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "SMS Permission Denied!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Data model class for Wish
    data class WishData(
        var name: String = "",
        var mobileNumber: String = "",
        var date: String = "",
        var time: String = "",
        var message: String = ""
    )
}
