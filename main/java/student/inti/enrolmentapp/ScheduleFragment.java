package student.inti.enrolmentapp;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import android.app.AlertDialog;
import android.widget.Toast;
import android.graphics.Color;

public class ScheduleFragment extends Fragment {
    private static final String ARG_USER_ID = "userId";
    private String userId;
    private static final String ARG_DAY = "day";
    private String day;
    private FirebaseFirestore db;
    private EditText etReason;
    private EditText etCancelledClassDate;
    private EditText etReplacementClassDate;
    private EditText etReplacementClassTime;
    private EditText etReplacementClassLocation;

    public static ScheduleFragment newInstance(String userId, String day) {
        ScheduleFragment fragment = new ScheduleFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USER_ID, userId);
        args.putString(ARG_DAY, day);
        Log.d("LoginTrack", "Loading schedule for user: " + userId + ", Day: " + day);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            day = getArguments().getString(ARG_DAY);
            userId = getArguments().getString(ARG_USER_ID);
        }
        db = FirebaseFirestore.getInstance();
        // Clear old cancelled/replacement classes
        removeOldCancelledClassesFromDb();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_schedule, container, false);

        // Retrieve and display subjects for the selected day
        fetchSubjectsForDay(view);

        return view;
    }

    private void fetchSubjectsForDay(View view) {
        String studentId = userId;
        db.collection("Students").document(studentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String currentCourses = documentSnapshot.getString("CurrentCourses");
                        if (currentCourses != null) {
                            List<String> courseIds = new ArrayList<>();
                            List<String> sections = new ArrayList<>();
                            for (String course : currentCourses.split(", ")) {
                                String[] parts = course.split(" ");
                                if (parts.length > 1) {
                                    String courseId = parts[0]; // Extract course ID
                                    String section = parts[1].replace("(", "").replace(")", "").trim(); // Extract section
                                    courseIds.add(courseId);
                                    sections.add(section); // Add the section to the list
                                }
                            }
                            // Call displayCourses with the day parameter
                            displayCourses(view, courseIds, sections, day);
                            Log.d("Timetable", "Course IDs: " + courseIds + ", Sections: " + sections);
                        }
                    } else {
                        Log.d("Timetable", "Student document does not exist");
                    }
                })
                .addOnFailureListener(e -> Log.e("Timetable", "Error fetching student: " + e.getMessage()));
    }

    @SuppressLint("SetTextI18n")
    private void displayCourses(View view, List<String> courseIds, List<String> sections, String day) {
        TableLayout tableLayout = view.findViewById(R.id.coursesTable);
        tableLayout.removeAllViews(); // Clear previous rows

        AtomicBoolean hasClasses = new AtomicBoolean(false); // Flag to check if there are any classes for today
        db.collection("Students").document(userId).get()
                .addOnSuccessListener(studentDocument -> {
                    if (studentDocument.exists()) {
                        String cancelledAndReplacementClasses = studentDocument.getString("CancelledAndReplacementClasses");
                        List<CancelledClass> cancelledClasses = parseCancelledClasses(cancelledAndReplacementClasses);

                        for (CancelledClass replacementClass : cancelledClasses) {
                            try {
                                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                                Date replacementDate = dateFormat.parse(replacementClass.getReplaceDate());
                                String replaceDay = getDayOfWeek(replacementClass.getReplaceDate());
                                if (isSameWeek(replacementDate, Calendar.getInstance().getTime()) &&
                                        replaceDay.equals(day)) {
                                    hasClasses.set(true); // Mark that we have classes for today

                                    // Add replacement class details to the table
                                    TableRow replacementRow = new TableRow(getActivity());
                                    replacementRow.setPadding(8, 8, 8, 50); // Padding for better display

                                    TextView replaceTimeTextView = new TextView(getActivity());

                                    String replaceTime = replacementClass.getReplaceTime();

                                    // Split the replaceTime into start and end times
                                    String[] times = replaceTime.split("-");

                                    // Ensure there are two parts (start and end)
                                    if (times.length == 2) {
                                        String startTimeFormatted = times[0] + ":00";
                                        String endTimeFormatted = times[1] + ":00";

                                        // Set up the TextView to display the formatted times
                                        replaceTimeTextView.setText(String.format("%s\n-\n%s", startTimeFormatted, endTimeFormatted));
                                        replaceTimeTextView.setGravity(Gravity.CENTER);
                                        replaceTimeTextView.setLayoutParams(new TableRow.LayoutParams(16, TableRow.LayoutParams.WRAP_CONTENT, 1f));
                                        replaceTimeTextView.setTextColor(Color.parseColor("#38761d"));

                                        // Add this TextView to the row
                                        replacementRow.addView(replaceTimeTextView);
                                    }

                                    // Create a new TextView for Course ID, Reason, etc. to avoid reusing views
                                    TextView courseIdTextView = new TextView(getActivity());
                                    courseIdTextView.setText("\n" + replacementClass.getCourseId());
                                    courseIdTextView.setGravity(Gravity.CENTER);
                                    courseIdTextView.setLayoutParams(new TableRow.LayoutParams(30, TableRow.LayoutParams.WRAP_CONTENT, 2f));
                                    courseIdTextView.setTextColor(Color.parseColor("#38761d"));

                                    // Reason
                                    String cancelledDate = replacementClass.getCancelledDate().substring(0, replacementClass.getCancelledDate().lastIndexOf("/"));

                                    TextView reasonTextView = new TextView(getActivity());
                                    reasonTextView.setText("Cancelled On: " + cancelledDate + " Because: " + replacementClass.getReason());
                                    reasonTextView.setGravity(Gravity.START);
                                    reasonTextView.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 3f));
                                    reasonTextView.setTextColor(Color.parseColor("#38761d"));

                                    // Set up the SimpleDateFormat to format without the year
                                    SimpleDateFormat dateFormatWithoutYear = new SimpleDateFormat("dd/MM", Locale.getDefault());

                                    // Format the date without the year
                                    String formattedDateWithoutYear = dateFormatWithoutYear.format(replacementDate);

                                    // Replace Date
                                    TextView replaceDateTextView = new TextView(getActivity());
                                    replaceDateTextView.setText("\n" + formattedDateWithoutYear);  // Set the date without the year
                                    replaceDateTextView.setGravity(Gravity.CENTER);
                                    replaceDateTextView.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 2f));
                                    replaceDateTextView.setTextColor(Color.parseColor("#38761d"));

                                    // Location
                                    TextView locationTextView = new TextView(getActivity());
                                    locationTextView.setText("\n" + replacementClass.getReplacementLocation());
                                    locationTextView.setGravity(Gravity.CENTER);
                                    locationTextView.setLayoutParams(new TableRow.LayoutParams(28, TableRow.LayoutParams.WRAP_CONTENT, 1f));
                                    locationTextView.setTextColor(Color.parseColor("#38761d"));

                                    // Add to the row
                                    replacementRow.addView(courseIdTextView);
                                    replacementRow.addView(reasonTextView);
                                    replacementRow.addView(replaceDateTextView);
                                    replacementRow.addView(locationTextView);

                                    // Add the row to the table
                                    tableLayout.addView(replacementRow);
                                }
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                        }

                        // Process Regular Classes
                        for (int j = 0; j < courseIds.size(); j++) {
                            String courseId = courseIds.get(j);
                            String sectionToCheck = sections.get(j); // Get the corresponding section
                            final int currentIndex = j; // Create a final variable to capture the cu

                            db.collection("Courses").document(courseId).get()
                                    .addOnSuccessListener(courseDocumentSnapshot -> {
                                        if (courseDocumentSnapshot.exists()) {
                                            String courseName = courseDocumentSnapshot.getString("Name");
                                            String lectureDays = courseDocumentSnapshot.getString("LectureDays");
                                            String lectureLocations = courseDocumentSnapshot.getString("LectureLocations");
                                            String lectureStartTimes = courseDocumentSnapshot.getString("LectureStartTimes");
                                            String lectureEndTimes = courseDocumentSnapshot.getString("LectureEndTimes");

                                            String practicalDays = courseDocumentSnapshot.getString("PracticalDays");
                                            String practicalLocations = courseDocumentSnapshot.getString("PracticalLocations");
                                            String practicalStartTimes = courseDocumentSnapshot.getString("PracticalStartTimes");
                                            String practicalEndTimes = courseDocumentSnapshot.getString("PracticalEndTimes");

                                            String lecturerId = courseDocumentSnapshot.getString("LecturerID");

                                            AtomicReference<String> lecturerName = new AtomicReference<>("");

                                            // Fetch the Lecturer's name using LecturerID
                                            assert lecturerId != null;
                                            db.collection("Lecturers").document(lecturerId).get()
                                                    .addOnSuccessListener(lecturerDocumentSnapshot -> {
                                                        if (lecturerDocumentSnapshot.exists()) {
                                                            lecturerName.set(lecturerDocumentSnapshot.getString("Name"));
                                                        } else {
                                                            Log.d("Lecturer", "No lecturer found with ID: " + lecturerId);
                                                        }
                                                    })
                                                    .addOnFailureListener(e -> Log.e("Lecturer", "Error fetching lecturer: " + e.getMessage()));

                                            List<CourseTime> courseTimes = new ArrayList<>();

                                            String remarks = courseDocumentSnapshot.getString("Remarks");

                                            // Process lecture times
                                            if (lectureDays != null && lectureStartTimes != null) {
                                                String[] lectureDaysArray = lectureDays.split(", ");
                                                assert lectureLocations != null;
                                                String[] lectureLocationsArray = lectureLocations.split(", ");
                                                String[] lectureStartTimesArray = lectureStartTimes.split(", ");
                                                assert lectureEndTimes != null;
                                                String[] lectureEndTimesArray = lectureEndTimes.split(", ");

                                                for (int i = 0; i < lectureDaysArray.length; i++) {
                                                    if (lectureDaysArray[i].contains(day)) {
                                                        hasClasses.set(true); // Mark that we have classes for today
                                                        String startTimeWithSection = lectureStartTimesArray[i];
                                                        String section = extractSection(startTimeWithSection);
                                                        if (section.equals(sectionToCheck)) {
                                                            courseTimes.add(new CourseTime(
                                                                    extractTimeLocation(startTimeWithSection),
                                                                    extractTimeLocation(lectureEndTimesArray[i]),
                                                                    lectureLocationsArray[i],
                                                                    "Lecture",
                                                                    courseName,
                                                                    courseId,
                                                                    sectionToCheck
                                                            ));
                                                        }
                                                    }
                                                }
                                            }

                                            // Process practical times
                                            if (practicalDays != null && practicalStartTimes != null) {
                                                String[] practicalDaysArray = practicalDays.split(", ");
                                                assert practicalLocations != null;
                                                String[] practicalLocationsArray = practicalLocations.split(", ");
                                                String[] practicalStartTimesArray = practicalStartTimes.split(", ");
                                                assert practicalEndTimes != null;
                                                String[] practicalEndTimesArray = practicalEndTimes.split(", ");

                                                for (int i = 0; i < practicalDaysArray.length; i++) {
                                                    if (practicalDaysArray[i].contains(day)) {
                                                        hasClasses.set(true); // Mark that we have classes for today
                                                        String startTimeWithSection = practicalStartTimesArray[i];
                                                        String section = extractSection(startTimeWithSection);
                                                        if (section.equals(sectionToCheck)) {
                                                            courseTimes.add(new CourseTime(
                                                                    extractTimeLocation(startTimeWithSection),
                                                                    extractTimeLocation(practicalEndTimesArray[i]),
                                                                    practicalLocationsArray[i],
                                                                    "Practical",
                                                                    courseName,
                                                                    courseId,
                                                                    sectionToCheck
                                                            ));
                                                        }
                                                    }
                                                }
                                            }

                                            // Sort the course times (Only works partially)
                                            courseTimes.sort((course1, course2) -> {
                                                int startTime1 = Integer.parseInt(course1.getStartTime());
                                                int startTime2 = Integer.parseInt(course2.getStartTime());
                                                return Integer.compare(startTime1, startTime2);
                                            });

                                            // Display the courses in the TableLayout
                                            for (CourseTime courseTime : courseTimes) {
                                                // Create a new TableRow
                                                TableRow tableRow = new TableRow(getActivity());
                                                tableRow.setPadding(8, 8, 8, 50); // Set padding for each row

                                                // Start time and End time with hyphen in between, formatted
                                                String startTimeFormatted = formatTime(courseTime.getStartTime());
                                                String endTimeFormatted = formatTime(courseTime.getEndTime());
                                                TextView timeTextView = new TextView(getActivity());
                                                timeTextView.setText(String.format("%s\n-\n%s", startTimeFormatted + ":00", endTimeFormatted + ":00"));
                                                timeTextView.setGravity(Gravity.CENTER);
                                                timeTextView.setLayoutParams(new TableRow.LayoutParams(16, TableRow.LayoutParams.WRAP_CONTENT, 1f));

                                                // Course ID
                                                TextView courseIdTextView = new TextView(getActivity());
                                                courseIdTextView.setText("\n" + courseTime.getCourseId());
                                                courseIdTextView.setGravity(Gravity.CENTER);
                                                courseIdTextView.setLayoutParams(new TableRow.LayoutParams(30, TableRow.LayoutParams.WRAP_CONTENT, 2f));

                                                // Course Name
                                                TextView courseNameTextView = new TextView(getActivity());
                                                courseNameTextView.setText(courseTime.getCourseName());
                                                courseNameTextView.setGravity(Gravity.START);
                                                courseNameTextView.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 3f));

                                                // Lecture or Practical
                                                TextView lecturePracticalTextView = new TextView(getActivity());
                                                lecturePracticalTextView.setText("\n" + courseTime.getType());
                                                lecturePracticalTextView.setGravity(Gravity.CENTER);
                                                lecturePracticalTextView.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 2f));

                                                // Location
                                                TextView locationTextView = new TextView(getActivity());
                                                locationTextView.setText("\n" + courseTime.getLocation());
                                                locationTextView.setGravity(Gravity.CENTER);
                                                locationTextView.setLayoutParams(new TableRow.LayoutParams(28, TableRow.LayoutParams.WRAP_CONTENT, 1f));

                                                // Add TextViews to the row
                                                tableRow.addView(timeTextView);
                                                tableRow.addView(courseIdTextView);
                                                tableRow.addView(courseNameTextView);
                                                tableRow.addView(lecturePracticalTextView);
                                                tableRow.addView(locationTextView);

                                                String status = "";

                                                // Check for cancelled classes and update row color
                                                for (CancelledClass cancelledClass : cancelledClasses) {
                                                    try {
                                                        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                                                        Date cancelledDate = dateFormat.parse(cancelledClass.getCancelledDate());

                                                        // Check if the course is cancelled for today
                                                        if (cancelledClass.getCourseId().equals(courseId) &&
                                                                isSameWeek(cancelledDate, Calendar.getInstance().getTime()) &&
                                                                cancelledClass.getCancelledDay().equals(day) &&
                                                        cancelledClass.getCancelledStartTime().equals(extractTimeLocation(courseTime.getStartTime()))) {
                                                            // If the class is canceled, set the status to "Cancelled"
                                                            status = "Cancelled";

                                                            // Set the text color of the row to red if cancelled
                                                            timeTextView.setTextColor(Color.RED);
                                                            courseIdTextView.setTextColor(Color.RED);
                                                            courseNameTextView.setTextColor(Color.RED);
                                                            lecturePracticalTextView.setTextColor(Color.RED);
                                                            locationTextView.setTextColor(Color.RED);
                                                            break; // Exit the loop as we don't need to check further once we find a match
                                                        }
                                                        else {
                                                            status = "";
                                                        }
                                                    } catch (ParseException e) {
                                                        e.printStackTrace();
                                                    }
                                                }
                                                String finalStatus = status;

                                                // Make the row clickable
                                                tableRow.setOnClickListener(v -> showCourseDetailPopup(courseTime.getCourseId(), courseTime.getCourseName(),
                                                        courseTime.getStartTime(), courseTime.getEndTime(), sectionToCheck, courseTime.getLocation(), lecturerName.get(), remarks, day, finalStatus));

                                                // Add the row to the TableLayout
                                                tableLayout.addView(tableRow);
                                            }
                                        } else {
                                            Log.d("Timetable", "No course found with ID: " + courseId);
                                        }

                                        TextView noClassesTextView = new TextView(getActivity());
                                        // Check if any classes were found after processing all course IDs
                                        if (currentIndex == courseIds.size() - 1 && !hasClasses.get()) {
                                            // Show "No Classes for Today" message
                                            noClassesTextView.setText("No Classes Today");
                                            noClassesTextView.setGravity(Gravity.CENTER);
                                            tableLayout.addView(noClassesTextView);
                                        } else {
                                            noClassesTextView.setText("");
                                        }
                                    })
                                    .addOnFailureListener(e -> Log.e("Timetable", "Error fetching course: " + e.getMessage()));
                        }
                    }
                });
    }

    private static class CancelledClass {
        private final String courseId;
        private final String cancelledDate;
        private final String cancelledDay;
        private final String cancelledStartTime;
        private final String reason;
        private final String replaceDate;
        private final String replaceTime;
        private final String replacementLocation;

        public CancelledClass(String courseId, String cancelledDate, String cancelledDay,
                              String cancelledStartTime, String reason, String replaceDate,
                              String replaceTime, String replacementLocation) {
            this.courseId = courseId;
            this.cancelledDate = cancelledDate;
            this.cancelledDay = cancelledDay;
            this.cancelledStartTime = cancelledStartTime;
            this.reason = reason;
            this.replaceDate = replaceDate;
            this.replaceTime = replaceTime;
            this.replacementLocation = replacementLocation;
        }

        // Getters for all fields
        public String getCourseId() {
            return courseId;
        }

        public String getCancelledDate() {
            return cancelledDate;
        }

        public String getCancelledDay() {
            return cancelledDay;
        }

        public String getCancelledStartTime() {
            return cancelledStartTime;
        }

        public String getReason() {
            return reason;
        }

        public String getReplaceDate() {
            return replaceDate;
        }

        public String getReplaceTime() {
            return replaceTime;
        }

        public String getReplacementLocation() {
            return replacementLocation;
        }
    }

    private List<CancelledClass> parseCancelledClasses(String rawData) {
        List<CancelledClass> cancelledClasses = new ArrayList<>();
        if (rawData == null || rawData.equals("-")) return cancelledClasses;

        String[] entries = rawData.split("\\), \\(");
        for (String entry : entries) {
            String[] parts = entry.replace("(", "").replace(")", "").split(" ");
            if (parts.length >= 7) { // Make sure we have enough parts
                String courseId = parts[0];
                String cancelledDate = parts[1];
                String cancelledDay = getDayOfWeek(cancelledDate);
                String cancelledStartTime = parts[2]; // Start time for cancelled class
                String replaceDate = parts[3]; // Replace date
                String replaceTime = parts[4]; // Replace time
                String location = parts[5]; // Location
                // Concatenate everything from index 6 onwards as the reason
                StringBuilder reasonBuilder = new StringBuilder();
                for (int i = 6; i < parts.length; i++) {
                    reasonBuilder.append(parts[i]).append(" ");
                }
                String reason = reasonBuilder.toString().trim(); // Combine the reason words

                // Add CancelledClass with all fields
                cancelledClasses.add(new CancelledClass(courseId, cancelledDate, cancelledDay,
                        cancelledStartTime, reason, replaceDate, replaceTime, location));
            }
        }
        return cancelledClasses;
    }

    private boolean isSameWeek(Date date1, Date date2) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(date1);
        cal2.setTime(date2);

        cal1.set(Calendar.DAY_OF_WEEK, cal1.getFirstDayOfWeek());
        cal2.set(Calendar.DAY_OF_WEEK, cal2.getFirstDayOfWeek());

        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.WEEK_OF_YEAR) == cal2.get(Calendar.WEEK_OF_YEAR);
    }

    private String getDayOfWeek(String dateStr) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        try {
            Date date = sdf.parse(dateStr);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            return new DateFormatSymbols().getWeekdays()[calendar.get(Calendar.DAY_OF_WEEK)];
        } catch (ParseException e) {
            e.printStackTrace();
            return "";
        }
    }

    // Method to format the time, adding leading zero if needed
    private String formatTime(String time) {
        if (time.length() == 1) {
            return "0" + time; // Add leading zero if it's a single digit
        }
        return time; // Return time as is if it's already two digits
    }

    // Helper function to extract section from time/location with section info
    private String extractSection(String timeOrLocation) {
        if (timeOrLocation.contains("(")) {
            return timeOrLocation.substring(timeOrLocation.indexOf("(") + 1, timeOrLocation.indexOf(")"));
        }
        return ""; // Return empty string if no section info is found
    }

    // CourseTime class to represent each course session
    private static class CourseTime {
        private final String startTime;
        private final String endTime;
        private final String location;
        private final String type;
        private final String courseName;
        private final String courseId;
        private final String section;

        public CourseTime(String startTime, String endTime, String location, String type, String courseName, String courseId, String section) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.location = location;
            this.type = type;
            this.courseName = courseName;
            this.courseId = courseId;
            this.section = section;
        }

        public String getStartTime() {
            return startTime;
        }

        public String getEndTime() {
            return endTime;
        }

        public String getLocation() {
            return location;
        }

        public String getType() {
            return type;
        }

        public String getCourseName() {
            return courseName;
        }

        public String getCourseId() {
            return courseId;
        }

        public String getSection() {
            return section;
        }
    }

    @SuppressLint("SetTextI18n")
    private void showCourseDetailPopup(String courseId, String courseName, String startTime, String endTime, String section, String location, String lecturer, String remarks, String classDay, String status) {
        // Declare cancellationList here
        List<CancelledClass> cancellationList = new ArrayList<>();

        // Fetch cancellation details from the database
        db.collection("Students").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String cancelledClasses = documentSnapshot.contains("CancelledAndReplacementClasses")
                                ? documentSnapshot.getString("CancelledAndReplacementClasses")
                                : "-";

                        assert cancelledClasses != null;
                        if (!cancelledClasses.equals("-")) {
                            // Split the cancelled classes string into individual entries
                            String[] cancellations = cancelledClasses.split(", ");

                            for (String cancellation : cancellations) {
                                cancellation = cancellation.trim().replace("(", "").replace(")", "");
                                String[] parts = cancellation.split(" ");

                                if (parts.length >= 7) {
                                    String cancelledCourseId = parts[0];
                                    String cancelledDate = parts[1];
                                    String cancelledStartTime = parts[2];
                                    String replacementDate = parts[3];
                                    String replacementTime = parts[4];
                                    String replacementLocation = parts[5];

                                    StringBuilder reasonBuilder = new StringBuilder();
                                    for (int i = 6; i < parts.length; i++) {
                                        reasonBuilder.append(parts[i]).append(" ");
                                    }
                                    String reason = reasonBuilder.toString().trim();

                                    // Skip irrelevant cancellations
                                    if (!cancelledCourseId.equals(courseId) || !cancelledStartTime.equals(startTime)) {
                                        continue;
                                    }

                                    cancellationList.add(new CancelledClass(courseId, cancelledDate, day, cancelledStartTime, reason, replacementDate, replacementTime, replacementLocation));
                                }
                            }

                            // Filter out cancellations based on replacement dates
                            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

                            // Sort cancellation list by date
                            cancellationList.sort((c1, c2) -> {
                                try {
                                    Date date1 = sdf.parse(c1.getCancelledDate());
                                    Date date2 = sdf.parse(c2.getCancelledDate());
                                    assert date1 != null;
                                    return date1.compareTo(date2);
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                    return 0;
                                }
                            });
                        }
                    }

                    // Inflate the popup view and populate the textviews
                    View popupView = LayoutInflater.from(getActivity()).inflate(R.layout.popup_course_details, null);
                    TextView courseNameTextView = popupView.findViewById(R.id.course_name);
                    TextView courseIdTextView = popupView.findViewById(R.id.course_id);
                    TextView classTimeTextView = popupView.findViewById(R.id.class_time);
                    TextView sectionTextView = popupView.findViewById(R.id.section);
                    TextView locationTextView = popupView.findViewById(R.id.location);
                    TextView lecturerTextView = popupView.findViewById(R.id.lecturer);
                    TextView classDayTextView = popupView.findViewById(R.id.class_day);
                    TextView remarksTextView = popupView.findViewById(R.id.remarks);
                    TextView statusTextView = popupView.findViewById(R.id.status);
                    View cancelLine = popupView.findViewById(R.id.cancel_line);
                    TextView cancelledClassesTextView = popupView.findViewById(R.id.cancelled_classes);

                    courseNameTextView.setText(courseName);
                    courseIdTextView.setText("Course Id: " + courseId);
                    classTimeTextView.setText(String.format("Time: %s:00 - %s:00", startTime, endTime));
                    sectionTextView.setText("Section: " + section);
                    locationTextView.setText("Location: " + extractTimeLocation(location));
                    lecturerTextView.setText("Lecturer: " + lecturer);
                    classDayTextView.setText("Day: " + classDay);

                    // Show remarks if they exist
                    remarksTextView.setVisibility(remarks != null && !remarks.isEmpty() && !remarks.equals("-") ? View.VISIBLE : View.GONE);
                    remarksTextView.setText("Remarks: " + remarks);

                    // Show status if it exists
                    statusTextView.setVisibility(status != null && !status.isEmpty() && !status.equals("-") ? View.VISIBLE : View.GONE);
                    statusTextView.setText("Status: " + status);

                    // Handle cancellations and replacements visibility
                    cancelLine.setVisibility(cancellationList.isEmpty() ? View.GONE : View.VISIBLE);
                    cancelledClassesTextView.setVisibility(cancellationList.isEmpty() ? View.GONE : View.VISIBLE);

                    TableLayout cancellationTable = popupView.findViewById(R.id.cancellation_table);

                    if (!cancellationList.isEmpty()) {
                        cancellationTable.setVisibility(View.VISIBLE);

                        // Clear existing rows
                        cancellationTable.removeAllViews();

                        for (CancelledClass cancellation : cancellationList) {
                            TableRow row = new TableRow(getActivity());
                            row.setLayoutParams(new TableRow.LayoutParams(
                                    TableRow.LayoutParams.MATCH_PARENT,
                                    TableRow.LayoutParams.WRAP_CONTENT
                            ));
                            row.setPadding(0, 0, 0, 16); // Add padding at the bottom for spacing

                            // Add cancellation info
                            TextView cancellationInfo = new TextView(getActivity());
                            cancellationInfo.setText(String.format("Cancelled Date: %s %s:00\nReason: %s\nReplacement: %s %s at %s",
                                    cancellation.getCancelledDate(), cancellation.getCancelledStartTime(),
                                    cancellation.getReason().equals("-") ? "No reason provided" : cancellation.getReason(),
                                    cancellation.getReplaceDate(), cancellation.getReplaceDate(), cancellation.getReplacementLocation()));
                            cancellationInfo.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1));
                            row.addView(cancellationInfo);

                            // Add delete button
                            ImageButton deleteButton = new ImageButton(getActivity());
                            deleteButton.setImageResource(R.drawable.delete_button);
                            deleteButton.setBackground(null);
                            deleteButton.setOnClickListener(v -> {
                                // Show confirmation dialog
                                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

                                builder.setMessage("Are you sure you want to delete this cancelled class?")
                                        .setCancelable(false) // Make the dialog non-cancelable (can be changed to true if needed)

                                        // Customizing the "No" button
                                        .setPositiveButton("No", (dialog, id) -> {
                                            // Close the dialog without deleting anything
                                            dialog.dismiss();
                                        })

                                        // Customizing the "Yes" button and changing its color to red
                                        .setNegativeButton("Delete", (dialog, id) -> {
                                            // Proceed with deletion if user confirms
                                            cancellationTable.removeView(row); // Remove the row
                                            removeCancelledClassFromDb(courseId, cancellation); // Remove from database
                                        });

                                // Create and show the dialog
                                AlertDialog dialog = builder.create();

                                // Access buttons and change the order and color
                                dialog.setOnShowListener(dialogInterface -> {
                                    Button btnYes = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

                                    // Make the "Yes" button red
                                    btnYes.setTextColor(Color.RED);
                                });

                                dialog.show();
                            });
                            row.addView(deleteButton);

                            // Add the row to the table
                            cancellationTable.addView(row);

                            // Add a spacer row for extra spacing (optional)
                            View spacer = new View(getActivity());
                            spacer.setLayoutParams(new TableRow.LayoutParams(
                                    TableRow.LayoutParams.MATCH_PARENT,
                                    16 // Height of the spacer in pixels
                            ));
                            cancellationTable.addView(spacer);
                        }
                    } else {
                        cancellationTable.setVisibility(View.GONE);
                    }

                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setView(popupView)
                            .setCancelable(true)
                            .setPositiveButton("OK", null)
                            .setNegativeButton("Class Cancelled", (dialog, which) -> openCancellationPopup(courseId, startTime));

                    AlertDialog dialog = builder.create();
                    dialog.show();

                    // Adjust dialog dimensions
                    dialog.getWindow().setLayout(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );
                });
    }

    // Helper method to remove the cancelled class from the database
    private void removeCancelledClassFromDb(String courseId, CancelledClass cancellation) {
        db.collection("Students").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String cancelledClasses = documentSnapshot.contains("CancelledAndReplacementClasses")
                                ? documentSnapshot.getString("CancelledAndReplacementClasses")
                                : "-";

                        assert cancelledClasses != null;
                        if (!cancelledClasses.equals("-")) {
                            // Split the cancelled classes string into individual entries
                            String[] cancellations = cancelledClasses.split(", ");
                            List<String> updatedCancellations = new ArrayList<>();

                            // Loop through the cancellations and match the exact cancellation
                            for (String cancellationEntry : cancellations) {
                                // Construct the exact cancellation string
                                String expectedCancellation = String.format("(%s %s %s %s %s %s %s)",
                                        courseId, cancellation.getCancelledDate(),
                                        cancellation.getCancelledStartTime(), cancellation.getReplaceDate(),
                                        cancellation.getReplaceTime(), cancellation.getReplacementLocation(), cancellation.getReason());

                                Log.d("RemoveFromDB", expectedCancellation);

                                // If the cancellationEntry does not match the exact string, add it to the list
                                if (!cancellationEntry.equals(expectedCancellation)) {
                                    updatedCancellations.add(cancellationEntry);
                                }
                            }

                            // Join the updated list back into a string (with commas separating entries)
                            String updatedCancelledClasses = String.join(", ", updatedCancellations);

                            // Update the cancelled classes string in Firestore
                            db.collection("Students").document(userId)
                                    .update("CancelledAndReplacementClasses", updatedCancelledClasses)
                                    .addOnSuccessListener(aVoid -> {
                                        // Show success Toast
                                        Toast.makeText(getActivity(), "Cancelled class removed from DB", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        // Show failure Toast
                                        Toast.makeText(getActivity(), "Error updating cancelled class list: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    });
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("CancellationFetch", "Error fetching cancelled classes: " + e.getMessage()));
    }

    // Helper method to remove all old cancelled and replacement classes
    private void removeOldCancelledClassesFromDb() {
        db.collection("Students").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String cancelledClasses = documentSnapshot.contains("CancelledAndReplacementClasses")
                                ? documentSnapshot.getString("CancelledAndReplacementClasses")
                                : "-";

                        assert cancelledClasses != null;
                        if (!cancelledClasses.equals("-")) {
                            // Split the cancelled classes string into individual entries
                            String[] cancellations = cancelledClasses.split(", ");
                            List<String> updatedCancellations = new ArrayList<>();

                            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

                            for (String cancellationEntry : cancellations) {
                                // Extract date parts (cancelled date and replacement date)
                                String[] parts = cancellationEntry.split(" ");
                                if (parts.length >= 6) {
                                    String cancelledDateString = parts[1];  // Cancelled date
                                    String replacementDateString = parts[3];  // Replacement date (if available)

                                    // Check if the cancellation or replacement date is older than 1 week
                                    Date cancelledDate = null;
                                    Date replacementDate = null;

                                    try {
                                        cancelledDate = sdf.parse(cancelledDateString);
                                        if (replacementDateString != null && !replacementDateString.isEmpty()) {
                                            replacementDate = sdf.parse(replacementDateString);
                                        }
                                    } catch (ParseException e) {
                                        e.printStackTrace();
                                    }

                                    // Get the date to one week ago
                                    Calendar calendar = Calendar.getInstance();
                                    calendar.add(Calendar.DATE, -7);
                                    Date oneWeekAgo = calendar.getTime();

                                    // Check if either date is older than one week, and if so, remove it
                                    if ((replacementDate != null && replacementDate.before(oneWeekAgo)) ||
                                            (replacementDate == null && cancelledDate != null && cancelledDate.before(oneWeekAgo))) {
                                        // Don't add to the updated list (i.e., delete it)
                                        continue;
                                    }
                                }

                                // Add the valid cancellation entry to the updated list
                                updatedCancellations.add(cancellationEntry);
                            }

                            // Join the updated list back into a string (with commas separating entries)
                            String updatedCancelledClasses = String.join(", ", updatedCancellations);

                            // Update the cancelled classes string in Firestore
                            db.collection("Students").document(userId)
                                    .update("CancelledAndReplacementClasses", updatedCancelledClasses)
                                    .addOnSuccessListener(aVoid -> {
                                        // Log success message
                                        Log.d("CancellationUpdated", "Old cancelled classes removed from DB successfully.");
                                    })
                                    .addOnFailureListener(e -> {
                                        // Log failure message
                                        Log.d("CancellationUpdated", "Error updating cancelled class list: " + e.getMessage());
                                    });
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("CancellationFetch", "Error fetching cancelled classes: " + e.getMessage()));
    }
    Button btnClose;
    @SuppressLint("SetTextI18n")
    private void openCancellationPopup(String courseId, String startTime) {

        // Inflate the popup layout
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View popupView = inflater.inflate(R.layout.popup_cancellation, null);

        etReason = popupView.findViewById(R.id.etReason);
        etCancelledClassDate = popupView.findViewById(R.id.etCancelledClassDate);
        etReplacementClassDate = popupView.findViewById(R.id.etReplacementClassDate);
        etReplacementClassTime = popupView.findViewById(R.id.etReplacementClassTime);
        etReplacementClassLocation = popupView.findViewById(R.id.etReplacementClassLocation);
        Button btnSubmit = popupView.findViewById(R.id.btnSubmit);
        btnClose = popupView.findViewById(R.id.btnClose);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(popupView);
        AlertDialog dialog = builder.create();

        btnClose.setOnClickListener(v -> {
            // Dismiss the popup when the close button is clicked
            dialog.dismiss();
        });

        // Create Submit Button
        btnSubmit.setOnClickListener(v -> {
            String reason = etReason.getText().toString();
            String cancelledDate = etCancelledClassDate.getText().toString();
            String replacementClassDate = etReplacementClassDate.getText().toString();
            String replacementClassTime = etReplacementClassTime.getText().toString();
            String replacementClassLocation = etReplacementClassLocation.getText().toString();

            cancelClassAndStoreDetails(courseId, reason, cancelledDate, startTime, replacementClassDate, replacementClassTime, replacementClassLocation);
        });

        dialog.show();
    }

    private void cancelClassAndStoreDetails(String courseId, String reason, String cancelledDate, String startTime, String replacementClassDate, String replacementClassTime, String replacementClassLocation) {
        // Input validation for reason
        if (reason.isEmpty()) {
            reason = "-";
        }

        // Validate cancelled date format (dd/MM/yyyy)
        if (cancelledDate.isEmpty()) {
            Toast.makeText(getActivity(), "Please provide a cancellation date.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!cancelledDate.matches("\\d{2}/\\d{2}/\\d{4}")) {
            Toast.makeText(getActivity(), "Invalid cancellation date format. Please use dd/MM/yyyy.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate replacement class date format (dd/MM/yyyy)
        if (replacementClassDate.isEmpty()) {
            Toast.makeText(getActivity(), "Please provide a replacement class date.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!replacementClassDate.matches("\\d{2}/\\d{2}/\\d{4}")) {
            Toast.makeText(getActivity(), "Invalid replacement date format. Please use dd/MM/yyyy.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate replacement class time format (HH:mm)
        if (replacementClassTime.isEmpty()) {
            Toast.makeText(getActivity(), "Please provide a replacement class time.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!replacementClassTime.matches("\\d{2}-\\d{2}")) {
            Toast.makeText(getActivity(), "Invalid replacement time format. Please use HH-HH.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate replacement class location
        if (replacementClassLocation.isEmpty()) {
            replacementClassLocation = "-";
        }

        // Parse and validate both dates
        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        try {
            // Parse and validate the cancelled date
            Date parsedCancelledDate = dateFormat.parse(cancelledDate);

            // Ensure the cancelled date is in the current year
            Calendar cancelledCalendar = Calendar.getInstance();
            cancelledCalendar.setTime(parsedCancelledDate);
            int cancelledYear = cancelledCalendar.get(Calendar.YEAR);

            Calendar currentYear = Calendar.getInstance();
            int currentYearInt = currentYear.get(Calendar.YEAR);

            if (cancelledYear != currentYearInt) {
                Toast.makeText(getActivity(), "Cancellation date must be within the current year.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Parse and validate the replacement date
            Date parsedReplacementDate = dateFormat.parse(replacementClassDate);

            // Ensure the replacement date is in the current year
            Calendar replacementCalendar = Calendar.getInstance();
            replacementCalendar.setTime(parsedReplacementDate);
            int replacementYear = replacementCalendar.get(Calendar.YEAR);

            if (replacementYear != currentYearInt) {
                Toast.makeText(getActivity(), "Replacement date must be within the current year.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Combine all details into the required format
            AtomicReference<String> cancellationDetails = new AtomicReference<>(String.format("(%s %s %s %s %s %s %s)",
                    courseId, cancelledDate, startTime, replacementClassDate, replacementClassTime, replacementClassLocation, reason));

            Log.d("ClassCancel", "Cancelling class for: " + userId);

            // Fetch and update the database
            db.collection("Students").document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            // Get the current cancelled and replacement classes
                            String cancelledClasses = documentSnapshot.contains("CancelledAndReplacementClasses")
                                    ? documentSnapshot.getString("CancelledAndReplacementClasses")
                                    : "-";

                            // Append the new cancellation detail
                            String newCancellationDetail = cancellationDetails.get();
                            if (cancelledClasses != null && !cancelledClasses.trim().equals("-")) {
                                newCancellationDetail = cancelledClasses + ", " + newCancellationDetail;
                            }

                            // Update the database
                            db.collection("Students").document(userId)
                                    .update("CancelledAndReplacementClasses", newCancellationDetail)
                                    .addOnSuccessListener(
                                            aVoid -> Toast.makeText(getActivity(), "Class cancelled successfully with replacement details.", Toast.LENGTH_SHORT).show())
                                    .addOnFailureListener(e -> Log.e("ClassCancel", "Error updating cancellation details: " + e.getMessage()));

                            btnClose.performClick();  // Close the popup

                        } else {
                            Log.e("ClassCancel", "Student document does not exist for userId: " + userId);
                        }
                    })
                    .addOnFailureListener(e -> Log.e("ClassCancel", "Error fetching existing cancellation details: " + e.getMessage()));

        } catch (ParseException e) {
            // Handle invalid date
            Toast.makeText(getActivity(), "Invalid date provided. Please ensure both dates are in the format dd/MM/yyyy.", Toast.LENGTH_SHORT).show();
        }
    }

    // Helper function to extract time/location without section info
    private String extractTimeLocation(String timeOrLocation) {
        return timeOrLocation.replaceAll("\\(.*?\\)", "").trim();
    }
}
