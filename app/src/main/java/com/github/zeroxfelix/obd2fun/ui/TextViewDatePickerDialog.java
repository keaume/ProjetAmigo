package com.github.zeroxfelix.obd2fun.ui;

import android.app.DatePickerDialog;
import android.content.Context;
import android.view.View;
import android.widget.DatePicker;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class TextViewDatePickerDialog implements View.OnClickListener, android.app.DatePickerDialog.OnDateSetListener{

    private final Context context;
    private final TextView dateTextView;
    private final Calendar calendar = Calendar.getInstance();

    public TextViewDatePickerDialog(Context context, TextView dateTextView){
        this.dateTextView = dateTextView;
        this.dateTextView.setOnClickListener(this);
        this.context = context;

        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE,0);
        calendar.set(Calendar.SECOND, 0);

        setTextViewText();
    }


    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        calendar.set(year, monthOfYear, dayOfMonth);

        setTextViewText();
    }

    @Override
    public void onClick(View v) {
        DatePickerDialog dialog = new DatePickerDialog(context, this, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    public Date getSetDate(){
        return calendar.getTime();
    }

    private void setTextViewText(){

        final SimpleDateFormat spinnerDateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY);
        dateTextView.setText(spinnerDateFormat.format(calendar.getTime()));
    }
}
