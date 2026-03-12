package com.example.fantrix.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.fantrix.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class OnboardingStage {
    SPLASH, WELCOME, LOGIN, SIGN_UP
}

@Composable
fun OnboardingFlowScreen(navController: NavController) {

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val galada = FontFamily(Font(R.font.galada_regular))

    var stage by remember { mutableStateOf(OnboardingStage.SPLASH) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isGoogleLoading by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // ✅ Auto-login: skip onboarding if already signed in
    LaunchedEffect(Unit) {
        delay(1800)
        val currentUser = auth.currentUser
        if (currentUser != null) {
            checkUserProfileAndNavigate(currentUser.uid, navController)
        } else {
            stage = OnboardingStage.WELCOME
        }
    }

    BackHandler(enabled = stage == OnboardingStage.LOGIN || stage == OnboardingStage.SIGN_UP) {
        stage = OnboardingStage.WELCOME
        emailError = ""
        passwordError = ""
    }

    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(context.getString(R.string.default_web_client_id))
        .requestEmail()
        .build()

    val googleClient: GoogleSignInClient = GoogleSignIn.getClient(context, gso)

    val googleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                isGoogleLoading = true
                auth.signInWithCredential(credential).addOnCompleteListener { authResult ->
                    isGoogleLoading = false
                    if (authResult.isSuccessful) {
                        val user = authResult.result?.user
                        if (user != null) {
                            if (authResult.result?.additionalUserInfo?.isNewUser == true) {
                                saveGoogleUserData(
                                    user.uid,
                                    user.displayName ?: "User",
                                    user.email ?: "",
                                    user.photoUrl?.toString() ?: ""
                                )
                            }
                            checkUserProfileAndNavigate(user.uid, navController)
                        }
                    } else {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                authResult.exception?.localizedMessage ?: "Google login failed"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                isGoogleLoading = false
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(e.localizedMessage ?: "Google login failed")
                }
            }
        }
    }

    val fantrixOffsetY by animateDpAsState(
        targetValue = if (stage == OnboardingStage.SPLASH) 0.dp else (-120).dp,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "FantrixOffset"
    )

    Scaffold(snackbarHost = { SnackbarHost(hostState = snackbarHostState) }) { paddingValues ->

        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

            // Background image — keep as-is since it's the splash/welcome bg
            Image(
                painter = painterResource(R.drawable.mainbg1),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(Modifier.fillMaxSize().background(Color.Black.copy(0.45f)))

            Column(
                Modifier.fillMaxSize().padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(260.dp))

                Text(
                    "Fantrix",
                    fontFamily = galada,
                    fontSize = 46.sp,
                    color = Color.White,
                    modifier = Modifier.offset(y = fantrixOffsetY)
                )

                /* ── SPLASH ── */
                AnimatedVisibility(stage == OnboardingStage.SPLASH) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
                    }
                }

                /* ── WELCOME ── */
                AnimatedVisibility(stage == OnboardingStage.WELCOME) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 200.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Welcome", fontSize = 28.sp, color = Color.White)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "For the real fans who live every moment of the game",
                            fontSize = 14.sp,
                            color = Color.White.copy(0.7f)
                        )
                        Spacer(Modifier.height(36.dp))

                        Button(
                            onClick = { stage = OnboardingStage.LOGIN },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                            shape = RoundedCornerShape(50)
                        ) { Text("Login") }

                        Spacer(Modifier.height(12.dp))

                        Button(
                            onClick = { stage = OnboardingStage.SIGN_UP },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                            shape = RoundedCornerShape(50)
                        ) { Text("Sign Up") }

                        Spacer(Modifier.height(24.dp))

                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Divider(Modifier.weight(1f), color = Color.White.copy(0.3f))
                            Text(" OR ", Modifier.padding(horizontal = 8.dp), color = Color.White)
                            Divider(Modifier.weight(1f), color = Color.White.copy(0.3f))
                        }

                        Spacer(Modifier.height(24.dp))

                        OutlinedButton(
                            onClick = {
                                isGoogleLoading = true
                                googleClient.signOut().addOnCompleteListener {
                                    isGoogleLoading = false
                                    googleLauncher.launch(googleClient.signInIntent)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(50),
                            enabled = !isGoogleLoading
                        ) {
                            if (isGoogleLoading) {
                                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                            } else {
                                Image(
                                    painter = painterResource(R.drawable.ic_google),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text("Continue with Google")
                            }
                        }
                    }
                }

                /* ── LOGIN / SIGN UP CARD ── */
                // Card uses MaterialTheme automatically — matches app theme perfectly
                AnimatedVisibility(stage == OnboardingStage.LOGIN || stage == OnboardingStage.SIGN_UP) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 20.dp).imePadding(),
                        shape = RoundedCornerShape(26.dp)
                        // No explicit colors — Card uses MaterialTheme.colorScheme.surface by default
                    ) {
                        Column(
                            modifier = Modifier.verticalScroll(rememberScrollState()).padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "← Back",
                                modifier = Modifier.align(Alignment.Start).clickable {
                                    stage = OnboardingStage.WELCOME
                                    email = ""; password = ""
                                    passwordVisible = false
                                    emailError = ""; passwordError = ""
                                },
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = if (stage == OnboardingStage.LOGIN) "Login to your account"
                                else "Create a new account",
                                style = MaterialTheme.typography.titleLarge
                            )

                            Spacer(Modifier.height(20.dp))

                            // Email
                            OutlinedTextField(
                                value = email,
                                onValueChange = {
                                    email = it
                                    emailError = when {
                                        it.isBlank() -> ""
                                        !android.util.Patterns.EMAIL_ADDRESS.matcher(it).matches() ->
                                            "Enter a valid email address"
                                        else -> ""
                                    }
                                },
                                label = { Text("Email") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                singleLine = true,
                                isError = emailError.isNotEmpty(),
                                supportingText = {
                                    if (emailError.isNotEmpty())
                                        Text(emailError, color = MaterialTheme.colorScheme.error)
                                }
                            )

                            Spacer(Modifier.height(14.dp))

                            // Password
                            OutlinedTextField(
                                value = password,
                                onValueChange = {
                                    password = it
                                    passwordError = when {
                                        it.isBlank() -> ""
                                        it.length < 6 -> "Password must be at least 6 characters"
                                        !it.any { c -> c.isDigit() } -> "Must contain at least one number"
                                        !it.any { c -> c.isLetter() } -> "Must contain at least one letter"
                                        else -> ""
                                    }
                                },
                                label = { Text("Password") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                singleLine = true,
                                isError = passwordError.isNotEmpty(),
                                supportingText = {
                                    if (passwordError.isNotEmpty())
                                        Text(passwordError, color = MaterialTheme.colorScheme.error)
                                },
                                visualTransformation = if (passwordVisible)
                                    VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            if (passwordVisible) Icons.Filled.Visibility
                                            else Icons.Filled.VisibilityOff,
                                            contentDescription = null
                                        )
                                    }
                                }
                            )

                            if (stage == OnboardingStage.LOGIN) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "Forgot password?",
                                    modifier = Modifier.align(Alignment.End).clickable {
                                        if (email.isBlank()) {
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Please enter your email first")
                                            }
                                        } else {
                                            auth.sendPasswordResetEmail(email).addOnCompleteListener { task ->
                                                coroutineScope.launch {
                                                    if (task.isSuccessful)
                                                        snackbarHostState.showSnackbar("Reset email sent to $email")
                                                    else
                                                        snackbarHostState.showSnackbar(
                                                            task.exception?.message ?: "Failed to send reset email"
                                                        )
                                                }
                                            }
                                        }
                                    }.padding(top = 4.dp, end = 4.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 12.sp
                                )
                            }

                            Spacer(Modifier.height(22.dp))

                            Button(
                                onClick = {
                                    if (email.isBlank()) { emailError = "Email cannot be empty"; return@Button }
                                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) { emailError = "Enter a valid email address"; return@Button }
                                    if (password.isBlank()) { passwordError = "Password cannot be empty"; return@Button }
                                    if (password.length < 6) { passwordError = "Password must be at least 6 characters"; return@Button }
                                    if (!password.any { it.isDigit() }) { passwordError = "Must contain at least one number"; return@Button }
                                    if (!password.any { it.isLetter() }) { passwordError = "Must contain at least one letter"; return@Button }

                                    isLoading = true

                                    if (stage == OnboardingStage.LOGIN) {
                                        auth.signInWithEmailAndPassword(email, password)
                                            .addOnCompleteListener { task ->
                                                isLoading = false
                                                if (task.isSuccessful) {
                                                    task.result?.user?.let {
                                                        checkUserProfileAndNavigate(it.uid, navController)
                                                    }
                                                } else {
                                                    val msg = when {
                                                        task.exception?.message?.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) == true ->
                                                            "Wrong email or password"
                                                        task.exception?.message?.contains("wrong-password", ignoreCase = true) == true ->
                                                            "Wrong password. Please try again"
                                                        task.exception?.message?.contains("user-not-found", ignoreCase = true) == true ->
                                                            "No account found with this email"
                                                        task.exception?.message?.contains("too-many-requests", ignoreCase = true) == true ->
                                                            "Too many failed attempts. Try again later"
                                                        task.exception?.message?.contains("network", ignoreCase = true) == true ->
                                                            "No internet connection"
                                                        else -> "Wrong email or password"
                                                    }
                                                    coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
                                                }
                                            }
                                    } else {
                                        auth.createUserWithEmailAndPassword(email, password)
                                            .addOnCompleteListener { task ->
                                                isLoading = false
                                                if (task.isSuccessful) {
                                                    task.result?.user?.let {
                                                        saveUserData(it.uid, email.split("@")[0], email, "")
                                                        navController.navigate("userDetails") {
                                                            popUpTo("onboarding") { inclusive = true }
                                                        }
                                                    }
                                                } else {
                                                    val msg = when {
                                                        task.exception?.message?.contains("email-already-in-use", ignoreCase = true) == true ->
                                                            "Account already exists. Please login instead"
                                                        task.exception?.message?.contains("weak-password", ignoreCase = true) == true ->
                                                            "Password too weak. Use letters and numbers"
                                                        task.exception?.message?.contains("network", ignoreCase = true) == true ->
                                                            "No internet connection"
                                                        else -> task.exception?.localizedMessage ?: "Signup failed"
                                                    }
                                                    coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
                                                }
                                            }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isLoading && emailError.isEmpty() && passwordError.isEmpty()
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else {
                                    Text(if (stage == OnboardingStage.LOGIN) "Login" else "Sign Up")
                                }
                            }

                            Spacer(Modifier.height(14.dp))

                            Text(
                                text = if (stage == OnboardingStage.LOGIN)
                                    "Don't have an account? Sign Up"
                                else "Already have an account? Login",
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable {
                                    stage = if (stage == OnboardingStage.LOGIN)
                                        OnboardingStage.SIGN_UP else OnboardingStage.LOGIN
                                    email = ""; password = ""
                                    passwordVisible = false
                                    emailError = ""; passwordError = ""
                                }
                            )

                            Spacer(Modifier.height(20.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun checkUserProfileAndNavigate(userId: String, navController: NavController) {
    Firebase.firestore.collection("users").document(userId).get()
        .addOnSuccessListener { document ->
            if (document.exists() && document.getBoolean("profileCompleted") == true) {
                navController.navigate("home") { popUpTo("onboarding") { inclusive = true } }
            } else {
                navController.navigate("userDetails") { popUpTo("onboarding") { inclusive = true } }
            }
        }
        .addOnFailureListener {
            navController.navigate("userDetails") { popUpTo("onboarding") { inclusive = true } }
        }
}

private fun saveUserData(userId: String, username: String, email: String, profileImage: String) {
    Firebase.firestore.collection("users").document(userId).set(
        hashMapOf("fullName" to username, "email" to email, "profileImage" to profileImage, "profileCompleted" to false)
    )
}

private fun saveGoogleUserData(userId: String, username: String, email: String, profileImage: String) {
    Firebase.firestore.collection("users").document(userId).set(
        hashMapOf("fullName" to username, "email" to email, "profileImage" to profileImage, "profileCompleted" to false)
    )
}