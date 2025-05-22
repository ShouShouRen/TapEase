package com.example.appbarpoc;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AchievementActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private AchievementAdapter adapter;
    private List<Achievement> achievementList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_achievement);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        setContentView(R.layout.activity_achievement);

        recyclerView = findViewById(R.id.achievementRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        achievementList = new ArrayList<>();
        getClickCountAndCheckAchievements();
        getMaxDecibelAndCheckAchievements();
        checkLoginStreakAndUnlockAchievement();
        adapter = new AchievementAdapter(this, achievementList);
        recyclerView.setAdapter(adapter);

    }

    public void Onback(View view) {
        finish();
    }

    private void getClickCountAndCheckAchievements() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            // 獲取所有日期的點擊計數
            db.collection("user_clicks")
                    .document(userId)
                    .collection("daily_counts")
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        int totalClicks = 0;
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            Long count = doc.getLong("count");
                            if (count != null) {
                                totalClicks += count;
                            }
                        }
                        checkAchievements(totalClicks); // 檢查成就
                    })
                    .addOnFailureListener(e -> {
                        Log.e("AchievementActivity", "Error getting click count", e);
                    });
        }
    }

    private void getMaxDecibelAndCheckAchievements() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            // 假設我們要查詢最近30天的資料
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_YEAR, -30);
            Date fromDate = calendar.getTime();

            db.collection("user_yells")
                    .document(userId)
                    .collection("records")
                    .whereGreaterThanOrEqualTo("timestamp", fromDate)
                    .get()
                    .addOnSuccessListener(yellSnapshots -> {
                        double maxDecibel = 0.0;
                        for (QueryDocumentSnapshot doc : yellSnapshots) {
                            Double d = doc.getDouble("maxDecibel");
                            if (d != null && d > maxDecibel) {
                                maxDecibel = d;
                            }
                        }

                        // 這裡根據最大音量解鎖成就
                        checkVolumeAchievements(maxDecibel);
                    });
        }
    }

    private void checkLoginStreakAndUnlockAchievement() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null)
            return;

        String userId = user.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        DocumentReference userRef = db.collection("user_loginDays").document(userId);
        userRef.get().addOnSuccessListener(documentSnapshot -> {
            Log.d("LoginStreak", "成功讀取用戶資料");

            Date today = getDateOnly(new Date());
            Date lastLoginDate = documentSnapshot.getDate("lastLoginDate");
            Long streak = documentSnapshot.getLong("streakCount");

            final long[] newStreakCount = { (streak == null) ? 0 : streak };

            Calendar cal = Calendar.getInstance();
            cal.setTime(today);
            cal.add(Calendar.DAY_OF_YEAR, -1);
            Date yesterday = cal.getTime();

            if (!documentSnapshot.exists()) {
                // 首次創建使用者資料
                Log.d("LoginStreak", "首次登入，用戶文件尚不存在，正在創建");
                newStreakCount[0] = 1;
            } else if (lastLoginDate == null || lastLoginDate.before(yesterday)) {
                // 非連續登入
                newStreakCount[0] = 1;
            } else if (!lastLoginDate.equals(today)) {
                // 昨天有登入
                newStreakCount[0]++;
            }

            Map<String, Object> updateData = new HashMap<>();
            updateData.put("lastLoginDate", today);
            updateData.put("streakCount", newStreakCount[0]);

            userRef.set(updateData, SetOptions.merge()).addOnSuccessListener(unused -> {
                Log.d("LoginStreak", "成功寫入 streakCount: " + newStreakCount[0]);
                checkStreakAchievements((int) newStreakCount[0]);
            }).addOnFailureListener(e -> {
                Log.e("LoginStreak", "寫入失敗", e);
            });

        }).addOnFailureListener(e -> {
            Log.e("LoginStreak", "讀取用戶資料失敗", e);
        });
    }

    private void checkVolumeAchievements(double maxDecibel) {
        if (maxDecibel >= 0)
            achievementList.add(new Achievement(R.drawable.ic_volume_bronze, "感謝參與：音量達到 0 dB"));
        if (maxDecibel >= 80)
            achievementList.add(new Achievement(R.drawable.ic_volume_bronze, "吼到爆：音量達到 80 dB"));
        if (maxDecibel >= 100)
            achievementList.add(new Achievement(R.drawable.ic_volume_silver, "雷霆咆哮：音量達到 100 dB"));
        if (maxDecibel >= 120)
            achievementList.add(new Achievement(R.drawable.ic_volume_gold, "地鳴級怒吼：音量達到 120 dB"));

        adapter.notifyDataSetChanged(); // 更新 RecyclerView 畫面
    }

    private void checkAchievements(int clickCount) {
        achievementList.clear(); // 清除舊資料

        if (clickCount >= 100)
            achievementList.add(new Achievement(R.drawable.ic_click_bronze, "點擊達到100次"));
        if (clickCount >= 500)
            achievementList.add(new Achievement(R.drawable.ic_click_bronze, "點擊達到500次"));
        if (clickCount >= 1000)
            achievementList.add(new Achievement(R.drawable.ic_click_silver, "點擊達到1000次"));
        if (clickCount >= 10000)
            achievementList.add(new Achievement(R.drawable.ic_click_gold, "點擊達到10000次"));

        adapter.notifyDataSetChanged(); // 通知 RecyclerView 更新畫面
    }

    private void checkStreakAchievements(int streakCount) {
        if (streakCount >= 1)
            achievementList.add(new Achievement(R.drawable.ic_streak_bronze, "連續登入 1 天"));
        if (streakCount >= 3)
            achievementList.add(new Achievement(R.drawable.ic_streak_bronze, "三日不斷：連續登入 3 天"));
        if (streakCount >= 7)
            achievementList.add(new Achievement(R.drawable.ic_streak_silver, "一週堅持：連續登入 7 天"));
        if (streakCount >= 30)
            achievementList.add(new Achievement(R.drawable.ic_streak_gold, "毅力之王：連續登入 30 天"));

        adapter.notifyDataSetChanged(); // 更新畫面
    }

    public class Achievement {
        private int iconResId;
        private String description;

        public Achievement(int iconResId, String description) {
            this.iconResId = iconResId;
            this.description = description;
        }

        public int getIconResId() {
            return iconResId;
        }

        public String getDescription() {
            return description;
        }
    }

    public class AchievementAdapter extends RecyclerView.Adapter<AchievementAdapter.ViewHolder> {
        private List<Achievement> achievements;
        private Context context;

        public AchievementAdapter(Context context, List<Achievement> achievements) {
            this.context = context;
            this.achievements = achievements;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_achievement, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Achievement achievement = achievements.get(position);
            holder.icon.setImageResource(achievement.getIconResId());
            holder.description.setText(achievement.getDescription());

            holder.shareButton.setOnClickListener(v -> {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, "我解鎖了成就：「" + achievement.getDescription() + "」快來看看吧！");
                context.startActivity(Intent.createChooser(shareIntent, "分享成就"));
            });
        }

        @Override
        public int getItemCount() {
            return achievements.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            ImageView icon;
            TextView description;
            Button shareButton;

            public ViewHolder(View itemView) {
                super(itemView);
                icon = itemView.findViewById(R.id.achievementIcon);
                description = itemView.findViewById(R.id.achievementDescription);
                shareButton = itemView.findViewById(R.id.shareButton);
            }
        }
    }

    private Date getDateOnly(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }
}
