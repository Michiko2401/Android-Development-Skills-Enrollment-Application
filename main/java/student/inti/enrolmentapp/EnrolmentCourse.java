package student.inti.enrolmentapp;

public class EnrolmentCourse {
    private final String id;
    private final String name;

    public EnrolmentCourse(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}

