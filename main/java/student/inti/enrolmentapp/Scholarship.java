package student.inti.enrolmentapp;

public class Scholarship {
    private final String name;
    private final String type;
    private final String percentage;
    private final String amount;

    public Scholarship(String name, String type, String percentage, String amount) {
        this.name = name;
        this.type = type;
        this.percentage = percentage;
        this.amount = amount;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getPercentage() {
        return percentage;
    }

    public String getAmount() {
        return amount;
    }

    public boolean isAmountAvailable() {
        return !amount.equals("N/A");
    }
}
