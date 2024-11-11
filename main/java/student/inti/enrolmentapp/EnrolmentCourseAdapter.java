package student.inti.enrolmentapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class EnrolmentCourseAdapter extends RecyclerView.Adapter<EnrolmentCourseAdapter.ViewHolder> {

    private final List<EnrolmentCourse> courses;
    private final OnCourseClickListener onCourseClickListener;

    // Interface for handling course clicks
    public interface OnCourseClickListener {
        void onCourseClick(EnrolmentCourse course);
    }

    // Constructor to pass course data and click listener
    public EnrolmentCourseAdapter(List<EnrolmentCourse> courses, OnCourseClickListener onCourseClickListener) {
        this.courses = courses;
        this.onCourseClickListener = onCourseClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout for each item in the RecyclerView
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_enrolment_course, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // Get the course data at the current position
        EnrolmentCourse course = courses.get(position);

        // Bind the course ID and name to the TextViews
        holder.courseIdTextView.setText(course.getId());
        holder.courseNameTextView.setText(course.getName());

        // Handle the item click by passing the course to the listener
        holder.itemView.setOnClickListener(v -> onCourseClickListener.onCourseClick(course));
    }

    @Override
    public int getItemCount() {
        // Return the total number of courses
        return courses.size();
    }

    // ViewHolder class for caching the views of each list item
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView courseIdTextView;
        TextView courseNameTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Initialize the TextViews from the item layout
            courseIdTextView = itemView.findViewById(R.id.course_id);
            courseNameTextView = itemView.findViewById(R.id.course_name);
        }
    }
}
