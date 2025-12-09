package jp.ac.meijou.android.mobileapp2025_a_final;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * 1時間ごとの天気予報リストを表示するためのアダプタークラス
 * RecyclerViewを使用して、各時間の天気情報と運動適性スコアを表示します。
 */
public class HourlyForecastAdapter extends RecyclerView.Adapter<HourlyForecastAdapter.ViewHolder> {

    private List<HourlyItem> hourlyItems = new ArrayList<>();

    /**
     * リストの各行に表示するデータモデル
     */
    public static class HourlyItem {
        public String time;                     // 表示時刻
        public double temperature;              // 気温 (℃)
        public int precipitationProbability;    // 降水確率 (%)
        public int humidity;                    // 湿度 (%)
        public double windSpeed;                // 風速 (km/h)
        public double score;                    // 運動適性スコア (0-100)

        public HourlyItem(String time, double temperature, int precipitationProbability, int humidity, double windSpeed, double score) {
            this.time = time;
            this.temperature = temperature;
            this.precipitationProbability = precipitationProbability;
            this.humidity = humidity;
            this.windSpeed = windSpeed;
            this.score = score;
        }
    }

    /**
     * 表示するデータリストを更新します
     * @param items 新しいデータのリスト
     */
    public void setHourlyItems(List<HourlyItem> items) {
        this.hourlyItems = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_hourly_forecast, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HourlyItem item = hourlyItems.get(position);
        
        holder.textTime.setText(item.time);
        holder.textScore.setText(String.format("%.0f点", item.score)); 
        holder.textTemp.setText(String.format("%.1f℃", item.temperature));
        holder.textPrecip.setText(String.format("%d%%", item.precipitationProbability));
        holder.textHumidity.setText(String.format("%d%%", item.humidity));
        holder.textWind.setText(String.format("%.1fkm/h", item.windSpeed));
    }

    @Override
    public int getItemCount() {
        return hourlyItems.size();
    }

    /**
     * リスト項目のビューを保持するホルダー
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textTime;
        TextView textScore; 
        TextView textTemp;
        TextView textPrecip;
        TextView textHumidity;
        TextView textWind;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textTime = itemView.findViewById(R.id.textTime);
            textScore = itemView.findViewById(R.id.textScore);
            textTemp = itemView.findViewById(R.id.textTemp);
            textPrecip = itemView.findViewById(R.id.textPrecip);
            textHumidity = itemView.findViewById(R.id.textHumidity);
            textWind = itemView.findViewById(R.id.textWind);
        }
    }
}
