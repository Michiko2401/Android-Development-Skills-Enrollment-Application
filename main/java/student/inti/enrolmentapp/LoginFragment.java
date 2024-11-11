package student.inti.enrolmentapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class LoginFragment extends Fragment {

    private EditText editTextUserId;
    private EditText editTextPassword;
    private Button buttonLogin;
    private TextView textViewError;

    private FirebaseFirestore db;

    private static final String PREF_NAME = "EnrolmentAppPrefs";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String ARG_USER_ID = "arg_user_id";
    private static final String ARG_ROLE = "arg_role";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        editTextUserId = view.findViewById(R.id.editTextUserId);
        editTextPassword = view.findViewById(R.id.editTextPassword);
        buttonLogin = view.findViewById(R.id.btnLogin);
        textViewError = view.findViewById(R.id.textViewError);

        db = FirebaseFirestore.getInstance();

        buttonLogin.setOnClickListener(v -> loginUser());

        return view;
    }

    @SuppressLint("SetTextI18n")
    private void loginUser() {
        String id = editTextUserId.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (id.isEmpty() || password.isEmpty()) {
            textViewError.setText("ID and Password must not be empty.");
            textViewError.setVisibility(View.VISIBLE);
            return;
        }

        buttonLogin.setEnabled(false); // Prevent multiple logins
        db.collection("Users").document(id).get()
                .addOnCompleteListener(task -> {
                    buttonLogin.setEnabled(true); // Re-enable button
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            String storedPasswordHash = document.getString("Password");
                            String role = document.getString("Role");
                            String enteredPasswordHash = hashPassword(password);
                            if (storedPasswordHash != null && storedPasswordHash.equals(enteredPasswordHash)) {
                                saveLoginState(id, role);
                                Toast.makeText(getActivity(), "Login Successful.", Toast.LENGTH_SHORT).show();
                                navigateToMainActivity();
                            } else {
                                textViewError.setText("Incorrect password.");
                                textViewError.setVisibility(View.VISIBLE);
                            }
                        } else {
                            textViewError.setText("User ID not found.");
                            textViewError.setVisibility(View.VISIBLE);
                        }
                    } else {
                        textViewError.setText("Error fetching data.");
                        textViewError.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    buttonLogin.setEnabled(true);
                    textViewError.setText("Network error. Please try again.");
                    textViewError.setVisibility(View.VISIBLE);
                });
    }

    private void saveLoginState(String userId, String role) {
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(ARG_USER_ID, userId);  // Storing user ID
        editor.putString(ARG_ROLE, role);  // Storing role
        editor.apply();  // Apply changes
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private void navigateToMainActivity() {
        // Navigate to MainActivity after successful login
        Context context = getActivity();
        if (context != null) {
            Intent intent = new Intent(context, MainActivity.class);
            startActivity(intent);
            requireActivity().finish(); // Close the LoginActivity if applicable
        }
    }
}