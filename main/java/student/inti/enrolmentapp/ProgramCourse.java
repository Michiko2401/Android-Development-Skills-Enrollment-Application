package student.inti.enrolmentapp;

public class ProgramCourse {
    private final String courseID;
    private final String courseName;

    public ProgramCourse(String courseID, String courseName) {
        this.courseID = courseID;
        this.courseName = courseName;
    }

    public String getCourseID() {
        return courseID;
    }

    public String getCourseName() {
        return courseName;
    }
}

