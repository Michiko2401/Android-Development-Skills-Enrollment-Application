package student.inti.enrolmentapp;

public class Course {
    private final String id;
    private final String name;
    private final String price;

    public Course(String id, String name, String price) {
        this.id = id;
        this.name = name;
        this.price = price;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPrice() {
        return price;
    }
}
