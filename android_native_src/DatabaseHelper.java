package com.example.jamiya;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "Jamiya.db";
    private static final int DATABASE_VERSION = 2;

    // Table: Members
    private static final String TABLE_MEMBERS = "members";
    private static final String COL_ID = "id";
    private static final String COL_NAME = "name";
    private static final String COL_ORDER = "payout_order";

    // Table: Payments
    private static final String TABLE_PAYMENTS = "payments";
    private static final String COL_MEMBER_ID = "member_id";
    private static final String COL_MONTH_INDEX = "month_index";
    private static final String COL_IS_PAID = "is_paid";

    // Table: Settings
    private static final String TABLE_SETTINGS = "settings";
    private static final String COL_KEY = "key";
    private static final String COL_VALUE = "value";

    // Keys
    public static final String KEY_ASSOC_NAME = "assoc_name";
    public static final String KEY_START_DATE = "start_date"; // YYYY-MM
    public static final String KEY_AMOUNT = "amount";
    public static final String KEY_CURRENT_MONTH_IDX = "current_month_idx";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createMembers = "CREATE TABLE " + TABLE_MEMBERS + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_NAME + " TEXT, " +
                COL_ORDER + " INTEGER)";
        
        String createPayments = "CREATE TABLE " + TABLE_PAYMENTS + " (" +
                COL_MEMBER_ID + " INTEGER, " +
                COL_MONTH_INDEX + " INTEGER, " +
                COL_IS_PAID + " INTEGER, " +
                "PRIMARY KEY (" + COL_MEMBER_ID + ", " + COL_MONTH_INDEX + "))";

        String createSettings = "CREATE TABLE " + TABLE_SETTINGS + " (" +
                COL_KEY + " TEXT PRIMARY KEY, " +
                COL_VALUE + " TEXT)";

        db.execSQL(createMembers);
        db.execSQL(createPayments);
        db.execSQL(createSettings);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MEMBERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PAYMENTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SETTINGS);
        onCreate(db);
    }

    // --- Settings Operations ---

    public void saveSetting(String key, String value) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_KEY, key);
        values.put(COL_VALUE, value);
        db.replace(TABLE_SETTINGS, null, values);
    }

    public String getSetting(String key) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_SETTINGS, new String[]{COL_VALUE}, COL_KEY + "=?", new String[]{key}, null, null, null);
        String val = null;
        if (cursor.moveToFirst()) val = cursor.getString(0);
        cursor.close();
        return val;
    }

    public boolean isSetupDone() {
        return getSetting(KEY_ASSOC_NAME) != null;
    }

    public void resetAllData() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_MEMBERS, null, null);
        db.delete(TABLE_PAYMENTS, null, null);
        db.delete(TABLE_SETTINGS, null, null);
    }

    // --- Member Operations ---

    public void addMember(String name, int order) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_NAME, name);
        values.put(COL_ORDER, order);
        db.insert(TABLE_MEMBERS, null, values);
    }

    public void updateMemberName(int id, String newName) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_NAME, newName);
        db.update(TABLE_MEMBERS, values, COL_ID + "=?", new String[]{String.valueOf(id)});
    }

    public void deleteMember(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_MEMBERS, COL_ID + "=?", new String[]{String.valueOf(id)});
        db.delete(TABLE_PAYMENTS, COL_MEMBER_ID + "=?", new String[]{String.valueOf(id)});
    }

    public List<Member> getAllMembers() {
        List<Member> members = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_MEMBERS + " ORDER BY " + COL_ORDER + " ASC", null);

        if (cursor.moveToFirst()) {
            do {
                Member member = new Member();
                member.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID)));
                member.setName(cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME)));
                member.setOrder(cursor.getInt(cursor.getColumnIndexOrThrow(COL_ORDER)));
                members.add(member);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return members;
    }

    // --- Payment Operations ---

    public boolean isMemberPaid(int memberId, int monthIndex) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COL_IS_PAID + " FROM " + TABLE_PAYMENTS + 
                " WHERE " + COL_MEMBER_ID + "=? AND " + COL_MONTH_INDEX + "=?", 
                new String[]{String.valueOf(memberId), String.valueOf(monthIndex)});
        
        boolean paid = false;
        if (cursor.moveToFirst()) {
            paid = cursor.getInt(0) == 1;
        }
        cursor.close();
        return paid;
    }

    public void setPaymentStatus(int memberId, int monthIndex, boolean isPaid) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_MEMBER_ID, memberId);
        values.put(COL_MONTH_INDEX, monthIndex);
        values.put(COL_IS_PAID, isPaid ? 1 : 0);
        db.replace(TABLE_PAYMENTS, null, values);
    }

    // --- Logic Helpers ---
    public int getCurrentMonthIndex() {
        String val = getSetting(KEY_CURRENT_MONTH_IDX);
        return val == null ? 0 : Integer.parseInt(val);
    }

    public void incrementCurrentMonth() {
        int current = getCurrentMonthIndex();
        saveSetting(KEY_CURRENT_MONTH_IDX, String.valueOf(current + 1));
    }
}