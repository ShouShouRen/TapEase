package com.example.appbarpoc;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
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
  private TextView feverText;
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
    feverText = findViewById(R.id.feverText);
    faceContainer = findViewById(R.id.faceContainer);
    faceImage = findViewById(R.id.faceImage);
    rootLayout = findViewById(R.id.rootLayout);
    feverBar = findViewById(R.id.energyBar);

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
      if(localCount%10==0) {//每點10下進行一次切換
        faceIndex = (faceIndex + 1) % faceImages.length;
        faceImage.setImageResource(faceImages[faceIndex]);
      }

      // 2. 製造掉落表情動畫
      spawnFallingFace(faceImages[faceIndex]);

      // 3. 每次點擊都震動
      if (vibrator != null) {
        vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
      }

      // 4. Fever 模式累積能量
      if (!isFever) {
        feverProgress += 1;
        updateEnergyBarAnimated(feverProgress);// EnergyBar動畫更新
        if (feverProgress >= 100) {
          startFeverMode();
        }
      }

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

  private void spawnFallingFace(int drawableResId) {
    ImageView emoji = new ImageView(this);
    emoji.setImageResource(drawableResId);
    emoji.setAlpha(0.5f);
    int size = 80;
    FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(size, size);
    emoji.setLayoutParams(params);

    rootLayout.addView(emoji);
    emoji.setX(faceContainer.getX() + faceContainer.getWidth() / 2 - size / 2);
    emoji.setY(faceContainer.getY());

    // 隨機向左下或右下
    Random random = new Random();
    float endX = emoji.getX() + (random.nextBoolean() ? -random.nextInt(300) : random.nextInt(300));
    float endY = emoji.getY() + 1000 + random.nextInt(300);

    Path path = new Path();
    path.moveTo(emoji.getX(), emoji.getY());
    float controlX = (emoji.getX() + endX) / 2 + random.nextInt(100) - 50;
    float controlY = emoji.getY() + random.nextInt(300);
    path.quadTo(controlX, controlY, endX, endY);

    ObjectAnimator animator = ObjectAnimator.ofFloat(emoji, View.X, View.Y, path);
    animator.setDuration(2000);
    animator.addListener(new AnimatorListenerAdapter() {
      @Override
      public void onAnimationEnd(Animator animation) {
        ((ViewGroup) emoji.getParent()).removeView(emoji);
      }
    });
    animator.start();
  }

  private void startFeverMode() {
    isFever = true;
    feverBar.setProgress(100);
      CountDownTimer feverTimer = new CountDownTimer(30000, 1000) {
          @Override
          public void onTick(long millisUntilFinished) {
              feverText.setText("Fever模式中！" + (millisUntilFinished / 1000) + "秒剩餘");
          }

          @Override
          public void onFinish() {
              isFever = false;
              feverProgress = 0;
              feverBar.setProgress(0);
              feverText.setText(""); // Fever 結束後清除文字
              releaseCount.setText(getPeriodText("today", localCount));
          }
      }.start();
  }

  private void updateEnergyBarAnimated(int progress) {
    ObjectAnimator progressAnimator = ObjectAnimator.ofInt(feverBar, "progress", feverBar.getProgress(), progress);
    progressAnimator.setDuration(300); // 動畫時長
    progressAnimator.setInterpolator(new DecelerateInterpolator());
    progressAnimator.start();
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
