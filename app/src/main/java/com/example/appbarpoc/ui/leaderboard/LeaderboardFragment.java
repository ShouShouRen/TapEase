package com.example.appbarpoc.ui.leaderboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.appbarpoc.R;
import com.example.appbarpoc.databinding.FragmentLeaderboardBinding;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class LeaderboardFragment extends Fragment {

    private Spinner spinnerPeriod;
    private BarChart barChartClicks;
    private BarChart barChartDecibels;
    private String[] periods = {"today", "week", "month", "year"};
    private int currentPeriodIndex = 0;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_leaderboard, container, false);
        Spinner spinnerChartType = root.findViewById(R.id.spinnerChartType);
        ArrayAdapter<CharSequence> chartTypeAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.chart_type_options, android.R.layout.simple_spinner_item);
        chartTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerChartType.setAdapter(chartTypeAdapter);
        spinnerChartType.setSelection(0);
        spinnerPeriod = root.findViewById(R.id.spinnerPeriod);
        barChartClicks = root.findViewById(R.id.barChartClicks);
        barChartDecibels = root.findViewById(R.id.barChartDecibels);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.statistics_periods, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPeriod.setAdapter(adapter);
        spinnerPeriod.setSelection(0);
        spinnerChartType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    barChartClicks.setVisibility(View.VISIBLE);
                    barChartDecibels.setVisibility(View.GONE);
                } else {
                    barChartClicks.setVisibility(View.GONE);
                    barChartDecibels.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        spinnerPeriod.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentPeriodIndex = position;
                fetchLeaderboardData();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        return root;
    }

    private void fetchLeaderboardData() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Calendar cal = Calendar.getInstance();
        Date fromDate;
        switch (periods[currentPeriodIndex]) {
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

        db.collection("users")
                .get()
                .addOnSuccessListener(userSnapshots -> {
                    Map<String, String> userIdNameMap = new HashMap<>();
                    for (QueryDocumentSnapshot userDoc : userSnapshots) {
                        String userId = userDoc.getId();
                        String name = userDoc.getString("name");
                        if (name != null) {
                            userIdNameMap.put(userId, name);
                        }
                    }

                    Map<String, Integer> userClickCounts = new HashMap<>();
                    Map<String, Double> userMaxDecibels = new HashMap<>();

                    List<String> userIds = new ArrayList<>(userIdNameMap.keySet());
                    AtomicInteger processedUsers = new AtomicInteger(0);

                    for (String userId : userIds) {
                        db.collection("user_clicks")
                                .document(userId)
                                .collection("daily_counts")
                                .whereGreaterThanOrEqualTo("lastUpdated", fromDate)
                                .get()
                                .addOnSuccessListener(clickSnapshots -> {
                                    int totalClicks = 0;
                                    for (QueryDocumentSnapshot doc : clickSnapshots) {
                                        Long count = doc.getLong("count");
                                        if (count != null) {
                                            totalClicks += count.intValue();
                                        }
                                    }
                                    userClickCounts.put(userId, totalClicks);

                                    db.collection("user_yells")
                                            .document(userId)
                                            .collection("daily_records")
                                            .whereGreaterThanOrEqualTo("lastUpdated", fromDate)
                                            .get()
                                            .addOnSuccessListener(yellSnapshots -> {
                                                double maxDecibel = 0.0;
                                                for (QueryDocumentSnapshot doc : yellSnapshots) {
                                                    Double d = doc.getDouble("maxDecibel");
                                                    if (d != null && d > maxDecibel) {
                                                        maxDecibel = d;
                                                    }
                                                }
                                                userMaxDecibels.put(userId, maxDecibel);

                                                if (processedUsers.incrementAndGet() == userIds.size()) {
                                                    displayLeaderboard(userIdNameMap, userClickCounts, userMaxDecibels);
                                                }
                                            });
                                });
                    }
                });
    }

    private void displayLeaderboard(Map<String, String> userIdNameMap,
                                    Map<String, Integer> userClickCounts,
                                    Map<String, Double> userMaxDecibels) {
        List<Map.Entry<String, Integer>> clickList = new ArrayList<>(userClickCounts.entrySet());
        clickList.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));
        List<Map.Entry<String, Double>> decibelList = new ArrayList<>(userMaxDecibels.entrySet());
        decibelList.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));

        List<BarEntry> clickEntries = new ArrayList<>();
        List<String> clickLabels = new ArrayList<>();
        for (int i = 0; i < Math.min(3, clickList.size()); i++) {
            Map.Entry<String, Integer> entry = clickList.get(i);
            clickEntries.add(new BarEntry(i, entry.getValue()));
            clickLabels.add(userIdNameMap.get(entry.getKey()));
        }

        List<BarEntry> decibelEntries = new ArrayList<>();
        List<String> decibelLabels = new ArrayList<>();
        for (int i = 0; i < Math.min(3, decibelList.size()); i++) {
            Map.Entry<String, Double> entry = decibelList.get(i);
            decibelEntries.add(new BarEntry(i, entry.getValue().floatValue()));
            decibelLabels.add(userIdNameMap.get(entry.getKey()));
        }

        BarDataSet clickDataSet = new BarDataSet(clickEntries, "點擊數");
        clickDataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        BarData clickData = new BarData(clickDataSet);
        barChartClicks.setData(clickData);
        barChartClicks.getXAxis().setValueFormatter(new IndexAxisValueFormatter(clickLabels));
        barChartClicks.getXAxis().setGranularity(1f);
        barChartClicks.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        barChartClicks.getAxisLeft().setAxisMinimum(0f);
        barChartClicks.getAxisRight().setEnabled(false);
        barChartClicks.getDescription().setText("點擊數排行榜");
        barChartClicks.animateY(1000);
        barChartClicks.invalidate();

        BarDataSet decibelDataSet = new BarDataSet(decibelEntries, "最大音量 (dB)");
        decibelDataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        BarData decibelData = new BarData(decibelDataSet);
        barChartDecibels.setData(decibelData);
        barChartDecibels.getXAxis().setValueFormatter(new IndexAxisValueFormatter(decibelLabels));
        barChartDecibels.getXAxis().setGranularity(1f);
        barChartDecibels.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        barChartDecibels.getAxisLeft().setAxisMinimum(0f);
        barChartDecibels.getAxisRight().setEnabled(false);
        barChartDecibels.getDescription().setText("最大音量排行榜");
        barChartDecibels.animateY(1000);
        barChartDecibels.invalidate();
    }
}
