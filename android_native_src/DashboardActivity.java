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
    private TextView tvAssocName, tvCurrentDate, tvEndDate, tvMemberCount, tvCollected, tvRemaining, tvRecipientName, tvCurrentMonthDisplay;
    private CheckBox cbPayoutDone, cbSelectAll;
    private RecyclerView rvMembers;
    private Button btnReset;
    private ImageButton btnChat, btnPrevMonth, btnNextMonth;
    private FloatingActionButton fabAddMember;
    private DashboardAdapter adapter;
    private List<Member> memberList;

    // State
    private int currentMonthIdx; // The actual active month in DB
    private int viewingMonthIdx; // The month currently being viewed
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
        tvCurrentMonthDisplay = findViewById(R.id.tvCurrentMonthDisplay);
        
        cbPayoutDone = findViewById(R.id.cbPayoutDone);
        cbSelectAll = findViewById(R.id.cbSelectAll);
        
        rvMembers = findViewById(R.id.rvDashMembers);
        btnReset = findViewById(R.id.btnReset);
        btnChat = findViewById(R.id.btnChat);
        btnPrevMonth = findViewById(R.id.btnPrevMonth);
        btnNextMonth = findViewById(R.id.btnNextMonth);
        fabAddMember = findViewById(R.id.fabAddMember);

        rvMembers.setLayoutManager(new LinearLayoutManager(this));

        // Logic for "Done" button (Payout)
        cbPayoutDone.setOnClickListener(v -> handlePayoutClick());

        // Month Navigation
        btnPrevMonth.setOnClickListener(v -> {
            if (viewingMonthIdx > 0) {
                viewingMonthIdx--;
                refreshDashboard();
            }
        });

        btnNextMonth.setOnClickListener(v -> {
            // Allow viewing up to total duration (members count)
            // If members list is empty, we can't really navigate much, but let's assume at least 1
            int maxMonths = memberList.isEmpty() ? 0 : memberList.size() - 1;
            if (viewingMonthIdx < maxMonths) {
                viewingMonthIdx++;
                refreshDashboard();
            } else {
                Toast.makeText(this, "هذا هو الشهر الأخير في الدورة", Toast.LENGTH_SHORT).show();
            }
        });

        // Logic for "Select All" button
        cbSelectAll.setOnClickListener(v -> {
            boolean isChecked = cbSelectAll.isChecked();
            cbSelectAll.setChecked(!isChecked); // Revert visual state pending confirmation

            new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(isChecked ? "تحديد الكل" : "إلغاء تحديد الكل")
                .setMessage(isChecked ? "هل أنت متأكد من تسجيل دفع جميع الأعضاء لهذا الشهر؟" : "هل أنت متأكد من إلغاء دفع جميع الأعضاء لهذا الشهر؟")
                .setPositiveButton("نعم", (dialog, which) -> {
                    cbSelectAll.setChecked(isChecked); // Apply visual state
                    for (int i = 0; i < memberList.size(); i++) {
                        Member m = memberList.get(i);
                        if (m.isPaidForCurrentMonth() != isChecked) {
                            m.setPaidForCurrentMonth(isChecked);
                            dbHelper.setPaymentStatus(m.getId(), viewingMonthIdx, isChecked);
                            if (adapter != null) adapter.notifyItemChanged(i);
                        }
                    }
                    // Recalculate stats
                    paidCount = 0;
                    for (Member m : memberList) {
                        if (m.isPaidForCurrentMonth()) paidCount++;
                    }
                    updateSummary();
                })
                .setNegativeButton("إلغاء", null)
                .show();
        });

        btnReset.setOnClickListener(v -> {
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
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
        
        // Default viewing month to current active month
        viewingMonthIdx = currentMonthIdx;

        tvAssocName.setText(name);

        // Load Members
        memberList = dbHelper.getAllMembers();
        
        refreshDashboard();
    }

    private void refreshDashboard() {
        // Update Stats Views
        String dateStart = calculateFormattedDate(startDateStr, 0);
        
        // End Date is Start + (Members - 1) months
        int monthsDuration = memberList.isEmpty() ? 0 : memberList.size() - 1;
        String dateEnd = calculateFormattedDate(startDateStr, monthsDuration);
        
        tvCurrentDate.setText("البداية: " + dateStart);
        tvEndDate.setText("النهاية: " + dateEnd);
        tvMemberCount.setText("عدد الأعضاء: " + memberList.size());

        // Update Month Display
        String currentMonthDate = calculateFormattedDate(startDateStr, viewingMonthIdx);
        tvCurrentMonthDisplay.setText("الشهر " + (viewingMonthIdx + 1) + ": " + currentMonthDate);

        // Determine Recipient (Round Robin based on VIEWING month)
        if (!memberList.isEmpty()) {
            Member recipient = memberList.get(viewingMonthIdx % memberList.size());
            tvRecipientName.setText(recipient.getName());
        } else {
            tvRecipientName.setText("-");
        }

        // Check payment status for VIEWING month
        paidCount = 0;
        boolean allPaid = true;
        for (Member m : memberList) {
            boolean isPaid = dbHelper.isMemberPaid(m.getId(), viewingMonthIdx);
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
        
        // Update navigation buttons state
        btnPrevMonth.setEnabled(viewingMonthIdx > 0);
        btnPrevMonth.setAlpha(viewingMonthIdx > 0 ? 1.0f : 0.5f);
        
        int maxMonths = memberList.isEmpty() ? 0 : memberList.size() - 1;
        btnNextMonth.setEnabled(viewingMonthIdx < maxMonths);
        btnNextMonth.setAlpha(viewingMonthIdx < maxMonths ? 1.0f : 0.5f);
    }

    private void updateSummary() {
        double collected = paidCount * amountPerPerson;
        double total = memberList.size() * amountPerPerson;
        double remaining = total - collected;

        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("ar", "EG"));
        tvCollected.setText(nf.format(collected));
        tvRemaining.setText(nf.format(remaining));

        boolean isComplete = (remaining == 0) && !memberList.isEmpty();
        
        // Only allow "Payout" (Closing the month) if we are viewing the CURRENT ACTIVE month
        if (viewingMonthIdx == currentMonthIdx) {
            cbPayoutDone.setVisibility(View.VISIBLE);
            cbPayoutDone.setEnabled(isComplete);
            cbPayoutDone.setChecked(false); 
            
            if (!isComplete) {
                cbPayoutDone.setText("بانتظار التحصيل...");
            } else {
                cbPayoutDone.setText("تسليم الجمعية (إغلاق الشهر)");
            }
        } else {
            // If viewing history or future, hide the payout button to avoid confusion
            cbPayoutDone.setVisibility(View.GONE);
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
        android.widget.FrameLayout container = new android.widget.FrameLayout(this);
        android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        
        com.google.android.material.textfield.TextInputLayout textInputLayout = new com.google.android.material.textfield.TextInputLayout(this);
        textInputLayout.setHint("اسم العضو الجديد");
        textInputLayout.setBoxBackgroundMode(com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE);
        textInputLayout.setLayoutParams(params);
        
        com.google.android.material.textfield.TextInputEditText input = new com.google.android.material.textfield.TextInputEditText(textInputLayout.getContext());
        textInputLayout.addView(input);
        
        container.addView(textInputLayout);
        container.setPadding(60, 40, 60, 20);

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("إضافة عضو جديد")
            .setView(container)
            .setPositiveButton("إضافة", (dialog, which) -> {
                String newName = input.getText() != null ? input.getText().toString().trim() : "";
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
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
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
        android.widget.FrameLayout container = new android.widget.FrameLayout(this);
        android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        com.google.android.material.textfield.TextInputLayout textInputLayout = new com.google.android.material.textfield.TextInputLayout(this);
        textInputLayout.setHint("الاسم الجديد");
        textInputLayout.setBoxBackgroundMode(com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE);
        textInputLayout.setLayoutParams(params);

        com.google.android.material.textfield.TextInputEditText input = new com.google.android.material.textfield.TextInputEditText(textInputLayout.getContext());
        input.setText(m.getName());
        textInputLayout.addView(input);
        
        container.addView(textInputLayout);
        container.setPadding(60, 40, 60, 20);

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("تعديل الاسم")
            .setView(container)
            .setPositiveButton("حفظ", (dialog, which) -> {
                String newName = input.getText() != null ? input.getText().toString().trim() : "";
                if (!newName.isEmpty()) {
                    dbHelper.updateMemberName(m.getId(), newName);
                    loadData();
                }
            })
            .setNegativeButton("إلغاء", null)
            .show();
    }

    private void showDeleteConfirmation(Member m) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
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

            // Resolve Colors
            int colorSurface = android.graphics.Color.WHITE;
            int colorPaid = android.graphics.Color.parseColor("#E6FFFA"); // Fallback
            int colorOutline = android.graphics.Color.LTGRAY;
            int colorPrimary = android.graphics.Color.parseColor("#059669");

            android.util.TypedValue typedValue = new android.util.TypedValue();
            android.content.res.Resources.Theme theme = holder.itemView.getContext().getTheme();
            
            if (theme.resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true))
                colorSurface = typedValue.data;
            if (theme.resolveAttribute(com.google.android.material.R.attr.colorPrimaryContainer, typedValue, true))
                colorPaid = typedValue.data;
            if (theme.resolveAttribute(com.google.android.material.R.attr.colorOutlineVariant, typedValue, true))
                colorOutline = typedValue.data;
            if (theme.resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true))
                colorPrimary = typedValue.data;

            com.google.android.material.card.MaterialCardView card = (com.google.android.material.card.MaterialCardView) holder.itemView;

            if (m.isPaidForCurrentMonth()) {
                card.setCardBackgroundColor(colorPaid);
                card.setStrokeColor(colorPrimary);
                card.setStrokeWidth(2);
                holder.cbPaid.setChecked(true);
            } else {
                card.setCardBackgroundColor(colorSurface);
                card.setStrokeColor(colorOutline);
                card.setStrokeWidth(1);
                holder.cbPaid.setChecked(false);
            }

            // Recipient Highlight
            if (m.getId() == memberList.get(viewingMonthIdx % memberList.size()).getId()) {
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
                holder.cbPaid.setChecked(!isChecked); // Revert visual state pending confirmation

                String action = isChecked ? "تسجيل دفع" : "إلغاء دفع";
                String message = "هل أنت متأكد من " + action + " العضو " + m.getName() + "؟";

                new com.google.android.material.dialog.MaterialAlertDialogBuilder(holder.itemView.getContext())
                    .setTitle("تأكيد العملية")
                    .setMessage(message)
                    .setPositiveButton("نعم", (dialog, which) -> {
                        // Apply logic
                        dbHelper.setPaymentStatus(m.getId(), viewingMonthIdx, isChecked);
                        m.setPaidForCurrentMonth(isChecked);
                        
                        // Recalculate stats
                        paidCount = 0;
                        boolean allPaid = true;
                        for (Member member : memberList) {
                            if (member.isPaidForCurrentMonth()) paidCount++;
                            else allPaid = false;
                        }
                        if (memberList.isEmpty()) allPaid = false;
                        cbSelectAll.setChecked(allPaid);

                        updateSummary();
                        notifyItemChanged(position); 
                    })
                    .setNegativeButton("إلغاء", null)
                    .show();
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