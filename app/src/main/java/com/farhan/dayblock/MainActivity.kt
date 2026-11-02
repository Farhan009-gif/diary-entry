package com.farhan.dayblock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.farhan.dayblock.ui.DayblockApp
import com.farhan.dayblock.ui.DiaryViewModel
import com.farhan.dayblock.ui.theme.DayblockTheme

class MainActivity : ComponentActivity() {

    private val viewModel: DiaryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DayblockTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DayblockApp(viewModel = viewModel)
                }
            }
        }
    }
}
