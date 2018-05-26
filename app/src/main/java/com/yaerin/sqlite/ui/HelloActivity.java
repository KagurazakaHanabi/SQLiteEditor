package com.yaerin.sqlite.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.MenuItem;

import com.yaerin.sqlite.R;

public class HelloActivity extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        findViewById(R.id.action_ok).setOnClickListener(v ->
                new AlertDialog.Builder(HelloActivity.this)
                        .setCancelable(false)
                        .setMessage(R.string.app_notice)
                        .setPositiveButton(R.string.action_know, (dialog, which) -> finish())
                        .create()
                        .show());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.hello, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        startActivity(new Intent(Intent.ACTION_VIEW)
                .setData(Uri.parse("https://github.com/Yaerin/SQLiteEditor"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        return true;
    }

    @Override
    protected void onStop() {
        disableSelf();
        super.onStop();
    }

    private void disableSelf() {
        getPackageManager().setComponentEnabledSetting(
                new ComponentName(this, this.getClass()),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, 0);
    }
}
