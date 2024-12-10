package com.example.birthday
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var emailField: EditText
    private lateinit var passwordField: EditText
    private lateinit var loginButton: Button
    private lateinit var forgotPassword: TextView
    private lateinit var signupLink: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        emailField = findViewById(R.id.emailField)
        passwordField = findViewById(R.id.passwordField)
        loginButton = findViewById(R.id.loginButton)
        forgotPassword = findViewById(R.id.forgotPassword)
        signupLink = findViewById(R.id.signupLink)

        // Login Button Click Listener
        loginButton.setOnClickListener {
            val email = emailField.text.toString()
            val password = passwordField.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                loginUser(email, password)
            } else {
                Toast.makeText(this, "Please enter both email and password", Toast.LENGTH_SHORT).show()
            }
        }

        // Signup Link Click Listener
        signupLink.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loginUser(email: String, password: String) {
        // Attempt to sign in the user with email and password
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, navigate to HomeActivity
                    val user: FirebaseUser? = auth.currentUser
                    if (user != null) {
                        // User is logged in, move to HomeActivity
                        val intent = Intent(this, HomeActivity::class.java)
                        startActivity(intent)
                        finish() // Close the login activity
                    }
                } else {
                    // If sign in fails, display a message to the user
                    showUserNotFoundDialog()
                }
            }
    }

    private fun showUserNotFoundDialog() {
        // Create an AlertDialog to show a user not found message
        val builder = AlertDialog.Builder(this)
        builder.setTitle("User Not Found")
        builder.setMessage("The email and password do not match. Please check your credentials or sign up for an account.")
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }
        builder.setNegativeButton("Sign Up") { dialog, _ ->
            // Redirect to Sign Up Activity
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.show()
    }

    override fun onStart() {
        super.onStart()
        // Check if the user is already logged in
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // User is already logged in, navigate to HomeActivity
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
