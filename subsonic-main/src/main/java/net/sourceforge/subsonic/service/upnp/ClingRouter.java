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
package net.sourceforge.subsonic.service.upnp;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

import org.fourthline.cling.UpnpService;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.support.igd.PortMappingListener;
import org.fourthline.cling.support.igd.callback.PortMappingAdd;
import org.fourthline.cling.support.igd.callback.PortMappingDelete;
import org.fourthline.cling.support.model.PortMapping;

import net.sourceforge.subsonic.service.UPnPService;

/**
* @author Sindre Mehus
* @version $Id$
*/
public class ClingRouter implements Router {

    private final Service connectionService;
    private final UpnpService upnpService;

    public static ClingRouter findRouter(UPnPService upnpService) {
        final Service connectionService = findConnectionService(upnpService.getUpnpService());
        if (connectionService == null) {
            return null;
        }
        return new ClingRouter(connectionService, upnpService.getUpnpService());
    }

    /**
     * Returns the UPnP service used for port mapping.
     */
    private static Service findConnectionService(UpnpService upnpService) {

        class ConnectionServiceDiscoverer extends PortMappingListener {
            ConnectionServiceDiscoverer() {
                super(new PortMapping[0]);
            }

            @Override
            public Service discoverConnectionService(Device device) {
                return super.discoverConnectionService(device);
            }
        }

        ConnectionServiceDiscoverer discoverer = new ConnectionServiceDiscoverer();
        Collection<Device> devices = upnpService.getRegistry().getDevices();
        for (Device device : devices) {
            Service service = discoverer.discoverConnectionService(device);
            if (service != null) {
                return service;
            }
        }
        return null;
    }

    public ClingRouter(Service connectionService, UpnpService upnpService) {
        this.connectionService = connectionService;
        this.upnpService = upnpService;
    }

    public void addPortMapping(int externalPort, int internalPort, int leaseDuration) throws Exception {
        addPortMappingImpl(connectionService, internalPort);
    }

    public void deletePortMapping(int externalPort, int internalPort) throws Exception {
        deletePortMappingImpl(connectionService, internalPort);
    }

    private void addPortMappingImpl(Service connectionService, int port) throws Exception {
        final Semaphore gotReply = new Semaphore(0);
        final AtomicReference<String> error = new AtomicReference<String>();
        upnpService.getControlPoint().execute(
                new PortMappingAdd(connectionService, createPortMapping(port)) {

                    @Override
                    public void success(ActionInvocation invocation) {
                        gotReply.release();
                    }

                    @Override
                    public void failure(ActionInvocation invocation, UpnpResponse response, String defaultMsg) {
                        error.set(String.valueOf(response) + ": " + defaultMsg);
                        gotReply.release();
                    }
                }
        );
        gotReply.acquire();
        if (error.get() != null) {
            throw new Exception(error.get());
        }
    }

    private void deletePortMappingImpl(Service connectionService, int port) throws Exception {
        final Semaphore gotReply = new Semaphore(0);
        upnpService.getControlPoint().execute(
                new PortMappingDelete(connectionService, createPortMapping(port)) {

                    @Override
                    public void success(ActionInvocation invocation) {
                        gotReply.release();
                    }

                    @Override
                    public void failure(ActionInvocation invocation, UpnpResponse response, String defaultMsg) {
                        gotReply.release();
                    }
                }
        );
        gotReply.acquire();
    }

    private PortMapping createPortMapping(int port) throws UnknownHostException {
        String localIp = InetAddress.getLocalHost().getHostAddress();
        return new PortMapping(port, localIp, PortMapping.Protocol.TCP, "Subsonic");
    }
}
