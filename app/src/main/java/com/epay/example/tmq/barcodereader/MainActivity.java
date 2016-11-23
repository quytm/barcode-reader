package com.epay.example.tmq.barcodereader;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.edwardvanraak.materialbarcodescanner.MaterialBarcodeScanner;
import com.edwardvanraak.materialbarcodescanner.MaterialBarcodeScannerBuilder;
import com.google.android.gms.vision.barcode.Barcode;

import java.io.IOException;

public class MainActivity extends AppCompatActivity{

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int MSG_SCAN_RESULT = 1;

    private static final int COUNT_SCAN_LIMIT = 3;
    private static final int CAMERA_REQUEST = 1232;

    private Barcode barcodeResult;

    private TextView tvResult;
    private Button btnScan;

    private int countScan;
    private String [] arrResultScan;
    private Uri photoUri;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();

        countScan = 0;
        arrResultScan = new String[COUNT_SCAN_LIMIT];
    }


    private void initViews(){
        btnScan = (Button) findViewById(R.id.btn_scan);
        tvResult = (TextView) findViewById(R.id.tv_scan_text);

        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startScan();
                btnScan.setEnabled(false);
            }
        });
    }

    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case MSG_SCAN_RESULT:
                    if (countScan < COUNT_SCAN_LIMIT) {
                        tvResult.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                startScan();
                            }
                        }, 3000);
                    } else {
                        btnScan.setEnabled(true);
                        countScan = 0;
                        String value = "";
                        for (int i=0; i<COUNT_SCAN_LIMIT; i++){
                            value += ("Scan " + (i+1) + ": " + arrResultScan[i] + "\n");
                        }

                        if (!equalResult()) tvResult.setText(value + "\t=> Failed");
                        else {
                            tvResult.setText(value + "\t=> Success!");
                            btnScan.setText(getString(R.string.txt_cap_picture));

                            btnScan.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    pickImageFromCamera();
                                }
                            });
                        }
                    }
                    break;
            }
        }
    };

    private void startScan() {
        final MaterialBarcodeScanner materialBarcodeScanner = new MaterialBarcodeScannerBuilder()
                .withActivity(MainActivity.this)
                .withEnableAutoFocus(true)
                .withBleepEnabled(true)
                .withBackfacingCamera()
                .withText("Scanning...")
                .withCenterTracker()
                .withResultListener(new MaterialBarcodeScanner.OnResultListener() {
                    @Override
                    public void onResult(Barcode barcode) {
                        Log.i(TAG, "Code = " + barcode.rawValue);
                        arrResultScan[countScan] = barcode.rawValue;
                        countScan++;

                        handler.sendEmptyMessage(MSG_SCAN_RESULT);
                    }
                })
                .build();
        materialBarcodeScanner.startScan();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != MaterialBarcodeScanner.RC_HANDLE_CAMERA_PERM) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }
        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startScan();
            return;
        }
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Error")
                .setMessage(R.string.no_camera_permission)
                .setPositiveButton(android.R.string.ok, listener)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAMERA_REQUEST && resultCode == RESULT_OK) {
            try {
//                Bitmap photo = MediaStore.Images.Media.getBitmap(getContentResolver(), data.getData());
                Bitmap photo = MediaStore.Images.Media.getBitmap(getContentResolver(), photoUri);
                ImageView ivCapPicture = (ImageView) findViewById(R.id.iv_cap_picture);
                ivCapPicture.setImageBitmap(photo);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // ---------------------------------------------------------------------------------------------

    private boolean equalResult(){
        String result1 = arrResultScan[0];
        for (int i=1; i<COUNT_SCAN_LIMIT; i++){
            if (!arrResultScan[i].equals(result1)) return false;
        }

        return true;
    }

    private void pickImageFromCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "New Picture");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From your Camera");
        photoUri = getContentResolver().insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);

        startActivityForResult(intent, CAMERA_REQUEST);
    }
}
