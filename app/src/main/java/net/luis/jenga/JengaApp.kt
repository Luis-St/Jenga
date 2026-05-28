package net.luis.jenga

import android.app.Application
import androidx.room.Room
import kotlinx.coroutines.flow.MutableStateFlow
import net.luis.jenga.data.export.ExportData
import net.luis.jenga.data.local.AppDatabase

class JengaApp : Application() {

    val importBackup: MutableStateFlow<ExportData?> = MutableStateFlow(null)

    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "jenga_db"
        ).build()
    }
}
