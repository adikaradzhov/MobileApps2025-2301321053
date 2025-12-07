package com.example.quickstage

import android.app.Application
import com.example.quickstage.data.AppDatabase

class QuickStageApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
}
