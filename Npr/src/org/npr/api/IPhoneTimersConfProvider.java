package org.npr.api;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;
import org.npr.android.util.ArrayUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class IPhoneTimersConfProvider extends ContentProvider {

    public static final Uri CONTENT_URL =
            Uri.parse("content://org.npr.apr.IPhoneTimersConf");
    private static final String CONTENT_TYPE =
            "vnd.android.cursor.dir/vnd.npr.timers";
    private static final String CONF_URL =
            "http://www.npr.org/services/apps/iphone_timers.conf";

    private static final String LOG_TAG = IPhoneTimersConfProvider.class.getName();
    private static List<String[]> data;

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projections, String selection,
                        String[] selectionArgs, String sortOrder) {
        if (selection != null && !selection.equals(Items.NAME + " = ?")) {
            return null;
        }

        if (data == null) {
            data = new ArrayList<String[]>();
            if (!load()) {
                return null;
            }
        }

        MatrixCursor cursor = new MatrixCursor(Items.COLUMNS);
        for (String[] row : data) {
            if (selection == null) {
                cursor.addRow(row);
            } else if (row[6].equals(selectionArgs[0])) {
                cursor.addRow(row);
            }
        }

        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        return CONTENT_TYPE;
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(Uri uri, String s, String[] strings) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String s, String[] strings) {
        throw new UnsupportedOperationException();
    }

    /**
     * Parses the CSV data and loads the data member table.
     * <p/>
     * TODO: Use a real CSV parser or even better a CSV database implementation.
     * e.g. http://sourceforge.net/projects/javacsv/develop
     * or import into SQLite in-memory?
     *
     * @return true on success; false if the stream is null or there is an
     *         exception (which is logged)
     */
    private boolean load() {
        try {
            InputStream stream = HttpHelper.download(CONF_URL, getContext());
            if (stream == null) {
                return false;
            }

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(stream),
                    8192
            );
            String buffer;
            while ((buffer = reader.readLine()) != null) {
                String[] rowData = buffer.split(",");
                if (rowData.length > 0) {
                    if (rowData.length < Items.COLUMNS.length) {
                        rowData = ArrayUtils.copyOf(rowData, Items.COLUMNS.length);
                    }
                    data.add(rowData);
                }
            }
            reader.close();
            stream.close();
        } catch (IOException e) {
            Log.e(LOG_TAG, "", e);
            return false;
        }
        return true;
    }

    public static class Items implements BaseColumns {
        public static final String NAME = "name";
        public static final String TIMER_LENGTH = "timer_length";

        public static final String[] COLUMNS = {NAME, TIMER_LENGTH};

        // This class cannot be instantiated
        private Items() {
        }
    }
}
