package jp.ac.meijou.android.mobileapp2025_a_final;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import jp.ac.meijou.android.mobileapp2025_a_final.databinding.ActivityMainBinding;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * アプリのメイン画面用のアクティビティ
 * 天気予報の表示や、運動適性スコアの計算と表示を行います。
 */
public class MainActivity extends AppCompatActivity {

    // 東京駅周辺の緯度・経度を定数として設定 (デフォルト位置)
    private static String TOKYO_LATITUDE = "35.6895";
    private static String TOKYO_LONGITUDE = "139.6917";

    // 現在設定されている位置情報
    private String latitude = TOKYO_LATITUDE;
    private String longitude = TOKYO_LONGITUDE;

    // API通信用クライアント
    private final OkHttpClient client = new OkHttpClient();

    // JSONパース用インスタンス
    private final Moshi moshi = new Moshi.Builder().build();

    private ActivityMainBinding binding;
    private HourlyForecastAdapter hourlyForecastAdapter;

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

        // RecyclerView（天気予報リスト）の初期化と設定
        hourlyForecastAdapter = new HourlyForecastAdapter();
        binding.recyclerViewForecast.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewForecast.setAdapter(hourlyForecastAdapter);

        // 起動時のパラメータ処理（MainActivity2から渡された位置情報の取得）
        Intent intent = getIntent();
        // Intentに "EXTRA_LATITUDE" と "EXTRA_LONGITUDE" のキーが存在するかチェック
        if (intent.hasExtra("EXTRA_LATITUDE") && intent.hasExtra("EXTRA_LONGITUDE")) {
            // double型で値を取得し、Stringに変換して変数に格納する
            // もし何らかの理由で値が取得できなかった場合は、デフォルト値(東京)が使われる
            double lat = intent.getDoubleExtra("EXTRA_LATITUDE", Double.parseDouble(TOKYO_LATITUDE));
            double lon = intent.getDoubleExtra("EXTRA_LONGITUDE", Double.parseDouble(TOKYO_LONGITUDE));
            this.latitude = String.valueOf(lat);
            this.longitude = String.valueOf(lon);
        }

        // 「天気予報を取得」ボタンのクリックリスナー設定
        binding.buttonSearch.setOnClickListener(view -> {
            binding.textViewResult.setText("天気情報を取得中...");
            fetchWeatherData(latitude, longitude);
        });

