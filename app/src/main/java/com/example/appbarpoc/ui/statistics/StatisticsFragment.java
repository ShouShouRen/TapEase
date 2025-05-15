package com.example.appbarpoc.ui.statistics;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.appbarpoc.R;
import com.example.appbarpoc.databinding.FragmentStatisticsBinding;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class StatisticsFragment extends Fragment {

    private FragmentStatisticsBinding binding;
    private Switch switchChartType;
    private Spinner spinnerPeriod;
    private BarChart barChart;
    private PieChart pieChart;
    private TextView textView;
    private String[] periods = { "today", "week", "month", "year" };
    private int currentPeriodIndex = 0;
    private boolean isBarChart = true;
    private int clickCount = 0;
    private double maxDecibel = 0.0;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentStatisticsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        switchChartType = binding.switchChartType;
        spinnerPeriod = binding.spinnerPeriod;
        barChart = binding.barChart;
        pieChart = binding.pieChart;
        textView = binding.textDashboard;

        // Spinner 設定
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.statistics_periods, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPeriod.setAdapter(adapter);
        spinnerPeriod.setSelection(0);
        spinnerPeriod.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentPeriodIndex = position;
                fetchStatistics();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // Switch 設定
        switchChartType.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isBarChart = !isChecked; // Switch 開啟時顯示 PieChart
            updateChart();
        });

        // 預設顯示
        isBarChart = true;
        switchChartType.setChecked(false);
        fetchStatistics();
        return root;
    }

    private void fetchStatistics() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null)
            return;
        String userId = user.getUid();
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
        db.collection("user_clicks")
                .document(userId)
                .collection("clicks")
                .whereGreaterThanOrEqualTo("timestamp", fromDate)
                .get()
                .addOnSuccessListener(clickSnapshots -> {
                    clickCount = clickSnapshots.size();
                    db.collection("user_yells")
                            .document(userId)
                            .collection("records")
                            .whereGreaterThanOrEqualTo("timestamp", fromDate)
                            .get()
                            .addOnSuccessListener(yellSnapshots -> {
                                maxDecibel = 0.0;
                                for (QueryDocumentSnapshot doc : yellSnapshots) {
                                    Double d = doc.getDouble("maxDecibel");
                                    if (d != null && d > maxDecibel) {
                                        maxDecibel = d;
                                    }
                                }
                                updateChart();
                                textView.setText(getPeriodText(periods[currentPeriodIndex], clickCount, maxDecibel));
                            });
                });
    }

    private void updateChart() {
        if (isBarChart) {
            barChart.setVisibility(View.VISIBLE);
            pieChart.setVisibility(View.GONE);
            showBarChart();
        } else {
            barChart.setVisibility(View.GONE);
            pieChart.setVisibility(View.VISIBLE);
            showPieChart();
        }
    }

    private void showBarChart() {
        List<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0, clickCount));
        entries.add(new BarEntry(1, (float) maxDecibel));
        BarDataSet dataSet = new BarDataSet(entries, "統計數據");
        dataSet.setColors(new int[] {
                ContextCompat.getColor(requireContext(), R.color.purple_500),
                ContextCompat.getColor(requireContext(), R.color.teal_700)
        });
        dataSet.setValueTextSize(16f);
        dataSet.setBarShadowColor(ContextCompat.getColor(requireContext(), R.color.gray));
        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.35f);
        barChart.setData(barData);

        final String[] labels = new String[] { "釋放次數", "最大分貝" };
        barChart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value >= 0 && value < labels.length) {
                    return labels[(int) value];
                } else {
                    return "";
                }
            }
        });
        barChart.getXAxis().setGranularity(1f);
        barChart.getXAxis().setGranularityEnabled(true);
        barChart.getXAxis().setDrawGridLines(false);
        barChart.getXAxis().setDrawAxisLine(false);
        barChart.getXAxis().setTextSize(14f);
        barChart.getXAxis().setPosition(com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM);

        barChart.getAxisLeft().setDrawGridLines(false);
        barChart.getAxisLeft().setAxisMinimum(0f);
        barChart.getAxisLeft().setTextSize(14f);
        barChart.getAxisRight().setEnabled(false);

        Description desc = new Description();
        desc.setText("");
        barChart.setDescription(desc);
        barChart.getLegend().setEnabled(false);

        barChart.animateY(800);
        barChart.invalidate();
    }

    private void showPieChart() {
        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(clickCount, "釋放次數"));
        entries.add(new PieEntry((float) maxDecibel, "最大分貝"));
        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(new int[] {
                ContextCompat.getColor(requireContext(), R.color.purple_500),
                ContextCompat.getColor(requireContext(), R.color.teal_700)
        });
        dataSet.setValueTextSize(16f);
        dataSet.setSliceSpace(8f);
        dataSet.setSelectionShift(10f);
        PieData pieData = new PieData(dataSet);
        pieChart.setData(pieData);

        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleRadius(50f);
        pieChart.setTransparentCircleRadius(55f);
        pieChart.setHoleColor(ContextCompat.getColor(requireContext(), android.R.color.white));

        Description desc = new Description();
        desc.setText("");
        pieChart.setDescription(desc);
        pieChart.getLegend().setEnabled(true);
        pieChart.getLegend().setTextSize(14f);
        pieChart.setEntryLabelTextSize(14f);

        pieChart.animateY(800);
        pieChart.invalidate();
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
