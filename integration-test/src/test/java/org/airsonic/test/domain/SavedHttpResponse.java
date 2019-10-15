package org.airsonic.test.domain;

import org.apache.http.Header;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class SavedHttpResponse  {
    private final List<Header> headers;
    private final byte[] body;

    public SavedHttpResponse(List<Header> headers, byte[] body) {
        this.headers = headers;
        this.body = body;
    }

    public List<Header> getHeaders() {
        return headers;
    }

    public byte[] getBody() {
        return body;
    }

    public Header getHeader(String name) {
        List<Header> matchingHeaders = headers.stream().filter(header ->
                Objects.equals(header.getName(), name))
                .collect(Collectors.toList());
        if(matchingHeaders.size() != 1) {
            return null;
        }
        return matchingHeaders.iterator().next();
    }

}
