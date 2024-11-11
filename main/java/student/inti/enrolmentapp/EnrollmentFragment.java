package student.inti.enrolmentapp;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.TextView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;

public class EnrollmentFragment extends Fragment {
    private static final String ARG_USER_ID = "userId";
    private RecyclerView enrolmentRecyclerView, courseListRecyclerView;
    private FirebaseFirestore db;
    private String userId;
    private ArrayList<String> upcomingCourses;  // List of upcoming course IDs
    private ArrayList<Course> courseList;  // All available courses for the program
    private ArrayList<Course> filteredCourses;  // Filtered courses that are in the upcomingCourses list
    private ArrayList<Course> selectedUpcomingCourses;  // Courses from the UpcomingCourse list
    private ArrayList<CourseSection> selectedCourseSections;
    private TextView courseDetailsTextView;

    public EnrollmentFragment() {
        // Required empty public constructor
    }

    public static EnrollmentFragment newInstance(String userId) {
        EnrollmentFragment fragment = new EnrollmentFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USER_ID, userId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        upcomingCourses = new ArrayList<>();
        courseList = new ArrayList<>();
        filteredCourses = new ArrayList<>();
        selectedUpcomingCourses = new ArrayList<>();
        selectedCourseSections = new ArrayList<>();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (getArguments() != null) {
            userId = getArguments().getString(ARG_USER_ID);
        }

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_enrollment, container, false);

        enrolmentRecyclerView = view.findViewById(R.id.enrolment_course_RecycleView);
        courseListRecyclerView = view.findViewById(R.id.course_list_checkbox_RecyclerView);
        courseDetailsTextView = view.findViewById(R.id.course_details_text_view); // Initialize TextView

        // Apply hex color to course details text view
        courseDetailsTextView.setTextColor(Color.parseColor("#FF5733")); // Example hex color

        enrolmentRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        courseListRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Fetch upcoming courses from Firestore
        db.collection("Students").document(userId).get().addOnSuccessListener(documentSnapshot -> {
            if (isAdded() && documentSnapshot.exists()) {
                Object upcomingCoursesData = documentSnapshot.get("UpcomingCourse");
                if (upcomingCoursesData != null) {
                    if (upcomingCoursesData instanceof ArrayList) {
                        upcomingCourses = (ArrayList<String>) upcomingCoursesData;
                    } else if (upcomingCoursesData instanceof String) {
                        String[] coursesArray = ((String) upcomingCoursesData).split(",\\s*");
                        upcomingCourses = new ArrayList<>();
                        Collections.addAll(upcomingCourses, coursesArray);
                    }
                } else {
                    upcomingCourses = new ArrayList<>();
                }

                loadCourses(); // Load courses after verifying upcoming courses
            }
        }).addOnFailureListener(e -> {
            Log.e("EnrollmentFragment", "Error fetching student data", e);
        });

