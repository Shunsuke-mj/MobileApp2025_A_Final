package jp.ac.meijou.android.mobileapp2025_a_final;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

public class MainActivity2 extends AppCompatActivity {

    private FusedLocationProviderClient fusedLocationClient;
    private double latitude = 0;    //緯度
    private double longitude = 0;   //経度

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main2);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // ボタンのインスタンスを取得
        Button sendButton = findViewById(R.id.button);

        // ボタンにクリックリスナーを設定
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 画面1（MainActivity）へのIntentを作成
                Intent intent = new Intent(MainActivity2.this, MainActivity.class);

                // 緯度と経度をIntentに格納
                // キー名は他の場所と重複しないように、パッケージ名などを含めるとより安全です。
                intent.putExtra("EXTRA_LATITUDE", latitude);
                intent.putExtra("EXTRA_LONGITUDE", longitude);

                // 画面1を起動
                startActivity(intent);
            }
        });


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            // Got last known location. In some rare situations this can be null.
                            if (location != null) {
                                // Logic to handle location object
                                latitude = location.getLatitude();
                                longitude = location.getLongitude();
                                
                                // Update status text
                                TextView statusText = findViewById(R.id.textStatus);
                                if (statusText != null) {
                                    statusText.setText("位置情報の取得が完了しました！");
                                    statusText.setTextColor(0xFF4CAF50); // Green color
                                }
                                
                                // Optionally keep updating hidden views if needed for debugging
                                TextView text4 = findViewById(R.id.textView);
                                TextView text5 = findViewById(R.id.textView2);
                                if (text4 != null) text4.setText(String.valueOf(latitude));
                                if (text5 != null) text5.setText(String.valueOf(longitude));
                            }
                        }
                    });
        } else {
            //権限がない場合
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1000);
        }
    }
}