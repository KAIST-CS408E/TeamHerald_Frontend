package junpu.junpu.ViewHolders;

import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bignerdranch.expandablerecyclerview.ViewHolder.ChildViewHolder;

import junpu.junpu.R;

public class MyChildViewHolder extends com.bignerdranch.expandablerecyclerview.ViewHolder.ChildViewHolder {

    public TextView points, distance, duration;

    public RelativeLayout safe_speed, safe_weather, safe_phone, safe_lane, safe_intersection;

    public MyChildViewHolder(View itemView) {
        super(itemView);

        //text views
        points = itemView.findViewById(R.id.points);
        distance = itemView.findViewById(R.id.distance);
        duration = itemView.findViewById(R.id.duration);

        //relative layouts
        safe_speed = itemView.findViewById(R.id.safe_speed);
        safe_weather = itemView.findViewById(R.id.safe_weather);
        safe_phone = itemView.findViewById(R.id.safe_phone);
        safe_lane = itemView.findViewById(R.id.safe_lane);
        safe_intersection = itemView.findViewById(R.id.safe_intersection);
    }
}

