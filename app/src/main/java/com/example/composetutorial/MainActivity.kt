package com.example.composetutorial

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import coil.compose.rememberAsyncImagePainter
import com.example.composetutorial.ui.theme.ComposeTutorialTheme
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.Intent

@Serializable object Conversation
@Serializable object Settings

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //data for saving it to device memory
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "saved-data-database"
        ).build()

        val dao = db.userDao()
        enableEdgeToEdge()

        setContent {
            ComposeTutorialTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MyAppNavHost(
                        modifier = Modifier.padding(innerPadding),
                        dao = dao
                    )
                }
            }
        }
    }
}
@Composable
fun MyAppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    dao: UserDao
) {
    NavHost(
        modifier = modifier, navController = navController, startDestination = Conversation
    ) {
        composable<Conversation> {
            ConversationScreen(dao = dao,onNavigateToSettings = {
                navController.navigate(route = Settings)
            })
        }
        composable<Settings> {
            SettingsScreen(dao = dao, onBack = {
                navController.navigate(Conversation) {
                    popUpTo(Conversation) { inclusive = true }
                }
            })
        }
    }
}

@Composable
fun ConversationScreen(dao: UserDao, messages: List<Message> = SampleData.conversationSample,
    onNavigateToSettings: () -> Unit
) {
    val savedUserState = dao.getUser().collectAsState(initial = null)
    val userProfile = savedUserState.value
    LazyColumn { items(messages) { message ->
            MessageCard(
                msg = message,
                userProfile = userProfile,
                onProfileClick = { onNavigateToSettings() }
            )
        }
    }
}

@Composable
fun MessageCard(msg: Message,userProfile: UserProfile?, onProfileClick: () -> Unit) {
    Row(modifier = Modifier.padding(all = 8.dp)) {
        val painter = if (userProfile?.imagePath != null) {
            rememberAsyncImagePainter(File(userProfile.imagePath))
        } else {
            painterResource(R.drawable.profile_picture)
        }
        Image(painter = painter, contentDescription = null, modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                .clickable { onProfileClick() },
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(8.dp))

        var isExpanded by remember { mutableStateOf(false) }
        val surfaceColor by animateColorAsState(
            if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            label = "colorAnimation"
        )
        Column(modifier = Modifier.clickable { isExpanded = !isExpanded }) {
            Text(text = msg.author, color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(4.dp))

            Surface(shape = MaterialTheme.shapes.medium, shadowElevation = 1.dp,
                color = surfaceColor,
                modifier = Modifier.animateContentSize().padding(1.dp)
            ) {
                Text(text = msg.body, modifier = Modifier.padding(all = 4.dp),
                    maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun SettingsScreen(dao: UserDao, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var username by remember { mutableStateOf("") }
    val savedUserState = dao.getUser().collectAsState(initial = null)
    val savedUser = savedUserState.value
    //launch when got permission
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {isGranted ->
            if (isGranted) {
                val intent = Intent(context, SensorService::class.java)
                ContextCompat.startForegroundService(context, intent)
            }
        }
    )
    LaunchedEffect(savedUser) {
        if (savedUser != null) {
            if (username.isEmpty()) username = savedUser.username
            if (savedUser.imagePath != null) {
                imageUri = Uri.fromFile(File(savedUser.imagePath))
            }
        }
    }
    fun saveAndExit() {
        scope.launch {
            dao.insertUser(
                UserProfile(username = username, imagePath = imageUri?.path
                )
            )
            onBack()
        }
    }
    //Back saves the state
    BackHandler {
        saveAndExit()
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val savedPath = saveImageToInternalStorage(context, uri)
            if (savedPath != null) {
                imageUri = Uri.fromFile(File(savedPath))
            }
        }
    }
    Column(modifier = Modifier.fillMaxSize().padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (imageUri != null) {
            Image(painter = rememberAsyncImagePainter(imageUri), contentDescription = "Profile Image",
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            //put all in the center of screen
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Text("Pic missing", color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(onClick = { launcher.launch("image/*") }) {
            Text(text = "Change pic")
        }

        Spacer(modifier = Modifier.height(10.dp))

        TextField(value = username, onValueChange = { username = it },
            label = { Text("User") }
        )
        //button for start proximitysensor
        Button(onClick = {
            // permission
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                val intent = Intent(context, SensorService::class.java)
                ContextCompat.startForegroundService(context, intent)
            } else {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }) {Text("Use proximity sensor")
        }

        Spacer(modifier = Modifier.height(6.dp))
        // stop button fro proximity sensor
        Button(onClick = {
            val intent = Intent(context, SensorService::class.java)
            context.stopService(intent)
        }) {Text("Stop proximity sensor")
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(onClick = { saveAndExit() }) {
            Text("Go back")
        }
    }
}

fun saveImageToInternalStorage(context: Context, uri: Uri): String? {
    val fileName = "profile_image.jpg"
    val file = File(context.filesDir, fileName)

    return try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        val outputStream = FileOutputStream(file)
        inputStream?.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}