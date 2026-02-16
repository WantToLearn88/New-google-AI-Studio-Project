package com.example.jamiya;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private ListView listViewMembers;
    private TextView tvSummary;
    private MemberAdapter adapter;
    private List<Member> memberList;
    
    // For demo purposes, we assume we are viewing Month 0
    private int currentMonthIndex = 0;
    private double amountPerPerson = 1000.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        listViewMembers = findViewById(R.id.listViewMembers);
        tvSummary = findViewById(R.id.tvSummary);

        // Seed some data if empty (for testing)
        if (dbHelper.getAllMembers().isEmpty()) {
            dbHelper.addMember("أحمد محمد", 0);
            dbHelper.addMember("سارة علي", 1);
            dbHelper.addMember("خالد حسن", 2);
        }

        loadData();
    }

    private void loadData() {
        memberList = dbHelper.getAllMembers();
        
        // Populate paid status
        int paidCount = 0;
        for (Member m : memberList) {
            boolean paid = dbHelper.isMemberPaid(m.getId(), currentMonthIndex);
            m.setPaidForCurrentMonth(paid);
            if (paid) paidCount++;
        }

        updateSummary(paidCount);

        adapter = new MemberAdapter(this, memberList);
        listViewMembers.setAdapter(adapter);
    }

    private void updateSummary(int paidCount) {
        double collected = paidCount * amountPerPerson;
        double total = memberList.size() * amountPerPerson;
        tvSummary.setText("تم تحصيل: " + collected + " / المتبقي: " + (total - collected));
    }

    // Custom Adapter for the ListView
    private class MemberAdapter extends ArrayAdapter<Member> {
        public MemberAdapter(Context context, List<Member> members) {
            super(context, 0, members);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_member, parent, false);
            }

            final Member member = getItem(position);
            
            TextView tvName = convertView.findViewById(R.id.tvMemberName);
            TextView tvOrder = convertView.findViewById(R.id.tvMemberOrder);
            CheckBox cbPaid = convertView.findViewById(R.id.cbPaid);

            tvName.setText(member.getName());
            tvOrder.setText("الدور: " + (member.getOrder() + 1));
            
            // Remove listener before setting state to avoid infinite loop
            cbPaid.setOnCheckedChangeListener(null);
            cbPaid.setChecked(member.isPaidForCurrentMonth());

            cbPaid.setOnCheckedChangeListener((buttonView, isChecked) -> {
                dbHelper.setPaymentStatus(member.getId(), currentMonthIndex, isChecked);
                member.setPaidForCurrentMonth(isChecked);
                
                // Recalculate summary
                int paidCount = 0;
                for (Member m : memberList) {
                    if (m.isPaidForCurrentMonth()) paidCount++;
                }
                updateSummary(paidCount);
                
                Toast.makeText(getContext(), isChecked ? "تم الدفع" : "تم إلغاء الدفع", Toast.LENGTH_SHORT).show();
            });

            return convertView;
        }
    }
}
