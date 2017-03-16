package com.github.zeroxfelix.obd2fun.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.androidplot.util.PixelUtils;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.PointLabeler;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;
import com.androidplot.xy.XYStepMode;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;

import com.github.zeroxfelix.obd2fun.R;
import timber.log.Timber;

public class WidgetListViewAdapter extends BaseAdapter implements Parcelable{
    private Context context;
    private final int layoutResourceId;
    private final ArrayList<DataWidget> data;

    public WidgetListViewAdapter(Context context, ArrayList<DataWidget> data) {
        this.layoutResourceId = R.layout.widget_template;
        this.context = context;
        this.data = data;
    }

    private WidgetListViewAdapter(Parcel in) {
        layoutResourceId = in.readInt();
        data = in.createTypedArrayList(DataWidget.CREATOR);
    }

    public static final Creator<WidgetListViewAdapter> CREATOR = new Creator<WidgetListViewAdapter>() {
        @Override
        public WidgetListViewAdapter createFromParcel(Parcel in) {
            return new WidgetListViewAdapter(in);
        }

        @Override
        public WidgetListViewAdapter[] newArray(int size) {
            return new WidgetListViewAdapter[size];
        }
    };

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View row = convertView;
        ViewHolder holder;

        final DataWidget dataWidget = data.get(position);

