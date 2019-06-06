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

import org.airsonic.player.domain.Player;
import org.airsonic.player.domain.TransferStatus;
import org.airsonic.player.service.StatusService;
import org.airsonic.player.util.FileUtil;
import org.airsonic.player.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContextUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.*;

/**
 * Controller for the status page.
 *
 * @author Sindre Mehus
 */
@Controller
@RequestMapping("/status")
public class StatusController {

    @Autowired
    private StatusService statusService;

    @GetMapping
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Map<String, Object> map = new HashMap<>();

        List<TransferStatus> streamStatuses = statusService.getAllStreamStatuses();
        List<TransferStatus> downloadStatuses = statusService.getAllDownloadStatuses();
        List<TransferStatus> uploadStatuses = statusService.getAllUploadStatuses();

        Locale locale = RequestContextUtils.getLocale(request);
        List<TransferStatusHolder> transferStatuses = new ArrayList<>();

        for (int i = 0; i < streamStatuses.size(); i++) {
            long minutesAgo = streamStatuses.get(i).getMillisSinceLastUpdate() / 1000L / 60L;
            if (minutesAgo < 60L) {
                transferStatuses.add(new TransferStatusHolder(streamStatuses.get(i), true, false, false, i, locale));
            }
        }
        for (int i = 0; i < downloadStatuses.size(); i++) {
            transferStatuses.add(new TransferStatusHolder(downloadStatuses.get(i), false, true, false, i, locale));
        }
        for (int i = 0; i < uploadStatuses.size(); i++) {
            transferStatuses.add(new TransferStatusHolder(uploadStatuses.get(i), false, false, true, i, locale));
        }

        map.put("transferStatuses", transferStatuses);
        map.put("chartWidth", StatusChartController.IMAGE_WIDTH);
        map.put("chartHeight", StatusChartController.IMAGE_HEIGHT);

        return new ModelAndView("status","model",map);
    }


    public static class TransferStatusHolder {
        private TransferStatus transferStatus;
        private boolean isStream;
        private boolean isDownload;
        private boolean isUpload;
        private int index;
        private Locale locale;

        TransferStatusHolder(TransferStatus transferStatus, boolean isStream, boolean isDownload, boolean isUpload,
                             int index, Locale locale) {
            this.transferStatus = transferStatus;
            this.isStream = isStream;
            this.isDownload = isDownload;
            this.isUpload = isUpload;
            this.index = index;
            this.locale = locale;
        }

        public boolean isStream() {
            return isStream;
        }

        public boolean isDownload() {
            return isDownload;
        }

        public boolean isUpload() {
            return isUpload;
        }

        public int getIndex() {
            return index;
        }

        public Player getPlayer() {
            return transferStatus.getPlayer();
        }

        public String getPlayerType() {
            Player player = transferStatus.getPlayer();
            return player == null ? null : player.getType();
        }

        public String getUsername() {
            Player player = transferStatus.getPlayer();
            return player == null ? null : player.getUsername();
        }

        public String getPath() {
            return FileUtil.getShortPath(transferStatus.getFile());
        }

        public String getBytes() {
            return StringUtil.formatBytes(transferStatus.getBytesTransfered(), locale);
        }
    }

}
