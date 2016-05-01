/*
 This file is part of Subsonic.

 Subsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Subsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Subsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2009 (C) Sindre Mehus
 */
package net.sourceforge.subsonic.controller;

import net.sourceforge.subsonic.domain.*;
import net.sourceforge.subsonic.service.*;
import org.jfree.chart.*;
import org.jfree.chart.axis.*;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.xy.*;
import org.jfree.data.*;
import org.jfree.data.time.*;
import org.springframework.web.servlet.*;

import javax.servlet.http.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Controller for generating a chart showing bitrate vs time.
 *
 * @author Sindre Mehus
 */
public class StatusChartController extends AbstractChartController {

    private StatusService statusService;

    public static final int IMAGE_WIDTH = 350;
    public static final int IMAGE_HEIGHT = 150;

    public synchronized ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String type = request.getParameter("type");
        int index = Integer.parseInt(request.getParameter("index"));

        List<TransferStatus> statuses = Collections.emptyList();
        if ("stream".equals(type)) {
            statuses = statusService.getAllStreamStatuses();
        } else if ("download".equals(type)) {
            statuses = statusService.getAllDownloadStatuses();
        } else if ("upload".equals(type)) {
            statuses = statusService.getAllUploadStatuses();
        }

        if (index < 0 || index >= statuses.size()) {
            return null;
        }
        TransferStatus status = statuses.get(index);

        TimeSeries series = new TimeSeries("Kbps", Millisecond.class);
        TransferStatus.SampleHistory history = status.getHistory();
        long to = System.currentTimeMillis();
        long from = to - status.getHistoryLengthMillis();
        Range range = new DateRange(from, to);

        if (!history.isEmpty()) {

            TransferStatus.Sample previous = history.get(0);

            for (int i = 1; i < history.size(); i++) {
                TransferStatus.Sample sample = history.get(i);

                long elapsedTimeMilis = sample.getTimestamp() - previous.getTimestamp();
                long bytesStreamed = Math.max(0L, sample.getBytesTransfered() - previous.getBytesTransfered());

                double kbps = (8.0 * bytesStreamed / 1024.0) / (elapsedTimeMilis / 1000.0);
                series.addOrUpdate(new Millisecond(new Date(sample.getTimestamp())), kbps);

                previous = sample;
            }
        }

        // Compute moving average.
        series = MovingAverage.createMovingAverage(series, "Kbps", 20000, 5000);

        // Find min and max values.
        double min = 100;
        double max = 250;
        for (Object obj : series.getItems()) {
            TimeSeriesDataItem item = (TimeSeriesDataItem) obj;
            double value = item.getValue().doubleValue();
            if (item.getPeriod().getFirstMillisecond() > from) {
                min = Math.min(min, value);
                max = Math.max(max, value);
            }
        }

        // Add 10% to max value.
        max *= 1.1D;

        // Subtract 10% from min value.
        min *= 0.9D;

        TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(series);
        JFreeChart chart = ChartFactory.createTimeSeriesChart(null, null, null, dataset, false, false, false);
        XYPlot plot = (XYPlot) chart.getPlot();

        plot.setRangeAxisLocation(AxisLocation.BOTTOM_OR_RIGHT);
        Paint background = new GradientPaint(0, 0, Color.lightGray, 0, IMAGE_HEIGHT, Color.white);
        plot.setBackgroundPaint(background);

        XYItemRenderer renderer = plot.getRendererForDataset(dataset);
        renderer.setSeriesPaint(0, Color.blue.darker());
        renderer.setSeriesStroke(0, new BasicStroke(2f));

        // Set theme-specific colors.
        Color bgColor = getBackground(request);
        Color fgColor = getForeground(request);

        chart.setBackgroundPaint(bgColor);

        ValueAxis domainAxis = plot.getDomainAxis();
        domainAxis.setRange(range);
        domainAxis.setTickLabelPaint(fgColor);
        domainAxis.setTickMarkPaint(fgColor);
        domainAxis.setAxisLinePaint(fgColor);

        ValueAxis rangeAxis = plot.getRangeAxis();
        rangeAxis.setRange(new Range(min, max));
        rangeAxis.setTickLabelPaint(fgColor);
        rangeAxis.setTickMarkPaint(fgColor);
        rangeAxis.setAxisLinePaint(fgColor);

        ChartUtilities.writeChartAsPNG(response.getOutputStream(), chart, IMAGE_WIDTH, IMAGE_HEIGHT);

        return null;
    }

    public void setStatusService(StatusService statusService) {
        this.statusService = statusService;
    }
}
