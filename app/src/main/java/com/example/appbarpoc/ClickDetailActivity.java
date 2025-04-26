package com.example.appbarpoc;

import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ClickDetailActivity extends AppCompatActivity {
  private TextView releaseCount;
  private LinearLayout faceContainer;
  private ImageView faceImage;
  private int count = 0;
  private int localCount = -1;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_home_detail);

    releaseCount = findViewById(R.id.releaseCount);
    faceContainer = findViewById(R.id.faceContainer);
    faceImage = findViewById(R.id.faceImage);

    // 1. 預設顯示 loading
    releaseCount.setText("載入中...");
    localCount = -1;

    // 2. 查詢今日點擊數
    getClickCount("today");

    faceContainer.setOnClickListener(v -> {
      if (localCount < 0)
        return; // 尚未查到資料時不允許點擊
      // 動畫
      Animation rotate = AnimationUtils.loadAnimation(this, R.anim.rotate);
      faceImage.startAnimation(rotate);
      // 本地+1
      localCount++;
      releaseCount.setText(getPeriodText("today", localCount));
      // 寫入Firebase
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
            .addOnSuccessListener(documentReference -> {
              getClickCount("today");
            });
      }
    });

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
        fromDate = new java.util.Date(0); // 查全部
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
