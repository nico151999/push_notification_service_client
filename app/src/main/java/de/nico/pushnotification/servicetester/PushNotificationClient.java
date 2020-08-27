package de.nico.pushnotification.servicetester;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class PushNotificationClient {
    private final Socket mClientSocket;
    private final PrintWriter mOut;
    private MessageReceiver mMessageReceiver;
    private final String mIp;
    private final int mPort;

    public PushNotificationClient(String ip, int port) throws IOException {
        mIp = ip;
        mPort = port;
        mClientSocket = new Socket(ip, port);
        mOut = new PrintWriter(mClientSocket.getOutputStream(), true);
        mMessageReceiver = new MessageReceiver(mClientSocket);
    }

    public String getIp() {
        return mIp;
    }

    public int getPort() {
        return mPort;
    }

    private static final class MessageReceiver extends Thread {
        private List<Consumer<String>> mmOnReceiveMessageListeners;
        private Socket mmClientSocket;
        private BufferedReader mmIn;
        private boolean mmAliveSynchronously;

        private MessageReceiver(Socket clientSocket) throws IOException {
            mmOnReceiveMessageListeners = new ArrayList<>();
            mmClientSocket = clientSocket;
            mmIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        }

        private void addOnReceiveMessageListeners(List<Consumer<String>> listeners) {
            mmOnReceiveMessageListeners.addAll(listeners);
        }

        private void removeOnReceiveMessageListener(Consumer<String> listener) {
            mmOnReceiveMessageListeners.remove(listener);
        }

        private boolean isAliveSynchronously() {
            return mmAliveSynchronously;
        }

        private void setAliveSynchronously(boolean aliveSynchronously) {
            this.mmAliveSynchronously = aliveSynchronously;
        }

        @Override
        public void run() {
            if (!mmAliveSynchronously) {
                Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
            }
            while (mmAliveSynchronously || (!mmClientSocket.isClosed() && !isInterrupted())) {
                try {
                    String message = mmIn.readLine();
                    mmOnReceiveMessageListeners.forEach(listener -> listener.accept(message));
                } catch (IOException e) {
                    if (mmAliveSynchronously)  {
                        mmAliveSynchronously = false;
                        close();
                    } else {
                        interrupt();
                    }
                    break;
                }
            }
        }

        private void close() {
            try {
                mmIn.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void interrupt() {
            super.interrupt();
            close();
        }
    }

    public void sendMessage(String msg) {
        synchronized (mOut) {
            mOut.println(msg);
        }
    }

    public synchronized void addOnReceiveMessageListeners(List<Consumer<String>> listeners, boolean synchronous) throws IOException {
        if (mMessageReceiver.isInterrupted()) {
            mMessageReceiver = new MessageReceiver(mClientSocket);
        }
        mMessageReceiver.addOnReceiveMessageListeners(listeners);
        if (!mMessageReceiver.isAlive() && !mMessageReceiver.isAliveSynchronously()) {
            if (synchronous) {
                mMessageReceiver.setAliveSynchronously(true);
                mMessageReceiver.run();
            } else {
                mMessageReceiver.start();
            }
        }
    }

    public synchronized void removeOnReceiveMessageListener(Consumer<String> listener) {
        mMessageReceiver.removeOnReceiveMessageListener(listener);
    }

    public synchronized void removeOnReceiveMessageListeners() {
        mMessageReceiver.interrupt();
    }

    public synchronized void stopConnection() throws IOException {
        mMessageReceiver.interrupt();
        mOut.close();
        mClientSocket.close();
    }
}