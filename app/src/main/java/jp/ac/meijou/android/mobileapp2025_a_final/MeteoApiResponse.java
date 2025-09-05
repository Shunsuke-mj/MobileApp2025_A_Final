package jp.ac.meijou.android.mobileapp2025_a_final;

import com.squareup.moshi.Json;
import java.util.List;

public class MeteoApiResponse {
    public Hourly hourly;

    public static class Hourly {
        public List<String> time;

        @Json(name = "temperature_2m")
        public List<Double> temperature_2m;

        @Json(name = "relativehumidity_2m")
        public List<Integer> relativehumidity_2m;

        @Json(name = "precipitation_probability")
        public List<Integer> precipitation_probability;

        @Json(name = "windspeed_10m")
        public List<Double> windspeed_10m;

        @Json(name = "apparent_temperature")
        public List<Double> apparent_temperature;

        // MainActivityで利用している他のフィールドもここに追加
    }
}