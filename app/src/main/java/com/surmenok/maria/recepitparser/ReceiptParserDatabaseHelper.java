package com.surmenok.maria.recepitparser;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Maria on 5/1/2016.
 */
public class ReceiptParserDatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "receipt_parcer";
    private static final int DB_VERSION = 1;

    ReceiptParserDatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE PURCHASE ("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "DATE INTEGER,"
                + "NAME TEXT,"
                + "PRICE REAL"
                + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }


}
