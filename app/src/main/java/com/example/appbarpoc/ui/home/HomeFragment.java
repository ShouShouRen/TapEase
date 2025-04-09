package com.example.appbarpoc.ui.home;

import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.appbarpoc.R;
import com.example.appbarpoc.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;
    private int releaseCount = 0;
    private boolean isRelaxed = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        LinearLayout faceContainer = binding.faceContainer;
        ImageView faceImage = binding.faceImage;
        TextView releaseText = binding.releaseCount;

        faceContainer.setOnClickListener(v -> {
            releaseCount++;
            releaseText.setText("今日已釋放 " + releaseCount + " 次");

            faceImage.setImageResource(R.drawable.baseline_pie_chart_24);

            ScaleAnimation scale = new ScaleAnimation(
                    1.0f, 1.2f, 1.0f, 1.2f,
                    ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                    ScaleAnimation.RELATIVE_TO_SELF, 0.5f);
            scale.setDuration(150);
            scale.setRepeatCount(1);
            scale.setRepeatMode(ScaleAnimation.REVERSE);
            faceImage.startAnimation(scale);

            // 震動
//            Vibrator vibrator = (Vibrator) requireContext().getSystemService(getContext().VIBRATOR_SERVICE);
//            if (vibrator != null && vibrator.hasVibrator()) {
//                vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
//            }

            new Handler().postDelayed(() -> {
                faceImage.setImageResource(R.drawable.baseline_favorite_24);
            }, 1000);
        });

        return root;
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}