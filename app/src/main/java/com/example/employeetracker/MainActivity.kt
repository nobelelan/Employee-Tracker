package com.example.employeetracker

import android.content.Intent
import android.content.IntentSender
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.example.employeetracker.databinding.ActivityMainBinding
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlin.math.log

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var oneTapClient: SignInClient
    private lateinit var signUpRequest: BeginSignInRequest
    private lateinit var signInRequest: BeginSignInRequest

    private lateinit var firebaseAuth: FirebaseAuth

    val TAG = "Firebase"

    private val oneTapResult = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()){ result ->
        try {
            val credential = oneTapClient.getSignInCredentialFromIntent(result.data)
            val idToken = credential.googleIdToken
            when{
                idToken != null -> {
//                     Got an ID token from Google. Use it to authenticate
//                     with your backend.
                    val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                    firebaseAuth.signInWithCredential(firebaseCredential)
                        .addOnSuccessListener { authResult ->
                            Log.d(TAG, "Firebase: Log in successful")

                            val firebaseUser = firebaseAuth.currentUser
                            val uid = firebaseUser?.uid
                            val email = firebaseUser?.email
                            Log.d(TAG, "Uid: $uid")
                            Log.d(TAG, "Email: $email")

                            if (authResult.additionalUserInfo!!.isNewUser){
                                Log.d(TAG, "Firebase: account creadted...$email")
                                Toast.makeText(this@MainActivity, "account creadted...$email", Toast.LENGTH_SHORT).show()
                            }
                            else{
                                Log.d(TAG, "Firebase: Existing user...$email")
                                Toast.makeText(this@MainActivity, "Logged in...$email", Toast.LENGTH_SHORT).show()
                            }

                            startActivity(Intent(this, ProfileActivity::class.java))
                            finish()

                        }
                        .addOnFailureListener { e ->
                            Log.d(TAG, "Firebase: login failed due to ${e.message}")
                            Toast.makeText(this@MainActivity, "login failed due to ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    val msg = "idToken: $idToken"
                    Snackbar.make(binding.root, msg, Snackbar.LENGTH_INDEFINITE).show()
                    Log.d("one tap", msg)
                }
                else -> {
                    // Shouldn't happen.
                    Log.d("one tap", "No ID token!")
                    Snackbar.make(binding.root, "No ID token!", Snackbar.LENGTH_INDEFINITE).show()
                }
            }
        }catch (e: ApiException){
            when(e.statusCode){
                CommonStatusCodes.CANCELED -> {
                    Log.d("one tap", "One-tap dialog was closed.")
                    // Don't re-prompt the user.
                    Snackbar.make(binding.root, "One-tap dialog was closed.", Snackbar.LENGTH_INDEFINITE).show()
                }
                CommonStatusCodes.NETWORK_ERROR -> {
                    Log.d("one tap", "One-tap encountered a network error.")
                    // Try again or just ignore.
                    Snackbar.make(binding.root, "One-tap encountered a network error.", Snackbar.LENGTH_INDEFINITE).show()
                }
                else -> {
                    Log.d("one tap", "Couldn't get credential from result." +
                            " (${e.localizedMessage})")
                    Snackbar.make(binding.root, "Couldn't get credential from result.\" +\n" +
                            " (${e.localizedMessage})", Snackbar.LENGTH_INDEFINITE).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        oneTapClient = Identity.getSignInClient(this)

        firebaseAuth = Firebase.auth
        checkUser()

        signUpRequest = BeginSignInRequest.Builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.Builder()
                    .setSupported(true)
                    .setServerClientId(getString(R.string.your_web_client_id))
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            ).build()

        signInRequest = BeginSignInRequest.Builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.Builder()
                    .setSupported(true)
                    .setServerClientId(getString(R.string.your_web_client_id))
                    .setFilterByAuthorizedAccounts(true)
                    .build()
            ).build()

        binding.buttonGoogleSignIn.setOnClickListener {
            displaySignIn()
        }
    }

    private fun checkUser() {
        val firebaseUser = firebaseAuth.currentUser
        if (firebaseUser != null){
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }
    }

    private fun displaySignIn() {
        oneTapClient.beginSignIn(signInRequest)
            .addOnSuccessListener(this) { result ->
                try {
                    val ib = IntentSenderRequest.Builder(result.pendingIntent.intentSender).build()
                    oneTapResult.launch(ib)
                }catch (e: IntentSender.SendIntentException){
                    Log.e("btn click", "Couldn't start One Tap UI: ${e.localizedMessage}")
                }
            }
            .addOnFailureListener(this) { e ->
                // No Google Accounts found. Just continue presenting the signed-out UI.
                displaySignUp()
                Log.d("btn click", "hoho+${e.localizedMessage}")
            }
    }

    private fun displaySignUp() {
        oneTapClient.beginSignIn(signUpRequest)
            .addOnSuccessListener(this) { result ->
                try {
                    val ib = IntentSenderRequest.Builder(result.pendingIntent.intentSender).build()
                    oneTapResult.launch(ib)
                } catch (e: IntentSender.SendIntentException) {
                    Log.e("btn click", "Couldn't start One Tap UI: ${e.localizedMessage}")
                }
            }
            .addOnFailureListener(this) { e ->
                // No Google Accounts found. Just continue presenting the signed-out UI.
                displaySignUp()
                Log.d("btn click", "haha+${e.localizedMessage}")
            }
    }
}