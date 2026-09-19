package io.github.myapp;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 聊天页：读取已保存的 API 地址与 Key，按 OpenAI 兼容协议
 * POST {api_url}/chat/completions（Authorization: Bearer <key>）。
 */
public class ChatActivity extends Activity {

    private static final String MODEL = "deepseek-v4-pro";

    private LinearLayout llMessages;
    private ScrollView scroll;
    private EditText etInput;
    private Button btnSend;

    private String apiUrl;
    private String apiKey;
    private final JSONArray history = new JSONArray();
    private final Handler ui = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        SharedPreferences sp = getSharedPreferences("config", MODE_PRIVATE);
        apiUrl = sp.getString("api_url", "").trim();
        apiKey = sp.getString("api_key", "").trim();
        if (apiUrl.length() == 0 || apiKey.length() == 0) {
            Toast.makeText(this, R.string.please_config, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        llMessages = (LinearLayout) findViewById(R.id.ll_messages);
        scroll = (ScrollView) findViewById(R.id.scroll);
        etInput = (EditText) findViewById(R.id.et_input);
        btnSend = (Button) findViewById(R.id.btn_send);
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { sendMessage(); }
        });

        addBubble("系统", "已连接：" + apiUrl + "\n输入消息开始对话。", Color.parseColor("#ECEFF1"));
    }

    private void sendMessage() {
        final String text = etInput.getText().toString().trim();
        if (text.length() == 0) return;
        etInput.setText("");
        btnSend.setEnabled(false);
        addBubble("我", text, Color.parseColor("#DCEDC8"));

        try {
            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            userMsg.put("content", text);
            history.put(userMsg);
        } catch (Exception e) {
            btnSend.setEnabled(true);
            addBubble("错误", e.getMessage(), Color.parseColor("#FFCDD2"));
            return;
        }

        new Thread(new Runnable() {
            @Override public void run() {
                String result;
                try {
                    result = callChatApi();
                } catch (final Exception e) {
                    result = "请求失败：" + e.getMessage();
                }
                final String reply = result;
                ui.post(new Runnable() {
                    @Override public void run() {
                        try {
                            JSONObject a = new JSONObject();
                            a.put("role", "assistant");
                            a.put("content", reply);
                            history.put(a);
                        } catch (Exception ignore) { }
                        addBubble("AI", reply, Color.parseColor("#E3F2FD"));
                        btnSend.setEnabled(true);
                    }
                });
            }
        }).start();
    }

    private String callChatApi() throws Exception {
        String endpoint = apiUrl;
        if (!endpoint.endsWith("/chat/completions")) {
            if (!endpoint.endsWith("/")) endpoint += "/";
            endpoint += "chat/completions";
        }

        JSONObject body = new JSONObject();
        body.put("model", MODEL);
        body.put("messages", history);
        byte[] data = body.toString().getBytes("UTF-8");

        HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(60000);
        conn.setDoOutput(true);
        conn.getOutputStream().write(data);
        conn.getOutputStream().flush();

        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        StringBuilder sb = new StringBuilder();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
        } finally {
            if (br != null) try { br.close(); } catch (Exception ignore) { }
        }
        conn.disconnect();

        String resp = sb.toString();
        if (code < 200 || code >= 300) {
            return "HTTP " + code + "：" + resp;
        }
        JSONObject root = new JSONObject(resp);
        JSONArray choices = root.optJSONArray("choices");
        if (choices != null && choices.length() > 0) {
            JSONObject msg = choices.getJSONObject(0).optJSONObject("message");
            if (msg != null) {
                String content = msg.optString("content", "");
                if (content.length() > 0) return content;
            }
            JSONObject first = choices.getJSONObject(0);
            String text = first.optString("text", "");
            if (text.length() > 0) return text;
        }
        return resp;
    }

    private void addBubble(String who, String text, int color) {
        TextView tv = new TextView(this);
        tv.setText(who + "：" + text);
        tv.setPadding(28, 24, 28, 24);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 16, 0, 16);
        tv.setLayoutParams(lp);
        tv.setBackgroundColor(color);
        llMessages.addView(tv);
        scroll.post(new Runnable() {
            @Override public void run() {
                scroll.fullScroll(View.FOCUS_DOWN);
            }
        });
    }
}
