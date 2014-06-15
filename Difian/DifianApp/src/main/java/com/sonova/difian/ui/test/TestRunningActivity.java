// Copyright (c) 2014 Phonak, Inc. and Oberon microsystems, Inc. All rights reserved.

package com.sonova.difian.ui.test;

import android.app.Activity;
import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;
import com.sonova.difian.R;

public class TestRunningActivity extends Activity {
    private static final class ModelFragment extends Fragment {
        private static final String TAG = ModelFragment.class.getSimpleName();

        private ConnectionTestAsyncTask _task;

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            setRetainInstance(true);
        }

        @Override
        public void onResume () {
            super.onResume();
            if (_task == null) {
                _task = new ConnectionTestAsyncTask(this);
                _task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        }

        @Override
        public void onDetach() {
            if (getActivity().isFinishing()) {
                if (_task != null) {
                    _task.cancel(true);
                }
                _task = null;
            }
            super.onDetach();
        }

        @Override
        public void onPause () {
            super.onPause();
            if (_task != null) {
                _task.cancel(true);
                _task = null;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.com_sonova_difian_ui_test_testactivity);
        if (getFragmentManager().findFragmentByTag(ModelFragment.TAG) == null)
        {
            getFragmentManager().beginTransaction().add(new ModelFragment(), ModelFragment.TAG).commit();
        }
    }
}
