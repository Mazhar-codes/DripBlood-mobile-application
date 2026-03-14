package com.example.dripblood.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dripblood.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;

public class AdminCampListActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private CampAdapter adapter;
    private List<CampItem> campList = new ArrayList<>();
    private DatabaseReference campsRef;
    private TextView totalCampsCount, activeCampsCount;
    private FloatingActionButton fabAddCamp;
    private static final int REQUEST_LOCATION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_camp_list);

        // Setup toolbar
        setupToolbar();

        // Initialize views
        initializeViews();
        setupRecyclerView();
        setupClickListeners();

        // Initialize Firebase
        campsRef = FirebaseDatabase.getInstance().getReference("bloodCamps");
        loadCamps();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_LOCATION) {
            if (resultCode == RESULT_OK) {
                // Camp was posted successfully from MapActivity
                // Refresh the camp list to show the new camp
                loadCamps();
                Snackbar.make(findViewById(android.R.id.content), "Camp posted successfully!", Snackbar.LENGTH_LONG).show();
            } else if (resultCode == RESULT_CANCELED) {
                // User cancelled the camp posting
                Toast.makeText(this, "Camp posting cancelled", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Camp Management");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.recyclerCamps);
        totalCampsCount = findViewById(R.id.totalCampsCount);
        activeCampsCount = findViewById(R.id.activeCampsCount);
        fabAddCamp = findViewById(R.id.fabAddCamp);
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CampAdapter();
        recyclerView.setAdapter(adapter);
    }

    private void setupClickListeners() {
        fabAddCamp.setOnClickListener(v -> {
            Intent intent = new Intent(AdminCampListActivity.this, MapActivity.class);
            intent.putExtra("mode", "admin");
            startActivityForResult(intent, REQUEST_LOCATION);
        });
    }

    private void loadCamps() {
        campsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                campList.clear();
                int totalCamps = 0;
                int activeCamps = 0;
                
                for (DataSnapshot campSnap : snapshot.getChildren()) {
                    CampItem item = new CampItem();
                    item.key = campSnap.getKey();
                    item.latitude = campSnap.child("latitude").getValue(Double.class);
                    item.longitude = campSnap.child("longitude").getValue(Double.class);
                    item.name = campSnap.child("name").getValue(String.class);
                    item.address = campSnap.child("address").getValue(String.class);
                    item.date = campSnap.child("date").getValue(String.class);
                    item.isActive = campSnap.child("isActive").getValue(Boolean.class);
                    
                    campList.add(item);
                    totalCamps++;
                    
                    if (item.isActive != null && item.isActive) {
                        activeCamps++;
                    }
                }
                
                // Update statistics
                totalCampsCount.setText(String.valueOf(totalCamps));
                activeCampsCount.setText(String.valueOf(activeCamps));
                
                adapter.notifyDataSetChanged();
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AdminCampListActivity.this, "Failed to load camps: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    class CampAdapter extends RecyclerView.Adapter<CampAdapter.CampViewHolder> {
        @NonNull
        @Override
        public CampViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_camp, parent, false);
            return new CampViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull CampViewHolder holder, int position) {
            CampItem item = campList.get(position);
            
            // Set camp information
            if (item.name != null && !item.name.isEmpty()) {
                holder.campName.setText(item.name);
            } else {
                holder.campName.setText("Camp Location");
            }
            
            if (item.address != null && !item.address.isEmpty()) {
                holder.campAddress.setText(item.address);
            } else {
                holder.campAddress.setText("Lat: " + item.latitude + ", Lng: " + item.longitude);
            }
            
            if (item.date != null && !item.date.isEmpty()) {
                holder.campDate.setText("Date: " + item.date);
            } else {
                holder.campDate.setText("Date: Not specified");
            }
            
            // Set status
            if (item.isActive != null && item.isActive) {
                holder.campStatus.setText("Active");
                holder.campStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            } else {
                holder.campStatus.setText("Inactive");
                holder.campStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            }
            
            // Setup delete button
            holder.btnDelete.setOnClickListener(v -> confirmDelete(item));
            
            // Setup edit button
            holder.btnEdit.setOnClickListener(v -> editCamp(item));
        }
        
        @Override
        public int getItemCount() {
            return campList.size();
        }
        
        class CampViewHolder extends RecyclerView.ViewHolder {
            TextView campName, campAddress, campDate, campStatus;
            Button btnDelete, btnEdit;
            
            CampViewHolder(@NonNull View itemView) {
                super(itemView);
                campName = itemView.findViewById(R.id.campName);
                campAddress = itemView.findViewById(R.id.campAddress);
                campDate = itemView.findViewById(R.id.campDate);
                campStatus = itemView.findViewById(R.id.campStatus);
                btnDelete = itemView.findViewById(R.id.btnDeleteCamp);
                btnEdit = itemView.findViewById(R.id.btnEditCamp);
            }
        }
        
        private void confirmDelete(CampItem item) {
            new AlertDialog.Builder(AdminCampListActivity.this)
                .setTitle("Delete Camp")
                .setMessage("Are you sure you want to delete this camp location?")
                .setPositiveButton("Delete", (dialog, which) -> deleteCamp(item))
                .setNegativeButton("Cancel", null)
                .show();
        }
        
        private void deleteCamp(CampItem item) {
            campsRef.child(item.key).removeValue()
                .addOnSuccessListener(aVoid -> {
                    Snackbar.make(findViewById(android.R.id.content), "Camp deleted successfully!", Snackbar.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AdminCampListActivity.this, "Failed to delete camp: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
        }
        
        private void editCamp(CampItem item) {
            // TODO: Implement camp editing functionality
            Toast.makeText(AdminCampListActivity.this, "Edit functionality coming soon", Toast.LENGTH_SHORT).show();
        }
    }

    static class CampItem {
        String key;
        double latitude;
        double longitude;
        String name;
        String address;
        String date;
        Boolean isActive;
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning from adding a new camp
        loadCamps();
    }
} 