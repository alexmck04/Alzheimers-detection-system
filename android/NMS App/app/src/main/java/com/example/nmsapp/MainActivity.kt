package com.example.nmsapp

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Environment
import com.google.firebase.auth.FirebaseAuth
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import android.Manifest
import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontWeight
import com.google.firebase.storage.FirebaseStorage
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.FirebaseApp
import java.util.Calendar
import okhttp3.MediaType.Companion.toMediaType
import java.io.File
import java.io.IOException
import kotlin.random.Random

import androidx.compose.material3.CircularProgressIndicator
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import com.stripe.android.PaymentConfiguration
import okhttp3.RequestBody.Companion.toRequestBody

import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.rememberPaymentSheet
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.util.concurrent.TimeUnit

import androidx.compose.material.icons.filled.Public

import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import androidx.compose.ui.res.stringResource


class MainActivity : ComponentActivity() {

    lateinit var paymentSheet: PaymentSheet

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Firebase init
        FirebaseApp.initializeApp(this)

        // Stripe SDK init
        PaymentConfiguration.init(applicationContext, STRIPE_PUBLISHABLE_KEY)

        // creates PaymentSheet before setContent()
        paymentSheet = PaymentSheet(this) { result ->
            when (result) {
                is PaymentSheetResult.Completed ->
                    StripeUiBus.status.value = "Payment completed. Waiting for activation..."
                is PaymentSheetResult.Canceled ->
                    StripeUiBus.status.value = "Payment cancelled."
                is PaymentSheetResult.Failed ->
                    StripeUiBus.status.value = "Payment failed: ${result.error.message ?: "Unknown error"}"
            }
        }

        setContent {
            App()
        }
    }
}


object StripeUiBus {
    val status = androidx.compose.runtime.mutableStateOf<String?>(null)
}

@Composable
fun AppBackground(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        content()
    }
}
@Composable
fun App() {
    var loggedIn by rememberSaveable { mutableStateOf(false) }
    var showSignUp by rememberSaveable { mutableStateOf(false) }

    when {
        loggedIn -> MainApp()
        showSignUp -> SignUpScreen(
            onSignUpSuccess = { loggedIn = true },
            onBackToLogin = { showSignUp = false }
        )
        else -> LoginScreen(
            onLoginSuccess = { loggedIn = true },
            onSignUpClick = { showSignUp = true }
        )
    }
}

@Composable
fun MainApp(){
    val navController = rememberNavController()

    val navItemList = listOf(
        NavItem("Home", icon = Icons.Default.Home, Screen.Home.route),
        NavItem("News", icon = Icons.Default.Public, Screen.News.route),
        NavItem("Tests", icon = Icons.Default.Update, Screen.Personal.route),
        NavItem("Profile", icon = Icons.Default.AccountCircle, Screen.Profile.route),
        NavItem("Report", icon = Icons.AutoMirrored.Filled.List, Screen.Report.route),
        NavItem("Review", icon = Icons.Default.Star, Screen.Rating.route)


    )

    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    val isSubscribed = rememberSubscriptionActive()

    Scaffold(
        modifier = Modifier
            .fillMaxSize(),
        bottomBar = {
            NavigationBar {
                navItemList.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedIndex == index,
                        onClick = {
                            selectedIndex = index
                            if (navController.currentDestination?.route != item.route) {
                                navController.navigate(item.route) {
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title
                            )
                        },
                        label = { Text(text = item.title) }
                    )
                }
            }

        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Personal.route) {
                SubscriptionGate(
                    hasPaid = isSubscribed,
                    message = "Subscription required to access tests."
                ) {
                    questionaireScreen(
                        onLifestyleClick = { navController.navigate(Screen.Lifestyle.route) },
                        onCognitiveClick = { navController.navigate(Screen.Cognitive.route) },
                        onRecorderClick = { navController.navigate(Screen.ItemList.route) }
                    )
                }
            }
            composable(Screen.Home.route) { HomeScreen() }

            composable(Screen.News.route) { NewsScreen() }

            composable(Screen.Lifestyle.route) {
                SubscriptionGate(
                    hasPaid = isSubscribed,
                    message = "Subscription required to access this feature."
                ) { lifestyle() }
            }
            composable(Screen.Cognitive.route) {
                SubscriptionGate(
                    hasPaid = isSubscribed,
                    message = "Subscription required to access this feature."
                ) { Cognitive() }
            }
            composable(Screen.ItemList.route) {
                SubscriptionGate(
                    hasPaid = isSubscribed,
                    message = "Subscription required to access this feature."
                ) { RecorderScreen() }
            }
            composable(Screen.Profile.route) { ProfileScreen() }
            composable(Screen.Report.route) { ReportScreen() }
            composable(Screen.Rating.route) {
                Rating(
                    onSubmitRating = ::submitRating
                )
            }
        }
    }
}

// questionnaire
@Composable
fun questionaireScreen(
    onLifestyleClick: () -> Unit,
    onCognitiveClick: () -> Unit,
    onRecorderClick: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid

    var latestCognitiveScore by remember { mutableStateOf<Int?>(null) }
    var latestLifestyleRisk by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(userId) {
        userId?.let { uid ->
            FirebaseFirestore.getInstance()
                .collection("tests")
                .document(uid)
                .collection("cognitiveTests")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener { docs ->
                    val score = docs.documents.firstOrNull()?.getLong("score")?.toInt()
                    latestCognitiveScore = score
                }
        }
    }

    // gets latest Lifestyle score
    LaunchedEffect(userId) {
        userId?.let { uid ->
            FirebaseFirestore.getInstance()
                .collection("tests")
                .document(uid)
                .collection("lifestyleTests")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener { docs ->
                    val risk = docs.documents.firstOrNull()?.getLong("score")?.toInt()
                    latestLifestyleRisk = risk
                }
        }
    }
    AppBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Text(
                "Personal Tests",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(24.dp))

            // Lifestyle Questionnaire Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Lifestyle Questionnaire", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    latestLifestyleRisk?.let {
                        Text("Most Recent Risk Score: $it%", fontSize = 16.sp, color = Color.Gray)
                    }
                    Spacer(Modifier.height(12.dp))

                    Text(
                        "Evaluate your daily habits and lifestyle choices to see how they affect your risk factors.",
                        fontSize = 16.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onLifestyleClick) {
                        Text("Start Questionnaire")
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Cognitive Test Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Cognitive Test", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    latestCognitiveScore?.let {
                        Text("Most Recent Score: $it/14", fontSize = 16.sp, color = Color.Gray)
                    }
                    Spacer(Modifier.height(12.dp))

                    Text(
                        "Test your memory, orientation, and attention skills.",
                        fontSize = 16.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onCognitiveClick) {
                        Text("Start Test")
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Audio Recorder", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Record your audio descriptions and upload for AI analysis.",
                        fontSize = 16.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onRecorderClick) {
                        Text("Open Recorder")
                    }
                }
            }
        }
    }
}

sealed class Screen(val route: String) {
    object Home: Screen("home")
    object ItemDetail: Screen("itemDetail")
    object ItemList: Screen("recordAudio")
    object Personal: Screen("personal")
    object Lifestyle: Screen("lifestyle")
    object Cognitive: Screen("cognitive")
    object Profile: Screen("profile")
    object Report: Screen("report")
    object Rating: Screen("rating")
    object News : Screen("news")
}

data class NavItem(
    val title: String,
    val icon: ImageVector,
    val route: String
)
@Composable
fun Rating(
    onSubmitRating: (Int, String, () -> Unit, (Exception) -> Unit) -> Unit
) {
    AppBackground {
        var rating by remember { mutableStateOf(3) }
        var reviewText by remember { mutableStateOf("") }
        var submitMessage by remember { mutableStateOf("") }
        val maxStars = 5

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Rate your experience",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row {
                for (i in 1..maxStars) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Star $i",
                        tint = if (i <= rating) Color(0xFFFFC107) else Color.LightGray,
                        modifier = Modifier
                            .size(32.dp)
                            .clickable { rating = i }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = reviewText,
                onValueChange = { reviewText = it },
                label = { Text("Write a review") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    onSubmitRating(
                        rating,
                        reviewText,
                        {
                            submitMessage = "Rating submitted successfully!"
                            rating = 3
                            reviewText = ""
                        },
                        { e ->
                            submitMessage = "Failed to submit: ${e.message}"
                        }
                    )
                },
                enabled = rating > 0
            ) {
                Text("Submit")
            }

            if (submitMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = submitMessage,
                    color = if (submitMessage.contains("success")) Color.Green else Color.Red
                )
            }
        }
    }
}

