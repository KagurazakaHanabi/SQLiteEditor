package com.yaerin.sqlite.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bin.david.form.core.SmartTable;
import com.bin.david.form.data.CellInfo;
import com.bin.david.form.data.column.Column;
import com.bin.david.form.data.format.bg.ICellBackgroundFormat;
import com.bin.david.form.data.format.draw.TextDrawFormat;
import com.bin.david.form.data.table.ArrayTableData;
import com.bin.david.form.data.table.TableData;
import com.yaerin.sqlite.C;
import com.yaerin.sqlite.R;
import com.yaerin.sqlite.bean.Cell;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class TableActivity extends Activity {

    private SQLiteDatabase mDatabase;
    private String mTableName;
    private String[] mColumnNames;
    private List<String> mUndoList = new ArrayList<>();
    private List<String> mRedoList = new ArrayList<>();
    private List<Cell> mUndoVList = new ArrayList<>();
    private List<Cell> mRedoVList = new ArrayList<>();

    private ProgressBar mProgressBar;
    private SmartTable<String> mTable;
    private ArrayTableData<String> mTableData;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_table);
        mProgressBar = findViewById(R.id.progress);
        mTable = findViewById(R.id.table);
        mTable.setZoom(true, 2.0f, 0.5f);
        mTable.getConfig()
                .setMinTableWidth(mTable.getWidth())
                .setShowTableTitle(false)
                .setShowXSequence(false)
                .setShowYSequence(false)
                .setFixedTitle(true)
                .setContentCellBackgroundFormat(new ICellBackgroundFormat<CellInfo>() {
                    @Override
                    public void drawBackground(Canvas canvas, Rect rect,
                                               CellInfo cellInfo, Paint paint) {
                        if (cellInfo.row % 2 == 0) {
                            paint.setColor(0xFFFAFAFA);
                            canvas.drawRect(rect, paint);
                        }
                        for (Cell cell : mUndoVList) {
                            if (cell.col == cellInfo.col && cell.row == cellInfo.row) {
                                paint.setColor(0xFFFFE4B5);
                                canvas.drawRect(rect, paint);
                            }
                        }
                    }

                    @Override
                    public int getTextColor(CellInfo cellInfo) {
                        return 0;
                    }
                });

        String path = getIntent().getStringExtra(C.EXTRA_DATABASE_PATH);
        mDatabase = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READWRITE);
        mTableName = getIntent().getStringExtra(C.EXTRA_TABLE_NAME);

        String[] arr = path.split("/");
        setTitle("`" + arr[arr.length - 1].replaceAll("\\.db$", "") +
                "`." + mTableName);

        init();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.table, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean enabledUndo = mUndoList.size() > 0;
        boolean enabledRedo = mRedoList.size() > 0;
        boolean enabledSave = mUndoList.size() > 0;

        MenuItem undo = menu.findItem(R.id.action_undo);
        MenuItem redo = menu.findItem(R.id.action_redo);
        MenuItem save = menu.findItem(R.id.action_save);

        undo.setEnabled(enabledUndo);
        undo.setIcon(enabledUndo ? R.drawable.ic_undo_white_24dp : R.drawable.ic_undo_gray_24dp);
        redo.setEnabled(enabledRedo);
        redo.setIcon(enabledRedo ? R.drawable.ic_redo_white_24dp : R.drawable.ic_redo_gray_24dp);
        save.setEnabled(enabledSave);
        save.setIcon(enabledSave ? R.drawable.ic_save_white_24dp : R.drawable.ic_save_gray_24dp);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_undo: {
                if (mUndoList.size() > 0 && mUndoVList.size() > 0) {
                    String s = mUndoList.get(mUndoList.size() - 1);
                    mRedoList.add(s);
                    mUndoList.remove(s);
                    Cell c = mUndoVList.get(mUndoVList.size() - 1);
                    mRedoVList.add(c);
                    mUndoVList.remove(c);
                    mTableData.getData()[c.col][c.row] = c.oldVal;
                    mTable.invalidate();
                }
                invalidateOptionsMenu();
                break;
            }

            case R.id.action_redo: {
                if (mRedoList.size() > 0) {
                    String s = mRedoList.get(mRedoList.size() - 1);
                    mUndoList.add(s);
                    mRedoList.remove(s);
                    Cell c = mRedoVList.get(mRedoVList.size() - 1);
                    mUndoVList.add(c);
                    mRedoVList.remove(c);
                    mTableData.getData()[c.col][c.row] = c.newVal;
                    mTable.invalidate();
                }
                invalidateOptionsMenu();
                break;
            }

            case R.id.action_save: {
                try {
                    execSQLs();
                } catch (Exception e) {
                    Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        mDatabase.close();
        super.onDestroy();
    }

    private void execSQLs() {
        for (String sql : mUndoList) {
            mDatabase.execSQL(sql);
        }
        mUndoList.clear();
        mRedoList.clear();
        mUndoVList.clear();
        mRedoVList.clear();
        mTable.invalidate();
        invalidateOptionsMenu();
    }

    private String[] getColumnNames() {
        Cursor c = mDatabase.rawQuery(
                "PRAGMA table_info(\"" + mTableName + "\")", null);
        List<String> list = new ArrayList<>();
        while (c.moveToNext()) {
            list.add(c.getString(c.getColumnIndex("name")));
        }
        c.close();
        return list.toArray(new String[0]);
    }

    private String[][] buildArray() {
        Cursor cursor = mDatabase.rawQuery(
                "SELECT * FROM \"" + mTableName + "\"", null);
        Cursor c = mDatabase.rawQuery(
                "PRAGMA table_info(\"" + mTableName + "\")", null);

        if (cursor.getCount() == 0) {
            cursor.close();
            c.close();
            return null;
        }

        String[][] data = new String[cursor.getColumnCount()][cursor.getCount()];
        for (int i = 0; i < cursor.getColumnCount(); i++) {
            c.moveToPosition(i);
            for (int j = 0; j < cursor.getCount(); j++) {
                cursor.moveToPosition(j);
                if (c.getString(c.getColumnIndex("type")).equals("BLOB")) {
                    data[i][j] = "[BLOB]";
                } else {
                    data[i][j] = cursor.getString(cursor.getColumnIndex(mColumnNames[i]));
                }
            }
        }

        cursor.close();
        c.close();
        return data;
    }

    @Nullable
    private String getPrimaryKey(String sql) {
        String primaryKey = null;
        int start = sql.indexOf("(") + 1;
        int end = sql.length() - new StringBuilder(sql).reverse().toString().indexOf(")") - 1;
        sql = sql.substring(start, end);
        String[] arr = sql.replaceAll("\\s+", " ").split(",");
        for (String s : arr) {
            s = s.trim();
            if (Pattern.compile(".*PRIMARY\\s+KEY.*",
                    Pattern.CASE_INSENSITIVE).matcher(s).matches()) {
                primaryKey = s.split("\\s+")[0].replaceAll("\"", "");
            }
        }
        return primaryKey;
    }

    private String buildSQL(int col, int row, String name, String newValue) {
        Cursor cursor = mDatabase.rawQuery(
                "SELECT sql FROM sqlite_master WHERE name=\"" + mTableName + "\"", null);
        cursor.moveToPosition(0);
        String sql = cursor.getString(cursor.getColumnIndex("sql"));
        cursor.close();

        String primaryKey = getPrimaryKey(sql);

        if (primaryKey == null) {
            Toast.makeText(this, R.string.err_primary_key, Toast.LENGTH_LONG).show();
            return null;
        }

        Cursor c = mDatabase.rawQuery(
                String.format("SELECT \"%s\",\"%s\" FROM \"%s\"", primaryKey, name, mTableName), null);
        c.moveToPosition(row);
        String key = c.getString(c.getColumnIndex(primaryKey));
        String value = c.getString(c.getColumnIndex(name));
        c.close();
        if (value.equals(newValue)) {
            return null;
        } else {
            mTableData.getData()[col][row] = newValue;
            mTable.invalidate();
            mUndoVList.add(new Cell(col, row, value, newValue));
            if (!mRedoVList.isEmpty()) {
                mRedoVList.clear();
            }
            return "UPDATE " + mTableName +
                    " SET " + name + "=\"" + newValue + "\"" +
                    " WHERE " + primaryKey + "=\"" + key + "\"";
        }
    }

    private void init() {
        mProgressBar.setVisibility(View.VISIBLE);
        mColumnNames = getColumnNames();
        new Thread(() -> {
            final String[][][] array = {buildArray()};
            runOnUiThread(() -> {
                if (array[0] == null) {
                    array[0] = new String[mColumnNames.length][0];
                }
                mTableData =
                        ArrayTableData.create(null, mColumnNames, array[0], new TextDrawFormat<>());
                mTableData.setOnItemClickListener(new OnItemClickListener());
                mTable.setTableData(mTableData);
                mProgressBar.setVisibility(View.GONE);
            });
        }).start();
    }

    private class OnItemClickListener implements TableData.OnItemClickListener {

        @Override
        public void onClick(Column column, String value, Object o, int col, int row) {
            Cursor cursor = mDatabase.rawQuery(
                    "PRAGMA table_info(\"" + mTableName + "\")", null);
            cursor.moveToPosition(col);
            String name = cursor.getString(cursor.getColumnIndex("name"));
            String type = cursor.getString(cursor.getColumnIndex("type"));
            boolean notNull = cursor.getInt(cursor.getColumnIndex("notnull")) == 1;
            cursor.close();

            if (type.equals("BLOB")) {
                Toast.makeText(TableActivity.this, R.string.err_blob_data, Toast.LENGTH_SHORT).show();
                return;
            }

            View view = View.inflate(TableActivity.this, R.layout.dialog_edit, null);
            EditText editText = view.findViewById(R.id.edit_text);
            editText.setText(value);
            editText.requestFocus();
            editText.selectAll();
            new AlertDialog.Builder(TableActivity.this)
                    .setTitle(getString(R.string.title_edit,
                            name, type + (notNull ? " NOT NULL" : "")))
                    .setView(view)
                    .setNegativeButton(R.string.action_cancel, (dialog, which) -> dialog.dismiss())
                    .setPositiveButton(R.string.action_ok, (dialog, which) -> {
                        String newValue = editText.getText().toString();
                        if (TextUtils.isEmpty(newValue) && notNull) {
                            Toast.makeText(TableActivity.this,
                                    "NOT NULL", Toast.LENGTH_SHORT).show();
                        } else if (!newValue.equals(value)) {
                            String sql = buildSQL(col, row, name, newValue);
                            if (sql != null) {
                                mUndoList.add(sql);
                                if (!mRedoList.isEmpty()) {
                                    mRedoList.clear();
                                }
                                invalidateOptionsMenu();
                            }
                        }
                    })
                    .create()
                    .show();
        }
    }
}
