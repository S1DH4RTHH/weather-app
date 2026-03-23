package com.example.weatherapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.weatherapp.ui.theme.WeatherAppTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import kotlinx.coroutines.*
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WeatherAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    WeatherScreen(modifier = Modifier.padding(innerPadding))
                }

                }
            }
        }

    }




@Composable
fun WeatherScreen(modifier: Modifier = Modifier) {

    var city by remember { mutableStateOf("") }
    var temperature by remember { mutableStateOf("") }
    var condition by remember { mutableStateOf("") }
    var forecastList by remember { mutableStateOf(listOf<String>()) }
    var isLoading by remember { mutableStateOf(false) }

    val apiKey = "YOUR_API_KEY"

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF4A90E2),
                        Color(0xFF87CEFA)
                    )
                )
            ),
        contentAlignment = Alignment.TopCenter
    ) {

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.9f)
            )
        ) {

            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                Text(
                    text = "Weather App",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("Enter City") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (isLoading) {
                    CircularProgressIndicator()
                } else {

                    if (temperature.isNotEmpty() && temperature != "Error") {
                        if (temperature == "Error") {
                            Text(
                                text = condition,
                                color = Color.Red,
                                fontSize = 18.sp
                            )
                        }

                        val emoji = when (condition) {
                            "Clouds" -> "☁️"
                            "Rain" -> "🌧️"
                            "Clear" -> "☀️"
                            "Haze" -> "🌫️"
                            else -> "🌍"
                        }

                        Text(
                            text = "📍 ${city.replaceFirstChar { it.uppercase() }}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = temperature,
                            fontSize = 60.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "$emoji $condition",
                            fontSize = 22.sp,
                            color = Color.Gray
                        )
                    }
                }

                Button(
                    onClick = {

                        if (city.isNotEmpty()) {

                            isLoading = true
                            forecastList = emptyList() // ✅ important

                            fetchWeather(city, apiKey) { temp, cond ->
                                temperature = "${temp.toDouble().toInt()}°C"
                                condition = cond
                                isLoading = false
                            }

                            fetchForecast(city, apiKey) { list ->
                                forecastList = list
                            }
                        }
                    },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("Refresh Weather")
                }

                // ✅ FORECAST DISPLAY
                if (forecastList.isNotEmpty()) {

                    Text(
                        text = "Forecast",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    forecastList.forEach {
                        Text(it)
                    }
                }
            }
        }
    }
}






fun fetchWeather(city: String, apiKey: String, onResult: (String, String) -> Unit) {

    val client = OkHttpClient()

    val url = "https://api.openweathermap.org/data/2.5/weather?q=$city&appid=$apiKey&units=metric"
    val request = Request.Builder().url(url).build()

    client.newCall(request).enqueue(object : Callback {

        override fun onFailure(call: Call, e: IOException) {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                onResult("Error", "Network Error ❌")
            }
        }

        override fun onResponse(call: Call, response: Response) {
            if (!response.isSuccessful) {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    onResult("Error", "City not found ❌")
                }
                return
            }

            response.body?.string()?.let { jsonString ->
                println("API RESPONSE: $jsonString")


                try {
                    val json = JSONObject(jsonString)

                    val temp = json.getJSONObject("main").getDouble("temp")
                    val condition = json.getJSONArray("weather")
                        .getJSONObject(0)
                        .getString("main")

                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        onResult(temp.toString(), condition)
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    })
}
fun fetchForecast(city: String, apiKey: String, onResult: (List<String>) -> Unit) {

    val client = OkHttpClient()

    val url = "https://api.openweathermap.org/data/2.5/forecast?q=$city&appid=$apiKey&units=metric"
    val request = Request.Builder().url(url).build()

    client.newCall(request).enqueue(object : Callback {

        override fun onFailure(call: Call, e: IOException) {
            e.printStackTrace()
        }

        override fun onResponse(call: Call, response: Response) {
            response.body?.string()?.let { jsonString ->

                val resultList = mutableListOf<String>()

                try {
                    val json = JSONObject(jsonString)
                    val list = json.getJSONArray("list")

                    // take first 5 items
                    for (i in 0 until 5) {
                        val item = list.getJSONObject(i)

                        val temp = item.getJSONObject("main").getDouble("temp")
                        val condition = item.getJSONArray("weather")
                            .getJSONObject(0)
                            .getString("main")
                        val emoji = when (condition) {
                            "Clouds" -> "☁️"
                            "Rain" -> "🌧️"
                            "Clear" -> "☀️"
                            else -> "🌍"
                        }

                        val text = "${temp.toInt()}°C - $emoji $condition"

                        resultList.add(text)
                    }

                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        onResult(resultList)
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    })
}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    WeatherAppTheme {
        WeatherScreen()
    }
}
