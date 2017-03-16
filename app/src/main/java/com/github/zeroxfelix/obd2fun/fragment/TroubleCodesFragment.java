package com.github.zeroxfelix.obd2fun.fragment;

import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import com.github.zeroxfelix.obd2fun.R;
import com.github.zeroxfelix.obd2fun.interfaces.GetCurrentVinInterface;
import com.github.zeroxfelix.obd2fun.interfaces.GetIsObdConnectionActiveInterface;
import com.github.zeroxfelix.obd2fun.interfaces.SetActionBarTitleInterface;
import com.github.zeroxfelix.obd2fun.interfaces.SetSelectedDrawerMenuItemInterface;
import com.github.zeroxfelix.obd2fun.obd.ObdBroadcastIntent;
import com.github.zeroxfelix.obd2fun.obd.ObdCommandJob;
import com.github.zeroxfelix.obd2fun.obd.ObdCommandJobResult;
import com.github.zeroxfelix.obd2fun.obd.ObdCommandType;
import com.github.zeroxfelix.obd2fun.obd.ObdConnectionState;
import com.github.zeroxfelix.obd2fun.sql.Obd2FunDataSource;
import com.github.zeroxfelix.obd2fun.sql.TroubleCodesResult;
import timber.log.Timber;

public class TroubleCodesFragment extends Fragment {

    private LocalBroadcastManager localBroadcastManager;
    private ArrayAdapter<String> troubleCodesArrayAdapter;
    private Obd2FunDataSource obd2FunDataSource;
    private ViewSwitcher viewSwitcher;
    private FloatingActionMenu floatingActionMenu;
    private ArrayList<FloatingActionButton> floatingActionButtons;
    private TextView textView;
    private TextView troubleCodesListHeader;
    private ProgressDialog progressDialog;

    private boolean showTextView;
    private String textViewText;
    private String troubleCodesListHeaderText;
    private TroubleCodesResult.Type currentTroubleCodesType;
    private ArrayList<String> troubleCodesWithDescriptionList;
    private boolean troubleCodesAlreadySaved;

