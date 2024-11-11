package student.inti.enrolmentapp;

import android.annotation.SuppressLint;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class PurchaseHistoryFragment extends Fragment {

    private static final String ARG_STUDENT_ID = "studentId";
    private String studentId;
    private FirebaseFirestore firestore;
    private PaymentAdapter paymentAdapter;
    private List<PaymentDetail> paymentDetailsList;

    public PurchaseHistoryFragment() {

    }

    public static PurchaseHistoryFragment newInstance(String studentId) {
        PurchaseHistoryFragment fragment = new PurchaseHistoryFragment();
        Bundle args = new Bundle();
        args.putString(ARG_STUDENT_ID, studentId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            studentId = getArguments().getString(ARG_STUDENT_ID);
        }
        firestore = FirebaseFirestore.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_purchase_history, container,
                false);

        RecyclerView recyclerView = view.findViewById(R.id.purchase_history_RecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        paymentDetailsList = new ArrayList<>();
        paymentAdapter = new PaymentAdapter(paymentDetailsList);
        recyclerView.setAdapter(paymentAdapter);

        TextView studentIdValue = view.findViewById(R.id.studentId_value);
        studentIdValue.setText(studentId);

        fetchPaymentDetails();

        return view;
    }

    @SuppressLint("NotifyDataSetChanged")
    private void fetchPaymentDetails() {
        long startTime = System.currentTimeMillis();
        firestore.collection("Payments")
                .whereEqualTo("StudentID", studentId)
                .get()
                .addOnCompleteListener(task -> {
                    long duration = System.currentTimeMillis() - startTime; // Measure duration
                    Log.d("FetchPaymentDetails", "Duration: " + duration + " ms");

                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            paymentDetailsList.clear();
                            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                                String paymentID = document.getId();
                                String paymentMethod = document.getString("Method");

                                // Retrieve PaidAmount as a Double
                                Double paidAmount = document.getDouble("PaidAmount");

                                Timestamp timestamp = document.getTimestamp("PaymentDate");
                                String paymentDate = (timestamp != null)
                                        ? new SimpleDateFormat("dd/MM/yyyy HH:mm",
                                        Locale.forLanguageTag("ms-MY"))
                                        .format(timestamp.toDate())
                                        : "N/A";

                                // Create PaymentDetail object and add to the list
                                PaymentDetail paymentDetail = new PaymentDetail(paymentID,
                                        paymentDate, paidAmount, paymentMethod);
                                paymentDetailsList.add(paymentDetail);
                            }
                            paymentAdapter.notifyDataSetChanged();
                        } else {
                            Log.d("PurchaseHistoryFragment",
                                    "No payments found for the given student ID.");
                        }
                    } else {
                        Log.e("PurchaseHistoryFragment",
                                "Error fetching payments", task.getException());
                    }
                });
    }
}
