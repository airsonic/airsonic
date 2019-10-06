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
package org.airsonic.player.service;

import org.airsonic.player.service.upnp.ApacheUpnpServiceConfiguration;
import org.airsonic.player.service.upnp.CustomContentDirectory;
import org.airsonic.player.service.upnp.MSMediaReceiverRegistrarService;
import org.airsonic.player.util.FileUtil;
import org.fourthline.cling.UpnpService;
import org.fourthline.cling.UpnpServiceImpl;
import org.fourthline.cling.binding.annotations.AnnotationLocalServiceBinder;
import org.fourthline.cling.model.DefaultServiceManager;
import org.fourthline.cling.model.meta.*;
import org.fourthline.cling.model.types.DLNADoc;
import org.fourthline.cling.model.types.DeviceType;
import org.fourthline.cling.model.types.UDADeviceType;
import org.fourthline.cling.model.types.UDN;
import org.fourthline.cling.support.connectionmanager.ConnectionManagerService;
import org.fourthline.cling.support.model.ProtocolInfos;
import org.fourthline.cling.support.model.dlna.DLNAProfiles;
import org.fourthline.cling.support.model.dlna.DLNAProtocolInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Sindre Mehus
 * @version $Id$
 */
@Service
public class UPnPService {

    private static final Logger LOG = LoggerFactory.getLogger(UPnPService.class);

    @Autowired
    private SettingsService settingsService;
    private UpnpService upnpService;
    @Autowired
    @Qualifier("dispatchingContentDirectory")
    private CustomContentDirectory dispatchingContentDirectory;
    private AtomicReference<Boolean> running = new AtomicReference<>(false);

    @PostConstruct
    public void init() {
        if (settingsService.isDlnaEnabled() || settingsService.isSonosEnabled()) {
            ensureServiceStarted();
            if (settingsService.isDlnaEnabled()) {
                // Start DLNA media server?
                setMediaServerEnabled(true);
            }
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> ensureServiceStopped()));
    }

    public void ensureServiceStarted() {
        running.getAndUpdate(bo -> {
            if (!bo) {
                startService();
                return true;
            } else {
                return true;
            }
        });
    }

    public void ensureServiceStopped() {
        running.getAndUpdate(bo -> {
            if (bo) {
                if (upnpService != null) {
                    LOG.info("Disabling UPnP/DLNA media server");
                    upnpService.getRegistry().removeAllLocalDevices();
                    System.err.println("Shutting down UPnP service...");
                    upnpService.shutdown();
                    System.err.println("Shutting down UPnP service - Done!");
                }
                return false;
            } else {
                return false;
            }
        });

    }

    private void startService() {
        try {
            LOG.info("Starting UPnP service...");
            createService();
            LOG.info("Starting UPnP service - Done!");
        } catch (Throwable x) {
            LOG.error("Failed to start UPnP service: " + x, x);
        }
    }

    private synchronized void createService() {
        upnpService = new UpnpServiceImpl(new ApacheUpnpServiceConfiguration());

        // Asynch search for other devices (most importantly UPnP-enabled routers for port-mapping)
        upnpService.getControlPoint().search();

    }

    public void setMediaServerEnabled(boolean enabled) {
        if (enabled) {
            ensureServiceStarted();
            try {
                upnpService.getRegistry().addDevice(createMediaServerDevice());
                LOG.info("Enabling UPnP/DLNA media server");
            } catch (Exception x) {
                LOG.error("Failed to start UPnP/DLNA media server: " + x, x);
            }
        } else {
            ensureServiceStopped();
        }
    }

    private LocalDevice createMediaServerDevice() throws Exception {

        String serverName = settingsService.getDlnaServerName();
        DeviceIdentity identity = new DeviceIdentity(UDN.uniqueSystemIdentifier(serverName));
        DeviceType type = new UDADeviceType("MediaServer", 1);

        // TODO: DLNACaps

        DeviceDetails details = new DeviceDetails(serverName, new ManufacturerDetails(serverName),
                new ModelDetails(serverName),
                new DLNADoc[]{new DLNADoc("DMS", DLNADoc.Version.V1_5)}, null);

        InputStream in = getClass().getResourceAsStream("logo-512.png");
        Icon icon = new Icon("image/png", 512, 512, 32, "logo-512", in);
        FileUtil.closeQuietly(in);

        LocalService<CustomContentDirectory> contentDirectoryservice = new AnnotationLocalServiceBinder().read(CustomContentDirectory.class);
        contentDirectoryservice.setManager(new DefaultServiceManager<CustomContentDirectory>(contentDirectoryservice) {

            @Override
            protected CustomContentDirectory createServiceInstance() {
                return dispatchingContentDirectory;
            }
        });

        final ProtocolInfos protocols = new ProtocolInfos();
        for (DLNAProfiles dlnaProfile : DLNAProfiles.values()) {
            if (dlnaProfile == DLNAProfiles.NONE) {
                continue;
            }
            try {
                protocols.add(new DLNAProtocolInfo(dlnaProfile));
            } catch (Exception e) {
                // Silently ignored.
            }
        }

        LocalService<ConnectionManagerService> connetionManagerService = new AnnotationLocalServiceBinder().read(ConnectionManagerService.class);
        connetionManagerService.setManager(new DefaultServiceManager<ConnectionManagerService>(connetionManagerService) {
            @Override
            protected ConnectionManagerService createServiceInstance() {
                return new ConnectionManagerService(protocols, null);
            }
        });

        // For compatibility with Microsoft
        LocalService<MSMediaReceiverRegistrarService> receiverService = new AnnotationLocalServiceBinder().read(MSMediaReceiverRegistrarService.class);
        receiverService.setManager(new DefaultServiceManager<>(receiverService, MSMediaReceiverRegistrarService.class));

        return new LocalDevice(identity, type, details, new Icon[]{icon}, new LocalService[]{contentDirectoryservice, connetionManagerService, receiverService});
    }

    public List<String> getSonosControllerHosts() {
        ensureServiceStarted();
        List<String> result = new ArrayList<String>();
        for (Device device : upnpService.getRegistry().getDevices(new DeviceType("schemas-upnp-org", "ZonePlayer"))) {
            if (device instanceof RemoteDevice) {
                URL descriptorURL = ((RemoteDevice) device).getIdentity().getDescriptorURL();
                if (descriptorURL != null) {
                    result.add(descriptorURL.getHost());
                }
            }
        }
        return result;
    }

    public UpnpService getUpnpService() {
        return upnpService;
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setCustomContentDirectory(CustomContentDirectory customContentDirectory) {
        this.dispatchingContentDirectory = customContentDirectory;
    }
}
