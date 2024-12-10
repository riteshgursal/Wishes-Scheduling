package com.example.birthday

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.io.ByteArrayOutputStream
import java.io.InputStream

class HomeActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: WishAdapter
    private val wishList = mutableListOf<UploadActivity.WishData>()
    private val database = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()

    private lateinit var uploadIcon: ImageButton // Upload icon (ImageButton)
    private lateinit var birthdayButton: Button  // Button for Birthday
    private lateinit var anniversaryButton: Button // Button for Anniversary
    private lateinit var editIcon: ImageButton  // Edit icon (ImageButton)
    private lateinit var profileImageView: ImageView // Profile Image (ImageView)

    private val PICK_IMAGE_REQUEST = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = WishAdapter(wishList)
        recyclerView.adapter = adapter

        // Initialize Upload Icon
        uploadIcon = findViewById(R.id.uploadIcon)

        // Initialize Buttons
        birthdayButton = findViewById(R.id.birthdayButton)
        anniversaryButton = findViewById(R.id.anniversaryButton)

        // Initialize Edit Icon and Profile ImageView
        editIcon = findViewById(R.id.editIcon)
        profileImageView = findViewById(R.id.profileImageView)

        // Set click listener to navigate to UploadActivity
        uploadIcon.setOnClickListener {
            val intent = Intent(this, UploadActivity::class.java)
            startActivity(intent)
        }

        // Set click listener to show under-development popup on Edit Icon
        editIcon.setOnClickListener {
            Toast.makeText(this, "This feature is under development. We will update soon.", Toast.LENGTH_SHORT).show()
        }

        // Set click listener to allow the user to choose a new profile image
        profileImageView.setOnClickListener {
            openImagePicker()
        }

        // Fetch Birthday data when Birthday button is clicked
        birthdayButton.setOnClickListener {
            fetchData("Birthday")
        }

        // Fetch Anniversary data when Anniversary button is clicked
        anniversaryButton.setOnClickListener {
            fetchData("Anniversary")
        }
    }

    override fun onResume() {
        super.onResume()
        // Fetch and display the profile photo from Firebase when the activity is resumed
        fetchProfilePhoto()
    }

    private fun fetchProfilePhoto() {
        val userId = auth.currentUser?.uid ?: return
        val userRef = database.child("users").child(userId)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java)
                if (user?.profilePhoto?.isNotEmpty() == true) {
                    val decodedByteArray = Base64.decode(user.profilePhoto, Base64.DEFAULT)
                    val decodedBitmap = BitmapFactory.decodeByteArray(decodedByteArray, 0, decodedByteArray.size)
                    profileImageView.setImageBitmap(decodedBitmap)
                } else {
                    profileImageView.setImageResource(R.drawable.img_3) // Replace with your default image
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@HomeActivity, "Failed to fetch profile photo: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            val imageUri: Uri = data.data!!
            val bitmap = getBitmapFromUri(imageUri)
            val base64Image = convertBitmapToBase64(bitmap)
            updateProfileImage(base64Image)
        }
    }

    private fun getBitmapFromUri(uri: Uri): Bitmap {
        val inputStream: InputStream? = contentResolver.openInputStream(uri)
        return BitmapFactory.decodeStream(inputStream)
    }

    private fun convertBitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private fun updateProfileImage(base64Image: String) {
        val userId = auth.currentUser?.uid ?: return
        val userRef = database.child("users").child(userId)
        val user = User(name = "User Name", email = "user@example.com", profilePhoto = base64Image)

        userRef.setValue(user).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Profile photo updated successfully", Toast.LENGTH_SHORT).show()
                val decodedByteArray = Base64.decode(base64Image, Base64.DEFAULT)
                val decodedBitmap = BitmapFactory.decodeByteArray(decodedByteArray, 0, decodedByteArray.size)
                profileImageView.setImageBitmap(decodedBitmap)
            } else {
                Toast.makeText(this, "Failed to update profile photo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchData(category: String) {
        val userId = auth.currentUser?.uid ?: return

        database.child("users").child(userId).child("wishes").child(category)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    wishList.clear()
                    for (wishSnapshot in snapshot.children) {
                        val wish = wishSnapshot.getValue(UploadActivity.WishData::class.java)
                        if (wish != null) {
                            wishList.add(wish)
                        }
                    }
                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@HomeActivity, "Failed to fetch data: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
