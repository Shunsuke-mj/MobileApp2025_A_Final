package jp.ac.meijou.android.mobileapp2025_a_final;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class HourlyForecastAdapter extends RecyclerView.Adapter<HourlyForecastAdapter.ViewHolder> {

    private List<HourlyItem> hourlyItems = new ArrayList<>();

    public static class HourlyItem {
        public String time;
        public double temperature;
        public int precipitationProbability;
        public int humidity;
        public double windSpeed;

        public HourlyItem(String time, double temperature, int precipitationProbability, int humidity, double windSpeed) {
            this.time = time;
            this.temperature = temperature;
            this.precipitationProbability = precipitationProbability;
            this.humidity = humidity;
            this.windSpeed = windSpeed;
        }
    }

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
        holder.textTemp.setText(String.format("%.1f℃", item.temperature));
        holder.textPrecip.setText(String.format("%d%%", item.precipitationProbability));
        holder.textHumidity.setText(String.format("%d%%", item.humidity));
        holder.textWind.setText(String.format("%.1fkm/h", item.windSpeed));
    }

    @Override
    public int getItemCount() {
        return hourlyItems.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textTime;
        TextView textTemp;
        TextView textPrecip;
        TextView textHumidity;
        TextView textWind;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textTime = itemView.findViewById(R.id.textTime);
            textTemp = itemView.findViewById(R.id.textTemp);
            textPrecip = itemView.findViewById(R.id.textPrecip);
            textHumidity = itemView.findViewById(R.id.textHumidity);
            textWind = itemView.findViewById(R.id.textWind);
        }
    }
}
