package com.example.appbarpoc.ui.relaxation;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.appbarpoc.R;
import com.example.appbarpoc.databinding.FragmentRelaxationBinding;

public class RelaxationFragment extends Fragment {

    private FragmentRelaxationBinding binding;
    private Vibrator vibrator;
    private View animationCircle;
    private TextView phaseText;
    private TextView countdownText;
    private Button startButton;

    private int inhaleColor;
    private int holdColor;
    private int exhaleColor;

    private RelaxationViewModel viewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        viewModel = new ViewModelProvider(this).get(RelaxationViewModel.class);
        binding = FragmentRelaxationBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        animationCircle = binding.circleView;
        phaseText = binding.textPhase;
        countdownText = binding.textCountdown;
        startButton = binding.startButton;

        inhaleColor = ContextCompat.getColor(requireContext(), R.color.inhale);
        holdColor = ContextCompat.getColor(requireContext(), R.color.hold);
        exhaleColor = ContextCompat.getColor(requireContext(), R.color.exhale);

        vibrator = (Vibrator) requireContext().getSystemService(Context.VIBRATOR_SERVICE);

        viewModel.getBreathingPhase().observe(getViewLifecycleOwner(), phase -> {
            phaseText.setText(phase);
            triggerAnimationAndVibration(phase);
        });

        viewModel.getTimeRemaining().observe(getViewLifecycleOwner(), seconds -> {
            countdownText.setText(seconds + " 秒");
        });

        startButton.setOnClickListener(v -> {
            viewModel.reset();
            viewModel.startBreathing();
        });

        return root;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void triggerAnimationAndVibration(String phase) {
        int targetColor;
        float scale;

        switch (phase) {
            case "吸氣":
                targetColor = inhaleColor;
                scale = 1.4f;
                vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
                break;
            case "閉氣":
                targetColor = holdColor;
                scale = 1.0f;
                break;
            case "吐氣":
                targetColor = exhaleColor;
                scale = 0.6f;
                vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE));
                break;
            default:
                return;
        }

        animationCircle.animate()
                .scaleX(scale)
                .scaleY(scale)
                .setDuration(800)
                .start();

        Drawable bg = animationCircle.getBackground();
        if (bg instanceof GradientDrawable) {
            GradientDrawable gradientDrawable = (GradientDrawable) bg;
            ValueAnimator colorAnim = ValueAnimator.ofObject(new ArgbEvaluator(), Color.WHITE, targetColor);
            colorAnim.setDuration(1000);
            colorAnim.addUpdateListener(animator -> {
                gradientDrawable.setColor((int) animator.getAnimatedValue());
            });
            colorAnim.start();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

