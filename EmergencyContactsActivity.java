package com.example.dripblood.activities;

import android.Manifest;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.dripblood.R;
import com.example.dripblood.widgets.SOSWidget;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class EmergencyContactsActivity extends AppCompatActivity {
    private EditText contactName, contactNumber, whatsappName;
    private Button saveButton, sosButton;
    private Switch widgetSwitch;
    private DatabaseReference emergencyRef;
    private FirebaseAuth mAuth;
    private FusedLocationProviderClient fusedLocationClient;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "SOSPrefs";
    private static final String WIDGET_ENABLED = "widgetEnabled";
    private static final int PERMISSION_REQUEST_CODE = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_contacts);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Emergency Contacts");

        mAuth = FirebaseAuth.getInstance();
        emergencyRef = FirebaseDatabase.getInstance().getReference("emergency_contacts")
                .child(mAuth.getCurrentUser().getUid());
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        contactName = findViewById(R.id.contactName);
        contactNumber = findViewById(R.id.contactNumber);
        whatsappName = findViewById(R.id.whatsappName);
        saveButton = findViewById(R.id.saveButton);
        sosButton = findViewById(R.id.sosButton);
        widgetSwitch = findViewById(R.id.widgetSwitch);

        // Set widget switch state
        widgetSwitch.setChecked(sharedPreferences.getBoolean(WIDGET_ENABLED, false));

        // Check if activity was launched from widget
        if (getIntent() != null && "TRIGGER_SOS".equals(getIntent().getAction())) {
            // Widget was clicked, trigger SOS immediately
            checkPermissionsAndTriggerSOS();
        }

        widgetSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(WIDGET_ENABLED, isChecked);
            editor.apply();

            if (isChecked) {
                // Enable widget - prompt user to add widget to home screen
                showAddWidgetDialog();
            } else {
                // Disable widget - remove from home screen
                removeWidgetFromHomeScreen();
            }
        });

        saveButton.setOnClickListener(v -> saveEmergencyContact());
        sosButton.setOnClickListener(v -> checkPermissionsAndTriggerSOS());

        // Load saved contact if exists
        loadSavedContact();
        
        // Update widget state
        updateWidgetState();
    }

    private void checkPermissionsAndTriggerSOS() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            
            ActivityCompat.requestPermissions(this,
                new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.CALL_PHONE
                },
                PERMISSION_REQUEST_CODE);
        } else {
            triggerSOS();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                triggerSOS();
            } else {
                Toast.makeText(this, "Permissions are required for SOS functionality", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void loadSavedContact() {
        emergencyRef.get().addOnSuccessListener(dataSnapshot -> {
            if (dataSnapshot.exists()) {
                EmergencyContact contact = dataSnapshot.getValue(EmergencyContact.class);
                if (contact != null) {
                    contactName.setText(contact.getName());
                    contactNumber.setText(contact.getNumber());
                    whatsappName.setText(contact.getWhatsappName());
                }
            }
        });
    }

    private void saveEmergencyContact() {
        String name = contactName.getText().toString().trim();
        String number = contactNumber.getText().toString().trim();
        String whatsappNameStr = whatsappName.getText().toString().trim();

        if (name.isEmpty() || number.isEmpty() || whatsappNameStr.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        EmergencyContact contact = new EmergencyContact(name, number, whatsappNameStr);
        emergencyRef.setValue(contact)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(EmergencyContactsActivity.this, "Emergency contact saved", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(EmergencyContactsActivity.this, "Failed to save contact", Toast.LENGTH_SHORT).show();
                });
    }

    private void triggerSOS() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission is required", Toast.LENGTH_SHORT).show();
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        String locationStr = location.getLatitude() + "," + location.getLongitude();
                        String message = "I need medical assistance! My location: " + locationStr;
                        
                        // Send WhatsApp message
                        try {
                            String whatsappName = this.whatsappName.getText().toString().trim();
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setData(Uri.parse("https://api.whatsapp.com/send?text=" + message));
                            startActivity(intent);
                        } catch (Exception e) {
                            Toast.makeText(this, "WhatsApp not installed", Toast.LENGTH_SHORT).show();
                        }

                        // Call emergency number
                        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                            Intent callIntent = new Intent(Intent.ACTION_CALL);
                            callIntent.setData(Uri.parse("tel:9876"));
                            startActivity(callIntent);
                        } else {
                            Toast.makeText(this, "Phone permission is required", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Unable to get location", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showAddWidgetDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Add SOS Widget")
                .setMessage("To add the SOS widget to your home screen:\n\n" +
                        "1. Long press on your home screen\n" +
                        "2. Select 'Widgets'\n" +
                        "3. Find 'DripBlood SOS' widget\n" +
                        "4. Drag it to your home screen\n\n" +
                        "The widget will allow you to quickly trigger SOS from your home screen.")
                .setPositiveButton("Got it!", (dialog, which) -> {
                    Toast.makeText(this, "Widget enabled! Add it to your home screen.", Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    // If user cancels, uncheck the switch
                    widgetSwitch.setChecked(false);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean(WIDGET_ENABLED, false);
                    editor.apply();
                    Toast.makeText(this, "Widget disabled", Toast.LENGTH_SHORT).show();
                })
                .setCancelable(false)
                .show();
    }

    private void removeWidgetFromHomeScreen() {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        ComponentName widgetComponent = new ComponentName(this, SOSWidget.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(widgetComponent);
        
        if (appWidgetIds.length > 0) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Remove SOS Widget")
                    .setMessage("To remove the SOS widget from your home screen:\n\n" +
                            "1. Long press on the SOS widget\n" +
                            "2. Drag it to 'Remove' or 'Delete'\n\n" +
                            "The widget will be removed from your home screen.")
                    .setPositiveButton("Got it!", null)
                    .setCancelable(false)
                    .show();
        } else {
            Toast.makeText(this, "No SOS widget found on home screen", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateWidgetState() {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        ComponentName widgetComponent = new ComponentName(this, SOSWidget.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(widgetComponent);
        
        // Update all existing widgets
        for (int appWidgetId : appWidgetIds) {
            SOSWidget.updateAppWidget(this, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

class EmergencyContact {
    private String name;
    private String number;
    private String whatsappName;

    public EmergencyContact() {
        // Required empty constructor for Firebase
    }

    public EmergencyContact(String name, String number, String whatsappName) {
        this.name = name;
        this.number = number;
        this.whatsappName = whatsappName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getWhatsappName() {
        return whatsappName;
    }

    public void setWhatsappName(String whatsappName) {
        this.whatsappName = whatsappName;
    }
} 