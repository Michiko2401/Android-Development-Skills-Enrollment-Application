package student.inti.enrolmentapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CourseAdapter extends RecyclerView.Adapter<CourseAdapter.CourseViewHolder> {

    private final List<Course> courseList;

    public CourseAdapter(List<Course> courseList) {
        this.courseList = courseList;
    }

    @NonNull
    @Override
    public CourseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_course, parent, false);
        return new CourseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CourseViewHolder holder, int position) {
        Course course = courseList.get(position);
        holder.courseIdTextView.setText(course.getId());
        holder.courseNameTextView.setText(course.getName());
        holder.coursePriceTextView.setText(course.getPrice());
    }

    @Override
    public int getItemCount() {
        return courseList.size();
    }

    public static class CourseViewHolder extends RecyclerView.ViewHolder {
        TextView courseIdTextView;
        TextView courseNameTextView;
        TextView coursePriceTextView;

        public CourseViewHolder(@NonNull View itemView) {
            super(itemView);
            courseIdTextView = itemView.findViewById(R.id.courseIdTextView);
            courseNameTextView = itemView.findViewById(R.id.courseNameTextView);
            coursePriceTextView = itemView.findViewById(R.id.coursePriceTextView);
        }
    }
}
