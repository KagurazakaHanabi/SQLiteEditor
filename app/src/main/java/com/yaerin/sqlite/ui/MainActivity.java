package com.yaerin.sqlite.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.yaerin.sqlite.C;
import com.yaerin.sqlite.R;

import java.io.File;
import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private String mPath;
    private SQLiteDatabase mDatabase;

    private ListView mTables;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTables = findViewById(R.id.table);

        Uri uri = getIntent().getData();
        if (uri == null || uri.getScheme() == null || uri.getPath() == null) {
            throw new NullPointerException();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mPath = getFileForUri(uri).getPath();
        } else {
            mPath = uri.getPath();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1000);
        } else {
            main();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        main();
    }

    private void main() {
        try {
            openDatabase();
            String[] tableNames = getTableNames();
            mTables.setAdapter(new ArrayAdapter<>(
                    this, R.layout.item_table, R.id.table_name, tableNames));
            mTables.setOnItemClickListener((parent, view, position, id) ->
                    startActivity(new Intent(MainActivity.this, TableActivity.class)
                            .putExtra(C.EXTRA_DATABASE_PATH, mPath)
                            .putExtra(C.EXTRA_TABLE_NAME, tableNames[position])));
        } catch (Exception e) {
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    private void openDatabase() {
        String[] arr = mPath.split("/");
        setTitle("`" + arr[arr.length - 1].replaceAll("\\.db$", "") + "`");
        mDatabase = SQLiteDatabase.openDatabase(mPath, null, SQLiteDatabase.OPEN_READWRITE);
    }

    private String[] getTableNames() {
        List<String> list = new ArrayList<>();
        Cursor cursor = mDatabase.rawQuery(
                "SELECT name FROM sqlite_master WHERE type = 'table' ORDER BY name",
                null);
        while (cursor.moveToNext()) {
            list.add(cursor.getString(0));
        }
        cursor.close();
        Collections.sort(list, Collator.getInstance(Locale.getDefault()));
        String[] arr = new String[list.size()];
        list.toArray(arr);
        return arr;
    }

    private File getFileForUri(Uri uri) {
        String path = uri.getEncodedPath();

        assert path != null;
        final int splitIndex = path.indexOf('/', 1);
        path = Uri.decode(path.substring(splitIndex + 1));
        File file = new File("/storage", path);
        try {
            file = file.getCanonicalFile();
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to resolve canonical path for " + file);
        }

        return file;
    }
}
