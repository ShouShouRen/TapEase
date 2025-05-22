package com.example.appbarpoc;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class YellDetailActivity extends AppCompatActivity {

    private AudioRecord audioRecord;
    private boolean isRecording = false;
    private double maxDecibel = 0.0;
    private Handler handler = new Handler(Looper.getMainLooper());
    private static final int SAMPLE_RATE = 44100;
    private static final int RECORDING_DURATION = 10000;
    private static final int REQUEST_AUDIO_PERMISSION_CODE = 1;

    private Button startButton;
    private ProgressBar volumeProgress;
    private TextView decibelText;
    private TextView maxDecibelText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_yell_detail);

        // 用 findViewById 綁定元件
        startButton = findViewById(R.id.start_button);
        volumeProgress = findViewById(R.id.volume_progress);
        decibelText = findViewById(R.id.decibel_text);
        maxDecibelText = findViewById(R.id.max_decibel_text);

        // 檢查權限
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestAudioPermission();
        }

        // 按鈕點擊
        startButton.setOnClickListener(v -> {
            if (!isRecording) {
                if (ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    startRecording();
                    startButton.setText("錄音中...");
                    maxDecibel = 0.0;
                } else {
                    Toast.makeText(this, "請授予錄音權限", Toast.LENGTH_SHORT).show();
                    requestAudioPermission();
                }
            }
        });
    }

    private void requestAudioPermission() {
        ActivityCompat.requestPermissions(this,
                new String[] { Manifest.permission.RECORD_AUDIO },
                REQUEST_AUDIO_PERMISSION_CODE);
    }

    private void startRecording() {
        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize);

        isRecording = true;
        audioRecord.startRecording();

        volumeProgress.setProgress(0);
        maxDecibelText.setText("最大分貝: 0.0 dB");

        new Thread(() -> {
            short[] buffer = new short[bufferSize];
            long startTime = System.currentTimeMillis();

            while (isRecording && (System.currentTimeMillis() - startTime) < RECORDING_DURATION) {
                int read = audioRecord.read(buffer, 0, buffer.length);
                double amplitude = getAmplitude(buffer, read);
                double decibel = calculateDecibel(amplitude);

                if (decibel > maxDecibel) {
                    maxDecibel = decibel;
                }

                handler.post(() -> {
                    volumeProgress.setProgress((int) decibel);
                    decibelText.setText(String.format("目前分貝: %.1f dB", decibel));
                });

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            stopRecording();
        }).start();
    }

    private double getAmplitude(short[] buffer, int read) {
        if (read <= 0)
            return 0;
        double sum = 0;
        for (int i = 0; i < read; i++) {
            sum += buffer[i] * buffer[i];
        }
        return Math.sqrt(sum / read);
    }

    private double calculateDecibel(double amplitude) {
        if (amplitude <= 0)
            return 0;
        double referenceAmplitude = 1.0;
        double ratio = amplitude / referenceAmplitude;
        double db = 20 * Math.log10(ratio);
        return Math.max(0, db);
    }

    private void stopRecording() {
        if (audioRecord != null) {
            isRecording = false;
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;

            handler.post(() -> {
                startButton.setText("開始錄音");
                maxDecibelText.setText(String.format("最大分貝: %.1f dB", maxDecibel));
            });

            // 錄音結束時寫入 Firestore
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                String userId = user.getUid();
                FirebaseFirestore db = FirebaseFirestore.getInstance();

                // 獲取今天的日期字串
                String todayStr = new java.text.SimpleDateFormat("yyyy-MM-dd").format(new Date());

                // 使用事務來更新音量記錄
                db.runTransaction(transaction -> {
                    DocumentReference recordRef = db.collection("user_yells")
                            .document(userId)
                            .collection("daily_records")
                            .document(todayStr);

                    DocumentSnapshot snapshot = transaction.get(recordRef);

                    if (!snapshot.exists()) {
                        // 如果文檔不存在，創建新文檔
                        Map<String, Object> data = new HashMap<>();
                        data.put("maxDecibel", maxDecibel);
                        data.put("lastUpdated", new Date());
                        transaction.set(recordRef, data);
                    } else {
                        // 如果文檔存在，更新最大分貝值（如果新的更大）
                        Double currentMax = snapshot.getDouble("maxDecibel");
                        if (currentMax == null || maxDecibel > currentMax) {
                            transaction.update(recordRef,
                                    "maxDecibel", maxDecibel,
                                    "lastUpdated", new Date());
                        }
                    }

                    return null;
                }).addOnFailureListener(e -> {
                    Log.e("YellDetailActivity", "Error updating volume record", e);
                });
            }
        }
    }

    // 移到 class 裡面
    public void Onback(View view) {
        finish();
    }
}
