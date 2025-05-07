package com.example.appbarpoc.receiver;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.appbarpoc.MainActivity;

public class AlarmReceiver extends BroadcastReceiver {
    /*
    * 通知頻道 ID
    * 通知唯一識別碼
    * */
    private static final String CHANNEL_ID = "DailyNotificationChannel";
    private static final int NOTIFICATION_ID = 2001;

    @Override
    public void onReceive(Context context, Intent intent) {
        // 呼叫通知頻道並顯示通知
        createNotificationChannel(context);
        showNotification(context);
    }

    private void createNotificationChannel(Context context) {
        // 檢查當前安卓版本如果在 API 26 以上版本就建立通知頻道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 通知頻道名稱與描述
            CharSequence name = "Daily Notification Channel";
            String description = "Channel for daily notifications";
            // 通知的重要性設定成預設一般通知
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            // 創建通知頻道
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // 向系統註冊通知頻道
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);

            // 如果成功獲取通知管理器，則創建並註冊通知頻道
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    /*
    * 顯示每日通知提醒
    * */
    private void showNotification(Context context) {
        Intent mainIntent = new Intent(context, MainActivity.class);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_IMMUTABLE : 0;
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, mainIntent, flags | PendingIntent.FLAG_UPDATE_CURRENT);
        // 取得到系統通知管理器
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        // 建立通知
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info) // 通知的 icon
                .setContentTitle("每日提醒") // 通知的title
                .setContentText("快來記錄今天的心情吧！") // 通知內容
                .setPriority(NotificationCompat.PRIORITY_DEFAULT) // 設置通知優先級
                .setContentIntent(pendingIntent) // 設置點擊通知的行為
                .setAutoCancel(true); // 點擊後自動移除通知


        // 如果成功獲取通知管理器，則發送通知
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        }
    }
}