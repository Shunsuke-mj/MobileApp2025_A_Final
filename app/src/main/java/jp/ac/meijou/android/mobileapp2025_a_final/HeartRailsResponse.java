package jp.ac.meijou.android.mobileapp2025_a_final;

import java.util.List;

/**
 * HeartRails Express APIからのレスポンスを格納するクラス
 * 郵便番号検索の結果として返される地域情報を扱います。
 */
public class HeartRailsResponse {
    public Response response;

    public static class Response {
        public List<Location> location;
    }

    /**
     * 地域情報の詳細を保持するクラス
     */
    public static class Location {
        public String city;       // 市区町村
        public String city_kana;  // 市区町村（カナ）
        public String town;       // 町域
        public String town_kana;  // 町域（カナ）
        public String x;          // 経度
        public String y;          // 緯度
        public String prefecture; // 都道府県
        public String postal;     // 郵便番号
    }
}
