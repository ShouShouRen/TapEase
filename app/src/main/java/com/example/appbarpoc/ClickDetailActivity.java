package com.example.appbarpoc;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.graphics.Path;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
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
  private int localCount = 0;
  private int faceIndex = 0;
  private int feverProgress = 0;
  private boolean isFever = false;
  private Vibrator vibrator;
  private SoundPool soundPool;
  private int soundId1, soundId2, feverSound;
  private boolean soundsLoaded = false;
  private LottieAnimationView fireworkView;
  private long lastClickTime = 0;
  private String currentDate;

  private int[] faceImages = {
      R.drawable.emoji_1,
      R.drawable.emoji_2,
      R.drawable.emoji_3,
      R.drawable.emoji_4,
      R.drawable.emoji_5,
      R.drawable.emoji_6,
      R.drawable.emoji_7,
      R.drawable.emoji_8,
      R.drawable.emoji_9,
      R.drawable.emoji_10,
      R.drawable.emoji_11,
      R.drawable.emoji_12
  };

  private float convertIntervalToSpeed(long interval) {// 平滑轉換速速度公式
    // 限制範圍 100ms ~ 800ms
    interval = Math.max(100, Math.min(interval, 800));

    // 線性轉換為播放速率：越快點擊 → 播放越快
    float speed = 2.0f - ((interval - 100f) / 700f) * 1.3f;

    // 限定最小與最大速率
    return Math.max(0.7f, Math.min(speed, 2.0f));
  }

  @Override
  protected void onDestroy() {// 釋放資源
    super.onDestroy();
    if (soundPool != null) {
      soundPool.release();
      soundPool = null;
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.O)
  @SuppressLint({ "SetTextI18n", "ClickableViewAccessibility" })
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_home_detail);

    currentDate = new java.text.SimpleDateFormat("yyyy-MM-dd").format(new Date());

    fireworkView = findViewById(R.id.fireworkView);
    fireworkView.setAnimation("firework_animation.json");
    releaseCount = findViewById(R.id.releaseCount);
    feverText = findViewById(R.id.feverText);
    faceContainer = findViewById(R.id.faceContainer);
    faceImage = findViewById(R.id.faceImage);
    rootLayout = findViewById(R.id.rootLayout);
    feverBar = findViewById(R.id.energyBar);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      AudioAttributes audioAttributes = new AudioAttributes.Builder()
          .setUsage(AudioAttributes.USAGE_GAME)
          .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
          .build();

      soundPool = new SoundPool.Builder()
          .setMaxStreams(5) // 可同時播放的最大聲音數
          .setAudioAttributes(audioAttributes)
          .build();
    } else {
      soundPool = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
    }

    // 載入音效
    soundId1 = soundPool.load(this, R.raw.click_voice1, 1);
    soundId2 = soundPool.load(this, R.raw.click_voice2, 1);
    feverSound = soundPool.load(this, R.raw.fever_music, 1);

    // 確保音效載入完成再允許播放
    soundPool.setOnLoadCompleteListener((soundPool, sampleId, status) -> {
      if (status == 0) {
        soundsLoaded = true;
      }
    });

    // 初始化震動器
    vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

    releaseCount.setText("載入中...");
    getClickCount("today");

    faceContainer.setOnTouchListener((v, event) -> {
      if (event.getAction() == MotionEvent.ACTION_DOWN && soundsLoaded) {

        // 計算時間差
        long currentTime = System.currentTimeMillis();
        long interval = currentTime - lastClickTime;
        lastClickTime = currentTime;

        // 將間隔時間轉為播放速率（範圍建議 0.7f ～ 2.0f）
        float speed = convertIntervalToSpeed(interval);

        int soundToPlay = new Random().nextBoolean() ? soundId1 : soundId2;
        float volume = isFever ? 1.0f : 0.5f;
        soundPool.play(soundToPlay, volume, volume, 1, 0, speed);

        handleClickAt(event.getRawX(), event.getRawY());
      }

      return true;
    });

    Handler decayHandler = new Handler();
    Runnable decayRunnable = new Runnable() {
      @Override
      public void run() {
        if (!isFever && feverProgress > 0) {
          feverProgress -= 1;
          updateEnergyBarAnimated(feverProgress);
        }
        decayHandler.postDelayed(this, 1500); // 每1.5秒檢查一次
      }
    };

    decayHandler.postDelayed(decayRunnable, 1000);

  }

  private void handleClickAt(float clickX, float clickY) {
    if (localCount < 0)
      return;

    // 1. 表情切換（每 10 次）
    if (localCount % 10 == 0) {
      faceIndex = (faceIndex + 1) % faceImages.length;
      faceImage.setImageResource(faceImages[faceIndex]);
    }

    // 2. 掉落表情動畫，傳入點擊座標
    spawnFallingFace(faceImages[faceIndex], clickX, clickY);

    // 3. 震動
    if (vibrator != null) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
      }
    }

    // 4. 累積 Fever
    if (!isFever) {
      feverProgress += 1;
      updateEnergyBarAnimated(feverProgress);
      if (feverProgress >= 100) {
        startFeverMode();
      }
    }
    feverBar.setProgress(feverProgress);

    // 5. Firebase 紀錄
    localCount++;
    releaseCount.setText(getPeriodText("today", localCount));
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    if (user != null) {
      String userId = user.getUid();
      FirebaseFirestore db = FirebaseFirestore.getInstance();

      db.runTransaction(transaction -> {
        DocumentReference counterRef = db.collection("user_clicks")
            .document(userId)
            .collection("daily_counts")
            .document(currentDate);

        DocumentSnapshot snapshot = transaction.get(counterRef);

        if (!snapshot.exists()) {
          Map<String, Object> data = new HashMap<>();
          data.put("count", 1);
          data.put("date", currentDate);
          data.put("lastUpdated", new Date());
          transaction.set(counterRef, data);
        } else {
          // 如果文檔存在，增加計數
          long newCount = snapshot.getLong("count") + 1;
          transaction.update(counterRef,
              "count", newCount,
              "lastUpdated", new Date());
        }

        return null;
      }).addOnSuccessListener(aVoid -> {
        Log.d("ClickDetailActivity", "Successfully updated click count");
      }).addOnFailureListener(e -> {
        Log.e("ClickDetailActivity", "Error updating click count", e);
        localCount--;
        releaseCount.setText(getPeriodText("today", localCount));
      });
    }
  }

  private void spawnFallingFace(int drawableResId, float startX, float startY) {
    // 在點擊處新增表情
    ImageView emoji = new ImageView(this);
    emoji.setImageResource(drawableResId);
    emoji.setAlpha(0.5f);
    int size = 80;
    FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(size, size);
    emoji.setLayoutParams(params);
    rootLayout.addView(emoji);

    emoji.setX(startX - size / 2);
    emoji.setY(startY - size / 2);
    // 隨機生成每個點的落點
    Random random = new Random();
    float endX = startX + (random.nextBoolean() ? -random.nextInt(300) : random.nextInt(300));
    float endY = startY + 1000 + random.nextInt(300);

    Path path = new Path();
    path.moveTo(startX, startY);
    float controlX = (startX + endX) / 2 + random.nextInt(100) - 50;
    float controlY = startY + random.nextInt(300);
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
    // 加入濾鏡效果
    View feverOverlay = findViewById(R.id.feverOverlay);
    feverOverlay.setVisibility(View.VISIBLE);
    // 讓畫面閃爍
    AlphaAnimation blink = new AlphaAnimation(0.3f, 0.7f);
    blink.setDuration(300);
    blink.setRepeatMode(Animation.REVERSE);
    blink.setRepeatCount(Animation.INFINITE);
    feverOverlay.startAnimation(blink);
    // 加入煙火動畫效果
    fireworkView.setAnimation("firework_animation.json"); // 字串需對應 assets 下的檔名
    fireworkView.setVisibility(View.VISIBLE);
    fireworkView.bringToFront(); // 確保在最上層
    fireworkView.playAnimation();
    // 撥放fever音樂
    int feverStreamId = soundPool.play(feverSound, 1.0f, 1.0f, 1, -1, 1.0f);
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
        // Fever 結束時清除動畫
        feverOverlay.clearAnimation();
        feverOverlay.setVisibility(View.GONE);
        fireworkView.cancelAnimation();
        fireworkView.setVisibility(View.GONE);
        // 結束fever音樂
        soundPool.stop(feverStreamId);
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
    if (user == null)
      return;
    String userId = user.getUid();
    FirebaseFirestore db = FirebaseFirestore.getInstance();

    // 獲取今天的點擊計數
    db.collection("user_clicks")
        .document(userId)
        .collection("daily_counts")
        .document(currentDate)
        .get()
        .addOnSuccessListener(documentSnapshot -> {
          if (documentSnapshot.exists()) {
            Long count = documentSnapshot.getLong("count");
            if (count != null) {
              localCount = count.intValue();
              releaseCount.setText(getPeriodText("today", localCount));
            }
          } else {
            localCount = 0;
            releaseCount.setText(getPeriodText("today", 0));
          }
        })
        .addOnFailureListener(e -> {
          Log.e("ClickDetailActivity", "Error getting click count", e);
          releaseCount.setText("載入失敗");
        });
  }
}
