package student.inti.enrolmentapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.FirebaseFirestore;

public class EditProfile extends Fragment {

    private static final String ARG_STUDENT_ID = "studentId";
    private static final String ARG_STUDENT_NAME = "studentName";
    private static final String ARG_PHONE_NO = "phoneNo";
    private static final String ARG_EMERGENCY_NO = "emergencyNo";
    private static final String ARG_ADDRESS = "address";

    private String studentId;
    private String studentName;
    private String phoneNo;
    private String emergencyNo;
    private String address;

    private FirebaseFirestore db;

    public EditProfile() {
        // Required empty public constructor
    }

    public static EditProfile newInstance(String studentId, String studentName, String phoneNo, String emergencyNo, String address) {
        EditProfile fragment = new EditProfile();
        Bundle args = new Bundle();
        args.putString(ARG_STUDENT_ID, studentId);
        args.putString(ARG_STUDENT_NAME, studentName);
        args.putString(ARG_PHONE_NO, phoneNo);
        args.putString(ARG_EMERGENCY_NO, emergencyNo);
        args.putString(ARG_ADDRESS, address);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            studentId = getArguments().getString(ARG_STUDENT_ID);
            studentName = getArguments().getString(ARG_STUDENT_NAME);
            phoneNo = getArguments().getString(ARG_PHONE_NO);
            emergencyNo = getArguments().getString(ARG_EMERGENCY_NO);
            address = getArguments().getString(ARG_ADDRESS);
        }

        // Initialize Firebase Firestore
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_profile, container, false);

        TextView studentNameTextView = view.findViewById(R.id.edit_student_name);
        TextView studentIdTextView = view.findViewById(R.id.edit_student_id);
        EditText phoneNoEditText = view.findViewById(R.id.edit_student_phone_no);
        EditText emergencyNoEditText = view.findViewById(R.id.edit_student_emergency_no);
        EditText addressEditText = view.findViewById(R.id.edit_student_address);

        // Set data to views
        studentIdTextView.setText(studentId);
        studentNameTextView.setText(studentName);
        phoneNoEditText.setText(phoneNo);
        emergencyNoEditText.setText(emergencyNo);
        addressEditText.setText(address);

        phoneNoEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                phoneNoEditText.setText("");
            }
        });

        emergencyNoEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                emergencyNoEditText.setText("");
            }
        });

        addressEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                addressEditText.setText("");
            }
        });

        // Set up the button click listener
        view.findViewById(R.id.submit_button).setOnClickListener(v -> {
            // Retrieve user input
            String phoneNumber = phoneNoEditText.getText().toString();
            String emergencyNumber = emergencyNoEditText.getText().toString();
            String addressInput = addressEditText.getText().toString();

            // Validate input
            if (phoneNumber.isEmpty() || emergencyNumber.isEmpty() || addressInput.isEmpty()) {
                Toast.makeText(getActivity(), "All fields must be filled out.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Update Firestore
            db.collection("Students").document(studentId)
                    .update("PhoneNumber", phoneNumber, "EmergencyNo", emergencyNumber, "Address", addressInput)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getActivity(), "Profile updated successfully.", Toast.LENGTH_SHORT).show();
                    })

                    .addOnFailureListener(e -> {
                        Toast.makeText(getActivity(), "Error updating profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        });

        return view;
    }
}
