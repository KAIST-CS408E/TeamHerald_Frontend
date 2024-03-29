package junpu.junpu.Models;

import com.bignerdranch.expandablerecyclerview.Model.ParentObject;

import java.util.List;
import java.util.UUID;

public class Parent implements ParentObject {

    private List<Object> mChildrenList;
    private UUID _id;

    private String sessionDate;

    public Parent(String sessionDate){
        String[] date = sessionDate.split("T");
        String[] time = date[1].split(".000Z");

        this.sessionDate = date[0] + "    " + time[0];
        _id = UUID.randomUUID();
    }

    public UUID get_id() {
        return _id;
    }

    public void set_id(UUID _id) {
        this._id = _id;
    }

    public String getTitle() {
        return sessionDate;
    }

    public void setTitle(String title) {
        this.sessionDate = title;
    }

    @Override
    public List<Object> getChildObjectList() {
        return mChildrenList;
    }

    @Override
    public void setChildObjectList(List<Object> list) {
        mChildrenList = list;
    }

}
