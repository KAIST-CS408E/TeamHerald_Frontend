package junpu.junpu.Models;

public class Child {

    //general stats
    private String points;
    private String distance;
    private String duration;

    //violations
    private boolean speedingCheck;
    private boolean weatherCheck;
    private boolean phoneCheck;
    private boolean laneCheck;
    private boolean intersectionCheck;

    public Child(String points, /*String distance, String duration,*/
                 boolean speedingCheck, boolean weatherCheck, boolean phoneCheck, boolean laneCheck, boolean intersectionCheck){


        this.points = "Points: " + points;
        /*
        this.distance = "Distance: " + distance + " m";

        int total = Integer.parseInt(duration);
        String minutes = String.valueOf(total/60);
        String seconds = String.valueOf(total%60);

        this.duration = "Duration: " + minutes + " mins " + seconds + " secs";
        */
        this.speedingCheck = speedingCheck;
        this.weatherCheck = weatherCheck;
        this.phoneCheck = phoneCheck;
        this.laneCheck = laneCheck;
        this.intersectionCheck = intersectionCheck;
    }

    public String getPoints() {
        return points;
    }

    public void setPoints(String points) {
        this.points = points;
    }
    /*
    public String getDistance() {
        return distance;
    }

    public void setDistance(String distance) {
        this.distance = distance;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }
    */
    public boolean isSpeedingCheck() {
        return speedingCheck;
    }

    public void setSpeedingCheck(boolean speedingCheck) {
        this.speedingCheck = speedingCheck;
    }

    public boolean isWeatherCheck() {
        return weatherCheck;
    }

    public void setWeatherCheck(boolean weatherCheck) {
        this.weatherCheck = weatherCheck;
    }

    public boolean isPhoneCheck() {
        return phoneCheck;
    }

    public void setPhoneCheck(boolean phoneCheck) {
        this.phoneCheck = phoneCheck;
    }

    public boolean isLaneCheck() {
        return laneCheck;
    }

    public void setLaneCheck(boolean laneCheck) {
        this.laneCheck = laneCheck;
    }

    public boolean isIntersectionCheck() {
        return intersectionCheck;
    }

    public void setIntersectionCheck(boolean intersectionCheck) {
        this.intersectionCheck = intersectionCheck;
    }
}
