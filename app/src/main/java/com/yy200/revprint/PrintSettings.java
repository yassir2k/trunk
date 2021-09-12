package com.yy200.revprint;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputFilter;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.paypal.android.sdk.payments.PayPalConfiguration;
import com.paypal.android.sdk.payments.PayPalPayment;
import com.paypal.android.sdk.payments.PayPalService;
import com.paypal.android.sdk.payments.PaymentActivity;
import com.paypal.android.sdk.payments.PaymentConfirmation;

import org.json.JSONException;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class PrintSettings extends AppCompatActivity{

    public static final int PAYPAL_REQUEST_CODE = 1987;


    //Paypal Configuration Object
    private static PayPalConfiguration config = new PayPalConfiguration()
            .environment(PayPalConfiguration.ENVIRONMENT_SANDBOX) //Using SANDBOX for simulation
            .clientId(PayPalConfig.PAYPAL_CLIENT_ID);

    Double price = 0.0;
    //This are used for binding Form objects from the file print_settings_view.xml
    TextView amount; //For displaying price
    EditText no_of_pages; //this is used for calculating price ONLY
    EditText copies ; //For capturing number of print copies
    Spinner orientation ; //Landscape or Portrait
    Spinner colorOptions ; //Color or Black & white
    Spinner paperSize ; //A4 or A3 (For now)
    EditText printInstruction; //For any instruction user intends to admin

    //Variables below are used for retrieving variables from the immediate previous activity
    String user = ""; //user's email
    Double price_color = 0.0;
    Double price_bw = 0.0;
    String kiosk = "";
    String docpath = ""; //Physical path of file uploaded from phone.
    String color_option = "";
    String print_orientation = "";
    int number_of_copies ;
    String paper_size = "";
    String instruct_note = "";

    @Override
    public void onDestroy() {
        stopService(new Intent(this, PayPalService.class));
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.print_settings_view);

        //Paypal Service needs to be Started
        Intent PaypalIntent = new Intent(this, PayPalService.class);
        PaypalIntent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config);
        startService(PaypalIntent);

        //Retrieve Bundle from previous activity
        Intent getPrev = getIntent();
        Bundle xtraB = getPrev.getExtras();
        if(!xtraB.isEmpty())//Means there is data
        {
            user = xtraB.getString("user");
            Uri uri = getIntent().getData();
            docpath = uri.toString();
            kiosk = xtraB.getString("kiosk");
            price_color = xtraB.getDouble("colorPrice");
            price_bw = xtraB.getDouble("bwPrice");

            File file = new File(docpath);
            if(file.exists()) {
                Toast.makeText(this, "Akwai." , Toast.LENGTH_SHORT).show();
            }
            else
            {
                Toast.makeText(this, "The file doesn't exist." , Toast.LENGTH_SHORT).show();
            }
        }


       // amount.setText("0.00"); //Set initial Price to 0.00
        Button toPay = (Button) findViewById(R.id.toPayment);
        amount = (TextView) findViewById(R.id.amount);
        colorOptions = (Spinner) findViewById(R.id.colorOptions);
        printInstruction = (EditText)findViewById(R.id.instruction);

        //Add color options according to availability in kiosk.
        //This is done by checking the prices. kiosk with a 0.00 price tag for either
        //coloured or black and white means such options is unavailable in that particular kiosk.
        List<String> colorArrayList =  new ArrayList<String>();
        if(price_color > 0 && price_bw > 0) //Means this Kiosk supports both coloured and Black & white
        {
            colorArrayList.add("Colored");
            colorArrayList.add("Black and White Print");
        }
        if( price_color == 0  && price_bw > 0) //Means this Kiosk supports only Black & white
        {
            colorArrayList.add("Black and White Print");
        }

        //This function below was adopted from
        //https://stackoverflow.com/questions/11920754/android-fill-spinner-from-java-code-programmatically
        //For binding ArrayList to Spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, colorArrayList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner sItems = (Spinner) findViewById(R.id.colorOptions);
        sItems.setAdapter(adapter);

        //This event handler for spinner ensures for each change, price gets updated
        colorOptions.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                calculate_pay();
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        //For the document's number of pages, we enter between 1 and 20
        no_of_pages = (EditText) findViewById(R.id.no_of_Pages);
        no_of_pages.setFilters(new InputFilter[]{new InputFilterMinMax("1", "20")});
        //update price each time a key is typed in page number textfield
        no_of_pages.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((keyCode == KeyEvent.KEYCODE_ENTER)) {
                    calculate_pay();
                    return true;
                } else
                    return false;
            }
        });

        //The method below for copies was adopted from
        //https://stackoverflow.com/questions/7397391/event-for-handling-the-focus-of-the-edittext
        //update price when number of pages field is focused
        no_of_pages.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {
                    calculate_pay();
                }

            }
        });

        //For this project, max number of print copies is between 1 and 100
        copies = (EditText) findViewById(R.id.no_of_Copies);
        copies.setFilters(new InputFilter[]{new InputFilterMinMax("1", "100")});
        //update price when number is keyed up
        copies.setOnKeyListener(new View.OnKeyListener() {
        @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
            if ((keyCode == KeyEvent.KEYCODE_ENTER)) {
                calculate_pay();
                return true;
            } else
                return false;
            }
        });

        //The method below for copies was adopted from
        //https://stackoverflow.com/questions/7397391/event-for-handling-the-focus-of-the-edittext
        //Update price on copies textfield being focused
        copies.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {
                    calculate_pay();
                }
            }
        });

        paperSize = (Spinner) findViewById(R.id.paperSize);
        //Next method below for paperSize was sourced from
        //https://www.programcreek.com/java-api-examples/android.widget.AdapterView.OnItemSelectedListener
        //update price when item is selected each time
        paperSize.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                calculate_pay();
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        orientation = (Spinner) findViewById(R.id.orientation);
        //Next method below for paperSize was sourced from
        //https://www.programcreek.com/java-api-examples/android.widget.AdapterView.OnItemSelectedListener
        //Update price when item is selected each time
        orientation.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                calculate_pay();
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        toPay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calculate_pay();
                toPaymentPage();
            }
        });
    }

    //Adopted from
    //https://android--examples.blogspot.co.uk/2015/04/alertdialog-in-android.html
    //This function gathers necessary data and directs user to payment page
    private void toPaymentPage()
    {
        //Check if no empty entries
        if(Utility.isNotNull(no_of_pages.getText().toString()) && Utility.isNotNull(copies.getText().toString()))
        {
            AlertDialog.Builder adb = new AlertDialog.Builder(this);

            //Set the dialog title
            adb.setTitle("Notification");

            //Define Alert Dialog Message
            adb.setMessage("You are about to be charged " + amount.getText() + " for this print job. Are you sure you want to proceed to payment?");

            //Specify this dialog is not cancelable
            adb.setCancelable(false);

            //Set the Yes/Positive and No/Negative Button text
            String yesButtonText = "Yes";
            String noButtonText = "No";

            //Define the positive button text and action on alert dialog
            adb.setPositiveButton(yesButtonText, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which){
                    //Do something when user press ok button from alert dialog
                    //After user clicks the 'Yes' button from the prompt,
                    //application goes to Paypal payment page
                    number_of_copies = Integer.parseInt(copies.getText().toString());
                    paper_size = paperSize.getSelectedItem().toString();
                    print_orientation = orientation.getSelectedItem().toString();
                    color_option = colorOptions.getSelectedItem().toString();
                    instruct_note = printInstruction.getText().toString();
                    pay_with_paypal();
                }
            });

            //Define the negative button text and action on alert dialog
            adb.setNegativeButton(noButtonText, new DialogInterface.OnClickListener(){
                @Override
                public void onClick(DialogInterface dialog, int which){
                    //Do something when user press no button from alert dialog
                }
            });
            //Display the Alert Dialog on app interface
            adb.show();
        }
        else
        {
            Toast.makeText(this, "Empty field. Make sure all entries are not empty.", Toast.LENGTH_LONG).show();
        }

    }

    //As from the name, this method calculates the charges for the print job
    private void calculate_pay()
    {
        double copiesFlag = 0.0;
        double pagesFlag = 0.0;
        double sizeFlag = 0.0;
        double colorFlag = 0.0;
        sizeFlag = paperSize.getSelectedItem().equals("A4")? 1.0: 1.2; //A3 is twice the price of A4
        copiesFlag = Utility.isNotNull(copies.getText().toString())? Double.parseDouble(copies.getText().toString()) : 1.0;
        pagesFlag = Utility.isNotNull(no_of_pages.getText().toString())? Double.parseDouble(no_of_pages.getText().toString()) : 1.0;
        colorFlag = colorOptions.getSelectedItem().equals("Colored")? price_color: price_bw;
        price = sizeFlag * colorFlag * pagesFlag * copiesFlag;
        String tmp = String.format("%.2f", price);
        price = Double.parseDouble(tmp);
        amount.setText("£ " + Double.toString(price));
    }

    /*
    This method below was adopted from
    https://github.com/probelalkhan/android-paypal-integration-example/blob/master/app/src/main/java/net/simplifiedcoding/paypalintegration/MainActivity.java
    */

    private void pay_with_paypal()
    {
        PayPalPayment payment = new PayPalPayment(new BigDecimal(price), "GBP", "Printing Fee",
                PayPalPayment.PAYMENT_INTENT_SALE);
        //Creating Paypal Payment activity intent
        Intent intent = new Intent(this, PaymentActivity.class);

        //putting the paypal configuration to the intent
        intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config);

        //Puting paypal payment to the intent
        intent.putExtra(PaymentActivity.EXTRA_PAYMENT, payment);

        //Starting the intent activity for result
        //the request code will be used on the method onActivityResult
        startActivityForResult(intent, PAYPAL_REQUEST_CODE);
    }


    /*
    This method below was adopted from
    https://github.com/probelalkhan/android-paypal-integration-example/blob/master/app/src/main/java/net/simplifiedcoding/paypalintegration/MainActivity.java
    */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //If the result is from paypal
        if (requestCode == PAYPAL_REQUEST_CODE) {

            //If the result is OK i.e. user has not canceled the payment
            if (resultCode == Activity.RESULT_OK) {
                //Getting the payment confirmation
                PaymentConfirmation confirm = data.getParcelableExtra(PaymentActivity.EXTRA_RESULT_CONFIRMATION);

                //if confirmation is not null
                if (confirm != null) {
                    try {
                        //Getting the payment details
                        String paymentDetails = confirm.toJSONObject().toString(4);
                        Log.i("paymentExample", paymentDetails);

                        //Pass to next activity (Payment details and on to database)
                        Bundle bundle = new Bundle();
                        bundle.putString("kiosk", kiosk);
                        bundle.putString("user", user);
                        bundle.putDouble("price", price);
                        bundle.putInt("copies", number_of_copies);
                        bundle.putString("size",paper_size);
                        bundle.putString("orientation", print_orientation);
                        bundle.putString("coloroption",color_option);
                        bundle.putString("instruction", instruct_note);

                        //Starting a new activity for the payment details and also putting the payment details with intent
                        Intent paymentIntent = new Intent(getApplicationContext(), PaymentDetails.class);
                        paymentIntent.putExtra("PaymentDetails", paymentDetails);
                        paymentIntent.putExtra("PaymentAmount", price.toString());
                        paymentIntent.setData(Uri.parse(docpath));
                        paymentIntent.putExtras(bundle);
                        paymentIntent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config);
                        startActivity(paymentIntent);

                    } catch (JSONException e) {
                        Log.e("paymentExample", "an extremely unlikely failure occurred: ", e);
                    }
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Log.i("paymentExample", "The user canceled.");
            } else if (resultCode == PaymentActivity.RESULT_EXTRAS_INVALID) {
                Log.i("paymentExample", "An invalid Payment or PayPal Configuration was submitted.");
            }
        }
    }

}
