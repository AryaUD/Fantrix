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
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class OnboardingStage {
    SPLASH,
    WELCOME,
    LOGIN,
    SIGN_UP
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

    // ── Real-time field error states ──────────────────────────────────────────
    var emailError by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // ✅ FIX 1: Check if user is already logged in during splash
    // If logged in → skip onboarding, go straight to home (or profile completion)
    LaunchedEffect(Unit) {
        delay(1800)
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // User is already logged in — skip to home
            checkUserProfileAndNavigate(currentUser.uid, navController)
        } else {
            // Not logged in — show welcome screen
            stage = OnboardingStage.WELCOME
        }
    }

    BackHandler(enabled = stage == OnboardingStage.LOGIN || stage == OnboardingStage.SIGN_UP) {
        stage = OnboardingStage.WELCOME
        emailError = ""
        passwordError = ""
    }

    /* ---------------- GOOGLE SIGN IN ---------------- */

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

                auth.signInWithCredential(credential)
                    .addOnCompleteListener { authResult ->
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

    /* ---------------- UI ---------------- */

    val fantrixOffsetY by animateDpAsState(
        targetValue = if (stage == OnboardingStage.SPLASH) 0.dp else (-120).dp,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "FantrixOffset"
    )

    Scaffold(snackbarHost = { SnackbarHost(hostState = snackbarHostState) }) { paddingValues ->

        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

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

                /* ---------------- SPLASH ---------------- */
                AnimatedVisibility(stage == OnboardingStage.SPLASH) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
                    }
                }

                /* ---------------- WELCOME ---------------- */
                AnimatedVisibility(stage == OnboardingStage.WELCOME) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 200.dp),
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

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Divider(modifier = Modifier.weight(1f), color = Color.White.copy(0.3f))
                            Text(" OR ", modifier = Modifier.padding(horizontal = 8.dp), color = Color.White)
                            Divider(modifier = Modifier.weight(1f), color = Color.White.copy(0.3f))
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

                /* ---------------- LOGIN / SIGN UP CARD ---------------- */
                AnimatedVisibility(stage == OnboardingStage.LOGIN || stage == OnboardingStage.SIGN_UP) {

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp)
                            .imePadding(),
                        shape = RoundedCornerShape(26.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            Text(
                                text = "← Back",
                                modifier = Modifier
                                    .align(Alignment.Start)
                                    .clickable {
                                        stage = OnboardingStage.WELCOME
                                        email = ""
                                        password = ""
                                        passwordVisible = false
                                        emailError = ""
                                        passwordError = ""
                                    }
                            )

                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = if (stage == OnboardingStage.LOGIN)
                                    "Login to your account"
                                else
                                    "Create a new account",
                                fontSize = 22.sp
                            )

                            Spacer(Modifier.height(20.dp))

                            // ── Email field ───────────────────────────────────
                            OutlinedTextField(
                                value = email,
                                onValueChange = {
                                    email = it
                                    // ✅ FIX 2: Validate email in real time
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
                                    if (emailError.isNotEmpty()) {
                                        Text(emailError, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            )

                            Spacer(Modifier.height(14.dp))

                            // ── Password field ────────────────────────────────
                            OutlinedTextField(
                                value = password,
                                onValueChange = {
                                    password = it
                                    // ✅ FIX 2: Validate password in real time
                                    passwordError = when {
                                        it.isBlank() -> ""
                                        it.length < 6 -> "Password must be at least 6 characters"
                                        !it.any { c -> c.isDigit() } ->
                                            "Password must contain at least one number"
                                        !it.any { c -> c.isLetter() } ->
                                            "Password must contain at least one letter"
                                        else -> ""
                                    }
                                },
                                label = { Text("Password") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                singleLine = true,
                                isError = passwordError.isNotEmpty(),
                                supportingText = {
                                    if (passwordError.isNotEmpty()) {
                                        Text(passwordError, color = MaterialTheme.colorScheme.error)
                                    }
                                },
                                visualTransformation = if (passwordVisible)
                                    VisualTransformation.None
                                else
                                    PasswordVisualTransformation(),
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

                            // Forgot password
                            if (stage == OnboardingStage.LOGIN) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "Forgot password?",
                                    modifier = Modifier
                                        .align(Alignment.End)
                                        .clickable {
                                            if (email.isBlank()) {
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar("Please enter your email first")
                                                }
                                            } else {
                                                auth.sendPasswordResetEmail(email)
                                                    .addOnCompleteListener { task ->
                                                        coroutineScope.launch {
                                                            if (task.isSuccessful) {
                                                                snackbarHostState.showSnackbar("Password reset email sent to $email")
                                                            } else {
                                                                snackbarHostState.showSnackbar(
                                                                    task.exception?.message ?: "Failed to send reset email"
                                                                )
                                                            }
                                                        }
                                                    }
                                            }
                                        }
                                        .padding(top = 4.dp, end = 4.dp),
                                    color = Color(0xFF2196F3),
                                    fontSize = 12.sp
                                )
                            }

                            Spacer(Modifier.height(22.dp))

                            // ── Login / Sign Up button ────────────────────────
                            Button(
                                onClick = {
                                    // Final validation before calling Firebase
                                    if (email.isBlank()) {
                                        emailError = "Email cannot be empty"
                                        return@Button
                                    }
                                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                                        emailError = "Enter a valid email address"
                                        return@Button
                                    }
                                    if (password.isBlank()) {
                                        passwordError = "Password cannot be empty"
                                        return@Button
                                    }
                                    if (password.length < 6) {
                                        passwordError = "Password must be at least 6 characters"
                                        return@Button
                                    }
                                    if (!password.any { it.isDigit() }) {
                                        passwordError = "Password must contain at least one number"
                                        return@Button
                                    }
                                    if (!password.any { it.isLetter() }) {
                                        passwordError = "Password must contain at least one letter"
                                        return@Button
                                    }

                                    isLoading = true

                                    if (stage == OnboardingStage.LOGIN) {
                                        auth.signInWithEmailAndPassword(email, password)
                                            .addOnCompleteListener { task ->
                                                isLoading = false
                                                if (task.isSuccessful) {
                                                    val user = task.result?.user
                                                    if (user != null) {
                                                        checkUserProfileAndNavigate(user.uid, navController)
                                                    }
                                                } else {
                                                    // ✅ Show specific Firebase error as toast
                                                    val errorMessage = when {
                                                        task.exception?.message?.contains("invalid-email", ignoreCase = true) == true ->
                                                            "Invalid email address"
                                                        task.exception?.message?.contains("wrong-password", ignoreCase = true) == true ->
                                                            "Wrong password. Please try again"
                                                        task.exception?.message?.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) == true ->
                                                            "Wrong email or password. Please try again"
                                                        task.exception?.message?.contains("user-not-found", ignoreCase = true) == true ->
                                                            "No account found with this email"
                                                        task.exception?.message?.contains("too-many-requests", ignoreCase = true) == true ->
                                                            "Too many failed attempts. Try again later"
                                                        task.exception?.message?.contains("user-disabled", ignoreCase = true) == true ->
                                                            "This account has been disabled"
                                                        task.exception?.message?.contains("network", ignoreCase = true) == true ->
                                                            "No internet connection. Check your network"
                                                        else ->
                                                            "Wrong email or password"
                                                    }
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar(errorMessage)
                                                    }
                                                }
                                            }
                                    } else {
                                        auth.createUserWithEmailAndPassword(email, password)
                                            .addOnCompleteListener { task ->
                                                isLoading = false
                                                if (task.isSuccessful) {
                                                    val user = task.result?.user
                                                    if (user != null) {
                                                        saveUserData(user.uid, email.split("@")[0], email, "")
                                                        navController.navigate("userDetails") {
                                                            popUpTo("onboarding") { inclusive = true }
                                                        }
                                                    }
                                                } else {
                                                    val errorMessage = when {
                                                        task.exception?.message?.contains("email-already-in-use", ignoreCase = true) == true ->
                                                            "An account already exists with this email. Please login instead"
                                                        task.exception?.message?.contains("weak-password", ignoreCase = true) == true ->
                                                            "Password is too weak. Use at least 6 characters with letters and numbers"
                                                        task.exception?.message?.contains("invalid-email", ignoreCase = true) == true ->
                                                            "Invalid email address"
                                                        task.exception?.message?.contains("network", ignoreCase = true) == true ->
                                                            "No internet connection. Check your network"
                                                        else ->
                                                            task.exception?.localizedMessage ?: "Signup failed"
                                                    }
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar(errorMessage)
                                                    }
                                                }
                                            }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isLoading && emailError.isEmpty() && passwordError.isEmpty()
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        color = Color.White,
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
                                else
                                    "Already have an account? Login",
                                modifier = Modifier.clickable {
                                    stage = if (stage == OnboardingStage.LOGIN)
                                        OnboardingStage.SIGN_UP
                                    else
                                        OnboardingStage.LOGIN
                                    email = ""
                                    password = ""
                                    passwordVisible = false
                                    emailError = ""
                                    passwordError = ""
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
    val firestore = Firebase.firestore
    firestore.collection("users").document(userId).get()
        .addOnSuccessListener { document ->
            if (document.exists() && document.getBoolean("profileCompleted") == true) {
                navController.navigate("home") {
                    popUpTo("onboarding") { inclusive = true }
                }
            } else {
                navController.navigate("userDetails") {
                    popUpTo("onboarding") { inclusive = true }
                }
            }
        }
        .addOnFailureListener {
            navController.navigate("userDetails") {
                popUpTo("onboarding") { inclusive = true }
            }
        }
}

private fun saveUserData(userId: String, username: String, email: String, profileImage: String) {
    val firestore = Firebase.firestore
    val userData = hashMapOf(
        "fullName" to username,
        "email" to email,
        "profileImage" to profileImage,
        "profileCompleted" to false
    )
    firestore.collection("users").document(userId).set(userData)
}

private fun saveGoogleUserData(userId: String, username: String, email: String, profileImage: String) {
    val firestore = Firebase.firestore
    val userData = hashMapOf(
        "fullName" to username,
        "email" to email,
        "profileImage" to profileImage,
        "profileCompleted" to false
    )
    firestore.collection("users").document(userId).set(userData)
}