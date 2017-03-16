package com.github.zeroxfelix.obd2fun.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.preference.PreferenceManager;

import com.github.pires.obd.commands.ObdCommand;
import com.github.pires.obd.commands.control.VinCommand;
import com.github.pires.obd.commands.protocol.AvailablePidsCommand;
import com.github.pires.obd.exceptions.BusInitException;
import com.github.pires.obd.exceptions.MisunderstoodCommandException;
import com.github.pires.obd.exceptions.NoDataException;
import com.github.pires.obd.exceptions.StoppedException;
import com.github.pires.obd.exceptions.UnableToConnectException;
import com.github.pires.obd.exceptions.UnsupportedCommandException;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import com.github.zeroxfelix.obd2fun.R;
import com.github.zeroxfelix.obd2fun.activity.MainActivity;
import com.github.zeroxfelix.obd2fun.fragment.SettingsFragment;
import com.github.zeroxfelix.obd2fun.obd.ObdBroadcastIntent;
import com.github.zeroxfelix.obd2fun.obd.ObdCommandJob;
import com.github.zeroxfelix.obd2fun.obd.ObdCommandJobResult;
import com.github.zeroxfelix.obd2fun.obd.ObdCommandType;
import com.github.zeroxfelix.obd2fun.obd.ObdConnectionState;
import timber.log.Timber;

public abstract class AbstractObdService extends Service {

    private static final int NOTIFICATION_ID = 123456789;
    private static final int MESSAGE_QUEUE_NOT_EMPTY = 123456789;
    private static final int OBD_COMMAND_TIMEOUT = 5000;
    private static final int OBD_COMMAND_DELAY = 500;

    private final AtomicBoolean isConnectionActive = new AtomicBoolean(false);
    private long sessionId = 0L;

