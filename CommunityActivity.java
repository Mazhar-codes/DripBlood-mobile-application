package com.example.dripblood.activities;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.dripblood.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import android.app.Dialog;
import com.github.chrisbanes.photoview.PhotoView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.app.ProgressDialog;
import android.widget.EditText;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.UploadCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommunityActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private CommunityAdapter adapter;
    private List<CommunityPost> postList = new ArrayList<>();
    private DatabaseReference postsRef;
    private String currentUserId;
    private boolean isAdmin = false;
    private FloatingActionButton fabUpload;
    private static final int PICK_IMAGE_REQUEST = 1011;
    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community);
        setTitle("Community");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Cloudinary config (use your own credentials here, do not commit real secrets)
        Map config = new HashMap();
        config.put("cloud_name", "YOUR_CLOUDINARY_CLOUD_NAME");
        config.put("api_key", "YOUR_CLOUDINARY_API_KEY");
        config.put("api_secret", "YOUR_CLOUDINARY_API_SECRET");
        try {
            MediaManager.init(this.getApplicationContext(), config);
        } catch (Exception e) {
            // Already initialized
        }

        // Check admin status first
        checkAdminStatus();

        recyclerView = findViewById(R.id.recyclerCommunity);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CommunityAdapter(this, postList, isAdmin);
        recyclerView.setAdapter(adapter);

        postsRef = FirebaseDatabase.getInstance().getReference("community_posts");
        Log.d("CommunityActivity", "Database reference: " + postsRef.toString());
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        // Update adapter with admin status
        updateAdapterAdminStatus();

        loadPosts();
        
        // Invalidate options menu to refresh the menu
        invalidateOptionsMenu();

        fabUpload = findViewById(R.id.fabUpload);
        fabUpload.setOnClickListener(v -> showUploadDialog());
        
        // Show admin status for debugging
        Toast.makeText(this, "Admin status: " + isAdmin, Toast.LENGTH_SHORT).show();
    }

    private void checkAdminStatus() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        Log.d("CommunityActivity", "Checking admin status...");
        
        if (currentUser != null) {
            Log.d("CommunityActivity", "Current user found: " + currentUser.getEmail());
            // Check if user is admin
            isAdmin = currentUser.getEmail() != null && 
                     (currentUser.getEmail().contains("admin") || 
                      currentUser.getEmail().equals("admin@dripblood.com") ||
                      currentUser.getEmail().equals("CHANGE_ME_SUPERADMIN_USERNAME"));
            
            Log.d("CommunityActivity", "User email: " + currentUser.getEmail());
            Log.d("CommunityActivity", "User is admin: " + isAdmin);
        } else {
            Log.d("CommunityActivity", "No current Firebase user found");
            // Check if we're coming from admin login by checking the intent
            Intent intent = getIntent();
            if (intent != null && intent.hasExtra("isAdmin")) {
                isAdmin = intent.getBooleanExtra("isAdmin", false);
                Log.d("CommunityActivity", "Admin status from intent: " + isAdmin);
            } 
        }
        
        Log.d("CommunityActivity", "Final admin status: " + isAdmin);
    }

    private void updateAdapterAdminStatus() {
        if (adapter != null) {
            adapter.setAdmin(isAdmin);
            adapter.notifyDataSetChanged();
        }
        
        // Show/hide floating action button based on admin status
        if (fabUpload != null) {
            fabUpload.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
            Log.d("CommunityActivity", "FAB visibility set to: " + (isAdmin ? "VISIBLE" : "GONE"));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d("CommunityActivity", "Creating options menu, isAdmin: " + isAdmin);
        if (isAdmin) {
            getMenuInflater().inflate(R.menu.community_admin_menu, menu);
            Log.d("CommunityActivity", "Admin menu inflated");
        } else {
            Log.d("CommunityActivity", "Admin menu not inflated - user is not admin");
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_upload_post) {
            if (isAdmin) {
                showUploadDialog();
            } else {
                Toast.makeText(this, "Only admins can upload posts", Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showUploadDialog() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            showCaptionDialog();
        }
    }

    private void showCaptionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Caption");
        final EditText captionInput = new EditText(this);
        builder.setView(captionInput);
        builder.setPositiveButton("Upload", (dialog, which) -> {
            String caption = captionInput.getText().toString().trim();
            if (imageUri != null && !caption.isEmpty()) {
                uploadCommunityPost(imageUri, caption);
            } else {
                Toast.makeText(this, "Please select an image and enter a caption", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void uploadCommunityPost(Uri imageUri, String caption) {
        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Uploading...");
        pd.setCancelable(false);
        pd.show();
        MediaManager.get().upload(imageUri)
            .callback(new UploadCallback() {
                @Override
                public void onStart(String requestId) { }
                @Override
                public void onProgress(String requestId, long bytes, long totalBytes) { }
                @Override
                public void onSuccess(String requestId, Map resultData) {
                    String imageUrl = (String) resultData.get("secure_url");
                    saveCommunityPostToDatabase(imageUrl, caption);
                    pd.dismiss();
                    Toast.makeText(CommunityActivity.this, "Community post published!", Toast.LENGTH_SHORT).show();
                }
                @Override
                public void onError(String requestId, com.cloudinary.android.callback.ErrorInfo error) {
                    pd.dismiss();
                    Toast.makeText(CommunityActivity.this, "Cloudinary upload failed: " + error.getDescription(), Toast.LENGTH_SHORT).show();
                }
                @Override
                public void onReschedule(String requestId, com.cloudinary.android.callback.ErrorInfo error) { }
            })
            .dispatch();
    }

    private void saveCommunityPostToDatabase(String imageUrl, String caption) {
        Map<String, Object> post = new HashMap<>();
        post.put("imageUrl", imageUrl);
        post.put("caption", caption);
        post.put("timestamp", System.currentTimeMillis());
        post.put("likes", new HashMap<>());
        postsRef.push().setValue(post);
    }

    private void loadPosts() {
        postsRef.orderByChild("timestamp").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                postList.clear();
                for (DataSnapshot postSnap : snapshot.getChildren()) {
                    CommunityPost post = postSnap.getValue(CommunityPost.class);
                    if (post != null) {
                        post.key = postSnap.getKey();
                        postList.add(0, post); // newest first
                    }
                }
                
                // If no posts exist and user is admin, create a test post
                if (postList.isEmpty() && isAdmin) {
                    createTestPost();
                }
                
                adapter.notifyDataSetChanged();
                Log.d("CommunityActivity", "Loaded " + postList.size() + " posts");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CommunityActivity.this, "Failed to load posts", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createTestPost() {
        Log.d("CommunityActivity", "Creating test post for admin");
        CommunityPost testPost = new CommunityPost();
        testPost.imageUrl = "https://via.placeholder.com/400x300/FF0000/FFFFFF?text=Test+Post";
        testPost.caption = "Test post created by admin";
        testPost.timestamp = System.currentTimeMillis();
        testPost.likes = new HashMap<>();
        
        postsRef.push().setValue(testPost)
            .addOnSuccessListener(aVoid -> {
                Log.d("CommunityActivity", "Test post created successfully");
                Toast.makeText(this, "Test post created for admin testing", Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> {
                Log.e("CommunityActivity", "Failed to create test post: " + e.getMessage());
            });
    }

    public static class CommunityPost {
        public String key;
        public String imageUrl;
        public String caption;
        public long timestamp;
        public Map<String, Boolean> likes = new HashMap<>();
        public CommunityPost() {}
    }

    public class CommunityAdapter extends RecyclerView.Adapter<CommunityAdapter.PostViewHolder> {
        private Context context;
        private List<CommunityPost> posts;
        private boolean isAdmin;
        
        public CommunityAdapter(Context context, List<CommunityPost> posts, boolean isAdmin) {
            this.context = context;
            this.posts = posts;
            this.isAdmin = isAdmin;
        }
        
        public void setAdmin(boolean isAdmin) {
            this.isAdmin = isAdmin;
            Log.d("CommunityAdapter", "Admin status set to: " + isAdmin);
            notifyDataSetChanged();
        }
        
        @NonNull
        @Override
        public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_community_post, parent, false);
            return new PostViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
            CommunityPost post = posts.get(position);
            Glide.with(context).load(post.imageUrl).into(holder.imageView);
            holder.caption.setText(post.caption);
            int likeCount = post.likes != null ? post.likes.size() : 0;
            holder.likeCount.setText(likeCount + " likes");
            boolean liked = post.likes != null && post.likes.containsKey(currentUserId);
            holder.likeBtn.setText(liked ? "Unlike" : "Like");
            
            // Show/hide delete button based on admin status
            Log.d("CommunityAdapter", "Binding position " + position + ", isAdmin: " + isAdmin);
            if (isAdmin) {
                holder.deleteBtn.setVisibility(View.VISIBLE);
                holder.deleteBtn.setOnClickListener(v -> deletePost(post));
                Log.d("CommunityAdapter", "Delete button made visible for position " + position);
            } else {
                holder.deleteBtn.setVisibility(View.GONE);
                Log.d("CommunityAdapter", "Delete button hidden for position " + position);
            }
            
            holder.likeBtn.setOnClickListener(v -> toggleLike(post));
            holder.shareBtn.setOnClickListener(v -> sharePost(post));
            holder.imageView.setOnClickListener(v -> showFullScreenImage(post.imageUrl));
        }
        
        @Override
        public int getItemCount() {
            return posts.size();
        }
        
        class PostViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;
            TextView caption, likeCount;
            Button likeBtn, shareBtn, deleteBtn;
            
            public PostViewHolder(@NonNull View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.communityImage);
                caption = itemView.findViewById(R.id.communityCaption);
                likeCount = itemView.findViewById(R.id.communityLikeCount);
                likeBtn = itemView.findViewById(R.id.btnLike);
                shareBtn = itemView.findViewById(R.id.btnShare);
                deleteBtn = itemView.findViewById(R.id.btnDelete);
            }
        }
        
        private void toggleLike(CommunityPost post) {
            if (currentUserId.isEmpty()) {
                Toast.makeText(context, "Please login to like posts", Toast.LENGTH_SHORT).show();
                return;
            }
            
            DatabaseReference postRef = postsRef.child(post.key).child("likes");
            if (post.likes != null && post.likes.containsKey(currentUserId)) {
                postRef.child(currentUserId).removeValue();
            } else {
                postRef.child(currentUserId).setValue(true);
            }
        }
        
        private void sharePost(CommunityPost post) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, post.caption + "\n" + post.imageUrl);
            context.startActivity(Intent.createChooser(shareIntent, "Share via"));
        }
        
        private void deletePost(CommunityPost post) {
            if (!isAdmin) {
                Toast.makeText(context, "Only admins can delete posts", Toast.LENGTH_SHORT).show();
                return;
            }
            
            new AlertDialog.Builder(context)
                .setTitle("Delete Post")
                .setMessage("Are you sure you want to delete this post?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    postsRef.child(post.key).removeValue();
                    Toast.makeText(context, "Post deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
        }
        
        private void showFullScreenImage(String imageUrl) {
            Dialog dialog = new Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
            dialog.setContentView(R.layout.dialog_fullscreen_image);
            PhotoView photoView = dialog.findViewById(R.id.photoView);
            Glide.with(context).load(imageUrl).into(photoView);
            photoView.setOnClickListener(v -> dialog.dismiss());
            dialog.show();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
} 