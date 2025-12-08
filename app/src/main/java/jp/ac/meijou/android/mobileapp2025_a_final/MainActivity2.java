package jp.ac.meijou.android.mobileapp2025_a_final;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText; // Added
import android.widget.TextView;
import android.widget.Toast; // Added

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
import com.squareup.moshi.JsonAdapter; // Added
import com.squareup.moshi.Moshi; // Added

import java.io.IOException; // Added

import okhttp3.Call; // Added
import okhttp3.Callback; // Added
import okhttp3.OkHttpClient; // Added
import okhttp3.Request; // Added
import okhttp3.Response; // Added

public class MainActivity2 extends AppCompatActivity {

    private FusedLocationProviderClient fusedLocationClient;
    private double latitude = 0;    //緯度
    private double longitude = 0;   //経度
    
    // API stuff
    private final OkHttpClient client = new OkHttpClient();
    private final Moshi moshi = new Moshi.Builder().build();

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

        // 郵便番号検索ボタンの設定
        Button searchPostalButton = findViewById(R.id.buttonSearchPostal);
        EditText postalInput = findViewById(R.id.editTextPostalCode);

        searchPostalButton.setOnClickListener(v -> {
            String postalCode = postalInput.getText().toString();
            if (postalCode.isEmpty()) {
                Toast.makeText(this, "郵便番号を入力してください", Toast.LENGTH_SHORT).show();
            } else {
                searchByPostalCode(postalCode);
            }
        });




        // 現在地を使用ボタン
        Button currentLocationButton = findViewById(R.id.buttonCurrentLocation);
        currentLocationButton.setOnClickListener(v -> {
            getCurrentLocation();
        });


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
        } else {
            //権限がない場合
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1000);
        }
    }

    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1000);
            return;
        }

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
                                statusText.setText("現在地を取得しました！");
                                statusText.setTextColor(0xFF4CAF50); // Green color
                            }
                        }
                    }
                });

    }


    private void searchByPostalCode(String postalCode) {
        TextView statusText = findViewById(R.id.textStatus);
        statusText.setText("郵便番号から検索中...");

        String url = "https://geoapi.heartrails.com/api/json?method=searchByPostal&postal=" + postalCode;
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity2.this, "通信エラー", Toast.LENGTH_SHORT).show();
                    statusText.setText("通信に失敗しました");
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity2.this, "検索に失敗しました", Toast.LENGTH_SHORT).show();
                        statusText.setText("検索に失敗しました");
                    });
                    return;
                }

                String body = response.body().string();
                JsonAdapter<HeartRailsResponse> adapter = moshi.adapter(HeartRailsResponse.class);
                try {
                    HeartRailsResponse hrResponse = adapter.fromJson(body);
                    if (hrResponse != null && hrResponse.response != null && hrResponse.response.location != null && !hrResponse.response.location.isEmpty()) {
                        HeartRailsResponse.Location loc = hrResponse.response.location.get(0);
                        
                        double lat = Double.parseDouble(loc.y);
                        double lon = Double.parseDouble(loc.x);
                        String locationName = loc.prefecture + loc.city + loc.town;

                        runOnUiThread(() -> {
                            latitude = lat;
                            longitude = lon;
                            statusText.setText("位置情報を設定しました:\n" + locationName);
                            statusText.setTextColor(0xFF4CAF50);
                            Toast.makeText(MainActivity2.this, "場所を設定しました", Toast.LENGTH_SHORT).show();
                        });

                    } else {
                        runOnUiThread(() -> {
                            statusText.setText("該当する場所が見つかりませんでした");
                            Toast.makeText(MainActivity2.this, "該当なし", Toast.LENGTH_SHORT).show();
                        });
                    }
                } catch (Exception e) {
                   runOnUiThread(() -> {
                       statusText.setText("データの解析に失敗しました");
                   });
                }
            }
        });
    }
}