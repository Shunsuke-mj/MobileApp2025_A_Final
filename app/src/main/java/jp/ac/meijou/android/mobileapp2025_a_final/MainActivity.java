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

public class MainActivity extends AppCompatActivity {

    // 東京駅周辺の緯度・経度を定数として設定
    private static String TOKYO_LATITUDE = "35.6895";
    private static String TOKYO_LONGITUDE = "139.6917";

    private String latitude = TOKYO_LATITUDE;
    private String longitude = TOKYO_LONGITUDE;

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

        // 情報取得ボタンの処理
        binding.buttonSearch.setOnClickListener(view -> {
            ;
            binding.textViewResult.setText("天気情報を取得中...");
            fetchWeatherData(latitude, longitude);
        });
    }

    /**
     * Open-Meteo APIを使用して天気情報を取得するメソッド
     *
     * @param latitude  緯度
     * @param longitude 経度
     */
    private void fetchWeatherData(String latitude, String longitude) {
        // APIエンドポイントのURLを構築（緯度経度から気温、湿度、降水確率、風速、体感温度を日本時間で1日ぶん取得）
        String url = "https://api.open-meteo.com/v1/forecast" +
                "?latitude=" + latitude +
                "&longitude=" + longitude +
                "&hourly=temperature_2m,relativehumidity_2m,precipitation_probability,windspeed_10m,apparent_temperature" +
                "&timezone=Asia%2FTokyo" +
                "&forecast_days=1";
        Request request = new Request.Builder().url(url).build();

        // 非同期でAPIリクエストを実行
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // 通信失敗時の処理
                runOnUiThread(() -> binding.textViewResult.setText("通信に失敗"));
            }

            // 通信成功時の処理
            @Override
            public void onResponse(Call call, Response response) {
                try (Response r = response) {
                    // レスポンスが正常でない場合の処理
                    if (!r.isSuccessful()) {
                        runOnUiThread(() -> binding.textViewResult.setText("正常に取得できず"));
                        return;
                    }


                    // レスポンスボディを文字列として取得し、Moshiでパース
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
     * 取得した天気情報を分析し、ベストな運動時間をTextViewに表示するメソッド
     *
     * @param meteoData 取得した天気情報
     */
    private void analyzeAndDisplayWeather(MeteoApiResponse meteoData) {
        // 天気情報の解析とメッセージ生成
        MeteoApiResponse.Hourly hourly = meteoData.hourly;

        // APIからデータが十分に取得できているか確認
        if (hourly == null || hourly.time == null || hourly.time.isEmpty()) {
            binding.textViewResult.setText("表示できる天気情報がありません。");
            binding.textViewResultTop3.setText(""); // Top3表示もクリア
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

        List<TimeScore> scoreList = new ArrayList<>();

        // 各時間のデータをチェックし、スコアをリストに追加
        for (int i = 0; i < hourly.time.size(); i++) {
            LocalDateTime forecastTime;
            try {
                // APIからの時刻文字列 ("YYYY-MM-DDTHH:MM") をパース
                forecastTime = LocalDateTime.parse(hourly.time.get(i));
            } catch (Exception e) {
                continue; // パースに失敗した場合はスキップ
            }

            // 現在時刻より後で、かつ今日中の予報のみを対象にする
            if (forecastTime.isAfter(now) && forecastTime.toLocalDate().isEqual(today)) {
                double temp = hourly.temperature_2m.get(i);
                double apparentTemp = hourly.apparent_temperature.get(i);
                int precip = hourly.precipitation_probability.get(i);
                double wind = hourly.windspeed_10m.get(i);
                int humidity = hourly.relativehumidity_2m.get(i);

                // 各要素に基づいてスコアを計算
                double currentScore = calculateExerciseScore(temp, apparentTemp, precip, wind, humidity);
                scoreList.add(new TimeScore(i, currentScore));
            }
        }

        // スコアの降順にソート
        Collections.sort(scoreList);

        // --- 結果の表示 ---

        // 1. トップ1の詳細情報を textViewResult に表示
        StringBuilder resultText = new StringBuilder();
        resultText.append("【今日の運動ベストタイム！】\n");
        if (!scoreList.isEmpty()) {
            int bestTimeIndex = scoreList.get(0).index;
            String time = hourly.time.get(bestTimeIndex).substring(11, 16);
            double temp = hourly.temperature_2m.get(bestTimeIndex);
            double apparentTemp = hourly.apparent_temperature.get(bestTimeIndex);
            int humidity = hourly.relativehumidity_2m.get(bestTimeIndex);
            int precip = hourly.precipitation_probability.get(bestTimeIndex);
            double wind = hourly.windspeed_10m.get(bestTimeIndex);

            resultText.append("時間: ").append(time).append("\n");
            resultText.append("気温: ").append(temp).append(" °C\n");
            resultText.append("体感温度: ").append(apparentTemp).append(" °C\n");
            resultText.append("湿度: ").append(humidity).append(" %\n");
            resultText.append("降水確率: ").append(precip).append(" %\n");
            resultText.append("風速: ").append(wind).append(" km/h\n");
            resultText.append("▶︎ ").append(generateWeatherMessage(temp, precip, apparentTemp)).append("\n");
        } else {
            resultText.append("本日、運動に適した時間帯は見つかりませんでした。\n");
        }
        binding.textViewResult.setText(resultText.toString());

        // 2. トップ3のランキングを textViewResultTop3 に表示
        StringBuilder top3Text = new StringBuilder();
        top3Text.append("【運動おすすめ時間 Top 3】\n");

        if (scoreList.isEmpty()) {
            top3Text.append("候補がありませんでした。\n");
        } else {
            // 表示する件数をリストのサイズと3の小さい方に合わせる
            int limit = Math.min(scoreList.size(), 3);
            for (int i = 0; i < limit; i++) {
                TimeScore timeScore = scoreList.get(i);
                String time = hourly.time.get(timeScore.index).substring(11, 16);
                double score = timeScore.score;

                // String.formatを使ってきれいに整形 (スコアは小数点以下1桁まで)
                top3Text.append(String.format(Locale.JAPAN, "%d位: %s (スコア: %.1f点)\n", i + 1, time, score));
            }
        }
        binding.textViewResultTop3.setText(top3Text.toString());
    }

    /**
     * テスト用
     */
    private void analyzeAndDisplayWeather1(MeteoApiResponse meteoData) {
        // 天気情報の解析とメッセージ生成
        MeteoApiResponse.Hourly hourly = meteoData.hourly;
        StringBuilder resultText = new StringBuilder();

        // APIからデータが十分に取得できているか確認
        if (hourly == null || hourly.time == null || hourly.time.isEmpty()) {
            binding.textViewResult.setText("表示できる天気情報がありません。");
            return;
        }

        // 今日の運動に最適な時間を見つける
        int bestTimeIndex = -1;
        double bestScore = -1.0;

        // 現在の日時を取得
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        // 各時間のデータをチェック
        for (int i = 0; i < hourly.time.size(); i++) {
            LocalDateTime forecastTime;
            try {
                // APIからの時刻文字列 ("YYYY-MM-DDTHH:MM") をパース
                forecastTime = LocalDateTime.parse(hourly.time.get(i));
            } catch (Exception e) {
                continue; // パースに失敗した場合はスキップ
            }

            // 現在時刻より後で、かつ今日中の予報のみを対象にする
            if (forecastTime.isAfter(now) && forecastTime.toLocalDate().isEqual(today)) {
                double temp = hourly.temperature_2m.get(i);
                double apparentTemp = hourly.apparent_temperature.get(i);
                int precip = hourly.precipitation_probability.get(i);
                double wind = hourly.windspeed_10m.get(i);
                int humidity = hourly.relativehumidity_2m.get(i);

                // 各要素に基づいてスコアを計算
                double currentScore = calculateExerciseScore(temp, apparentTemp, precip, wind, humidity);

                // これまでの最高スコアを更新したら、インデックスとスコアを保存
                if (currentScore > bestScore) {
                    bestScore = currentScore;
                    bestTimeIndex = i;
                }
            }
        }

        // 最適な時間が見つかった場合、その情報を表示
        resultText.append("【今日の運動ベストタイム！】\n");
        if (bestTimeIndex != -1) {
            String time = hourly.time.get(bestTimeIndex).substring(11, 16);
            double temp = hourly.temperature_2m.get(bestTimeIndex);
            double apparentTemp = hourly.apparent_temperature.get(bestTimeIndex);
            int humidity = hourly.relativehumidity_2m.get(bestTimeIndex);
            int precip = hourly.precipitation_probability.get(bestTimeIndex);
            double wind = hourly.windspeed_10m.get(bestTimeIndex);

            resultText.append("時間: ").append(time).append("\n");
            resultText.append("気温: ").append(temp).append(" °C\n");
            resultText.append("体感温度: ").append(apparentTemp).append(" °C\n");
            resultText.append("湿度: ").append(humidity).append(" %\n");
            resultText.append("降水確率: ").append(precip).append(" %\n");
            resultText.append("風速: ").append(wind).append(" km/h\n");
            resultText.append("▶︎ ").append(generateWeatherMessage(temp, precip, apparentTemp)).append("\n");
        } else {
            resultText.append("本日、運動に適した時間帯は見つかりませんでした。\n");
        }

        binding.textViewResult.setText(resultText.toString());
    }

    /**
     * 天気データに基づいて運動のしやすさをスコアリングするメソッド (新規追加)
     * @return 0から100のスコア
     */
    private double calculateExerciseScore(double temperature, double apparentTemperature,
                                          int precipitationProbability, double windspeed, int humidity) {
        double score = 100.0; // 満点（100点）から減点していく方式

        // 体感温度による減点
        if (apparentTemperature > 35) {
            score -= 100.0;
        } else if (apparentTemperature > 32) {
            score -= 70.0;
        } else if (apparentTemperature > 28) {
            score -= 30.0;
        }

        // 降水確率による減点
        if (precipitationProbability > 70) {
            score -= 80.0;
        } else if (precipitationProbability > 50) {
            score -= 50.0;
        } else if (precipitationProbability > 30) {
            score -= 20.0;
        }

        // 気温による減点 (快適な範囲から外れるほど減点)
        if (temperature < 5 || temperature > 30) {
            score -= 20.0;
        } else if (temperature < 10) {
            score -= 10.0;
        }

        // 風速による減点 (km/h)
        if (windspeed > 30) {
            score -= 30.0;
        } else if (windspeed > 20) {
            score -= 15.0;
        }

        // 湿度による減点
        if (humidity > 85) {
            score -= 15.0;
        }

        // スコアが0未満にならないようにする
        return Math.max(0, score);
    }

    /**
     * テスト用
     */
    private void analyzeAndDisplayWeather_test(MeteoApiResponse meteoData) {
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
     * 現在使用されていない
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