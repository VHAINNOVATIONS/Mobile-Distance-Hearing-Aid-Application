// Copyright (c) 2014 Phonak, Inc. and Oberon microsystems, Inc. All rights reserved.

package com.sonova.difian.ui.fitting;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.sonova.difian.R;
import com.sonova.difian.communication.FittingBinder;
import com.sonova.difian.communication.chat.ChatMessage;
import com.sonova.difian.communication.chat.ChatMessageSource;
import com.sonova.difian.communication.messaging.SmartMessage;
import com.sonova.difian.communication.messaging.SmartMessageOption;
import com.sonova.difian.communication.messaging.SmartMessageOptionsType;
import com.sonova.difian.utilities.Contract;

import java.util.ArrayList;
import java.util.List;

public final class ChatListAdapter extends BaseAdapter
{
    private final Context _context;
    private final List<ChatMessage> _messages;
    @SuppressWarnings("FieldHasSetterButNoGetter")
    private FittingBinder _binder;
    private boolean _isTyping;
    private final Object _typingMessage = new Object();

    public ChatListAdapter(Context context, List<ChatMessage> messages)
    {
        Contract.check(context != null);
        Contract.check(messages != null);

        _context = context;
        //noinspection AssignmentToCollectionOrArrayFieldFromParameter
        _messages = messages;
    }

    @SuppressWarnings("RefusedBequest")
    @Override
    public boolean hasStableIds()
    {
        return true;
    }

    @SuppressWarnings("RefusedBequest")
    @Override
    public boolean isEnabled(int position)
    {
        return false;
    }

    @SuppressWarnings("RefusedBequest")
    @Override
    public int getItemViewType(int position)
    {
        int result;
        Object item = getItem(position);
        if (item == _typingMessage)
        {
            result = 0;
        }
        else
        {
            ChatMessage m = (ChatMessage)item;

            if (m.getSource() == ChatMessageSource.AUDIOLOGIST)
            {
                result = 1;
            }
            else if (m.getSource() == ChatMessageSource.SYSTEM)
            {
                if (ChatMessage.TEXT_HI_MUTED_LEFT.equals(m.getText()) || ChatMessage.TEXT_HI_MUTED_RIGHT.equals(m.getText()) || ChatMessage.TEXT_HI_MUTED_BOTH.equals(m.getText()) || ChatMessage.TEXT_HI_UNMUTED_LEFT.equals(m.getText()) || ChatMessage.TEXT_HI_UNMUTED_RIGHT.equals(m.getText()) || ChatMessage.TEXT_HI_UNMUTED_BOTH.equals(m.getText()))
                {
                    result = 3;
                }
                else
                {
                    result = 2;
                }
            }
            else
            {
                Contract.check(m.getSource() == ChatMessageSource.USER);
                if (m.isAborted())
                {
                    result = 3;
                }
                else
                {
                    result = 4;
                }
            }
        }
        return result;
    }

    @SuppressWarnings("RefusedBequest")
    @Override
    public int getViewTypeCount()
    {
        return 5;
    }

    public void setBinder(FittingBinder binder)
    {
        _binder = binder;
        if (_binder != null)
        {
            _isTyping = _binder.isAudiologistTyping();
        }
    }

    @SuppressWarnings("PackageVisibleField")
    private static final class ViewHolder
    {
        TextView _messageText;
        ImageView _messageImage;
        ViewGroup _messageButtons;
        ProgressBar _messagePending;
        ImageView _messageFailed;
    }

    @Override
    public int getCount()
    {
        int result = _messages.size();
        if (_isTyping)
        {
            result++;
        }
        return result;
    }


    @Override
    public Object getItem(int i)
    {
        Object result;
        if (_isTyping && (i == (getCount() - 1)))
        {
            result = _typingMessage;
        }
        else
        {
            result = _messages.get(i);
        }
        return result;
    }