        return view;
    }

    private void loadCourses() {
        courseList.clear(); // Clear any existing data

        // Fetch ProgrammeID from Students collection
        db.collection("Students").document(userId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String programmeId = documentSnapshot.getString("ProgrammeID");

                if (programmeId != null) {
                    fetchCoursesInProgramme(programmeId);
                }
            }
        }).addOnFailureListener(e -> {
            Log.e("loadCourses", "Error fetching student data", e);
        });
    }

    private void fetchCoursesInProgramme(String programmeId) {
        db.collection("Courses")
                .whereEqualTo("ProgrammeID", programmeId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        // Clear filteredCourses to ensure it only contains the relevant courses
                        filteredCourses.clear();
                        for (QueryDocumentSnapshot courseDoc : querySnapshot) {
                            String courseName = courseDoc.getString("Name");
                            String courseId = courseDoc.getId(); // Document ID as course ID

                            if (courseName != null) {
                                // Add all courses with the matching ProgrammeID to the filtered list
                                Course course = new Course(courseName, courseId, "", "", "", "", "", "", "", "");
                                filteredCourses.add(course);
                            }
                        }
                        // Now populate the RecyclerViews
                        populateRecyclerViews();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("fetchCoursesInProgramme", "Error fetching courses for ProgrammeID: " + programmeId, e);
                });
    }

    private void populateRecyclerViews() {
        // Show courses the student is enrolled in (UpcomingCourse)
        selectedUpcomingCourses.clear();
        for (String courseId : upcomingCourses) {
            for (Course course : filteredCourses) {
                if (course.getId().equals(courseId)) {
                    selectedUpcomingCourses.add(course);
                }
            }
        }

        // Set up the RecyclerView for enrolled courses
        EnrollmentAdapter enrolledAdapter = new EnrollmentAdapter(selectedUpcomingCourses);
        enrolmentRecyclerView.setAdapter(enrolledAdapter);

        // Set up the RecyclerView for all courses in the program
        EnrollmentAdapter allCoursesAdapter = new EnrollmentAdapter(filteredCourses);
        courseListRecyclerView.setAdapter(allCoursesAdapter);
    }

    private void showCourseSelection(Course course) {
        // Start with the basic course information
        String courseDetails = "ID: " + course.getId() + "\n" + "Name: " + course.getName() + "\n";

        // Initialize Firestore
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Query to get section details from Firestore (assuming "Sections" is a collection in your Firestore DB)
        db.collection("Courses")
                .document(course.getId())
                .collection("Sections") // Assuming each course has a "Sections" subcollection
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    // StringBuilder to hold the section-specific details
                    StringBuilder sectionDetails = new StringBuilder();

                    // Iterate through each section document retrieved
                    for (QueryDocumentSnapshot sectionDoc : querySnapshot) {
                        String sectionName = sectionDoc.getString("sectionName");
                        String lectureDay = sectionDoc.getString("lectureDay");
                        String lectureStartTime = sectionDoc.getString("lectureStartTime");
                        String lectureEndTime = sectionDoc.getString("lectureEndTime");
                        String practicalDay = sectionDoc.getString("practicalDay");
                        String practicalStartTime = sectionDoc.getString("practicalStartTime");
                        String practicalEndTime = sectionDoc.getString("practicalEndTime");
                        String remarks = sectionDoc.getString("remarks");

                        // Append the section details to the StringBuilder
                        sectionDetails.append("Section: ").append(sectionName).append("\n")
                                .append("Lecture Day: ").append(lectureDay).append("\n")
                                .append("Lecture Time: ").append(lectureStartTime).append(" - ").append(lectureEndTime).append("\n")
                                .append("Practical Day: ").append(practicalDay).append("\n")
                                .append("Practical Time: ").append(practicalStartTime).append(" - ").append(practicalEndTime).append("\n")
                                .append("Remarks: ").append(remarks).append("\n\n");
                    }

                    // Set the detailed course information in the TextView
                    courseDetailsTextView.setText(courseDetails + sectionDetails.toString());
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore Error", "Error fetching section details", e);
                    // In case of error, set a fallback message
                    courseDetailsTextView.setText("Failed to load section details.");
                });
    }

