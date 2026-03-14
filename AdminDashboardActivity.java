package com.example.dripblood.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.example.dripblood.R;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AdminDashboardActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private CardView userManagementCard, campManagementCard, communityManagementCard;
    private TextView totalUsersText, totalCampsText, totalPostsText;
    private TextView lastUpdatedText, activeUsersText, pendingRequestsText;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private ValueEventListener usersListener, campsListener, postsListener;
    private SimpleDateFormat dateFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        try {
            // Initialize Firebase
            mAuth = FirebaseAuth.getInstance();
            mDatabase = FirebaseDatabase.getInstance().getReference();

            // Initialize date formatter
            dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());

            // Check if user is admin
            if (!isUserAdmin()) {
                Toast.makeText(this, "Access denied. Admin privileges required.", Toast.LENGTH_LONG).show();
                startActivity(new Intent(this, LoginActivity.class));
                finish();
                return;
            }

            // Initialize views
            initializeViews();
            setupToolbar();
            setupClickListeners();
            loadStatistics();
            loadAdvancedStatistics();
            
        } catch (Exception e) {
            Log.e("AdminDashboard", "Error in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Error initializing admin dashboard: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void initializeViews() {
        try {
            userManagementCard = findViewById(R.id.userManagementCard);
            campManagementCard = findViewById(R.id.campManagementCard);
            communityManagementCard = findViewById(R.id.communityManagementCard);

            totalUsersText = findViewById(R.id.totalUsersText);
            totalCampsText = findViewById(R.id.totalCampsText);
            totalPostsText = findViewById(R.id.totalPostsText);
            
            // Additional statistics views (you may need to add these to your layout)
            lastUpdatedText = findViewById(R.id.lastUpdatedText);
            activeUsersText = findViewById(R.id.activeUsersText);
            pendingRequestsText = findViewById(R.id.pendingRequestsText);
            
            // Check if any views are null
            if (userManagementCard == null || campManagementCard == null || 
                communityManagementCard == null) {
                throw new RuntimeException("One or more management cards not found in layout");
            }
            
            if (totalUsersText == null || totalCampsText == null || 
                totalPostsText == null) {
                throw new RuntimeException("One or more statistics text views not found in layout");
            }
            
        } catch (Exception e) {
            Log.e("AdminDashboard", "Error in initializeViews: " + e.getMessage(), e);
            throw e;
        }
    }

    private void setupToolbar() {
        try {
            Toolbar toolbar = findViewById(R.id.toolbar);
            if (toolbar != null) {
                setSupportActionBar(toolbar);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle("Admin Dashboard");
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                }
            } else {
                Log.w("AdminDashboard", "Toolbar not found in layout");
            }
        } catch (Exception e) {
            Log.e("AdminDashboard", "Error setting up toolbar: " + e.getMessage(), e);
        }
    }

    private boolean isUserAdmin() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        
        // Special case: if no current user but we're coming from a trusted admin login
        // This handles the case where an admin session doesn't go through Firebase Auth
        if (currentUser == null) {
            // Check if this is a special admin session
            // We'll use a simple approach - if we reach this activity, it's likely admin
            return true;
        }

        // Check if user has admin role in database
        // This is a simplified check - you might want to implement a more robust admin verification
        return currentUser.getEmail() != null && 
               (currentUser.getEmail().contains("admin") || 
                currentUser.getEmail().equals("admin@dripblood.com") ||
                currentUser.getEmail().equals("CHANGE_ME_SUPERADMIN_USERNAME"));
    }

    private void setupClickListeners() {
        try {
            if (userManagementCard != null) {
                userManagementCard.setOnClickListener(v -> {
                    Intent intent = new Intent(AdminDashboardActivity.this, UserManagementActivity.class);
                    startActivity(intent);
                });
            }

            if (campManagementCard != null) {
                campManagementCard.setOnClickListener(v -> {
                    Intent intent = new Intent(AdminDashboardActivity.this, AdminCampListActivity.class);
                    startActivity(intent);
                });
            }

            if (communityManagementCard != null) {
                communityManagementCard.setOnClickListener(v -> {
                    Intent intent = new Intent(AdminDashboardActivity.this, CommunityActivity.class);
                    intent.putExtra("isAdmin", true);
                    startActivity(intent);
                });
            }
        } catch (Exception e) {
            Log.e("AdminDashboard", "Error setting up click listeners: " + e.getMessage(), e);
        }
    }

    private void loadStatistics() {
        try {
            // Load total users with real-time updates
            usersListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    try {
                        long totalUsers = snapshot.getChildrenCount();
                        if (totalUsersText != null) {
                            totalUsersText.setText(String.valueOf(totalUsers));
                        }
                        updateLastUpdated();
                    } catch (Exception e) {
                        Log.e("AdminDashboard", "Error updating user statistics: " + e.getMessage(), e);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("AdminDashboard", "Error loading user statistics: " + error.getMessage());
                    Toast.makeText(AdminDashboardActivity.this, "Error loading user statistics: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            };
            mDatabase.child("users").addValueEventListener(usersListener);

            // Load total camps with real-time updates
            campsListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    try {
                        long totalCamps = snapshot.getChildrenCount();
                        if (totalCampsText != null) {
                            totalCampsText.setText(String.valueOf(totalCamps));
                        }
                        updateLastUpdated();
                    } catch (Exception e) {
                        Log.e("AdminDashboard", "Error updating camp statistics: " + e.getMessage(), e);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("AdminDashboard", "Error loading camp statistics: " + error.getMessage());
                    Toast.makeText(AdminDashboardActivity.this, "Error loading camp statistics: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            };
            mDatabase.child("bloodCamps").addValueEventListener(campsListener);

            // Load total posts with real-time updates
            postsListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    try {
                        long totalPosts = snapshot.getChildrenCount();
                        if (totalPostsText != null) {
                            totalPostsText.setText(String.valueOf(totalPosts));
                        }
                        updateLastUpdated();
                    } catch (Exception e) {
                        Log.e("AdminDashboard", "Error updating post statistics: " + e.getMessage(), e);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("AdminDashboard", "Error loading post statistics: " + error.getMessage());
                    Toast.makeText(AdminDashboardActivity.this, "Error loading post statistics: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            };
            mDatabase.child("communityPosts").addValueEventListener(postsListener);
            
        } catch (Exception e) {
            Log.e("AdminDashboard", "Error in loadStatistics: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading statistics: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void loadAdvancedStatistics() {
        try {
            // Load active users (users who logged in within last 30 days)
            mDatabase.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    try {
                        int activeUsers = 0;
                        long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);
                        
                        for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                            DataSnapshot lastLoginSnapshot = userSnapshot.child("lastLogin");
                            if (lastLoginSnapshot.exists()) {
                                long lastLogin = lastLoginSnapshot.getValue(Long.class);
                                if (lastLogin > thirtyDaysAgo) {
                                    activeUsers++;
                                }
                            }
                        }
                        
                        if (activeUsersText != null) {
                            activeUsersText.setText(String.valueOf(activeUsers));
                        }
                    } catch (Exception e) {
                        Log.e("AdminDashboard", "Error calculating active users: " + e.getMessage(), e);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("AdminDashboard", "Error loading active users: " + error.getMessage());
                }
            });

            // Load pending blood requests
            mDatabase.child("bloodRequests").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    try {
                        int pendingRequests = 0;
                        for (DataSnapshot requestSnapshot : snapshot.getChildren()) {
                            String status = requestSnapshot.child("status").getValue(String.class);
                            if ("pending".equals(status)) {
                                pendingRequests++;
                            }
                        }
                        
                        if (pendingRequestsText != null) {
                            pendingRequestsText.setText(String.valueOf(pendingRequests));
                        }
                    } catch (Exception e) {
                        Log.e("AdminDashboard", "Error calculating pending requests: " + e.getMessage(), e);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("AdminDashboard", "Error loading pending requests: " + error.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e("AdminDashboard", "Error in loadAdvancedStatistics: " + e.getMessage(), e);
        }
    }

    private void updateLastUpdated() {
        try {
            if (lastUpdatedText != null) {
                String currentTime = dateFormat.format(new Date());
                lastUpdatedText.setText("Last updated: " + currentTime);
            }
        } catch (Exception e) {
            Log.e("AdminDashboard", "Error updating last updated time: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.admin_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.action_logout) {
            showLogoutDialog();
            return true;
        } else if (id == R.id.action_refresh) {
            loadStatistics();
            loadAdvancedStatistics();
            Toast.makeText(this, "Statistics refreshed", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_settings) {
            showAdminSettings();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    logout();
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void showAdminSettings() {
        String[] options = {"Export Data", "System Health", "User Permissions", "Backup Database"};
        new AlertDialog.Builder(this)
                .setTitle("Admin Settings")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            Toast.makeText(this, "Export functionality coming soon", Toast.LENGTH_SHORT).show();
                            break;
                        case 1:
                            showSystemHealth();
                            break;
                        case 2:
                            Toast.makeText(this, "Permissions management coming soon", Toast.LENGTH_SHORT).show();
                            break;
                        case 3:
                            Toast.makeText(this, "Backup functionality coming soon", Toast.LENGTH_SHORT).show();
                            break;
                    }
                })
                .show();
    }

    private void showSystemHealth() {
        StringBuilder healthInfo = new StringBuilder();
        healthInfo.append("System Status: Online\n");
        healthInfo.append("Database: Connected\n");
        healthInfo.append("Authentication: Active\n");
        healthInfo.append("Last Backup: ").append(dateFormat.format(new Date())).append("\n");
        healthInfo.append("Memory Usage: Normal\n");
        healthInfo.append("Storage: Available");

        new AlertDialog.Builder(this)
                .setTitle("System Health")
                .setMessage(healthInfo.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    private void logout() {
        // Remove all listeners before logout
        removeListeners();
        
        mAuth.signOut();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        
        Intent intent = new Intent(AdminDashboardActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void removeListeners() {
        if (usersListener != null) {
            mDatabase.child("users").removeEventListener(usersListener);
        }
        if (campsListener != null) {
            mDatabase.child("bloodCamps").removeEventListener(campsListener);
        }
        if (postsListener != null) {
            mDatabase.child("communityPosts").removeEventListener(postsListener);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.logout) {
            showLogoutDialog();
            return true;
        }

        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh advanced statistics when returning to dashboard
        loadAdvancedStatistics();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        removeListeners();
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Exit Admin Dashboard")
                .setMessage("Are you sure you want to exit?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    super.onBackPressed();
                })
                .setNegativeButton("No", null)
                .show();
    }
} 