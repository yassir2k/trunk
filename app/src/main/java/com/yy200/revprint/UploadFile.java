package com.yy200.revprint;

/*
* Acknowledgement
* Some of the codes here were adopted from
* https://stackoverflow.com/questions/7856959/android-file-chooser
* */
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.pdf.PdfDocument;


import com.itextpdf.text.io.RandomAccessSourceFactory;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.RandomAccessFileOrArray;
import  com.yy200.revprint.FileUtils;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URISyntaxException;
import java.net.URL;

import static android.content.ContentValues.TAG;
import static java.lang.String.*;

/**
 * Created by Yassir on 3/29/2018.
 */

public class UploadFile extends Activity {

    String docpath = "";
    String user = "";
    Double price_color = 0.0;
    Double price_bw = 0.0;
    String kiosk = "";
    private static String fullPath = "";
    private android.view.View.OnClickListener View;
    private static final int FILE_SELECT_CODE = 0;
    TextView infotxt;
    Button proceedBtn;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.file_chooser);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        Button button = (Button) findViewById(R.id.btnSelectFile);
        infotxt = (TextView)findViewById(R.id.infotxt);
        proceedBtn = (Button)findViewById(R.id.btnProceed);
        proceedBtn.setEnabled(false);
        infotxt.setText("File path: ");
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFileChooser();
            }
        });
        proceedBtn.setOnClickListener(new View.OnClickListener(){
          @Override
          public void onClick(View v) {
              toPrintSettings();
          }
        });

        Intent getPrev = getIntent();
        Bundle xtraB = getPrev.getExtras();
        if(!xtraB.isEmpty())//Means there is data
        {
            user = xtraB.getString("user");
            kiosk = xtraB.getString("kiosk");
            price_color = xtraB.getDouble("colorPrice");
            price_bw = xtraB.getDouble("bwPrice");

        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.logoutMenu:
                Toast.makeText(getApplicationContext(), "Item 1 Selected", Toast.LENGTH_LONG).show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    private void toPrintSettings()
    {
        //passing data from this activity to PrintSettings Activity


        Bundle bundle = new Bundle();
        bundle.putDouble("colorPrice", price_color);
        bundle.putDouble("bwPrice", price_bw);
        bundle.putString("kiosk", kiosk);
        bundle.putString("user", user);
        Intent intent = new Intent(getApplicationContext(), PrintSettings.class);
        intent.putExtras(bundle);
        intent.setData(Uri.parse(docpath));
        startActivity(intent);
    }
    private void showFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(
                    Intent.createChooser(intent, "Select a File to Upload..."),
                    FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            // Potentially direct the user to the Market with a Dialog
            Toast.makeText(this, "Please install a File Manager.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_SELECT_CODE && resultCode == Activity.RESULT_OK && data.getData()!= null && data!=null) {
            Uri uri = data.getData();
            //Log.d(TAG, "File Uri: " + uri.toString());
            //Log.d("ABCD","encoded uri: "+  uri.getEncodedPath());
            // Get the path
           // String path = null;
            try {
                   String path = FileUtils.getPath(this, uri);
                   if(infotxt.getText() != null){
                   String mainPath = Environment.getExternalStorageDirectory().toString() + "//";

                   proceedBtn.setEnabled(true);
                   proceedBtn.setBackgroundColor(0xffffa700);
                   File fl = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
                   File testfile = new File(fl,path);
                   if(testfile.exists()) {
                       docpath = testfile.getAbsolutePath();
                       infotxt.setText("Path: " + docpath);
                       try {
                           RandomAccessFileOrArray filex = new RandomAccessFileOrArray(testfile.toString(), false, true );
                           PdfReader reader = new PdfReader(String.valueOf(filex));
                           int ret = reader.getNumberOfPages();
                           reader.close();
                           Log.d(TAG, "Pages: " + ret);
                           Toast.makeText(this, "Number of Pages: " + ret, Toast.LENGTH_LONG).show();
                       } catch (IOException e) {
                           e.printStackTrace();
                       }
                   }
                   else
                   {
                       Toast.makeText(this, "The file doesn't exist. :(" , Toast.LENGTH_SHORT).show();
                   }
               }else{
                   infotxt.setText("Select file ..");
               }
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }else{
               // Log.d("ABCD","cancel");
            Toast.makeText(this, "Data is unreadable." , Toast.LENGTH_SHORT).show();
            }
        }

}
