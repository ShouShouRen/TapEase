package com.example.appbarpoc.ui.home;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class EmojiAnimator {
    private final FrameLayout container;
    private final Context context;
    private final List<Integer> emojiResIds;
    private final Random random = new Random();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean running = false;
    private int containerWidth = 0;
    private int containerHeight = 0;
    private final List<ImageView> activeEmojis = new ArrayList<>();
    private static final int MAX_EMOJIS = 6;

    public EmojiAnimator(Context context, FrameLayout container, List<Integer> emojiResIds) {
        this.context = context;
        this.container = container;
        this.emojiResIds = emojiResIds;
    }

    public void start() {
        running = true;
        container.post(() -> {
            containerWidth = container.getWidth();
            containerHeight = container.getHeight();
            handler.post(spawnEmojiRunnable);
        });
    }

    public void stop() {
        running = false;
        handler.removeCallbacksAndMessages(null);
        container.removeAllViews();
        activeEmojis.clear();
    }

    private final Runnable spawnEmojiRunnable = new Runnable() {
        @Override
        public void run() {
            if (!running)
                return;
            if (activeEmojis.size() < MAX_EMOJIS) {
                spawnEmoji();
            }
            handler.postDelayed(this, 1800 + random.nextInt(1200));
        }
    };

    private void spawnEmoji() {
        if (containerWidth == 0 || containerHeight == 0)
            return;
        int resId = emojiResIds.get(random.nextInt(emojiResIds.size()));
        ImageView emoji = new ImageView(context);
        emoji.setImageDrawable(ContextCompat.getDrawable(context, resId));
        int size = dpToPx(32 + random.nextInt(16));
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(size, size);
        params.topMargin = containerHeight - size;
        params.leftMargin = random.nextInt(Math.max(1, containerWidth - size));
        emoji.setLayoutParams(params);
        container.addView(emoji);
        activeEmojis.add(emoji);
        randomMoveEmoji(emoji, size);
    }

    private void randomMoveEmoji(ImageView emoji, int size) {
        if (!running)
            return;
        // 有10%機率直接飛出螢幕消失
        if (random.nextInt(10) == 0) {
            flyOutAnimation(emoji, size);
            return;
        }
        int targetX = random.nextInt(Math.max(1, containerWidth - size));
        int minY = 0;
        int maxY = containerHeight - size;
        int targetY = minY + random.nextInt(Math.max(1, maxY - minY + 1));
        // 50% 機率用跳躍動畫
        if (random.nextBoolean()) {
            jumpTo(emoji, size, targetX, targetY);
        } else {
            smoothMoveTo(emoji, size, targetX, targetY);
        }
    }

    private void smoothMoveTo(ImageView emoji, int size, int targetX, int targetY) {
        ObjectAnimator moveX = ObjectAnimator.ofFloat(emoji, "translationX", emoji.getTranslationX(),
                targetX - emoji.getLeft());
        ObjectAnimator moveY = ObjectAnimator.ofFloat(emoji, "translationY", emoji.getTranslationY(),
                targetY - emoji.getTop());
        int duration = 3200 + random.nextInt(2000);
        moveX.setDuration(duration);
        moveY.setDuration(duration);
        moveX.setInterpolator(new AccelerateDecelerateInterpolator());
        moveY.setInterpolator(new AccelerateDecelerateInterpolator());
        // 30% 機率加旋轉
        ObjectAnimator rotate = null;
        if (random.nextInt(10) < 3) {
            float from = emoji.getRotation();
            float to = from + (random.nextBoolean() ? 360f : -360f);
            rotate = ObjectAnimator.ofFloat(emoji, "rotation", from, to);
            rotate.setDuration(duration);
            rotate.setInterpolator(new AccelerateDecelerateInterpolator());
            rotate.start();
        }
        moveX.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (running && container.indexOfChild(emoji) != -1) {
                    handler.postDelayed(() -> randomMoveEmoji(emoji, size), 900 + random.nextInt(800));
                }
            }
        });
        moveX.start();
        moveY.start();
    }

    private void jumpTo(ImageView emoji, int size, int targetX, int targetY) {
        final float startX = emoji.getTranslationX();
        final float startY = emoji.getTranslationY();
        final float endX = targetX - emoji.getLeft();
        final float endY = targetY - emoji.getTop();
        final float midX = (startX + endX) / 2f;
        final float midY = Math.min(startY, endY) - dpToPx(32 + random.nextInt(32));
        int duration = 3200 + random.nextInt(2000);
        ValueAnimator jumpAnim = ValueAnimator.ofFloat(0f, 1f);
        jumpAnim.setDuration(duration);
        jumpAnim.setInterpolator(new BounceInterpolator());
        // 30% 機率加旋轉
        ObjectAnimator rotate = null;
        if (random.nextInt(10) < 3) {
            float from = emoji.getRotation();
            float to = from + (random.nextBoolean() ? 360f : -360f);
            rotate = ObjectAnimator.ofFloat(emoji, "rotation", from, to);
            rotate.setDuration(duration);
            rotate.setInterpolator(new AccelerateDecelerateInterpolator());
            rotate.start();
        }
        jumpAnim.addUpdateListener(animation -> {
            float t = (float) animation.getAnimatedValue();
            float x = (1 - t) * (1 - t) * startX + 2 * (1 - t) * t * midX + t * t * endX;
            float y = (1 - t) * (1 - t) * startY + 2 * (1 - t) * t * midY + t * t * endY;
            emoji.setTranslationX(x);
            emoji.setTranslationY(y);
        });
        jumpAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (running && container.indexOfChild(emoji) != -1) {
                    handler.postDelayed(() -> randomMoveEmoji(emoji, size), 900 + random.nextInt(800));
                }
            }
        });
        jumpAnim.start();
    }

    private void flyOutAnimation(ImageView emoji, int size) {
        int direction = random.nextBoolean() ? 1 : -1;
        float fromX = emoji.getTranslationX();
        float toX = fromX + direction * (containerWidth + size);
        float fromY = emoji.getTranslationY();
        float toY = fromY - dpToPx(32 + random.nextInt(32));
        ObjectAnimator moveX = ObjectAnimator.ofFloat(emoji, "translationX", fromX, toX);
        ObjectAnimator moveY = ObjectAnimator.ofFloat(emoji, "translationY", fromY, toY);
        int duration = 3800 + random.nextInt(1200);
        moveX.setDuration(duration);
        moveY.setDuration(duration);
        moveX.setInterpolator(new AccelerateDecelerateInterpolator());
        moveY.setInterpolator(new AccelerateDecelerateInterpolator());
        // 30% 機率加旋轉
        ObjectAnimator rotate = null;
        if (random.nextInt(10) < 3) {
            float from = emoji.getRotation();
            float to = from + (random.nextBoolean() ? 360f : -360f);
            rotate = ObjectAnimator.ofFloat(emoji, "rotation", from, to);
            rotate.setDuration(duration);
            rotate.setInterpolator(new AccelerateDecelerateInterpolator());
            rotate.start();
        }
        moveX.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                container.removeView(emoji);
                activeEmojis.remove(emoji);
            }
        });
        moveX.start();
        moveY.start();
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                context.getResources().getDisplayMetrics());
    }
}
