package org.airsonic.player.service.sonos;

import com.google.common.collect.Sets;

import com.sonos.services._1.Credentials;
import com.sun.org.apache.xerces.internal.dom.ElementNSImpl;
import org.airsonic.player.service.SecurityService;
import org.airsonic.player.service.SettingsService;
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

import java.util.Set;

import static org.airsonic.player.service.sonos.SonosServiceRegistration.AuthenticationType;

/**
 * <p>Interceptor for the Soap sonos, is validate access for all methods (Soap Action) except methods for exchange
 * on set link between airsonic and sonos controller.</p>
 *
 * <p>The validation can be adapt for different link method from sonos.</p>
 */
@Component
public class SonosLinkInterceptor extends AbstractSoapInterceptor {
    private static final Logger LOG = LoggerFactory.getLogger(SonosLinkInterceptor.class);
    private static Set<String> openMethod = Sets.newHashSet("getAppLink", "getDeviceAuthToken");

    public SonosLinkInterceptor() {
        super(Phase.PRE_INVOKE);
    }

    @Autowired
    private SecurityService securityService;

    @Autowired
    private SettingsService settingsService;

    private JAXBContext jaxbContext;

    @PostConstruct
    public void postConstruct() throws JAXBException {
        jaxbContext = JAXBContext.newInstance("com.sonos.services._1");
    }

    @Override
    public void handleMessage(SoapMessage message) throws Fault {
        try {
            if (!settingsService.isSonosEnabled()) {
                throw new SonosSoapFault.LoginUnauthorized();
            }

            String action = getAction(message);
            AuthenticationType authenticationType = AuthenticationType.valueOf(settingsService.getSonosLinkMethod());

            if (action != null && openMethod.contains(action)) {
                LOG.debug("Unable to process SOAP message: " + message.toString());

            } else if (action != null && authenticationType == AuthenticationType.APPLICATION_LINK) {

                String sonosLinkToken = getToken(message);
                if (sonosLinkToken != null) {
                    securityService.authenticate(sonosLinkToken);
                }
            } else if (action != null && authenticationType == AuthenticationType.ANONYMOUS) {
                securityService.authenticate();
            } else {
                LOG.debug("Unable to process SOAP message : " + message.toString());
                throw new SonosSoapFault.LoginUnauthorized();
            }
        } catch (JAXBException e) {
            throw new SonosSoapFault.LoginUnauthorized();
        }

    }


    private String getToken(SoapMessage message) throws JAXBException {
        QName creadentialQName = new QName("http://www.sonos.com/Services/1.1", "credentials");

        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

        for (Header header : message.getHeaders()) {
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
