package student.inti.enrolmentapp;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CourseSectionAdapter extends RecyclerView.Adapter<CourseSectionAdapter.ViewHolder> {

    private final List<CourseSection> courseSections;

    public CourseSectionAdapter(List<CourseSection> courseSections) {
        this.courseSections = courseSections;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_course_section, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CourseSection section = courseSections.get(position);
        holder.lectureDayTextView.setText(section.getLectureDay());
        holder.lectureStartTimeTextView.setText(section.getLectureStartTime());
        holder.lectureEndTimeTextView.setText(section.getLectureEndTime());
        holder.practicalDayTextView.setText(section.getPracticalDay());
        holder.practicalStartTimeTextView.setText(section.getPracticalStartTime());
        holder.practicalEndTimeTextView.setText(section.getPracticalEndTime());
        holder.remarkTextView.setText(section.getRemarks());
        holder.sectionTextView.setText(section.getSection());
    }

    @Override
    public int getItemCount() {
        return courseSections.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView lectureDayTextView;
        TextView lectureStartTimeTextView;
        TextView lectureEndTimeTextView;
        TextView practicalDayTextView;
        TextView practicalStartTimeTextView;
        TextView practicalEndTimeTextView;
        TextView remarkTextView;
        TextView sectionTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            lectureDayTextView = itemView.findViewById(R.id.lecture_day);
            lectureStartTimeTextView = itemView.findViewById(R.id.lecture_start_time);
            lectureEndTimeTextView = itemView.findViewById(R.id.lecture_end_time);
            practicalDayTextView = itemView.findViewById(R.id.practical_day);
            practicalStartTimeTextView = itemView.findViewById(R.id.practical_start_time);
            practicalEndTimeTextView = itemView.findViewById(R.id.practical_end_time);
            remarkTextView = itemView.findViewById(R.id.remarks);
            sectionTextView = itemView.findViewById(R.id.section);
        }
    }
}
