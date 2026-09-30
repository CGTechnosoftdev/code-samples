package com.composebasics.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.composebasics.ComposeBasicsApp
import com.composebasics.ui.addpost.AddPostScreen
import com.composebasics.ui.forgot.ForgotPasswordScreen
import com.composebasics.ui.home.HomeScreen
import com.composebasics.ui.login.LoginScreen
import com.composebasics.ui.signup.SignUpScreen
import com.composebasics.ui.splash.SplashScreen

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val SIGN_UP = "sign_up"
    const val FORGOT_PASSWORD = "forgot_password"
    const val HOME = "home"
    const val ADD_POST = "add_post"
}

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val auth = (LocalContext.current.applicationContext as ComposeBasicsApp).container.authRepository

    NavHost(navController = navController, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) {
            SplashScreen(isLoggedIn = { auth.isLoggedIn }) { loggedIn ->
                navController.navigate(if (loggedIn) Routes.HOME else Routes.LOGIN) {
                    popUpTo(Routes.SPLASH) { inclusive = true }
                }
            }
        }
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoggedIn = {
                    navController.navigate(Routes.HOME) { popUpTo(Routes.LOGIN) { inclusive = true } }
                },
                onForgotPassword = { navController.navigate(Routes.FORGOT_PASSWORD) },
                onSignUp = { navController.navigate(Routes.SIGN_UP) },
            )
        }
        composable(Routes.SIGN_UP) {
            SignUpScreen(
                onBack = { navController.popBackStack() },
                onSignedUp = {
                    navController.navigate(Routes.HOME) { popUpTo(Routes.LOGIN) { inclusive = true } }
                },
            )
        }
        composable(Routes.FORGOT_PASSWORD) {
            ForgotPasswordScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.HOME) {
            HomeScreen(
                onAddPost = { navController.navigate(Routes.ADD_POST) },
                onLoggedOut = {
                    navController.navigate(Routes.LOGIN) { popUpTo(Routes.HOME) { inclusive = true } }
                },
            )
        }
        composable(Routes.ADD_POST) {
            AddPostScreen(onBack = { navController.popBackStack() })
        }
    }
}
