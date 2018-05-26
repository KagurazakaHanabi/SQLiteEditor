package com.yaerin.sqlite.ui;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupMenu;
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

public class MainActivity extends Activity implements PopupMenu.OnMenuItemClickListener {

    private String mPath;
    private SQLiteDatabase mDatabase;
    private List<String> mTableNames;
    private String mTableName;

    private ListView mTables;
    private ArrayAdapter mAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTables = findViewById(R.id.table);

        Uri uri = getIntent().getData();
        if (uri == null || uri.getScheme() == null || uri.getPath() == null) {
            Toast.makeText(this, getIntent().toString(), Toast.LENGTH_LONG).show();
            return;
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_exec: {
                View view = View.inflate(MainActivity.this, R.layout.dialog_edit, null);
                EditText editText = view.findViewById(R.id.edit_text);
                editText.setHint(R.string.hint_sql);
                new AlertDialog.Builder(this)
                        .setCancelable(false)
                        .setTitle(R.string.action_exec_sql)
                        .setView(view)
                        .setNegativeButton(R.string.action_cancel, (dialog, which) -> dialog.dismiss())
                        .setPositiveButton(R.string.action_ok, (dialog, which) -> {
                            try {
                                mDatabase.execSQL(editText.getText().toString());
                                Toast.makeText(this, R.string.message_success, Toast.LENGTH_SHORT).show();
                                refresh();
                            } catch (Exception e) {
                                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        })
                        .create()
                        .show();
                break;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        main();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_delete: {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.title_confirm)
                        .setMessage(R.string.message_irreversible)
                        .setPositiveButton(R.string.action_ok, (dialog, which) -> {
                            mDatabase.execSQL("DROP TABLE \"" + mTableName + "\"");
                            refresh();
                        })
                        .create()
                        .show();
                break;
            }
        }
        return true;
    }

    private void main() {
        try {
            openDatabase();
            mTableNames = getTableNames();
            initView();
        } catch (Exception e) {
            Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    private void refresh() {
        mTableNames.clear();
        mTableNames.addAll(getTableNames());
        mAdapter.notifyDataSetChanged();
    }

    private void initView() {
        mTables.setAdapter(mAdapter = new ArrayAdapter<>(
                this, R.layout.item_table, R.id.table_name, mTableNames));
        mTables.setOnItemClickListener((parent, view, position, id) ->
                startActivity(new Intent(MainActivity.this, TableActivity.class)
                        .putExtra(C.EXTRA_DATABASE_PATH, mPath)
                        .putExtra(C.EXTRA_TABLE_NAME, mTableNames.get(position))));
        mTables.setOnItemLongClickListener((parent, view, position, id) -> {
            mTableName = mTableNames.get(position);
            PopupMenu popup = new PopupMenu(MainActivity.this, view);
            popup.getMenuInflater().inflate(R.menu.main_popup, popup.getMenu());
            popup.setOnMenuItemClickListener(this);
            popup.show();
            return true;
        });
    }

    private void openDatabase() {
        String[] arr = mPath.split("/");
        setTitle("`" + arr[arr.length - 1].replaceAll("\\.db$", "") + "`");
        mDatabase = SQLiteDatabase.openDatabase(mPath, null, SQLiteDatabase.OPEN_READWRITE);
    }

    private List<String> getTableNames() {
        List<String> list = new ArrayList<>();
        Cursor cursor = mDatabase.rawQuery(
                "SELECT name FROM sqlite_master WHERE type = 'table' ORDER BY name",
                null);
        while (cursor.moveToNext()) {
            list.add(cursor.getString(0));
        }
        cursor.close();
        Collections.sort(list, Collator.getInstance(Locale.getDefault()));
        return list;
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