fun submitRating(
    rating: Int,
    review: String,
    onSuccess: () -> Unit,
    onFailure: (Exception) -> Unit
) {
    val user = FirebaseAuth.getInstance().currentUser

    if (user == null) {
        onFailure(Exception("User not logged in"))
        return
    }

    val db = FirebaseFirestore.getInstance()

    val data = mapOf(
        "userId" to user.uid,
        "rating" to rating,
        "review" to review,
        "timestamp" to System.currentTimeMillis()
    )

    db.collection("ratings")
        .add(data)
        .addOnSuccessListener {
            Log.d("RatingReview", "Rating saved successfully")
            onSuccess()
        }
        .addOnFailureListener { e ->
            Log.e("RatingReview", "Failed to save rating", e)
            onFailure(e)
        }
}

@Composable
fun HomeScreen() {
    AppBackground {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start

        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {

                    Text("Understanding Alzheimer's", fontSize = 32.sp, fontWeight = FontWeight.Bold)

                    Spacer(Modifier.height(16.dp))

                    Text(
                        "Alzheimer's disease is a type of dementia that affects memory, thinking and behaviour.",
                        fontSize = 18.sp
                    )

                    Spacer(Modifier.height(12.dp))

                    Text("Understanding Dementia", fontSize = 32.sp, fontWeight = FontWeight.Bold)

                    Spacer(Modifier.height(16.dp))

                    Text(
                        "Dementia is a general term for a decline in mental ability severe enough to interfere with daily life. It affects memory, thinking, and social abilities. Early diagnosis and support can help improve quality of life.",
                        fontSize = 18.sp
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        "Common symptoms include memory loss, difficulty communicating, confusion, and changes in behaviour. If you or someone you know shows these signs, consider speaking to a healthcare professional.",
                        fontSize = 18.sp
                    )
                }
            }

        }
    }

}

// PROFILE PAGE

@Composable
fun ProfileScreen() {
    val isSubscribed = rememberSubscriptionActive()
    var paymentStatus by rememberSaveable { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    val auth = FirebaseAuth.getInstance()
    val uid = auth.currentUser?.uid

    var loading by rememberSaveable { mutableStateOf(true) }
    var saving by rememberSaveable { mutableStateOf(false) }
    var status by rememberSaveable { mutableStateOf<String?>(null) }

    var address by rememberSaveable { mutableStateOf("") }
    var dob by rememberSaveable { mutableStateOf("00/00/0000") }
    var dementiaScoreText by rememberSaveable { mutableStateOf("0") }
    var emergencyContactName by rememberSaveable { mutableStateOf("") }
    var emergencyContactNumber by rememberSaveable { mutableStateOf("123 456 7890") }
    var firstName by rememberSaveable { mutableStateOf("") }
    var lastName by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("085 213 5685") }
    var assignedDoctorId by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }

    var doctorName by rememberSaveable { mutableStateOf("") }

    var dobError by rememberSaveable { mutableStateOf<String?>(null) }

    var originalAddress by rememberSaveable { mutableStateOf("") }
    var originalDob by rememberSaveable { mutableStateOf("00/00/0000") }
    var originalDementiaScoreText by rememberSaveable { mutableStateOf("0") }
    var originalEmergencyContactName by rememberSaveable { mutableStateOf("") }
    var originalEmergencyContactNumber by rememberSaveable { mutableStateOf("123 456 7890") }
    var originalFirstName by rememberSaveable { mutableStateOf("") }
    var originalLastName by rememberSaveable { mutableStateOf("") }
    var originalPhone by rememberSaveable { mutableStateOf("085 213 5685") }
    var originalAssignedDoctorId by rememberSaveable { mutableStateOf("") }
    var originalEmail by rememberSaveable { mutableStateOf("") }

    fun snapshotCurrentToOriginal() {
        originalAddress = address
        originalDob = dob
        originalDementiaScoreText = dementiaScoreText
        originalEmergencyContactName = emergencyContactName
        originalEmergencyContactNumber = emergencyContactNumber
        originalFirstName = firstName
        originalLastName = lastName
        originalPhone = phone
        originalAssignedDoctorId = assignedDoctorId
        originalEmail = email
    }

    fun revertToOriginal() {
        address = originalAddress
        dob = originalDob
        dementiaScoreText = originalDementiaScoreText
        emergencyContactName = originalEmergencyContactName
        emergencyContactNumber = originalEmergencyContactNumber
        firstName = originalFirstName
        lastName = originalLastName
        phone = originalPhone
        assignedDoctorId = originalAssignedDoctorId
        email = originalEmail
        dobError = null
        status = null
    }

    fun fetchDoctorName(id: String) {
        if (id.isBlank()) {
            doctorName = ""
            return
        }
        FirebaseFirestore.getInstance()
            .collection("doctors")
            .document(id)
            .get()
            .addOnSuccessListener { doc ->
                val fn = doc.getString("firstName") ?: ""
                val ln = doc.getString("lastName") ?: ""
                doctorName = listOf(fn, ln).filter { it.isNotBlank() }.joinToString(" ").trim()
            }
            .addOnFailureListener {
                doctorName = ""
            }
    }

    LaunchedEffect(uid) {
        if (uid == null) {
            status = "Not logged in"
            loading = false
            return@LaunchedEffect
        }

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                address = doc.getString("address") ?: ""
                dob = doc.getString("dob") ?: "00/00/0000"
                dementiaScoreText = (doc.getLong("dementiaScore")?.toInt() ?: 0).toString()
                emergencyContactName = doc.getString("emergencyContactName") ?: ""
                emergencyContactNumber = doc.getString("emergencyContactNumber") ?: "123 456 7890"
                firstName = doc.getString("firstName") ?: ""
                lastName = doc.getString("lastName") ?: ""
                phone = doc.getString("phone") ?: "085 213 5685"
                assignedDoctorId = doc.getString("assignedDoctorId") ?: ""
                email = doc.getString("email") ?: (auth.currentUser?.email ?: "")
                snapshotCurrentToOriginal()
                fetchDoctorName(assignedDoctorId)
                loading = false
            }
            .addOnFailureListener { e ->
                status = "Failed to load: ${e.message}"
                loading = false
            }
    }

    AppBackground {
        val scroll = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ===== SUBSCRIPTION SECTION =====
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isSubscribed)
                            "Subscription: Active"
                        else
                            "Subscription: Inactive",
                        fontWeight = FontWeight.Bold
                    )

                    if (!isSubscribed) {
                        Spacer(Modifier.height(10.dp))

                        SubscriptionPayButton(
                            onStatus = { paymentStatus = it }
                        )
                    }

                    paymentStatus?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = it,
                            color = if (it.contains("completed", true))
                                Color.Green
                            else
                                Color.Red
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Text("Profile Overview", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))

            if (loading) {
                Text("Loading...", color = Color.Blue)
                return@Column
            }

            @Composable
            fun field(label: String, value: String, onChange: (String) -> Unit) {
                OutlinedTextField(
                    value = value,
                    onValueChange = onChange,
                    label = { Text(text = label) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        disabledContainerColor = Color.White,
                        errorContainerColor = Color.White
                    )
                )
            }

            field("First name", firstName) { firstName = it }
            field("Last name", lastName) { lastName = it }
            field("Email", email) { email = it }
            field("Phone", phone) { phone = it }
            field("Address", address) { address = it }

            OutlinedTextField(
                value = dob,
                onValueChange = {
                    dob = it
                    dobError = if (dob.isBlank() || dob == "00/00/0000") null else "DOB must be DD/MM/YYYY"
                },
                label = { Text("DOB (DD/MM/YYYY)") },
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    disabledContainerColor = Color.White,
                    errorContainerColor = Color.White
                )
            )
            dobError?.let {
                Text(it, color = Color.Red, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp))
            }

            OutlinedTextField(
                value = dementiaScoreText,
                onValueChange = { },
                label = { Text("DementiaScore") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    disabledContainerColor = Color.White,
                    errorContainerColor = Color.White,
                    disabledTextColor = Color.Black,
                    disabledLabelColor = Color.Gray
                )
            )

            field("Emergency contact name", emergencyContactName) { emergencyContactName = it }
            field("Emergency contact number", emergencyContactNumber) { emergencyContactNumber = it }

            if (doctorName.isNotBlank()) {
                OutlinedTextField(
                    value = doctorName,
                    onValueChange = { },
                    label = { Text("Assigned Doctor") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        disabledContainerColor = Color.White,
                        errorContainerColor = Color.White,
                        disabledTextColor = Color.Black,
                        disabledLabelColor = Color.Gray
                    )
                )
            } else {
                OutlinedTextField(
                    value = "Not assigned",
                    onValueChange = { },
                    label = { Text("Assigned Doctor") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        disabledContainerColor = Color.White,
                        errorContainerColor = Color.White,
                        disabledTextColor = Color.Black,
                        disabledLabelColor = Color.Gray
                    )
                )
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = {
                        revertToOriginal()
                    },
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        val safeUid = uid ?: return@Button
                        status = null

                        if (dob.isNotBlank() && dob != "00/00/0000") {
                            dobError = "DOB must be DD/MM/YYYY"
                            status = "Please fix DOB"
                            return@Button
                        }

                        saving = true

                        val dementiaScore = dementiaScoreText.toIntOrNull() ?: 0

                        val data = mapOf(
                            "firstName" to firstName.trim(),
                            "lastName" to lastName.trim(),
                            "email" to email.trim(),
                            "phone" to phone.trim(),
                            "address" to address.trim(),
                            "dob" to dob.trim(),
                            "dementiaScore" to dementiaScore,
                            "emergencyContactName" to emergencyContactName.trim(),
                            "emergencyContactNumber" to emergencyContactNumber.trim(),
                            "updatedAt" to System.currentTimeMillis()
                        )

                        FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(safeUid)
                            .set(data, com.google.firebase.firestore.SetOptions.merge())
                            .addOnSuccessListener {
                                saving = false
                                status = "Saved"
                                snapshotCurrentToOriginal()
                                fetchDoctorName(assignedDoctorId)
                            }
                            .addOnFailureListener { e ->
                                saving = false
                                status = "Save failed: ${e.message}"
                            }
                    },
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                ) {
                    Text(if (saving) "Saving..." else "Save Changes")
                }
            }

            status?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, color = if (it == "Saved") Color.Green else Color.Red)
            }

            // SUPPORT
            Spacer(Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Support", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))

                    Button(onClick = { dialSupport(context) }) {
                        Text("Call Support (555 999 1000)")
                    }

                    Spacer(Modifier.height(16.dp))

                    var supportSubject by rememberSaveable { mutableStateOf("") }
                    var supportMessage by rememberSaveable { mutableStateOf("") }
                    var supportStatus by rememberSaveable { mutableStateOf<String?>(null) }
                    var supportSending by rememberSaveable { mutableStateOf(false) }

                    OutlinedTextField(
                        value = supportSubject,
                        onValueChange = { supportSubject = it },
                        label = { Text("Support subject") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            disabledContainerColor = Color.White,
                            errorContainerColor = Color.White
                        )
                    )

                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = supportMessage,
                        onValueChange = { supportMessage = it },
                        label = { Text("Describe the issue") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 6,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            disabledContainerColor = Color.White,
                            errorContainerColor = Color.White
                        )
                    )

                    Spacer(Modifier.height(10.dp))

                    Button(
                        onClick = {
                            supportStatus = null
                            supportSending = true

                            submitSupportRequest(
                                subject = supportSubject,
                                message = supportMessage,
                                onSuccess = {
                                    supportSending = false
                                    supportStatus = "Support request sent ✅"
                                    supportSubject = ""
                                    supportMessage = ""
                                },
                                onFailure = { e ->
                                    supportSending = false
                                    supportStatus = "Failed to send: ${e.message}"
                                }
                            )
                        },
                        enabled = !supportSending && (supportSubject.isNotBlank() || supportMessage.isNotBlank())
                    ) {
                        Text(if (supportSending) "Sending..." else "Send Support Request")
                    }

                    supportStatus?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = if (it.contains("✅")) Color.Green else Color.Red)
                    }
                }
            }

        }
    }
}

