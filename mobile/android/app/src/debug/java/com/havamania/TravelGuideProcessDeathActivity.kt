package com.havamania

import android.os.Bundle
import android.os.Process
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.havamania.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.io.File
import java.time.LocalDate
import org.json.JSONObject

/** Shell-only QA task/process; owns a dedicated disk database, never production Room. */
class TravelGuideProcessDeathActivity : ComponentActivity() {
    private lateinit var database: WeatherDatabase
    private var restored = false
    private fun status(saved: Boolean) {
        val file = File(filesDir, "guide-process-qa.json")
        file.writeText(JSONObject().put("pid", Process.myPid()).put("restored", restored)
            .put("saved", saved).toString())
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.getBooleanExtra("qa_finish", false)) { finish(); return }
        check(packageName == "com.havamania")
        com.google.firebase.FirebaseApp.initializeApp(this)
        check(com.google.firebase.auth.FirebaseAuth.getInstance().currentUser == null)
        restored = savedInstanceState != null
        database = Room.databaseBuilder(applicationContext, WeatherDatabase::class.java, "guide-process-qa.db").build()
        runBlocking(Dispatchers.IO) {
            if (database.weatherDao().getAllTravelPlans("legacy").isEmpty()) {
                val date = LocalDate.now().plusDays(20)
                val plan = TravelPlan(id = "process-guide-upcoming", userId = "legacy", city = "Ankara",
                    latitude = 39.9, longitude = 32.8, startDate = date, endDate = date)
                database.weatherDao().saveLocalTrip(plan.toEntity())
                database.weatherDao().saveLocalTrip(plan.copy(id = "process-guide-past", city = "Bursa",
                    startDate = LocalDate.now().minusDays(5), endDate = LocalDate.now().minusDays(4)).toEntity())
            }
        }
        val model = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                TravelViewModel(application, database, { emptyList() }, object : NetworkMonitor {
                    override val isOnline = kotlinx.coroutines.flow.flowOf(false)
                }) as T
        })[TravelViewModel::class.java]
        status(false)
        setContent {
            val colors = ThemeFactory.createColors(AppTheme.DARK)
            CompositionLocalProvider(LocalHavamaniaColors provides colors,
                LocalHavamaniaTypography provides ThemeFactory.createTypography(colors),
                LocalTravelGuideRefresher provides { _, _, _ -> }) {
                ProvideResponsiveLayout { MaterialTheme {
                    TravelPlannerScreen(model, focusId = "process-guide-upcoming", onBack = { finish() })
                } }
            }
        }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        status(true)
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra("qa_finish", false)) finish()
    }
    override fun onDestroy() {
        super.onDestroy()
        if (::database.isInitialized && !isChangingConfigurations) database.close()
    }
}
