package com.example.diseasedetection;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationManager;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.example.diseasedetection.ml.DiseaseDetection;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MainActivity extends AppCompatActivity {
    // Variable to handle location-related functionalities
    private FusedLocationProviderClient mfusedlocation;
    private static final int MY_REQUEST_CODE = 1010; // Request code for permission

    // UI Elements
    TextView result, demoText, classified, clickHere;
    ImageView imageView, arrowImage;
    Button picture, weatherButton;

    int imageSize = 224; // Default image size for the model

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI Elements
        result = findViewById(R.id.result);
        imageView = findViewById(R.id.imageView);
        picture = findViewById(R.id.button);
        weatherButton = findViewById(R.id.btn);

        demoText = findViewById(R.id.demoText);
        clickHere = findViewById(R.id.click_here);
        arrowImage = findViewById(R.id.demoArrow);
        classified = findViewById(R.id.classified);

        // Set visibility of demo-related UI elements
        demoText.setVisibility(View.VISIBLE);
        clickHere.setVisibility(View.GONE);
        arrowImage.setVisibility(View.VISIBLE);
        classified.setVisibility(View.GONE);
        result.setVisibility(View.GONE);

        // Initialize the fused location provider client for location services
        mfusedlocation = LocationServices.getFusedLocationProviderClient(this);

        // Get the latitude and longitude if passed from previous activity (optional)
        String lat = getIntent().getStringExtra("lat");
        String lon = getIntent().getStringExtra("long");

        // Set an onClickListener for the weather button to fetch location and open weather activity
        weatherButton.setOnClickListener(v -> {
            getLastLocationAndOpenWeather();
        });

        // Set an onClickListener for the picture button to launch the camera
        picture.setOnClickListener(view -> {
            // Launch camera if we have the required permission
            if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, 1);
            } else {
                // Request camera permission if it's not granted
                requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
            }
        });
    }

    // Method to get the last known location and open the Weather activity with that location
    private void getLastLocationAndOpenWeather() {
        if (checkPermission()) { // Check if location permission is granted
            if (isLocationEnabled()) { // Check if location services are enabled
                mfusedlocation.getLastLocation().addOnCompleteListener(task -> {
                    Location location = task.getResult();
                    if (location == null) {
                        requestNewLocationData(); // Request new location data if the last location is null
                    } else {
                        // Pass the location to the Weather activity
                        Intent intent = new Intent(MainActivity.this, Weather.class);
                        intent.putExtra("lat", String.valueOf(location.getLatitude()));
                        intent.putExtra("long", String.valueOf(location.getLongitude()));
                        startActivity(intent);
                    }
                });
            } else {
                Toast.makeText(this, "Please Turn on your location", Toast.LENGTH_LONG).show(); // Prompt user to enable location services
            }
        } else {
            requestGpsPermission(); // Request GPS permission if not already granted
        }
    }

    @SuppressLint("MissingPermission")
    private void requestNewLocationData() {
        // Configure the location request with high accuracy and immediate updates
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(0);
        locationRequest.setFastestInterval(0);
        locationRequest.setNumUpdates(1);

        // Request location updates with the configured request and a callback
        mfusedlocation.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
    }

    // Callback that handles the location result once the location is determined
    private final LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull LocationResult locationResult) {
            if (locationResult != null && locationResult.getLastLocation() != null) {
                Location lastLocation = locationResult.getLastLocation();
                // Pass the obtained location to the Weather activity
                Intent intent = new Intent(MainActivity.this, Weather.class);
                intent.putExtra("lat", String.valueOf(lastLocation.getLatitude()));
                intent.putExtra("long", String.valueOf(lastLocation.getLongitude()));
                startActivity(intent);
            }
        }
    };

    // Method to check if location services are enabled on the device
    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    // Method to request GPS permission from the user
    private void requestGpsPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                MY_REQUEST_CODE);
    }

    // Method to check if location permissions are granted
    private boolean checkPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    // Handle the result of the permission request
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocationAndOpenWeather(); // If permission granted, get location and open weather
            }
        }
    }

    // Handle the result of the camera intent after taking a picture
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1 && resultCode == RESULT_OK) {
            // Get the captured image and process it
            Bitmap image = (Bitmap) data.getExtras().get("data");
            int dimension = Math.min(image.getWidth(), image.getHeight());
            image = ThumbnailUtils.extractThumbnail(image, dimension, dimension);
            imageView.setImageBitmap(image); // Set the image to the ImageView

            // Update UI visibility for classification
            demoText.setVisibility(View.GONE);
            clickHere.setVisibility(View.VISIBLE);
            arrowImage.setVisibility(View.GONE);
            classified.setVisibility(View.VISIBLE);
            result.setVisibility(View.VISIBLE);

            // Resize the image to the required input size and classify it
            image = Bitmap.createScaledBitmap(image, imageSize, imageSize, false);
            classifyImage(image);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    // Method to classify the image using the TensorFlow Lite model
    private void classifyImage(Bitmap image) {
        try {
            // Load the model
            DiseaseDetection model = DiseaseDetection.newInstance(getApplicationContext());

            // Prepare the input buffer with the image data
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 224, 224, 3}, DataType.FLOAT32);
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3);
            byteBuffer.order(ByteOrder.nativeOrder());

            // Get 1D array of 224 * 224 pixels from the image
            int[] intValue = new int[imageSize * imageSize];
            image.getPixels(intValue, 0, image.getWidth(), 0, 0, image.getWidth(), image.getHeight());

            // Iterate over pixels and extract R, G, B values to add to byteBuffer
            int pixel = 0;
            for (int i = 0; i < imageSize; i++) {
                for (int j = 0; j < imageSize; j++) {
                    int val = intValue[pixel++]; // RGB
                    byteBuffer.putFloat(((val >> 16) & 0xFF) * (1.f / 255.f)); // Red
                    byteBuffer.putFloat(((val >> 8) & 0xFF) * (1.f / 255.f));  // Green
                    byteBuffer.putFloat((val & 0xFF) * (1.f / 255.f));        // Blue
                }
            }

            // Load the image data into the input tensor
            inputFeature0.loadBuffer(byteBuffer);

            // Run model inference and get the result
            DiseaseDetection.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            float[] confidence = outputFeature0.getFloatArray();

            // Find the index of the class with the highest confidence
            int maxPos = 0;
            float maxConfidence = 0;
            for (int i = 0; i < confidence.length; i++) {
                if (confidence[i] > maxConfidence) {
                    maxConfidence = confidence[i];
                    maxPos = i;
                }
            }

            // Define the disease classes
            String[] classes = {"Pepper Bell Bacterial Spot", "Potato Early Blight", "Potato Late Blight", "Tomato Bacterial Spot", "Tomato Tomato YellowLeaf Curl Virus"};
            result.setText(classes[maxPos]); // Set the classification result

            // Set an onClickListener to search the result on Google when clicked
            result.setOnClickListener(view -> {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://www.google.com/search?q=" + result.getText())));
            });

            model.close(); // Close the model to release resources

        } catch (IOException e) {
            e.printStackTrace(); // Handle any errors during model inference
        }
    }
}
