package student.inti.enrolmentapp;

public class PaymentDetail {
    private final String receiptID;
    private final String paymentDate;
    private final Double paidAmount; // Change to Double
    private final String paymentMethod;

    public PaymentDetail(String receiptID, String paymentDate, Double paidAmount, String paymentMethod) {
        this.receiptID = receiptID;
        this.paymentDate = paymentDate;
        this.paidAmount = paidAmount;
        this.paymentMethod = paymentMethod;
    }

    // Getters
    public String getReceiptID() {
        return receiptID;
    }

    public String getPaymentDate() {
        return paymentDate;
    }

    public Double getPaidAmount() {
        return paidAmount; // Return as Double
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }
}
