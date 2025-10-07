package za.co.studysync

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import za.co.studysync.data.Api
import za.co.studysync.databinding.ActivitySignInBinding
import za.co.studysync.i18n.LocaleManager   // <-- add this import

class SignInActivity : ComponentActivity() {

    // Apply saved locale before resources inflate
    override fun attachBaseContext(newBase: Context) {
        val lang = LocaleManager.getSavedLanguage(newBase)
        val ctx = LocaleManager.applyLocale(newBase, lang)
        super.attachBaseContext(ctx)
    }

    private lateinit var binding: ActivitySignInBinding

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account: GoogleSignInAccount = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken.isNullOrBlank()) {
                showMessage("Google returned no ID token. Check Web Client ID in strings.xml")
                setUiEnabled(true)
                return@registerForActivityResult
            }

            // Exchange Google ID token -> our API JWT (keep in memory only)
            lifecycleScope.launch {
                try {
                    val resp = withContext(Dispatchers.IO) {
                        Api.svc.authGoogle(mapOf("idToken" to idToken))
                    }
                    Api.setToken(resp.access_token) // in-memory for this session only
                    goToMain()
                } catch (e: Exception) {
                    showMessage("Auth failed: ${e.localizedMessage ?: "network error"}")
                    setUiEnabled(true)
                }
            }

        } catch (e: ApiException) {
            showMessage("Sign-in failed: ${e.statusCode}")
            setUiEnabled(true)
        } catch (e: Exception) {
            showMessage("Sign-in error: ${e.localizedMessage ?: "Unknown"}")
            setUiEnabled(true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 🔒 Always force a fresh sign-in on app open
        Api.clearToken()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        GoogleSignIn.getClient(this, gso).signOut()

        binding.btnGoogle.setOnClickListener {
            setUiEnabled(false)
            launchGoogleSignIn()
        }
    }

    private fun launchGoogleSignIn() {
        val webClientId = getString(R.string.server_client_id)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)  // IMPORTANT: Web client ID
            .requestEmail()
            .build()

        val client = GoogleSignIn.getClient(this, gso)
        signInLauncher.launch(client.signInIntent)
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun setUiEnabled(enabled: Boolean) {
        binding.btnGoogle.isEnabled = enabled
        binding.progress.visibility = if (enabled) android.view.View.GONE else android.view.View.VISIBLE
    }

    private fun showMessage(msg: String) {
        Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
    }
}