//record audio class
class AudioRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var outputFile: String = ""

    fun startRecording(): String {
        val outputDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        val output = File(outputDir, "recording_${System.currentTimeMillis()}.mp3")
        outputFile = output.absolutePath

        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4) // This is standard for mobile MP4/MP3 containers
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)    // Standard for mobile audio
            setOutputFile(outputFile)
            prepare()
            start()
        }

        return outputFile
    }

    fun stopRecording() {
        recorder?.apply {
            stop()
            release()
        }
        recorder = null
    }
}
fun SaveMP3(filePath: String,
            onSuccess: (String) -> Unit,
            onFailure: (Exception) -> Unit) {

    val file = File(filePath)
    if (!file.exists()) {
        onFailure(Exception("File does not exist"))
        return
    }

    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    if (currentUser == null) {
        onFailure(Exception("User not logged in"))
        return
    }

    val userId = currentUser.uid
    val db = FirebaseFirestore.getInstance()

    // checks if this file was already uploaded by this user
    db.collection("audios")
        .whereEqualTo("userId", userId)
        .whereEqualTo("fileName", file.name)
        .get()
        .addOnSuccessListener { querySnapshot ->
            if (!querySnapshot.isEmpty) {
                onFailure(Exception("File already uploaded"))
                return@addOnSuccessListener
            }

            // Upload the file to Firebase Storage
            val storage = FirebaseStorage.getInstance()
            val storageRef = storage.reference
            val fileUri = Uri.fromFile(file)
            val storageFileName = "audio/$userId/${file.name}"
            val audioRef = storageRef.child(storageFileName)

            audioRef.putFile(fileUri)
                .addOnSuccessListener {
                    audioRef.downloadUrl.addOnSuccessListener { uri ->
                        val audioData = mapOf(
                            "userId" to userId,
                            "fileName" to file.name,
                            "url" to uri.toString(),
                            "timestamp" to System.currentTimeMillis()
                        )
                        db.collection("audios")
                            .add(audioData)
                            .addOnSuccessListener { onSuccess(uri.toString()) }
                            .addOnFailureListener { e -> onFailure(e) }
                    }.addOnFailureListener { e -> onFailure(e) }
                }
                .addOnFailureListener { e -> onFailure(e) }
        }
        .addOnFailureListener { e ->
            onFailure(e)
        }

}

