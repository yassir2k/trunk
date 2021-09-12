package com.yy200.revprint;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.*;
/**
 *
 * Register Activity Class
 *
 * Adopted from
 * http://programmerguru.com/android-tutorial/android-restful-webservice-tutorial-how-to-call-restful-webservice-in-android-part-3/
 */
public class Register extends Activity {

    private static final String BASE_URL = "http://52.56.85.16:8080/revPrintREST_/rest/demo/register";
    // Progress Dialog Object
    ProgressDialog prgDialog;
    // Error Msg TextView Object
    TextView errorMsg;
    // Name Edit View Object
    EditText nameET;
    // Email Edit View Object
    EditText emailET;
    // Password Edit View Object
    EditText pwdET;
    //Mobile Edit View Object
    EditText mobileET;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register);
        // Find Error Msg Text View control by ID
        errorMsg = (TextView)findViewById(R.id.register_error);
        // Find Name Edit View control by ID
        nameET = (EditText)findViewById(R.id.registerName);
        // Find Email Edit View control by ID
        emailET = (EditText)findViewById(R.id.registerEmail);
        // Find Password Edit View control by ID
        pwdET = (EditText)findViewById(R.id.registerPassword);
        //Find mobile Edit view control by ID
        mobileET = (EditText)findViewById(R.id.registerPhone);
        // Instantiate Progress Dialog object
        prgDialog = new ProgressDialog(this);
        // Set Progress Dialog Text
        prgDialog.setMessage("Please wait...");
        // Set Cancelable as False
        prgDialog.setCancelable(false);
    }

    /*
     * Method gets triggered when Register button is clicked
     * @param view
     */
    public void registerUser(View view){
        // Get NAme ET control value
        String name = nameET.getText().toString();
        // Get Email ET control value
        String email = emailET.getText().toString();
        // Get Password ET control value
        String password = pwdET.getText().toString();
        // Get Mobile ET control value
        String mobile = pwdET.getText().toString();
        // Instantiate Http Request Param Object
        String params = "";
        // When Name Edit View, Email Edit View and Password Edit View have values other than Null
        if(Utility.isNotNull(name) && Utility.isNotNull(email) && Utility.isNotNull(password) && Utility.isNotNull(mobile)){
            // When Email entered is Valid
            if(Utility.validate(email)){
                // Add the fullname parameter value to the API's Link
                params+="/" + name;
                // Add the email parameter value to the API's Link
                params+="/" + email;
                // Add the password parameter value to the API's Link
                params+="/" + password;
                // Add the mobile parameter value to the API's Link
                params+="/" + mobile;
                // Invoke RESTful Web Service with Http parameters
                invokeWS(params);
            }
            // When Email is invalid
            else{
                Toast.makeText(getApplicationContext(), "Please enter valid email", Toast.LENGTH_LONG).show();
            }
        }
        // When any of the Edit View control left blank
        else{
            Toast.makeText(getApplicationContext(), "Please fill the form, don't leave any field blank", Toast.LENGTH_LONG).show();
        }

    }

    /**
     * Method that performs RESTful webservice invocations
     *
     * @param params
     */
    public void invokeWS(String params){
        // Show Progress Dialog
        prgDialog.show();
        // Make RESTful webservice call using AsyncHttpClient object
        AsyncHttpClient client = new AsyncHttpClient();
        client.get(BASE_URL + params ,new AsyncHttpResponseHandler() {
        // When the response returned by REST has Http response code '200'
        @Override
        public void onSuccess(String response) {
            // Hide Progress Dialog
            prgDialog.hide();
            try {
                // JSON Object
                JSONObject obj = new JSONObject(response);
                // When the JSON response has status boolean value assigned with true
                if(obj.getBoolean("status")){
                    // Set Default Values for Edit View controls
                    setDefaultValues();
                    // Display successfully registered message using Toast
                    Toast.makeText(getApplicationContext(), "You are successfully registered!", Toast.LENGTH_LONG).show();
                    navigatetoLogin();
                }
                // Else display error message
                else{
                    errorMsg.setText(obj.getString("error_msg"));
                    Toast.makeText(getApplicationContext(), obj.getString("error_msg"), Toast.LENGTH_LONG).show();
                }
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                Toast.makeText(getApplicationContext(), "Error Occured [Server's JSON response might be invalid]!", Toast.LENGTH_LONG).show();
                e.printStackTrace();

            }
        }
        // When the response returned by REST has Http response code other than '200'
        @Override
        public void onFailure(int statusCode, Throwable error,
                String content) {
            // Hide Progress Dialog
            prgDialog.hide();
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

    /**
     * Method which navigates from Register Activity to Login Activity
     */
    public void navigatetoLoginActivity(View view){
        Intent loginIntent = new Intent(getApplicationContext(),Login.class);
        // Clears History of Activity
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(loginIntent);
    }
    public void navigatetoLogin(){
        Intent loginIntent = new Intent(getApplicationContext(), Login.class);
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(loginIntent);
    }

    /**
     * Set degault values for Edit View controls
     */
    public void setDefaultValues(){
        nameET.setText("");
        emailET.setText("");
        pwdET.setText("");
        mobileET.setText("");
    }

}