package com.example.jamiya;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatActivity extends AppCompatActivity {

    // TODO: تأكد من وضع مفتاح API الصحيح هنا
    private static final String API_KEY = "YOUR_API_KEY_HERE";
    private static final String MODEL_NAME = "gemini-3-flash-preview";

    // تعليمات النظام: تصف التطبيق وتحدد شخصية البوت
    private static final String SYSTEM_INSTRUCTION = 
        "أنت مساعد ذكي مخصص فقط لتطبيق أندرويد يسمى 'الجمعية' (Jam'iya Manager). " +
        "مهمتك هي مساعدة المستخدمين في فهم كيفية استخدام هذا التطبيق فقط. " +
        "إذا سأل المستخدم عن أي موضوع عام (مثل الرياضة، الطقس، معلومات عامة) اعتذر بلطف وقل أنك مخصص فقط لتطبيق الجمعية.\n\n" +
        "بيانات التطبيق وكيفية استخدامه:\n" +
        "1. الغرض: التطبيق يدير الجمعيات المالية الدورية (ROSCA) ويسجل المدفوعات.\n" +
        "2. الشاشة الرئيسية (لوحة التحكم): تعرض اسم الجمعية، المبلغ المحصل، والمبلغ المتبقي.\n" +
        "3. تسجيل الدفع: يوجد قائمة بالأعضاء، يمكن وضع علامة 'صح' في المربع بجانب اسم العضو لتسجيل أنه دفع القسط لهذا الشهر.\n" +
        "4. إضافة عضو جديد: يتم ذلك عبر الزر العائم (+) الموجود أسفل الشاشة.\n" +
        "5. تعديل أو حذف عضو: يجب الضغط ضغطة مطولة (Long Press) على كارت العضو في القائمة لتظهر خيارات التعديل أو الحذف.\n" +
        "6. تسليم الجمعية: عندما يتم تحصيل المبلغ بالكامل (يصبح المتبقي 0)، يتم تفعيل مربع 'تسليم'. عند الضغط عليه وتأكيده، ينتقل التطبيق للشهر التالي.\n" +
        "7. إعادة ضبط التطبيق: يوجد زر أحمر صغير عليه حرف (R) في الأعلى، يقوم بحذف كل البيانات والبدء من جديد.\n" +
        "8. أيقونة المحادثة: هي التي نستخدمها الآن لطرح الأسئلة.\n\n" +
        "أجب باختصار وباللغة العربية.";
    
    private RecyclerView rvMessages;
    private EditText etInput;
    private Button btnSend;
    private ImageButton btnBack;
    private ChatAdapter adapter;
    private ExecutorService executorService;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        rvMessages = findViewById(R.id.rvChatMessages);
        etInput = findViewById(R.id.etChatMessage);
        btnSend = findViewById(R.id.btnSend);
        btnBack = findViewById(R.id.btnBack);

        adapter = new ChatAdapter();
        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        rvMessages.setAdapter(adapter);

        // Initial greeting
        adapter.addMessage(new ChatMessage("مرحباً بك! أنا مساعدك الخاص لتطبيق الجمعية. يمكنك سؤالي عن كيفية إضافة أعضاء، تسجيل الدفعات، أو أي شيء يخص التطبيق.", false));

        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        btnSend.setOnClickListener(v -> sendMessage());
        btnBack.setOnClickListener(v -> finish());
    }

    private void sendMessage() {
        String text = etInput.getText().toString().trim();
        if (text.isEmpty()) return;

        // 1. Show User Message
        adapter.addMessage(new ChatMessage(text, true));
        etInput.setText("");
        scrollToBottom();

        // 2. Call API in Background
        executorService.execute(() -> {
            String responseText = callGeminiApi(text);
            
            // 3. Update UI
            mainHandler.post(() -> {
                adapter.addMessage(new ChatMessage(responseText, false));
                scrollToBottom();
            });
        });
    }

    private void scrollToBottom() {
        rvMessages.smoothScrollToPosition(adapter.getItemCount() - 1);
    }

    private String callGeminiApi(String userPrompt) {
        if (API_KEY.equals("YOUR_API_KEY_HERE")) {
            return "الرجاء ضبط مفتاح API في كود التطبيق (ChatActivity.java).";
        }

        try {
            URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/" + MODEL_NAME + ":generateContent?key=" + API_KEY);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            // دمج تعليمات النظام مع سؤال المستخدم
            // هذا يجعل البوت يعرف سياق التطبيق في كل رسالة
            String combinedPrompt = SYSTEM_INSTRUCTION + "\n\nسؤال المستخدم: " + userPrompt;

            // Construct JSON Payload
            JSONObject textPart = new JSONObject();
            textPart.put("text", combinedPrompt);
            
            JSONArray parts = new JSONArray();
            parts.put(textPart);
            
            JSONObject content = new JSONObject();
            content.put("parts", parts);
            
            JSONArray contents = new JSONArray();
            contents.put(content);
            
            JSONObject payload = new JSONObject();
            payload.put("contents", contents);

            // Send Data
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = payload.toString().getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // Read Response
            int code = conn.getResponseCode();
            if (code != 200) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "utf-8"))) {
                    StringBuilder errorResponse = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    Log.e("GeminiAPI", "Error Body: " + errorResponse.toString());
                } catch (Exception e) {
                    Log.e("GeminiAPI", "Could not read error stream");
                }
                return "حدث خطأ في الاتصال: " + code;
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }

            // Parse Response
            JSONObject jsonResponse = new JSONObject(response.toString());
            JSONArray candidates = jsonResponse.getJSONArray("candidates");
            JSONObject firstCandidate = candidates.getJSONObject(0);
            JSONObject contentObj = firstCandidate.getJSONObject("content");
            JSONArray partsArray = contentObj.getJSONArray("parts");
            JSONObject partObj = partsArray.getJSONObject(0);
            
            return partObj.getString("text");

        } catch (Exception e) {
            e.printStackTrace();
            return "عذراً، حدث خطأ أثناء معالجة الطلب.";
        }
    }
}