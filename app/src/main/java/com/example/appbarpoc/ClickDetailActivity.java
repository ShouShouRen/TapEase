package com.example.appbarpoc;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class ClickDetailActivity extends AppCompatActivity {
  private TextView releaseCount;
  private LinearLayout faceContainer;
  private ImageView faceImage;
  private ProgressBar feverBar;
  private FrameLayout rootLayout;
  private int localCount = -1;
  private int faceIndex = 0;
  private int feverProgress = 0;
  private boolean isFever = false;
  private Vibrator vibrator;

    private int[] faceImages = {
          R.drawable.face_happy,
          R.drawable.face_angry,
          R.drawable.face_cry
  };

  @RequiresApi(api = Build.VERSION_CODES.O)
  @SuppressLint("SetTextI18n")
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_home_detail);

    releaseCount = findViewById(R.id.releaseCount);
    faceContainer = findViewById(R.id.faceContainer);
    faceImage = findViewById(R.id.faceImage);
    rootLayout = findViewById(R.id.rootLayout);

    // 初始化震動器
    vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

    // 建立能量條 Fever Bar
    feverBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
    feverBar.setMax(100);
    feverBar.setProgress(0);
    feverBar.setLayoutParams(new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            20
    ));
    ((ViewGroup) rootLayout).addView(feverBar);

    releaseCount.setText("載入中...");
    localCount = -1;
    getClickCount("today");

    faceContainer.setOnClickListener(v -> {
      if (localCount < 0) return;

      // 1. 切換表情圖
      faceIndex = (faceIndex + 1) % faceImages.length;
      faceImage.setImageResource(faceImages[faceIndex]);

      // 2. 製造掉落表情動畫
      spawnFallingFace();

      // 3. 每次點擊都震動
      if (vibrator != null) {
        vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
      }

      // 4. Fever 模式累積能量
      if (!isFever) {
        feverProgress += 1;
        if (feverProgress >= 100) {
          startFeverMode();
        }
      }
      feverBar.setProgress(feverProgress);

      // 5. Firebase 點擊記錄
      localCount++;
      releaseCount.setText(getPeriodText("today", localCount));

      FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
      if (user != null) {
        String userId = user.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> data = new HashMap<>();
        data.put("timestamp", new Date());
        db.collection("user_clicks")
                .document(userId)
                .collection("clicks")
                .add(data)
                .addOnSuccessListener(documentReference -> getClickCount("today"));
      }
    });
  }

  private void spawnFallingFace() {
    ImageView newFace = new ImageView(this);
    Drawable drawable = ContextCompat.getDrawable(this, faceImages[faceIndex]);
    newFace.setImageDrawable(drawable);
    newFace.setAlpha(0.5f);
    newFace.setScaleX(0.5f);
    newFace.setScaleY(0.5f);
    newFace.setLayoutParams(new FrameLayout.LayoutParams(150, 150));
    newFace.setX(faceContainer.getX() + 100);
    newFace.setY(faceContainer.getY() + 200);
    rootLayout.addView(newFace);

    ObjectAnimator animator = ObjectAnimator.ofFloat(newFace, "translationY", newFace.getY(), rootLayout.getHeight());
    animator.setDuration(isFever ? 700 : 1500);
    animator.setInterpolator(new AccelerateInterpolator());
    animator.start();

    animator.addListener(new android.animation.AnimatorListenerAdapter() {
      @Override
      public void onAnimationEnd(android.animation.Animator animation) {
        rootLayout.removeView(newFace);
      }
    });
  }

  private void startFeverMode() {
    isFever = true;
    feverBar.setProgress(100);
      CountDownTimer feverTimer = new CountDownTimer(30000, 1000) {
          @Override
          public void onTick(long millisUntilFinished) {
              releaseCount.setText("Fever模式中！" + (millisUntilFinished / 1000) + "秒剩餘");
          }

          @Override
          public void onFinish() {
              isFever = false;
              feverProgress = 0;
              feverBar.setProgress(0);
              releaseCount.setText(getPeriodText("today", localCount));
          }
      }.start();
  }

  public void Onback(View view) {
    finish();
  }

  private String getPeriodText(String period, int count) {
    switch (period) {
      case "today":
        return String.format("今日已釋放 %d 次", count);
      case "week":
        return String.format("近 7 天已釋放 %d 次", count);
      case "month":
        return String.format("近 30 天已釋放 %d 次", count);
      case "year":
        return String.format("今年已釋放 %d 次", count);
      default:
        return String.format("已釋放 %d 次", count);
    }
  }

  private void getClickCount(String period) {
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    if (user == null) return;
    String userId = user.getUid();
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    java.util.Calendar cal = java.util.Calendar.getInstance();
    java.util.Date fromDate;
    switch (period) {
      case "today":
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        fromDate = cal.getTime();
        break;
      case "week":
        cal.add(java.util.Calendar.DAY_OF_YEAR, -6);
        fromDate = cal.getTime();
        break;
      case "month":
        cal.add(java.util.Calendar.DAY_OF_YEAR, -29);
        fromDate = cal.getTime();
        break;
      case "year":
        cal.add(java.util.Calendar.DAY_OF_YEAR, -364);
        fromDate = cal.getTime();
        break;
      default:
        fromDate = new java.util.Date(0);
    }
    db.collection("user_clicks")
            .document(userId)
            .collection("clicks")
            .whereGreaterThanOrEqualTo("timestamp", fromDate)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
              int clickCount = queryDocumentSnapshots.size();
              localCount = clickCount;
              releaseCount.setText(getPeriodText(period, clickCount));
            });
  }
}
