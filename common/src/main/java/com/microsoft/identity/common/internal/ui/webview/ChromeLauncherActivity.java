package com.microsoft.identity.common.internal.ui.webview;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.microsoft.identity.common.R;

public class ChromeLauncherActivity extends AppCompatActivity {

    // Declare the ActivityResultLauncher to handle Chrome's result
    private final ActivityResultLauncher<Intent> chromeActivityResultLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null) {
                        String resultData = data.getDataString();
                        // Do something with the result
                        Toast.makeText(this, "Result: " + resultData, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "No data returned", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(this, "Chrome did not return OK", Toast.LENGTH_LONG).show();
                }
            });

    // GetContent creates an ActivityResultLauncher<String> to let you pass
    // in the mime type you want to let the user select
    private final ActivityResultLauncher<String> mGetContent = registerForActivityResult(new ActivityResultContracts.GetContent(),
            new ActivityResultCallback<Uri>() {
                @Override
                public void onActivityResult(Uri uri) {
                    // Handle the returned Uri
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);  // Assuming you have a layout for this activity

        // Launch Chrome when the activity is created
        launchChrome();
    }

    private void launchChrome() {
        // Create an intent to open Chrome with a specific URL
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.example.com"));
        intent.setPackage("com.android.chrome"); // Force Chrome to be used
        // Launch Chrome and wait for the result
        chromeActivityResultLauncher.launch(intent);
    }
}
