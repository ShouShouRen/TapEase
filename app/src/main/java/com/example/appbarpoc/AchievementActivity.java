package com.example.appbarpoc;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

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
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;


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
            CollectionReference clicksRef = db.collection("user_clicks").document(userId).collection("clicks");

            clicksRef.get().addOnSuccessListener(queryDocumentSnapshots -> {
                int clickCount = queryDocumentSnapshots.size();
                checkAchievements(clickCount); // 檢查成就
            });
        }
    }

    private void checkAchievements(int clickCount) {
        achievementList.clear(); // 清除舊資料

        if (clickCount >= 100)
            achievementList.add(new Achievement(R.drawable.baseline_achievement, "點擊達到100次"));
        if (clickCount >= 500)
            achievementList.add(new Achievement(R.drawable.baseline_achievement, "點擊達到500次"));
        if (clickCount >= 1000)
            achievementList.add(new Achievement(R.drawable.baseline_achievement, "點擊達到1000次"));
        if (clickCount >= 10000)
            achievementList.add(new Achievement(R.drawable.baseline_achievement, "點擊達到10000次"));

        adapter.notifyDataSetChanged(); // 通知 RecyclerView 更新畫面
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

}