package student.inti.enrolmentapp;

import android.graphics.drawable.AdaptiveIconDrawable;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Before do the selection course section, make sure that can get the information first
public class EnrolmentFragment extends Fragment {

    private static final String ARG_USER_ID = "userId";
    private String userId;
    private FirebaseFirestore firestore; // Firestore instance
    private boolean hasCourses = true; // Track if courses exist
    private RecyclerView enrolmentCourseRecyclerView; // RecyclerView for enrolment courses
    private List<EnrolmentCourse> courseList; // List for course data
    private EnrolmentCourseAdapter courseAdapter; // Adapter for RecyclerView
    private RecyclerView programCourseRecyclerView; // RecyclerView for program courses
    private List<ProgramCourse> programCourseList; // List for program course data
    private ProgramCourseAdapter programCourseAdapter; // Adapter for program RecyclerView
    private RecyclerView sectionRecyclerView;
    private CourseSectionAdapter courseSectionAdapter;
    private List<CourseSection> courseSectionList;

    public EnrolmentFragment() {
        // Required empty public constructor
    }

    public static EnrolmentFragment newInstance(String userId) {
        EnrolmentFragment fragment = new EnrolmentFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USER_ID, userId);
        fragment.setArguments(args);
        return fragment;
    }

    Button btnCS1;
    Button btnCS2;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (getArguments() != null) {
            userId = getArguments().getString(ARG_USER_ID);
        }

        // Initialize Firestore instance
        firestore = FirebaseFirestore.getInstance();

        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_enrolment, container, false);

        // Initialize course list and adapter for enrolment courses
        courseList = new ArrayList<>();
        courseAdapter = new EnrolmentCourseAdapter(courseList, course -> {
            // Handle item click for enrolment courses
            String courseID = course.getId();
            String courseName = course.getName();
            Toast.makeText(getContext(), "Clicked: " + courseID + " - " + courseName, Toast.LENGTH_SHORT).show();
        });


        // Initialize RecyclerView for enrolment courses
        enrolmentCourseRecyclerView = rootView.findViewById(R.id.enrolment_course_RecycleView);
        enrolmentCourseRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        enrolmentCourseRecyclerView.setAdapter(courseAdapter);

        // Initialize program course list and adapter
        programCourseList = new ArrayList<>();
        programCourseAdapter = new ProgramCourseAdapter(programCourseList, course -> {
            String courseID = course.getCourseID();
            String courseName = course.getCourseName();
            fetchCourseSection(courseID, courseName);
            Toast.makeText(getContext(), "Clicked: " + courseID + " - " + courseName, Toast.LENGTH_SHORT).show();

            // Update selected course TextViews
            TextView selectedCourseIdTextView = rootView.findViewById(R.id.selected_course_ID);
            selectedCourseIdTextView.setText(courseID);

            TextView selectedCourseNameTextView = rootView.findViewById(R.id.selected_course_name);
            selectedCourseNameTextView.setText(courseName);
        });

        // Initialize RecyclerView for program courses
        programCourseRecyclerView = rootView.findViewById(R.id.course_list_checkbox_RecyclerView);
        programCourseRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        programCourseRecyclerView.setAdapter(programCourseAdapter);

        courseSectionList = new ArrayList<>();
        courseSectionAdapter = new CourseSectionAdapter(courseSectionList);

        sectionRecyclerView = rootView.findViewById(R.id.course_section_info_RecyclerView);
        sectionRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        sectionRecyclerView.setAdapter(courseSectionAdapter);

        List<CourseSection> selectedCourseSectionList = new ArrayList<>();

        // Populate selectedCourseSectionList with the selected course data


        // Call function to fetch course document
        fetchStudentInfo(rootView);
        btnCS1 = rootView.findViewById(R.id.btnCS1);
        btnCS2 = rootView.findViewById(R.id.btnCS2);
        btnCS1.setVisibility(View.GONE);
        btnCS2.setVisibility(View.GONE);

        Button btnEnrol = rootView.findViewById(R.id.btnEnrol); // Assuming you have a button for enrollment


        // Set onClickListeners for the buttons
        btnCS1.setOnClickListener(v -> {
            Toast.makeText(getActivity(), "CS1 has been selected", Toast.LENGTH_SHORT).show();
        });

        btnCS2.setOnClickListener(v -> {
            Toast.makeText(getActivity(), "CS2 has been selected", Toast.LENGTH_SHORT).show();
        });

        btnEnrol.setOnClickListener(v -> {
            // Show a Toast with enrollment confirmation when the Enroll button is clicked
                String courseId = "5008ABC"; // Replace this with the actual course ID
                String enrollmentMessage = "Enrolled into " + courseId + " (CS1)";
                Toast.makeText(getActivity(), enrollmentMessage, Toast.LENGTH_SHORT).show();
        });

        return rootView;
    }

    private void fetchStudentInfo(View rootView) {
        firestore.collection("Students").document(userId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            Map<String, Object> data = document.getData();
                            if (data != null) {
                                String enrolledCourses = data.get("UpcomingCourse") != null ? data.get("UpcomingCourse").toString() : "N/A";
                                String programID = data.get("ProgrammeID") != null ? data.get("ProgrammeID").toString() : "N/A";

                                List<String> courseIds = Arrays.asList(enrolledCourses.split("\\s*,\\s*"));
                                courseIds.removeIf(String::isEmpty);  // Ensure non-empty course IDs

                                if (courseIds.isEmpty() || courseIds.contains("N/A")) {
                                    hasCourses = false;
                                    getActivity().runOnUiThread(() -> {
                                        TextView noCourseMessageTextView = rootView.findViewById(R.id.no_enrollment_course_message);
                                        noCourseMessageTextView.setVisibility(View.VISIBLE);
                                        enrolmentCourseRecyclerView.setVisibility(View.GONE);
                                    });
                                } else {
                                    enrolmentCourseRecyclerView.setVisibility(View.VISIBLE); // Make RecyclerView visible if courses are available
                                    fetchEnrolmentCourseNames(courseIds);  // Fetch course names and display them in RecyclerView
                                }

                                if (!"N/A".equals(programID)) {
                                    fetchProgrammeCourse(programID, rootView);
                                }
                            }
                        }
                    } else {
                        Log.e("EnrolmentFragment", "Error fetching student info", task.getException());
                    }
                });
    }

    private void fetchEnrolmentCourseNames(List<String> courseIds) {
        // Clear courseList to avoid duplicate entries
        courseList.clear();

        for (String courseId : courseIds) {
            firestore.collection("Courses").document(courseId).get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                Map<String, Object> data = document.getData();
                                if (data != null) {
                                    String courseName = data.get("Name") != null ? data.get("Name").toString() : "N/A";
                                    courseList.add(new EnrolmentCourse(courseId, courseName));
                                }
                            }
                        } else {
                            Log.e("EnrolmentFragment", "Error fetching course details for ID: " + courseId, task.getException());
                        }

                        // Update the adapter only once after all data is loaded
                        if (courseList.size() == courseIds.size()) {
                            courseAdapter.notifyDataSetChanged();  // Notify the adapter when all courses have been fetched
                        }
                    });
        }
    }

    private void fetchProgrammeCourse(String programID, View rootView) {
        firestore.collection("Courses")
                .whereEqualTo("ProgrammeID", programID) // Query for program courses
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            Map<String, String> programCourses = new HashMap<>();
                            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                                String courseID = document.getId(); // Document ID as courseID
                                String courseName = document.getString("Name");
                                if (courseName != null) {
                                    programCourses.put(courseID, courseName);
                                }
                            }
                            displayProgramCourses(programCourses,rootView);
                        } else {
                            Log.d("EnrolmentFragment", "No courses found for the given programmeID.");
                        }
                    } else {
                        Log.e("EnrolmentFragment", "Error fetching courses", task.getException());
                    }
                });
    }

    private void displayProgramCourses(Map<String, String> programCourses, View rootView) {
        if (programCourses.isEmpty()) {
            Log.d("EnrolmentFragment", "No program courses to display.");
            return;  // Exit early if there are no courses
        }

        // Update list without clearing prematurely
        programCourseList.clear();
        for (Map.Entry<String, String> entry : programCourses.entrySet()) {
            String courseID = entry.getKey();
            String courseName = entry.getValue();
            if (courseID != null && !courseID.isEmpty() && courseName != null && !courseName.isEmpty()) {
                programCourseList.add(new ProgramCourse(courseID, courseName));
            }

        }
        if (!programCourseList.isEmpty()) {
            programCourseAdapter.notifyDataSetChanged();
        } else {
            Log.d("EnrolmentFragment", "Program course list is still empty after updating.");
        }
    }

    private void fetchCourseSection(String courseID, String courseName) {
        courseSectionList.clear();
        courseSectionAdapter.notifyDataSetChanged();

        firestore.collection("Course99").document(courseID).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            Map<String, Object> data = document.getData();
                            if (data != null) {
                                String lectureDay = data.get("LectureDay") != null ? data.get("LectureDay").toString() : "N/A";
                                String lectureStartTime = data.get("LectureStartTime") != null ? data.get("LectureStartTime").toString() : "N/A";
                                String lectureEndTime = data.get("LectureEndTime") != null ? data.get("LectureEndTime").toString() : "N/A";
                                String practicalDay = data.get("PracticalDay") != null ? data.get("PracticalDay").toString() : "N/A";
                                String practicalStartTime = data.get("PracticalStartTime") != null ? data.get("PracticalStartTime").toString() : "N/A";
                                String practicalEndTime = data.get("PracticalEndTime") != null ? data.get("PracticalEndTime").toString() : "N/A";
                                String remark = data.get("Remarks") != null ? data.get("Remarks").toString() : "N/A";
                                String section = data.get("Section") != null ? data.get("Section").toString() : "N/A";

                                // Handle sections and times, split by comma
                                String[] sections = section.split(",");
                                String[] lectureStartTimes = lectureStartTime.split(",");
                                String[] lectureEndTimes = lectureEndTime.split(",");
                                String[] practicalStartTimes = practicalStartTime.split(",");
                                String[] practicalEndTimes = practicalEndTime.split(",");

                                // Ensure that if there's only one start/end time, it's used for all sections
                                String lectureStartTimeForAll = lectureStartTimes.length == 1 ? lectureStartTimes[0] : null;
                                String lectureEndTimeForAll = lectureEndTimes.length == 1 ? lectureEndTimes[0] : null;
                                String practicalStartTimeForAll = practicalStartTimes.length == 1 ? practicalStartTimes[0] : null;
                                String practicalEndTimeForAll = practicalEndTimes.length == 1 ? practicalEndTimes[0] : null;

                                for (int i = 0; i < sections.length; i++) {
                                    String currentSection = sections[i].trim();
                                    String currentLectureStartTime = lectureStartTimeForAll != null ? lectureStartTimeForAll : (i < lectureStartTimes.length ? lectureStartTimes[i].trim() : "N/A");
                                    String currentLectureEndTime = lectureEndTimeForAll != null ? lectureEndTimeForAll : (i < lectureEndTimes.length ? lectureEndTimes[i].trim() : "N/A");
                                    String currentPracticalStartTime = practicalStartTimeForAll != null ? practicalStartTimeForAll : (i < practicalStartTimes.length ? practicalStartTimes[i].trim() : "N/A");
                                    String currentPracticalEndTime = practicalEndTimeForAll != null ? practicalEndTimeForAll : (i < practicalEndTimes.length ? practicalEndTimes[i].trim() : "N/A");

                                    // Create a CourseSection for each section
                                    CourseSection courseSection = new CourseSection( courseID, courseName,
                                            lectureDay, currentLectureStartTime, currentLectureEndTime,
                                            practicalDay, currentPracticalStartTime, currentPracticalEndTime,
                                            remark, currentSection
                                    );

                                    // Add to the courseSectionList and notify adapter
                                    courseSectionList.add(courseSection);
                                }

                                btnCS1.setVisibility(View.VISIBLE);
                                btnCS2.setVisibility(View.VISIBLE);

                                courseSectionAdapter.notifyDataSetChanged();
                            }
                        }
                    }
                });
    }
}




