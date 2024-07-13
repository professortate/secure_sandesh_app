package com.example.chat3;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private DatabaseReference messagesRef;
    private MessageAdapter messageAdapter;
    private RecyclerView recyclerView;
    private List<MessageItem> messageList;
    private TextInputEditText messageInput;
    private ImageView sendButton;
    private FirebaseAuth mAuth;
    private TextView userEmailTextView;
    private Button signOutButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageList);
        recyclerView.setAdapter(messageAdapter);

        userEmailTextView = findViewById(R.id.userEmailTextView);
        Log.d(TAG, "userEmailTextView: " + userEmailTextView); // Add this line
        signOutButton = findViewById(R.id.signOutButton);

        if (currentUser == null) {
            startActivity(new Intent(MainActivity.this, SignInActivity.class));
            finish();
            return;
        }

        userEmailTextView.setText(currentUser.getEmail());

        signOutButton.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(MainActivity.this, SignInActivity.class));
            finish();
        });

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        messagesRef = database.getReference("messages");

        messagesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                messageList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    MessageItem messageItem = snapshot.getValue(MessageItem.class);
                    if (messageItem != null && messageItem.getMessage() != null) {
                        messageItem.setMessage(decryptMessage(messageItem.getMessage()));
                        messageList.add(messageItem);
                    }
                }
                Collections.sort(messageList, (m1, m2) -> m2.getTimestamp().compareTo(m1.getTimestamp()));
                messageAdapter.notifyDataSetChanged();
                recyclerView.scrollToPosition(0);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to read value.", databaseError.toException());
            }
        });

        messageInput = findViewById(R.id.message_input);
        sendButton = findViewById(R.id.send_button);

        messageInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                sendButton.setVisibility(s == null || s.length() == 0 ? View.GONE : View.VISIBLE);
            }
        });

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });
    }

    private void sendMessage() {
        String messageText = messageInput.getText().toString().trim();
        if (!messageText.isEmpty()) {
            String senderName = "Badu";
            String timestamp = getCurrentTimestamp();

            String encryptedMessage = encryptMessage(messageText);
            MessageItem messageItem = new MessageItem(senderName, encryptedMessage, timestamp);
            messagesRef.push().setValue(messageItem);
            messageItem.setMessage(messageText); // Set original text for display
            messageAdapter.addMessage(messageItem);
            messageInput.setText("");
        }
    }

    private String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    private String encryptMessage(String message) {
        StringBuilder encrypted = new StringBuilder();
        for (char c : message.toCharArray()) {
            if (Character.isLetter(c)) {
                char base = Character.isUpperCase(c) ? 'A' : 'a';
                char encryptedChar = (char) (base + ('z' - c));
                encrypted.append(encryptedChar);
            } else {
                encrypted.append(c);
            }
        }
        return encrypted.toString();
    }

    private String decryptMessage(String message) {
        if (message == null) {
            return "";  // or return null, depending on how you want to handle it
        }
        StringBuilder decrypted = new StringBuilder();
        for (char c : message.toCharArray()) {
            if (Character.isLetter(c)) {
                char base = Character.isUpperCase(c) ? 'A' : 'a';
                char decryptedChar = (char) (base + ('z' - c));
                decrypted.append(decryptedChar);
            } else {
                decrypted.append(c);
            }
        }
        return decrypted.toString();
    }
}