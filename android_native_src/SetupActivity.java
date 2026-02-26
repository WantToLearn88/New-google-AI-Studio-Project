package com.example.jamiya;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class SetupActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private TextInputEditText etName, etAmount, etDate, etMemberName;
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
            String name = etMemberName.getText() != null ? etMemberName.getText().toString().trim() : "";
            if (!name.isEmpty()) {
                tempMembers.add(name);
                adapter.notifyItemInserted(tempMembers.size() - 1);
                etMemberName.setText("");
            }
        });

        btnSave.setOnClickListener(v -> saveAndStart());
    }

    private void showDatePicker() {
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("اختر تاريخ البداية")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            // MaterialDatePicker returns UTC milliseconds
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            calendar.setTimeInMillis(selection);
            
            // Format for display
            SimpleDateFormat sdfDisplay = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
            sdfDisplay.setTimeZone(TimeZone.getTimeZone("UTC"));
            etDate.setText(sdfDisplay.format(calendar.getTime()));

            // Format for DB (YYYY-MM)
            SimpleDateFormat sdfIso = new SimpleDateFormat("yyyy-MM", Locale.US);
            sdfIso.setTimeZone(TimeZone.getTimeZone("UTC"));
            selectedDateIso = sdfIso.format(calendar.getTime());
        });

        datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
    }

    private void saveAndStart() {
        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        String amountStr = etAmount.getText() != null ? etAmount.getText().toString().trim() : "";

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
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_member_setup, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            holder.tvIndex.setText(String.valueOf(position + 1));
            holder.tvName.setText(tempMembers.get(position));
        }

        @Override
        public int getItemCount() { return tempMembers.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvIndex, tvName;
            public VH(View itemView) {
                super(itemView);
                tvIndex = itemView.findViewById(R.id.tvSetupMemberIndex);
                tvName = itemView.findViewById(R.id.tvSetupMemberName);
            }
        }
    }
}