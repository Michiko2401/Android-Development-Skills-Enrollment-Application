// balanced amount record(?)
// Need to build a feature for validate the expiration date
// Date validation
package student.inti.enrolmentapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;
import android.provider.Settings;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.ScrollView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;

import android.os.Bundle;
import android.os.Environment;
import android.graphics.pdf.PdfDocument;
import android.util.Log;
import android.net.Uri;
import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.itextpdf.layout.font.FontSet;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.property.TextAlignment;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.io.font.constants.StandardFonts;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;



import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.TimeZone;

import android.text.InputType;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;





public class ProceedPaymentFragment extends Fragment implements MonthYearPickerDialogFragment.OnDateSetListener {
    // variables for our buttons.
    // Keys for the arguments
    private static final String ARG_STUDENT_ID = "studentId";
    private static final String ARG_TOTAL_PRICE = "totalPrice";
    private static final String ARG_ACCOUNT_BALANCE = "accountBalance";
    private static final String ARG_TOTAL_SCHOLARSHIP = "totalScholarship";
    private static final String ARG_TOTAL_SCHOOL_FEES = "totalSchoolFees";
    private static final String ARG_ENROLLED_COURSE_ID = "enrolledCourseID";

    private TextInputEditText expiryDateEditText;
    private SimpleDateFormat dateFormatter;
    private FirebaseFirestore db;
    private View rootView;

    // Variables to store the data
    private String studentId;
    private double totalPrice;
    private double accountBalance;
    private double totalScholarship;
    private double totalSchoolFees;
    private String enrolledCourseID;
    private String paymentId, studentName;
    private double receiptTotalAmount;
    private String paymentMethod;

    Button btnGeneratePDFReceipt;

    // declaring width and height for our PDF file.
    int pageHeight = 1120;
    int pageWidth = 792;

    // creating a bitmap variable for storing our images
    Bitmap bmp, scaledBmp;

    // constant code for runtime permissions
    private static final int PERMISSION_REQUEST_CODE = 200;

    public ProceedPaymentFragment() {
        // Required empty public constructor
    }

    private void generateUniquePaymentId(PaymentIdCallback callback) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();

        // Generate a new ID with 'R' prefix and 5 additional characters
        StringBuilder id = new StringBuilder("R");
        while (id.length() < 9) {
            int index = random.nextInt(chars.length());
            id.append(chars.charAt(index));
        }

        paymentId = id.toString();

