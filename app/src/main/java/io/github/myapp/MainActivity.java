package io.github.myapp;

import android.app.*;
import android.os.*;
import android.content.*;
import android.view.*;
import android.widget.*;

public class MainActivity extends Activity
{
    private EditText etApiUrl;
    private EditText etApiKey;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        etApiUrl = (EditText) findViewById(R.id.et_api_url);
        etApiKey = (EditText) findViewById(R.id.et_api_key);
        tvStatus = (TextView) findViewById(R.id.tv_status);

        SharedPreferences sp = getSharedPreferences("config", MODE_PRIVATE);
        etApiUrl.setText(sp.getString("api_url", ""));
        etApiKey.setText(sp.getString("api_key", ""));

        Button btnSave = (Button) findViewById(R.id.btn_save);
        btnSave.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                String url = etApiUrl.getText().toString().trim();
                String key = etApiKey.getText().toString().trim();
                SharedPreferences s = getSharedPreferences("config", MODE_PRIVATE);
                SharedPreferences.Editor editor = s.edit();
                editor.putString("api_url", url);
                editor.putString("api_key", key);
                editor.apply();
                tvStatus.setText(R.string.saved);
                startActivity(new Intent(MainActivity.this, ChatActivity.class));
            }
        });

        Button btnChat = (Button) findViewById(R.id.btn_chat);
        btnChat.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                startActivity(new Intent(MainActivity.this, ChatActivity.class));
            }
        });
    }
}
