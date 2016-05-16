package com.surmenok.maria.recepitparser;

import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PreviewActivity extends Activity {
    public static final String RESULT_MESSAGE = "result";
    private static int DATE_PICKER_ID = 1;

    private Calendar calendar;
    private DatePicker datePicker;
    private int year, month, day;
    private long milliseconds;

    private ArrayList<Item> items;

    //Clear previous receipt

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);
        Intent intent = getIntent();
        String messageText = intent.getStringExtra(RESULT_MESSAGE);
        processData(messageText);

        //set date to button
        calendar = Calendar.getInstance();
        year = calendar.get(Calendar.YEAR);
        month = calendar.get(Calendar.MONTH);
        day = calendar.get(Calendar.DAY_OF_MONTH);
        milliseconds = calendar.getTimeInMillis();
        showDate();

        addElementsToLayout();
    }

    //for Trader Joe's and Safeway receipts
    private void processData(String result) {
        Log.i("Raw result: ", result);
        //prepare string for splitting
        result = result.replaceAll("OPEN\\s.*DAILY", ""); // for Trader Joe's
        result = result.replaceAll(",( )?", ".").replaceAll("\\$", "").replaceAll("\\d-\\s", "0\n");
        //if name and price occured in the same line add a \n between them
        Pattern priceWithSpaceRegex = Pattern.compile("(\\s\\d+\\.\\d{2})(( \\w)?)");
        Matcher regexMatcher = priceWithSpaceRegex.matcher(result);
        StringBuffer str = new StringBuffer();

        while (regexMatcher.find()) {
            regexMatcher.appendReplacement(str, "\n" + regexMatcher.group(1).trim() + "\n");
        }
        result = str.toString();

        //Break receipt to names and prices
        ArrayList<String> arr = new ArrayList<String>(Arrays.asList(result.split("\\n")));
        //Remove empty lines
        int j = arr.size() - 1;
        while (j >= 0) {
            if (arr.get(j).matches("\\s?")) {
                arr.remove(j);
            }
            else { //remove headers from receipt
                for (int k = 0; k < Exclusions.headerExclusions.length; k++) {
                    if (arr.get(j).toUpperCase().equals(Exclusions.headerExclusions[k])) {
                        arr.remove(j);
                    }
                }
            }
            j--;
        }

        //Create ArrayList of Item objects (name = price)
        items = new ArrayList<>();

        String priceRegex = "\\d+\\.\\d{2}";

        while (!arr.isEmpty() && arr.size() > 1) {
            if (arr.get(0).matches(priceRegex)) {
                if (!arr.get(1).matches(priceRegex)) {
                    items.add(new Item(arr.get(1), Double.parseDouble(arr.get(0))));
                    arr.remove(0);
                }
            }
            else {
                if (arr.get(1).matches(priceRegex)) {
                    items.add(new Item(arr.get(0), Double.parseDouble(arr.get(1))));
                    arr.remove(0);
                }
            }
            arr.remove(0);
        }

        //remove unnecessary lines such as total, tax, etc
        int i = items.size() - 1;
        while (i >= 0) {
            for (int k = 0; k < Exclusions.itemExclusions.length; k++) {
                if (items.get(i).getName().toUpperCase()
                        .equals(Exclusions.itemExclusions[k])) {
                    items.remove(i);
                    break;
                }
            }
            i--;
        }

        //show result
//        TextView messageView = (TextView) findViewById(R.id.result);
//        messageView.setText(result);
    }

    private void addElementsToLayout() {
        final LinearLayout layout = (LinearLayout)findViewById(R.id.previewLayoutContainer);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

        LinearLayout.LayoutParams params2 = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);

        for (int i = 0; i < items.size(); i++) {
            LinearLayout ll = new LinearLayout(this);
            //ll.setColumnCount(3);

            //add name
            EditText name = new EditText(this);
            name.setText(String.format("%s", items.get(i).getName()));
            name.setTextSize(15);
            name.setLayoutParams(params2);
            ll.addView(name);

            //add '=' sign
            TextView sign = new TextView(this);
            sign.setText(" = ");
            ll.addView(sign);

            //add price
            EditText price = new EditText(this);
            price.setText(String.format("%.2f", items.get(i).getPrice()));
            ll.addView(price);

            //add button delete
            final Button btn = new Button(this);
            // Give button an ID
            btn.setId(i + 1);
            btn.setText("X");
            // set the layoutParams on the button
            btn.setLayoutParams(params);
            final int index = i;
            // Set click listener for button
            //delete current item and refresh view page
            btn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    syncronizeValuesFromView(); //save changes before refreshing
                    //Log.i("values", items.toString());
                    items.remove(index);
                    layout.removeAllViews();
                    addElementsToLayout();
                }
            });
            ll.addView(btn);
            layout.addView(ll);
        }


    }

    //if user changes something, save data from view page to the ArrayList items before lost it :)
    private void syncronizeValuesFromView() {
        ArrayList<Item> temp = new ArrayList<>();
        ArrayList<EditText> ets = new ArrayList<>();
        getTextFields((ViewGroup) findViewById(R.id.previewLayoutContainer), ets);

        Log.i("number of textFields: ", ets.size() + "");
        for (int i = 0; i < ets.size(); i += 2) {
            String name = ets.get(i).getText().toString();
            double price;
            try {
                price = Double.parseDouble(ets.get(i + 1).getText().toString());
            }
            catch (NumberFormatException ex) {
                price = 0;
            }
            Log.i("values", name + "=" + price);
            temp.add(new Item(name, price));
        }
        items = temp;
    }

    //find all EditText fields in the activity
    private void getTextFields(ViewGroup vg, ArrayList<EditText> ets) {
        for (int i = 0; i < vg.getChildCount(); i++) {
            View child = vg.getChildAt(i);
            if (child instanceof ViewGroup) {
                getTextFields((ViewGroup) child, ets);
            }
            else if(child instanceof EditText) {
                ets.add((EditText) child);
            }
        }
    }

    //set today's date to button
    public void showDate() {
        Button btn = (Button) findViewById(R.id.datePickerDialog);
        String dateStr = String.format("%02d/%02d/%4d", (1 + month), day, year);
        btn.setText(dateStr);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected Dialog onCreateDialog(int id) {
        // TODO Auto-generated method stub
        if (id == DATE_PICKER_ID) {
            return new DatePickerDialog(this, myDateListener, year, month, day);
        }
        return null;
    }

    private DatePickerDialog.OnDateSetListener myDateListener = new DatePickerDialog.OnDateSetListener() {
        @Override
        public void onDateSet(DatePicker arg0, int arg1, int arg2, int arg3) {
            // TODO Auto-generated method stub
            year = arg1;
            month = arg2;
            day = arg3;
            calendar.set(arg1, arg2, arg3);
            milliseconds = calendar.getTimeInMillis();
            showDate();
        }
    };

    //onClick functions
    @SuppressWarnings("deprecation")
    public void setDate(View view) {
        showDialog(DATE_PICKER_ID);
    }

    public void onClickDiscard(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    //Save data to SQLite
    public void onClickSave(View view) {
        syncronizeValuesFromView();

        SQLiteOpenHelper receiptParserDatabaseHelper = new ReceiptParserDatabaseHelper(this);
        ContentValues purchaseValues = new ContentValues();

        try {
            SQLiteDatabase db = receiptParserDatabaseHelper.getWritableDatabase();

            for (int i = 0; i < items.size(); i++) {
                purchaseValues.put("DATE", milliseconds);
                purchaseValues.put("NAME", items.get(i).getName());
                purchaseValues.put("PRICE", items.get(i).getPrice());
                db.insert("PURCHASE", null, purchaseValues);
            }
            db.close();
        }
        catch (SQLiteException ex) {
            Toast toast = Toast.makeText(this, "Database unavailable", Toast.LENGTH_LONG);
            toast.show();
        }
        finally {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }
    }

    public void onClickAddItem(View view) {
        syncronizeValuesFromView(); //save changes from EditText
        items.add(new Item("", 0));
        final LinearLayout layout = (LinearLayout)findViewById(R.id.previewLayoutContainer);
        layout.removeAllViews();
        addElementsToLayout();
    }
}
