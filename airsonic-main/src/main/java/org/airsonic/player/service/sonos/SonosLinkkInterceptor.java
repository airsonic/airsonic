package org.airsonic.player.service.sonos;


import com.google.common.collect.Sets;
import com.sonos.services._1.Credentials;
import com.sun.org.apache.xerces.internal.dom.ElementNSImpl;
import org.airsonic.player.service.SecurityService;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.headers.Header;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.Phase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;

import java.util.List;
import java.util.Set;

/**
 * <p>Interceptor for the Soap sonos, is validate access for all methods (Soap Action) except methods for exchange
 * on set link between airsonic and sonos controller.</p>
 *
 * <p>The validation can be adapt for different link method from sonos.</p>
 */
@Component
public class SonosLinkkInterceptor extends AbstractSoapInterceptor {
    private static final Logger LOG = LoggerFactory.getLogger(SonosLinkkInterceptor.class);
    private static Set<String> openMethod = Sets.newHashSet("getAppLink", "getDeviceAuthToken");

    public SonosLinkkInterceptor() {
        super(Phase.PRE_INVOKE);
    }

    @Autowired
    private SecurityService securityService;

    private JAXBContext jaxbContext;

    @PostConstruct
    public void postConstruct() throws JAXBException {
        jaxbContext = JAXBContext.newInstance("com.sonos.services._1");
    }

    @Override
    public void handleMessage(SoapMessage message) throws Fault {
        try {
            String action = getAction(message);

            if (action != null && openMethod.contains(action)) {
                LOG.debug("Soap message not process : " + message.toString());

            } else if( action != null) {
                String sonosLinkToken = getToken(message);
                if (sonosLinkToken != null) {
                    securityService.setSonosUser(sonosLinkToken);
                }
            } else {
                LOG.debug("Soap message not process : " + message.toString());
                throw new SonosSoapFault.LoginUnauthorized();
            }
        } catch (JAXBException e) {
           throw new SonosSoapFault.LoginUnauthorized();
        }

    }


    private String getToken(SoapMessage message) throws JAXBException {
        QName creadentialQName = new QName("http://www.sonos.com/Services/1.1", "credentials");

        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        List<Header> headers = message.getHeaders();

        for (Header header : headers) {
            if (creadentialQName.equals(header.getName())) {
                ElementNSImpl elementNS = (ElementNSImpl) header.getObject();
                Credentials credentials = unmarshaller.unmarshal(elementNS, Credentials.class).getValue();

                return credentials.getLoginToken().getToken();
            }
        }

        return null;
    }

    private String getAction(SoapMessage message) {
        Object soapAction = message.get("SOAPAction");

        if (soapAction instanceof String) {
            String[] split = ((String) soapAction).split("#");
            if (split.length > 1) {
                return split[1];
            }
        }
        return null;

    }

}
