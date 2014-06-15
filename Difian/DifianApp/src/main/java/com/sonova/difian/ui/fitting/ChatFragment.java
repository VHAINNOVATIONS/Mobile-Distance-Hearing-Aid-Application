// Copyright (c) 2014 Phonak, Inc. and Oberon microsystems, Inc. All rights reserved.

package com.sonova.difian.ui.fitting;

import android.app.ListFragment;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.widget.AbsListView;
import android.widget.Toast;
import com.sonova.difian.R;
import com.sonova.difian.communication.FittingBinder;
import com.sonova.difian.communication.chat.ChatMessage;
import com.sonova.difian.communication.chat.ChatMessageSource;
import com.sonova.difian.utilities.Contract;

import java.util.ArrayList;
import java.util.List;

public final class ChatFragment extends ListFragment
{
    private final List<ChatMessage> _chatMessages = new ArrayList<ChatMessage>();
    private ChatListAdapter _adapter;

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        getListView().setDivider(null);
        getListView().setTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);

        _adapter = new ChatListAdapter(getActivity(), _chatMessages);
        setListAdapter(_adapter);
    }

    public void updateChatMessages(FittingBinder binder)
    {
        Contract.check(binder != null);

        Toast toast = null;

        for (int i = _chatMessages.size(); i < binder.getChatMessageCount(); i++)
        {
            ChatMessage m = binder.getChatMessage(i);

            _chatMessages.add(m);

            if (m.getSource() == ChatMessageSource.SYSTEM)
            {
                if (ChatMessage.TEXT_HI_MUTED_LEFT.equals(m.getText()))
                {
                    toast = Toast.makeText(getActivity(), R.string.com_sonova_difian_ui_fitting_chatlistadapter_messagetext_system_muted_left, Toast.LENGTH_LONG);
                }
                else if (ChatMessage.TEXT_HI_MUTED_RIGHT.equals(m.getText()))
                {
                    toast = Toast.makeText(getActivity(), R.string.com_sonova_difian_ui_fitting_chatlistadapter_messagetext_system_muted_right, Toast.LENGTH_LONG);
                }
                else if (ChatMessage.TEXT_HI_MUTED_BOTH.equals(m.getText()))
                {
                    toast = Toast.makeText(getActivity(), R.string.com_sonova_difian_ui_fitting_chatlistadapter_messagetext_system_muted_both, Toast.LENGTH_LONG);
                }
                else if (ChatMessage.TEXT_HI_UNMUTED_LEFT.equals(m.getText()))
                {
                    toast = Toast.makeText(getActivity(), R.string.com_sonova_difian_ui_fitting_chatlistadapter_messagetext_system_unmuted_left, Toast.LENGTH_LONG);
                }
                else if (ChatMessage.TEXT_HI_UNMUTED_RIGHT.equals(m.getText()))
                {
                    toast = Toast.makeText(getActivity(), R.string.com_sonova_difian_ui_fitting_chatlistadapter_messagetext_system_unmuted_right, Toast.LENGTH_LONG);
                }
                else if (ChatMessage.TEXT_HI_UNMUTED_BOTH.equals(m.getText()))
                {
                    toast = Toast.makeText(getActivity(), R.string.com_sonova_difian_ui_fitting_chatlistadapter_messagetext_system_unmuted_both, Toast.LENGTH_LONG);
                }
            }
            else if (m.getSource() == ChatMessageSource.AUDIOLOGIST)
            {
                AudioManager audio = (AudioManager)getActivity().getSystemService(Context.AUDIO_SERVICE);
                if ((audio.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE) || (audio.getRingerMode() == AudioManager.RINGER_MODE_NORMAL))
                {
                    Vibrator v = (Vibrator)getActivity().getSystemService(Context.VIBRATOR_SERVICE);
                    v.vibrate(350);
                }
            }
        }
        _adapter.setBinder(binder);
        _adapter.notifyDataSetChanged();

        if (toast != null)
        {
            toast.show();
        }
    }
}