    @SuppressWarnings("IntegerMultiplicationImplicitCastToLong")
    @Override
    public long getItemId(int i)
    {
        Object item = getItem(i);
        long result;
        if (item == _typingMessage)
        {
            result = 1 << 3;
        }
        else
        {
            ChatMessage m = (ChatMessage)item;
            result = i << 4;
            if (m.getSource() == ChatMessageSource.USER)
            {
                int acked = m.isAcked() ? 1 : 0;
                int failed = m.isFailed() ? 1 : 0;
                int aborted = m.isAborted() ? 1 : 0;
                result |= (acked << 2) | (failed << 1) | aborted;
            }
        }
        return result;
    }


    @Override
    public View getView(int i, View view, ViewGroup viewGroup)
    {
        View result;

        Object item = getItem(i);
        if (item == _typingMessage)
        {
            result = LayoutInflater.from(_context).inflate(R.layout.com_sonova_difian_ui_fitting_chatfragment_row_typing, viewGroup, false);
        }
        else
        {
            final ChatMessage m = (ChatMessage)item;

            // http://developer.android.com/training/improving-layouts/smooth-scrolling.html , 23.05.2013
            final ViewHolder holder;
            if (view == null)
            {
                holder = new ViewHolder();

                if (m.getSource() == ChatMessageSource.AUDIOLOGIST)
                {
                    result = LayoutInflater.from(_context).inflate(R.layout.com_sonova_difian_ui_fitting_chatfragment_row_audiologist, viewGroup, false);
                }
                else if (m.getSource() == ChatMessageSource.SYSTEM)
                {
                    if (ChatMessage.TEXT_HI_MUTED_LEFT.equals(m.getText()) || ChatMessage.TEXT_HI_MUTED_RIGHT.equals(m.getText()) || ChatMessage.TEXT_HI_MUTED_BOTH.equals(m.getText()) || ChatMessage.TEXT_HI_UNMUTED_LEFT.equals(m.getText()) || ChatMessage.TEXT_HI_UNMUTED_RIGHT.equals(m.getText()) || ChatMessage.TEXT_HI_UNMUTED_BOTH.equals(m.getText()))
                    {
                        result = LayoutInflater.from(_context).inflate(R.layout.com_sonova_difian_ui_fitting_chatfragment_row_hidden, viewGroup, false);
                    }
                    else
                    {
                        result = LayoutInflater.from(_context).inflate(R.layout.com_sonova_difian_ui_fitting_chatfragment_row_system, viewGroup, false);
                    }
                }
                else
                {
                    Contract.check(m.getSource() == ChatMessageSource.USER);

                    if (m.isAborted())
                    {
                        result = LayoutInflater.from(_context).inflate(R.layout.com_sonova_difian_ui_fitting_chatfragment_row_hidden, viewGroup, false);
                    }
                    else
                    {
                        result = LayoutInflater.from(_context).inflate(R.layout.com_sonova_difian_ui_fitting_chatfragment_row_user, viewGroup, false);
                    }
                }
                holder._messageText = (TextView)result.findViewById(R.id.com_sonova_difian_ui_fitting_chatfragment_row_messagetext);
                holder._messageImage = (ImageView)result.findViewById(R.id.com_sonova_difian_ui_fitting_chatfragment_row_messageimage);
                holder._messageButtons = (ViewGroup)result.findViewById(R.id.com_sonova_difian_ui_fitting_chatfragment_row_messagebuttons);
                holder._messagePending = (ProgressBar)result.findViewById(R.id.com_sonova_difian_ui_fitting_chatfragment_row_pending);
                holder._messageFailed = (ImageView)result.findViewById(R.id.com_sonova_difian_ui_fitting_chatfragment_row_failed);
                result.setTag(holder);
            }
            else
            {
                result = view;
                holder = (ViewHolder)view.getTag();
            }

            if (m.getSource() == ChatMessageSource.AUDIOLOGIST)
            {
                holder._messageText.setText(m.getText());
                holder._messageImage.setImageBitmap(m.getImage());
                holder._messageButtons.removeAllViews();

                if (m.getImage() == null)
                {
                    holder._messageImage.setVisibility(View.GONE);
                }
                else
                {
                    holder._messageImage.setVisibility(View.VISIBLE);
                }

                if ((m.getMessage().getOptionsCount() == 0) || (m.getMessage().getOptionsType() == SmartMessageOptionsType.TEXT))
                {
                    holder._messageButtons.setVisibility(View.GONE);
                }
                else
                {
                    holder._messageButtons.setVisibility(View.VISIBLE);

                    LayoutInflater inflater = LayoutInflater.from(_context);

                    final SmartMessage msg = m.getMessage();

                    for (int j = 0; j < msg.getOptionsCount(); j++)
                    {
                        final SmartMessageOption option = msg.getOption(j);

                        Button button = (Button)inflater.inflate(R.layout.com_sonova_difian_ui_fitting_chatfragment_row_button, holder._messageButtons, false);
                        button.setText(option.getText());
                        button.setOnClickListener(new View.OnClickListener()
                        {
                            @Override
                            public void onClick(View unused)
                            {
                                Log.i(ChatListAdapter.class.getSimpleName(), "Button clicked. Sending message...");
                                List<SmartMessageOption> options = new ArrayList<SmartMessageOption>();
                                options.add(option);
                                ChatMessage m = new ChatMessage(ChatMessageSource.USER, msg, options);
                                _binder.sendChatMessage(m);
                                Log.i(ChatListAdapter.class.getSimpleName(), "Button clicked. Message sent.");
                            }
                        });
                        holder._messageButtons.addView(button);
                    }
                }
            }
            else if ((m.getSource() == ChatMessageSource.USER) && !m.isAborted())
            {
                if (m.getMessage() == null)
                {
                    holder._messageText.setText(m.getText());
                }
                else
                {
                    StringBuilder text = new StringBuilder();
                    for (int j = 0; j < m.getSelection().size(); j++)
                    {
                        if (j != 0)
                        {
                            text.append(';');
                        }
                        text.append(m.getSelection().get(j).getText());
                    }
                    holder._messageText.setText(text);
                }
                if (m.isAcked())
                {
                    holder._messagePending.setVisibility(View.GONE);
                    holder._messageFailed.setVisibility(View.GONE);
                }
                else if (m.isFailed())
                {
                    holder._messagePending.setVisibility(View.GONE);
                    holder._messageFailed.setVisibility(View.VISIBLE);

                    // https://developer.android.com/guide/topics/ui/menus.html#PopupMenu
                    holder._messageFailed.setOnClickListener(new View.OnClickListener()
                    {
                        @Override
                        public void onClick(View unused)
                        {
                            PopupMenu popup = getFailedPopupMenu(holder._messageFailed, m);
                            popup.show();
                        }
                    });
                }
                else
                {
                    holder._messagePending.setVisibility(View.VISIBLE);
                    holder._messageFailed.setVisibility(View.GONE);
                }
            }
            else if (m.getSource() == ChatMessageSource.SYSTEM)
            {
                String t = m.getText();
                if (ChatMessage.TEXT_CONNECTED.equals(t))
                {
                    holder._messageText.setText(_context.getString(R.string.com_sonova_difian_ui_fitting_chatlistadapter_messagetext_system_connected));
                }
                else if (ChatMessage.TEXT_DISCONNECTED.equals(t))
                {
                    holder._messageText.setText(_context.getString(R.string.com_sonova_difian_ui_fitting_chatlistadapter_messagetext_system_disconnected));
                }
            }
        }

        return result;
    }


    public PopupMenu getFailedPopupMenu(View failedIndicator, final ChatMessage message)
    {
        PopupMenu popup = new PopupMenu(_context, failedIndicator);
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener()
        {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem)
            {
                boolean result = false;
                int id = menuItem.getItemId();
                if (id == R.id.com_sonova_difian_ui_fitting_chatfragment_delivery_failed_menu_retry)
                {
                    Log.i(ChatListAdapter.class.getSimpleName(), "Retrying message");
                    message.abort();
                    _binder.sendChatMessage(new ChatMessage(message));
                    notifyDataSetChanged();
                    result = true;
                }
                else if (id == R.id.com_sonova_difian_ui_fitting_chatfragment_delivery_failed_menu_delete)
                {
                    Log.i(ChatListAdapter.class.getSimpleName(), "Aborting message");
                    message.abort();
                    notifyDataSetChanged();
                    result = true;
                }
                return result;
            }
        });
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.com_sonova_difian_ui_fitting_chatfragment_delivery_failed, popup.getMenu());
        return popup;
    }
}
