package student.inti.enrolmentapp;
import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;


public class ScholarshipAdapter extends RecyclerView.Adapter<ScholarshipAdapter.ViewHolder> {

    private final List<Scholarship> scholarships;

    public ScholarshipAdapter(List<Scholarship> scholarships) {
        this.scholarships = scholarships;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_scholarship,
                parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Scholarship scholarship = scholarships.get(position);

        holder.scholarshipName.setText(scholarship.getName());
        holder.scholarshipType.setText(scholarship.getType());

        // Format percentage
        String percentage = scholarship.getPercentage().trim();
        if ("-".equals(percentage) || percentage.isEmpty()) {
            holder.scholarshipPercentage.setText("-");
        } else {
            holder.scholarshipPercentage.setText(percentage + "%");
        }

        // Format amount
        String amount = scholarship.getAmount().trim();
        if ("-".equals(amount) || amount.isEmpty()) {
            holder.scholarshipAmount.setText("-");
        } else {
            holder.scholarshipAmount.setText("RM " + amount);
        }
    }


    @Override
    public int getItemCount() {
        return scholarships.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView scholarshipName;
        TextView scholarshipType;
        TextView scholarshipPercentage;
        TextView scholarshipAmount;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            scholarshipName = itemView.findViewById(R.id.scholarship_name);
            scholarshipType = itemView.findViewById(R.id.scholarship_type);
            scholarshipPercentage = itemView.findViewById(R.id.scholarship_percentage);
            scholarshipAmount = itemView.findViewById(R.id.scholarship_amount);
        }
    }
}
