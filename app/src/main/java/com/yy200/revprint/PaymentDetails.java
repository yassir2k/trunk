package com.yy200.revprint;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.Toast;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.apache.poi.util.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.Date;
import java.util.Locale;
import java.util.stream.Collectors;
/*
Acknowledgement that the codes below were adopted from
https://github.com/probelalkhan/android-paypal-integration-example/blob/master/app/src/main/java/net/simplifiedcoding/paypalintegration/ConfirmationActivity.java
*/


public class PaymentDetails extends AppCompatActivity {

    private static final String BASE_URL_TRANS = "http://52.56.85.16:8080/revPrintREST_/rest/demo/savetransaction";
    private static final String BASE_URL_POOL = "http://52.56.85.16:8080/revPrintREST_/rest/demo/savejob";
    //Server path for uploading file via SSH.
    // It gets modified below according to kiosk where transaction is to be conducted
    private String ServerPath = "";
    private  String paypalID = ""; //We'll use it as our trans id and in document renaming.
    String FileExtension = "";
    ProgressDialog prgDialog;
    String user = "";
    String kiosk = "";
    Double price = 0.0;
    String docpath = "";
    int number_of_copies;
    String paper_size = "";
    String print_orientation = "";
    String color_option = "";
    String instruct_note = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //The below 2 lines were copied from
        //https://stackoverflow.com/questions/22395417/error-strictmodeandroidblockguardpolicy-onnetwork
        //SSH Connections are by default prevented on Android
        //You need special permission to permit them hence, below
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        setContentView(R.layout.payment_confirmation_layout);

        //Getting Intent
        Intent intent = getIntent();
        try {
            JSONObject jsonDetails = new JSONObject(intent.getStringExtra("PaymentDetails"));

            //Displaying payment details
            showDetails(jsonDetails.getJSONObject("response"), intent.getStringExtra("PaymentAmount"));
        } catch (JSONException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        finally {

            //Now the task of uploading document, transaction details to the server using RESTful web service
            prgDialog = new ProgressDialog(this);
            // Set Progress Dialog Text
            prgDialog.setMessage("Uploading your print job to the server. Please wait...");
            // Set Cancelable as False
            prgDialog.setCancelable(false);

            //Get variables from previous Activity
            Bundle xtraB = intent.getExtras();
            if(!xtraB.isEmpty())//Means there is data
            {
                user = xtraB.getString("user");
                kiosk = xtraB.getString("kiosk");
                price = xtraB.getDouble("price");
                number_of_copies = xtraB.getInt("copies");
                paper_size = xtraB.getString("size");
                print_orientation = xtraB.getString("orientation");
                color_option = xtraB.getString("coloroption");
                instruct_note = xtraB.getString("instruction");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                finally {
                    //Directory at server
                    ServerPath = "/home/ec2-user/.ssh/kiosks/" + kiosk +"/";
                    Uri uri = getIntent().getData();
                    docpath = uri.toString();
                    File myFiletoServer = new File(docpath);
                    FileExtension = getExtensionOfFile(myFiletoServer);
                    move_transaction_to_server(prgDialog);
                }
            }

        }
    }

    //This method showDetails was adopted from
    //https://github.com/probelalkhan/android-paypal-integration-example/blob/master/app/src/main/java/net/simplifiedcoding/paypalintegration/ConfirmationActivity.java
    //It is used for displaying transaction details from Paypal's web service
    private void showDetails(JSONObject jsonDetails, String paymentAmount) throws JSONException {
        //Views
        TextView textViewId = (TextView) findViewById(R.id.paymentId);
        TextView textViewStatus= (TextView) findViewById(R.id.paymentStatus);
        TextView textViewAmount = (TextView) findViewById(R.id.paymentAmount);

        //Showing the details from json object
        textViewId.setText(jsonDetails.getString("id"));
        textViewStatus.setText(jsonDetails.getString("state"));
        textViewAmount.setText(paymentAmount+" GBP");
        //Get trans ID
        paypalID = jsonDetails.getString("id");
    }

