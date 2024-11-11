// Payment amount inconsistent
// Cannot handle if the amount is more than totalSchoolFees
package student.inti.enrolmentapp;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import android.content.DialogInterface;

// Enable to generate PDF for payment

public class PaymentFragment extends Fragment {

    private static final String ARG_USER_ID = "userId";
    private String userId;
    private FirebaseFirestore firestore;

    // Adapters and RecyclerViews
    private CourseAdapter courseAdapter;
    private ScholarshipAdapter scholarshipAdapter;
    private RecyclerView courseRecyclerView;

    // Lists
    private final List<Course> courseList = new ArrayList<>();
    private final List<Scholarship> scholarshipList = new ArrayList<>();

    private double totalPrice = 0.0;
    private double student_balanced= 0.0;
    private double total_balanced= 0.0;
    private double student_scholarship_amount = 0.0;
    private int student_scholarship_percentage = 0;
    private double totalScholarship, totalSchoolFees = 0.0;
    private double cardPaymentAmount = 0.0;
    private String enrolledCourseID = "";
    private boolean hasCourses = false; // Flag to indicate if courses are present

    public PaymentFragment() {
        // Required empty public constructor
    }

    public static PaymentFragment newInstance(String userId) {
        PaymentFragment fragment = new PaymentFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USER_ID, userId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_payment, container, false);

        // Initialize Firestore instance
        firestore = FirebaseFirestore.getInstance();

        if (getArguments() != null) {
            userId = getArguments().getString(ARG_USER_ID);
        }

        // Set up course RecyclerView
        courseRecyclerView = view.findViewById(R.id.courseRecyclerView);
        courseRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        courseAdapter = new CourseAdapter(courseList);
        courseRecyclerView.setAdapter(courseAdapter);

        // Set up scholarship RecyclerView
        RecyclerView scholarshipRecyclerView = view.findViewById(R.id.scholarshipRecyclerView);
        scholarshipRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        scholarshipAdapter = new ScholarshipAdapter(scholarshipList);
        scholarshipRecyclerView.setAdapter(scholarshipAdapter);

        // Fetch course and scholarship data
        fetchNextCourseDocument();
        fetchBalanced();

        // Set up button listener for proceeding to payment
        Button proceedPaymentButton = view.findViewById(R.id.proceed_to_payment_button);
        proceedPaymentButton.setOnClickListener(this::onProceedPaymentClick);

        Button payBalancedPaymentButton = view.findViewById(R.id.pay_by_balance_button);
        payBalancedPaymentButton.setOnClickListener(this::payByBalanceClick);

        // Set up purchase history button listener
        ImageButton purchaseHistoryButton = view.findViewById(R.id.btn_purchase_history);
        purchaseHistoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create new instance of PurchaseHistoryFragment
                PurchaseHistoryFragment purchaseHistoryFragment = new PurchaseHistoryFragment();

                // Create a bundle to pass the studentId
                Bundle bundle = new Bundle();
                bundle.putString("studentId", userId);
                purchaseHistoryFragment.setArguments(bundle);

