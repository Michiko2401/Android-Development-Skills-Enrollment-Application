package student.inti.enrolmentapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SelectedCourseSectionAdapter extends RecyclerView.Adapter<SelectedCourseSectionAdapter.ViewHolder> {

    protected List<CourseSection> courseSections; // Protected access

    public SelectedCourseSectionAdapter(List<CourseSection> courseSections) {
        this.courseSections = courseSections;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_selected_course, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CourseSection section = courseSections.get(position);
        // holder.courseIdTextView.setText(section.getCourseID());
        // holder.courseNameTextView.setText(section.getCourseName());
        holder.courseSectionTextView.setText(section.getSection());
    }

    @Override
    public int getItemCount() {
        return courseSections.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView courseIdTextView;
        TextView courseNameTextView;
        TextView courseSectionTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            courseIdTextView = itemView.findViewById(R.id.course_id_value);
            courseNameTextView = itemView.findViewById(R.id.course_name_value);
            courseSectionTextView = itemView.findViewById(R.id.course_section_value);
        }
    }
}