    private final BroadcastReceiver obdCommandJobResultReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Timber.d("Received new obdCommandJobResult");
            ObdCommandJobResult obdCommandJobResult = intent.getParcelableExtra("obdCommandJobResult");
            ObdCommandType obdCommandType = obdCommandJobResult.getObdCommandType();
            if (obdCommandType == ObdCommandType.TROUBLE_CODES || obdCommandType == ObdCommandType.PENDING_TROUBLE_CODES || obdCommandType == ObdCommandType.PERMANENT_TROUBLE_CODES) {
                handleTroubleCodesObdCommandJobResult(obdCommandJobResult);
            } else if (obdCommandType == ObdCommandType.RESET_TROUBLE_CODES) {
                handleResetTroubleCodesObdCommandJobResult(obdCommandJobResult);
            }
            if (progressDialog != null) {
                progressDialog.dismiss();
            }
        }
    };

    private final BroadcastReceiver obdConnectionStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ObdConnectionState obdConnectionState = (ObdConnectionState) intent.getSerializableExtra("obdConnectionState");
            switch (obdConnectionState) {
                case CONNECTED:
                    setEnabledFloatingActionButtons(true);
                    break;
                default:
                    setEnabledFloatingActionButtons(false);
                    if (progressDialog != null) {
                        progressDialog.dismiss();
                    }
            }
        }
    };

    private final View.OnClickListener fabOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.trouble_codes_fragment_fab_read_trouble_codes:
                    localBroadcastManager.sendBroadcast(ObdBroadcastIntent.getRegisterOneShotObdCommandJobIntent(ObdCommandType.TROUBLE_CODES));
                    showProgressDialog(getString(R.string.trouble_codes_fragment_progress_dialog_reading_codes));
                    break;
                case R.id.trouble_codes_fragment_fab_read_pending_trouble_codes:
                    localBroadcastManager.sendBroadcast(ObdBroadcastIntent.getRegisterOneShotObdCommandJobIntent(ObdCommandType.PENDING_TROUBLE_CODES));
                    showProgressDialog(getString(R.string.trouble_codes_fragment_progress_dialog_reading_pending_codes));
                    break;
                case R.id.trouble_codes_fragment_fab_read_permanent_trouble_codes:
                    localBroadcastManager.sendBroadcast(ObdBroadcastIntent.getRegisterOneShotObdCommandJobIntent(ObdCommandType.PERMANENT_TROUBLE_CODES));
                    showProgressDialog(getString(R.string.trouble_codes_fragment_progress_dialog_reading_permanent_codes));
                    break;
                case R.id.trouble_codes_fragment_fab_clear_trouble_codes:
                    showReallyClearTroubleCodesAlertDialog();
                    break;
            }
            floatingActionMenu.close(false);
        }
    };

    private final View.OnTouchListener touchOutsideFamListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (floatingActionMenu.isOpened()) {
                    Rect outRect = new Rect();
                    floatingActionMenu.getGlobalVisibleRect(outRect);
                    if(!outRect.contains((int) event.getRawX(), (int) event.getRawY())) {
                        floatingActionMenu.close(true);
                        return true;
                    }
                }
            }
            return false;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Timber.d("onCreate called");
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        localBroadcastManager = LocalBroadcastManager.getInstance(getActivity().getApplicationContext());
        obd2FunDataSource = new Obd2FunDataSource(getActivity().getApplicationContext());
        troubleCodesArrayAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1);
        troubleCodesListHeader = new TextView(getActivity());
        floatingActionButtons = new ArrayList<>();

        Timber.d("Registering receiver for ObdConnectionState broadcasts");
        localBroadcastManager.registerReceiver(obdConnectionStateReceiver, new IntentFilter(ObdBroadcastIntent.OBD_CONNECTION_STATE));

        Timber.d("Registering receiver for obdCommandJobResult broadcasts");
        localBroadcastManager.registerReceiver(obdCommandJobResultReceiver, new IntentFilter(ObdCommandType.TROUBLE_CODES.getNameForValue()));
        localBroadcastManager.registerReceiver(obdCommandJobResultReceiver, new IntentFilter(ObdCommandType.PENDING_TROUBLE_CODES.getNameForValue()));
        localBroadcastManager.registerReceiver(obdCommandJobResultReceiver, new IntentFilter(ObdCommandType.PERMANENT_TROUBLE_CODES.getNameForValue()));
        localBroadcastManager.registerReceiver(obdCommandJobResultReceiver, new IntentFilter(ObdCommandType.RESET_TROUBLE_CODES.getNameForValue()));

        if (savedInstanceState != null) {
            Timber.d("Restoring variables previous states");
            showTextView = savedInstanceState.getBoolean("showTextView");
            textViewText = savedInstanceState.getString("textViewText");
            troubleCodesListHeaderText = savedInstanceState.getString("troubleCodesListHeaderText");
            currentTroubleCodesType = (TroubleCodesResult.Type) savedInstanceState.getSerializable("currentTroubleCodesType");
            troubleCodesWithDescriptionList = savedInstanceState.getStringArrayList("troubleCodesWithDescriptionList");
            troubleCodesAlreadySaved = savedInstanceState.getBoolean("troubleCodesAlreadySaved");
        } else {
            Timber.d("Beginning with a fresh instance");
            showTextView = true;
            textViewText = getString(R.string.trouble_codes_fragment_text_view_no_codes_loaded);
            troubleCodesListHeaderText = "";
            currentTroubleCodesType = TroubleCodesResult.Type.NORMAL;
            troubleCodesWithDescriptionList = new ArrayList<>();
            troubleCodesAlreadySaved = true;
        }
    }

    @Override
    public void onDestroy() {
        Timber.d("onDestroy called");
        super.onDestroy();
        Timber.d("Unregistering receiver for obdCommandJobResult broadcasts");
        localBroadcastManager.unregisterReceiver(obdCommandJobResultReceiver);
        Timber.d("Unregistering receiver for ObdConnectionState broadcasts");
        localBroadcastManager.unregisterReceiver(obdConnectionStateReceiver);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Timber.d("onCreateView called");
        View view = inflater.inflate(R.layout.trouble_codes_fragment, container, false);
        viewSwitcher = (ViewSwitcher) view.findViewById(R.id.trouble_codes_fragment_view_switcher);
        floatingActionMenu = (FloatingActionMenu) view.findViewById(R.id.trouble_codes_fragment_fab_menu);

        textView = (TextView) view.findViewById(R.id.trouble_codes_fragment_text_view);
        textView.setOnTouchListener(touchOutsideFamListener);
        textView.setText(textViewText);

        troubleCodesListHeader.setText(troubleCodesListHeaderText);
        troubleCodesArrayAdapter.clear();
        troubleCodesArrayAdapter.addAll(troubleCodesWithDescriptionList);
        final ListView troubleCodesListView = (ListView) view.findViewById(R.id.trouble_codes_fragment_list_view);
        troubleCodesListView.setOnTouchListener(touchOutsideFamListener);
        troubleCodesListView.addHeaderView(troubleCodesListHeader, null, false);
        troubleCodesListView.setHeaderDividersEnabled(false);
        troubleCodesListView.setAdapter(troubleCodesArrayAdapter);
        //Bug Workaround: http://stackoverflow.com/a/27789593
        troubleCodesListView.setFooterDividersEnabled(false);
        troubleCodesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String troubleCodeText = ((TextView) view.findViewById(android.R.id.text1)).getText().toString();
                showWebSearchAlertDialog(new StringTokenizer(troubleCodeText, getString(R.string.trouble_codes_fragment_trouble_code_separator)).nextToken());
            }
        });

        floatingActionButtons.clear();
        floatingActionButtons.add((FloatingActionButton)view.findViewById(R.id.trouble_codes_fragment_fab_read_trouble_codes));
        floatingActionButtons.add((FloatingActionButton)view.findViewById(R.id.trouble_codes_fragment_fab_read_pending_trouble_codes));
        floatingActionButtons.add((FloatingActionButton)view.findViewById(R.id.trouble_codes_fragment_fab_read_permanent_trouble_codes));
        floatingActionButtons.add((FloatingActionButton)view.findViewById(R.id.trouble_codes_fragment_fab_clear_trouble_codes));
        for (FloatingActionButton floatingActionButton : floatingActionButtons) {
            floatingActionButton.setOnClickListener(fabOnClickListener);
        }
        setEnabledFloatingActionButtons(((GetIsObdConnectionActiveInterface)getActivity()).isObdConnectionActive());

        showTroubleCodesFragmentTextView(showTextView, true);

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Timber.d("onCreateOptionsMenu called");
        inflater.inflate(R.menu.trouble_codes_fragment_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu (Menu menu) {
        Timber.d("onPrepareOptionsMenu called");
        super.onPrepareOptionsMenu(menu);
        for(int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            if (item.getItemId() == R.id.save_trouble_codes) {
                item.setEnabled(!troubleCodesAlreadySaved && !troubleCodesWithDescriptionList.isEmpty());
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Timber.d("onOptionsItemSelected called");
        switch (item.getItemId()) {
            case R.id.save_trouble_codes:
                saveCurrentTroubleCodesList();
                return true;
            case R.id.manage_saved_trouble_codes:
                showManageSavedTroubleCodesDialog();
                return true;
            case R.id.delete_all_saved_trouble_codes:
                showDeleteAllSavedTroubleCodesDialog();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        Timber.d("onResume called");
        super.onResume();
        // Set title
        ((SetActionBarTitleInterface)getActivity()).setActionBarTitle(getString(R.string.main_activity_drawer_menu_trouble_codes));
        // Set selected menuItem in navigation drawer
        ((SetSelectedDrawerMenuItemInterface)getActivity()).setSelectedDrawerMenuItem(R.id.main_activity_drawer_menu_trouble_codes);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Timber.d("onSaveInstanceState called");
        super.onSaveInstanceState(outState);
        outState.putBoolean("showTextView", showTextView);
        outState.putString("textViewText", textViewText);
        outState.putString("troubleCodesListHeaderText", troubleCodesListHeaderText);
        outState.putSerializable("currentTroubleCodesType", currentTroubleCodesType);
        outState.putStringArrayList("troubleCodesWithDescriptionList", troubleCodesWithDescriptionList);
    }

    private void setEnabledFloatingActionButtons(boolean enabled) {
        for (FloatingActionButton floatingActionButton : floatingActionButtons) {
            floatingActionButton.setEnabled(enabled);
        }
    }

    private Map<String, Pair<String, String>> getTroubleCodesMap() {
        HashMap<String, Pair<String, String>> troubleCodesMap = new HashMap<>();
        String[] troubleCodeKeys = getResources().getStringArray(R.array.trouble_code_keys);
        String[] troubleCodeMakers = getResources().getStringArray(R.array.trouble_code_makers);
        String[] troubleCodeValues = getResources().getStringArray(R.array.trouble_code_values);

        for (int i = 0; i < troubleCodeKeys.length; i++) {
            troubleCodesMap.put(troubleCodeKeys[i], new Pair<>(troubleCodeMakers[i], troubleCodeValues[i]));
        }

        return Collections.unmodifiableMap(troubleCodesMap);
    }

    private ArrayList<String> getTroubleCodesWithDescriptionList(List<String> troubleCodesList) {
        ArrayList<String> troubleCodesWithDescriptionList = new ArrayList<>();
        Map<String, Pair<String, String>> troubleCodesMap = getTroubleCodesMap();

        Timber.d("Parsing trouble codes");
        for (String troubleCode : troubleCodesList) {
            if (troubleCodesMap.containsKey(troubleCode)) {
                troubleCodesWithDescriptionList.add(troubleCode + getString(R.string.trouble_codes_fragment_trouble_code_separator) + troubleCodesMap.get(troubleCode).first + getString(R.string.trouble_codes_fragment_manufacturerer_description_separator) + troubleCodesMap.get(troubleCode).second);
            } else {
                troubleCodesWithDescriptionList.add(troubleCode + getString(R.string.trouble_codes_fragment_trouble_code_separator) + getString(R.string.trouble_codes_fragment_list_view_unknown_trouble_code));
            }
        }

        return troubleCodesWithDescriptionList;
    }

    private void handleTroubleCodesObdCommandJobResult(ObdCommandJobResult obdCommandJobResult) {
        troubleCodesArrayAdapter.clear();
        if (obdCommandJobResult.getState() == ObdCommandJob.State.NO_DATA || obdCommandJobResult.getFormattedResult().isEmpty()) {
            Timber.d("Did receive no data in response to obdCommand");
            showNoCodesReceivedMessage(obdCommandJobResult);
        } else if (obdCommandJobResult.getState() == ObdCommandJob.State.FINISHED) {
            Timber.d("Filling troubleCodesArrayAdapter with new trouble codes");
            setTroubleCodesListHeader(obdCommandJobResult);
            troubleCodesWithDescriptionList = getTroubleCodesWithDescriptionList(Arrays.asList(obdCommandJobResult.getFormattedResult().split("\n")));
            troubleCodesArrayAdapter.addAll(troubleCodesWithDescriptionList);
            troubleCodesAlreadySaved = false;
            showTroubleCodesFragmentTextView(false, false);
        } else {
            Timber.e("Error while executing obdCommand");
            showMessage(getString(R.string.trouble_codes_fragment_text_view_reading_codes_error));
        }
    }

    private void handleResetTroubleCodesObdCommandJobResult(ObdCommandJobResult obdCommandJobResult) {
        if (obdCommandJobResult.getState() == ObdCommandJob.State.FINISHED) {
            Timber.d("Trouble codes successfully cleared");
            showMessage(getString(R.string.trouble_codes_fragment_text_view_reset_successful));
        } else {
            Timber.e("Error while clearing trouble codes");
            showMessage(getString(R.string.trouble_codes_fragment_text_view_reset_error));
        }
        troubleCodesAlreadySaved = true;
    }

    private void setTroubleCodesListHeader(ObdCommandJobResult obdCommandJobResult) {
        Timber.d("Setting troubleCodesListHeaderText via obdCommandJobResult");
        String type = "";
        switch (obdCommandJobResult.getObdCommandType()) {
            case TROUBLE_CODES:
                type = getString(R.string.trouble_codes_fragment_list_view_title_trouble_codes);
                currentTroubleCodesType = TroubleCodesResult.Type.NORMAL;
                break;
            case PENDING_TROUBLE_CODES:
                type = getString(R.string.trouble_codes_fragment_list_view_title_pending_trouble_codes);
                currentTroubleCodesType = TroubleCodesResult.Type.PENDING;
                break;
            case PERMANENT_TROUBLE_CODES:
                type = getString(R.string.trouble_codes_fragment_list_view_title_permanent_trouble_codes);
                currentTroubleCodesType = TroubleCodesResult.Type.PERMANENT;
                break;
        }
        String currentVin = ((GetCurrentVinInterface)getActivity()).getCurrentVin();
        String nameForVin = obd2FunDataSource.getNameForVin(currentVin);
        if (nameForVin == null) {
            nameForVin = getString(R.string.unknown_vin_name);
        }
        troubleCodesListHeaderText = String.format(getString(R.string.trouble_codes_fragment_list_view_title_template), type, nameForVin, currentVin, getString(R.string.trouble_codes_fragment_list_view_title_now));
        troubleCodesListHeader.setText(troubleCodesListHeaderText);
    }

    private void setTroubleCodesListHeader(TroubleCodesResult troubleCodesResult) {
        Timber.d("Setting troubleCodesListHeaderText via troubleCodesResult");
        currentTroubleCodesType = troubleCodesResult.getType();
        String type = "";
        switch (currentTroubleCodesType) {
            case NORMAL:
                type = getString(R.string.trouble_codes_fragment_list_view_title_trouble_codes);
                break;
            case PENDING:
                type = getString(R.string.trouble_codes_fragment_list_view_title_pending_trouble_codes);
                break;
            case PERMANENT:
                type = getString(R.string.trouble_codes_fragment_list_view_title_permanent_trouble_codes);
                break;
        }
        String nameForVin = obd2FunDataSource.getNameForVin(troubleCodesResult.getVin());
        if (nameForVin == null) {
            nameForVin = getString(R.string.unknown_vin_name);
        }
        troubleCodesListHeaderText = String.format(getString(R.string.trouble_codes_fragment_list_view_title_template), type, nameForVin, troubleCodesResult.getVin(), troubleCodesResult.getFormattedDate());
        troubleCodesListHeader.setText(troubleCodesListHeaderText);
    }

    private void showTroubleCodesFragmentTextView(boolean showTextView, boolean noAnimation) {
        Timber.d("showTroubleCodesFragmentTextView called, no animation was set to: %s", String.valueOf(noAnimation));
        this.showTextView = showTextView;
        Animation inAnimation = viewSwitcher.getInAnimation();
        Animation outAnimation = viewSwitcher.getOutAnimation();
        if (noAnimation) {
            viewSwitcher.setInAnimation(null);
            viewSwitcher.setOutAnimation(null);
        }
        if (showTextView) {
            viewSwitcher.setDisplayedChild(0);
        } else {
            viewSwitcher.setDisplayedChild(1);
        }
        if (noAnimation) {
            viewSwitcher.setInAnimation(inAnimation);
            viewSwitcher.setOutAnimation(outAnimation);
        }
    }

    private void showMessage(String message) {
        Timber.d("Showing a new message");
        textViewText = message;
        textView.setText(textViewText);
        showTroubleCodesFragmentTextView(true, false);
        troubleCodesArrayAdapter.clear();
    }

    private void showNoCodesReceivedMessage(ObdCommandJobResult obdCommandJobResult) {
        switch (obdCommandJobResult.getObdCommandType()) {
            case TROUBLE_CODES:
                showMessage(getString(R.string.trouble_codes_fragment_text_view_no_codes_received));
                break;
            case PENDING_TROUBLE_CODES:
                showMessage(getString(R.string.trouble_codes_fragment_text_view_no_pending_codes_received));
                break;
            case PERMANENT_TROUBLE_CODES:
                showMessage(getString(R.string.trouble_codes_fragment_text_view_no_permanent_codes_received));
                break;
            default:
                showMessage("");
        }
    }

    private void showProgressDialog(String message) {
        Timber.d("Showing progress dialog");
        progressDialog = ProgressDialog.show(getActivity(), null, message, true, false);
    }

    private void showReallyClearTroubleCodesAlertDialog() {
        Timber.d("Showing really clear trouble codes dialog");
        new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.alert_dialog_clear_trouble_codes_title))
                .setMessage(getString(R.string.alert_dialog_clear_trouble_codes_message))
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        localBroadcastManager.sendBroadcast(ObdBroadcastIntent.getRegisterOneShotObdCommandJobIntent(ObdCommandType.RESET_TROUBLE_CODES));
                        showProgressDialog(getString(R.string.trouble_codes_fragment_progress_dialog_resetting_codes));
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .show();
    }

    private void showWebSearchAlertDialog(final String troubleCode) {
        Timber.d("Showing web search dialog");
        new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.alert_dialog_web_search_title))
                .setMessage(String.format(getString(R.string.alert_dialog_web_search_message), troubleCode))
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        PackageManager packageManager = getActivity().getApplicationContext().getPackageManager();
                        Intent webSearchIntent = new Intent(Intent.ACTION_WEB_SEARCH);
                        webSearchIntent.putExtra(SearchManager.QUERY, getString(R.string.trouble_codes_fragment_web_search_prefix) + troubleCode);
                        if (packageManager.queryIntentActivities(webSearchIntent, 0).size() > 0) {
                            Timber.d("Using web search intent to do a web search");
                            startActivity(Intent.createChooser(webSearchIntent, getString(R.string.intent_chooser_web_search)));
                        } else {
                            Timber.d("No activities for web search intent available, using view intent to do a web search");
                            try {
                                Uri googleSearchUri = Uri.parse("https://www.google.de/search?q=" + URLEncoder.encode(getString(R.string.trouble_codes_fragment_web_search_prefix) + troubleCode, "UTF-8"));
                                Intent viewIntent = new Intent(Intent.ACTION_VIEW, googleSearchUri);
                                startActivity(Intent.createChooser(viewIntent, getString(R.string.intent_chooser_web_search)));
                            } catch (Exception e) {
                                Timber.d("Failed to do a web search via view intent");
                            }
                        }
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })
                .setIconAttribute(android.R.attr.dialogIcon)
                .show();
    }

    private void saveCurrentTroubleCodesList() {
        Timber.d("Saving current trouble codes to database");
        ArrayList<String> troubleCodesList = new ArrayList<>();
        for (String troubleCodeWithDescription : troubleCodesWithDescriptionList) {
            troubleCodesList.add(new StringTokenizer(troubleCodeWithDescription, getString(R.string.trouble_codes_fragment_trouble_code_separator)).nextToken());
        }
        obd2FunDataSource.saveTroubleCodesList(((GetCurrentVinInterface)getActivity()).getCurrentVin(), currentTroubleCodesType, troubleCodesList);
        troubleCodesAlreadySaved = true;
    }

    private int getInt(int resId) {
        FragmentActivity fragmentActivity = getActivity();
        if (fragmentActivity != null) {
            int[] attribute = {resId};
            TypedArray typedArray = fragmentActivity.getApplicationContext().getTheme().obtainStyledAttributes(attribute);
            int intValue = typedArray.getDimensionPixelSize(0, -1);
            typedArray.recycle();
            return intValue;
        } else {
            return -1;
        }
    }

    private int dpToPx(int dpValue) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        return (int) Math.ceil(dpValue * displayMetrics.density);
    }

    private void showManageSavedTroubleCodesDialog() {
        Timber.d("Showing manage saved trouble codes dialog");
        final TroubleCodesResultAdapter troubleCodesResultAdapter = new TroubleCodesResultAdapter(getActivity(), obd2FunDataSource.getAllAvailableTroubleCodesResults());
        if (troubleCodesResultAdapter.isEmpty()) {
            new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.alert_dialog_no_saved_trouble_codes_title)
                    .setMessage(R.string.alert_dialog_no_saved_trouble_codes_message)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // do nothing
                        }
                    })
                    .show();
        } else {
            AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.alert_dialog_choose_saved_trouble_codes_title)
                    .setSingleChoiceItems(troubleCodesResultAdapter, 1, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // do nothing
                        }
                    })
                    .setNeutralButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // do nothing
                        }
                    })
                    .setNegativeButton(R.string.alert_dialog_choose_saved_trouble_codes_delete, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            final ListView listView = ((AlertDialog) dialog).getListView();
                            new AlertDialog.Builder(getActivity())
                                    .setTitle(getString(R.string.alert_dialog_choose_saved_trouble_codes_delete))
                                    .setMessage(getString(R.string.alert_dialog_delete_trouble_codes_entry_message))
                                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            // do nothing
                                        }
                                    })
                                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Timber.d("Deleting a trouble codes entry");
                                            troubleCodesResultAdapter.deleteEntry(listView.getCheckedItemPosition());
                                        }
                                    })
                                    .setIconAttribute(android.R.attr.alertDialogIcon)
                                    .show();
                        }
                    })
                    .setPositiveButton(R.string.alert_dialog_choose_saved_trouble_codes_load, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Timber.d("Loading a trouble codes entry");
                            ListView listView = ((AlertDialog) dialog).getListView();
                            troubleCodesResultAdapter.loadEntry(listView.getCheckedItemPosition());
                        }
                    })
                    .create();
            TextView alertDialogListViewHeader = new TextView(getActivity());
            int paddingLeft = getInt(android.R.attr.listPreferredItemPaddingLeft);
            if (paddingLeft < 15) {
                paddingLeft = 25;
            }
            alertDialogListViewHeader.setPadding(dpToPx(paddingLeft), 0, 0, 0);
            alertDialogListViewHeader.setText(getString(R.string.alert_dialog_choose_saved_trouble_codes_list_title));
            ListView alertDialogListView = alertDialog.getListView();
            alertDialogListView.addHeaderView(alertDialogListViewHeader, null, false);
            alertDialogListView.setHeaderDividersEnabled(false);
            alertDialogListView.setFooterDividersEnabled(false);
            alertDialog.show();
        }
    }

    private void showDeleteAllSavedTroubleCodesDialog() {
        Timber.d("Showing delete all saved trouble codes dialog");
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.alert_dialog_delete_all_saved_trouble_codes_title)
                .setMessage(R.string.alert_dialog_delete_all_saved_trouble_codes_message)
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Timber.d("Deleting all saved trouble codes");
                        obd2FunDataSource.deleteAllTroubleCodesResults();
                    }
                })
                .show();
    }

    private class TroubleCodesResultAdapter extends ArrayAdapter<TroubleCodesResult> {
        public TroubleCodesResultAdapter(Context context, List<TroubleCodesResult> objects) {
            super(context, android.R.layout.simple_list_item_single_choice, objects);
        }

        public void deleteEntry(int which) {
            Timber.d("Deleting a trouble codes result");
            //Offset -1 for header
            TroubleCodesResult troubleCodesResult = getItem(which - 1);
            obd2FunDataSource.deleteTroubleCodesResultForDate(troubleCodesResult.getDate());
            remove(troubleCodesResult);
        }

        public void loadEntry(int which) {
            Timber.d("Loading a trouble codes result");
            //Offset -1 for header
            TroubleCodesResult troubleCodesResult = getItem(which - 1);
            setTroubleCodesListHeader(troubleCodesResult);
            troubleCodesWithDescriptionList = getTroubleCodesWithDescriptionList(troubleCodesResult.getTroubleCodesList());
            troubleCodesArrayAdapter.clear();
            troubleCodesArrayAdapter.addAll(troubleCodesWithDescriptionList);
            troubleCodesAlreadySaved = true;
            showTroubleCodesFragmentTextView(false, false);
        }
    }
}
