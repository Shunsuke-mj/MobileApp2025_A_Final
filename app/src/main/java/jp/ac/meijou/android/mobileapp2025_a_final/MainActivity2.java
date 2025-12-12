package jp.ac.meijou.android.mobileapp2025_a_final;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 位置情報設定画面用のアクティビティ
 * 郵便番号検索やGPSによる現在地取得機能を提供します。
 */
public class MainActivity2 extends AppCompatActivity {

    private FusedLocationProviderClient fusedLocationClient;
    private double latitude = 0;    // 取得した緯度
    private double longitude = 0;   // 取得した経度
    
    // API通信用クライアント
    private final OkHttpClient client = new OkHttpClient();
    // JSONパース用インスタンス
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

        // 「決定」ボタンの設定
        // 現在設定されている位置情報（latitude, longitude）を持ってメイン画面へ遷移します
        Button sendButton = findViewById(R.id.button);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 画面1（MainActivity）へのIntentを作成
                Intent intent = new Intent(MainActivity2.this, MainActivity.class);

                // 緯度と経度をIntentに格納
                intent.putExtra("EXTRA_LATITUDE", latitude);
                intent.putExtra("EXTRA_LONGITUDE", longitude);

                // 画面1を起動
                startActivity(intent);
            }
        });

        // 「郵便番号で検索」ボタンの設定
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




        // 「現在地を使用」ボタンの設定
        Button currentLocationButton = findViewById(R.id.buttonCurrentLocation);
        currentLocationButton.setOnClickListener(v -> {
            getCurrentLocation();
        });

        // アプリ起動時に位置情報の権限を確認し、許可されていれば現在地を取得します
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
        } else {
            // 権限がない場合はリクエストを行う（ユーザーに許可を求める）
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1000);
        }
    }

    /**
     * 現在地を取得するメソッド
     * 権限があれば FusedLocationProviderClient を使用して最終測位位置を取得します。
     * 権限がない場合は、権限リクエストを行います。
     */
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
                        // 最後に取得した位置情報。稀にnullになることがあります。
                        if (location != null) {
                            // 位置情報の取得成功
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();
                            
                            // ステータステキストの更新
                            TextView statusText = findViewById(R.id.textStatus);
                            if (statusText != null) {
                                statusText.setText("現在地を取得しました！");
                                statusText.setTextColor(0xFF4CAF50); // 緑色
                            }
                        }
                    }
                });

    }


    /**
     * 郵便番号を使用して HeartRails Express API から位置情報を検索するメソッド
     * 
     * @param postalCode 検索する郵便番号 (ハイフンなし7桁推奨)
     */
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
                    // JSONレスポンスをパースしてオブジェクトに変換
                    HeartRailsResponse hrResponse = adapter.fromJson(body);
                    
                    // レスポンスが有効かつ、位置情報が含まれているか確認
                    if (hrResponse != null && hrResponse.response != null && hrResponse.response.location != null && !hrResponse.response.location.isEmpty()) {
                        // 最初の候補地を取得（通常、郵便番号検索では1つまたは少数が返る）
                        HeartRailsResponse.Location loc = hrResponse.response.location.get(0);
                        
                        // 座標文字列をdouble型に変換
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