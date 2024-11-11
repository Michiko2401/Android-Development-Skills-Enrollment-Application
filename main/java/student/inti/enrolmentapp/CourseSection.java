package student.inti.enrolmentapp;

public class CourseSection {
    private final String courseID;
    private final String courseName;
    private final String lectureDay;
    private final String lectureStartTime;
    private final String lectureEndTime;
    private final String practicalDay;
    private final String practicalStartTime;
    private final String practicalEndTime;
    private final String remarks;
    private final String section;

    // Constructor
    public CourseSection(String courseID, String courseName, String lectureDay, String lectureStartTime, String lectureEndTime,
                         String practicalDay, String practicalStartTime, String practicalEndTime,
                         String remarks, String section) {
        this.courseID = courseID;
        this.courseName = courseName;
        this.lectureDay = lectureDay;
        this.lectureStartTime = lectureStartTime;
        this.lectureEndTime = lectureEndTime;
        this.practicalDay = practicalDay;
        this.practicalStartTime = practicalStartTime;
        this.practicalEndTime = practicalEndTime;
        this.remarks = remarks;
        this.section = section;
    }

    // Getter methods
    public String getLectureDay() {
        return lectureDay;
    }

    public String getLectureStartTime() {
        return lectureStartTime;
    }

    public String getLectureEndTime() {
        return lectureEndTime;
    }

    public String getPracticalDay() {
        return practicalDay;
    }

    public String getPracticalStartTime() {
        return practicalStartTime;
    }

    public String getPracticalEndTime() {
        return practicalEndTime;
    }

    public String getRemarks() {
        return remarks;
    }

    public String getSection() {
        return section;
    }
}