    //This method saves details of transaction to transaction table on AWS Database
    private void move_transaction_to_server(ProgressDialog pDialog)
    {
        pDialog.show();
        String params = "";
        Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        String formattedDate = df.format(c.getTime());

        //params containing data formatted to be passed to RESTful web service
        params = "/" + paypalID + "/" + user + "/" + kiosk + "/" + formattedDate + "/" + price + "/" + FileExtension;
        pDialog.hide();

        // Make RESTful webservice call using AsyncHttpClient object

        //The code below was adopted from
        //http://programmerguru.com/android-tutorial/android-restful-webservice-tutorial-how-to-call-restful-webservice-in-android-part-3/
        AsyncHttpClient client = new AsyncHttpClient();
        client.get(BASE_URL_TRANS + params, new AsyncHttpResponseHandler() {
            // When the response returned by REST has Http response code '200'

            @Override
            public void onSuccess(String response) {
                // Hide Progress Dialog
                pDialog.hide();
                try {
                    // JSON Object
                    JSONObject obj = new JSONObject(response);
                    // When the JSON response has status boolean value assigned with true
                    if(obj.getBoolean("status")){
                        Toast.makeText(getApplicationContext(), "Transaction Successfully Captured.", Toast.LENGTH_LONG).show();
                        upload_file_to_server();
                    }
                    // Else display error message
                    else{
                        Toast.makeText(getApplicationContext(), obj.getString("error_msg"), Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {
                    // TODO Auto-generated catch block
                    Toast.makeText(getApplicationContext(), "Error Occured [Server's JSON response might be invalid]!", Toast.LENGTH_LONG).show();
                    e.printStackTrace();

                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "Error : " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
            // When the response returned by REST has Http response code other than '200'
            @Override
            public void onFailure(int statusCode, Throwable error,
                                  String content) {
                // Hide Progress Dialog
                pDialog.hide();
                // When Http response code is '404'
                if(statusCode == 404){
                    Toast.makeText(getApplicationContext(), "Requested resource not found", Toast.LENGTH_LONG).show();
                }
                // When Http response code is '500'
                else if(statusCode == 500){
                    Toast.makeText(getApplicationContext(), "Something went wrong at server end", Toast.LENGTH_LONG).show();
                }
                // When Http response code other than 404, 500
                else{
                    Toast.makeText(getApplicationContext(), "Unexpected Error occcured! [Most common Error: Device might not be connected to Internet or remote server is not up and running]", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    //It moves file to directory on server (AWS)
    private void upload_file_to_server() throws IOException {
        String user = "ec2-user";
        String pass = "@87.Com#";
        String host = "ec2-52-56-85-16.eu-west-2.compute.amazonaws.com";
        JSch jsch = new JSch();
        Session session = null;

        //Adopted From
        //https://www.androidpit.de/forum/733495/filenotfoundexception-bei-referenzierung-auf-raw-ordner
        //It fetches android resources from internal project files (private key in this case)
        //and pass them as raw bytes to Jsch class' jsch.addidentity for successful SSH
        //connection to AWS
        InputStream pkeyStream = getResources().openRawResource(R.raw.revprint_key);
        ByteArrayOutputStream PrvbAOS = new ByteArrayOutputStream();
        int i;
        try
        {
            i = pkeyStream.read();
            while (i != -1)
            {
                PrvbAOS.write(i);
                i = pkeyStream.read();
            }
            pkeyStream.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        //Adopted From
        //https://www.androidpit.de/forum/733495/filenotfoundexception-bei-referenzierung-auf-raw-ordner
        //It fetches android resources from internal project files (public key in this case)
        //and pass them as raw bytes to Jsch class' jsch.addidentity for successful SSH
        //connection to AWS
        InputStream pubkeyStream = getResources().openRawResource(R.raw.authorized_keys);
        ByteArrayOutputStream PubbAOS = new ByteArrayOutputStream();
        int j;
        try
        {
            j = pubkeyStream.read();
            while (j != -1)
            {
                PubbAOS.write(j);
                j = pubkeyStream.read();
            }
            pubkeyStream.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        try{
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            jsch.addIdentity("revprint_key.pem",PrvbAOS.toByteArray(),PubbAOS.toByteArray(),null);
            session = jsch.getSession(user, host, 22);
            session.setConfig(config);
            session.setPassword("@87.Com#");
            session.setTimeout(100000);
            session.connect();

            Channel channel = session.openChannel("sftp");
            ChannelSftp sftpChannel = (ChannelSftp) channel;
            sftpChannel.connect();
            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
            {

                Toast.makeText(getApplicationContext(), "File Exist.", Toast.LENGTH_LONG).show();
                try {
                    sftpChannel.put(docpath, ServerPath + paypalID + "." + FileExtension);
                } catch (SftpException e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                }
                sftpChannel.exit();
                session.disconnect();
                Toast.makeText(getApplicationContext(), "File Successfully uploaded.", Toast.LENGTH_LONG).show();

                //Next is to upload settings and other print details to job pool
                save_details_to_job_pool();
            }
            else {
                Toast.makeText(getApplicationContext(), "Permission not granted.", Toast.LENGTH_LONG).show();
            }
        }
        catch(JSchException e){
            // show the error in the UI
            Toast.makeText(getApplicationContext(), "Error : " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    //Adopted from
    //https://java2blog.com/how-to-get-extension-of-file-in-java/
    public static String getExtensionOfFile(File file)
    {
        String fileExtension="";

        // Get file Name first
        String fileName=file.getName();

        // If fileName do not contain "." or starts with "." then it is not a valid file
        if(fileName.contains(".") && fileName.lastIndexOf(".")!= 0)
        {
            fileExtension=fileName.substring(fileName.lastIndexOf(".")+1);
        }
        return fileExtension;
    }

    //This method saves print settings and other related job details to job pool on DB
    private void save_details_to_job_pool()
    {
        //params containing data formatted to be passed to RESTful web service
        if(!Utility.isNotNull(instruct_note))
            instruct_note = "No Instructions.";
        String params = "/" + user + "/" + kiosk + "/" + paypalID + "/" + color_option + "/" + print_orientation + "/";
        params+= number_of_copies + "/" + paper_size + "/" + paypalID +"."+ FileExtension + "/" + instruct_note;

        // Make RESTful webservice call using AsyncHttpClient object

        //The code below was adopted from
        //http://programmerguru.com/android-tutorial/android-restful-webservice-tutorial-how-to-call-restful-webservice-in-android-part-3/
        AsyncHttpClient client = new AsyncHttpClient();
        client.get(BASE_URL_POOL + params, new AsyncHttpResponseHandler() {
            // When the response returned by REST has Http response code '200'

            @Override
            public void onSuccess(String response) {
                // Hide Progress Dialog
                try {
                    // JSON Object
                    JSONObject obj = new JSONObject(response);
                    // When the JSON response has status boolean value assigned with true
                    if(obj.getBoolean("status")){
                        Toast.makeText(getApplicationContext(), "Print job Successfully submitted to Job pool.", Toast.LENGTH_LONG).show();
                        upload_file_to_server();
                    }
                    // Else display error message
                    else{
                        Toast.makeText(getApplicationContext(), obj.getString("error_msg"), Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {
                    // TODO Auto-generated catch block
                    Toast.makeText(getApplicationContext(), "Error Occured [Server's JSON response might be invalid]!", Toast.LENGTH_LONG).show();
                    e.printStackTrace();

                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "Error : " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
            // When the response returned by REST has Http response code other than '200'
            @Override
            public void onFailure(int statusCode, Throwable error,
                                  String content) {
                // When Http response code is '404'
                if(statusCode == 404){
                    Toast.makeText(getApplicationContext(), "Requested resource not found", Toast.LENGTH_LONG).show();
                }
                // When Http response code is '500'
                else if(statusCode == 500){
                    Toast.makeText(getApplicationContext(), "Something went wrong at server end", Toast.LENGTH_LONG).show();
                }
                // When Http response code other than 404, 500
                else{
                    Toast.makeText(getApplicationContext(), "Unexpected Error occcured! [Most common Error: Device might not be connected to Internet or remote server is not up and running]", Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}
