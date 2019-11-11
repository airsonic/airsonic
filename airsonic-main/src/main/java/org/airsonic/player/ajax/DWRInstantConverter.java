package org.airsonic.player.ajax;

import org.directwebremoting.ConversionException;
import org.directwebremoting.extend.AbstractConverter;
import org.directwebremoting.extend.InboundVariable;
import org.directwebremoting.extend.NonNestedOutboundVariable;
import org.directwebremoting.extend.OutboundContext;
import org.directwebremoting.extend.OutboundVariable;
import org.directwebremoting.extend.ProtocolConstants;

import java.time.Instant;

/**
 * This class converts an Instant object between Java and JS
 *
 */
public class DWRInstantConverter extends AbstractConverter {

    @Override
    public Object convertInbound(Class<?> paramType, InboundVariable data) throws ConversionException {
        if (data.isNull()) {
            return null;
        }

        String value = data.getValue();

        // If the text is null then the whole bean is null
        if (value.trim().equals(ProtocolConstants.INBOUND_NULL)) {
            return null;
        }

        try {
            long millis = 0;
            if (value.length() > 0) {
                millis = Long.parseLong(value);
            }

            if (paramType == Instant.class) {
                return Instant.ofEpochMilli(millis);
            } else {
                throw new ConversionException(paramType);
            }
        }
        catch (ConversionException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new ConversionException(paramType, ex);
        }
    }

    @Override
    public OutboundVariable convertOutbound(Object data, OutboundContext outboundContext) throws ConversionException {
        long milliSeconds;

        if (data instanceof Instant) {
            Instant dateTime = (Instant) data;
            milliSeconds = dateTime.toEpochMilli();
        } else {
            throw new ConversionException(data.getClass());
        }
        return new NonNestedOutboundVariable("new Date(" + milliSeconds + ")");
    }

}
