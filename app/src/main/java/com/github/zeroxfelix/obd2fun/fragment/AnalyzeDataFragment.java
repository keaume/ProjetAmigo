package com.github.zeroxfelix.obd2fun.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.PointF;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.androidplot.util.PixelUtils;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.PointLabelFormatter;
import com.androidplot.xy.PointLabeler;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;
import com.androidplot.xy.XYStepMode;

import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.github.zeroxfelix.obd2fun.R;
import com.github.zeroxfelix.obd2fun.export.CsvExport;
import com.github.zeroxfelix.obd2fun.interfaces.SetActionBarTitleInterface;
import com.github.zeroxfelix.obd2fun.interfaces.SetSelectedDrawerMenuItemInterface;
import com.github.zeroxfelix.obd2fun.obd.ObdCommandType;
import com.github.zeroxfelix.obd2fun.sql.Obd2FunDataSource;
import com.github.zeroxfelix.obd2fun.sql.ObdData;
import com.github.zeroxfelix.obd2fun.ui.Session;
import com.github.zeroxfelix.obd2fun.ui.TextViewDatePickerDialog;
import timber.log.Timber;

public class AnalyzeDataFragment extends Fragment implements View.OnTouchListener {

    private static final int INTERPOLATED_POINT_AMOUNT = 500;

    private XYPlot analyzeDataGraph;
    private SimpleXYSeries series1 = null;
    private long leftBoundary;
    private long rightBoundary;
    private LineAndPointFormatter lineAndPointFormatter;

    private long minXYx;
    private long maxXYx;

    private List<Session> sessionList;
    private Obd2FunDataSource dataSource;
    private ArrayList<ObdData> rawObdData;
    private List<ObdCommandType> commandTypesInSession = new ArrayList<>();
    private Spinner commandTypeSpinner;
    private Spinner sessionSpinner;
    private TextView loadedDataTextview;
    private String chosenSessionId;
    private String chosenSession;
    private ObdCommandType chosenObdCommandType;
    private String loadedDataText;
    private boolean noSessions = false;
    private boolean interpolateOn;
    private CheckBox interpolateCheckbox;
    private int selectedSessionItemId;
    private int selectedCommandItemId;
    private String chosenExportSessionId;
    private String chosenExportSession;
    private TextViewDatePickerDialog startDateDialog;
    private TextViewDatePickerDialog endDateDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Timber.d("onCreate called");
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        dataSource = new Obd2FunDataSource(getActivity().getApplicationContext());

        if (savedInstanceState != null) {
            Timber.d("Restoring variables previous states");
            rawObdData = savedInstanceState.getParcelableArrayList("rawObdData");
            loadedDataText = savedInstanceState.getString("loadedDataText");
            minXYx = savedInstanceState.getLong("minXYx");
            maxXYx = savedInstanceState.getLong("maxXYx");
            selectedSessionItemId = savedInstanceState.getInt("selectedSessionItemId");
            selectedCommandItemId = savedInstanceState.getInt("selectedCommandItemId");
            interpolateOn = savedInstanceState.getBoolean("interpolateOn");
        } else {
            Timber.d("Beginning with a fresh instance");
            loadedDataText = getString(R.string.analyze_data_fragment_no_data_loaded);
            selectedCommandItemId = -1;
            selectedSessionItemId = -1;
            interpolateOn = false;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Timber.d("onCreateView called");
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.analyze_data_fragment, container, false);
        final Activity activity = getActivity();

        createSessionObjects();

        LinearLayout loadDataButton = (LinearLayout) view.findViewById(R.id.load_data_button);
        loadedDataTextview = (TextView) view.findViewById(R.id.loaded_data_textview);
        analyzeDataGraph = (XYPlot) view.findViewById(R.id.analyze_data_graph);

