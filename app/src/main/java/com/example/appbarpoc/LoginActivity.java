package com.example.appbarpoc;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private TextInputLayout emailInputLayout, passwordInputLayout;
    private TextInputEditText emailEditText, passwordEditText;
    private MaterialButton loginButton, registerButton;
    private FirebaseAuth mAuth;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        emailInputLayout = findViewById(R.id.emailInputLayout);
        passwordInputLayout = findViewById(R.id.passwordInputLayout);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);
        progressBar = findViewById(R.id.progressBar);

        loginButton.setOnClickListener(v -> loginUser());
        registerButton.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });

        setupTextChangeListeners();
    }

    private void setupTextChangeListeners() {
        emailEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus)
                emailInputLayout.setError(null);
        });
        passwordEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus)
                passwordInputLayout.setError(null);
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        }
    }

    private void loginUser() {
        clearAllErrors();

        if (validateInput()) {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            showLoadingState();

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        } else {
                            Log.w(TAG, "登入失敗", task.getException());
                            handleLoginError(task.getException());
                        }
                        resetLoadingState();
                    });
        }
    }

    private void handleLoginError(Exception exception) {
        if (exception instanceof FirebaseAuthInvalidUserException) {
            emailInputLayout.setError("此電子郵件尚未註冊");
        } else if (exception instanceof FirebaseAuthInvalidCredentialsException) {
            // 如果是密碼錯誤，顯示在密碼輸入框下方
            passwordInputLayout.setError("密碼錯誤，請重新輸入");
        } else {
            // 其他錯誤顯示在電子郵件輸入框下方
            emailInputLayout.setError("登入失敗，請稍後再試");
        }
    }

    private void showLoadingState() {
        progressBar.setVisibility(View.VISIBLE);
        loginButton.setEnabled(false);
        registerButton.setEnabled(false);
    }

    private void resetLoadingState() {
        progressBar.setVisibility(View.GONE);
        loginButton.setEnabled(true);
        registerButton.setEnabled(true);
    }

    private void clearAllErrors() {
        emailInputLayout.setError(null);
        passwordInputLayout.setError(null);
    }

    private boolean validateInput() {
        boolean isValid = true;
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (email.isEmpty()) {
            emailInputLayout.setError("請輸入電子郵件");
            isValid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInputLayout.setError("請輸入有效的電子郵件地址");
            isValid = false;
        }

        if (password.isEmpty()) {
            passwordInputLayout.setError("請輸入密碼");
            isValid = false;
        }

        return isValid;
    }
}