        // Query the Payments collection to check if the ID already exists
        db.collection("Payments")
                .document(paymentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // If the ID exists, generate a new ID and check again
                        generateUniquePaymentId(callback);
                    } else {
                        // ID is unique, return it via the callback
                        callback.onComplete(paymentId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("generateUniquePaymentId", "Error checking payment ID existence", e);
                    callback.onComplete(null);
                });
    }

    public interface PaymentIdCallback {
        void onComplete(String paymentId);
    }

    public static ProceedPaymentFragment newInstance(String studentId, double totalPrice, double accountBalance, double totalScholarship, double totalSchoolFees, String enrolledCourseID) {
        ProceedPaymentFragment fragment = new ProceedPaymentFragment();
        Bundle args = new Bundle();
        args.putString(ARG_STUDENT_ID, studentId);
        args.putDouble(ARG_TOTAL_PRICE, totalPrice);
        args.putDouble(ARG_ACCOUNT_BALANCE, accountBalance);
        args.putDouble(ARG_TOTAL_SCHOLARSHIP, totalScholarship);
        args.putDouble(ARG_TOTAL_SCHOOL_FEES, totalSchoolFees);
        args.putString(ARG_ENROLLED_COURSE_ID, enrolledCourseID);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            studentId = getArguments().getString("studentId");
            totalPrice = getArguments().getDouble("totalPrice");
            accountBalance = getArguments().getDouble("accountBalance");
            totalScholarship = getArguments().getDouble("totalScholarship");
            totalSchoolFees = getArguments().getDouble("totalSchoolFees");
            enrolledCourseID = getArguments().getString("enrolledCourseID");
        }

        db = FirebaseFirestore.getInstance();

        // Check and request permissions
        if (checkPermission()) {
            Toast.makeText(getContext(), "Permission Granted", Toast.LENGTH_SHORT).show();
        } else {
            requestPermission();
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_proceed_payment, container, false);

        EditText expiryDateEditText = rootView.findViewById(R.id.expiry_date_edit_text);

        // Set up expiryDateEditText click listener to show the MonthYearPickerDialogFragment
        expiryDateEditText.setOnClickListener(v -> {
            MonthYearPickerDialogFragment monthYearPicker = new MonthYearPickerDialogFragment((year, month) -> {
                // Format the selected date as MM/YY
                @SuppressLint("DefaultLocale") String formattedExpiryDate = String.format("%02d/%02d", month + 1, year % 100);
                expiryDateEditText.setText(formattedExpiryDate);  // Set the formatted date to the EditText
            });

            // Use getParentFragmentManager() to display the dialog
            monthYearPicker.show(getParentFragmentManager(), "MonthYearPickerDialog");
        });

        // Set data to TextViews
        TextView totalSchoolFeesValueTextView = rootView.findViewById(R.id.total_school_fees_value);
        EditText studentSchoolFeesEditText = rootView.findViewById(R.id.student_school_fees);

        totalSchoolFeesValueTextView.setText(String.format(Locale.getDefault(), "RM %.2f", totalSchoolFees));

        // Set the default value for the school fees input
        studentSchoolFeesEditText.setText(String.format(Locale.getDefault(), "%.2f", totalSchoolFees));

        // Set the click listener to clear the text when the user clicks on the EditText
        studentSchoolFeesEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                studentSchoolFeesEditText.setText(""); // Clear the text immediately when focused
            }
        });

        // Ensure EditText allows only numeric input
        studentSchoolFeesEditText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        // Set the button action for submission
        Button submitButton = rootView.findViewById(R.id.submit_button);
        TextView paymentCompletedMessage = rootView.findViewById(R.id.payment_completed_message);

        // Initially hide the Generate PDF button and the message
        btnGeneratePDFReceipt = rootView.findViewById(R.id.idBtnGeneratePDF);
        btnGeneratePDFReceipt.setVisibility(View.GONE);
        paymentCompletedMessage.setVisibility(View.GONE);

        // Set the click listener for the submit button
        submitButton.setOnClickListener(v -> {
            // Check if the button is already disabled
            if (!submitButton.isEnabled()) {
                // Display a message indicating that the payment has already been received
                Toast.makeText(getContext(), "Your payment has already been received", Toast.LENGTH_SHORT).show();
                return; // Exit the method to prevent further actions
            }

            // Call your submission method
            onSubmitPaymentClick(paymentCompletedMessage, btnGeneratePDFReceipt, submitButton);
        });

        btnGeneratePDFReceipt.setOnClickListener(v -> {
            // Call the generatePDF function
            generatePDFReceipt();
        });



        // Initialize and scale the bitmap
        bmp = BitmapFactory.decodeResource(getResources(), R.drawable.owl_collage);
        scaledBmp = Bitmap.createScaledBitmap(bmp, 140, 140, false);

        return rootView;
    }



    private void showDatePickerDialog() {
        MonthYearPickerDialogFragment dialogFragment = new MonthYearPickerDialogFragment(this);
        dialogFragment.show(getParentFragmentManager(), "MonthYearPickerDialog");
    }


    @Override
    public void onDateSet(int year, int month) {
        Calendar selectedDate = Calendar.getInstance();
        selectedDate.set(year, month, 1);
        expiryDateEditText.setText(dateFormatter.format(selectedDate.getTime()));
    }

    private void onSubmitPaymentClick(TextView paymentCompletedMessage, Button btnGeneratePDFReceipt, Button submitButton) {
        // Get references to the input fields
        EditText cardNameEditText = rootView.findViewById(R.id.card_name_value);
        EditText cardNumberEditText = rootView.findViewById(R.id.card_number_edit_text);
        EditText expiryDateEditText = rootView.findViewById(R.id.expiry_date_edit_text);
        EditText totalAmountEditText = rootView.findViewById(R.id.student_school_fees);
        TextView errorTextView = rootView.findViewById(R.id.payment_error);

        // Clear previous error messages
        errorTextView.setText("");

        // Get the entered values
        String cardHolderName = cardNameEditText.getText().toString().trim();
        String cardNumber = cardNumberEditText.getText().toString().trim();
        String expiryDate = expiryDateEditText.getText().toString().trim();
        String totalAmountStr = totalAmountEditText.getText().toString().trim();

        // StringBuilder to accumulate error messages
        StringBuilder errorMessages = new StringBuilder();

        // Validate that required fields are filled in
        if (cardHolderName.isEmpty()) {
            errorMessages.append("Please enter the card holder's name.\n");
        }

        if (cardNumber.length() != 16) {
            errorMessages.append("Please enter a valid 16-digit card number.\n");
        }

        if (expiryDate.isEmpty()) {
            // build a function for user to varify the date, the date cannot be at the pass, can be today
            errorMessages.append("Please enter the card expiry date.\n");
        }

        // Initialize totalAmount with totalSchoolFees as the default value
        double totalAmount = totalSchoolFees;
        receiptTotalAmount = totalSchoolFees;

        // If the total amount is provided, parse it
        if (!totalAmountStr.isEmpty()) {
            // Remove "RM" and trim the string
            totalAmountStr = totalAmountStr.replace("RM", "").trim();

            try {
                // Convert the string to a double
                double parsedAmount = Double.parseDouble(totalAmountStr);

                // Compare the parsed amount with the totalSchoolFees
                if (parsedAmount < totalSchoolFees) {
                    errorMessages.append("You have entered an invalid value.\nYour payment amount must be larger or equal to the school fees amount.\n");
                } else {
                    // If totalAmountStr differs from totalAmount, update totalAmountStr
                    if (parsedAmount != totalAmount) {
                        totalAmount = parsedAmount;
                        totalAmountStr = String.valueOf(totalAmount); // Optional: Keep totalAmountStr updated if needed
                    }
                }
            } catch (NumberFormatException e) {
                // Handle invalid number format
                errorMessages.append("Please enter a valid total amount.\n");
            }
        }

        final double finalTotalAmount = totalAmount; // Final total amount for Firestore
        receiptTotalAmount = totalAmount;
        double totalAccountBalanced = (getTotalAmount(totalAmount) - totalSchoolFees)+ accountBalance;


        // If there are any error messages, display them and return
        if (errorMessages.length() > 0) {
            errorTextView.setText(errorMessages.toString().trim());
            return;
        }

        // Proceed with saving the payment details to Firebase Firestore
        paymentMethod = "Credit card/ Debit card";
        final Date paymentDate = fetchTodayDateAndTime();


        // Generate unique payment ID and upload payment data to Firestore
        generateUniquePaymentId(paymentId -> {
            if (paymentId != null) {
                // Prepare payment details
                Map<String, Object> paymentDetails = new HashMap<>();
                paymentDetails.put("Method", paymentMethod);
                paymentDetails.put("PaymentDate", paymentDate);
                paymentDetails.put("SchoolFees", totalSchoolFees);
                paymentDetails.put("PaidAmount", finalTotalAmount);
                paymentDetails.put("CardHolderName", cardHolderName);
                paymentDetails.put("CardNumber", cardNumber);
                paymentDetails.put("ExpiryDate", expiryDate);
                paymentDetails.put("StudentID", studentId);

                // Save payment details in Firestore
                db.collection("Payments").document(paymentId)
                        .set(paymentDetails)
                        .addOnSuccessListener(aVoid -> {
                            // Payment recorded successfully
                            Toast.makeText(getContext(), "Payment recorded successfully", Toast.LENGTH_SHORT).show();

                            // Disable the submit button after successful payment
                            submitButton.setEnabled(false);
                            submitButton.setAlpha(0.5f); // Optional: make it look disabled

                            // Show the message and Generate PDF button after successful payment
                            paymentCompletedMessage.setVisibility(View.VISIBLE);
                            btnGeneratePDFReceipt.setVisibility(View.VISIBLE);
                            paymentCompletedMessage.setText("Payment successful!"); // Set your success message

                            cardNameEditText.setEnabled(false);
                            cardNumberEditText.setEnabled(false);
                            expiryDateEditText.setEnabled(false);
                            totalAmountEditText.setEnabled(false);

                        })


                        .addOnFailureListener(e -> {
                            // Log error and show error message
                            Log.e("ProceedPayment", "Error recording payment", e);
                            errorTextView.setText("Error recording payment. Please try again.");
                        });


                // Update Firestore
                db.collection("Students").document(studentId)
                        .update("Balanced", totalAccountBalanced)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(getActivity(), "Updating balanced successfully.", Toast.LENGTH_SHORT).show();
                        })

                        .addOnFailureListener(e -> {
                            Toast.makeText(getActivity(), "Error updating profile balanced: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });

                db.collection("Students").document(studentId)
                        .update("EnrolledCourse","-")
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(getActivity(), "Updating balanced successfully.", Toast.LENGTH_SHORT).show();
                        })

                        .addOnFailureListener(e -> {
                            Toast.makeText(getActivity(), "Error updating profile balanced: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });

            } else {
                // Handle failure to generate unique payment ID
                errorTextView.setText("Failed to generate unique payment ID. Please try again.");
            }

        });
    }

    private static double getTotalAmount(double totalAmount) {
        return totalAmount;
    }

    // Define your Firebase upload logic
    private void uploadDataToFirebase(OnUploadCompleteListener listener) {
        // Your Firebase logic goes here
        // Call listener.onComplete(true) for success or listener.onComplete(false) for failure
    }

    // Listener interface for upload completion
    interface OnUploadCompleteListener {
        void onComplete(boolean success);
    }


    private void generatePDFReceipt() {
        btnGeneratePDFReceipt.setVisibility(View.VISIBLE);
        PdfDocument pdfDocument = new PdfDocument();
        Paint paint = new Paint();
        Paint title = new Paint();

        // Set all text color to black
        title.setColor(ContextCompat.getColor(getContext(), R.color.black));

        // Create the PDF page
        PdfDocument.PageInfo mypageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page myPage = pdfDocument.startPage(mypageInfo);
        Canvas canvas = myPage.getCanvas();

        // Draw the header include college logo, address, phone number, fax number and school name
        if (scaledBmp != null) {
            canvas.drawBitmap(scaledBmp, 56, 40, paint);
        }

        // Draw College Information
        title.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        title.setTextSize(15);
        canvas.drawText("No. 123, Jalan Puncak,\nTaman Seri Penang,\n11600 George Town,\nPenang, Malaysia", 209, 120, title);
        title.setTextSize(10);
        canvas.drawText("Tel: 01-2345678", 215, 145, title);
        canvas.drawText("Fax: 08-7654321", 315, 145, title);

        // Draw School Name and Underline
        title.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        title.setTextSize(20);
        canvas.drawText("OWL INTERNATIONAL COLLEGE PENANG SDN BHD", 210, 105, title);

        // Draw a black underline after the header
        Paint underlinePaint = new Paint();
        underlinePaint.setColor(ContextCompat.getColor(getContext(), R.color.blue_gray));
        underlinePaint.setStrokeWidth(2);
        canvas.drawLine(0, 200, 800, 200, underlinePaint);

        // Draw the Receipt Title
        title.setTextSize(20);
        title.setTextAlign(Paint.Align.LEFT);
        String PDFTitle = "Receipt";
        float titleX = (pageWidth - title.measureText(PDFTitle)) / 2;
        float titleY = 240;
        canvas.drawText(PDFTitle, titleX, titleY, title);

        // Underline the Receipt Title
        underlinePaint.setColor(ContextCompat.getColor(getContext(), R.color.black));
        canvas.drawLine(titleX, titleY + 5, titleX + title.measureText(PDFTitle), titleY + 5, underlinePaint);

        title.setTextSize(15);
        title.setTextAlign(Paint.Align.LEFT);

        // Set up a mutable baseY reference using an array
        final float[] baseYRef = {315};
        float textHeight = title.getFontMetrics().bottom - title.getFontMetrics().top;

        title.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));

        // Receipt No.
        float x = 0;
        canvas.drawText("Receipt No: " + paymentId, x, baseYRef[0], title);

        // Payment Date and Time
        baseYRef[0] += textHeight + 10;
        Date paymentDate = fetchTodayDateAndTime();

        // Set the payment date and time format with the Asia/Kuala_Lumpur
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy HH:mm", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Kuala_Lumpur"));
        String formattedDate = sdf.format(paymentDate);

        canvas.drawText("Payment Date and time: " + formattedDate, x, baseYRef[0], title);

        // Student ID
        baseYRef[0] += textHeight + 10;
        canvas.drawText("Student ID: " + studentId, x, baseYRef[0], title);

        // Fetch and draw Student Name and payment method
        fetchStudentName((name) -> {
            baseYRef[0] += textHeight + 10;

            canvas.drawText("Student Name: " + name, x, baseYRef[0], title);
            baseYRef[0] += textHeight + 10;

            canvas.drawText("Payment method: " + paymentMethod, x, baseYRef[0], title);
            baseYRef[0] += textHeight + 10;

            // Fetch and draw Being Payment For
            fetchBeingPaymentFor((beingPaymentFor) -> {
                if (beingPaymentFor != null) {
                    Log.d("PaymentInfo", "Being Payment For: " + beingPaymentFor);

                    // Handle text wrapping for the "Being Payment For" string
                    String fullText = "Being Payment for: " + beingPaymentFor;
                    float maxTextWidth = pageWidth - x - 40;
                    List<String> wrappedText = getWrappedText(fullText, title, maxTextWidth);

                    for (String line : wrappedText) {
                        canvas.drawText(line, x, baseYRef[0], title);
                        baseYRef[0] += textHeight + 10;
                    }

                    // Draw headers for "Particulars" and "Amount"
                    baseYRef[0] += textHeight + 10;
                    float priceX = 250;
                    canvas.drawText("Particulars", x, baseYRef[0], title);
                    canvas.drawText("Amount", priceX, baseYRef[0], title);

                    // Underline the "Particulars" header
                    float particularsX = x;
                    float particularsY = baseYRef[0];
                    underlinePaint.setColor(ContextCompat.getColor(getContext(), R.color.black));
                    underlinePaint.setStrokeWidth(2);
                    canvas.drawLine(particularsX, particularsY + 2, particularsX + title.measureText("Particulars"), particularsY + 2, underlinePaint);

                    // Underline the "Amount" header
                    canvas.drawLine(priceX, particularsY + 2, priceX + title.measureText("Amount"), particularsY + 2, underlinePaint);

                    // Draw item description "ADVANCED PAYMENT"
                    baseYRef[0] += textHeight + 10;
                    canvas.drawText("ADVANCED PAYMENT", 0, baseYRef[0], title);

                    // Display the total payment amount in bold, formatted to 2 decimal places
                    title.setTextSize(20);
                    title.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                    canvas.drawText("RM " + String.format("%.2f", receiptTotalAmount), priceX, baseYRef[0], title);

                    // Reset text style to normal for terms and conditions
                    title.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));

                    // Display the terms and conditions
                    title.setTextSize(10);
                    baseYRef[0] += textHeight + 50;
                    canvas.drawText("Note: ", particularsX, baseYRef[0], title);
                    baseYRef[0] += textHeight;
                    canvas.drawText("1. All fees and retal paid are not refundable and non-transferable", 0, baseYRef[0], title);
                    baseYRef[0] += textHeight;
                    canvas.drawText("2. Please retain this official receipt for future reference and deposit refund.", 0, baseYRef[0], title);
                    baseYRef[0] += textHeight;
                    canvas.drawText("3. For cheque payment, this official receipt is valid upon clearance of the cheque.", 0, baseYRef[0], title);

                    // Finalize the PDF generation
                    pdfDocument.finishPage(myPage);

                    // Save the PDF to file
                    File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), paymentId + "_OWL_Receipt.pdf");
                    try {
                        pdfDocument.writeTo(new FileOutputStream(file));
                        Toast.makeText(getContext(), "Receipt has been downloaded.", Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(getContext(), "Failed to generate receipt file.", Toast.LENGTH_SHORT).show();
                    } finally {
                        // Close the document
                        pdfDocument.close();
                    }
                } else {
                    Log.e("PaymentInfo", "Failed to fetch beingPaymentFor");
                }
            });
        });
    }


    // Helper function to wrap text if it's too wide
    private List<String> getWrappedText(String text, Paint paint, float maxWidth) {
        List<String> wrappedLines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            String tempLine = currentLine + (currentLine.length() == 0 ? "" : " ") + word;
            float textWidth = paint.measureText(tempLine);

            // If the current line width exceeds maxWidth, start a new line
            if (textWidth > maxWidth) {
                wrappedLines.add(currentLine.toString());
                currentLine = new StringBuilder(word); // Start the new line with the current word
            } else {
                currentLine.append(currentLine.length() == 0 ? "" : " ").append(word);
            }
        }
        // Add the last line if not empty
        if (currentLine.length() > 0) {
            wrappedLines.add(currentLine.toString());
        }

        return wrappedLines;
    }


    private boolean checkPermission() {
        int permission1 = ContextCompat.checkSelfPermission(getContext(), WRITE_EXTERNAL_STORAGE);
        int permission2 = ContextCompat.checkSelfPermission(getContext(), READ_EXTERNAL_STORAGE);
        return permission1 == PackageManager.PERMISSION_GRANTED && permission2 == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(getActivity(), new String[]{WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0) {
                boolean writeStorage = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                boolean readStorage = grantResults[1] == PackageManager.PERMISSION_GRANTED;

                if (writeStorage && readStorage) {
                    Toast.makeText(getContext(), "Permission Granted.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Permission Denied.", Toast.LENGTH_SHORT).show();
                    showPermissionExplanationDialog();
                }
            }
        }
    }

    private void showPermissionExplanationDialog() {
        new AlertDialog.Builder(getContext())
                .setTitle("Permission Required")
                .setMessage("This app requires storage permissions to function properly. Please grant the permissions in the app settings.")
                .setPositiveButton("OK", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getActivity().getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    // Fetch the student name and trigger the callback to draw on the canvas
    public void fetchStudentName(StudentNameCallback callback) {
        db.collection("Students").document(studentId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            // Get data from document snapshot
                            Map<String, Object> data = document.getData();

                            if (data != null) {
                                // Safely extract the "Name" field with null check
                                String name = data.get("Name") != null ? data.get("Name").toString() : "N/A";
                                callback.onStudentNameFetched(name); // Pass the fetched name to the callback
                            }
                        }
                    } else {
                        // Handle the failure of the task
                        Exception exception = task.getException();
                        if (exception != null) {
                            Log.e("FirestoreError", "Error fetching document", exception);
                        }
                    }
                });
    }

    // Create an interface for a callback
    public interface StudentNameCallback {
        void onStudentNameFetched(String name);
    }

    public Date fetchTodayDateAndTime() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocalDateTime now = LocalDateTime.now();

            return Date.from(now.atZone(ZoneId.systemDefault()).toInstant());
        } else {

            return new Date();
        }
    }

    public void fetchBeingPaymentFor(OnBeingPaymentForFetchedListener listener) {
        db.collection("Students").document(studentId).get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.e("FirestoreError", "Error fetching student document", task.getException());
                        listener.onFetched(null); // Pass null in case of error
                        return;
                    }

                    DocumentSnapshot studentDocument = task.getResult();
                    if (studentDocument == null || !studentDocument.exists()) {
                        Log.e("FirestoreError", "Student document does not exist");
                        listener.onFetched(null); // Pass null in case of error
                        return;
                    }

                    Map<String, Object> studentData = studentDocument.getData();
                    String programID = studentData != null && studentData.get("ProgrammeID") != null
                            ? studentData.get("ProgrammeID").toString().trim()
                            : "N/A";

                    // Use an array to hold the intakeName
                    final String[] intakeName = {"N/A"};
                    if (studentData != null && studentData.get("Intake") != null) {
                        Timestamp intakeTimestamp = (Timestamp) studentData.get("Intake");
                        Date intakeDate = intakeTimestamp.toDate();
                        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
                        intakeName[0] = dateFormat.format(intakeDate);
                    }

                    db.collection("Programmes").document(programID).get()
                            .addOnCompleteListener(programTask -> {
                                if (!programTask.isSuccessful()) {
                                    Log.e("FirestoreError", "Error fetching programme document", programTask.getException());
                                    listener.onFetched(null); // Pass null in case of error
                                    return;
                                }

                                DocumentSnapshot programDocument = programTask.getResult();
                                if (programDocument == null || !programDocument.exists()) {
                                    Log.e("FirestoreError", "Programme document does not exist");
                                    listener.onFetched(null); // Pass null in case of error
                                    return;
                                }

                                Map<String, Object> programData = programDocument.getData();
                                String programName = programData != null && programData.get("FullName") != null
                                        ? programData.get("FullName").toString().trim()
                                        : "N/A";
                                String collaborationId = programData != null && programData.get("CollaborationID") != null
                                        ? programData.get("CollaborationID").toString().trim()
                                        : "N/A";

                                db.collection("Collaboration").document(collaborationId).get()
                                        .addOnCompleteListener(collabTask -> {
                                            if (!collabTask.isSuccessful()) {
                                                Log.e("FirestoreError", "Error fetching collaboration document", collabTask.getException());
                                                listener.onFetched(null); // Pass null in case of error
                                                return;
                                            }

                                            DocumentSnapshot collabDocument = collabTask.getResult();
                                            if (collabDocument == null || !collabDocument.exists()) {
                                                Log.e("FirestoreError", "Collaboration document does not exist");
                                                listener.onFetched(null); // Pass null in case of error
                                                return;
                                            }

                                            Map<String, Object> collabData = collabDocument.getData();
                                            String collaborationName = collabData != null && collabData.get("Name") != null
                                                    ? collabData.get("Name").toString().trim()
                                                    : "N/A";
                                            String countryCode = collabData != null && collabData.get("CountryCode") != null
                                                    ? collabData.get("CountryCode").toString().trim()
                                                    : "N/A";

                                            // Generate beingPaymentFor string and convert to uppercase
                                            String beingPaymentFor = (programID + " - " + programName + "\nin collaboration with "
                                                    + collaborationName + ", " + countryCode + " / " + intakeName[0]).toUpperCase();

                                            // Pass the result back to the listener
                                            listener.onFetched(beingPaymentFor);
                                        });
                            });
                });
    }


    public interface OnBeingPaymentForFetchedListener {
        void onFetched(String beingPaymentFor);
    }
}