package com.example.dripblood.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dripblood.R;
import com.example.dripblood.adapters.ChatAdapter;
import com.example.dripblood.classifier.MedicalEmbeddings;
import com.example.dripblood.models.ChatMessage;
import com.example.dripblood.utils.VoiceRecognitionHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BloodDonationChatActivity extends AppCompatActivity implements VoiceRecognitionHelper.VoiceRecognitionCallback {
    
    private RecyclerView chatRecyclerView;
    private EditText messageInput;
    private ImageButton sendButton;
    private ImageButton voiceButton;
    private ProgressBar progressBar;
    private TextView statusText;
    
    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatMessages;
    private MedicalEmbeddings medicalEmbeddings;
    private VoiceRecognitionHelper voiceRecognitionHelper;
    private ExecutorService executorService;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medical_chat);
        
        // Initialize views
        initViews();
        
        // Initialize chat
        initChat();
        
        // Initialize TFLite embeddings
        initEmbeddings();
        
        // Initialize voice recognition
        initVoiceRecognition();
        
        // Initialize thread pool
        executorService = Executors.newSingleThreadExecutor();
        
        // Add welcome message
        addBotMessage("🩸 Hello! I'm your Blood Donation Assistant. I can help you with information about blood donation, eligibility, process, safety, and more. You can type your questions or tap the microphone to speak. How can I help you today?");
    }
    
    private void initViews() {
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        voiceButton = findViewById(R.id.voiceButton);
        progressBar = findViewById(R.id.progressBar);
        statusText = findViewById(R.id.statusText);
        
        // Update title
        TextView titleText = findViewById(R.id.titleText);
        if (titleText != null) {
            titleText.setText("Blood Donation Assistant");
        }
        
        sendButton.setOnClickListener(v -> sendMessage());
        voiceButton.setOnClickListener(v -> toggleVoiceInput());
        
        // Enable send on Enter key
        messageInput.setOnEditorActionListener((v, actionId, event) -> {
            sendMessage();
            return true;
        });
    }
    
    private void initChat() {
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessages);
        
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);
    }
    
    private void initEmbeddings() {
        try {
            medicalEmbeddings = new MedicalEmbeddings(this);
            updateStatus("AI Blood Donation Assistant Ready", true);
        } catch (Exception e) {
            updateStatus("AI Model Loading Failed", false);
            Toast.makeText(this, "Error loading AI model: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void initVoiceRecognition() {
        voiceRecognitionHelper = new VoiceRecognitionHelper(this, this);
    }
    
    private void toggleVoiceInput() {
        if (voiceRecognitionHelper.isListening()) {
            voiceRecognitionHelper.stopListening();
        } else {
            voiceRecognitionHelper.startListening();
        }
    }
    
    private void sendMessage() {
        String message = messageInput.getText().toString().trim();
        if (message.isEmpty()) {
            return;
        }
        
        // Add user message to chat
        addUserMessage(message);
        messageInput.setText("");
        
        // Show loading
        setLoading(true);
        
        // Process with TFLite model in background
        executorService.execute(() -> {
            String response = processMessage(message);
            
            // Update UI on main thread
            runOnUiThread(() -> {
                setLoading(false);
                addBotMessage(response);
            });
        });
    }
    
    private String processMessage(String message) {
        if (medicalEmbeddings == null) {
            return "Sorry, the AI model is not available. Please restart the app.";
        }
        
        try {
            // Get embedding for the message
            float[] embedding = medicalEmbeddings.getEmbedding(message);
            
            // Get response based on the message
            String response = medicalEmbeddings.getBloodDonationResponse(message);
            
            return response;
        } catch (Exception e) {
            return "I'm having trouble processing your question. Please try again.";
        }
    }
    
    private void addUserMessage(String message) {
        ChatMessage chatMessage = new ChatMessage(message, true);
        chatMessages.add(chatMessage);
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        scrollToBottom();
    }
    
    private void addBotMessage(String message) {
        ChatMessage chatMessage = new ChatMessage(message, false);
        chatMessages.add(chatMessage);
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        scrollToBottom();
    }
    
    private void scrollToBottom() {
        if (chatMessages.size() > 0) {
            chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
        }
    }
    
    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        sendButton.setEnabled(!loading);
        voiceButton.setEnabled(!loading);
    }
    
    private void updateStatus(String status, boolean isReady) {
        statusText.setText(status);
        statusText.setTextColor(getResources().getColor(
            isReady ? android.R.color.holo_green_dark : android.R.color.holo_orange_dark
        ));
    }
    
    // Voice Recognition Callbacks
    @Override
    public void onSpeechResult(String result) {
        // Set the recognized text in the input field
        messageInput.setText(result);
        
        // Add user message to chat
        addUserMessage(result);
        
        // Show loading
        setLoading(true);
        
        // Process with TFLite model in background
        executorService.execute(() -> {
            String response = processMessage(result);
            
            // Update UI on main thread
            runOnUiThread(() -> {
                setLoading(false);
                addBotMessage(response);
            });
        });
    }
    
    @Override
    public void onSpeechError(String error) {
        Toast.makeText(this, "Voice Error: " + error, Toast.LENGTH_SHORT).show();
        updateVoiceButtonState(false);
    }
    
    @Override
    public void onListeningStarted() {
        updateVoiceButtonState(true);
        updateStatus("Listening... Speak now", false);
    }
    
    @Override
    public void onListeningStopped() {
        updateVoiceButtonState(false);
        updateStatus("AI Blood Donation Assistant Ready", true);
    }
    
    private void updateVoiceButtonState(boolean isListening) {
        voiceButton.setSelected(isListening);
        voiceButton.setEnabled(!isListening || !progressBar.isShown());
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        voiceRecognitionHelper.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
        if (medicalEmbeddings != null) {
            medicalEmbeddings.close();
        }
        if (voiceRecognitionHelper != null) {
            voiceRecognitionHelper.destroy();
        }
    }
} 