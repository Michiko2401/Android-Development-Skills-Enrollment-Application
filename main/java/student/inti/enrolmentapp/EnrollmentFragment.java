package student.inti.enrolmentapp;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Gravity;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.GridLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class EnrollmentFragment extends Fragment {

    private static final String ARG_USER_ID = "userId";
    private String userId;
    private String programmeId;
    private FirebaseFirestore db;
    private CourseListAdapter adapter;
    private LinearLayout courseSelectionLayout;  // To hold the course selection details
    private Button enrollButton;

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
        if (getArguments() != null) {
            userId = getArguments().getString(ARG_USER_ID);
        }
        db = FirebaseFirestore.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_enrollment, container, false);

        TableLayout enrollPlanTable = view.findViewById(R.id.enrollPlanTable);
        populateEnrollmentPlan(enrollPlanTable);

        RecyclerView courseListRecyclerView = view.findViewById(R.id.courseListRecyclerView);
        courseListRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        courseListRecyclerView.setVerticalScrollBarEnabled(true);

        adapter = new CourseListAdapter(new ArrayList<>(), (courseId, isChecked) -> onCourseClick(courseId, isChecked));

        courseListRecyclerView.setAdapter(adapter);

        courseSelectionLayout = view.findViewById(R.id.courseSelectionLayout);
        courseSelectionLayout.setVisibility(View.GONE); // Initially hidden

        populateCourseList();

        // Enroll button initialization
        enrollButton = view.findViewById(R.id.btnEnrol);
        enrollButton.setOnClickListener(v -> handleEnrollment()); // Set the click listener for the Enroll button

        return view;
    }

    private void populateEnrollmentPlan(TableLayout table) {
        // Clear any previous data
        table.removeAllViews();

        db.collection("Students").document(userId).get().addOnSuccessListener(studentDoc -> {
            if (studentDoc.exists() && studentDoc.contains("UpcomingCourse")) {
                String upcomingCourses = studentDoc.getString("UpcomingCourse");
                if (upcomingCourses != null && !upcomingCourses.isEmpty()) {
                    String[] courseIds = upcomingCourses.split(", ");
                    AtomicInteger count = new AtomicInteger(1); // Start the count from 1

                    for (String courseId : courseIds) {
                        db.collection("Courses").document(courseId).get().addOnSuccessListener(courseDoc -> {
                            if (courseDoc.exists()) {
                                String courseName = courseDoc.getString("Name");

                                TableRow row = new TableRow(getContext());
                                row.setLayoutParams(new TableRow.LayoutParams(
                                        TableRow.LayoutParams.MATCH_PARENT,
                                        TableRow.LayoutParams.WRAP_CONTENT));

                                // Count Column
                                TextView countCell = new TextView(getContext());
                                countCell.setText(String.valueOf(count.getAndIncrement()));
                                countCell.setTextColor(0xFF6393A8);
                                countCell.setPadding(8, 8, 8, 8);

                                // Course ID Column
                                TextView courseIdCell = new TextView(getContext());
                                courseIdCell.setText(courseId);
                                courseIdCell.setPadding(8, 8, 8, 8);

                                // Course Name Column
                                TextView courseNameCell = new TextView(getContext());
                                courseNameCell.setText(courseName);
                                courseNameCell.setPadding(8, 8, 8, 8);

                                row.addView(countCell);
                                row.addView(courseIdCell);
                                row.addView(courseNameCell);

                                table.addView(row);
                            }
                        });
                    }
                } else {
                    // If no upcoming courses or the string is empty, show the "No upcoming courses" message
                    showNoUpcomingCoursesMessage(table);
                }
            } else {
                // If the "UpcomingCourse" field does not exist in the document, show the message
                showNoUpcomingCoursesMessage(table);
            }
        });
    }

    private void showNoUpcomingCoursesMessage(TableLayout table) {
        TableRow row = new TableRow(getContext());
        row.setLayoutParams(new TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT));

        TextView messageCell = new TextView(getContext());
        messageCell.setText("No upcoming courses to enroll in");
        messageCell.setTextColor(0xFFB0B0B0);  // Light grey color
        messageCell.setPadding(16, 16, 16, 16);
        messageCell.setTextSize(16);
        messageCell.setGravity(Gravity.CENTER);

        row.addView(messageCell);
        table.addView(row);
    }

    private void populateCourseList() {
        db.collection("Students").document(userId).get().addOnSuccessListener(studentDoc -> {
            if (studentDoc.exists() && studentDoc.contains("ProgrammeID")) {
                String studentProgrammeId = studentDoc.getString("ProgrammeID");
                programmeId = studentProgrammeId;

                // Query all courses, we'll handle the matching logic later
                db.collection("Courses").get().addOnSuccessListener(courses -> {
                    ArrayList<String> courseData = new ArrayList<>();

                    for (com.google.firebase.firestore.QueryDocumentSnapshot courseDoc : courses) {
                        String courseProgrammeId = courseDoc.getString("ProgrammeID");
                        String courseName = courseDoc.getString("Name");
                        String courseId = courseDoc.getId();

                        // Show the course if the ProgrammeID contains the student's ProgrammeID or if ProgrammeID is "-"
                        if (courseProgrammeId != null &&
                                (courseProgrammeId.contains(studentProgrammeId) || courseProgrammeId.equals("-"))) {
                            String courseDisplay = courseId + " - " + courseName;
                            courseData.add(courseDisplay);
                            Log.d("EnrollData", courseDisplay);
                        }
                    }

                    // Update the RecyclerView with the populated course list
                    adapter.updateCourseList(courseData);
                });
            }
        });
    }

    // Inner RecyclerView Adapter Class
    public static class CourseListAdapter extends RecyclerView.Adapter<CourseListAdapter.CourseViewHolder> {

        private ArrayList<String> courseList;
        private OnCourseClickListener onCourseClickListener;
        private Map<String, Boolean> selectedCourses = new HashMap<>(); // To track selected courses by courseId

        public interface OnCourseClickListener {
            void onCourseClick(String courseId, boolean isChecked);
        }

        public CourseListAdapter(ArrayList<String> courseList, OnCourseClickListener onCourseClickListener) {
            this.courseList = courseList;
            this.onCourseClickListener = onCourseClickListener;
        }

        @NonNull
        @Override
        public CourseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            Context context = parent.getContext();
            if (context == null) {
                Log.e("CourseListAdapter", "Parent context is null");
                return null;
            }

            TableRow tableRow = new TableRow(context);
            tableRow.setPadding(16, 16, 0, 16);
            tableRow.setLayoutParams(new TableRow.LayoutParams(
                    TableRow.LayoutParams.MATCH_PARENT,
                    TableRow.LayoutParams.WRAP_CONTENT));

            TextView courseIdTextView = new TextView(context);
            courseIdTextView.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f));
            courseIdTextView.setGravity(Gravity.CENTER_VERTICAL);
            courseIdTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            courseIdTextView.setPadding(8, 8, 8, 8);
            tableRow.addView(courseIdTextView);

            TextView courseNameTextView = new TextView(context);
            courseNameTextView.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 2f));
            courseNameTextView.setGravity(Gravity.CENTER_VERTICAL);
            courseNameTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            courseNameTextView.setPadding(8, 8, 8, 8);
            tableRow.addView(courseNameTextView);

            CheckBox courseCheckBox = new CheckBox(context);
            TableRow.LayoutParams checkBoxParams = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f);
            checkBoxParams.gravity = Gravity.CENTER_VERTICAL;
            courseCheckBox.setLayoutParams(checkBoxParams);
            courseCheckBox.setGravity(Gravity.CENTER_VERTICAL);
            tableRow.addView(courseCheckBox);

            return new CourseViewHolder(tableRow, courseIdTextView, courseNameTextView, courseCheckBox);
        }

        @Override
        public void onBindViewHolder(@NonNull CourseViewHolder holder, int position) {
            String course = courseList.get(position);
            String[] parts = course.split(" - ");
            String courseId = parts[0];
            String courseName = parts.length > 1 ? parts[1] : "";

            holder.courseIdTextView.setText(courseId);
            holder.courseNameTextView.setText(courseName);

            // Set CheckBox state based on stored selection
            holder.courseCheckBox.setOnCheckedChangeListener(null); // Clear previous listener
            holder.courseCheckBox.setChecked(selectedCourses.getOrDefault(courseId, false));

            // Set up the CheckBox listener to manage selection changes
            holder.courseCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                selectedCourses.put(courseId, isChecked); // Update selected state in the map
                onCourseClickListener.onCourseClick(courseId, isChecked);
            });
        }

        @Override
        public int getItemCount() {
            return courseList.size();
        }

        public void updateCourseList(ArrayList<String> courseList) {
            this.courseList = courseList;
            notifyDataSetChanged();
        }

        static class CourseViewHolder extends RecyclerView.ViewHolder {
            TextView courseIdTextView;
            TextView courseNameTextView;
            CheckBox courseCheckBox;

            public CourseViewHolder(@NonNull View itemView, TextView courseIdTextView, TextView courseNameTextView, CheckBox courseCheckBox) {
                super(itemView);
                this.courseIdTextView = courseIdTextView;
                this.courseNameTextView = courseNameTextView;
                this.courseCheckBox = courseCheckBox;
            }
        }
    }

    // Handle click on course item
    private Map<String, CourseSchedule> selectedCourses = new HashMap<>();
    private Map<String, RadioButton> selectedRadioButtons = new HashMap<>(); // To store the selected radio button per course

    private void onCourseClick(String courseId, boolean isChecked) {
        TableLayout courseDetailsContainer = getView() != null ? getView().findViewById(R.id.courseDetailsContainer) : null;

        if (courseDetailsContainer == null) {
            Log.e("EnrollData", "Course Details Container not found.");
            return;
        }

        if (isChecked) {
            Log.d("EnrollData", "Checkbox for " + courseId + " checked.");

            db.collection("Courses").document(courseId).get().addOnSuccessListener(courseDoc -> {
                if (courseDoc.exists()) {
                    String courseName = courseDoc.getString("Name");
                    String sections = courseDoc.getString("Sections");
                    String lectureDays = courseDoc.getString("LectureDays");
                    String lectureStartTimes = courseDoc.getString("LectureStartTimes");
                    String lectureEndTimes = courseDoc.getString("LectureEndTimes");
                    String practicalDays = courseDoc.getString("PracticalDays");
                    String practicalStartTimes = courseDoc.getString("PracticalStartTimes");
                    String practicalEndTimes = courseDoc.getString("PracticalEndTimes");
                    String remarks = courseDoc.getString("Remarks");

                    // Split sections and relevant information into arrays
                    String[] sectionsArray = sections.split(", ");
                    String[] lectureDaysArray = lectureDays.split(", ");
                    String[] lectureStartTimesArray = lectureStartTimes.split(", ");
                    String[] lectureEndTimesArray = lectureEndTimes.split(", ");
                    String[] practicalDaysArray = practicalDays.split(", ");
                    String[] practicalStartTimesArray = practicalStartTimes.split(", ");
                    String[] practicalEndTimesArray = practicalEndTimes.split(", ");

                    // Show the course name
                    courseSelectionLayout.setVisibility(View.VISIBLE);

                    final String[] sectionId = {""};

                    // Query the Programmes collection to get SectionID
                    db.collection("Programmes").document(programmeId).get().addOnSuccessListener(programmeDoc -> {
                        if (programmeDoc.exists()) {
                            sectionId[0] = programmeDoc.getString("SectionID");
                        }

                        // Prepare to dynamically display sections details
                        StringBuilder sectionsDetails = new StringBuilder();

                        // Add the course ID and name before the course details
                        sectionsDetails.append("\n").append(courseId).append("\n");
                        sectionsDetails.append(courseName).append("\n\n");

                        // Create a new TableRow for the course details
                        TableRow row = new TableRow(getContext());
                        row.setTag(courseId);  // Set a unique tag for the course to identify it later
                        String lectureDaysAndTimes = "";
                        String practicalDaysAndTimes = "";
                        // Loop through each section in the course
                        // Loop through each section in the course
                        for (int i = 0; i < sectionsArray.length; i++) {
                            String section = sectionsArray[i].trim(); // Trim the section string to remove any leading/trailing whitespace

                            // Check if the section starts with the sectionId
                            if (section.startsWith(sectionId[0])) {
                                sectionsDetails.append("Section ").append(section).append("\n");

                                // Reset lectureDaysAndTimes and practicalDaysAndTimes for each section to avoid duplication
                                 lectureDaysAndTimes = ""; // Reset this variable for each section
                                 practicalDaysAndTimes = ""; // Reset this variable for each section

                                // Display lecture details
                                if (i < lectureDaysArray.length && i < lectureStartTimesArray.length && i < lectureEndTimesArray.length) {
                                    lectureDaysAndTimes += "Lecture Day(s): " + extractTimeLocation(lectureDaysArray[i]) + "\n";
                                    lectureDaysAndTimes += "Lecture Time(s): " + extractTimeLocation(lectureStartTimesArray[i]) + " to "
                                            + extractTimeLocation(lectureEndTimesArray[i]) + "\n";
                                }

                                // Display practical details only if available
                                if (i < practicalDaysArray.length && i < practicalStartTimesArray.length && i < practicalEndTimesArray.length) {
                                    if (!"-".equals(practicalDaysArray[i])) {
                                        practicalDaysAndTimes += "Practical Day(s): " + extractTimeLocation(practicalDaysArray[i]) + "\n";
                                        practicalDaysAndTimes += "Practical Time(s): " + extractTimeLocation(practicalStartTimesArray[i]) + " to "
                                                + extractTimeLocation(practicalEndTimesArray[i]) + "\n";
                                    }
                                }

                                // Append the details to sectionsDetails
                                sectionsDetails.append(lectureDaysAndTimes);
                                sectionsDetails.append(practicalDaysAndTimes);
                                sectionsDetails.append("Remarks: ").append(remarks != null ? remarks : "-").append("\n\n");
                            }
                        }

                        // Create a new TextView for displaying course details and add it to the row
                        TextView courseDetailsTextView = new TextView(getContext());
                        courseDetailsTextView.setText(sectionsDetails.toString());
                        row.addView(courseDetailsTextView);  // Add to the TableRow

                        // Add the TableRow to the TableLayout
                        courseDetailsContainer.addView(row);

                        // Dynamically create radio buttons for sections (after course details)
                        if (sectionsArray.length >= 1) {
                            // Display the message for multiple sections
                            TextView selectSectionMessage = new TextView(getContext());
                            selectSectionMessage.setText("Select a section to proceed");
                            selectSectionMessage.setVisibility(View.VISIBLE);

                            TableLayout tableLayout = new TableLayout(getContext()); // Create new TableLayout for radio buttons
                            tableLayout.setTag(courseId); // Set a unique tag for the TableLayout

                            TableRow currentRow = null; // To hold the current row of radio buttons
                            int columnCount = 0; // To track the number of radio buttons in the current row

                            // Dynamically create radio buttons for each section
                            for (int i = 0; i < sectionsArray.length; i++) {
                                String section = sectionsArray[i];

                                // Only create radio button if the section starts with the sectionId
                                if (!"-".equals(section) && section.startsWith(sectionId[0])) {
                                    if (columnCount == 4) {
                                        // If the row has 4 radio buttons, create a new row
                                        currentRow = new TableRow(getContext());
                                        tableLayout.addView(currentRow);
                                        columnCount = 0; // Reset column count for the new row
                                    }

                                    RadioButton radioButton = new RadioButton(getContext());
                                    radioButton.setText(section);
                                    radioButton.setId(View.generateViewId()); // Generate unique ID for each radio button
                                    radioButton.setTag(section); // Set a tag for the section

                                    // Add an OnClickListener to manually handle the selection logic
                                    String finalLectureDaysAndTimes = lectureDaysAndTimes;
                                    String finalPracticalDaysAndTimes = practicalDaysAndTimes;
                                    radioButton.setOnClickListener(v -> {
                                        // Check if there's a previously selected radio button for this course
                                        RadioButton previousSelectedButton = selectedRadioButtons.get(courseId);
                                        if (previousSelectedButton != null) {
                                            // Deselect the previously selected radio button
                                            previousSelectedButton.setChecked(false);
                                        }

                                        // Store the current selected radio button
                                        selectedRadioButtons.put(courseId, radioButton);

                                        // Check for clashes with the selected section
                                        String selectedSection = section;
                                        String selectedDaysAndTimes = finalLectureDaysAndTimes + finalPracticalDaysAndTimes;

                                        // Store the selected section information
                                        CourseSchedule selectedCourse = new CourseSchedule(courseId, selectedSection, selectedDaysAndTimes);

                                        // Store the selected course
                                        selectedCourses.put(courseId + selectedSection, selectedCourse);

                                        // Show a Toast message indicating the section has been selected
                                        Toast.makeText(getContext(), section + " selected", Toast.LENGTH_SHORT).show();
                                    });

                                    // Add the radio button to the current row
                                    if (currentRow == null) {
                                        currentRow = new TableRow(getContext());
                                        tableLayout.addView(currentRow); // Add the first row
                                    }
                                    currentRow.addView(radioButton); // Add radio button to current row
                                    columnCount++; // Increment the column count
                                }
                            }

                            // Add the select section message and the tableLayout to the layout
                            courseDetailsContainer.addView(selectSectionMessage);
                            courseDetailsContainer.addView(tableLayout); // Add the TableLayout with radio buttons

                            // Create and add the line of underscores
                            TextView lineSeparator = new TextView(getContext());
                            lineSeparator.setText("______________________________________________");
                            courseDetailsContainer.addView(lineSeparator);
                        }
                    });
                }
            });
        } else {
            Log.d("EnrollData", "Checkbox for " + courseId + " unchecked.");

            // Remove the course details row if unchecked
            if (courseDetailsContainer != null) {
                View courseView = courseDetailsContainer.findViewWithTag(courseId);
                if (courseView != null) {
                    courseDetailsContainer.removeView(courseView);
                }

                // Also remove the dynamically added TableLayout (radio buttons)
                for (int i = 0; i < courseDetailsContainer.getChildCount(); i++) {
                    View childView = courseDetailsContainer.getChildAt(i);
                    if (childView instanceof TableLayout) {
                        TableLayout radioButtonLayout = (TableLayout) childView;
                        if (radioButtonLayout.getTag() != null && radioButtonLayout.getTag().equals(courseId)) {
                            courseDetailsContainer.removeView(radioButtonLayout);
                        }
                    }
                }

                // Remove selectSectionMessage and lineSeparator if present
                for (int i = 0; i < courseDetailsContainer.getChildCount(); i++) {
                    View childView = courseDetailsContainer.getChildAt(i);
                    if (childView instanceof TextView) {
                        String text = ((TextView) childView).getText().toString();
                        if ("Select a section to proceed".equals(text) || "______________________________________________".equals(text)) {
                            courseDetailsContainer.removeView(childView);
                        }
                    }
                }
            }
        }
    }

    // Helper class to store course schedule information
    private static class CourseSchedule {
        String courseName;
        String section;
        String daysAndTimes;

        CourseSchedule(String courseName, String section, String daysAndTimes) {
            this.courseName = courseName;
            this.section = section;
            this.daysAndTimes = daysAndTimes;
        }
    }

    // Handle Enroll button click
    private void handleEnrollment() {
        List<CourseSchedule> selectedCourseList = new ArrayList<>();
        StringBuilder enrolledCourseDetails = new StringBuilder();
        boolean hasClashes = false;
        int checkedCoursesCount = 0;
        int expectedCheckedCourses = 0;

        // Collect selected courses and their associated radio buttons
        for (Map.Entry<String, RadioButton> entry : selectedRadioButtons.entrySet()) {
            String courseId = entry.getKey();
            RadioButton radioButton = entry.getValue();

            // Ensure radio button is selected for every course
            if (radioButton != null && radioButton.isChecked()) {
                String sectionTag = (String) radioButton.getTag();
                if (sectionTag != null && !sectionTag.isEmpty()) {
                    expectedCheckedCourses++;
                    CourseSchedule selectedCourse = selectedCourses.get(courseId + sectionTag);
                    if (selectedCourse != null) {
                        selectedCourseList.add(selectedCourse);
                        enrolledCourseDetails.append(courseId).append(" (").append(sectionTag).append("), ");
                        checkedCoursesCount++;
                    }
                }
            }
        }

        // Verify if all courses have selected sections
        if (checkedCoursesCount != expectedCheckedCourses) {
            Toast.makeText(getContext(), "Please select a section for each course to enroll.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check for schedule clashes
        if (selectedCourseList.size() > 1) {
            for (int i = 0; i < selectedCourseList.size(); i++) {
                for (int j = i + 1; j < selectedCourseList.size(); j++) {
                    CourseSchedule course1 = selectedCourseList.get(i);
                    CourseSchedule course2 = selectedCourseList.get(j);
                    if (hasScheduleClash(course1, course2)) {
                        Toast.makeText(getContext(), "Schedule clash between " + course1.courseName + " and " + course2.courseName, Toast.LENGTH_LONG).show();
                        hasClashes = true;
                        break;
                    }
                }
                if (hasClashes) break;
            }
        }

        if (!hasClashes && !selectedCourseList.isEmpty()) {
            enrolledCourseDetails.setLength(enrolledCourseDetails.length() - 2); // Remove last comma and space

            AlertDialog dialog = new AlertDialog.Builder(getContext())
                    .setTitle("Confirm Enrollment")
                    .setMessage("Enroll into these courses?\n" + enrolledCourseDetails.toString())
                    .setPositiveButton("Yes", (d, which) -> enrollCourses(selectedCourseList, enrolledCourseDetails))
                    .setNegativeButton("No", null)
                    .show();

            // Customize the "No" button
            Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            negativeButton.setTextColor(Color.parseColor("#FFFF0000"));

        } else if (selectedCourseList.isEmpty()) {
            Toast.makeText(getContext(), "Please select at least one course and section.", Toast.LENGTH_SHORT).show();
        }
    }

    private void enrollCourses(List<CourseSchedule> selectedCourseList, StringBuilder enrolledCourseDetails) {
        List<String> enrolledCourses = new ArrayList<>();
        for (CourseSchedule course : selectedCourseList) {
            enrolledCourses.add(course.courseName + " (" + course.section + ")");
        }

        String studentId = userId; // Replace with actual student ID retrieval logic
        db.collection("Students").document(studentId).get().addOnSuccessListener(studentDoc -> {
            if (studentDoc.exists()) {
                String currentEnrolledCourses = studentDoc.getString("EnrolledCourse");
                String upcomingCourses = studentDoc.getString("UpcomingCourse");

                Map<String, String> enrolledCourseMap = new HashMap<>();
                if (currentEnrolledCourses != null) {
                    for (String enrolledCourse : currentEnrolledCourses.split(", ")) {
                        String[] parts = enrolledCourse.split(" \\(");
                        if (parts.length == 2) {
                            enrolledCourseMap.put(parts[0], parts[1].replace(")", ""));
                        }
                    }
                }

                for (CourseSchedule course : selectedCourseList) {
                    if (enrolledCourseMap.containsKey(course.courseName)) {
                        if (!enrolledCourseMap.get(course.courseName).equals(course.section)) {
                            enrolledCourseMap.put(course.courseName, course.section);
                        }
                    } else {
                        enrolledCourseMap.put(course.courseName, course.section);
                    }
                }

                StringBuilder updatedUpcomingCourses = new StringBuilder();
                if (upcomingCourses != null) {
                    String[] upcomingArray = upcomingCourses.split(", ");
                    for (String course : upcomingArray) {
                        if (!enrolledCourseMap.containsKey(course.split(" \\(")[0])) {
                            updatedUpcomingCourses.append(course).append(", ");
                        }
                    }
                }

                StringBuilder enrolledCourseString = new StringBuilder();
                for (Map.Entry<String, String> entry : enrolledCourseMap.entrySet()) {
                    enrolledCourseString.append(entry.getKey()).append(" (").append(entry.getValue()).append("), ");
                }
                enrolledCourseString.setLength(enrolledCourseString.length() - 2);

                Map<String, Object> updates = new HashMap<>();
                updates.put("EnrolledCourse", enrolledCourseString.toString());
                updates.put("UpcomingCourse", updatedUpcomingCourses.length() > 0
                        ? updatedUpcomingCourses.substring(0, updatedUpcomingCourses.length() - 2)
                        : "");

                db.collection("Students").document(studentId).update(updates)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(getContext(), "Successfully enrolled in: " + enrolledCourseDetails.toString(), Toast.LENGTH_SHORT).show();
                            refreshEnrollmentFragment(); // Refresh the fragment
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(getContext(), "Enrollment update failed.", Toast.LENGTH_SHORT).show();
                            Log.e("Enrollment", "Error updating enrollment: ", e);
                        });
            }
        });
    }

    // Helper method to check for time clashes between two courses
    private boolean hasScheduleClash(CourseSchedule course1, CourseSchedule course2) {
        return course1.daysAndTimes.equals(course2.daysAndTimes);
    }

    // Method to refresh the EnrollmentFragment
    private void refreshEnrollmentFragment() {
        FragmentManager fragmentManager = getParentFragmentManager();  // Prefer parent fragment manager
        if (fragmentManager != null) {
            FragmentTransaction ft = fragmentManager.beginTransaction();
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);  // Optional fade transition
            ft.detach(this).attach(this).commit();
        } else {
            Log.e("Enrollment", "FragmentManager is null; unable to refresh fragment.");
        }
    }

    private String extractTimeLocation(String timeOrLocation) {
        // Method to strip text within parentheses and trim spaces
        return timeOrLocation != null ? timeOrLocation.replaceAll("\\(.*?\\)", "").trim() : "";
    }
}
