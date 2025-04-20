package com.example.appbarpoc;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {
  private static final String TAG = "RegisterActivity";
  private TextInputLayout nameInputLayout, emailInputLayout, passwordInputLayout, confirmPasswordInputLayout;
  private TextInputEditText nameEditText, emailEditText, passwordEditText, confirmPasswordEditText;
  private MaterialButton registerButton, loginButton;
  private FirebaseAuth mAuth;
  private FirebaseFirestore db;
  private ProgressBar progressBar;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_register);

    mAuth = FirebaseAuth.getInstance();
    db = FirebaseFirestore.getInstance();

    nameInputLayout = findViewById(R.id.nameInputLayout);
    emailInputLayout = findViewById(R.id.emailInputLayout);
    passwordInputLayout = findViewById(R.id.passwordInputLayout);
    confirmPasswordInputLayout = findViewById(R.id.confirmPasswordInputLayout);

    nameEditText = findViewById(R.id.nameEditText);
    emailEditText = findViewById(R.id.emailEditText);
    passwordEditText = findViewById(R.id.passwordEditText);
    confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
    registerButton = findViewById(R.id.registerButton);
    loginButton = findViewById(R.id.loginButton);
    progressBar = findViewById(R.id.progressBar);

    registerButton.setOnClickListener(v -> registerUser());

    loginButton.setOnClickListener(v -> {
      startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
    });

    setupTextChangeListeners();
  }

  private void setupTextChangeListeners() {
    nameEditText.setOnFocusChangeListener((v, hasFocus) -> {
      if (hasFocus)
        nameInputLayout.setError(null);
    });
    emailEditText.setOnFocusChangeListener((v, hasFocus) -> {
      if (hasFocus)
        emailInputLayout.setError(null);
    });
    passwordEditText.setOnFocusChangeListener((v, hasFocus) -> {
      if (hasFocus)
        passwordInputLayout.setError(null);
    });
    confirmPasswordEditText.setOnFocusChangeListener((v, hasFocus) -> {
      if (hasFocus)
        confirmPasswordInputLayout.setError(null);
    });
  }

  private void registerUser() {
    clearAllErrors();

    if (validateInput()) {
      String name = nameEditText.getText().toString().trim();
      String email = emailEditText.getText().toString().trim();
      String password = passwordEditText.getText().toString().trim();

      Log.d(TAG, "開始註冊流程");

      progressBar.setVisibility(View.VISIBLE);
      registerButton.setEnabled(false);
      loginButton.setEnabled(false);

      mAuth.createUserWithEmailAndPassword(email, password)
          .addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
              FirebaseUser user = mAuth.getCurrentUser();
              if (user != null) {
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("name", name);
                userMap.put("email", email);

                db.collection("users").document(user.getUid())
                    .set(userMap)
                    .addOnSuccessListener(aVoid -> {
                      Log.d(TAG, "用戶資料已成功儲存到 Firestore");
                      Toast.makeText(RegisterActivity.this, "註冊成功！", Toast.LENGTH_SHORT).show();
                      startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                      finish();
                    })
                    .addOnFailureListener(e -> {
                      Log.w(TAG, "儲存用戶資料到 Firestore 時發生錯誤", e);
                      emailInputLayout.setError("儲存用戶資料時發生錯誤");
                      resetLoadingState();
                    });
              }
            } else {
              Log.w(TAG, "註冊失敗", task.getException());
              handleRegistrationError(task.getException());
              resetLoadingState();
            }
          });
    }
  }

  private void handleRegistrationError(Exception exception) {
    if (exception instanceof FirebaseAuthWeakPasswordException) {
      passwordInputLayout.setError("密碼太弱，請確保密碼符合以下規則：\n" +
          "• 至少 8 個字元\n" +
          "• 至少包含一個大寫字母\n" +
          "• 至少包含一個小寫字母\n" +
          "• 至少包含一個數字\n" +
          "• 至少包含一個特殊符號");
    } else if (exception instanceof FirebaseAuthInvalidCredentialsException) {
      emailInputLayout.setError("電子郵件格式無效");
    } else if (exception instanceof FirebaseAuthUserCollisionException) {
      emailInputLayout.setError("此電子郵件已被使用");
    } else {
      emailInputLayout.setError("註冊失敗，請稍後再試");
    }
  }

  private void resetLoadingState() {
    progressBar.setVisibility(View.GONE);
    registerButton.setEnabled(true);
    loginButton.setEnabled(true);
  }

  private void clearAllErrors() {
    nameInputLayout.setError(null);
    emailInputLayout.setError(null);
    passwordInputLayout.setError(null);
    confirmPasswordInputLayout.setError(null);
  }

  private boolean validateInput() {
    boolean isValid = true;
    String name = nameEditText.getText().toString().trim();
    String email = emailEditText.getText().toString().trim();
    String password = passwordEditText.getText().toString().trim();
    String confirmPassword = confirmPasswordEditText.getText().toString().trim();

    if (name.isEmpty()) {
      nameInputLayout.setError("請輸入姓名");
      isValid = false;
    }

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
    } else if (!isPasswordStrong(password)) {
      passwordInputLayout.setError("密碼必須符合以下規則：\n" +
          "• 至少 8 個字元\n" +
          "• 至少包含一個大寫字母\n" +
          "• 至少包含一個小寫字母\n" +
          "• 至少包含一個數字\n" +
          "• 至少包含一個特殊符號");
      isValid = false;
    }

    if (confirmPassword.isEmpty()) {
      confirmPasswordInputLayout.setError("請確認密碼");
      isValid = false;
    } else if (!password.equals(confirmPassword)) {
      confirmPasswordInputLayout.setError("密碼不一致");
      isValid = false;
    }

    return isValid;
  }

  private boolean isPasswordStrong(String password) {
    if (password.length() < 8) {
      return false;
    }

    if (!password.matches(".*[A-Z].*")) {
      return false;
    }

    if (!password.matches(".*[a-z].*")) {
      return false;
    }

    if (!password.matches(".*\\d.*")) {
      return false;
    }

    if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
      return false;
    }

    return true;
  }
}
