// Copyright (c) 2014 Phonak, Inc. and Oberon microsystems, Inc. All rights reserved.

package com.sonova.difian.communication.chat;

import android.util.Log;
import com.sonova.difian.communication.messaging.HiMuteStatus;
import com.sonova.difian.communication.messaging.HiSide;
import com.sonova.difian.communication.messaging.HiStatusMessage;
import com.sonova.difian.communication.messaging.Message;
import com.sonova.difian.communication.messaging.MessageReader;
import com.sonova.difian.communication.messaging.MessageReaderException;
import com.sonova.difian.communication.messaging.MessageType;
import com.sonova.difian.communication.messaging.MessageWriter;
import com.sonova.difian.communication.messaging.SmartMessage;
import com.sonova.difian.communication.messaging.SmartReplyMessage;
import com.sonova.difian.communication.messaging.StreamHelpers;
import com.sonova.difian.communication.messaging.TypingMessage;
import com.sonova.difian.communication.messaging.UserInfoMessage;
import com.sonova.difian.communication.messaging.UserInfoRequestMessage;
import com.sonova.difian.communication.utilities.SocketHelpers;
import com.sonova.difian.utilities.Contract;
import com.sonova.difian.utilities.StringHelpers;
import org.xmlpull.v1.XmlPullParserException;
import org.yaler.AcceptCallback;
import org.yaler.AcceptCallbackState;
import org.yaler.YalerSSLServerSocket;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.AbstractList;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ChatManager
{
    private static final String TAG = ChatManager.class.getName();
    private static final int RX_SOCKET_COUNT = 2;
    private static final int TX_SOCKET_COUNT = 1;
    private static final int SOCKET_TIMEOUT = 10000;
    private final ChatManagerCallback _delegate;
    private final Object _lockObject = new Object();
    private final Object _txLockObject = new Object();
    private final SocketFactory _sslSocketFactory = SSLSocketFactory.getDefault();
    private final Pattern _locationPattern = Pattern.compile("^Location\\: https://([a-z0-9\\-\\.]+)\\:443/[a-z0-9]+\\_\\-1$", Pattern.CASE_INSENSITIVE);
    private final Pattern _chatMessagePattern = Pattern.compile("^[\u0020-\u7dff\ue000-\ufffd]*$", 0);
    // Guarded by _lockObject.
    private final AbstractList<YalerSSLServerSocket> _rxChatListenerSockets = new ArrayList<YalerSSLServerSocket>(RX_SOCKET_COUNT);
    // Guarded by _lockObject.
    private final AbstractList<Socket> _rxChatSockets = new ArrayList<Socket>();
    // Guarded by _lockObject.
    private final AbstractList<Socket> _txChatSockets = new ArrayList<Socket>(TX_SOCKET_COUNT);
    // Guarded by _lockObject.
    private final ArrayList<ChatMessage> _chatHistory = new ArrayList<ChatMessage>(4);
    // Guarded by _lockObject, _txLockObject.
    private final Queue<ChatMessage> _pendingSends = new ArrayDeque<ChatMessage>(2);
    // Guarded by _lockObject, _txLockObject.
    private boolean _pendingAudiologistInfo;
    // Guarded by _lockObject.
    private String _id;
    // Guarded by _lockObject.
    private String _relayHost;
    // Guarded by _lockObject.
    private AudiologistInfo _audiologistInfo = AudiologistInfo.EMPTY;
    // Guarded by _lockObject.
    private HiMuteStatus _muteStatusLeft = HiMuteStatus.UNDEFINED;
    // Guarded by _lockObject.
    private HiMuteStatus _muteStatusRight = HiMuteStatus.UNDEFINED;
    // Guarded by _lockObject.
    private boolean _isTyping;

    public ChatManager(ChatManagerCallback delegate)
    {
        synchronized (_lockObject)
        {
            Contract.check(delegate != null);
            _delegate = delegate;
        }
    }

    public void startup(String id, String relayHost)
    {
        synchronized (_txLockObject) {
            synchronized (_lockObject) {
                Log.v(TAG, String.format("startup, id = %s", id));
                Contract.check(id != null);
                Contract.check(relayHost != null);

                if (!id.equals(_id) || !relayHost.equals(_relayHost)) {
                    closeSockets();
                    _id = id;
                    _relayHost = relayHost;
                    openSockets();
                    requestAudiologistInfo();
                }
            }
        }
    }

    private void closeSockets()
    {
        synchronized (_lockObject)
        {
            for (YalerSSLServerSocket s : _rxChatListenerSockets)
            {
                s.close();
            }
            for (Socket s : _rxChatSockets)
            {
                try
                {
                    s.close();
                }
                catch (IOException ignored)
                {
                }
            }
            for (Socket s : _txChatSockets)
            {
                try
                {
                    s.close();
                }
                catch (IOException ignored)
                {
                }
            }
            _rxChatListenerSockets.clear();
            _rxChatSockets.clear();
            _txChatSockets.clear();
        }
    }

    private void openSockets()
    {
        synchronized (_lockObject)
        {
            Contract.check(_id != null);

            while (_rxChatListenerSockets.size() < RX_SOCKET_COUNT)
            {
                YalerSSLServerSocket chatListener = new YalerSSLServerSocket(_relayHost, 443, String.format("%s_1", _id));
                _rxChatListenerSockets.add(chatListener);
                startListening(chatListener, _id);
            }

            while (_txChatSockets.size() < TX_SOCKET_COUNT)
            {
                Socket socket = SocketHelpers.createSocket(_sslSocketFactory, SOCKET_TIMEOUT);
                _txChatSockets.add(socket);
                startTx(socket, _id);
            }
        }
    }

    private void startTx(final Socket initialSocket, final String id)
    {
        Log.i(TAG, "startTx");

        synchronized (_lockObject)
        {
            new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        Log.v(TAG, "TX thread active.");

                        Socket socket = initialSocket;
                        String currentId;
                        String currentHost;
                        synchronized (_lockObject)
                        {
                            currentId = _id;
                            currentHost = _relayHost;
                        }

                        // Pending message.
                        Message m = null;

                        while (id.equals(currentId))
                        {
                            boolean ok = true;
                            boolean resetHost = true;

                            // Connect socket.
                            Log.d(TAG, String.format("Connecting socket for TX (host = %s).", currentHost));

                            try
                            {
                                socket.connect(new InetSocketAddress(currentHost, 443), SOCKET_TIMEOUT);
                            }
                            catch (IOException e)
                            {
                                Log.w(TAG, "Socket.connect failed.", e);
                                ok = false;
                            }

                            InputStream is = null;
                            OutputStream os = null;

                            if (ok)
                            {
                                try
                                {
                                    is = socket.getInputStream();
                                    os = socket.getOutputStream();
                                }
                                catch (IOException e)
                                {
                                    ok = false;
                                }
                            }
                            else
                            {
                                Log.w(TAG, "Clearing pending sends.");
                                synchronized (_txLockObject) // ok to take _txLockObject: _lockObject is not taken
                                {
                                    for (ChatMessage cm : _pendingSends)
                                    {
                                        while (!cm.isFailed())
                                        {
                                            cm.retry();
                                        }
                                    }
                                    if ((m != null) && (m.getType() == MessageType.SMART_REPLY))
                                    {
                                        SmartReplyMessage sm = (SmartReplyMessage)m;
                                        while (!sm.isFailed())
                                        {
                                            sm.retry();
                                        }
                                        m = null;
                                    }
                                    _pendingSends.clear();
                                    _txLockObject.notifyAll();
                                }
                                _delegate.chatManagerStateChanged();
                            }

                            // Message processing loop.
                            while (ok)
                            {
                                // Check if current message delivery should be attempted again.
                                if ((m != null) && (m.getType() == MessageType.SMART_REPLY))
                                {
                                    SmartReplyMessage sm = (SmartReplyMessage)m;
                                    Log.i(TAG, "Retrying delivery of chat message...");
                                    synchronized (_txLockObject) // ok to take _txLockObject: _lockObject is not taken
                                    {
                                        sm.retry();
                                        if (sm.isFailed())
                                        {
                                            m = null;
                                        }
                                    }
                                    _delegate.chatManagerStateChanged();
                                }
                                else if ((m != null) && (m.getType() == MessageType.USER_INFO_REQUEST))
                                {
                                    Log.i(TAG, "Cancelling user info request message in order to give potential smart messages priority...");
                                    m = null;
                                }

                                // Get message.
                                if (m == null)
                                {
                                    m = getMessage(id);
                                    Log.v(TAG, "Processing message " + m);
                                }

                                // Transmit.
                                if (m != null)
                                {
                                    Log.i(TAG, String.format("Sending message: %s", m));
                                    try
                                    {
                                        MessageWriter.getInstance().transmit(os, currentHost, id, m);
                                    }
                                    catch (IOException e)
                                    {
                                        ok = false;
                                    }
                                }
                                else
                                {
                                    ok = false;
                                }

                                // Receive ack.
                                if (ok && (m != null))
                                {
                                    try
                                    {
                                        String headerLine = StreamHelpers.readLine(is);
                                        Log.i(TAG, String.format("Received response code: %s", headerLine));

                                        String http100 = "HTTP/1.1 100 Continue";
                                        String http200 = "HTTP/1.1 200 OK";
                                        String http307 = "HTTP/1.1 307";

                                        if (headerLine.startsWith(http100) || headerLine.startsWith(http200))
                                        {
                                            // Message transmitted successfully.

                                            if (m.getType() == MessageType.SMART_REPLY)
                                            {
                                                synchronized (_txLockObject) // ok to take _txLockObject: _lockObject is not taken
                                                {
                                                    SmartReplyMessage srm = (SmartReplyMessage)m;
                                                    srm.ack();
                                                }
                                                _delegate.chatManagerStateChanged();
                                            }

                                            m = null;
                                            resetHost = false;
                                        }
                                        else
                                        {
                                            resetHost = true;
                                        }

                                        if (!headerLine.startsWith(http200))
                                        {
                                            // Connection closed.
                                            ok = false;
                                        }

                                        if (headerLine.startsWith(http307))
                                        {
                                            // Redirect.
                                            resetHost = false;

                                            do
                                            {
                                                headerLine = StreamHelpers.readLine(is);
                                                if (headerLine.startsWith("Location: "))
                                                {
                                                    Matcher matcher = _locationPattern.matcher(headerLine);
                                                    if (matcher.matches() && (matcher.groupCount() == 1))
                                                    {
                                                        currentHost = matcher.group(1);
                                                        Log.i(TAG, String.format("Redirected to new host: %s ", currentHost));
                                                    }
                                                    else
                                                    {
                                                        resetHost = true;
                                                    }
                                                }
                                            } while (!headerLine.equals(""));
                                            if (currentHost == null)
                                            {
                                                resetHost = true;
                                            }
                                        }
                                        else
                                        {
                                            StreamHelpers.consumeHttpHeaders(is);
                                        }
                                    }
                                    catch (IOException e)
                                    {
                                        ok = false;
                                    }
                                }
                            }

                            // Replace socket.
                            if (resetHost)
                            {
                                try
                                {
                                    Thread.sleep(4200);
                                }
                                catch (InterruptedException ignored)
                                {
                                }
                            }

                            synchronized (_lockObject)
                            {
                                try
                                {
                                    socket.close();
                                }
                                catch (IOException ignored)
                                {
                                }

                                currentId = _id;

                                if (resetHost)
                                {
                                    currentHost = _relayHost;
                                }

                                if (id.equals(currentId))
                                {
                                    Log.i(TAG, "Creating new socket");

                                    _txChatSockets.remove(socket);
                                    socket = SocketHelpers.createSocket(_sslSocketFactory, SOCKET_TIMEOUT);
                                    _txChatSockets.add(socket);
                                }
                            }
                        }
                    }
                    catch (Throwable t)
                    {
                        Log.e(TAG, "Uncaught exception in startTx()", t);
                        throw new RuntimeException(t);
                    }
                }
            }).start();
        }
    }

    private Message getMessage(String id)
    {
        Log.d(TAG, String.format("getMessage(%s) pre txLock", id));
        synchronized (_txLockObject) // ok to take _txLockObject: _lockObject is not taken by caller of getMessage
        {
            Log.d(TAG, String.format("getMessage(%s) in txLock", id));
            Message result = null;

            String currentId;
            Log.d(TAG, String.format("getMessage(%s) pre Lock", id));
            synchronized (_lockObject)
            {
                Log.d(TAG, String.format("getMessage(%s) in Lock", id));
                currentId = _id;
            }

            while ((result == null) && id.equals(currentId))
            {
                synchronized (_lockObject)
                {
                    currentId = _id;

                    if (id.equals(currentId))
                    {
                        if (!_pendingSends.isEmpty())
                        {
                            ChatMessage message = _pendingSends.poll();
                            result = new SmartReplyMessage(message);
                        }
                        else if (_pendingAudiologistInfo)
                        {
                            result = new UserInfoRequestMessage();
                        }
                    }
                }

                if (id.equals(currentId) && (result == null))
                {
                    try
                    {
                        _txLockObject.wait();
                    }
                    catch (InterruptedException ignored)
                    {
                    }
                }
            }

            return result;
        }
    }

    private void startListening(final YalerSSLServerSocket listener, final String id)
    {
        Log.i(TAG, "startListening");

        synchronized (_lockObject)
        {
            new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        String currentId;
                        synchronized (_lockObject)
                        {
                            currentId = _id;
                        }

                        while (id.equals(currentId))
                        {
                            Socket s;
                            try
                            {
                                s = listener.accept(new AcceptCallback()
                                {
                                    @Override
                                    public void statusChanged(AcceptCallbackState state)
                                    {
                                        Log.v(TAG, String.format("%x %s", listener.hashCode(), state.toString()));
                                    }
                                });
                            }
                            catch (IOException e)
                            {
                                s = null;
                            }

                            synchronized (_lockObject)
                            {
                                currentId = _id;

                                if (s != null)
                                {
                                    if (id.equals(currentId))
                                    {
                                        startRx(s, id);
                                    }
                                    else
                                    {
                                        try
                                        {
                                            s.close();
                                        }
                                        catch (IOException ignored)
                                        {
                                        }
                                    }
                                }
                            }
                        }
                    }
                    catch (Throwable t)
                    {
                        Log.e(TAG, "Uncaught exception in startListening()", t);
                        throw new RuntimeException(t);
                    }
                }
            }).start();
        }
    }

    private void startRx(final Socket s, final String id)
    {
        Log.i(TAG, "startRx");

        synchronized (_lockObject)
        {
            boolean success = _rxChatSockets.add(s);
            Contract.check(success);

            new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        boolean ok = true;

                        String currentId;
                        synchronized (_lockObject)
                        {
                            currentId = _id;
                        }

                        while (ok && id.equals(currentId))
                        {
                            Message message = null;
                            try
                            {
                                InputStream stream = s.getInputStream();
                                boolean continueExpected = StreamHelpers.consumeHttpHeaders(stream);
                                if (continueExpected) {
                                    byte[] continueRspBytes =
                                        StringHelpers.convertToBytes(
                                            "HTTP/1.1 100 Continue\r\nContent-Length: 0\r\n\r\n");
                                    try
                                    {
                                        s.getOutputStream().write(continueRspBytes);
                                        s.getOutputStream().flush();
                                        Log.v(TAG, "Sent 100 Continue response");
                                    }
                                    catch (IOException e)
                                    {
                                        Log.w(TAG, "IOException while writing chat continue response.", e);
                                        ok = false;
                                    }
                                }
                                if (ok)
                                {
                                    message = MessageReader.getInstance().parse(stream);
                                }
                            }
                            catch (IOException e)
                            {
                                Log.w(TAG, "IOException while parsing chat message.", e);
                                ok = false;
                            }
                            catch (XmlPullParserException e)
                            {
                                Log.w(TAG, "XmlPullParserException while parsing chat message", e);
                                ok = false;
                            }
                            catch (MessageReaderException e)
                            {
                                Log.w(TAG, String.format("MessageReaderException while parsing chat message: %s", e.getMessage()));
                                ok = false;
                            }

                            synchronized (_lockObject)
                            {
                                currentId = _id;

                                if (message != null)
                                {
                                    if (id.equals(currentId))
                                    {
                                        handleMessage(message);
                                    }
                                }
                            }

                            byte[] rspBytes = StringHelpers.convertToBytes("HTTP/1.1 200 OK\r\nContent-Length: 0\r\n\r\n");

                            try
                            {
                                s.getOutputStream().write(rspBytes);
                            }
                            catch (IOException e)
                            {
                                Log.w(TAG, "IOException while writing chat response.", e);
                                ok = false;
                            }

                            synchronized (_lockObject)
                            {
                                currentId = _id;
                            }
                        }

                        synchronized (_lockObject)
                        {
                            try
                            {
                                s.close();
                            }
                            catch (IOException e)
                            {
                                Log.w(TAG, "IOException while closing chat socket.", e);
                            }
                            _rxChatSockets.remove(s);
                        }
                    }
                    catch (Throwable t)
                    {
                        Log.e(TAG, "Uncaught exception in startRx()", t);
                        throw new RuntimeException(t);
                    }
                }
            }).start();
        }
    }

    private void handleMessage(Message message)
    {
        synchronized (_lockObject)
        {
            Contract.check(message != null);

            Log.i(TAG, String.format("Retrieved message: %s", message));

            if (message.getType() == MessageType.USER_INFO)
            {
                _pendingAudiologistInfo = false;
                UserInfoMessage m = (UserInfoMessage)message;

                _audiologistInfo = new AudiologistInfo(m.getName(), m.getImage());
            }
            else if (message.getType() == MessageType.HI_STATUS)
            {
                HiStatusMessage m = (HiStatusMessage)message;

                HiMuteStatus newLeft = m.getMuteStatus(HiSide.LEFT);
                HiMuteStatus newRight = m.getMuteStatus(HiSide.RIGHT);

                if ((_muteStatusLeft != newLeft) && (_muteStatusRight != newRight) && (newLeft == newRight))
                {
                    if (newLeft == HiMuteStatus.MUTED)
                    {
                        addMessage(new ChatMessage(ChatMessageSource.SYSTEM, ChatMessage.TEXT_HI_MUTED_BOTH));
                    }
                    else if (newLeft == HiMuteStatus.UNMUTED)
                    {
                        addMessage(new ChatMessage(ChatMessageSource.SYSTEM, ChatMessage.TEXT_HI_UNMUTED_BOTH));
                    }
                }
                else
                {
                    if (_muteStatusLeft != newLeft)
                    {
                        if (newLeft == HiMuteStatus.MUTED)
                        {
                            addMessage(new ChatMessage(ChatMessageSource.SYSTEM, ChatMessage.TEXT_HI_MUTED_LEFT));
                        }
                        else if (newLeft == HiMuteStatus.UNMUTED)
                        {
                            addMessage(new ChatMessage(ChatMessageSource.SYSTEM, ChatMessage.TEXT_HI_UNMUTED_LEFT));
                        }
                    }
                    if (_muteStatusRight != newRight)
                    {
                        if (newRight == HiMuteStatus.MUTED)
                        {
                            addMessage(new ChatMessage(ChatMessageSource.SYSTEM, ChatMessage.TEXT_HI_MUTED_RIGHT));
                        }
                        else if (newRight == HiMuteStatus.UNMUTED)
                        {
                            addMessage(new ChatMessage(ChatMessageSource.SYSTEM, ChatMessage.TEXT_HI_UNMUTED_RIGHT));
                        }
                    }
                }

                _muteStatusLeft = newLeft;
                _muteStatusRight = newRight;
            }
            else if (message.getType() == MessageType.SMART)
            {
                SmartMessage m = (SmartMessage)message;

                addMessage(new ChatMessage(ChatMessageSource.AUDIOLOGIST, m));

                _isTyping = false;
            }
            else if (message.getType() == MessageType.TYPING)
            {
                TypingMessage m = (TypingMessage)message;

                _isTyping = m.isTyping();
            }
        }
        _delegate.chatManagerStateChanged();
    }

    public void addMessage(ChatMessage message)
    {
        synchronized (_lockObject)
        {
            if (message == null)
            {
                throw new IllegalArgumentException("message is null.");
            }

            _chatHistory.add(message);
        }
        _delegate.chatManagerStateChanged();
    }

    private void requestAudiologistInfo()
    {
        synchronized (_txLockObject) { // ok to take _txLockObject: _lockObject is already taken by caller.
            synchronized (_lockObject) {
                if (AudiologistInfo.EMPTY.equals(_audiologistInfo)) {

                    _pendingAudiologistInfo = true;
                    _txLockObject.notifyAll();
                }
            }
        }
    }

    public void shutdown() {
        synchronized (_txLockObject) { // ok to take _txLockObject: _lockObject is not taken by caller shutdown
            synchronized (_lockObject) {
                _id = null;
                _relayHost = null;
                _audiologistInfo = AudiologistInfo.EMPTY;
                _muteStatusLeft = HiMuteStatus.UNDEFINED;
                _muteStatusRight = HiMuteStatus.UNDEFINED;
                _chatHistory.clear();
                _isTyping = false;

                closeSockets();


                _pendingSends.clear();
                _pendingAudiologistInfo = true;
                _txLockObject.notifyAll();
            }
        }
    }

    public AudiologistInfo getAudiologistInfo()
    {
        synchronized (_lockObject)
        {
            return _audiologistInfo;
        }
    }

    public HiMuteStatus getMuteStatus(HiSide side)
    {
        synchronized (_lockObject)
        {
            HiMuteStatus result = HiMuteStatus.UNDEFINED;
            if (side == HiSide.LEFT)
            {
                result = _muteStatusLeft;
            }
            else if (side == HiSide.RIGHT)
            {
                result = _muteStatusRight;
            }
            return result;
        }
    }

    // Cannot decrease, except when fitting session is stopped where it is reset to 0.
    public int getChatMessageCount()
    {
        synchronized (_lockObject)
        {
            return _chatHistory.size();
        }
    }

    public ChatMessage getChatMessage(int index)
    {
        synchronized (_lockObject)
        {
            return _chatHistory.get(index);
        }
    }

    // Volatile value.
    public boolean isTyping()
    {
        synchronized (_lockObject)
        {
            return _isTyping;
        }
    }

    public void sendChatMessage(ChatMessage message)
    {
        Log.d(TAG, "sendChatMessage: pre lock");
        synchronized (_txLockObject) { // ok to take _txLockObject: _lockObject is not taken by caller of sendChatMessage.
            synchronized (_lockObject) {
                Log.d(TAG, "sendChatMessage: post lock");

                if (message == null) {
                    throw new IllegalArgumentException("message is null.");
                }

                if ((message.getText() != null) && !_chatMessagePattern.matcher(message.getText()).matches()) {
                    // The Android XML Serialization library does not support characters like emoji.
                    // See line 121-137 of https://android.googlesource.com/platform/libcore/+/9edf43dfcc35c761d97eb9156ac4254152ddbc55/xml/src/main/java/org/kxml2/io/KXmlSerializer.java                    
                    throw new IllegalArgumentException("message text contains invalid characters.");
                }

                addMessage(message);

                _pendingSends.add(message);
                _txLockObject.notifyAll();

                Log.i(TAG, "Chat message enqueued: " + message);
            }
        }
    }
}
