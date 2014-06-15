// Copyright (c) 2014 Phonak, Inc. and Oberon microsystems, Inc. All rights reserved.

package com.sonova.difian.ui.test;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import com.sonova.difian.R;

public class TestResultActivity extends Activity {
    private static final String EXTRA_CONN_TEST_SUCCESS = "com.sonova.difian.intent.extra.CONN_TEST_SUCCESS";
    private static final String EXTRA_CONN_TEST_LATENCY = "com.sonova.difian.intent.extra.CONN_TEST_LATENCY";
    private static final String EXTRA_CONN_TEST_ERROR_MSG = "com.sonova.difian.intent.extra.CONN_TEST_ERROR_MSG";

	@Override
	protected void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.com_sonova_difian_ui_test_testresultactivity);
	}

    @Override
    protected void onResume () {
        super.onResume();
        Bundle extras = getIntent().getExtras();
        boolean success = extras.getBoolean(EXTRA_CONN_TEST_SUCCESS);
        int latency = extras.getInt(EXTRA_CONN_TEST_LATENCY);
        String errorMsg = extras.getString(EXTRA_CONN_TEST_ERROR_MSG);
        TextView v = (TextView) findViewById(R.id.com_sonova_difian_ui_test_testresultactivity_textview);
        TextView v2 = (TextView) findViewById(R.id.com_sonova_difian_ui_test_testresultactivity_secondary_textview);
        if (success) {
            v.setText(R.string.com_sonova_difian_ui_test_testresultactivity_success_message);
            v2.setText(R.string.com_sonova_difian_ui_test_testresultactivity_success_secondary_message);
        } else {
            v.setText(R.string.com_sonova_difian_ui_test_testresultactivity_fail_message);
            if (errorMsg != null) {
                v2.setText(errorMsg);
            } else {
                v2.setText(R.string.com_sonova_difian_ui_test_testresultactivity_fail_secondary_message);
            }
        }
    }

    public void ok (View view) {
        setResult(RESULT_OK);
        finish();
    }

}
