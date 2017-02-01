package com.orgzly.android.provider.models;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

import com.orgzly.android.provider.DatabaseUtils;

public class DbPropertyValue extends Model {
    public static final String TABLE = "property_values";

    public static final String[] CREATE_SQL = new String[] {
            "CREATE TABLE IF NOT EXISTS " + TABLE + " (" +
            "_id INTEGER PRIMARY KEY AUTOINCREMENT," +

            "value TEXT UNIQUE)",

            "CREATE INDEX IF NOT EXISTS i_" + TABLE + "_" + "value" + " ON " + TABLE + "(" + "value" + ")",
    };

    public static final String DROP_SQL = "DROP TABLE IF EXISTS " + TABLE;

    public interface Columns {
        String VALUE = "value";
    }

    public static class Column implements Columns, BaseColumns {}

    public String value;

    public DbPropertyValue(String value) {
        this.value = value;
    }

    public long save(SQLiteDatabase db) {
        long id = DatabaseUtils.getId(
                db,
                TABLE,
                "value = ?",
                new String[] { value });

        if (id == 0) {
            ContentValues values = new ContentValues();

            values.put("value", value);

            id = db.insertOrThrow(TABLE, null, values);
        }

        return id;
    }
}
