package com.example.jamiya;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

    // TODO: REPLACE WITH YOUR VALID GEMINI API KEY
    private static final String API_KEY = "YOUR_API_KEY_HERE";
    private static final String MODEL_NAME = "gemini-3-pro-preview";
    
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
        adapter.addMessage(new ChatMessage("مرحباً بك! أنا مساعدك الذكي لتنظيم الجمعية. كيف يمكنني مساعدتك اليوم؟", false));

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

    private String callGeminiApi(String prompt) {
        if (API_KEY.equals("YOUR_API_KEY_HERE")) {
            return "الرجاء ضبط مفتاح API في كود التطبيق (ChatActivity.java).";
        }

        try {
            URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/" + MODEL_NAME + ":generateContent?key=" + API_KEY);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            // Construct JSON Payload
            JSONObject textPart = new JSONObject();
            textPart.put("text", prompt);
            
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