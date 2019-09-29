/*
 * This file is part of Airsonic.
 *
 *  Airsonic is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Airsonic is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Copyright 2015 (C) Sindre Mehus
 */

package org.airsonic.player.service.sonos;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.Phase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;

/**
 * Intercepts all SonosSoapFault exceptions and builds a SOAP Fault.
 *
 * @author Sindre Mehus
 * @version $Id$
 */
public class SonosFaultInterceptor extends AbstractSoapInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(SonosFaultInterceptor.class);

    /**
     * Constructor, setting the phase to Marshal.  This happens before the default Fault Interceptor
     */
    public SonosFaultInterceptor() {
        super(Phase.MARSHAL);
    }

    /*
     * Only handles instances of SonosSoapFault, all other exceptions fall through to the default Fault Interceptor
     */
    @Override
    public void handleMessage(SoapMessage message) throws Fault {
        Fault fault = (Fault) message.getContent(Exception.class);
        LOG.warn("Error: " + fault, fault);

        if (fault.getCause() instanceof SonosSoapFault) {
            SonosSoapFault cause = (SonosSoapFault) fault.getCause();
            fault.setFaultCode(new QName(cause.getFaultCode()));
            fault.setMessage(cause.getFaultCode());

            Document document = DOMUtils.createDocument();
            Element details = document.createElement("detail");
            fault.setDetail(details);

            details.appendChild(document.createElement("ExceptionInfo"));

            Element sonosError = document.createElement("SonosError");
            sonosError.setTextContent(String.valueOf(cause.getSonosError()));
            details.appendChild(sonosError);
        }
    }
}
