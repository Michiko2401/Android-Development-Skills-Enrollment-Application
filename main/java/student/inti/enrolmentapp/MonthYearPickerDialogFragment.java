package student.inti.enrolmentapp;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import java.util.Calendar;

public class MonthYearPickerDialogFragment extends DialogFragment {

    private final OnDateSetListener listener;

    public interface OnDateSetListener {
        void onDateSet(int year, int month);
    }

    public MonthYearPickerDialogFragment(OnDateSetListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Calendar calendar = Calendar.getInstance();
        int currentYear = calendar.get(Calendar.YEAR);
        int currentMonth = calendar.get(Calendar.MONTH);

        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_month_year_picker, null);

        NumberPicker monthPicker = dialogView.findViewById(R.id.month_picker);
        NumberPicker yearPicker = dialogView.findViewById(R.id.year_picker);

        monthPicker.setMinValue(0);
        monthPicker.setMaxValue(11);
        monthPicker.setDisplayedValues(new String[] {
                "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        });
        monthPicker.setValue(currentMonth);

        yearPicker.setMinValue(currentYear - 10);
        yearPicker.setMaxValue(currentYear + 10);
        yearPicker.setValue(currentYear);

        return new AlertDialog.Builder(requireActivity())
                .setTitle("Select Month and Year")
                .setView(dialogView)
                .setPositiveButton("Select", (dialog, which) -> {
                    int selectedMonth = monthPicker.getValue();
                    int selectedYear = yearPicker.getValue();

                    // Check if selected date is in the future
                    if (selectedYear > currentYear || (selectedYear == currentYear && selectedMonth > currentMonth)) {
                        // Future date selected, proceed
                        listener.onDateSet(selectedYear, selectedMonth);
                    } else {
                        // Display error message for past date and dismiss dialog without saving
                        Toast.makeText(requireContext(), "Please select a future date.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();
    }
}