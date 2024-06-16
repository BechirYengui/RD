package com.example.tcu_car;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.tcu_car.client.ThingsBoardClient;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.util.Log; // Import log

public class MainActivity extends AppCompatActivity {

    private ThingsBoardClient thingsBoardClient;
    private ExecutorService executorService;
    private TextView dataTextView;
    private TextView deviceInfo;
    private Button updateButton;

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dataTextView = findViewById(R.id.dataTextView);
        deviceInfo = findViewById(R.id.device_info);
        updateButton = findViewById(R.id.update_btn);

        executorService = Executors.newSingleThreadExecutor();

        Log.d(TAG, "onCreate: ExecutorService initialized");

        executorService.submit(() -> {
            thingsBoardClient = new ThingsBoardClient(MainActivity.this);
            Log.d(TAG, "onCreate: ThingsBoardClient initialized");
            runOnUiThread(this::initializeUI);
        });
    }

    private void initializeUI() {
        String deviceId = "1e63f520-2840-11ef-8e02-43e3c0f0fa12";
        Log.d(TAG, "initializeUI: Fetching and displaying devices");
        fetchAndDisplayDevices();

        deviceInfo.setText("Dispositif : MonDispositif\nVersion du Firmware : 1.0");

        updateButton.setOnClickListener(v -> {
            Log.d(TAG, "initializeUI: Update button clicked");
            Toast.makeText(MainActivity.this, "Update button clicked", Toast.LENGTH_SHORT).show();
        });
    }

    private void fetchAndDisplayDevices() {
        executorService.submit(() -> {
            try {
                Log.d(TAG, "fetchAndDisplayDevices: Fetching devices from ThingsBoardClient");
                String rawResponse = thingsBoardClient.getRawResponseForDevices();
                runOnUiThread(() -> {
                    Log.d(TAG, "fetchAndDisplayDevices: Devices fetched successfully");
                    dataTextView.setText(rawResponse);
                });
            } catch (Exception e) {
                Log.e(TAG, "fetchAndDisplayDevices: Error fetching devices", e);
                runOnUiThread(() -> dataTextView.setText("Error fetching devices: " + e.getMessage()));
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            Log.d(TAG, "onDestroy: ExecutorService shutdown");
        }
    }
}
