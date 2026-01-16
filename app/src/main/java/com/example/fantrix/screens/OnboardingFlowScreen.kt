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

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    BackHandler(enabled = stage == OnboardingStage.LOGIN || stage == OnboardingStage.SIGN_UP) {
        stage = OnboardingStage.WELCOME
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

                            val isNewUser =
                                authResult.result?.additionalUserInfo?.isNewUser == true

                            if (isNewUser) {
                                navController.navigate("userDetails") {
                                    popUpTo("onboarding") { inclusive = true }
                                }
                            } else {
                                navController.navigate("home") {
                                    popUpTo("onboarding") { inclusive = true }
                                }
                            }

                        } else {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    authResult.exception?.localizedMessage
                                        ?: "Google login failed"
                                )
                            }
                        }
                    }
            } catch (e: Exception) {
                isGoogleLoading = false
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        e.localizedMessage ?: "Google login failed"
                    )
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

    LaunchedEffect(Unit) {
        delay(1800)
        stage = OnboardingStage.WELCOME
    }

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

                /* ---------------- WELCOME ---------------- */

                AnimatedVisibility(stage == OnboardingStage.WELCOME) {
                    Column(Modifier.fillMaxWidth().padding(top = 200.dp)) {

                        Text("Welcome", fontSize = 28.sp, color = Color.White)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "For the real fans who live every moment of the game",
                            fontSize = 14.sp,
                            color = Color.White.copy(0.7f)
                        )

                        Spacer(Modifier.height(36.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {

                            Button(
                                onClick = { stage = OnboardingStage.LOGIN },
                                modifier = Modifier.weight(1f).height(56.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                                shape = RoundedCornerShape(50)
                            ) { Text("Login") }

                            Button(
                                onClick = { stage = OnboardingStage.SIGN_UP },
                                modifier = Modifier.weight(1f).height(56.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                                shape = RoundedCornerShape(50)
                            ) { Text("Sign Up") }
                        }
                    }
                }

                /* ---------------- AUTH CARD ---------------- */

                AnimatedVisibility(stage == OnboardingStage.LOGIN || stage == OnboardingStage.SIGN_UP) {

                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 20.dp).imePadding(),
                        shape = RoundedCornerShape(26.dp)
                    ) {
                        Column(
                            modifier = Modifier.verticalScroll(rememberScrollState()).padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            Text(
                                "← Back",
                                modifier = Modifier.align(Alignment.Start)
                                    .clickable { stage = OnboardingStage.WELCOME }
                            )

                            Spacer(Modifier.height(8.dp))

                            Text(
                                if (stage == OnboardingStage.LOGIN)
                                    "Login to your account"
                                else
                                    "Create a new account",
                                fontSize = 22.sp
                            )

                            Spacer(Modifier.height(20.dp))

                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Email") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                singleLine = true
                            )

                            Spacer(Modifier.height(14.dp))

                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Password") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                singleLine = true,
                                visualTransformation =
                                    if (passwordVisible) VisualTransformation.None
                                    else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = {
                                        passwordVisible = !passwordVisible
                                    }) {
                                        Icon(
                                            if (passwordVisible)
                                                Icons.Filled.Visibility
                                            else
                                                Icons.Filled.VisibilityOff,
                                            contentDescription = null
                                        )
                                    }
                                }
                            )

                            Spacer(Modifier.height(22.dp))

                            Button(
                                onClick = {

                                    if (email.isBlank() || password.isBlank()) {
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(
                                                "Email and password cannot be empty"
                                            )
                                        }
                                        return@Button
                                    }

                                    isLoading = true

                                    if (stage == OnboardingStage.LOGIN) {

                                        auth.signInWithEmailAndPassword(email, password)
                                            .addOnCompleteListener { task ->
                                                isLoading = false
                                                if (task.isSuccessful) {
                                                    navController.navigate("home") {
                                                        popUpTo("onboarding") { inclusive = true }
                                                    }
                                                } else {
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar(
                                                            task.exception?.localizedMessage
                                                                ?: "Login failed"
                                                        )
                                                    }
                                                }
                                            }

                                    } else {

                                        auth.createUserWithEmailAndPassword(email, password)
                                            .addOnCompleteListener { task ->
                                                isLoading = false
                                                if (task.isSuccessful) {
                                                    navController.navigate("userDetails") {
                                                        popUpTo("onboarding") { inclusive = true }
                                                    }
                                                } else {
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar(
                                                            task.exception?.localizedMessage
                                                                ?: "Signup failed"
                                                        )
                                                    }
                                                }
                                            }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (isLoading)
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(20.dp)
                                    )
                                else
                                    Text("Continue")
                            }

                            Spacer(Modifier.height(14.dp))

                            /* -------- GOOGLE (FORCE ACCOUNT CHOOSER) -------- */

                            OutlinedButton(
                                onClick = {
                                    isGoogleLoading = true
                                    googleClient.signOut().addOnCompleteListener {
                                        isGoogleLoading = false
                                        googleLauncher.launch(googleClient.signInIntent)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isGoogleLoading
                            ) {
                                if (isGoogleLoading) {
                                    CircularProgressIndicator(
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(20.dp)
                                    )
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

                            Spacer(Modifier.height(20.dp))

                            Text(
                                if (stage == OnboardingStage.LOGIN)
                                    "Don’t have an account? Sign Up"
                                else
                                    "Already have an account? Login",
                                modifier = Modifier.clickable {
                                    stage =
                                        if (stage == OnboardingStage.LOGIN)
                                            OnboardingStage.SIGN_UP
                                        else
                                            OnboardingStage.LOGIN
                                }
                            )

                            Spacer(Modifier.height(70.dp))
                        }
                    }
                }
            }
        }
    }
}
