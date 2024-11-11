package student.inti.enrolmentapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ProgramCourseAdapter extends RecyclerView.Adapter<ProgramCourseAdapter.ViewHolder> {

    private final List<ProgramCourse> courses;
    private final OnItemClickListener onItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(ProgramCourse course);
    }

    public ProgramCourseAdapter(List<ProgramCourse> courses, OnItemClickListener listener) {
        this.courses = courses;
        this.onItemClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_program_course, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProgramCourse course = courses.get(position);
        holder.courseIdTextView.setText(course.getCourseID());
        holder.courseNameTextView.setText(course.getCourseName());

        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(course);
            }
        });
    }

    @Override
    public int getItemCount() {
        return courses.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView courseIdTextView;
        TextView courseNameTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            courseIdTextView = itemView.findViewById(R.id.course_id);
            courseNameTextView = itemView.findViewById(R.id.course_name);
        }
    }
}