        if (row == null) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);

            holder = new ViewHolder();
            holder.txtTitle = (TextView) row.findViewById(R.id.item_title);
            holder.txtValue = (TextView) row.findViewById(R.id.item_value);
            holder.graphView = (XYPlot) row.findViewById(R.id.graph_view);
            holder.widgetChangeButton = (ImageButton) row.findViewById(R.id.widget_change);
            holder.widgetCloseButton = (ImageButton) row.findViewById(R.id.widget_close);

            Timber.d("Set Graph properties for '%s'", dataWidget.getTitle());
            //Graph Properties
            holder.lineAndPointFormatter = new LineAndPointFormatter();
            //Range Format
            holder.lineAndPointFormatter.setPointLabeler(new PointLabeler() {
                final DecimalFormat decimalFormat = new DecimalFormat("######.##");
                @Override
                public String getLabel(XYSeries series, int index) {
                    return decimalFormat.format(series.getY(index));
                }
            });
            //Disable Labels for every single Point
            holder.lineAndPointFormatter.setPointLabelFormatter(null);
            //Don't Fill area under graph
            holder.lineAndPointFormatter.getFillPaint().setColor(Color.TRANSPARENT);
            //Line Color and Size
            holder.lineAndPointFormatter.getLinePaint().setColor(ContextCompat.getColor(context,R.color.widget_graph_line));
            holder.lineAndPointFormatter.getLinePaint().setStrokeWidth(PixelUtils.dpToPix(2));
            //Dots Color and Size
            holder.lineAndPointFormatter.getVertexPaint().setColor(ContextCompat.getColor(context, R.color.widget_graph_dots));
            holder.lineAndPointFormatter.getVertexPaint().setStrokeWidth(PixelUtils.dpToPix(3));
            //Set Padding and Margin
            holder.graphView.getGraphWidget().setPadding(85,40,40,40);
            holder.graphView.getGraphWidget().setMargins(0,0,0,0);
            //Dont show X Axis Labels
            holder.graphView.getGraphWidget().getDomainOriginTickLabelPaint().setColor(ContextCompat.getColor(context, R.color.widget_graph_labels));
            holder.graphView.getGraphWidget().getDomainTickLabelPaint().setColor(ContextCompat.getColor(context, R.color.widget_graph_labels));
            //Y Axis Label Color
            holder.graphView.getGraphWidget().getRangeTickLabelPaint().setColor(ContextCompat.getColor(context, R.color.widget_graph_labels));
            holder.graphView.getGraphWidget().getRangeOriginTickLabelPaint().setColor(ContextCompat.getColor(context, R.color.widget_graph_labels));
            //Remove Legend and Title
            holder.graphView.getLayoutManager().remove(holder.graphView.getLegendWidget());
            holder.graphView.getLayoutManager().remove(holder.graphView.getTitleWidget());
            //Set Background(Outer) Color
            holder.graphView.getBackgroundPaint().setColor(Color.TRANSPARENT);
            //Set Border Color
            holder.graphView.getBorderPaint().setColor(Color.TRANSPARENT);
            //Set Background(behind Axis Labels)
            holder.graphView.getGraphWidget().getBackgroundPaint().setColor(Color.TRANSPARENT);
            //Set Background(behind graph)
            holder.graphView.getGraphWidget().getGridBackgroundPaint().setColor(Color.TRANSPARENT);
            //Set Grid Color
            holder.graphView.getGraphWidget().getDomainGridLinePaint().setColor(ContextCompat.getColor(context, R.color.widget_graph_grid));
            holder.graphView.getGraphWidget().getRangeGridLinePaint().setColor(ContextCompat.getColor(context, R.color.widget_graph_grid));

            holder.graphView.setDomainStep(XYStepMode.INCREMENT_BY_VAL, 5);
            holder.graphView.setDomainValueFormat(new DecimalFormat("0"));

            holder.graphView.setRangeLowerBoundary(0, BoundaryMode.FIXED);

            row.setTag(holder);
        } else {
            holder = (ViewHolder) row.getTag();
        }


        holder.txtValue.setText(dataWidget.getValue());
        holder.txtTitle.setText(dataWidget.getTitle());

        //Show Graph
        if (dataWidget.getIsGraph()){
            //holder.txtValue.setVisibility(View.GONE);
            holder.graphView.setVisibility(View.VISIBLE);
            holder.widgetChangeButton.setBackgroundResource(R.drawable.ic_action_list);

            //If there are Values
            if (dataWidget.getValuesOverTime().length > 0) {
                if (dataWidget.getUnit() != null) {
                    holder.txtTitle.setText(context.getString(R.string.read_data_fragment_graph_unit_header, dataWidget.getTitle(), dataWidget.getUnit()));
                }
                
                Timber.d("Rerender graph '%s'", dataWidget.getTitle());
                holder.graphView.clear();
                holder.graphData = new SimpleXYSeries(Arrays.asList(dataWidget.getValuesOverTime()), SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, "Values");
                holder.maxY = 0;
                for (int i = 0; i < holder.graphData.size(); i++) {
                    if (holder.graphData.getY(i).floatValue() > holder.maxY) {
                        holder.maxY = holder.graphData.getY(i).floatValue();
                    }
                }
                if(!dataWidget.isPercentage()) {
                    holder.graphView.setRangeUpperBoundary(holder.maxY * 1.1, BoundaryMode.FIXED);
                    holder.graphView.setRangeStep(XYStepMode.SUBDIVIDE, 6);
                }
                else{
                    holder.graphView.setRangeUpperBoundary(100, BoundaryMode.FIXED);
                    holder.graphView.setRangeStep(XYStepMode.INCREMENT_BY_VAL,20);
                }
                //Add Data and redraw
                holder.graphView.addSeries(holder.graphData, holder.lineAndPointFormatter);
                holder.graphView.redraw();
            }
        } else {
            //Show Value
            holder.txtValue.setVisibility(View.VISIBLE);
            holder.graphView.setVisibility(View.GONE);
            holder.widgetChangeButton.setBackgroundResource(R.drawable.ic_action_bargraph);
        }

        if (!dataWidget.getIsGraphable()) {
            Timber.d("DataWidget '%s' is not graphable. Removing Button.", dataWidget.getTitle());
            holder.widgetChangeButton.setVisibility(View.INVISIBLE);
        } else {
            holder.widgetChangeButton.setVisibility(View.VISIBLE);
        }

        holder.widgetCloseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                data.remove(position);
                dataWidget.unregisterReceiver();
                notifyDataSetChanged();
            }
        });

        holder.widgetChangeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Timber.d("Widget style changed");
                dataWidget.setIsGraph(!dataWidget.getIsGraph());
                notifyDataSetChanged();
            }
        });

        return row;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(layoutResourceId);
        dest.writeTypedList(data);
    }

    static class ViewHolder {
        TextView txtTitle;
        TextView txtValue;
        XYPlot graphView;
        ImageButton widgetChangeButton;
        ImageButton widgetCloseButton;
        LineAndPointFormatter lineAndPointFormatter;
        XYSeries graphData;
        float maxY;
    }
}
