package com.example.orientation_app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry-point for Hilt dependency injection.
 * Every component in the graph is rooted here.
 */
@HiltAndroidApp
class UnicompassApp : Application()
