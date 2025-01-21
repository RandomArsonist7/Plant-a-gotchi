package com.example.plant_a_gotchi.pages

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusModifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.plant_a_gotchi.AuthState
import com.example.plant_a_gotchi.AuthViewModel
import com.example.plant_a_gotchi.R

@Composable
fun LoginPage(modifier: Modifier = Modifier, navController: NavController, authViewModel: AuthViewModel){

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }


    val authState = authViewModel.authState.observeAsState()
    val context = LocalContext.current

    LaunchedEffect(authState.value) {
        when(authState.value){
            is AuthState.Authenticated -> navController.navigate("home")
            is AuthState.Error -> Toast.makeText(context, (authState.value as AuthState.Error).message, Toast.LENGTH_SHORT).show()
            else -> Unit
        }}

    Box(
           modifier = Modifier.background(Color(0xFFEEE7EB)),

        ){
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(36.dp))

        StyledSection {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Author: Anastazja Sofińska",
                    fontFamily = FontFamily(Font(R.font.pacifico)),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF8b5e3c),
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                )
                Text(
                    text = "Title: Plant-a-gotchi",
                    fontFamily = FontFamily(Font(R.font.pacifico)),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF8b5e3c),
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                )
                Text(
                    text = "Description: A Tamagotchi app but for plants ;) Play sensor-based minigames and see your plant thrive or leave it alone and find it miserable sometime later ;)",
                    fontFamily = FontFamily(Font(R.font.pacifico)),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF8b5e3c),
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(30.dp))
        // Logo and App Name
        Image(
            painter = painterResource(R.drawable.logo), // Replace with your logo resource
            contentDescription = "App Logo",
            modifier = Modifier.size(120.dp)
        )
        Text(
            text = "Plant-a-gotchi",
            fontFamily = FontFamily(Font(R.font.chewy)),
            style = TextStyle(
            fontSize = 30.sp,
            color = Color.Black),
            modifier = Modifier.padding(vertical = 16.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Email Input
        StyledTextField(value = email,onValueChange = { email = it },
            label = "E-mail"
        )

        Spacer(modifier = Modifier.height(8.dp))

            // Password Input
        StyledTextField(value = password,onValueChange = { password = it },
            label = "Password"
        )


        // Login Button
        StyledSection {
            Text(text = "Log In",fontFamily = FontFamily(Font(R.font.pacifico)), color = Color(0xFF8b5e3c), modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).clickable(onClick = {authViewModel.login(email, password)}))
        }

        // Register Link
        TextButton(
            onClick = { navController.navigate("signup") },
            modifier = Modifier.padding(bottom = 20.dp),
        ) {
            Text(text = "Don't have an account? Sign up!",fontFamily = FontFamily(Font(R.font.pacifico)))
        }
    }
}}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StyledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFa67c52), RoundedCornerShape(12.dp)) // Ciemna ramka
            .padding(4.dp)
            .background(Color(0xFF8b5e3c), RoundedCornerShape(11.dp)) // Jasna ramka
            .padding(4.dp)
            .background(Color(0xFFf1efe7), RoundedCornerShape(10.dp)) // Jasne tło
            .clip(RoundedCornerShape(16.dp))
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label, fontFamily = FontFamily(Font(R.font.pacifico))) },
            modifier = Modifier.fillMaxWidth().padding(4.dp), // Wewnętrzne pole
            shape = RoundedCornerShape(10.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
fun StyledSection(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .background(Color(0xFFa67c52), RoundedCornerShape(12.dp)) // Ciemna wewnętrzna ramka
            .padding(4.dp)
            .background(Color(0xFF8b5e3c), RoundedCornerShape(11.dp)) // Jasna zewnętrzna ramka
            .padding(4.dp)
            .background(Color(0xFFf1efe7), RoundedCornerShape(10.dp)) // Jasnobeżowe tło
            .clip(RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center,
        content = content
    )
}