@Composable
fun RecorderScreen() {
    val imageList = listOf(
        R.drawable.beach, R.drawable.cow, R.drawable.gardener,
        R.drawable.london, R.drawable.paris, R.drawable.prague, R.drawable.street
    )

    AppBackground {
        val context = LocalContext.current
        val recorder = remember { AudioRecorder(context) }

        var isRecording by rememberSaveable { mutableStateOf(false) }
        var recordedFilePath by rememberSaveable { mutableStateOf<String?>(null) }
        var isPlaying by rememberSaveable { mutableStateOf(false) }

        var aiScore by rememberSaveable { mutableStateOf<Double?>(null) }
        var loadingAi by rememberSaveable { mutableStateOf(false) }
        var aiError by rememberSaveable { mutableStateOf<String?>(null) }
        var uploadStatus by rememberSaveable { mutableStateOf("") }

        var imageIndex by rememberSaveable { mutableStateOf(0) }
        var mediaPlayer: MediaPlayer? by remember { mutableStateOf(null) }

        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { granted ->
                if (granted) {
                    recordedFilePath = recorder.startRecording()
                    isRecording = true
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Image
            Image(
                painter = painterResource(id = imageList[imageIndex]),
                contentDescription = "Target Image",
                modifier = Modifier
                    .size(300.dp)
                    .clip(RoundedCornerShape(16.dp))
            )

            Text("Describe the photo in detail", modifier = Modifier.padding(8.dp))

            Button(onClick = {
                imageIndex = (imageIndex + 1) % imageList.size
            }) {
                Text("Change Image")
            }

            Spacer(Modifier.height(24.dp))

            // Recording controls
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = {
                    if (isRecording) {
                        recorder.stopRecording()
                        isRecording = false
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }) {
                    Text(if (isRecording) "Stop Recording" else "Start Recording")
                }

                Button(
                    onClick = {
                        recordedFilePath?.let { path ->
                            if (isPlaying) {
                                mediaPlayer?.stop()
                                isPlaying = false
                            } else {
                                mediaPlayer = MediaPlayer().apply {
                                    setDataSource(path)
                                    prepare()
                                    start()
                                    setOnCompletionListener { isPlaying = false }
                                }
                                isPlaying = true
                            }
                        }
                    },
                    enabled = recordedFilePath != null && !isRecording
                ) {
                    Text(if (isPlaying) "Stop" else "Play")
                }
            }

            Spacer(Modifier.height(24.dp))

            // MAIN ACTION BUTTON
            Button(
                onClick = {
                    val path = recordedFilePath ?: return@Button
                    val audioFile = File(path)

                    loadingAi = true
                    aiScore = null
                    aiError = null
                    uploadStatus = "Analyzing audio..."

                    sendToAi(audioFile) { score, error ->
                        aiScore = score
                        aiError = error
                        loadingAi = false
                        uploadStatus =
                            if (error == null) "Analysis complete"
                            else "Analysis failed"
                    }
                },
                enabled = recordedFilePath != null && !isRecording && !loadingAi,
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text(if (loadingAi) "AI Working..." else "Analyze with AI")
            }

            if (loadingAi) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            }

            Text(uploadStatus, color = Color.Gray, fontSize = 12.sp)

            aiError?.let {
                Text(it, color = Color.Red, modifier = Modifier.padding(8.dp))
            }

            aiScore?.let { score ->
                Text(
                    text = "Risk Score: ${(score * 100).toInt()}%",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = if (score > 0.5) Color.Red else Color.Green,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun Questionnaire(
    questions: List<String> = List(9) { "Question ${it + 1}" },
    onSubmit: (List<String>) -> Unit
) {
    var currentIndex by rememberSaveable { mutableStateOf(0) }
    var showAnswerBox by rememberSaveable { mutableStateOf(false) }
    var answers by rememberSaveable { mutableStateOf(List(questions.size) { "" }) }
    var currentAnswer by rememberSaveable { mutableStateOf("") }

    if (currentIndex >= questions.size) {
        // All questions answered
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Thank you! You have completed the questionnaire.", fontSize = 20.sp)
            Button(onClick = { onSubmit(answers) }, modifier = Modifier.padding(top = 16.dp)) {
                Text("Submit All Answers")
            }
        }
        return
    }

    val question = questions[currentIndex]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!showAnswerBox) {
                    // Show question text
                    Text(question, fontSize = 20.sp)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = {
                        showAnswerBox = true
                        currentAnswer = answers[currentIndex]
                    }) {
                        Text("Answer")
                    }
                } else {
                    // Show answer input
                    Text("Answer the question below:", fontSize = 16.sp)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = currentAnswer,
                        onValueChange = { currentAnswer = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            disabledContainerColor = Color.White,
                            errorContainerColor = Color.White
                        )
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = {
                        // Save the answer
                        answers = answers.toMutableList().apply { set(currentIndex, currentAnswer) }
                        currentIndex++
                        showAnswerBox = false
                        currentAnswer = ""
                    }) {
                        Text(if (currentIndex == questions.size - 1) "Finish" else "Next")
                    }
                }
            }
        }
    }
}
@Composable
fun Cognitive() {
    AppBackground {
        val auth = FirebaseAuth.getInstance()

        val (questions, expectedAnswers) = remember { makeQs() }
        var score by remember { mutableStateOf<Int?>(null) }  // store score to display

        Questionnaire(
            questions = questions,
            onSubmit = { answers ->
                // Calculate score
                val calculatedScore = calculateScore(answers, expectedAnswers)
                score = calculatedScore

                submitCognitiveScore(
                    score = calculatedScore,
                    onSuccess = { println("Score submitted successfully: $calculatedScore") },
                    onFailure = { e -> println("Failed to submit score: ${e.message}") }
                )
            }
        )

        score?.let { userScore ->
            Text(
                text = "Your Risk Score: ${userScore}",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Green,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

    }
}

fun Modifier.Companion.align(centerHorizontally: Alignment.Horizontal) {}

fun makeQs(): Pair<List<String>, Map<String, Any>> {
    val calendar = Calendar.getInstance()
    val currentYear = calendar.get(Calendar.YEAR).toString()
    val currentMonth = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, java.util.Locale.getDefault()) ?: "December"

    // Random numbers for digit span
    val numbers = List(3) { (0..9).random() }
    val numbersForward = numbers.joinToString(" ")
    val numbersBackward = numbers.reversed().joinToString(" ")

    // Random words for immediate and delayed recall
    val allWords = listOf(
        "Apple", "Train", "Chair", "Dog", "House", "River", "Book",
        "Car", "Tree", "Phone", "Bread", "Sun", "Clock", "Shoes",
        "Flower", "Table", "Milk", "Window", "Bird", "Road"
    )
    val testWords = allWords.shuffled().take(3)

    val questions = listOf(
        "What year is it?",
        "What month is it?",
        "Repeat these words: ${testWords.joinToString(", ")}",
        "List as many animals as you can",
        "Repeat these numbers: ${numbersForward.replace(" ", ", ")}",
        "Reverse the numbers: ${numbersBackward.replace(" ", ", ")}",
        "Repeat this sentence: 'The cat jumped over the small fence.'",
        "Recall the words you were asked earlier"
    )

    val answers = mapOf(
        "year" to currentYear,
        "month" to currentMonth,
        "repeatedWords" to testWords,
        "numbersForward" to numbersForward,
        "numbersBackward" to numbersBackward,
        "animals" to listOf(
            "Dog", "Cat", "Horse", "Cow", "Sheep", "Goat", "Pig", "Rabbit", "Mouse", "Rat",
            "Lion", "Tiger", "Elephant", "Bear", "Wolf", "Fox", "Deer", "Monkey", "Giraffe",
            "Zebra", "Kangaroo", "Panda", "Leopard", "Cheetah", "Hippopotamus", "Rhinoceros",
            "Squirrel", "Otter", "Seal", "Walrus", "Dolphin", "Whale", "Shark", "Octopus",
            "Penguin", "Eagle", "Owl", "Parrot", "Swan", "Duck", "Chicken", "Goose", "Turkey",
            "Frog", "Turtle", "Snake", "Lizard", "Crocodile", "Alligator", "Bee", "Butterfly",
            "Ant", "Crab", "Lobster", "Starfish", "Jellyfish", "Horsefly", "Moth", "Fly", "Mosquito"
        ),
        "sentence" to "The cat jumped over the small fence"
    )

    return questions to answers
}

fun calculateScore(
    answers: List<String>,
    expectedAnswers: Map<String, Any>
): Int {
    var score = 0

    // 1. Orientation
    if (answers.getOrNull(0)?.trim()?.equals(expectedAnswers["year"]?.toString(), ignoreCase = true) == true) score++
    if (answers.getOrNull(1)?.trim()?.equals(expectedAnswers["month"]?.toString(), ignoreCase = true) == true) score++

    // 2. Immediate memory (3 words)
    val correctWords = (expectedAnswers["repeatedWords"] as List<String>).map { it.lowercase() }
    val userWords = answers.getOrNull(2)?.split(",", " ")?.map { it.trim().lowercase() } ?: emptyList()
    score += userWords.intersect(correctWords.toSet()).size  // 0–3 points

    // 3. Animals - flexible input
    val correctAnimals = (expectedAnswers["animals"] as List<String>).map { it.lowercase() }
    val userAnimalsRaw = answers.getOrNull(3) ?: ""
    val userAnimals = userAnimalsRaw.split(",", " ").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
    if (userAnimals.intersect(correctAnimals.toSet()).isNotEmpty()) score++  // 0–1 point

    // 4. Digit span - flexible input
    val forwardExpected = (expectedAnswers["numbersForward"] as String).replace(" ", "")
    val backwardExpected = (expectedAnswers["numbersBackward"] as String).replace(" ", "")
    val userForward = answers.getOrNull(4)?.replace(",", "")?.replace(" ", "") ?: ""
    val userBackward = answers.getOrNull(5)?.replace(",", "")?.replace(" ", "") ?: ""
    if (userForward == forwardExpected) score++
    if (userBackward == backwardExpected) score++

    // 5. Sentence repetition
    if (answers.getOrNull(6)?.trim()?.equals(expectedAnswers["sentence"]?.toString(), ignoreCase = true) == true) score++

    // 6. Delayed recall (3 words)
    val delayedWords = answers.getOrNull(7)?.split(",", " ")?.map { it.trim().lowercase() } ?: emptyList()
    score += delayedWords.intersect(correctWords.toSet()).size  // 0–3 points

    return score
}

fun submitCognitiveScore(
    score: Int,
    onSuccess: () -> Unit,
    onFailure: (Exception) -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser

    if (user == null) {
        onFailure(Exception("User not logged in"))
        return
    }

    val userId = user.uid
    val db = FirebaseFirestore.getInstance()

    val data = mapOf(
        "score" to score,
        "timestamp" to System.currentTimeMillis()
    )

    db.collection("tests")
        .document(userId)
        .collection("cognitiveTests")
        .add(data)
        .addOnSuccessListener {
            createTestNotification(
                testType = "cognitive",
                rawScore = score,
                onSuccess = { onSuccess() },
                onFailure = { e -> onFailure(e) }
            )
        }
        .addOnFailureListener { e ->
            onFailure(e)
        }
}

fun submitLifestyleScore(
    score: Int,
    onSuccess: () -> Unit,
    onFailure: (Exception) -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser

    if (user == null) {
        onFailure(Exception("User not logged in"))
        return
    }

    val userId = user.uid
    val db = FirebaseFirestore.getInstance()

    val data = mapOf(
        "score" to score,
        "timestamp" to System.currentTimeMillis()
    )

    db.collection("tests")
        .document(userId)
        .collection("lifestyleTests")
        .add(data)
        .addOnSuccessListener {
            createTestNotification(
                testType = "lifestyle",
                rawScore = score,
                onSuccess = { onSuccess() },
                onFailure = { e -> onFailure(e) }
            )
        }
        .addOnFailureListener { e ->
            onFailure(e)
        }
}

@Composable
fun lifestyle() {
    AppBackground {

        // ================== INT ANSWERS ==================
        var q1 by remember { mutableStateOf<Int?>(null) }
        var q2 by remember { mutableStateOf<Int?>(null) }
        var q3 by remember { mutableStateOf<Int?>(null) }
        var q4 by remember { mutableStateOf<Int?>(null) }
        var q5 by remember { mutableStateOf<Int?>(null) }
        var q6 by remember { mutableStateOf<Int?>(null) }
        var q7 by remember { mutableStateOf<Int?>(null) }
        var q8 by remember { mutableStateOf<Int?>(null) }
        var q9 by remember { mutableStateOf<Int?>(null) }
        var q10 by remember { mutableStateOf<Int?>(null) }

        // ================== TEXT INPUT ==================
        var q1Text by remember { mutableStateOf("") }
        var q2Text by remember { mutableStateOf("") }
        var q3Text by remember { mutableStateOf("") }
        var q4Text by remember { mutableStateOf("") }
        var q5Text by remember { mutableStateOf("") }
        var q6Text by remember { mutableStateOf("") }
        var q7Text by remember { mutableStateOf("") }
        var q8Text by remember { mutableStateOf("") }
        var q9Text by remember { mutableStateOf("") }
        var q10Text by remember { mutableStateOf("") }

        var message by remember { mutableStateOf("") }
        var submitMessage by remember { mutableStateOf("") }


        // HELPERS
        fun parse1to5(input: String): Int? {
            val n = input.toIntOrNull() ?: return null
            return if (n in 1..5) n else null
        }

        fun allAnswered(): Boolean {
            return listOf(q1, q2, q3, q4, q5, q6, q7, q8, q9, q10).all { it != null }
        }

        fun totalScore(): Int? {
            if (!allAnswered()) return null
            return q1!! + q2!! + q3!! + q4!! + q5!! +
                    q6!! + q7!! + q8!! + q9!! + q10!!
        }

        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text("Lifestyle Questionnaire", fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Enter a number from 1 to 5 for each question.")
            Text("1 = Very Poor / Never, 5 = Excellent / Always")
            Spacer(Modifier.height(20.dp))

            val numberKeyboard = KeyboardOptions(keyboardType = KeyboardType.Number)
            val tfColors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                disabledContainerColor = Color.White,
                errorContainerColor = Color.White
            )

            OutlinedTextField(
                value = q1Text,
                onValueChange = { q1Text = it; q1 = parse1to5(it) },
                label = { Text("1) Forget appointments or important tasks? (1–5)") },
                keyboardOptions = numberKeyboard,
                modifier = Modifier.fillMaxWidth(),
                colors = tfColors
            )

            OutlinedTextField(
                value = q2Text,
                onValueChange = { q2Text = it; q2 = parse1to5(it) },
                label = { Text("2) Remembering recent events? (1–5)") },
                keyboardOptions = numberKeyboard,
                modifier = Modifier.fillMaxWidth(),
                colors = tfColors
            )

            OutlinedTextField(
                value = q3Text,
                onValueChange = { q3Text = it; q3 = parse1to5(it) },
                label = { Text("3) Misplacing everyday items? (1–5)") },
                keyboardOptions = numberKeyboard,
                modifier = Modifier.fillMaxWidth(),
                colors = tfColors
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = q4Text,
                onValueChange = { q4Text = it; q4 = parse1to5(it) },
                label = { Text("4) Mentally stimulating activities? (1–5)") },
                keyboardOptions = numberKeyboard,
                modifier = Modifier.fillMaxWidth(),
                colors = tfColors
            )

            OutlinedTextField(
                value = q5Text,
                onValueChange = { q5Text = it; q5 = parse1to5(it) },
                label = { Text("5) Ability to concentrate? (1–5)") },
                keyboardOptions = numberKeyboard,
                modifier = Modifier.fillMaxWidth(),
                colors = tfColors
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = q6Text,
                onValueChange = { q6Text = it; q6 = parse1to5(it) },
                label = { Text("6) Physical exercise frequency? (1–5)") },
                keyboardOptions = numberKeyboard,
                modifier = Modifier.fillMaxWidth(),
                colors = tfColors
            )

            OutlinedTextField(
                value = q7Text,
                onValueChange = { q7Text = it; q7 = parse1to5(it) },
                label = { Text("7) Daily routine consistency? (1–5)") },
                keyboardOptions = numberKeyboard,
                modifier = Modifier.fillMaxWidth(),
                colors = tfColors
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = q8Text,
                onValueChange = { q8Text = it; q8 = parse1to5(it) },
                label = { Text("8) Sleep quality? (1–5)") },
                keyboardOptions = numberKeyboard,
                modifier = Modifier.fillMaxWidth(),
                colors = tfColors
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = q9Text,
                onValueChange = { q9Text = it; q9 = parse1to5(it) },
                label = { Text("9) Social interaction frequency? (1–5)") },
                keyboardOptions = numberKeyboard,
                modifier = Modifier.fillMaxWidth(),
                colors = tfColors
            )

            OutlinedTextField(
                value = q10Text,
                onValueChange = { q10Text = it; q10 = parse1to5(it) },
                label = { Text("10) Stress/anxiety? (1–5, 5 = rarely/never)") },
                keyboardOptions = numberKeyboard,
                modifier = Modifier.fillMaxWidth(),
                colors = tfColors
            )

            Spacer(Modifier.height(20.dp))

            val score = totalScore()

            if (score == null) {
                message = "Please enter numbers 1–5 for all questions."
                Text(message, color = Color.Red)
            } else {
                val lifestylePercent = score * 2
                val riskPercent = 100 - lifestylePercent

                Text("Risk Indicator: $riskPercent%", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Text(
                    "Note: This is not a medical diagnosis.",
                    fontSize = 12.sp
                )
                Button(
                    onClick = {
                        val finalScore = totalScore()
                        if (finalScore != null) {
                            val finalRisk = 100 - (finalScore * 2)
                            submitLifestyleScore(
                                score = finalScore,
                                onSuccess = { submitMessage = "Score submitted successfully!" },
                                onFailure = { e -> submitMessage = "Failed to submit: ${e.message}" }
                            )
                        } else {
                            submitMessage = "Please fill all questions with numbers 1–5"
                        }
                    },
                    enabled = allAnswered()
                ) {
                    Text("Submit Lifestyle Score")
                }

                if (submitMessage.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text(submitMessage, color = if (submitMessage.contains("success")) Color.Green else Color.Red)
                }
            }

        }
    }
}

fun assignRandomDoctorToPatient(
    userId: String,
    onSuccess: (String) -> Unit,
    onFailure: (Exception) -> Unit
) {
    FirebaseFirestore.getInstance()
        .collection("doctors")
        .whereEqualTo("role", "doctor")
        .get()
        .addOnSuccessListener { snap ->
            if (snap.isEmpty) {
                onFailure(Exception("No doctors available"))
                return@addOnSuccessListener
            }

            val validDoctors = snap.documents.filter { doc ->
                val role = doc.getString("role") ?: ""
                val isPending = doc.getBoolean("isPending") ?: false
                role != "admin" && !isPending
            }

            if (validDoctors.isEmpty()) {
                onFailure(Exception("No approved doctors available"))
                return@addOnSuccessListener
            }

            val picked = validDoctors[Random.nextInt(validDoctors.size)]
            val doctorId = picked.id

            FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .set(mapOf("assignedDoctorId" to doctorId), com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener { onSuccess(doctorId) }
                .addOnFailureListener { e -> onFailure(e) }
        }
        .addOnFailureListener { e ->
            onFailure(e)
        }
}

@Composable
fun SignUpScreen(
    onSignUpSuccess: () -> Unit,
    onBackToLogin: () -> Unit
) {
    AppBackground {
        val auth = FirebaseAuth.getInstance()

        var email by rememberSaveable { mutableStateOf("") }
        var password by rememberSaveable { mutableStateOf("") }
        var confirmPassword by rememberSaveable { mutableStateOf("") }
        var error by rememberSaveable { mutableStateOf<String?>(null) }
        var loading by rememberSaveable { mutableStateOf(false) }

        val tfColors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            disabledContainerColor = Color.White,
            errorContainerColor = Color.White
        )

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                Text("Sign Up", fontSize = 32.sp)

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    colors = tfColors
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    colors = tfColors
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    colors = tfColors
                )

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (password != confirmPassword) {
                            error = "Passwords do not match"
                            return@Button
                        }

                        loading = true
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    error = null

                                    val userId = auth.currentUser?.uid
                                    if (userId == null) {
                                        loading = false
                                        error = "Failed to create user"
                                        return@addOnCompleteListener
                                    }

                                    FirebaseFirestore.getInstance()
                                        .collection("users")
                                        .document(userId)
                                        .set(
                                            mapOf(
                                                "email" to email,
                                                "createdAt" to System.currentTimeMillis()
                                            )
                                        )
                                        .addOnSuccessListener {
                                            assignRandomDoctorToPatient(
                                                userId = userId,
                                                onSuccess = {
                                                    loading = false
                                                    onSignUpSuccess()
                                                },
                                                onFailure = { e ->
                                                    loading = false
                                                    error = e.message
                                                }
                                            )
                                        }
                                        .addOnFailureListener { e ->
                                            loading = false
                                            error = e.message
                                        }
                                } else {
                                    loading = false
                                    error = task.exception?.message
                                }
                            }
                    }
                ) {
                    Text("Create Account")
                }

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = onBackToLogin
                ) {
                    Text("Back to Login")
                }

                if (loading) Text("Creating account...", color = Color.Blue)
                if (error != null) Text(error!!, color = Color.Red)
            }
        }
    }
}

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onSignUpClick: () -> Unit
) {
    AppBackground {
        val auth = FirebaseAuth.getInstance()

        var email by rememberSaveable { mutableStateOf("") }
        var password by rememberSaveable { mutableStateOf("") }
        var error by rememberSaveable { mutableStateOf<String?>(null) }
        var loading by rememberSaveable { mutableStateOf(false) }

        val tfColors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            disabledContainerColor = Color.White,
            errorContainerColor = Color.White
        )

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Login", fontSize = 32.sp)

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    colors = tfColors
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    colors = tfColors
                )

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = {
                        loading = true
                        auth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                loading = false
                                if (task.isSuccessful) {
                                    error = null
                                    onLoginSuccess()
                                } else {
                                    error = task.exception?.message ?: "Login failed"
                                }
                            }
                    }
                ) {
                    Text("Login")
                }

                Spacer(Modifier.height(10.dp))

                GoogleSignInButton(
                    onSuccess = {
                        handlePostGoogleSignup(
                            onSuccess = {
                                error = null
                                onLoginSuccess()
                            },
                            onFailure = { msg ->
                                error = msg
                            }
                        )
                    },
                    onError = { msg ->
                        error = msg
                    }
                )


                Spacer(Modifier.height(10.dp))

                Button(onClick = onSignUpClick) {
                    Text("Sign Up")
                }

                if (loading) Text("Loading...", color = Color.Blue)
                if (error != null) Text(error!!, color = Color.Red)
            }
        }
    }
}


