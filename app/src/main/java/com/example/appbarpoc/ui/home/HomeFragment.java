package com.example.appbarpoc.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.example.appbarpoc.AchievementActivity;
import com.example.appbarpoc.ClickDetailActivity;
import com.example.appbarpoc.R;
import com.example.appbarpoc.YellDetailActivity;
import com.example.appbarpoc.MessageDetailActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {
    FirebaseAuth mAuth = FirebaseAuth.getInstance();
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    FirebaseUser user = mAuth.getCurrentUser();
    private static final String TAG = "HomeFragment";
    private EmojiAnimator emojiAnimator;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        try {
            CardView smileCard = root.findViewById(R.id.smileCard);
            CardView countCard = root.findViewById(R.id.countCard);
            CardView messageCard = root.findViewById(R.id.messageCard);
            CardView AchievementCard = root.findViewById(R.id.AchievementCard);

            if (smileCard != null) {
                smileCard.setOnClickListener(v -> {
                    try {
                        if (getActivity() != null) {
                            Intent intent = new Intent(getActivity(), ClickDetailActivity.class);
                            startActivity(intent);
                        } else {
                            Log.e(TAG, "Activity is null when trying to start DetailActivity");
                            Toast.makeText(requireContext(), "無法開啟詳情頁面", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error starting DetailActivity", e);
                        Toast.makeText(requireContext(), "開啟詳情頁面時發生錯誤", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Log.e(TAG, "smileCard view not found");
            }

            if (countCard != null) {
                countCard.setOnClickListener(v -> {
                    Intent intent = new Intent(getActivity(), YellDetailActivity.class);
                    startActivity(intent);
                });
            } else {
                Log.e(TAG, "countCard view not found");
            }
            if (messageCard != null) {
                messageCard.setOnClickListener(v -> {
                    Intent intent = new Intent(getActivity(), MessageDetailActivity.class);
                    startActivity(intent);
                });
            } else {
                Log.e(TAG, "messageCard view not found");
            }
            if (AchievementCard != null) {
                AchievementCard.setOnClickListener(v -> {
                    Intent intent = new Intent(getActivity(), AchievementActivity.class);
                    startActivity(intent);
                });
            } else {
                Log.e(TAG, "AchievementCard view not found");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreateView", e);
        }

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        TextView userNameTextView = root.findViewById(R.id.HelloUserName);

        if (user != null) {
            db.collection("users").document(user.getUid()).get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            String name = document.getString("name");
                            userNameTextView.setText(name);
                        }
                    });
        }

        // emoji動畫初始化
        FrameLayout emojiContainer = root.findViewById(R.id.emojiContainer);
        List<Integer> emojiResIds = new ArrayList<>();
        emojiResIds.add(R.drawable.emoji_1);
        emojiResIds.add(R.drawable.emoji_2);
        emojiResIds.add(R.drawable.emoji_3);
        emojiResIds.add(R.drawable.emoji_4);
        emojiResIds.add(R.drawable.emoji_5);
        emojiResIds.add(R.drawable.emoji_6);
        emojiResIds.add(R.drawable.emoji_7);
        emojiResIds.add(R.drawable.emoji_8);
        emojiResIds.add(R.drawable.emoji_9);
        emojiResIds.add(R.drawable.emoji_10);
        emojiResIds.add(R.drawable.emoji_11);
        emojiResIds.add(R.drawable.emoji_12);
        emojiAnimator = new EmojiAnimator(requireContext(), emojiContainer, emojiResIds);
        emojiAnimator.start();

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "HomeFragment resumed");
        if (emojiAnimator != null) emojiAnimator.start();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (emojiAnimator != null) emojiAnimator.stop();
    }
}
