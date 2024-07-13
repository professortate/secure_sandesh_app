package com.example.chat3;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class SignUpActivity extends AppCompatActivity {

    private static final String TAG = "SignUpActivity";
    private EditText emailEditText, passwordEditText, phoneNumberEditText, verificationCodeEditText;
    private Button signUpButton, sendVerificationCodeButton, verifyCodeButton;

    private FirebaseAuth mAuth;
    private String mVerificationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        mAuth = FirebaseAuth.getInstance();

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        phoneNumberEditText = findViewById(R.id.phoneNumberEditText);
        verificationCodeEditText = findViewById(R.id.verificationCodeEditText);
        signUpButton = findViewById(R.id.signUpButton);
        sendVerificationCodeButton = findViewById(R.id.sendVerificationCodeButton);
        verifyCodeButton = findViewById(R.id.verifyCodeButton);

        signUpButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString();
            String password = passwordEditText.getText().toString();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(SignUpActivity.this, "Please enter email and password", Toast.LENGTH_SHORT).show();
                return;
            }

            createAccount(email, password);
        });

        sendVerificationCodeButton.setOnClickListener(v -> {
            String phoneNumber = phoneNumberEditText.getText().toString();

            if (TextUtils.isEmpty(phoneNumber)) {
                Toast.makeText(SignUpActivity.this, "Please enter a phone number", Toast.LENGTH_SHORT).show();
                return;
            }

            sendVerificationCode(phoneNumber);
        });

        verifyCodeButton.setOnClickListener(v -> {
            String code = verificationCodeEditText.getText().toString();

            if (TextUtils.isEmpty(code)) {
                Toast.makeText(SignUpActivity.this, "Please enter the verification code", Toast.LENGTH_SHORT).show();
                return;
            }

            verifyCode(code);
        });
    }

    private void createAccount(String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "createUserWithEmail:success");
                            Toast.makeText(SignUpActivity.this, "Sign Up Successful", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(SignUpActivity.this, MainActivity.class));
                            finish();
                        } else {
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            Toast.makeText(SignUpActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void sendVerificationCode(String phoneNumber) {
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(mCallbacks)
                .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void verifyCode(String code) {
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, code);
        linkCredential(credential);
    }

    private void linkCredential(PhoneAuthCredential credential) {
        FirebaseAuth.getInstance().getCurrentUser()
                .linkWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "linkWithCredential:success");
                            Toast.makeText(SignUpActivity.this, "Phone number linked", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(SignUpActivity.this, MainActivity.class));
                            finish();
                        } else {
                            Log.w(TAG, "linkWithCredential:failure", task.getException());
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                Toast.makeText(SignUpActivity.this, "Invalid verification code", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
    }

    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks =
            new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                @Override
                public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                    linkCredential(credential);
                }

                @Override
                public void onVerificationFailed(@NonNull FirebaseException e) {
                    Log.w(TAG, "onVerificationFailed", e);
                    Toast.makeText(SignUpActivity.this, "Verification failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                    mVerificationId = verificationId;
                    Toast.makeText(SignUpActivity.this, "Verification code sent", Toast.LENGTH_SHORT).show();
                }
            };
}