                // Replace the current fragment with PurchaseHistoryFragment
                FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
                transaction.replace(R.id.frame_layout, purchaseHistoryFragment); // Make sure you replace the correct container ID
                transaction.addToBackStack(null); // Add to backstack so the user can navigate back
                transaction.commit();
            }
        });

        return view;
    }



    public void onProceedPaymentClick(View view) {
        ProceedPaymentFragment proceedPaymentFragment = getProceedPaymentFragment();

        // Begin the fragment transaction to replace the current fragment
        FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.frame_layout, proceedPaymentFragment);
        transaction.addToBackStack(null);
        transaction.commit();

    }

    private void payByBalanceClick(View view) {
        // Format the message with totalSchoolFees
        String message = String.format("Are you sure you want to pay RM %.2f from balance?", totalSchoolFees);

        // Show a confirmation dialog with the formatted message
        new AlertDialog.Builder(getContext())
                .setMessage(message)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Code to execute when the user confirms
                        Toast.makeText(getContext(), "Payment confirmed by balance.", Toast.LENGTH_SHORT).show();

                        // Update Firebase with the new balance and enrolled course status
                        FirebaseFirestore db = FirebaseFirestore.getInstance();

                        // Update "Balanced" field in Firebase
                        db.collection("Students").document(userId)
                                .update("Balanced", total_balanced)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(getActivity(), "Balance updated successfully.", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(getActivity(), "Error updating balance: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                });

                        // Update "EnrolledCourse" field in Firebase
                        db.collection("Students").document(userId)
                                .update("EnrolledCourse", "-")
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(getActivity(), "Enrolled course status updated successfully.", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(getActivity(), "Error updating enrolled course status: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                });

                        // Reload the PaymentFragment after the transaction
                        reloadFragment();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Dismiss the dialog without any further action
                        dialog.dismiss();
                    }
                })
                .show();
    }

    private void reloadFragment() {
        // Create a new instance of PaymentFragment with the current userId
        PaymentFragment paymentFragment = PaymentFragment.newInstance(userId);

        // Begin the fragment transaction to replace the current fragment
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.frame_layout, paymentFragment);
        transaction.addToBackStack(null); // Add to backstack so the user can navigate back
        transaction.commit();
    }






    @SuppressLint("SetTextI18n")
    private void fetchNextCourseDocument() {
        firestore.collection("Students").document(userId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Map<String, Object> data = document.getData();
                            if (data != null) {
                                enrolledCourseID = data.get("EnrolledCourse") != null ?
                                        Objects.requireNonNull(data.get("EnrolledCourse")).toString() :
                                        "N/A";

                                if (enrolledCourseID.equals("-") || enrolledCourseID.isEmpty()) {
                                    // No courses, update flag and UI
                                    hasCourses = false;
                                    TextView noCourseMessageTextView = getView().findViewById(R.id.no_course_message);
                                    noCourseMessageTextView.setVisibility(View.VISIBLE);
                                    courseRecyclerView.setVisibility(View.GONE);

                                    // Set course-related values to RM 0.00
                                    TextView totalCoursePriceTextView = getView().findViewById(R.id.total_course_price);
                                    totalCoursePriceTextView.setText("RM 0.00");

                                    TextView totalScholarshipTextView = getView().findViewById(R.id.total_scholarship);
                                    totalScholarshipTextView.setText("- RM 0.00");

                                    TextView totalSchoolFeesTextView = getView().findViewById(R.id.total_school_fees);
                                    totalSchoolFeesTextView.setText("RM 0.00");

                                    // Fetch balance with updated UI
                                    fetchBalanced();
                                } else {
                                    hasCourses = true;
                                    String[] upcomingCoursesArray = enrolledCourseID.split(",");
                                    for (String courseAndSection : upcomingCoursesArray) {
                                        String courseId = courseAndSection.split("\\(")[0].trim();
                                        String section = courseAndSection.split("\\(")[1].replace(")", "").trim();
                                        fetchCourseDetails(courseId, section);
                                    }
                                }
                            }
                        }
                    }
                });
    }

    @SuppressLint({"NotifyDataSetChanged", "DefaultLocale"})
    private void fetchCourseDetails(String courseId, String section) {
        firestore.collection("Courses").document(courseId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Map<String, Object> data = document.getData();
                            if (data != null) {
                                String courseName = data.get("Name") != null ?
                                        Objects.requireNonNull(data.get("Name")).toString() : "N/A";
                                String priceString = data.get("Price") != null ?
                                        Objects.requireNonNull(data.get("Price")).toString() : "0";

                                try {
                                    // Parse and format price
                                    double price = Double.parseDouble(priceString);
                                    @SuppressLint("DefaultLocale") String formattedPrice =
                                            String.format("RM %.2f", price);

                                    // Add course to the list
                                    courseList.add(new Course(courseId, courseName, formattedPrice));

                                    // Update total price
                                    totalPrice += price;
                                    courseAdapter.notifyDataSetChanged();

                                    // Update the total price TextView
                                    TextView totalCoursePriceTextView = getView().findViewById(R.id.total_course_price);
                                    totalCoursePriceTextView.setText(String.format("RM %.2f", totalPrice));

                                    Log.d("fetchCourseDetails", "Added course: " + courseName);
                                } catch (NumberFormatException e) {
                                    Log.e("fetchCourseDetails", "Error parsing price: ", e);
                                }
                            }
                        }
                    }
                });
    }

    @SuppressLint("DefaultLocale")
    private void fetchBalanced() {
        // Use Firestore real-time listener if you need automatic updates, or leave it as a one-time fetch
        firestore.collection("Students").document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            Map<String, Object> data = document.getData();
                            if (data != null) {
                                // Retrieve and format balance
                                Object balancedObject = data.get("Balanced");
                                if (balancedObject instanceof Number) {
                                    student_balanced = ((Number) balancedObject).doubleValue();
                                } else if (balancedObject instanceof String) {
                                    try {
                                        student_balanced = Double.parseDouble((String) balancedObject);
                                    } catch (NumberFormatException e) {
                                        Log.e("fetchBalanced", "Error parsing balance value: " + balancedObject, e);
                                        student_balanced = 0.0; // Fallback if parsing fails
                                    }
                                } else {
                                    Log.e("fetchBalanced", "Balance format is unsupported: " + balancedObject);
                                    student_balanced = 0.0; // Default if balance is missing or of unexpected type
                                }

                                Log.d("fetchBalanced", "Balanced: RM " + student_balanced);

                                // Update TextView with balance
                                if (getView() != null) {
                                    TextView balancedTextView = getView().findViewById(R.id.total_account_balanced);
                                    if (hasCourses) {
                                        balancedTextView.setText(String.format("RM %.2f", student_balanced));
                                    } else {
                                        balancedTextView.setText(String.format("RM %.2f", student_balanced)); // No negative sign
                                    }
                                }

                                // Retrieve and process scholarships
                                Object scholarshipIdObject = data.get("ScholarshipID");
                                if (scholarshipIdObject != null) {
                                    String[] scholarshipIdArray = scholarshipIdObject.toString().replaceAll("\\s+", "").split(",");
                                    for (String scholarshipId : scholarshipIdArray) {
                                        fetchScholarshipDetails(scholarshipId);
                                    }
                                }
                            }
                        } else {
                            Log.d("fetchBalanced", "Document does not exist.");
                        }
                    } else {
                        Log.e("fetchBalanced", "Task failed with exception: ", task.getException());
                    }
                });
    }


    @SuppressLint("NotifyDataSetChanged")
    private void fetchScholarshipDetails(String scholarshipId) {
        // Check if the scholarshipList already contains 3 items
        if (scholarshipList.size() >= 3) {
            return; // Stop adding more scholarships once we have 3
        }

        firestore.collection("Scholarship").document(scholarshipId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot scholarshipDocument = task.getResult();
                        if (scholarshipDocument.exists()) {
                            Map<String, Object> scholarshipData = scholarshipDocument.getData();
                            if (scholarshipData != null) {
                                String name = scholarshipData.get("Name") != null ?
                                        Objects.requireNonNull(scholarshipData.get("Name")).toString() : "N/A";

                                // Handle Percentage field
                                String percentageStr = scholarshipData.get("Percentage") != null ?
                                        Objects.requireNonNull(scholarshipData.get("Percentage")).toString() : "-";
                                int percentage = 0;
                                if (percentageStr.equals("-")) {
                                    percentageStr = "-"; // Keep displaying "-" but treat it as 0 for calculations
                                } else {
                                    try {
                                        percentage = Integer.parseInt(percentageStr.trim());
                                    } catch (NumberFormatException e) {
                                        System.err.println("Invalid percentage format: " + percentageStr);
                                    }
                                }

                                // Handle Amount field
                                String amountStr = scholarshipData.get("Amount") != null ? Objects.requireNonNull(scholarshipData.get("Amount")).toString() : "-";
                                double amount = 0.0;
                                if (amountStr.equals("-")) {
                                    amountStr = "-"; // Keep displaying "-" but treat it as 0.0 for calculations
                                } else {
                                    try {
                                        amount = Double.parseDouble(amountStr.trim());
                                    } catch (NumberFormatException e) {
                                        System.err.println("Invalid amount format: " + amountStr);
                                    }
                                }

                                // Check if the scholarship is already in the list (to prevent duplicates)
                                boolean alreadyInList = false;
                                for (Scholarship scholarship : scholarshipList) {
                                    if (scholarship.getName().equals(name)) {
                                        alreadyInList = true;
                                        break;
                                    }
                                }

                                String type = scholarshipData.get("Type") != null ? Objects.requireNonNull(scholarshipData.get("Type")).toString() : "N/A";

                                // Add scholarship to the list if it's not already present and if we don't have 3 items yet
                                if (!alreadyInList && scholarshipList.size() < 3) {
                                    // Add the new scholarship (keep the original amountStr and percentageStr even if it's "-")
                                    scholarshipList.add(new Scholarship(name, type, percentageStr, amountStr));
                                    scholarshipAdapter.notifyDataSetChanged();
                                    Log.d("fetchScholarshipDetails", "Added scholarship: " + name);

                                    // Accumulate values (use actual numbers, not "-")
                                    student_scholarship_amount += amount;
                                    student_scholarship_percentage += percentage;

                                    // Calculate total amount after adding the new scholarship

                                    calculateTotalAmount();
                                }

                                // Log accumulated values
                                Log.d("fetchScholarshipDetails", "Student Scholarship Percentage: " + student_scholarship_percentage + "%");
                                Log.d("fetchScholarshipDetails", "Student Scholarship Amount: RM " + student_scholarship_amount);
                            }
                        }
                    } else {
                        // Handle unsuccessful task
                        Log.e("fetchScholarshipDetails", "Error fetching scholarship details", task.getException());
                    }
                });
    }

    private void calculateTotalAmount() {


        if (totalPrice > 0) {
            // Calculate total scholarship based on percentage and amount
            totalScholarship = student_scholarship_amount + ((student_scholarship_percentage / 100.0) * totalPrice);

            // Calculate total school fees after applying the scholarship

            totalSchoolFees = totalPrice - totalScholarship;

            // proceed_to_payment_button
            Button proceedPaymentButton = getView().findViewById(R.id.proceed_to_payment_button);
            Button payBalancedPaymentButton = getView().findViewById(R.id.pay_by_balance_button);

            if (totalSchoolFees > 0 && student_balanced > 0 && student_balanced < totalSchoolFees){
                // Pay by balanced and card
                cardPaymentAmount = totalSchoolFees - student_balanced;
                payBalancedPaymentButton.setVisibility(View.GONE);

            } else if (totalSchoolFees > 0 && student_balanced > 0 && student_balanced > totalSchoolFees ) {
                // pay by balanced only
                total_balanced = student_balanced - totalSchoolFees;
                cardPaymentAmount = 0.0;
                proceedPaymentButton.setVisibility(View.GONE);
                payBalancedPaymentButton.setVisibility(View.VISIBLE);



            }

            //totalSchoolFees = totalPrice - totalScholarship- student_balanced;

            /*
            if (totalSchoolFees >0.0){
                student_balanced = 0.0;
            } else if(totalSchoolFees < 0.0){
                student_balanced = totalSchoolFees;
                totalSchoolFees = 0.0;
            }

             */

            // Adjust totalSchoolFees based on student_balanced
        } else {
            // No price available, set values to 0.00
            totalScholarship = 0.0;
            totalSchoolFees = 0.0;
        }

        // Format and display values
        @SuppressLint("DefaultLocale") String formattedTotalScholarship = totalScholarship > 0 ?
                String.format("- RM %.2f", totalScholarship) : "- RM 0.00";
        @SuppressLint("DefaultLocale") String formattedTotalSchoolFees = totalSchoolFees > 0 ?
                String.format("RM %.2f", totalSchoolFees) : "RM 0.00";
        @SuppressLint("DefaultLocale") String formattedTotalBalanced = student_balanced > 0 ?
                String.format("RM %.2f", student_balanced) : "RM 0.00";
        @SuppressLint("DefaultLocale") String formattedCardAmount = cardPaymentAmount > 0 ?
                String.format("RM %.2f", cardPaymentAmount) : "RM 0.00";

        TextView totalScholarshipTextView = getView().findViewById(R.id.total_scholarship);
        totalScholarshipTextView.setText(formattedTotalScholarship);

        TextView totalSchoolFeesTextView = getView().findViewById(R.id.total_school_fees);
        totalSchoolFeesTextView.setText(formattedTotalSchoolFees);

        TextView cardPaymentTextView = getView().findViewById(R.id.card_payment_amount_value);
        cardPaymentTextView.setText(formattedCardAmount);

    }



    private @NonNull ProceedPaymentFragment getProceedPaymentFragment() {
        ProceedPaymentFragment proceedPaymentFragment = new ProceedPaymentFragment();

        // Create a Bundle to pass the student information
        Bundle bundle = new Bundle();
        bundle.putString("studentId", userId);
        bundle.putDouble("totalPrice", totalPrice);
        bundle.putDouble("accountBalance", student_balanced);
        bundle.putDouble("totalScholarship", totalScholarship);
        bundle.putDouble("totalSchoolFees", cardPaymentAmount);
        bundle.putString("enrolledCourseID", enrolledCourseID);

        // Log values for debugging
        Log.d("PaymentFragment", "Passing values to ProceedPaymentFragment:");
        Log.d("PaymentFragment", "Total Price: " + totalPrice);
        Log.d("PaymentFragment", "Account Balance: " + student_balanced);
        Log.d("PaymentFragment", "Total Scholarship: " + totalScholarship);
        Log.d("PaymentFragment", "Total School Fees: " + totalSchoolFees);
        Log.d("PaymentFragment", "Total Card Payment: " + cardPaymentAmount);

        // Set the arguments to the fragment
        proceedPaymentFragment.setArguments(bundle);
        return proceedPaymentFragment;
    }
}
