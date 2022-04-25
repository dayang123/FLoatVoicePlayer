package com.yang.floatvoiceplayer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 11;
    private TextView openFileButton;

    @Override
    protected void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.acticity_main);
        initView();
    }

    private void initView() {
        openFileButton = findViewById(R.id.open_file_button);
        openFileButton.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("audio/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
            startActivityForResult(Intent.createChooser(intent, "选择音频文件"), REQUEST_CODE);
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK && requestCode == REQUEST_CODE){
            Uri uri = data.getData();
            if(uri != null) {
                PlayActivity.start(this, uri);
            } else {
                Toast.makeText(this, "资源文件为空", Toast.LENGTH_SHORT).show();
            }
        }

    }
}
