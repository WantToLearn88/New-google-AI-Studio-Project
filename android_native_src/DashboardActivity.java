package com.example.jamiya;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class DashboardActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private TextView tvAssocName, tvCurrentDate, tvEndDate, tvMemberCount, tvCollected, tvRemaining, tvRecipientName;
    private CheckBox cbPayoutDone, cbSelectAll;
    private RecyclerView rvMembers;
    private Button btnReset;
    private ImageButton btnChat;
    private FloatingActionButton fabAddMember;
    private DashboardAdapter adapter;
    private List<Member> memberList;

    // State
    private int currentMonthIdx;
    private double amountPerPerson;
    private String startDateStr;
    private int paidCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        dbHelper = new DatabaseHelper(this);
        initViews();
        loadData();
    }

    private void initViews() {
        tvAssocName = findViewById(R.id.tvDashAssocName);
        tvCurrentDate = findViewById(R.id.tvDashDate);
        tvEndDate = findViewById(R.id.tvDashEndDate);
        tvMemberCount = findViewById(R.id.tvMemberCount);
        tvCollected = findViewById(R.id.tvDashCollected);
        tvRemaining = findViewById(R.id.tvDashRemaining);
        tvRecipientName = findViewById(R.id.tvRecipientName);
        cbPayoutDone = findViewById(R.id.cbPayoutDone);
        cbSelectAll = findViewById(R.id.cbSelectAll);
        rvMembers = findViewById(R.id.rvDashMembers);
        btnReset = findViewById(R.id.btnReset);
        btnChat = findViewById(R.id.btnChat);
        fabAddMember = findViewById(R.id.fabAddMember);

        rvMembers.setLayoutManager(new LinearLayoutManager(this));

        // Logic for "Done" button (Payout)
        cbPayoutDone.setOnClickListener(v -> handlePayoutClick());

        // Logic for "Select All" button
        cbSelectAll.setOnClickListener(v -> {
            boolean isChecked = cbSelectAll.isChecked();
            for (int i = 0; i < memberList.size(); i++) {
                Member m = memberList.get(i);
                if (m.isPaidForCurrentMonth() != isChecked) {
                    m.setPaidForCurrentMonth(isChecked);
                    dbHelper.setPaymentStatus(m.getId(), currentMonthIdx, isChecked);
                    if (adapter != null) adapter.notifyItemChanged(i);
                }
            }
            // Recalculate stats
            paidCount = 0;
            for (Member m : memberList) {
                if (m.isPaidForCurrentMonth()) paidCount++;
            }
            updateSummary();
        });

        btnReset.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("حذف الجمعية")
                .setMessage("هل أنت متأكد؟ سيتم حذف جميع البيانات والبدء من جديد.")
                .setPositiveButton("حذف", (d, w) -> {
                    dbHelper.resetAllData();
                    startActivity(new Intent(this, SetupActivity.class));
                    finish();
                })
                .setNegativeButton("إلغاء", null)
                .show();
        });

        fabAddMember.setOnClickListener(v -> showAddMemberDialog());
        
        btnChat.setOnClickListener(v -> {
            startActivity(new Intent(this, ChatActivity.class));
        });
    }

    private void loadData() {
        // Load Settings
        String name = dbHelper.getSetting(DatabaseHelper.KEY_ASSOC_NAME);
        startDateStr = dbHelper.getSetting(DatabaseHelper.KEY_START_DATE);
        String amountStr = dbHelper.getSetting(DatabaseHelper.KEY_AMOUNT);
        amountPerPerson = Double.parseDouble(amountStr);
        currentMonthIdx = dbHelper.getCurrentMonthIndex();

        tvAssocName.setText(name);

        // Load Members
        memberList = dbHelper.getAllMembers();
        
        // Update Stats Views
        String dateStart = calculateFormattedDate(startDateStr, 0);
        
        // End Date is Start + (Members - 1) months
        int monthsDuration = memberList.isEmpty() ? 0 : memberList.size() - 1;
        String dateEnd = calculateFormattedDate(startDateStr, monthsDuration);
        
        tvCurrentDate.setText("البداية: " + dateStart);
        tvEndDate.setText("النهاية: " + dateEnd);
        tvMemberCount.setText("عدد الأعضاء: " + memberList.size());

        // Determine Recipient (Round Robin)
        if (!memberList.isEmpty()) {
            Member recipient = memberList.get(currentMonthIdx % memberList.size());
            tvRecipientName.setText(recipient.getName());
        } else {
            tvRecipientName.setText("-");
        }

        // Check payment status for this month
        paidCount = 0;
        boolean allPaid = true;
        for (Member m : memberList) {
            boolean isPaid = dbHelper.isMemberPaid(m.getId(), currentMonthIdx);
            m.setPaidForCurrentMonth(isPaid);
            if (isPaid) paidCount++;
            else allPaid = false;
        }
        
        if (memberList.isEmpty()) allPaid = false;
        cbSelectAll.setChecked(allPaid);

        updateSummary();

        // Bind Adapter
        if (adapter == null) {
            adapter = new DashboardAdapter();
            rvMembers.setAdapter(adapter);
        } else {
            adapter.notifyDataSetChanged();
        }
    }

    private void updateSummary() {
        double collected = paidCount * amountPerPerson;
        double total = memberList.size() * amountPerPerson;
        double remaining = total - collected;

        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("ar", "EG"));
        tvCollected.setText(nf.format(collected));
        tvRemaining.setText(nf.format(remaining));

        boolean isComplete = (remaining == 0) && !memberList.isEmpty();
        cbPayoutDone.setEnabled(isComplete);
        cbPayoutDone.setChecked(false); 
        
        if (!isComplete) {
            cbPayoutDone.setText("بانتظار التحصيل...");
        } else {
            cbPayoutDone.setText("تسليم الجمعية (إغلاق الشهر)");
        }
    }

    private void handlePayoutClick() {
        if (!cbPayoutDone.isChecked()) return;

        new AlertDialog.Builder(this)
            .setTitle("تأكيد التسليم")
            .setMessage("هل تم تسليم المبلغ بالكامل؟ سيتم الانتقال للشهر التالي.")
            .setPositiveButton("نعم، تم التسليم", (d, w) -> {
                dbHelper.incrementCurrentMonth();
                Toast.makeText(this, "بدء الشهر الجديد", Toast.LENGTH_SHORT).show();
                recreate(); 
            })
            .setNegativeButton("إلغاء", (d, w) -> cbPayoutDone.setChecked(false))
            .show();
    }

    private String calculateFormattedDate(String start, int offset) {
        try {
            SimpleDateFormat sdfInput = new SimpleDateFormat("yyyy-MM", Locale.US);
            Calendar cal = Calendar.getInstance();
            cal.setTime(sdfInput.parse(start));
            cal.add(Calendar.MONTH, offset);
            SimpleDateFormat sdfOutput = new SimpleDateFormat("d/M/yyyy", Locale.US);
            return sdfOutput.format(cal.getTime());
        } catch (Exception e) { return start; }
    }

    private void showAddMemberDialog() {
        final EditText input = new EditText(this);
        input.setHint("اسم العضو الجديد");
        input.setPadding(32, 32, 32, 32);

        new AlertDialog.Builder(this)
            .setTitle("إضافة عضو جديد")
            .setView(input)
            .setPositiveButton("إضافة", (dialog, which) -> {
                String newName = input.getText().toString().trim();
                if (!newName.isEmpty()) {
                    // Calculate next order
                    int nextOrder = 0;
                    if (!memberList.isEmpty()) {
                         nextOrder = memberList.get(memberList.size() - 1).getOrder() + 1;
                    }
                    dbHelper.addMember(newName, nextOrder);
                    loadData();
                    Toast.makeText(this, "تم إضافة العضو", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("إلغاء", null)
            .show();
    }

    private void showEditDeleteDialog(Member m) {
        String[] options = {"تعديل الاسم", "حذف العضو"};
        new AlertDialog.Builder(this)
            .setTitle(m.getName())
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    showRenameDialog(m);
                } else {
                    showDeleteConfirmation(m);
                }
            })
            .show();
    }

    private void showRenameDialog(Member m) {
        final EditText input = new EditText(this);
        input.setText(m.getName());
        input.setPadding(32, 32, 32, 32);

        new AlertDialog.Builder(this)
            .setTitle("تعديل الاسم")
            .setView(input)
            .setPositiveButton("حفظ", (dialog, which) -> {
                String newName = input.getText().toString().trim();
                if (!newName.isEmpty()) {
                    dbHelper.updateMemberName(m.getId(), newName);
                    loadData();
                }
            })
            .setNegativeButton("إلغاء", null)
            .show();
    }

    private void showDeleteConfirmation(Member m) {
        new AlertDialog.Builder(this)
            .setTitle("حذف العضو")
            .setMessage("هل أنت متأكد؟ سيتم حذف هذا العضو من القائمة نهائياً.")
            .setPositiveButton("حذف", (dialog, which) -> {
                dbHelper.deleteMember(m.getId());
                loadData();
                Toast.makeText(this, "تم الحذف", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("إلغاء", null)
            .show();
    }

    // --- Adapter ---
    class DashboardAdapter extends RecyclerView.Adapter<DashboardAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_member_dashboard, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Member m = memberList.get(position);
            
            holder.tvName.setText(m.getName());
            
            // Set Order Badge
            holder.tvOrderBadge.setText(String.valueOf(m.getOrder() + 1));

            String payoutDate = calculateFormattedDate(startDateStr, m.getOrder());
            holder.tvDate.setText("موعد القبض: " + payoutDate);

            if (m.isPaidForCurrentMonth()) {
                holder.itemView.setBackgroundResource(R.drawable.bg_card_paid);
                holder.cbPaid.setChecked(true);
            } else {
                holder.itemView.setBackgroundResource(R.drawable.bg_card);
                holder.cbPaid.setChecked(false);
            }

            // Recipient Highlight
            if (m.getId() == memberList.get(currentMonthIdx % memberList.size()).getId()) {
                holder.tvRole.setVisibility(View.VISIBLE);
                holder.ivRoleBookmark.setVisibility(View.VISIBLE);
                holder.tvRole.setText("المستلم هذا الشهر");
            } else {
                holder.tvRole.setVisibility(View.GONE);
                holder.ivRoleBookmark.setVisibility(View.GONE);
            }

            // Click Handlers
            holder.cbPaid.setOnClickListener(v -> {
                boolean isChecked = holder.cbPaid.isChecked();
                dbHelper.setPaymentStatus(m.getId(), currentMonthIdx, isChecked);
                m.setPaidForCurrentMonth(isChecked);
                paidCount = isChecked ? paidCount + 1 : paidCount - 1;
                
                // Update Select All Checkbox state
                boolean allPaid = true;
                for (Member member : memberList) {
                    if (!member.isPaidForCurrentMonth()) {
                        allPaid = false;
                        break;
                    }
                }
                cbSelectAll.setChecked(allPaid);

                updateSummary();
                notifyItemChanged(position); 
            });

            // Long Press for Edit/Delete
            holder.itemView.setOnLongClickListener(v -> {
                showEditDeleteDialog(m);
                return true;
            });
        }

        @Override
        public int getItemCount() { return memberList.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvDate, tvRole, tvOrderBadge;
            ImageView ivRoleBookmark;
            CheckBox cbPaid;
            public VH(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvMemberDashName);
                tvDate = itemView.findViewById(R.id.tvMemberDashDate);
                tvRole = itemView.findViewById(R.id.tvMemberRole);
                tvOrderBadge = itemView.findViewById(R.id.tvMemberOrderBadge);
                ivRoleBookmark = itemView.findViewById(R.id.ivRoleBookmark);
                cbPaid = itemView.findViewById(R.id.cbMemberPaid);
            }
        }
    }
}