package junpu.junpu.ViewHolders;

import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.bignerdranch.expandablerecyclerview.ViewHolder.ParentViewHolder;

import junpu.junpu.R;


public class MyParentViewHolder extends ParentViewHolder {

    public TextView _textView;
    public ImageButton _imageButton;

    public MyParentViewHolder(View itemView) {
        super(itemView);

        _textView = itemView.findViewById(R.id.sessionDate);
//        _imageButton = itemView.findViewById(R.id.expandArrow);
    }
}