// Helper methods to extract specific details

    // Extracts the lecture day for the given section from the comma-separated list
    private String extractDay(String daysData, String section) {
        // Days data is in format: "Monday (CS1), Tuesday (CS2)"
        String[] daysArray = daysData.split(", ");
        for (String dayData : daysArray) {
            if (dayData.contains(section)) {
                return dayData.split(" ")[0]; // Split and return the day part (e.g., "Monday")
            }
        }
        return "Unknown";
    }

    // Extracts the time for the given section from the comma-separated list
    private String extractTime(String timesData, String section) {
        // Times data is in format: "8 (CS1), 10 (CS2)"
        String[] timesArray = timesData.split(", ");
        for (String timeData : timesArray) {
            if (timeData.contains(section)) {
                return timeData.split(" ")[0]; // Split and return the time part (e.g., "8")
            }
        }
        return "Unknown";
    }

    // Extracts the remarks for the given section (if any)
    private String extractRemarks(String remarksData, String section) {
        // Remarks data is in format: "- (CS1), - (CS2)"
        String[] remarksArray = remarksData.split(", ");
        for (String remark : remarksArray) {
            if (remark.contains(section)) {
                return remark.split(" ")[0]; // In this case, there seems to be no specific remark, so just return "-"
            }
        }
        return "-"; // Default remark if no specific remark found
    }

    private class EnrollmentAdapter extends RecyclerView.Adapter<EnrollmentAdapter.ViewHolder> {
        private ArrayList<Course> courses;

        EnrollmentAdapter(ArrayList<Course> courses) {
            this.courses = courses;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Course course = courses.get(position);
            holder.courseIdTextView.setText(course.getId());
            holder.courseNameTextView.setText(course.getName());
        }

        @Override
        public int getItemCount() {
            return courses.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView courseIdTextView;
            TextView courseNameTextView;

            ViewHolder(View itemView) {
                super(itemView);
                courseIdTextView = itemView.findViewById(android.R.id.text1);
                courseNameTextView = itemView.findViewById(android.R.id.text2);
                itemView.setOnClickListener(v -> showCourseSelection(courses.get(getAdapterPosition())));
            }
        }
    }

    public static class CourseSection {
        private String sectionName;
        private String lecturerId;
        private String lectureLocation;
        private String lectureDay;
        private String lectureStartTime;
        private String lectureEndTime;
        private String practicalDay;
        private String practicalStartTime;
        private String practicalEndTime;
        private String remarks;

        public CourseSection(String sectionName, String lecturerId, String lectureLocation, String lectureDay,
                             String lectureStartTime, String lectureEndTime, String practicalDay,
                             String practicalStartTime, String practicalEndTime, String remarks) {
            this.sectionName = sectionName;
            this.lecturerId = lecturerId;
            this.lectureLocation = lectureLocation;
            this.lectureDay = lectureDay;
            this.lectureStartTime = lectureStartTime;
            this.lectureEndTime = lectureEndTime;
            this.practicalDay = practicalDay;
            this.practicalStartTime = practicalStartTime;
            this.practicalEndTime = practicalEndTime;
            this.remarks = remarks;
        }

        public String getSectionName() {
            return sectionName;
        }

        public String getLecturerId() {
            return lecturerId;
        }

        public String getLectureLocation() {
            return lectureLocation;
        }

        public String getLectureDay() {
            return lectureDay;
        }

        public String getLectureStartTime() {
            return lectureStartTime;
        }

        public String getLectureEndTime() {
            return lectureEndTime;
        }

        public String getPracticalDay() {
            return practicalDay;
        }

        public String getPracticalStartTime() {
            return practicalStartTime;
        }

        public String getPracticalEndTime() {
            return practicalEndTime;
        }

        public String getRemarks() {
            return remarks;
        }
    }

    public static class Course {
        private String id;
        private String name;
        private String sections; // Comma-separated list of sections (e.g., "CS1, CS2")
        private String lectureDays; // Comma-separated list of lecture days (e.g., "Monday (CS1), Tuesday (CS2)")
        private String lectureStartTimes; // Comma-separated list of lecture start times (e.g., "8 (CS1), 10 (CS2)")
        private String lectureEndTimes; // Comma-separated list of lecture end times (e.g., "10 (CS1), 12 (CS2)")
        private String practicalDays; // Comma-separated list of practical days (e.g., "Wednesday (CS1), Thursday (CS2)")
        private String practicalStartTimes; // Comma-separated list of practical start times (e.g., "11 (CS1), 12 (CS2)")
        private String practicalEndTimes; // Comma-separated list of practical end times (e.g., "13 (CS1), 14 (CS2)")
        private String remarks; // Comma-separated list of remarks (e.g., "- (CS1), - (CS2)")

        // Constructor
        public Course(String id, String name, String sections, String lectureDays, String lectureStartTimes,
                      String lectureEndTimes, String practicalDays, String practicalStartTimes,
                      String practicalEndTimes, String remarks) {
            this.id = id;
            this.name = name;
            this.sections = sections;
            this.lectureDays = lectureDays;
            this.lectureStartTimes = lectureStartTimes;
            this.lectureEndTimes = lectureEndTimes;
            this.practicalDays = practicalDays;
            this.practicalStartTimes = practicalStartTimes;
            this.practicalEndTimes = practicalEndTimes;
            this.remarks = remarks;
        }

        // Getter methods
        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getSections() {
            return sections;
        }

        public String getLectureDays() {
            return lectureDays;
        }

        public String getLectureStartTimes() {
            return lectureStartTimes;
        }

        public String getLectureEndTimes() {
            return lectureEndTimes;
        }

        public String getPracticalDays() {
            return practicalDays;
        }

        public String getPracticalStartTimes() {
            return practicalStartTimes;
        }

        public String getPracticalEndTimes() {
            return practicalEndTimes;
        }

        public String getRemarks() {
            return remarks;
        }
    }
}
