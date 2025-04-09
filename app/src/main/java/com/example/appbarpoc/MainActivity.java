package com.example.appbarpoc;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 設置底部導航欄
        BottomNavigationView navView = findViewById(R.id.nav_view);
        // 定義頂部應用欄配置
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home,
                R.id.navigation_statistics,
                R.id.navigation_leaderboard,
                R.id.navigation_relaxation)
                .build();

        // 設置導航控制器
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        // 如果您想在頂部顯示標題
        // NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        // 設置底部導航與導航控制器的聯動
        NavigationUI.setupWithNavController(navView, navController);
    }
}