        // 「場所を変更」ボタンのクリックリスナー設定
        Button changeLocationButton = findViewById(R.id.buttonChangeLocation);
        changeLocationButton.setOnClickListener(view -> {
            // MainActivity2に戻る
            finish();
        });
    }


    /**
     * Open-Meteo APIを使用して天気情報を取得するメソッド
     * 指定された緯度・経度に基づいて、現在から2日間の時間ごとの天気予報データを取得します。
     *
     * @param latitude  緯度 (例: "35.6895")
     * @param longitude 経度 (例: "139.6917")
     */
    private void fetchWeatherData(String latitude, String longitude) {
        // APIエンドポイントのURLを構築（緯度経度から気温、湿度、降水確率、風速、体感温度を日本時間で2日ぶん取得）
        String url = "https://api.open-meteo.com/v1/forecast" +
                "?latitude=" + latitude +
                "&longitude=" + longitude +
                "&hourly=temperature_2m,relativehumidity_2m,precipitation_probability,windspeed_10m,apparent_temperature" +
                "&timezone=Asia%2FTokyo" +
                "&forecast_days=2";
        Request request = new Request.Builder().url(url).build();

        // 非同期でAPIリクエストを実行
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // 通信失敗時の処理：UIスレッドでエラーメッセージを表示
                runOnUiThread(() -> binding.textViewResult.setText("通信に失敗"));
            }

            // 通信成功時の処理
            @Override
            public void onResponse(Call call, Response response) {
                try (Response r = response) {
                    // レスポンスコードを確認 (200 OK以外はエラー扱い)
                    if (!r.isSuccessful()) {
                        runOnUiThread(() -> binding.textViewResult.setText("正常に取得できず"));
                        return;
                    }


                    // レスポンスボディを文字列として取得し、MoshiでJavaオブジェクトに変換
                    String responseBody = r.body().string();
                    JsonAdapter<MeteoApiResponse> jsonAdapter = moshi.adapter(MeteoApiResponse.class);
                    MeteoApiResponse meteoData = jsonAdapter.fromJson(responseBody);

                    if (meteoData != null) {
                        // データ取得成功：UIスレッドで結果を分析・表示
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
     * 取得した天気情報を分析し、運動に適した時間をスコアリングして表示するメソッド
     * 
     * 1. 取得データを解析して HourlyItem リストを作成
     * 2. RecyclerViewのアダプターにデータをセット
     * 3. スコアが高い順におすすめの時間帯トップ3を表示
     *
     * @param meteoData 取得した天気情報レスポンス
     */
    private void analyzeAndDisplayWeather(MeteoApiResponse meteoData) {
        // 天気情報の解析とメッセージ生成
        MeteoApiResponse.Hourly hourly = meteoData.hourly;

        // APIからデータが十分に取得できているか確認
        if (hourly == null || hourly.time == null || hourly.time.isEmpty()) {
            binding.textViewResult.setText("表示できる天気情報がありません。");
            return;
        }

        // スコアと時間データのインデックスを保持するためのインナークラス
        class TimeScore implements Comparable<TimeScore> {
            final int index;
            final double score;

            TimeScore(int index, double score) {
                this.index = index;
                this.score = score;
            }

            @Override
            public int compareTo(TimeScore other) {
                // スコアの降順でソートするための比較
                return Double.compare(other.score, this.score);
            }
        }

        // 現在の日時を取得
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        LocalDate tomorrow = today.plusDays(1);

        List<TimeScore> scoreList = new ArrayList<>();
        List<HourlyForecastAdapter.HourlyItem> hourlyItems = new ArrayList<>();

        // 各時間のデータをチェックし、スコアをリストに追加
        for (int i = 0; i < hourly.time.size(); i++) {
            LocalDateTime forecastTime;
            try {
                // APIからの時刻文字列 ("YYYY-MM-DDTHH:MM") をパース
                forecastTime = LocalDateTime.parse(hourly.time.get(i));
            } catch (Exception e) {
                continue; // パースに失敗した場合はスキップ
            }

            // 今日または明日のデータをリスト用に追加
             if (forecastTime.toLocalDate().isEqual(today) || forecastTime.toLocalDate().isEqual(tomorrow)) {
                double temp = hourly.temperature_2m.get(i);
                double apparentTemp = hourly.apparent_temperature.get(i);
                int precip = hourly.precipitation_probability.get(i);
                double wind = hourly.windspeed_10m.get(i);
                int humidity = hourly.relativehumidity_2m.get(i);
                
                // 日付を含めた表示形式 (例: 12/09 10:00)
                String timeStr = String.format(Locale.JAPAN, "%02d/%02d %02d:%02d",
                        forecastTime.getMonthValue(),
                        forecastTime.getDayOfMonth(),
                        forecastTime.getHour(),
                        forecastTime.getMinute());
                
                // 全ての時間のスコアを計算
                double itemScore = calculateExerciseScore(temp, apparentTemp, precip, wind, humidity);

                hourlyItems.add(new HourlyForecastAdapter.HourlyItem(
                        timeStr, 
                        temp, 
                        precip, 
                        humidity, 
                        wind,
                        itemScore // スコアをコンストラクタに渡す
                ));

                // 現在時刻より後のみを対象にランキング用スコアリストに追加
                 if (forecastTime.isAfter(now)) {
                    scoreList.add(new TimeScore(i, itemScore));
                 }
             }
        }
        
        // アダプターにデータをセット
        hourlyForecastAdapter.setHourlyItems(hourlyItems);

        // スコアの降順にソート
        Collections.sort(scoreList);

        // --- 結果の表示 ---

        // 1. トップ3の詳細情報を textViewResult に表示
        StringBuilder resultText = new StringBuilder();
        resultText.append("【おすすめの運動時間 Top 3】\n");
        
        if (scoreList.isEmpty()) {
            resultText.append("この期間、運動に適した時間帯は見つかりませんでした。\n");
        } else {
            // 表示する件数をリストのサイズと3の小さい方に合わせる
            int limit = Math.min(scoreList.size(), 3);
            for (int i = 0; i < limit; i++) {
                TimeScore timeScore = scoreList.get(i);
                int idx = timeScore.index;
                
                LocalDateTime timeObj = LocalDateTime.parse(hourly.time.get(idx));
                String timeDisplay = String.format(Locale.JAPAN, "%02d/%02d %02d:%02d",
                        timeObj.getMonthValue(),
                        timeObj.getDayOfMonth(),
                        timeObj.getHour(),
                        timeObj.getMinute());
                        
                double temp = hourly.temperature_2m.get(idx);
                double score = timeScore.score;

                // 1位の場合は少し詳細を表示、2,3位はシンプルに
                resultText.append(String.format(Locale.JAPAN, "No.%d: %s (%.0f点)\n", i + 1, timeDisplay, score));
                if (i == 0) {
                     resultText.append("  ").append(generateWeatherMessage(temp, hourly.precipitation_probability.get(idx), hourly.apparent_temperature.get(idx))).append("\n");
                }
            }
        }
        binding.textViewResult.setText(resultText.toString());
    }


    /**
     * 天気データに基づいて運動のしやすさをスコアリングするメソッド
     * 気温、体感温度、降水確率、風速から算出します。
     * 
     * ■ 採点基準 (100点満点からの減点方式)
     * - 降水確率: 雨は大敵。確率が高いほど大きく減点。
     * - 気温/体感温度: 暑すぎる(熱中症リスク)、寒すぎる場合は減点。
     * - 湿度/不快指数: 気温が高く多湿な場合は減点。
     * - 風速: 強風は減点。
     *
     * @param temperature 気温 (℃)
     * @param apparentTemperature 体感温度 (℃)
     * @param precipitationProbability 降水確率 (%)
     * @param windspeed 風速 (km/h)
     * @param humidity 湿度 (%)
     * @return 0から100のスコア
     */
    private double calculateExerciseScore(double temperature, double apparentTemperature,
                                          int precipitationProbability, double windspeed, int humidity) {
        
        double score = 100.0;

        // 1. 降水確率 (最優先)
        // 運動に雨は大敵。30%を超えると徐々に減点、60%以上は大きく減点
        if (precipitationProbability >= 80) {
            score -= 80;
        } else if (precipitationProbability >= 50) {
            score -= 50;
        } else if (precipitationProbability >= 30) {
            score -= 20;
        } // 0-20%は減点なし

        // 2. 気温・体感温度 (重要)
        // 運動に最適なのは 15℃〜25℃ 程度とする
        // 暑さ対策: 熱中症リスク
        if (apparentTemperature > 35) {
            score -= 100; // 運動危険
        } else if (apparentTemperature > 31) {
            score -= 60;  // 厳重警戒
        } else if (apparentTemperature > 28) {
            score -= 30;  // 警戒
        }
        
        // 寒さ対策
        if (temperature < 0) {
            score -= 40;
        } else if (temperature < 5) {
            score -= 20;
        } else if (temperature < 10) {
            score -= 10;
        }

        // 3. 不快指数 (湿度 + 気温) 簡易判定
        // 気温が高く湿度も高い場合はさらに減点
        if (temperature > 25 && humidity > 80) {
            score -= 15;
        } else if (temperature > 25 && humidity > 60) {
            score -= 10;
        }

        // 4. 風 (強風は不適)
        if (windspeed > 25) { // 約90km/hとかではないので、25km/h (約7m/s) 程度を基準に
             score -= 30;
        } else if (windspeed > 15) {
             score -= 10;
        }

        // スコアの範囲を0〜100に収める
        return Math.max(0, Math.min(100, score));
    }

    /**
     * 天気条件に基づいて、ユーザーへのワンポイントアドバイスメッセージを生成するメソッド
     * おすすめ時間の詳細表示に使用されます。
     *
     * @param temperature 気温
     * @param precipitationProbability 降水確率
     * @param apparentTemperature 体感温度
     * @return アドバイスメッセージ
     */
    private String generateWeatherMessage(double temperature, int precipitationProbability,
                                          double apparentTemperature) {
        // 危険度が高いものから順に判定する

        // 熱中症・暑さに関する警告
        if (apparentTemperature > 31) {
            return "危険な暑さです。屋外運動は控えましょう。";
        }
        
        // 降水に関する警告
        if (precipitationProbability > 50) {
            return "雨が降る可能性があります。";
        }

        // 快適な場合
        if (temperature >= 15 && temperature <= 25 && precipitationProbability < 30) {
            return "運動に最適なコンディションです！";
        }

        // その他の一般的なアドバイス
        if (temperature > 25) {
            return "少し暑いです。水分補給を忘れずに。";
        }
        if (temperature < 10) {
            return "肌寒いです。体を冷やさないように。";
        }

        return "良い運動日和になりますように。";
    }
}