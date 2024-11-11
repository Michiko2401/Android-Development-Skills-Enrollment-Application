package student.inti.enrolmentapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PaymentAdapter extends RecyclerView.Adapter<PaymentAdapter.PaymentViewHolder> {
    private final List<PaymentDetail> paymentDetails;

    public PaymentAdapter(List<PaymentDetail> paymentDetails) {
        this.paymentDetails = paymentDetails;
    }

    @NonNull
    @Override
    public PaymentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_payment_detail, parent, false);
        return new PaymentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PaymentViewHolder holder, int position) {
        PaymentDetail paymentDetail = paymentDetails.get(position);
        holder.receiptIdTextView.setText(paymentDetail.getReceiptID());
        holder.paymentDateTextView.setText(paymentDetail.getPaymentDate());
        holder.paidAmountTextView.setText(String.format("%.2f", paymentDetail.getPaidAmount())); // Format Double to String
        holder.methodTextView.setText(paymentDetail.getPaymentMethod());
    }

    @Override
    public int getItemCount() {
        return paymentDetails.size();
    }

    public static class PaymentViewHolder extends RecyclerView.ViewHolder {
        TextView receiptIdTextView;
        TextView paymentDateTextView;
        TextView paidAmountTextView;
        TextView methodTextView;

        public PaymentViewHolder(@NonNull View itemView) {
            super(itemView);
            receiptIdTextView = itemView.findViewById(R.id.receipt_id_value);
            paymentDateTextView = itemView.findViewById(R.id.payment_date_value);
            paidAmountTextView = itemView.findViewById(R.id.payment_amount_value);
            methodTextView = itemView.findViewById(R.id.payment_method_value);
        }
    }
}