    private NotificationManager notificationManager;
    private LocalBroadcastManager localBroadcastManager;
    private PowerManager.WakeLock wakeLock;
    private InputStream obdConnectionInputStream;
    private OutputStream obdConnectionOutputStream;
    private final IBinder binder = new AbstractObdServiceBinder();
    private final BroadcastReceiver obdCommandJobRequestReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Timber.d("Received new ManageObdCommandJobIntent");
            ObdCommandType obdCommandType = (ObdCommandType) intent.getSerializableExtra("obdCommandType");
            ObdCommandJob.Action obdCommandJobAction = (ObdCommandJob.Action) intent.getSerializableExtra("obdCommandJobAction");
            if (obdCommandType != null && obdCommandJobAction != null) {
                switch (obdCommandJobAction) {
                    case REGISTER_PERIODIC:
                        registerPeriodicObdCommandJob(obdCommandType);
                        break;
                    case UNREGISTER_PERIODIC:
                        unregisterPeriodicObdCommandJob(obdCommandType);
                        break;
                    case REGISTER_ONE_SHOT:
                        Timber.d("Sending one shot %s %s", obdCommandType.getNameForValue(), "command to HandlerThread");
                        sendObdCommandToHandlerThread(obdCommandType, false);
                        sendMessageQueueNotEmptyMessage(false);
                        break;
                    default:
                        Timber.e("ManageObdCommandJobIntent did not contain a known obdCommandJobAction, discarding");
                }
            } else {
                Timber.e("ManageObdCommandJobIntent did not contain obdCommandType and/or obdCommandJobAction, discarding");
            }
        }
    };
    private String currentVIN;
    private final BroadcastReceiver vinObdCommandJobResultReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Timber.d("Received new vinObdCommandJobResult");
            ObdCommandJobResult obdCommandJobResult = intent.getParcelableExtra("obdCommandJobResult");
            if (obdCommandJobResult.getObdCommandType() == ObdCommandType.VIN) {
                if (obdCommandJobResult.getState() == ObdCommandJob.State.FINISHED && !obdCommandJobResult.getFormattedResult().isEmpty()) {
                    Timber.d("Setting current VIN");
                    currentVIN = obdCommandJobResult.getFormattedResult();
                } else {
                    Timber.d("VIN result is empty, setting currentVin to NA");
                    currentVIN = getString(R.string.unknown_vin);
                }
                localBroadcastManager.sendBroadcast(ObdBroadcastIntent.getVinChangedIntent(currentVIN));
                Timber.d("Unregistering receiver for VIN obdCommandJobResult broadcasts");
                localBroadcastManager.unregisterReceiver(vinObdCommandJobResultReceiver);
            }
        }
    };

    private boolean useImperialUnits;
    private long obdCommandJobCounter = 0L;
    private ObdCommandJobHandler obdCommandJobHandler;
    private final HandlerThread obdCommandJobHandlerThread = new HandlerThread("obdCommandJobHandlerThread");

    private int periodicObdCommandJobDelay;
    private int obdCommandJobHandlerBusyCounter = 0;
    private Handler periodicObdCommandJobHandler;
    private final HandlerThread periodicObdCommandJobHandlerThread = new HandlerThread("periodObdCommandJobHandlerThread");
    private final HashMap<ObdCommandType, Integer> periodicObdCommandJobQueue = new HashMap<>();
    private final Runnable executePeriodicObdCommandJobsTask = new Runnable() {
        @Override
        public void run() {
            if (isConnectionActive()) {
                if (obdCommandJobHandler.hasMessages(MESSAGE_QUEUE_NOT_EMPTY)) {
                    obdCommandJobHandlerBusyCounter++;
                    Timber.w("obdCommandJobHandler's message queue was not empty");
                } else {
                    obdCommandJobHandlerBusyCounter = 0;
                }
                if (!periodicObdCommandJobQueue.isEmpty()) {
                    if (obdCommandJobHandlerBusyCounter < 4) {
                        Timber.d("Queuing periodic obdCommandJobs");
                        for(ObdCommandType obdCommandType : periodicObdCommandJobQueue.keySet()){
                            sendObdCommandToHandlerThread(obdCommandType, false);
                        }
                        sendMessageQueueNotEmptyMessage(false);
                    } else {
                        Timber.d("obdCommandJobHandler was busy %d times while trying to queue new periodic jobs, holding off this time", obdCommandJobHandlerBusyCounter);
                    }
                }
            }
            periodicObdCommandJobHandler.postDelayed(executePeriodicObdCommandJobsTask, periodicObdCommandJobDelay);
        }
    };

    private StartObdConnectionAsyncTask startObdConnectionAsyncTask;

    private Handler obdCommandHandler;
    private final HandlerThread obdCommandHandlerThread = new HandlerThread("obdCommandHandlerThread");

    abstract protected ObdConnectionState _startObdConnection();
    abstract protected ObdConnectionState _stopObdConnection();
    abstract protected boolean isSocketConnected();

    @Override
    public void onCreate() {
        Timber.d("onCreate called");
        super.onCreate();

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        localBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AbstractObdServiceWakeLock");

        Timber.d("Getting periodicObdCommandJobDelay");
        periodicObdCommandJobDelay = SettingsFragment.getObdCommandJobDelay();

        Timber.d("Starting obdCommandJobHandlerThread");
        obdCommandJobHandlerThread.start();
        obdCommandJobHandler = new ObdCommandJobHandler(obdCommandJobHandlerThread.getLooper());

        Timber.d("Starting periodicObdCommandJobHandlerThread");
        periodicObdCommandJobHandlerThread.start();
        periodicObdCommandJobHandler = new Handler(periodicObdCommandJobHandlerThread.getLooper());
        periodicObdCommandJobHandler.post(executePeriodicObdCommandJobsTask);

        Timber.d("Starting obdCommandHandlerThread");
        obdCommandHandlerThread.start();
        obdCommandHandler = new Handler(obdCommandHandlerThread.getLooper());

        Timber.d("Registering receiver for ManageObdCommandJob broadcasts");
        localBroadcastManager.registerReceiver(obdCommandJobRequestReceiver, new IntentFilter(ObdBroadcastIntent.MANAGE_OBD_COMMAND_JOB));
    }

    @Override
    public void onDestroy() {
        Timber.d("onDestroy called");
        super.onDestroy();

        Timber.d("Stopping obdCommandJobHandlerThread");
        obdCommandJobHandlerThread.quit();

        Timber.d("Stopping periodicObdCommandJobHandlerThread");
        periodicObdCommandJobHandlerThread.quit();

        Timber.d("Stopping obdCommandHandlerThread");
        obdCommandHandlerThread.quit();

        Timber.d("Unregistering receiver for ManageObdCommandJob broadcasts");
        localBroadcastManager.unregisterReceiver(obdCommandJobRequestReceiver);

        Timber.d("Unregistering possibly left receiver for VIN obdCommandJobResult broadcasts");
        localBroadcastManager.unregisterReceiver(vinObdCommandJobResultReceiver);

        if (startObdConnectionAsyncTask != null) {
            Timber.d("Cancelling ongoing startObdConnectionAsyncTask");
            startObdConnectionAsyncTask.cancel(true);
        }

        if (isConnectionActive()) {
            Timber.d("Stopping OBD connection and outstanding connecting attempts");
            stopObdConnection(true);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private Notification buildNotification(String contentText) {
        return new NotificationCompat.Builder(getApplicationContext())
                .setContentTitle(getString(R.string.app_name) + " - " + getString(R.string.obd_service_name))
                .setContentText(contentText)
                .setSmallIcon(android.R.drawable.sym_def_app_icon)
                .setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0))
                .setWhen(System.currentTimeMillis())
                .setOngoing(true)
                .build();
    }

    private void showNotification(Notification notification) {
        Timber.d("Showing a new notification");
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    void setObdConnectionInputStream(InputStream obdConnectionInputStream) {
        Timber.d("Setting obdConnectionInputStream");
        this.obdConnectionInputStream = obdConnectionInputStream;
    }

    void setObdConnectionOutputStream(OutputStream obdConnectionOutputStream) {
        Timber.d("Setting obdConnectionOutputStream");
        this.obdConnectionOutputStream = obdConnectionOutputStream;
    }

    private void setIsConnectionActive(boolean isConnectionActive) {
        Timber.d("Setting isConnectionActive to %s", String.valueOf(isConnectionActive));
        this.isConnectionActive.set(isConnectionActive);
        if (isConnectionActive) {
            sessionId = new Date().getTime();
        } else {
            sessionId = 0L;
        }
        Timber.d("New sessionId is %d", sessionId);
    }

    public boolean isConnectionActive() {
        return isConnectionActive.get();
    }

    public long getCurrentSessionId() {
        return sessionId;
    }

    public String getCurrentVIN() { return currentVIN; }

    protected void sendObdConnectionStateBroadcast(ObdConnectionState obdConnectionState) {
        localBroadcastManager.sendBroadcast(ObdBroadcastIntent.getObdConnectionStateIntent(obdConnectionState));
    }

    public void startObdConnection() {
        Timber.d("startObdConnection called");
        if (startObdConnectionAsyncTask == null && !isConnectionActive()) {
            Timber.d("Entering foreground");
            startForeground(NOTIFICATION_ID, buildNotification(getString(R.string.obd_connecting)));

            Timber.d("Acquiring WakeLock");
            wakeLock.acquire();

            Timber.d("Registering receiver for VIN obdCommandJobResult broadcasts");
            localBroadcastManager.registerReceiver(vinObdCommandJobResultReceiver, new IntentFilter(ObdCommandType.VIN.getNameForValue()));

            Timber.d("Getting useImperialUnits from SharedPreferences");
            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            useImperialUnits = sharedPrefs.getBoolean(SettingsFragment.IMPERIAL_UNITS_KEY, false);

            Timber.d("Getting periodicObdCommandJobDelay");
            periodicObdCommandJobDelay = SettingsFragment.getObdCommandJobDelay();

            Timber.d("Executing startObdConnectionAsyncTask");
            startObdConnectionAsyncTask = new StartObdConnectionAsyncTask();
            startObdConnectionAsyncTask.execute();
        } else {
            Timber.w("startObdConnectionAsyncTask was not null or connection already active, nothing to do");
        }
    }

    public void stopObdConnection(boolean silent) {
        Timber.d("stopObdConnection called, silent was set to: %s", String.valueOf(silent));
        setIsConnectionActive(false);

        ObdConnectionState obdConnectionState = _stopObdConnection();
        if (!silent) {
            sendObdConnectionStateBroadcast(obdConnectionState);
        }

        Timber.d("Setting obdCommandJobCounter to 0L");
        obdCommandJobCounter = 0L;

        Timber.d("Setting current VIN to NA");
        currentVIN = getString(R.string.unknown_vin);

        Timber.d("Resetting PersistentCommands");
        VinCommand.reset();
        AvailablePidsCommand.reset();

        if (wakeLock.isHeld()) {
            Timber.d("Releasing WakeLock");
            wakeLock.release();
        } else {
            Timber.d("WakeLock already released, nothing to do");
        }

        Timber.d("Leaving foreground");
        stopForeground(true);
    }

    private void runObdCommandWithTimeout(ObdCommand obdCommand) throws Exception {
        ObdCommandRunnable obdCommandRunnable = new ObdCommandRunnable(obdCommand, Thread.currentThread());
        try {
            obdCommandHandler.post(obdCommandRunnable);
            Thread.sleep(OBD_COMMAND_TIMEOUT);
            // below only reached if command times out
            throw new TimeoutException();
        } catch (InterruptedException ie) {
            if (obdCommandRunnable.getException() != null) {
                throw obdCommandRunnable.getException();
            }
        }
    }

    private void sendObdCommandToHandlerThread(ObdCommandType obdCommandType, boolean delayed) {
        obdCommandJobCounter++;
        Message obdCommandJobMessage = obdCommandJobHandler.obtainMessage();
        ObdCommandJob obdCommandJob = new ObdCommandJob(obdCommandType);
        obdCommandJob.setId(obdCommandJobCounter);
        obdCommandJobMessage.obj = obdCommandJob;
        if (delayed) {
            Timber.d("Sending delayed obdCommandJobMessage to obdCommandJobHandler");
            obdCommandJobHandler.sendMessageDelayed(obdCommandJobMessage, OBD_COMMAND_DELAY);
        } else {
            obdCommandJobHandler.sendMessage(obdCommandJobMessage);
        }
    }

    private void sendMessageQueueNotEmptyMessage(boolean delayed) {
        obdCommandJobHandler.removeMessages(MESSAGE_QUEUE_NOT_EMPTY);
        if (delayed) {
            Timber.d("Sending delayed messageQueueNotEmptyMessage to obdCommandJobHandler");
            obdCommandJobHandler.sendEmptyMessageDelayed(MESSAGE_QUEUE_NOT_EMPTY, OBD_COMMAND_DELAY);
        } else {
            obdCommandJobHandler.sendEmptyMessage(MESSAGE_QUEUE_NOT_EMPTY);
        }
    }

    private void initializeObdInterface() {
        Timber.d("Initializing the OBD interface");
        sendObdCommandToHandlerThread(ObdCommandType.RESET_OBD, false);
        sendObdCommandToHandlerThread(ObdCommandType.ECHO_OFF, true);
        sendObdCommandToHandlerThread(ObdCommandType.ECHO_OFF, true);
        sendObdCommandToHandlerThread(ObdCommandType.LINE_FEED_OFF, true);
        sendObdCommandToHandlerThread(ObdCommandType.ADAPTIVE_TIMING, true);
        sendObdCommandToHandlerThread(ObdCommandType.SELECT_PROTOCOL, true);
        sendObdCommandToHandlerThread(ObdCommandType.VIN, true);
        sendMessageQueueNotEmptyMessage(true);
    }

    private void registerPeriodicObdCommandJob(ObdCommandType obdCommandType) {
        if (periodicObdCommandJobQueue.containsKey(obdCommandType)) {
            Timber.d("Incrementing registeredCount of %s %s", obdCommandType.getNameForValue(), "command");
            periodicObdCommandJobQueue.put(obdCommandType, periodicObdCommandJobQueue.get(obdCommandType) + 1);
        } else {
            Timber.d("Adding %s %s", obdCommandType.getNameForValue(), "command to periodicObdCommandJobQueue");
            periodicObdCommandJobQueue.put(obdCommandType, 1);
        }
    }

    private void unregisterPeriodicObdCommandJob(ObdCommandType obdCommandType) {
        if (periodicObdCommandJobQueue.containsKey(obdCommandType)) {
            int registeredCount = periodicObdCommandJobQueue.get(obdCommandType);
            if (registeredCount > 1) {
                Timber.d("Decrementing registeredCount of %s %s", obdCommandType.getNameForValue(), "command");
                periodicObdCommandJobQueue.put(obdCommandType, registeredCount - 1);
            } else {
                Timber.d("Removing %s %s", obdCommandType.getNameForValue(), "command from periodicObdCommandJobQueue");
                periodicObdCommandJobQueue.remove(obdCommandType);
            }
        } else {
            Timber.e("periodicObdCommandJobQueue did not contain %s %s", obdCommandType.getNameForValue(), "command, nothing to remove");
        }
    }

    private class ObdCommandJobHandler extends Handler {
        public ObdCommandJobHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Timber.d("Handling a new obdCommandJobMessage");
            super.handleMessage(msg);

            if (msg.what == MESSAGE_QUEUE_NOT_EMPTY) {
                Timber.d("ObdCommandJobHandler message queue is empty");
                return;
            }

            ObdCommandJob obdCommandJob = (ObdCommandJob) msg.obj;
            ObdCommand obdCommand = obdCommandJob.getCommand();
            try {
                Timber.d("Running new obdCommandJob with id %s", obdCommandJob.getId());
                obdCommandJob.setState(ObdCommandJob.State.RUNNING);
                if (obdCommand != null) {
                    if (isConnectionActive()) {
                        if (obdConnectionInputStream != null && obdConnectionOutputStream != null) {
                            if (isSocketConnected()) {
                                obdCommand.useImperialUnits(useImperialUnits);
                                runObdCommandWithTimeout(obdCommand);
                                obdCommandJob.setState(ObdCommandJob.State.FINISHED);
                            } else {
                                Timber.e("Socket is closed, cannot run obdCommandJob with id %s", obdCommandJob.getId());
                                obdCommandJob.setState(ObdCommandJob.State.EXECUTION_ERROR);
                                stopObdConnection(false);
                            }
                        } else {
                            Timber.e("InputStream and/or OutputStream  not set, cannot run obdCommandJob with id %s", obdCommandJob.getId());
                            obdCommandJob.setState(ObdCommandJob.State.EXECUTION_ERROR);
                            stopObdConnection(false);
                        }
                    } else {
                        Timber.e("Connection is not active, cannot run obdCommandJob with id %s", obdCommandJob.getId());
                        obdCommandJob.setState(ObdCommandJob.State.EXECUTION_ERROR);
                    }
                } else {
                    Timber.e("obdCommand in obdCommandJob was null, cannot run obdCommandJob with id %s", obdCommandJob.getId());
                    obdCommandJob.setState(ObdCommandJob.State.EXECUTION_ERROR);
                }
            } catch (UnableToConnectException utce) {
                Timber.e(utce, "Failed to connect to OBD2 target, stopping connection");
                obdCommandJob.setState(ObdCommandJob.State.UNABLE_TO_CONNECT);
                stopObdConnection(false);
            } catch (BusInitException bie) {
                Timber.e(bie, "Failed to init OBD2 bus, stopping connection");
                obdCommandJob.setState(ObdCommandJob.State.BUS_INIT_EXCEPTION);
                stopObdConnection(false);
            } catch (MisunderstoodCommandException mce) {
                Timber.e(mce, "obdCommand \"%s%s", obdCommandJob.getCommandType().getNameForValue(), "\" was misunderstood");
                obdCommandJob.setState(ObdCommandJob.State.MISUNDERSTOOD_COMMAND);
            } catch (NoDataException nde) {
                Timber.w(nde, "No data was sent in response to obdCommand \"%s%s %s", obdCommandJob.getCommandType().getNameForValue(), "\" in obdCommandJob with id", obdCommandJob.getId());
                obdCommandJob.setState(ObdCommandJob.State.NO_DATA);
            } catch (StoppedException se) {
                Timber.e(se, "obdCommand \"%s%s %s %s", obdCommandJob.getCommandType().getNameForValue(), "\" in obdCommandJob with id", obdCommandJob.getId(), "was stopped");
                obdCommandJob.setState(ObdCommandJob.State.STOPPED);
            } catch (UnsupportedCommandException uce) {
                Timber.w(uce, "obdCommand \"%s%s %s %s", obdCommandJob.getCommandType().getNameForValue(), "\" in obdCommandJob with id", obdCommandJob.getId(), "is not supported by OBD2 target");
                obdCommandJob.setState(ObdCommandJob.State.NOT_SUPPORTED);
            } catch (TimeoutException te) {
                Timber.e(te, "obdCommand \"%s%s %s %s", obdCommandJob.getCommandType().getNameForValue(), "\" in obdCommandJob with id", obdCommandJob.getId(), "timed out, stopping connection");
                obdCommandJob.setState(ObdCommandJob.State.TIMEOUT);
                stopObdConnection(false);
            } catch (Exception e) {
                Timber.e(e, "Failed to run obdCommand \"%s%s %s", obdCommandJob.getCommandType().getNameForValue(), "\" in obdCommandJob with id", obdCommandJob.getId());
                obdCommandJob.setState(ObdCommandJob.State.EXECUTION_ERROR);
            }
            if (obdCommand != null && obdCommandJob.getState() == ObdCommandJob.State.FINISHED) {
                Timber.d("Successfully ran obdCommand \"%s%s %s%s %s", obdCommandJob.getCommandType().getNameForValue(), "\" in obdCommandJob with id", obdCommandJob.getId(), ", result is:", obdCommand.getFormattedResult());
            }
            localBroadcastManager.sendBroadcast(ObdBroadcastIntent.getObdCommandJobResultIntent(obdCommandJob, currentVIN, sessionId));
        }
    }

    public class AbstractObdServiceBinder extends Binder {
        public AbstractObdService getService() {
            return AbstractObdService.this;
        }
    }

    private class StartObdConnectionAsyncTask extends AsyncTask<Void, Void, ObdConnectionState> {
        @Override
        protected ObdConnectionState doInBackground(Void... params) {
            Timber.d("doInBackground called");
            return _startObdConnection();
        }
        @Override
        protected void onPostExecute(ObdConnectionState obdConnectionState) {
            Timber.d("onPostExecute called");
            startObdConnectionAsyncTask = null;
            if (obdConnectionState == ObdConnectionState.CONNECTED) {
                showNotification(buildNotification(getString(R.string.obd_connected)));
                setIsConnectionActive(true);
                initializeObdInterface();
            } else {
                stopObdConnection(true);
            }
            sendObdConnectionStateBroadcast(obdConnectionState);
        }
        @Override
        protected void onCancelled() {
            Timber.d("I was cancelled, this should only happen after exiting the app while I was still connecting, better stop the connection and quickly disappear...");
            startObdConnectionAsyncTask = null;
            stopObdConnection(true);
        }
    }

    private class ObdCommandRunnable implements Runnable {
        private final ObdCommand obdCommand;
        private final Thread parentThread;
        private Exception exception;

        public ObdCommandRunnable(ObdCommand obdCommand, Thread parentThread) {
            this.obdCommand = obdCommand;
            this.parentThread = parentThread;
        }

        public void run() {
            try {
                obdCommand.run(obdConnectionInputStream, obdConnectionOutputStream);
            } catch (Exception e) {
                exception = e;
            }
            parentThread.interrupt();
        }

        public Exception getException() {
            return exception;
        }
    }
}