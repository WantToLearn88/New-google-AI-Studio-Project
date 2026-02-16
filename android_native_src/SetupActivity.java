package com.example.jamiya;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class SetupActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private EditText etName, etAmount, etDate, etMemberName;
    private Button btnAddMember, btnSave;
    private RecyclerView rvMembers;
    private MemberAdapter adapter;
    private List<String> tempMembers = new ArrayList<>();
    
    private String selectedDateIso = ""; // YYYY-MM format for DB

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        dbHelper = new DatabaseHelper(this);

        if (dbHelper.isSetupDone()) {
            startActivity(new Intent(this, DashboardActivity.class));
            finish();
            return;
        }

        initViews();
    }

    private void initViews() {
        etName = findViewById(R.id.etAssocName);
        etAmount = findViewById(R.id.etAmount);
        etDate = findViewById(R.id.etStartDate);
        etMemberName = findViewById(R.id.etMemberName);
        btnAddMember = findViewById(R.id.btnAddMember);
        btnSave = findViewById(R.id.btnSave);
        rvMembers = findViewById(R.id.rvSetupMembers);

        // Date Picker Logic
        etDate.setOnClickListener(v -> showDatePicker());

        // Setup RecyclerView
        adapter = new MemberAdapter();
        rvMembers.setLayoutManager(new LinearLayoutManager(this));
        rvMembers.setAdapter(adapter);

        btnAddMember.setOnClickListener(v -> {
            String name = etMemberName.getText().toString().trim();
            if (!name.isEmpty()) {
                tempMembers.add(name);
                adapter.notifyItemInserted(tempMembers.size() - 1);
                etMemberName.setText("");
            }
        });

        btnSave.setOnClickListener(v -> saveAndStart());
    }

    private void showDatePicker() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year1, monthOfYear, dayOfMonth) -> {
                    // Store as YYYY-MM for logic consistency
                    selectedDateIso = String.format(Locale.US, "%d-%02d", year1, monthOfYear + 1);
                    // Display friendly format
                    etDate.setText(String.format(Locale.US, "%d/%d/%d", dayOfMonth, monthOfYear + 1, year1));
                }, year, month, day);
        datePickerDialog.show();
    }

    private void saveAndStart() {
        String name = etName.getText().toString().trim();
        String amountStr = etAmount.getText().toString().trim();

        if (name.isEmpty() || amountStr.isEmpty() || selectedDateIso.isEmpty() || tempMembers.size() < 2) {
            Toast.makeText(this, "الرجاء إكمال البيانات وإضافة عضوين على الأقل", Toast.LENGTH_LONG).show();
            return;
        }

        dbHelper.saveSetting(DatabaseHelper.KEY_ASSOC_NAME, name);
        dbHelper.saveSetting(DatabaseHelper.KEY_AMOUNT, amountStr);
        dbHelper.saveSetting(DatabaseHelper.KEY_START_DATE, selectedDateIso);
        dbHelper.saveSetting(DatabaseHelper.KEY_CURRENT_MONTH_IDX, "0");

        for (int i = 0; i < tempMembers.size(); i++) {
            dbHelper.addMember(tempMembers.get(i), i);
        }

        startActivity(new Intent(this, DashboardActivity.class));
        finish();
    }

    class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            holder.text.setText((position + 1) + ". " + tempMembers.get(position));
        }

        @Override
        public int getItemCount() { return tempMembers.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView text;
            public VH(View itemView) {
                super(itemView);
                text = itemView.findViewById(android.R.id.text1);
            }
        }
    }
}