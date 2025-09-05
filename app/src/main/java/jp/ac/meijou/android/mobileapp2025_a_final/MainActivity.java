package jp.ac.meijou.android.mobileapp2025_a_final;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.io.IOException;

import jp.ac.meijou.android.mobileapp2025_a_final.databinding.ActivityMainBinding;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    // 東京駅周辺の緯度・経度を定数として設定
    private static final String TOKYO_LATITUDE = "35.6895";
    private static final String TOKYO_LONGITUDE = "139.6917";

    // api通信の準備
    private final OkHttpClient client = new OkHttpClient();

    private final Moshi moshi = new Moshi.Builder().build();

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 情報取得ボタンの処理
        binding.buttonSearch.setOnClickListener(view -> {
            ;
            binding.textViewResult.setText("天気情報を取得中...");
            fetchWeatherData(TOKYO_LATITUDE, TOKYO_LONGITUDE);
        });
    }

    /**
     * Open-Meteo APIを使用して天気情報を取得するメソッド
     *
     * @param latitude  緯度
     * @param longitude 経度
     */
    private void fetchWeatherData(String latitude, String longitude) {
        // APIエンドポイントのURLを構築（地上2mの情報を取得）
        String url = "https://api.open-meteo.com/v1/forecast" +
                "?latitude=" + latitude +
                "&longitude=" + longitude +
                "&hourly=temperature_2m,relativehumidity_2m,precipitation_probability,windspeed_10m,apparent_temperature,uv_index,shortwave_radiation" +
                "&timezone=Asia%2FTokyo";
        Request request = new Request.Builder().url(url).build();

        // 非同期でAPIリクエストを実行
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // 通信失敗時の処理
                runOnUiThread(() -> binding.textViewResult.setText("通信に失敗"));
            }

            @Override
            public void onResponse(Call call, Response response) {
                try (Response r = response) {
                    if (!r.isSuccessful()) {
                        runOnUiThread(() -> binding.textViewResult.setText("正常に取得できず"));
                        return;
                    }


                    String responseBody = r.body().string();
                    JsonAdapter<MeteoApiResponse> jsonAdapter = moshi.adapter(MeteoApiResponse.class);
                    MeteoApiResponse meteoData = jsonAdapter.fromJson(responseBody);

                    if (meteoData != null) {
                        // UIスレッドで結果を分析・表示
                        runOnUiThread(() -> analyzeAndDisplayWeather(meteoData));
                    }

                } catch (IOException e) {
                    // JSONのパース失敗や通信のその他エラー
                    runOnUiThread(() -> binding.textViewResult.setText("データの解析に失敗しました"));
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 取得した天気情報を分析し、適切なメッセージをTextViewに表示するメソッド
     *
     * @param meteoData 取得した天気情報
     */
    private void analyzeAndDisplayWeather(MeteoApiResponse meteoData) {
        // 天気情報の解析とメッセージ生成
        MeteoApiResponse.Hourly hourly = meteoData.hourly;
        StringBuilder resultText = new StringBuilder();

        // 1時間後の予報とメッセージ
        double temp1 = hourly.temperature_2m.get(1);
        int humidity1 = hourly.relativehumidity_2m.get(1);
        int precip1 = hourly.precipitation_probability.get(1);
        double wind1 = hourly.windspeed_10m.get(1);
        double apparentTemp1 = hourly.apparent_temperature.get(1);

        resultText.append("【1時間後の予報】\n");
        resultText.append("時間: ").append(hourly.time.get(1).substring(11, 16)).append("\n");
        resultText.append("気温: ").append(temp1).append(" °C\n");
        resultText.append("体感温度: ").append(apparentTemp1).append(" °C\n");
        resultText.append("湿度: ").append(humidity1).append(" %\n");
        resultText.append("降水確率: ").append(precip1).append(" %\n");
        resultText.append("風速: ").append(wind1).append(" km/h\n");
        resultText.append("▶︎ ").append(generateWeatherMessage(temp1, precip1, apparentTemp1)).append("\n\n");

        // 2時間後の予報とメッセージ
        double temp2 = hourly.temperature_2m.get(2);
        int humidity2 = hourly.relativehumidity_2m.get(2);
        int precip2 = hourly.precipitation_probability.get(2);
        double wind2 = hourly.windspeed_10m.get(2);
        double apparentTemp2 = hourly.apparent_temperature.get(2);

        resultText.append("【2時間後の予報】\n");
        resultText.append("時間: ").append(hourly.time.get(2).substring(11, 16)).append("\n");
        resultText.append("気温: ").append(temp2).append(" °C\n");
        resultText.append("体感温度: ").append(apparentTemp2).append(" °C\n");
        resultText.append("湿度: ").append(humidity2).append(" %\n");
        resultText.append("降水確率: ").append(precip2).append(" %\n");
        resultText.append("風速: ").append(wind2).append(" km/h\n");
        resultText.append("▶︎ ").append(generateWeatherMessage(temp2, precip2, apparentTemp2)).append("\n");

        binding.textViewResult.setText(resultText.toString());
    }

    /**
     * 天気データに基づいてアドバイスメッセージを生成するメソッド
     *
     * @return アドバイスメッセージの文字列
     */
    private String generateWeatherMessage(double temperature, int precipitationProbability,
                                          double apparentTemperature) {
        // 危険度が高いものから順に判定する

        // 熱中症・暑さに関する警告
        if (apparentTemperature > 32) {
            return "熱中症の危険性が非常に高いです。屋外での運動は避けましょう。";
        }

        // 降水に関する警告
        if (precipitationProbability > 50) {
            return "雨の可能性が高いです。室内でのトレーニングがおすすめです。";
        }



        // その他の一般的なアドバイス
        if (temperature > 30) {
            return "かなり暑くなります。こまめな水分補給を心がけてください。";
        }
        if (temperature < 10) {
            return "肌寒いです。運動の前後で体が冷えないように上着を用意しましょう。";
        }

        return "運動に適した気候です。良い汗を流しましょう！";
    }
}