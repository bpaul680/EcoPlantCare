package com.example.diseasedetection;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import static java.lang.Math.ceil;

public class Weather extends AppCompatActivity {

    // Declare TextViews for displaying weather information
    private TextView city, coordinates, weather, temp, min_temp, max_temp, pressure, humidity, wind, degree;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        // Initialize the TextViews by linking them to their corresponding views in the layout
        city = findViewById(R.id.city);
        coordinates = findViewById(R.id.coordinates);
        weather = findViewById(R.id.weather);
        temp = findViewById(R.id.temp);
        min_temp = findViewById(R.id.min_temp);
        max_temp = findViewById(R.id.max_temp);
        pressure = findViewById(R.id.pressure);
        humidity = findViewById(R.id.humidity);
        wind = findViewById(R.id.wind);
        degree = findViewById(R.id.degree);

        // Retrieve latitude and longitude passed via Intent from the previous activity
        String lat = getIntent().getStringExtra("lat");
        String lon = getIntent().getStringExtra("long");

        // Set the status bar color
        getWindow().setStatusBarColor(Color.parseColor("#1383C3"));

        // Display a toast with the latitude and longitude
        Toast.makeText(this, lat + " " + lon, Toast.LENGTH_LONG).show();

        // Fetch weather data using the provided latitude and longitude
        getJsonData(lat, lon);
    }

    // Method to fetch weather data from OpenWeatherMap API
    private void getJsonData(String lat, String lon) {
        // Create a new request queue for network operations
        RequestQueue queue = Volley.newRequestQueue(this);

        // Define the API key and URL for the weather API request
        String API_KEY = "47abf6e95124e501347f76927e7b1b24";
        String url = "https://api.openweathermap.org/data/2.5/weather?lat=" + lat + "&lon=" + lon + "&appid=" + API_KEY;

        // Create a JSON object request to get the weather data from the API
        JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // On a successful response, call setValues to update the UI with the data
                        setValues(response);
                    }
                },
                // On error, show a toast message
                error -> Toast.makeText(this, "Error", Toast.LENGTH_LONG).show());

        // Add the request to the request queue for execution
        queue.add(jsonRequest);
    }

    // Method to parse and set the values from the JSON response to the TextViews
    private void setValues(JSONObject response) {
        try {
            // Set city name
            city.setText(response.getString("name"));

            // Set coordinates
            String lat = response.getJSONObject("coord").getString("lat");
            String lon = response.getJSONObject("coord").getString("lon");
            coordinates.setText(lat + " , " + lon);

            // Set weather description
            weather.setText(response.getJSONArray("weather").getJSONObject(0).getString("main"));

            // Convert temperature from Kelvin to Celsius and set the temperature
            String temporary = response.getJSONObject("main").getString("temp");
            temporary = String.valueOf((int) ((((Float.parseFloat(temporary)) - 273.15))));
            temp.setText(temporary + "°C");

            // Convert minimum temperature from Kelvin to Celsius and set it
            String mintemp = response.getJSONObject("main").getString("temp_min");
            mintemp = String.valueOf((int) ((((Float.parseFloat(mintemp)) - 273.15))));
            min_temp.setText(mintemp + "°C");

            // Convert maximum temperature from Kelvin to Celsius, round it up, and set it
            String maxtemp = response.getJSONObject("main").getString("temp_max");
            maxtemp = String.valueOf((int) ceil((Float.parseFloat(maxtemp)) - 273.15));
            max_temp.setText(maxtemp + "°C");

            // Set pressure
            pressure.setText(response.getJSONObject("main").getString("pressure"));

            // Set humidity
            humidity.setText(response.getJSONObject("main").getString("humidity") + "%");

            // Set wind speed
            wind.setText(response.getJSONObject("wind").getString("speed"));

            // Set wind direction in degrees
            degree.setText("Degree:" + response.getJSONObject("wind").getString("deg") + "°");

        } catch (Exception e) {
            // Catch any exceptions and print the stack trace for debugging
            e.printStackTrace();
        }
    }
}
