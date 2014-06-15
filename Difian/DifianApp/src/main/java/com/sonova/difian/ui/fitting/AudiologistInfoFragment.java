// Copyright (c) 2014 Phonak, Inc. and Oberon microsystems, Inc. All rights reserved.

package com.sonova.difian.ui.fitting;

import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.sonova.difian.R;
import com.sonova.difian.communication.FittingBinder;
import com.sonova.difian.communication.fittingconnection.FittingConnectionError;
import com.sonova.difian.communication.fittingconnection.FittingConnectionState;
import com.sonova.difian.communication.messaging.HiMuteStatus;
import com.sonova.difian.utilities.Contract;

public final class AudiologistInfoFragment extends Fragment implements FittingServiceConnectionFragmentCallback
{
    private FittingServiceConnectionFragment _fittingServiceConnection;
    private TextView _audiologistNameTextView;
    private ImageView _audiologistPictureImageView;
    private TextView _timerTextView;
    private boolean _uiFinalized;
    private final Handler _handler = new Handler();
    private final Runnable _updateTimerRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            if ((_timerTextView != null) && !getActivity().isFinishing())
            {
                _timerTextView.setText(getTime());
                _handler.postDelayed(_updateTimerRunnable, 250);
            }

            finalizeUi();
        }
    };

    @Override
    public void onFittingServiceStateUpdated(FittingConnectionState state, FittingConnectionError error, String id, String audiologistName, Bitmap audiologistPicture, HiMuteStatus muteStatusLeft, HiMuteStatus muteStatusRight)
    {
        finalizeUi();

        if (_audiologistNameTextView != null)
        {
            _audiologistNameTextView.setText(audiologistName);
        }
        if (_audiologistPictureImageView != null)
        {
            if (audiologistPicture == null)
            {
                Bitmap anonymousPicture = BitmapFactory.decodeResource(getResources(), R.drawable.anonymous);
                _audiologistPictureImageView.setImageBitmap(anonymousPicture);
            }
            else
            {
                _audiologistPictureImageView.setImageBitmap(audiologistPicture);
            }
        }
    }

    private void finalizeUi()
    {
        if (!_uiFinalized)
        {
            View rootView = getActivity().findViewById(R.id.com_sonova_difian_ui_fitting_fittingmainactivity_audiologistinfo_linearlayout).getRootView();

            float maxWidth = rootView.getWidth();

            int imageSize = _audiologistPictureImageView.getHeight();
            _audiologistPictureImageView.getLayoutParams().width = imageSize;

            Log.i(AudiologistInfoFragment.class.getSimpleName(), "maxW = " + maxWidth + " imageSize = " + imageSize);

            float padding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());

            if (maxWidth != 0)
            {
                _audiologistNameTextView.setMaxWidth((int)Math.floor(maxWidth - padding - (2 * imageSize)));
                _uiFinalized = true;
            }
        }
    }

    @SuppressWarnings("RefusedBequest")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        _uiFinalized = false;

        return inflater.inflate(R.layout.com_sonova_difian_ui_fitting_audiologistinfofragment, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        _fittingServiceConnection = ((FittingMainActivity)getActivity()).getFittingServiceConnection();
        _audiologistNameTextView = (TextView)getActivity().findViewById(R.id.com_sonova_difian_ui_fitting_fittingmainactivity_audiologistname_textview);
        _audiologistPictureImageView = (ImageView)getActivity().findViewById(R.id.com_sonova_difian_ui_fitting_fittingmainactivity_audiologistpicture_imageview);
        _timerTextView = (TextView)getActivity().findViewById(R.id.com_sonova_difian_ui_fitting_fittingmainactivity_timer_textview);

        Contract.check(_audiologistNameTextView != null);
        Contract.check(_audiologistPictureImageView != null);
        Contract.check(_timerTextView != null);
    }

    @Override
    public void onResume()
    {
        super.onResume();

        _handler.postDelayed(_updateTimerRunnable, 100);
    }

    @Override
    public void onPause()
    {
        super.onPause();

        _handler.removeCallbacks(_updateTimerRunnable);
    }

    private String getTime()
    {
        FittingBinder binder = _fittingServiceConnection.getBinder();
        String result = "0:00";
        if (binder != null)
        {
            long seconds = binder.getSessionDurationSeconds();
            long m = seconds / 60;
            long s = seconds % 60;
            result = String.format("%d:%02d", m, s);
        }

        return result;
    }
}
