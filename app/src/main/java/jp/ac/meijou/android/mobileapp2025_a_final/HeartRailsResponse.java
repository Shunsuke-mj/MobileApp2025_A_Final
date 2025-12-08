package jp.ac.meijou.android.mobileapp2025_a_final;

import java.util.List;

public class HeartRailsResponse {
    public Response response;

    public static class Response {
        public List<Location> location;
    }

    public static class Location {
        public String city;
        public String city_kana;
        public String town;
        public String town_kana;
        public String x; // Longitude
        public String y; // Latitude
        public String prefecture;
        public String postal;
    }
}
