package com.example.appbarpoc;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;


public class ClickDetailActivity extends AppCompatActivity {
  private TextView releaseCount ;
  private LinearLayout faceContainer;
  private int count = 0;


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_home_detail);

    releaseCount = findViewById(R.id.releaseCount);
    faceContainer = findViewById(R.id.faceContainer);


    faceContainer.setOnClickListener(v -> {
      count++;
      releaseCount.setText(String.format("今日已釋放 %d 次", count));
    });



  }

  public void Onback(View view) {
    finish();
  }
}
