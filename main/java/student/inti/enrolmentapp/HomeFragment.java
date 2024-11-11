package student.inti.enrolmentapp;

import android.os.Bundle;
import androidx.fragment.app.Fragment;

import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.graphics.Bitmap;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Calendar;
import java.util.Date;

public class HomeFragment extends Fragment {
    Button generatePDFButton;
    Bitmap bmp, scaledBmp;
    private static final int PERMISSION_REQUEST_CODE = 200;
    private static final String ARG_USER_ID = "userId";
    private static final String ARG_ROLE = "role";
    private String userId;
    private String role;
    private FirebaseFirestore db;
    private GestureDetector gestureDetector;
    private TextView weekInfoTextView;
    private ListenerRegistration listenerRegistration;

    public HomeFragment() {
        // Required empty public constructor
    }

    public static HomeFragment newInstance(String userId, String role) {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USER_ID, userId);
        args.putString(ARG_ROLE, role);
        fragment.setArguments(args);
        Log.d("LoginTrack", "Loading home for user: " + userId + ", Role: " + role);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (getArguments() != null) {
            userId = getArguments().getString(ARG_USER_ID);
        }

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize Firestore instance
        db = FirebaseFirestore.getInstance();

        // Fetch the ProgrammeID from Firestore based on the userId
        DocumentReference studentRef = db.collection("Students").document(userId);
        studentRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()) {
                    // Get the ProgrammeID from the student's document
                    String programmeId = documentSnapshot.getString("ProgrammeID");

                    // Set up the ViewPager2 and DaysPagerAdapter once programmeId is retrieved
                    ViewPager2 viewPager = view.findViewById(R.id.viewPagerDays);
                    DaysPagerAdapter daysPagerAdapter = new DaysPagerAdapter(requireActivity());
                    viewPager.setAdapter(daysPagerAdapter);

                    // Set up TabLayout
                    TabLayout tabLayout = view.findViewById(R.id.tabLayoutDays);
                    new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
                        tab.setText(daysPagerAdapter.getPageTitle(position));
                    }).attach();

                    // Initialize the week info TextView
                    weekInfoTextView = view.findViewById(R.id.weekInfoTextView);

                    // Fetch semester dates from Firestore (Programmes collection)
                    assert programmeId != null;
                    DocumentReference programmeRef = db.collection("Programmes").document(programmeId);
                    programmeRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            if (documentSnapshot.exists()) {
                                // Retrieve dates from the document and calculate the week info
                                Date semesterStart = documentSnapshot.getDate("SemesterStart");
                                Date finalsStart = documentSnapshot.getDate("FinalsStart");
                                Date finalsEnd = documentSnapshot.getDate("FinalsEnd");
                                Date semesterEnd = documentSnapshot.getDate("SemesterEnd");
                                Date newSemesterStart = documentSnapshot.getDate("NewSemesterStart");

                                assert semesterStart != null;
                                updateWeekInfo(semesterStart, finalsStart, finalsEnd, semesterEnd, newSemesterStart);
                            }
                        }
                    });
                } else {
                    Log.d("HomeFragment", "Student document not found for user: " + userId);
                }
            }
        });
        return view;
    }


    public class DaysPagerAdapter extends FragmentStateAdapter {

        private final String[] daysOfWeek = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};

        public DaysPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            String day = daysOfWeek[position];
            Log.d("LoginTrack", "Calling schedule for user: " + userId);
            return ScheduleFragment.newInstance(userId, day);
        }

        @Override
        public int getItemCount() {
            return daysOfWeek.length;
        }

        // Method to get the title for the tab
        public CharSequence getPageTitle(int position) {
            return daysOfWeek[position];
        }
    }

    private void updateWeekInfo(Date semesterStart, Date finalsStart, Date finalsEnd, Date semesterEnd, Date newSemesterStart) {
        Calendar today = Calendar.getInstance();
        String weekInfo = "";

        // Calculate the week number based on semester start
        long diffInMillis = today.getTimeInMillis() - semesterStart.getTime();
        int weekNumber = (int) (diffInMillis / (1000 * 60 * 60 * 24 * 7)) + 1; // Weeks start from 1

        if (today.before(semesterStart) || today.after(semesterEnd)) {
            weekInfo = "Week - | New Semester Starts On: " + newSemesterStart;
        } else if (today.after(finalsEnd) && today.before(semesterEnd)) {
            weekInfo = "Week " + weekNumber;
        } else if (today.after(finalsStart) && today.before(finalsEnd)) {
            weekInfo = "Week " + weekNumber + "Finals Week";
        } else {
            weekInfo = "Week " + weekNumber;
        }

        weekInfoTextView.setText(weekInfo);
    }

}
