/*
 This file is part of Airsonic.

 Airsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Airsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2016 (C) Airsonic Authors
 Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
 */
package org.airsonic.player.controller;

import org.airsonic.player.domain.TransferStatus;
import org.airsonic.player.service.StatusService;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.Range;
import org.jfree.data.time.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.awt.*;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Controller for generating a chart showing bitrate vs time.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/statusChart")
public class StatusChartController extends AbstractChartController {

    @Autowired
    private StatusService statusService;

    public static final int IMAGE_WIDTH = 350;
    public static final int IMAGE_HEIGHT = 150;

    @GetMapping
    public synchronized ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String type = request.getParameter("type");
        int index = ServletRequestUtils.getIntParameter(request, "index", 0);

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

        TimeSeries series = new TimeSeries("Kbps");
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
        renderer.setSeriesStroke(0, new BasicStroke(2.0f));

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

        ChartUtils.writeChartAsPNG(response.getOutputStream(), chart, IMAGE_WIDTH, IMAGE_HEIGHT);

        return null;
    }

}
