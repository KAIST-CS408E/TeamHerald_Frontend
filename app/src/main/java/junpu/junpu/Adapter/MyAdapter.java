package junpu.junpu.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.bignerdranch.expandablerecyclerview.Adapter.ExpandableRecyclerAdapter;
import com.bignerdranch.expandablerecyclerview.Model.ParentObject;

import java.util.List;

import junpu.junpu.Models.Child;
import junpu.junpu.Models.Parent;
import junpu.junpu.R;
import junpu.junpu.ViewHolders.MyChildViewHolder;
import junpu.junpu.ViewHolders.MyParentViewHolder;


public class MyAdapter extends ExpandableRecyclerAdapter<MyParentViewHolder, MyChildViewHolder> {

    LayoutInflater inflater;

    public MyAdapter(Context context, List<ParentObject> parentItemList) {
        super(context, parentItemList);
        inflater = LayoutInflater.from(context);
    }

    @Override
    public MyParentViewHolder onCreateParentViewHolder(ViewGroup viewGroup) {
        View view = inflater.inflate(R.layout.list_parent,viewGroup,false);
        return new MyParentViewHolder(view);
    }

    @Override
    public MyChildViewHolder onCreateChildViewHolder(ViewGroup viewGroup) {
        View view = inflater.inflate(R.layout.list_child,viewGroup,false);
        return new MyChildViewHolder(view);
    }

    @Override
    public void onBindParentViewHolder(MyParentViewHolder titleParentViewHolder, int i, Object o) {
        Parent title = (Parent) o;
        titleParentViewHolder._textView.setText(title.getTitle());
    }

    @Override
    public void onBindChildViewHolder(MyChildViewHolder titleChildViewHolder, int i, Object o) {
        Child title = (Child) o;

        //general statistics
        titleChildViewHolder.points.setText(title.getPoints());
        titleChildViewHolder.distance.setText(title.getDistance());
        titleChildViewHolder.duration.setText(title.getDuration());

        //violations
        setCheckOrCross(titleChildViewHolder.safe_speed, title.isSpeedingCheck(),R.id.speed_check, R.id.speed_cross);
        setCheckOrCross(titleChildViewHolder.safe_weather, title.isWeatherCheck(), R.id.weather_check, R.id.weather_cross);
        setCheckOrCross(titleChildViewHolder.safe_phone, title.isPhoneCheck(),R.id.phone_check, R.id.phone_cross);
        setCheckOrCross(titleChildViewHolder.safe_lane, title.isLaneCheck(), R.id.lane_check, R.id.lane_cross);
        setCheckOrCross(titleChildViewHolder.safe_intersection, title.isIntersectionCheck(),R.id.intersection_check, R.id.intersection_cross);
    }

    private void setCheckOrCross(RelativeLayout rl, Boolean violated, int check, int cross){
        if(violated){
            ImageView image = rl.findViewById(cross);
            image.setVisibility(View.VISIBLE);
            ImageView image2 = rl.findViewById(check);
            image2.setVisibility(View.INVISIBLE);

        }else{
            ImageView image = rl.findViewById(check);
            image.setVisibility(View.VISIBLE);
            ImageView image2 = rl.findViewById(cross);
            image2.setVisibility(View.INVISIBLE);
        }

    }
}
