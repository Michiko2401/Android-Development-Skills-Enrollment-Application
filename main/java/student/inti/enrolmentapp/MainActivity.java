package student.inti.enrolmentapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import student.inti.enrolmentapp.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private static final String PREF_NAME = "EnrolmentAppPrefs";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String ARG_USER_ID = "arg_user_id";
    private static final String ARG_ROLE = "arg_role";

    private ActivityMainBinding binding;
    private String userId;
    private String role;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Retrieve shared preferences
        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        boolean isLoggedIn = sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
        userId = sharedPreferences.getString(ARG_USER_ID, null);
        role = sharedPreferences.getString(ARG_ROLE, null);

        if (isLoggedIn && userId != null && role != null) {
            // User is logged in; go to HomeFragment
            Log.d("MainActivity", "User logged in: " + userId + " with role: " + role);
            replaceFragment(HomeFragment.newInstance(userId, role));
            binding.bottomNavigationView.setVisibility(View.VISIBLE); // Show navigation bar
        } else {
            // User not logged in; go to LoginFragment
            Log.d("MainActivity", "No user logged in. Redirecting to LoginFragment.");
            replaceFragment(new LoginFragment());
            binding.bottomNavigationView.setVisibility(View.GONE); // Hide navigation bar
        }

        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment fragment = null;

            // Handle navigation
            if (item.getItemId() == R.id.home) {
                if (userId != null && role != null) {
                    fragment = HomeFragment.newInstance(userId, role);
                } else {
                    fragment = new LoginFragment(); // Go to login if data is missing
                }
            } else if (item.getItemId() == R.id.enrolment) {
                if (userId != null) {
                    fragment = EnrollmentFragment.newInstance(userId);
                } else {
                    fragment = new LoginFragment(); // Go to login if user is not logged in
                }
            } else if (item.getItemId() == R.id.payment) {
                if (userId != null) {
                    fragment = PaymentFragment.newInstance(userId);
                } else {
                    fragment = new LoginFragment(); // Go to login if user is not logged in
                }
            } else if (item.getItemId() == R.id.profile) {
                if (userId != null) {
                    fragment = ProfileFragment.newInstance(userId);
                } else {
                    fragment = new LoginFragment(); // Go to login if user is not logged in
                }
            }

            // Replace fragment
            if (fragment != null) {
                replaceFragment(fragment);
            }
            return true;
        });
    }

    // Handle login success
    public void onLoginSuccess(String userId, String role) {
        this.userId = userId;
        this.role = role;

        // Save login state in SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(ARG_USER_ID, userId);
        editor.putString(ARG_ROLE, role);
        editor.apply();

        Log.d("LoginTrack", "Login successful for user: " + userId + ", role: " + role);
        replaceFragment(HomeFragment.newInstance(userId, role));
        binding.bottomNavigationView.setVisibility(View.VISIBLE); // Show navigation bar after login
    }

    // Method to replace fragments
    void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frame_layout, fragment);
        fragmentTransaction.commit();
    }
}
