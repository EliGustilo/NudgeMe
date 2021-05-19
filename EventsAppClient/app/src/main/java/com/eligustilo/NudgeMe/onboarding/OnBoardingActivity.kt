package com.eligustilo.NudgeMe.onboarding

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import com.eligustilo.NudgeMe.DataManager.initWith
import com.eligustilo.NudgeMe.R

class OnBoardingActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.onboarding_activity_layout)
        val navController = findNavController(R.id.onboarding_nav_host_fragment)
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.onboarding_fragment_1, R.id.onboarding_fragment_2
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        initWith(this)
    }
}