package junpu.junpu.Models;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

//helper class to create list Parent
public class TitleCreator {

    static TitleCreator _titleCreator;
    List<Parent> _titleParents;

    public TitleCreator(Context context) {
        _titleParents = new ArrayList<>();
        for(int i = 1; i < 100; i++){
            Parent title = new Parent(String.format("Session #%d:",i));
            _titleParents.add(title);
        }
    }

    public static TitleCreator get(Context context){
        if(_titleCreator == null){
            _titleCreator = new TitleCreator(context);
        }
        return _titleCreator;
    }

    public List<Parent> getAll() {
        return _titleParents;
    }
}
