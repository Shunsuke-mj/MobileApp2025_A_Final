package jp.ac.meijou.android.mobileapp2025_a_final;

import com.squareup.moshi.Json;
import java.util.List;

/**
 * Open-Meteo APIからのレスポンスを格納するクラス
 * 天気予報データ（時系列）を扱います。
 */
public class MeteoApiResponse {
    public Hourly hourly;

    /**
     * 1時間ごとの予報データを保持するクラス
     */
    public static class Hourly {
        public List<String> time; // 時刻 (ISO 8601形式)

        @Json(name = "temperature_2m")
        public List<Double> temperature_2m; // 気温 (℃)

        @Json(name = "relativehumidity_2m")
        public List<Integer> relativehumidity_2m; // 相対湿度 (%)

        @Json(name = "precipitation_probability")
        public List<Integer> precipitation_probability; // 降水確率 (%)

        @Json(name = "windspeed_10m")
        public List<Double> windspeed_10m; // 風速 (km/h)

        @Json(name = "apparent_temperature")
        public List<Double> apparent_temperature; // 体感温度 (℃)

        // 将来的に他の気象データ（気圧、雲量など）が必要になった場合はここにフィールドを追加します
    }
}