        if (!noSessions) {
            Timber.d("Clickable");
            loadDataButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    @SuppressLint("InflateParams") final View loadDataView = inflater.inflate(R.layout.load_data_popup, null);
                    interpolateCheckbox = (CheckBox) loadDataView.findViewById(R.id.interpolate_checkbox);
                    new AlertDialog.Builder(activity)
                            .setTitle(R.string.analyze_data_fragment_load_data_popup_headline)
                            .setView(loadDataView)
                            .setPositiveButton(R.string.analyze_data_fragment_load_data_popup_load, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Timber.d("Loading data");
                                    interpolateOn = interpolateCheckbox.isChecked();
                                    rawObdData = dataSource.getObdDataForObdCommandTypeInSession(chosenObdCommandType, chosenSessionId);
                                    loadedDataText = chosenSession;
                                    setLoadedDataText();
                                    refreshPlot();
                                }
                            })
                            .setNegativeButton(R.string.analyze_data_fragment_load_data_popup_abort, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            })
                            .show();

                    sessionSpinner = (Spinner) loadDataView.findViewById(R.id.session_spinner);
                    ArrayAdapter<Session> sessionSpinnerArrayAdapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item, sessionList);
                    sessionSpinner.setAdapter(sessionSpinnerArrayAdapter);
                    if(selectedSessionItemId >= 0 && sessionList.size() > selectedSessionItemId){
                        sessionSpinner.setSelection(selectedSessionItemId);
                    }

                    commandTypeSpinner = (Spinner) loadDataView.findViewById(R.id.commandtype_spinner);
                    ArrayAdapter<ObdCommandType> commandTypeSpinnerArrayAdapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item, commandTypesInSession);
                    commandTypeSpinner.setAdapter(commandTypeSpinnerArrayAdapter);

                    sessionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            chosenSessionId = sessionList.get(position).getSessionId();
                            chosenSession = sessionList.get(position).toString();
                            selectedSessionItemId = position;
                            Timber.d("Chosen Session: %s", chosenSessionId);
                            commandTypesInSession = sessionList.get(position).getObdCommandTypes();
                            ArrayAdapter<ObdCommandType> commandTypeSpinnerArrayAdapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item, commandTypesInSession);
                            commandTypeSpinner.setAdapter(commandTypeSpinnerArrayAdapter);
                            if(selectedCommandItemId >= 0 && commandTypesInSession.size() > selectedCommandItemId){
                                commandTypeSpinner.setSelection(selectedCommandItemId);
                            }
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {

                        }
                    });
                    commandTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            chosenObdCommandType = commandTypesInSession.get(position);
                            selectedCommandItemId = position;
                            Timber.d("Chosen CommandType: %s", chosenObdCommandType);
                        }
                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {
                        }
                    });
                }
            });
        }

        //Plot Setup
        lineAndPointFormatter = new LineAndPointFormatter();
        lineAndPointFormatter.setPointLabeler(new PointLabeler() {
            final DecimalFormat decimalFormat = new DecimalFormat("######.##");
            @Override
            public String getLabel(XYSeries series, int index) {
                return decimalFormat.format(series.getY(index));
            }
        });

        lineAndPointFormatter.setPointLabelFormatter(new PointLabelFormatter(Color.TRANSPARENT));
        lineAndPointFormatter.getFillPaint().setColor(ContextCompat.getColor(activity,R.color.analyze_data_graph_fill));
        lineAndPointFormatter.getLinePaint().setStrokeWidth(PixelUtils.dpToPix(1));

        //Dots Color and Size
        lineAndPointFormatter.getVertexPaint().setColor(ContextCompat.getColor(activity, R.color.analyze_data_graph_dots));
        lineAndPointFormatter.getVertexPaint().setStrokeWidth(PixelUtils.dpToPix(3));
        //Line color and Size
        lineAndPointFormatter.getLinePaint().setColor(ContextCompat.getColor(activity,R.color.analyze_data_graph_line));
        lineAndPointFormatter.getLinePaint().setStrokeWidth(PixelUtils.dpToPix(1));

        analyzeDataGraph.getGraphWidget().setTicksPerRangeLabel(1);
        analyzeDataGraph.getGraphWidget().setTicksPerDomainLabel(2);
        analyzeDataGraph.setRangeStep(XYStepMode.SUBDIVIDE, 9);
        analyzeDataGraph.getLayoutManager().remove(analyzeDataGraph.getLegendWidget());
        analyzeDataGraph.getGraphWidget().setPadding(70,50,0,50);
        analyzeDataGraph.setDomainValueFormat(new Format() {
            private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss", Locale.GERMANY);
            @Override
            public StringBuffer format(Object object, @NonNull StringBuffer buffer, @NonNull FieldPosition field) {
                long timestamp = ((Number) object).longValue();
                Date date = new Date(timestamp);
                return dateFormat.format(date, buffer, field);
            }
            @Override
            public Object parseObject(String string, @NonNull ParsePosition position) {
                return null;
            }
        });

        analyzeDataGraph.setPlotPaddingTop(30);
        analyzeDataGraph.getTitleWidget().getLabelPaint().setTextSize(PixelUtils.spToPix(14));
        analyzeDataGraph.getTitleWidget().getLabelPaint().setColor(ContextCompat.getColor(activity, R.color.analyze_data_graph_title));

        //Set Background(Outer) Color
        analyzeDataGraph.getBackgroundPaint().setColor(Color.TRANSPARENT);
        //Set Border Color
        analyzeDataGraph.getBorderPaint().setColor(Color.TRANSPARENT);
        //Set Background(behind Axis Labels)
        analyzeDataGraph.getGraphWidget().getBackgroundPaint().setColor(Color.TRANSPARENT);
        //Set Background(behind graph)
        analyzeDataGraph.getGraphWidget().getGridBackgroundPaint().setColor(Color.TRANSPARENT);
        //Set Grid Color
        analyzeDataGraph.getGraphWidget().getDomainGridLinePaint().setColor(ContextCompat.getColor(activity, R.color.analyze_data_graph_grid));
        analyzeDataGraph.getGraphWidget().getRangeGridLinePaint().setColor(ContextCompat.getColor(activity, R.color.analyze_data_graph_grid));
        //Y Axis Label Color
        analyzeDataGraph.getGraphWidget().getRangeTickLabelPaint().setColor(ContextCompat.getColor(activity, R.color.analyze_data_graph_labels));
        analyzeDataGraph.getGraphWidget().getRangeOriginTickLabelPaint().setColor(ContextCompat.getColor(activity, R.color.analyze_data_graph_labels));
        //X Axis Label Color
        analyzeDataGraph.getGraphWidget().getDomainTickLabelPaint().setColor(ContextCompat.getColor(activity, R.color.analyze_data_graph_labels));
        analyzeDataGraph.getGraphWidget().getDomainOriginTickLabelPaint().setColor(ContextCompat.getColor(activity, R.color.analyze_data_graph_labels));



        SwitchCompat dataDescriptionSwitch = (SwitchCompat) view.findViewById(R.id.data_description_switch);
        dataDescriptionSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    lineAndPointFormatter.setPointLabelFormatter(new PointLabelFormatter(ContextCompat.getColor(activity, R.color.analyze_data_graph_point_labels)));
                }
                else{
                    lineAndPointFormatter.setPointLabelFormatter(null);
                }
                analyzeDataGraph.redraw();
            }
        });

        refreshPlotWithSameBoundaries();
        setLoadedDataText();

        return view;
    }

    private void refreshPlot(){
        Timber.d("RefreshPlot called");
        if (rawObdData != null) {
            Timber.d("Clear old data");
            analyzeDataGraph.clear();
            analyzeDataGraph.removeSeries(series1);
            series1 = null;
            series1 = new SimpleXYSeries("");

            Number timestamps[] = getTimestampsAsNumber(rawObdData);
            Number values[] = getValuesAsNumber(rawObdData);

            if(interpolateOn && timestamps.length > INTERPOLATED_POINT_AMOUNT){
                int interpolationValue = timestamps.length/INTERPOLATED_POINT_AMOUNT;
                if(interpolationValue < 2) interpolationValue = 2;
                Timber.d("Fill series with interpolated data (%s Points)", timestamps.length/interpolationValue);
                for (int i = 0; i < timestamps.length; i=i+interpolationValue) {
                    series1.addLast(timestamps[i], values[i]);
                }
            }
            else {
                Timber.d("Fill series with full data (%s Points)", timestamps.length);
                for (int i = 0; i < timestamps.length; i++) {
                    series1.addLast(timestamps[i], values[i]);
                }
            }

            analyzeDataGraph.addSeries(series1, lineAndPointFormatter);

            //Hack because getCalculatedMaxY doesn't work
            float maxXYy = 0;
            for (int i = 0; i < series1.size(); i++) {
                if (series1.getY(i).floatValue() > maxXYy) {
                    maxXYy = series1.getY(i).floatValue();
                }
            }
            minXYx = series1.getX(0).longValue();
            maxXYx = series1.getX(series1.size() - 1).longValue();

            leftBoundary = minXYx;
            rightBoundary = maxXYx;
            float upperBoundary;
            if (rawObdData.get(0).getObdCommandType().getIsPercentage()) {
                upperBoundary = 100;
            } else {
                //Set upperBoundary 10 percent higher than the maximum
                upperBoundary = maxXYy * 1.1f;
            }

            Timber.d("Setting Boundaries");
            analyzeDataGraph.setRangeBoundaries(0, upperBoundary, BoundaryMode.FIXED);

            analyzeDataGraph.setDomainBoundaries(leftBoundary, rightBoundary, BoundaryMode.FIXED);
            analyzeDataGraph.setTitle(getString(R.string.analyze_data_fragment_graph_title, rawObdData.get(0).getObdCommandType().getNameForValue(), rawObdData.get(0).getResultUnit()));
            analyzeDataGraph.setOnTouchListener(this);

            Timber.d("Redraw Plot");
            analyzeDataGraph.redraw();
        }
    }

    private void refreshPlotWithSameBoundaries(){
        long oldminXYx = minXYx;
        long oldmaxXYx = maxXYx;
        refreshPlot();
        analyzeDataGraph.setDomainBoundaries(oldminXYx, oldmaxXYx,BoundaryMode.FIXED);
        minXYx = oldminXYx;
        maxXYx = oldmaxXYx;
    }

    // Definition of the touch states
    private static final int NONE = 0;
    private static final int ONE_FINGER_DRAG = 1;
    private static final int TWO_FINGERS_DRAG = 2;
    private int mode = NONE;

    private PointF firstFinger;
    private float distBetweenFingers;

    @Override
    public boolean onTouch(View arg0, MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: // Start gesture
                firstFinger = new PointF(event.getX(), event.getY());
                mode = ONE_FINGER_DRAG;
                break;
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_UP:
                mode = NONE;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                distBetweenFingers = spacing(event);
                if (distBetweenFingers > 5f) {
                    mode = TWO_FINGERS_DRAG;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mode == ONE_FINGER_DRAG) {
                    PointF oldFirstFinger = firstFinger;
                    firstFinger = new PointF(event.getX(), event.getY());
                    scroll(oldFirstFinger.x - firstFinger.x);
                    analyzeDataGraph.setDomainBoundaries(minXYx, maxXYx,
                            BoundaryMode.FIXED);
                    analyzeDataGraph.redraw();

                } else if (mode == TWO_FINGERS_DRAG) {
                    float oldDist = distBetweenFingers;
                    distBetweenFingers = spacing(event);
                    zoom(oldDist / distBetweenFingers);
                    analyzeDataGraph.setDomainBoundaries(minXYx, maxXYx,
                            BoundaryMode.FIXED);
                    analyzeDataGraph.redraw();
                }
                break;
        }
        return true;
    }

    private void zoom(float scale) {
        long domainSpan = maxXYx - minXYx;
        long oldMax = maxXYx;
        long oldMin = minXYx;
        long domainMidPoint = maxXYx - (domainSpan / 2);
        long offset = (long) (domainSpan * scale / 2);
        minXYx = domainMidPoint - offset;
        maxXYx = domainMidPoint + offset;
        long newSpan = maxXYx - minXYx;
        if (newSpan < 5) {
            minXYx = oldMin;
            maxXYx = oldMax;
        }

        long zoomRatio = 2;
        if (minXYx < leftBoundary) {
            minXYx = leftBoundary;
            maxXYx = leftBoundary + domainSpan * zoomRatio;
            if (maxXYx > series1.getX(series1.size() - 1).longValue())
                maxXYx = rightBoundary;
        }
        if (maxXYx > series1.getX(series1.size() - 1).longValue()) {
            maxXYx = rightBoundary;
            minXYx = rightBoundary - domainSpan * zoomRatio;
            if (minXYx < leftBoundary) minXYx = leftBoundary;
        }

    }

    private void scroll(float pan) {
        float domainSpan = maxXYx - minXYx;
        float step = domainSpan / analyzeDataGraph.getWidth();
        float offset = pan * step;
        minXYx = minXYx + (long) offset;
        maxXYx = maxXYx + (long) offset;

        if (minXYx < leftBoundary) {
            minXYx = leftBoundary;
            maxXYx = leftBoundary + (long) domainSpan;
        }
        if (maxXYx > series1.getX(series1.size() - 1).longValue()) {
            maxXYx = rightBoundary;
            minXYx = rightBoundary - (long) domainSpan;
        }

    }

    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    @Override
    public void onResume() {
        Timber.d("onResume called");
        super.onResume();
        // Set title
        ((SetActionBarTitleInterface)getActivity()).setActionBarTitle(getString(R.string.main_activity_drawer_menu_analyze_data));
        // Set selected menuItem in navigation drawer
        ((SetSelectedDrawerMenuItemInterface)getActivity()).setSelectedDrawerMenuItem(R.id.main_activity_drawer_menu_analyze_data);
    }

    private void createSessionObjects(){
        Timber.d("Creating Session objects");
        List<Session> sessionList = new ArrayList<>();
        for (String sessionId : dataSource.getAllSessions()) {
            Session session = new Session(sessionId, dataSource);
            if(session.getObdCommandTypes().size() > 0) {
                sessionList.add(session);
            }
        }
        if (sessionList.size() == 0) {
            loadedDataText = getString(R.string.analyze_data_fragment_no_sessions_recorded);
            Timber.d("No Sessions");
            noSessions = true;
        } else {
            if (loadedDataText.equals(getString(R.string.analyze_data_fragment_no_sessions_recorded))) {
                loadedDataText = getString(R.string.analyze_data_fragment_no_data_loaded);
            }
            Timber.d("Sessions");
            noSessions = false;
        }
        this.sessionList = sessionList;
    }

    private Number[] getValuesAsNumber(List<ObdData> rawObdData){
        Number[] output = new Number[rawObdData.size()];
        for (int i = 0; i < rawObdData.size(); i++) {
            output[i] = Float.valueOf(rawObdData.get(i).getCalculatedResult());
        }
        return output;
    }

    private Number[] getTimestampsAsNumber(List<ObdData> rawObdData){
        Number[] output = new Number[rawObdData.size()];
        for (int i = 0; i < rawObdData.size(); i++) {
            output[i] = rawObdData.get(i).getRecordingDate().getTime();
        }
        return output;
    }

    private void setLoadedDataText(){
        loadedDataTextview.setText(loadedDataText);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Timber.d("onCreateOptionsMenu called");
        inflater.inflate(R.menu.analyze_data_fragment_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu (Menu menu) {
        Timber.d("onPrepareOptionsMenu called");
        super.onPrepareOptionsMenu(menu);
        if(noSessions) {
            for (int i = 0; i < menu.size(); i++) {
                MenuItem item = menu.getItem(i);
                item.setEnabled(false);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Timber.d("onOptionsItemSelected called");
        switch (item.getItemId()) {
            case R.id.export_between_dates:
                exportBetweenDates();
                break;
            case R.id.export_session:
                exportSession();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void exportSession(){
        final Activity activity = getActivity();
        final Obd2FunDataSource dataSource = new Obd2FunDataSource(activity);
        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        @SuppressLint("InflateParams") final View exportSessionView = inflater.inflate(R.layout.export_session_popup, null);
        new AlertDialog.Builder(activity)
                .setTitle(R.string.analyze_data_fragment_export_session_popup_header)
                .setView(exportSessionView)
                .setPositiveButton(R.string.analyze_data_fragment_export_session_popup_export, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Timber.d("Exporting data from %s", chosenExportSession);
                        Toast.makeText(activity, getString(R.string.analyze_data_fragment_exporting_data_toast) , Toast.LENGTH_SHORT).show();
                        ArrayList<ObdData> sessionObdData = dataSource.getObdDataForSession(chosenExportSessionId);
                        CsvExport csvExport = new CsvExport(sessionObdData, chosenExportSessionId);
                        csvExport.writeFileToExternalStorage();
                    }
                })
                .setNegativeButton(R.string.analyze_data_fragment_export_session_popup_abort, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();

        Spinner exportSessionSpinner = (Spinner) exportSessionView.findViewById(R.id.session_spinner);
        ArrayAdapter<Session> sessionSpinnerArrayAdapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item, sessionList);
        exportSessionSpinner.setAdapter(sessionSpinnerArrayAdapter);

        exportSessionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                chosenExportSessionId = sessionList.get(position).getSessionId();
                chosenExportSession = sessionList.get(position).toString();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void exportBetweenDates(){
        final Activity activity = getActivity();
        final Obd2FunDataSource dataSource = new Obd2FunDataSource(activity);
        final LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        @SuppressLint("InflateParams") final View exportBetweenDatesView = inflater.inflate(R.layout.export_between_dates_popup, null);
        new AlertDialog.Builder(activity)
                .setTitle(R.string.analyze_data_fragment_export_between_dates_popup_header)
                .setView(exportBetweenDatesView)
                .setPositiveButton(R.string.analyze_data_fragment_export_between_dates_popup_export, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Date startDate = startDateDialog.getSetDate();
                        Date endDate = endDateDialog.getSetDate();

                        Timber.d("Picked Start Date: %s", startDate);
                        Timber.d("Picked End Date: %s", endDate);

                        if(endDate.before(startDate) || startDate.equals(endDate)) {
                            Timber.d("End date is before or equal to Start date");
                            Toast.makeText(activity, getString(R.string.analyze_data_fragment_export_between_dates_popup_end_before_start_date) , Toast.LENGTH_SHORT).show();
                        }
                        else{
                            Timber.d("Exporting data between dates");
                            Toast.makeText(activity, getString(R.string.analyze_data_fragment_exporting_data_toast) , Toast.LENGTH_SHORT).show();
                            ArrayList<ObdData> betweenDatesObdData = dataSource.getObdDataBetweenDates(startDate, endDate);
                            CsvExport csvExport = new CsvExport(betweenDatesObdData, startDate, endDate);
                            csvExport.writeFileToExternalStorage();
                        }
                    }
                })
                .setNegativeButton(R.string.analyze_data_fragment_export_between_dates_popup_abort, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();

        TextView startDateSpinner = (TextView) exportBetweenDatesView.findViewById(R.id.start_date_spinner);
        startDateDialog = new TextViewDatePickerDialog(activity, startDateSpinner);
        TextView endDateSpinner = (TextView) exportBetweenDatesView.findViewById(R.id.end_date_spinner);
        endDateDialog = new TextViewDatePickerDialog(activity, endDateSpinner);

    }



    @Override
    public void onSaveInstanceState(Bundle outState) {
        Timber.d("onSaveInstanceState called");
        outState.putParcelableArrayList("rawObdData", rawObdData);
        outState.putString("loadedDataText", loadedDataText);
        outState.putLong("minXYx", minXYx);
        outState.putLong("maxXYx", maxXYx);
        outState.putInt("selectedSessionItemId", selectedSessionItemId);
        outState.putInt("selectedCommandItemId", selectedCommandItemId);
        super.onSaveInstanceState(outState);
    }
}