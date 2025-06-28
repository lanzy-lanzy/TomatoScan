package com.ml.tomatoscan.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ml.tomatoscan.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController) {
    // Animation states
    val logoScale = remember { Animatable(0f) }
    val logoAlpha = remember { Animatable(0f) }
    val leafScale = remember { Animatable(0f) }
    val leafAlpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val descriptionAlpha = remember { Animatable(0f) }

    LaunchedEffect(key1 = true) {
        // Sequential animations for a polished effect

        // 1. Logo appears first with spring animation
        logoAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
        logoScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )

        delay(300L)

        // 2. Leaf elements appear with gentle animation
        leafAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
        )
        leafScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow
            )
        )

        delay(200L)

        // 3. Text appears
        textAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
        )

        delay(150L)

        // 4. Description appears
        descriptionAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
        )

        // Hold the splash screen for a moment
        delay(800L)

        // Navigate to dashboard
        navController.popBackStack()
        navController.navigate("dashboard")
    }

    // Gradient background using tomato theme colors
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primaryContainer
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Decorative leaf elements positioned around the logo
            Box(
                contentAlignment = Alignment.Center
            ) {
                // Top-left leaf
                Image(
                    painter = painterResource(id = R.drawable.tomato_leaf),
                    contentDescription = "Decorative Leaf",
                    modifier = Modifier
                        .size(80.dp)
                        .offset(x = (-60).dp, y = (-40).dp)
                        .scale(leafScale.value * 0.6f)
                        .alpha(leafAlpha.value * 0.7f)
                )

                // Bottom-right leaf
                Image(
                    painter = painterResource(id = R.drawable.tomato_leaf),
                    contentDescription = "Decorative Leaf",
                    modifier = Modifier
                        .size(70.dp)
                        .offset(x = 50.dp, y = 30.dp)
                        .scale(leafScale.value * 0.5f)
                        .alpha(leafAlpha.value * 0.6f)
                )

                // Main app logo
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "TomatoScan Logo",
                    modifier = Modifier
                        .size(180.dp)
                        .scale(logoScale.value)
                        .alpha(logoAlpha.value)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // App name
            Text(
                text = "TomatoScan",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 32.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(textAlpha.value)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // App description
            Text(
                text = "AI-Powered Tomato Disease Detection",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(descriptionAlpha.value)
            )
        }
    }
}
