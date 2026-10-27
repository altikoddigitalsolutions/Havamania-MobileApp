package com.havamania

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import com.havamania.ui.theme.*
import java.time.LocalDate

/** Debug-only local fixture: no ViewModel, repository or account calls. */
open class TravelEditorTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val colors = ThemeFactory.createColors(AppTheme.DARK)
            CompositionLocalProvider(
                LocalHavamaniaColors provides colors,
                LocalHavamaniaTypography provides ThemeFactory.createTypography(colors)
            ) {
                ProvideResponsiveLayout {
                    MaterialTheme {
                        var editor by rememberTravelEditor("local-fixture")
                        var result by remember { mutableStateOf("") }
                        val date = LocalDate.now().plusDays(2)
                        val plan = TravelPlan(id = "local-trip", userId = "local-fixture", city = "Ankara",
                            latitude = 39.9, longitude = 32.8, startDate = date, endDate = date)
                        val origin = GeocodingResultDto(id = 16, name = "Bursa", country = "Türkiye",
                            latitude = 40.2, longitude = 29.1)
                        Column {
                            Button(onClick = { editor = editor.edit(plan.id) }) { Text("Test düzenleyicisini aç") }
                            Text(result)
                        }
                        TravelEditorHost(editor, listOf(plan), "local-fixture", false, {},
                            { editor = editor.close() }) { base, requireSelection ->
                            TravelPlanForm(base, emptyList(), listOf(origin), LocationSearchStatus.IDLE,
                                LocationSearchStatus.RESULTS, false, {}, {},
                                { editor = editor.close() }, {
                                    result = "Test çıktısı: ${it.id}, ${it.originCity}, ${it.originLatitude}, ${it.originLongitude}"
                                    editor = editor.close()
                                }, requireSelection)
                        }
                    }
                }
            }
        }
    }
}
