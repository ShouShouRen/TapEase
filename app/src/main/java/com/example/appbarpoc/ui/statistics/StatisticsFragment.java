package com.example.appbarpoc.ui.statistics;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.appbarpoc.R;
import com.example.appbarpoc.databinding.FragmentStatisticsBinding;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.Calendar;
import java.util.Date;

public class StatisticsFragment extends Fragment {

    private FragmentStatisticsBinding binding;
    private TabLayout tabLayout;
    private TextView textView;
    private String[] periods = { "today", "week", "month", "year" };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
        StatisticsViewModel dashboardViewModel = new ViewModelProvider(this).get(StatisticsViewModel.class);

        binding = FragmentStatisticsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        tabLayout = binding.tabLayout;
        textView = binding.textDashboard;
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_today));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_week));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_month));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_year));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int pos = tab.getPosition();
                getClickCount(periods[pos]);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
        tabLayout.getTabAt(0).select();
        getClickCount("today");
        return root;
    }

    private void getClickCount(String period) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null)
            return;
        String userId = user.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Calendar cal = Calendar.getInstance();
        Date fromDate;
        switch (period) {
            case "today":
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                fromDate = cal.getTime();
                break;
            case "week":
                cal.add(Calendar.DAY_OF_YEAR, -6);
                fromDate = cal.getTime();
                break;
            case "month":
                cal.add(Calendar.DAY_OF_YEAR, -29);
                fromDate = cal.getTime();
                break;
            case "year":
                cal.add(Calendar.DAY_OF_YEAR, -364);
                fromDate = cal.getTime();
                break;
            default:
                fromDate = new Date(0);
        }
        db.collection("user_clicks")
                .document(userId)
                .collection("clicks")
                .whereGreaterThanOrEqualTo("timestamp", fromDate)
                .get()
                .addOnSuccessListener(clickSnapshots -> {
                    int clickCount = clickSnapshots.size();
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
                                textView.setText(getPeriodText(period, clickCount, maxDecibel));
                            });
                });
    }

    private String getPeriodText(String period, int count, double maxDecibel) {
        switch (period) {
            case "today":
                return String.format("今日已釋放 %d 次，最大分貝 %.1f dB", count, maxDecibel);
            case "week":
                return String.format("近 7 天已釋放 %d 次，最大分貝 %.1f dB", count, maxDecibel);
            case "month":
                return String.format("近 30 天已釋放 %d 次，最大分貝 %.1f dB", count, maxDecibel);
            case "year":
                return String.format("今年已釋放 %d 次，最大分貝 %.1f dB", count, maxDecibel);
            default:
                return String.format("已釋放 %d 次，最大分貝 %.1f dB", count, maxDecibel);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