//AI CONNECTION

fun getFirebaseIdToken(onTokenReady: (String) -> Unit) {
    val user = FirebaseAuth.getInstance().currentUser
    user?.getIdToken(false)
        ?.addOnSuccessListener { result ->
            onTokenReady(result.token!!)
        }
        ?.addOnFailureListener {

        }
}

fun sendToAi(audioFile: File, onResult: (Double?, String?) -> Unit) {
    val user = FirebaseAuth.getInstance().currentUser
    if (user == null) {
        onResult(null, "User not logged in")
        return
    }

    if (!audioFile.exists()) {
        onResult(null, "Audio file missing")
        return
    }

    SaveMP3(
        filePath = audioFile.absolutePath,
        onSuccess = {
            getFirebaseIdToken { token ->
                val client = OkHttpClient.Builder()
                    .callTimeout(60, TimeUnit.SECONDS)
                    .build()

                val mime = when (audioFile.extension.lowercase()) {
                    "mp3" -> "audio/mpeg"
                    "wav" -> "audio/wav"
                    "m4a" -> "audio/mp4"
                    "aac" -> "audio/aac"
                    "3gp" -> "audio/3gpp"
                    else -> "audio/*"
                }.toMediaType()

                val body = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        "file",
                        audioFile.name,
                        audioFile.asRequestBody(mime)
                    )
                    .build()

                val request = Request.Builder()
                    .url("https://nms-ai-backend-596631887809.europe-west1.run.app/predict/audio")
                    .post(body)
                    .addHeader("Authorization", "Bearer $token")
                    .addHeader("Accept", "application/json")
                    .build()

                Thread {
                    try {
                        client.newCall(request).execute().use { response ->
                            val text = response.body?.string().orEmpty()

                            if (!response.isSuccessful) {
                                onResult(null, "AI ${response.code}: ${text.take(1000)}")
                                return@use
                            }

                            val json = JSONObject(text)
                            val score = json.getDouble("risk_score")
                            onResult(score, null)
                        }
                    } catch (e: Exception) {
                        onResult(null, e.message)
                    }
                }.start()
            }
        },
        onFailure = { e ->
            if ((e.message ?: "").contains("already uploaded", ignoreCase = true)) {
                getFirebaseIdToken { token ->
                    val client = OkHttpClient.Builder()
                        .callTimeout(60, TimeUnit.SECONDS)
                        .build()

                    val mime = when (audioFile.extension.lowercase()) {
                        "mp3" -> "audio/mpeg"
                        "wav" -> "audio/wav"
                        "m4a" -> "audio/mp4"
                        "aac" -> "audio/aac"
                        "3gp" -> "audio/3gpp"
                        else -> "audio/*"
                    }.toMediaType()

                    val body = MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart(
                            "file",
                            audioFile.name,
                            audioFile.asRequestBody(mime)
                        )
                        .build()

                    val request = Request.Builder()
                        .url("https://nms-ai-backend-596631887809.europe-west1.run.app/predict/audio")
                        .post(body)
                        .addHeader("Authorization", "Bearer $token")
                        .addHeader("Accept", "application/json")
                        .build()

                    Thread {
                        try {
                            client.newCall(request).execute().use { response ->
                                val text = response.body?.string().orEmpty()

                                if (!response.isSuccessful) {
                                    onResult(null, "AI ${response.code}: ${text.take(1000)}")
                                    return@use
                                }

                                val json = JSONObject(text)
                                val score = json.getDouble("risk_score")
                                onResult(score, null)
                            }
                        } catch (ex: Exception) {
                            onResult(null, ex.message)
                        }
                    }.start()
                }
            } else {
                onResult(null, e.message ?: "Upload failed")
            }
        }
    )
}


