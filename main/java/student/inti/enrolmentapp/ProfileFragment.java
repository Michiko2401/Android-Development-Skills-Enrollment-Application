// Collaboration id, name
// Display balanced and intake
package student.inti.enrolmentapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
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
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;

import androidx.fragment.app.FragmentTransaction;
import android.widget.Button;
import android.widget.Toast;

// Make sure the remaining course and fail course can be display

public class ProfileFragment extends Fragment {

    private static final String PREF_NAME = "EnrolmentAppPrefs";
    private static final String ARG_USER_ID = "userId";
    private String userId;
    private FirebaseFirestore firestore;
    private String studentName, studentPhoneNo, studentEmergencyNo, studentAddress;

    public ProfileFragment() {
    }

    public static ProfileFragment newInstance(String userId) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USER_ID, userId);
        fragment.setArguments(args);
        Log.d("LoginTrack", "Loading profile for: " + userId);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        Button btnLogout = view.findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> {
            // Perform the logout action here
            btnLogoutClick();
        });

        Button editProfileButton = view.findViewById(R.id.edit_profile_button); // Replace with your button ID
        editProfileButton.setOnClickListener(this::onEditProfileClick);

        // Retrieve userId from arguments
        Bundle arguments = getArguments();
        if (arguments != null) {
            userId = arguments.getString(ARG_USER_ID);
        }

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();

        // Check if userId is valid before querying Firestore
        if (userId != null && !userId.isEmpty()) {
            // Fetch and display data for the student
            fetchSpecificDocument(view);

            return view;

        } else {
            Log.e("ProfileFragment", "User ID is null or empty.");
        }

        return view;
    }

    // Method that handles the logout action
    private void btnLogoutClick() {
        // Clear user session from SharedPreferences
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear(); // Clear all saved data
        editor.apply();

        // Debugging: Log the SharedPreferences after clearing
        Map<String, ?> allEntries = sharedPreferences.getAll();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            Log.d("SharedPreferencesAfter", entry.getKey() + ": " + entry.getValue().toString());
        }

        // Show logout message
        Toast.makeText(requireContext(), "Logged out successfully.", Toast.LENGTH_SHORT).show();

        // Navigate to MainActivity after successful login
        Context context = getActivity();
        if (context != null) {
            Intent intent = new Intent(context, MainActivity.class);
            startActivity(intent);
            requireActivity().finish();
        }
    }

    public void onEditProfileClick(View view) {
        EditProfile editProfileFragment = getEditProfile();

        // Begin the fragment transaction to replace the current fragment
        FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.frame_layout, editProfileFragment); // Replace with the FrameLayout ID from XML
        transaction.addToBackStack(null); // Add the transaction to the back stack
        transaction.commit(); // Commit the transaction
    }

    private @NonNull EditProfile getEditProfile() {
        EditProfile editProfileFragment = new EditProfile();

        // Create a Bundle to pass the student information
        Bundle bundle = new Bundle();
        bundle.putString("studentId", userId);
        bundle.putString("studentName", studentName);
        bundle.putString("phoneNo", studentPhoneNo);
        bundle.putString("emergencyNo", studentEmergencyNo);
        bundle.putString("address", studentAddress);

        // Set the arguments to the fragment
        editProfileFragment.setArguments(bundle);
        return editProfileFragment;
    }

    private void fetchSpecificDocument(View view) {
        // Fetch student information from the "Students" collection based on the userId value
        firestore.collection("Students").document(userId).get()
                .addOnCompleteListener(task -> {
                    // Validate if Firestore data is retrieved successfully
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        // Validate document exists
                        if (document.exists()) {
                            Map<String, Object> data = document.getData();

                            // Validate the document is not null
                            if (data != null) {
                                // Retrieve and set each field from Firestore, default value is 'N/A'
                                String email = data.get("Email") != null ? Objects.requireNonNull(data.get("Email")).toString() : "N/A";
                                String failedCourses = data.get("FailedCourses") != null ? Objects.requireNonNull(data.get("FailedCourses")).toString() : "N/A";
                                String currentCourses = data.get("CurrentCourses") != null ? Objects.requireNonNull(data.get("CurrentCourses")).toString() : "N/A";

                                // Retrieve intake and currentSession timestamp, convert to date, format the date, and set the timezone
                                String intake = "N/A";
                                if (data.get("Intake") != null) {
                                    Timestamp timestamp = (Timestamp) data.get("Intake");
                                    assert timestamp != null;
                                    Date date = timestamp.toDate();
                                    @SuppressLint("SimpleDateFormat")
                                    SimpleDateFormat sdf = new SimpleDateFormat("MMMyyyy");
                                    sdf.setTimeZone(TimeZone.getTimeZone("Asia/Kuala_Lumpur"));
                                    intake = sdf.format(date);
                                }

                                String currentSession = "N/A";
                                if (data.get("Session") != null) {
                                    Timestamp timestamp = (Timestamp) data.get("Session");
                                    assert timestamp != null;
                                    Date date = timestamp.toDate();
                                    @SuppressLint("SimpleDateFormat")
                                    SimpleDateFormat sdf = new SimpleDateFormat("MMMyyyy");
                                    sdf.setTimeZone(TimeZone.getTimeZone("Asia/Kuala_Lumpur"));
                                    currentSession = sdf.format(date);
                                }

                                // Retrieve and set each field from Firestore, default value is 'N/A'
                                studentName = data.get("Name") != null ? Objects.requireNonNull(data.get("Name")).toString() : "N/A";
                                String schoolId = data.get("SchoolID") != null ? Objects.requireNonNull(data.get("SchoolID")).toString() : "N/A";
                                String programmeID = data.get("ProgrammeID") != null ? Objects.requireNonNull(data.get("ProgrammeID")).toString() : "N/A";
                                String completedCourses = data.get("CompletedCourses") != null ? Objects.requireNonNull(data.get("CompletedCourses")).toString() : "N/A";
                                String remainingCourses = data.get("RemainingCourses") != null ? Objects.requireNonNull(data.get("RemainingCourses")).toString() : "N/A";
                                String studyMode = data.get("StudyMode") != null ? Objects.requireNonNull(data.get("StudyMode")).toString() : "N/A";
                                String level = data.get("Level") != null ? Objects.requireNonNull(data.get("Level")).toString() : "N/A";
                                studentAddress = data.get("Address") != null ? Objects.requireNonNull(data.get("Address")).toString() : "N/A";
                                studentPhoneNo = data.get("PhoneNumber") != null ? Objects.requireNonNull(data.get("PhoneNumber")).toString() : "N/A";
                                studentEmergencyNo = data.get("EmergencyNo") != null ? Objects.requireNonNull(data.get("EmergencyNo")).toString() : "N/A";
                                int currentSemester = data.get("CurrentSemester") != null ? ((Long) Objects.requireNonNull(data.get("CurrentSemester"))).intValue() : -1;
                                int totalCourses = data.get("TotalCourses") != null ? ((Long) Objects.requireNonNull(data.get("TotalCourses"))).intValue() : -1;
                                int year = data.get("Year") != null ? ((Long) Objects.requireNonNull(data.get("Year"))).intValue() : -1;

                                // Set student information fields in the UI
                                TextView studentNameTextView = view.findViewById(R.id.student_name);
                                studentNameTextView.setText(studentName);

                                TextView studentIDTextView = view.findViewById(R.id.student_ID);
                                studentIDTextView.setText(userId);

                                TextView studentPhoneNoTextView = view.findViewById(R.id.student_phone_no);
                                studentPhoneNoTextView.setText(studentPhoneNo);

                                TextView studentEmergencyNoTextView = view.findViewById(R.id.student_emergency_no);
                                studentEmergencyNoTextView.setText(studentEmergencyNo);

                                TextView studentEmailTextView = view.findViewById(R.id.student_email);
                                studentEmailTextView.setText(email);

                                TextView studentAddressTextView = view.findViewById(R.id.student_address);
                                studentAddressTextView.setText(studentAddress);

                                TextView studentIntakeTextView = view.findViewById(R.id.student_intake);
                                studentIntakeTextView.setText(intake);

                                TextView currentSessionTextView = view.findViewById(R.id.student_session);
                                currentSessionTextView.setText(currentSession);

                                TextView studentSchoolIDTextView = view.findViewById(R.id.student_school_Id);
                                studentSchoolIDTextView.setText(schoolId);

                                TextView studentProgramIdTextView = view.findViewById(R.id.student_programId);
                                studentProgramIdTextView.setText(programmeID);

                                TextView studyModeTextView = view.findViewById(R.id.student_study_mode);
                                studyModeTextView.setText(studyMode);

                                TextView levelTextView = view.findViewById(R.id.student_level);
                                levelTextView.setText(level);

                                TextView currentSemesterTextView = view.findViewById(R.id.student_current_semester);
                                currentSemesterTextView.setText(String.valueOf(currentSemester));

                                TextView totalNoCourseTextView = view.findViewById(R.id.student_course_no);
                                totalNoCourseTextView.setText(String.valueOf(totalCourses));

                                TextView yearTextView = view.findViewById(R.id.student_year);
                                yearTextView.setText(String.valueOf(year));

                                // Display current course information
                                TextView currentCourseTextView = view.findViewById(R.id.student_current_course);
                                if (currentCourses.equals("-")){
                                    TextView noCurrentCourseMessageTextView = getView().findViewById(R.id.no_current_course_message);
                                    noCurrentCourseMessageTextView.setVisibility(View.VISIBLE);
                                    currentCourseTextView.setVisibility(View.GONE);
                                }else{
                                    String[] currentCoursesArray = currentCourses.split(",");
                                    for (String courseAndSection : currentCoursesArray) {
                                        String courseId = courseAndSection.split("\\(")[0].trim();
                                        String section = courseAndSection.split("\\(")[1].replace(")", "").trim();
                                        fetchCourseName(courseId, section, currentCourseTextView, true);
                                    }
                                }

                                // Display remaining course information
                                TextView remainingCourseTextView = view.findViewById(R.id.student_remaining_course);
                                if (remainingCourses.equals("-")){
                                    TextView noRemainingCourseMessageTextView = getView().findViewById(R.id.no_remaining_course_message);
                                    noRemainingCourseMessageTextView.setVisibility(View.VISIBLE);
                                    remainingCourseTextView.setVisibility(View.GONE);
                                }else{
                                    String[] remainingCoursesArray = remainingCourses.split(",");
                                    for (String courseId : remainingCoursesArray) {
                                        courseId = courseId.trim();  // Removes any leading and trailing spaces
                                        fetchRemainCourseName(courseId, remainingCourseTextView);  // Pass the trimmed courseId
                                    }
                                }

                                // Display completed course information
                                TextView completeCourseTextView = view.findViewById(R.id.student_completed_course);
                                if (completedCourses.equals("-")){
                                    TextView noCompletedCourseMessageTextView = getView().findViewById(R.id.no_completed_course_message);
                                    noCompletedCourseMessageTextView.setVisibility(View.VISIBLE);
                                    completeCourseTextView.setVisibility(View.GONE);
                                }else{
                                    String[] completeCoursesArray = completedCourses.split(",");
                                    for (String courseId : completeCoursesArray) {
                                        courseId = courseId.trim();
                                        fetchCompletedCourseName(courseId, completeCourseTextView);
                                    }
                                }

                                // Display failed course information
                                TextView failedCourseTextView = view.findViewById(R.id.student_failed_course);
                                if (failedCourses.equals("-")){
                                    TextView noFailedCourseMessageTextView = getView().findViewById(R.id.no_failed_course_message);
                                    noFailedCourseMessageTextView.setVisibility(View.VISIBLE);
                                    failedCourseTextView.setVisibility(View.GONE);
                                }else {
                                    String[] failedCoursesArray = failedCourses.split(",");
                                    for (String courseAndSection : failedCoursesArray) {
                                        String courseId = courseAndSection.split("\\(")[0].trim();
                                        String section = courseAndSection.split("\\(")[1].replace(")", "").trim();
                                        fetchCourseName(courseId, section, failedCourseTextView, false);
                                    }
                                }

                                // Validate programme ID and fetch program name and collaboration name
                                if (!programmeID.equals("N/A")) {
                                    fetchProgramName(programmeID, view);
                                    fetchCollaboration(programmeID, view);
                                }else {
                                    Log.d("Firestore", "Invalid programmeID: " + programmeID);
                                }

                                // Validate school ID and fetch school name
                                if (!schoolId.equals("N/A")) {
                                    Log.d("Firestore", "Attempting to fetch school name for schoolID: " + schoolId);
                                    schoolId = schoolId.toUpperCase().replace(" ", "");
                                    fetchSchoolName(schoolId, view);
                                } else {
                                    Log.d("Firestore", "Invalid schoolID: " + schoolId);
                                }

                            } else {
                                Log.d("FirestoreData", "No data in document.");
                            }
                        } else {
                            Log.d("FirestoreData", "No such document.");
                        }
                    } else {
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "Unknown error occurred";
                        Log.e("FirestoreData", "Failed to retrieve document: " + errorMessage, task.getException());
                    }
                });
    }

    private void fetchSchoolName(String schoolId, View view) {

        firestore.collection("School").document(schoolId).get()
                .addOnCompleteListener(task -> {
                    DocumentSnapshot document = task.getResult(); // Fetch the document here

                    // Log the DocumentSnapshot before checking if it's successful
                    Log.d("fetchSchoolName", "Fetched document: " + document);
                    Log.d("fetchSchoolName", "Fetched school id: " + schoolId);

                    if (task.isSuccessful()) {
                        if (document != null && document.exists()) {
                            Map<String, Object> data = document.getData();

                            if (data != null) {
                                String schoolName = data.get("Name") != null ? Objects.requireNonNull(data.get("Name")).toString() : "N/A";
                                TextView schoolNameTextView = view.findViewById(R.id.student_school_name);
                                schoolNameTextView.setText(schoolName);
                                Log.d("fetchSchoolName", "Setting school name: " + schoolName);
                            } else {
                                Log.d("Firestore", "No data in the school document.");
                            }
                        } else {
                            Log.d("Firestore", "No such school document.");
                        }
                    } else {
                        Log.e("Firestore", "Error fetching school document: ", task.getException());
                    }
                });
    }

    private int courseCounter = 1; // Counter for the courses
    private int failedCourseCounter = 1;

    @SuppressLint("SetTextI18n")
    private void fetchCourseName(String courseId, String section, TextView targetTextView, boolean isCurrentCourse) {
        firestore.collection("Courses").document(courseId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Map<String, Object> data = document.getData();
                            if (data != null) {
                                String courseName = data.get("Name") != null ? Objects.requireNonNull(data.get("Name")).toString() : "N/A";
                                String courseIdText = courseId + " (" + section + "): \n";
                                String newText = (isCurrentCourse ? courseCounter : failedCourseCounter) + ". " + courseIdText + courseName + "\n";

                                // Post update to TextView on the main thread
                                targetTextView.post(() -> {
                                    String previousText = targetTextView.getText().toString();
                                    targetTextView.setText(previousText + newText);
                                });

                                // Increment the counter after updating the TextView
                                if (isCurrentCourse) {
                                    courseCounter++;
                                } else {
                                    failedCourseCounter++;
                                }

                                Log.d("fetchCourseName", "Setting course name: " + courseName);
                            } else {
                                Log.d("Firestore", "No data in the course document.");
                            }
                        } else {
                            Log.d("Firestore", "No such course document.");
                        }
                    } else {
                        Log.e("Firestore", "Error fetching course document: ", task.getException());
                    }
                });
    }

    private int remainCourseCounter = 1;

    @SuppressLint("SetTextI18n")
    private void fetchRemainCourseName(String courseId, TextView targetTextView) {
        // Don't reset remainCourseCounter here
        firestore.collection("Course99").document(courseId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Map<String, Object> data = document.getData();

                            if (data != null) {
                                String courseName = data.get("Name") != null ? Objects.requireNonNull(data.get("Name")).toString() : "N/A";

                                String courseIdText = courseId + ": \n";
                                String newText = remainCourseCounter + ". " + courseIdText + courseName + "\n";

                                targetTextView.post(() -> {
                                    // Safely append the new text to the current text in the TextView
                                    String previousText = targetTextView.getText().toString();
                                    targetTextView.setText(previousText + newText);
                                });

                                // Increment the counter after updating the TextView
                                remainCourseCounter++;

                                Log.d("fetchCourseName", "Setting course name: " + courseName);
                            } else {
                                Log.d("Firestore", "No data in the course document.");
                            }
                        } else {
                            Log.d("Firestore", "No such course document.");
                        }
                    } else {
                        Log.e("Firestore", "Error fetching course document: ", task.getException());
                    }
                });
    }

    private int completedCourseCounter = 1; // Counter for the courses

    @SuppressLint("SetTextI18n")
    private void fetchCompletedCourseName(String courseId, TextView targetTextView) {
        remainCourseCounter = 1;
        firestore.collection("Course99").document(courseId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Map<String, Object> data = document.getData();

                            if (data != null) {
                                String courseName = data.get("Name") != null ? Objects.requireNonNull(data.get("Name")).toString() : "N/A";

                                String courseIdText = courseId + ": \n";
                                String newText = completedCourseCounter + ". " + courseIdText + courseName + "\n";

                                targetTextView.post(() -> {
                                    // Safely append the new text to the current text in the TextView
                                    String previousText = targetTextView.getText().toString();
                                    targetTextView.setText(previousText + newText);
                                });

                                // Increment the counter after updating the TextView
                                completedCourseCounter++;

                                Log.d("fetchCourseName", "Setting course name: " + courseName);
                            } else {
                                Log.d("Firestore", "No data in the course document.");
                            }
                        } else {
                            Log.d("Firestore", "No such course document.");
                        }
                    } else {
                        Log.e("Firestore", "Error fetching course document: ", task.getException());
                    }
                });
    }

    private void fetchProgramName(String programId, View view) {
        firestore.collection("Programmes").document(programId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Map<String, Object> data = document.getData();


                            if (data != null) {
                                String programName = data.get("FullName") != null ? Objects.requireNonNull(data.get("FullName")).toString() : "N/A";
                                TextView programNameTextView = view.findViewById(R.id.student_program_name);
                                programNameTextView.setText(programName);
                                Log.d("fetchProgramName", "Setting program name: " + programName);
                            } else {
                                Log.d("Firestore", "No data in the program document.");
                            }
                        } else {
                            Log.d("Firestore", "No such program document.");
                        }
                    } else {
                        Log.e("Firestore", "Error fetching program document: ", task.getException());
                    }
                });
    }

    private void fetchCollaboration(String programID, View view) {
        firestore.collection("Collaboration")
                .whereEqualTo("ProgramID", programID) // Query for program courses
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                                String collaborationID = document.getId(); // Document ID as courseID
                                String collaborationName = document.getString("Name");
                                TextView collaborationIDTextView = view.findViewById(R.id.collaboration_id);
                                collaborationIDTextView.setText(collaborationID);

                                TextView collaborationNameTextView = view.findViewById(R.id.collaboration_name);
                                collaborationNameTextView.setText(collaborationName);
                            }

                        } else {
                            Log.d("EnrolmentFragment", "No courses found for the given programmeID.");
                        }
                    } else {
                        Log.e("EnrolmentFragment", "Error fetching courses", task.getException());
                    }
                });
    }
}