fun createTestNotification(
    testType: String,
    rawScore: Int,
    onSuccess: () -> Unit,
    onFailure: (Exception) -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser ?: run {
        onFailure(Exception("User not logged in"))
        return
    }

    val userId = user.uid
    val db = FirebaseFirestore.getInstance()

    db.collection("users")
        .document(userId)
        .get()
        .addOnSuccessListener { userDoc ->
            val assignedDoctorId = userDoc.getString("assignedDoctorId")
            if (assignedDoctorId.isNullOrBlank()) {
                onFailure(Exception("No assignedDoctorId on user profile"))
                return@addOnSuccessListener
            }

            val riskPercent = (100 - (rawScore * 2)).coerceIn(0, 100)

            val notification = hashMapOf(
                "patientId" to userId,
                "assignedDoctorId" to assignedDoctorId,
                "type" to testType,
                "rawScore" to rawScore,
                "riskPercent" to riskPercent,
                "timestamp" to System.currentTimeMillis(),
                "seen" to false
            )

            db.collection("notifications")
                .add(notification)
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { e -> onFailure(e) }
        }
        .addOnFailureListener { e -> onFailure(e) }
}

@Composable
fun ReportScreen() {
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid

    var loading by rememberSaveable { mutableStateOf(true) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    var dementiaScore by rememberSaveable { mutableStateOf<Long?>(null) }
    var doctorName by rememberSaveable { mutableStateOf<String?>(null) }

    var reportText by rememberSaveable { mutableStateOf<String?>(null) }
    var riskLevel by rememberSaveable { mutableStateOf<String?>(null) }
    var averageRiskPercent by rememberSaveable { mutableStateOf<Long?>(null) }
    var highestRiskPercent by rememberSaveable { mutableStateOf<Long?>(null) }
    var lowestRiskPercent by rememberSaveable { mutableStateOf<Long?>(null) }
    var basedOnTestCount by rememberSaveable { mutableStateOf<Long?>(null) }
    var generatedAt by rememberSaveable { mutableStateOf<Long?>(null) }

    LaunchedEffect(userId) {
        if (userId == null) {
            error = "Not logged in"
            loading = false
            return@LaunchedEffect
        }

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { snap ->
                dementiaScore = snap.getLong("dementiaScore")

                val assignedDoctorId = snap.getString("assignedDoctorId")

                if (assignedDoctorId != null) {
                    FirebaseFirestore.getInstance()
                        .collection("doctors")
                        .document(assignedDoctorId)
                        .get()
                        .addOnSuccessListener { docSnap ->
                            val first = docSnap.getString("firstName") ?: ""
                            val last = docSnap.getString("lastName") ?: ""
                            doctorName = "Dr. $first $last"
                        }
                }

                val report = snap.get("latestReport") as? Map<*, *>

                reportText = report?.get("text") as? String
                riskLevel = report?.get("riskLevel") as? String
                averageRiskPercent = (report?.get("averageRiskPercent") as? Number)?.toLong()
                highestRiskPercent = (report?.get("highestRiskPercent") as? Number)?.toLong()
                lowestRiskPercent = (report?.get("lowestRiskPercent") as? Number)?.toLong()
                basedOnTestCount = (report?.get("basedOnTestCount") as? Number)?.toLong()
                generatedAt = (report?.get("generatedAt") as? Number)?.toLong()

                loading = false
            }
            .addOnFailureListener { e ->
                error = e.message ?: "Failed to load report"
                loading = false
            }
    }

    AppBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("My Report", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))

            if (loading) {
                Text("Loading report...", fontSize = 16.sp)
                return@Column
            }

            if (error != null) {
                Text(error!!, color = Color.Red, fontSize = 16.sp)
                return@Column
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {

                    doctorName?.let {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Assigned Doctor: $it",
                            fontSize = 24.sp,
                            color = Color.Red
                        )
                    }

                    dementiaScore?.let {
                        Text(
                            text = "Current Dementia Score: ${it}%",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(10.dp))
                    }

                    averageRiskPercent?.let {
                        Text("Average Risk: ${it}%", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    riskLevel?.let {
                        Text("Risk Level: $it", fontSize = 14.sp, color = Color.Gray)
                    }
                    basedOnTestCount?.let {
                        Text("Based on $it tests", fontSize = 14.sp, color = Color.Gray)
                    }
                    highestRiskPercent?.let {
                        Text("Highest: ${it}%", fontSize = 14.sp, color = Color.Gray)
                    }
                    lowestRiskPercent?.let {
                        Text("Lowest: ${it}%", fontSize = 14.sp, color = Color.Gray)
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = reportText ?: "No report available yet.",
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

////// PAYMENT

private const val STRIPE_PUBLISHABLE_KEY =
    "pk_test_51SgDViR9VlQXqKurHANU3g2ZHZEGPz0SAvjTX3HqY0fIN5H3VCLoXUY6cBYfNM5JMWQZALVoutgKDlQnOiXsAZ1J00kc9EGKyz"

private const val CREATE_ONE_TIME_PAYMENT_INTENT_URL =
    "https://europe-west1-alzheimer-s-detection-system.cloudfunctions.net/createOneTimePaymentIntent"

@Composable
fun rememberSubscriptionActive(): Boolean {
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    var isActive by remember { mutableStateOf(false) }

    LaunchedEffect(uid) {
        if (uid == null) {
            isActive = false
            return@LaunchedEffect
        }

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .addSnapshotListener { snap, _ ->
                isActive = snap?.getBoolean("subscriptionActive") == true
            }
    }
    return isActive
}

private fun fetchPaymentSheetParams(
    functionUrl: String,
    uid: String,
    email: String?,
    amountCents: Int = 1000,
    onSuccess: (customerId: String, ephemeralKey: String, clientSecret: String) -> Unit,
    onError: (String) -> Unit
) {
    val client = OkHttpClient()

    val json = JSONObject().apply {
        put("uid", uid)
        if (!email.isNullOrBlank()) put("email", email)
        put("amount", amountCents) // backend expects cents
    }

    val body = json.toString().toRequestBody("application/json".toMediaType())

    val request = Request.Builder()
        .url(functionUrl)
        .post(body)
        .addHeader("Content-Type", "application/json")
        .addHeader("Accept", "application/json")
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            onError("Network error: ${e.message ?: "unknown"}")
        }

        override fun onResponse(call: Call, response: Response) {
            response.use {
                val text = response.body?.string().orEmpty()

                if (!response.isSuccessful) {
                    onError("Backend ${response.code}: ${text.take(600)}")
                    return
                }

                try {
                    val obj = JSONObject(text)

                    val customerId = obj.optString("customerId", "")
                    val ephemeralKey = obj.optString("ephemeralKeySecret", "")
                    val clientSecret = obj.optString("paymentIntentClientSecret", "")

                    if (customerId.isBlank() || ephemeralKey.isBlank() || clientSecret.isBlank()) {
                        onError(
                            "Backend JSON missing fields. " +
                                    "customerId=${customerId.isNotBlank()}, " +
                                    "ephemeralKeySecret=${ephemeralKey.isNotBlank()}, " +
                                    "paymentIntentClientSecret=${clientSecret.isNotBlank()}. " +
                                    "Body: ${text.take(600)}"
                        )
                        return
                    }

                    onSuccess(customerId, ephemeralKey, clientSecret)
                } catch (e: Exception) {
                    onError("Bad JSON from backend: ${e.message}. Body: ${text.take(600)}")
                }
            }
        }
    })
}

@Composable
fun SubscriptionPayButton(
    label: String = "Pay €10 (one-time)",
    onStatus: (String) -> Unit
) {
    val activity = LocalContext.current as? MainActivity

    val auth = FirebaseAuth.getInstance()
    val uid = auth.currentUser?.uid
    val email = auth.currentUser?.email

    // PaymentSheet instance + result handler
    val paymentSheet = rememberPaymentSheet { result ->
        when (result) {
            is PaymentSheetResult.Completed -> {
                onStatus("Payment completed ✅")
                val safeUid = FirebaseAuth.getInstance().currentUser?.uid
                if (safeUid != null) {
                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(safeUid)
                        .set(
                            mapOf(
                                "subscriptionActive" to true,
                                "subscriptionStatus" to "active",
                                "subscriptionUpdatedAt" to System.currentTimeMillis()
                            ),
                            com.google.firebase.firestore.SetOptions.merge()
                        )
                }
            }
            is PaymentSheetResult.Canceled -> onStatus("Payment canceled")
            is PaymentSheetResult.Failed -> onStatus("Payment failed: ${result.error.localizedMessage}")
        }
    }

    val mainHandler = remember { android.os.Handler(android.os.Looper.getMainLooper()) }

    Button(
        onClick = {
            if (activity == null) {
                onStatus("Not running inside MainActivity")
                return@Button
            }
            if (uid == null) {
                onStatus("Not logged in")
                return@Button
            }

            onStatus("Starting payment...")

            fetchPaymentSheetParams(
                functionUrl = CREATE_ONE_TIME_PAYMENT_INTENT_URL,
                uid = uid,
                email = email,
                amountCents = 1000, // €10.00 (use 500 for €5)
                onSuccess = { customerId, ephemeralKey, clientSecret ->
                    // ✅ Present PaymentSheet on the main thread
                    mainHandler.post {
                        onStatus("Presenting PaymentSheet...")

                        val config = PaymentSheet.Configuration(
                            merchantDisplayName = "NeuroMind",
                            customer = PaymentSheet.CustomerConfiguration(
                                id = customerId,
                                ephemeralKeySecret = ephemeralKey
                            )
                        )

                        paymentSheet.presentWithPaymentIntent(clientSecret, config)
                    }
                },
                onError = { msg ->
                    mainHandler.post { onStatus(msg) }
                }
            )
        }
    ) {
        Text(label)
    }
}

@Composable
fun SubscriptionGate(
    hasPaid: Boolean,
    message: String = "Payment required to access this feature.",
    content: @Composable () -> Unit
) {
    if (hasPaid) {
        content()
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(message, color = Color.Red, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        var status by rememberSaveable { mutableStateOf<String?>(null) }

        // Keep using your existing button if you want:
        SubscriptionPayButton(onStatus = { status = it }) // rename later if you like

        status?.let { s ->
            Spacer(Modifier.height(10.dp))

            val isSuccess = s.contains("paid", ignoreCase = true) ||
                    s.contains("success", ignoreCase = true) ||
                    s.contains("completed", ignoreCase = true)

            Text(
                s,
                color = if (isSuccess) Color.Green else Color.Red
            )
        }
    }
}




//  Google
private const val NEWS_API_KEY = "1f80a9e3dadf4b058ed916efe00b1a7b"
private const val NEWS_URL =
    "https://newsapi.org/v2/everything?q=alzheimers%20OR%20dementia%20OR%20%22brain%20health%22%20OR%20neurology&sortBy=publishedAt&language=en&apiKey=$NEWS_API_KEY"

data class NewsArticle(
    val title: String,
    val description: String,
    val url: String,
    val source: String,
    val publishedAt: String
)

@Composable
fun NewsScreen() {
    AppBackground {
        val context = LocalContext.current
        var loading by rememberSaveable { mutableStateOf(true) }
        var message by rememberSaveable { mutableStateOf("") }
        var articles by rememberSaveable { mutableStateOf<List<NewsArticle>>(emptyList()) }

        fun openUrl(link: String) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                context.startActivity(intent)
            } catch (_: Exception) {}
        }

        LaunchedEffect(Unit) {
            loading = true
            message = ""
            val client = OkHttpClient.Builder()
                .callTimeout(30, TimeUnit.SECONDS)
                .build()

            val request = Request.Builder()
                .url(NEWS_URL)
                .get()
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    loading = false
                    message = "Failed to load news."
                    articles = emptyList()
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        val text = response.body?.string().orEmpty()
                        if (!response.isSuccessful) {
                            loading = false
                            message = "Failed to load news."
                            articles = emptyList()
                            return
                        }

                        try {
                            val obj = JSONObject(text)
                            val arr = obj.optJSONArray("articles")

                            if (arr == null || arr.length() == 0) {
                                loading = false
                                message = "No relevant news found."
                                articles = emptyList()
                                return
                            }

                            val regex = Regex(
                                "alz|dementia|memory|cognitive|brain|neuro",
                                RegexOption.IGNORE_CASE
                            )

                            val parsed = mutableListOf<NewsArticle>()
                            for (i in 0 until arr.length()) {
                                val a = arr.getJSONObject(i)
                                val title = a.optString("title", "")
                                val description = a.optString("description", "")
                                val url = a.optString("url", "")
                                val publishedAt = a.optString("publishedAt", "")
                                val sourceObj = a.optJSONObject("source")
                                val sourceName = sourceObj?.optString("name", "") ?: ""

                                val combined = "$title $description"
                                if (regex.containsMatchIn(combined)) {
                                    if (title.isNotBlank() && url.isNotBlank()) {
                                        parsed.add(
                                            NewsArticle(
                                                title = title,
                                                description = description,
                                                url = url,
                                                source = sourceName,
                                                publishedAt = publishedAt
                                            )
                                        )
                                    }
                                }
                            }

                            val finalList = parsed.take(12)
                            loading = false
                            message = if (finalList.isEmpty()) "No Alzheimer-related news found." else ""
                            articles = finalList
                        } catch (_: Exception) {
                            loading = false
                            message = "Failed to load news."
                            articles = emptyList()
                        }
                    }
                }
            })
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                "Latest Alzheimer’s & Brain Health News",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))

            if (loading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator()
                    Spacer(Modifier.padding(start = 12.dp))
                    Text("Loading news...")
                }
                return@Column
            }

            if (message.isNotBlank() && articles.isEmpty()) {
                Text(message, color = Color.Gray)
                return@Column
            }

            articles.forEach { a ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(a.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        if (a.description.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(a.description, fontSize = 14.sp)
                        }
                        Spacer(Modifier.height(10.dp))
                        Button(onClick = { openUrl(a.url) }) {
                            Text("Read Full Article →")
                        }
                        Spacer(Modifier.height(6.dp))

                        val footer = listOf(
                            a.source.takeIf { it.isNotBlank() },
                            a.publishedAt.takeIf { it.isNotBlank() }
                        ).filterNotNull().joinToString(" — ")

                        if (footer.isNotBlank()) {
                            Text(footer, fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

private const val SUPPORT_NUMBER = "5559991000" // its a fake number

private fun dialSupport(context: Context) {
    val uri = Uri.parse("tel:$SUPPORT_NUMBER")
    val intent = Intent(Intent.ACTION_DIAL, uri)
    context.startActivity(intent)
}

private fun submitSupportRequest(
    subject: String,
    message: String,
    onSuccess: () -> Unit,
    onFailure: (Exception) -> Unit
) {
    val user = FirebaseAuth.getInstance().currentUser ?: run {
        onFailure(Exception("User not logged in"))
        return
    }

    val data = mapOf(
        "userId" to user.uid,
        "email" to (user.email ?: ""),
        "subject" to subject.trim(),
        "message" to message.trim(),
        "timestamp" to System.currentTimeMillis(),
        "status" to "new"
    )

    FirebaseFirestore.getInstance()
        .collection("supportRequests")
        .add(data)
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener { e -> onFailure(e) }
}

@Composable
fun GoogleSignInButton(
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity ?: run {
        onError("Not running inside an Activity")
        return
    }

    val webClientId = stringResource(id = com.example.nmsapp.R.string.default_web_client_id)

    val gso = remember(webClientId) {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
    }


    val googleClient = remember {
        GoogleSignIn.getClient(context, gso)
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            if (result.resultCode != Activity.RESULT_OK) {
                onError("Google sign-in cancelled")
                return@rememberLauncherForActivityResult
            }

            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.getResult(ApiException::class.java)

            val idToken = account.idToken
            if (idToken.isNullOrBlank()) {
                onError("Missing Google ID token")
                return@rememberLauncherForActivityResult
            }

            val credential = GoogleAuthProvider.getCredential(idToken, null)

            FirebaseAuth.getInstance()
                .signInWithCredential(credential)
                .addOnSuccessListener {
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    onError(e.message ?: "Firebase authentication failed")
                }

        } catch (e: ApiException) {
            onError("Google sign-in failed: ${e.statusCode}")
        } catch (e: Exception) {
            onError(e.message ?: "Google sign-in error")
        }
    }

    Button(
        onClick = {
            googleClient.signOut().addOnCompleteListener {
                launcher.launch(googleClient.signInIntent)
            }
        }
    ) {
        Text("Continue with Google")
    }
}

fun handlePostGoogleSignup(
    onSuccess: () -> Unit,
    onFailure: (String) -> Unit
) {
    val user = FirebaseAuth.getInstance().currentUser ?: run {
        onFailure("No user after Google sign-in")
        return
    }

    val uid = user.uid
    val db = FirebaseFirestore.getInstance()

    db.collection("users")
        .document(uid)
        .get()
        .addOnSuccessListener { snap ->
            if (snap.exists()) {
                onSuccess()
                return@addOnSuccessListener
            }

            val data = mapOf(
                "email" to (user.email ?: ""),
                "firstName" to (user.displayName?.split(" ")?.firstOrNull() ?: ""),
                "lastName" to (user.displayName?.split(" ")?.drop(1)?.joinToString(" ") ?: ""),
                "createdAt" to System.currentTimeMillis()
            )

            db.collection("users")
                .document(uid)
                .set(data)
                .addOnSuccessListener {
                    assignRandomDoctorToPatient(
                        userId = uid,
                        onSuccess = { onSuccess() },
                        onFailure = { e -> onFailure(e.message ?: "Doctor assignment failed") }
                    )
                }
                .addOnFailureListener { e ->
                    onFailure(e.message ?: "Failed creating user profile")
                }
        }
        .addOnFailureListener { e ->
            onFailure(e.message ?: "Failed checking user profile")
        }
}
