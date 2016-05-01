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
package net.sourceforge.subsonic.ajax;

import net.sourceforge.subsonic.Logger;
import net.sourceforge.subsonic.service.SecurityService;
import net.sourceforge.subsonic.util.BoundedList;
import org.apache.commons.lang.StringUtils;
import org.directwebremoting.WebContext;
import org.directwebremoting.WebContextFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Provides AJAX-enabled services for the chatting.
 * This class is used by the DWR framework (http://getahead.ltd.uk/dwr/).
 *
 * @author Sindre Mehus
 */
public class ChatService {

    private static final Logger LOG = Logger.getLogger(ChatService.class);
    private static final String CACHE_KEY = "1";
    private static final int MAX_MESSAGES = 10;
    private static final long TTL_MILLIS = 3L * 24L * 60L * 60L * 1000L; // 3 days.

    private final LinkedList<Message> messages = new BoundedList<Message>(MAX_MESSAGES);
    private SecurityService securityService;

    private long revision = System.identityHashCode(this);

    /**
     * Invoked by Spring.
     */
    public void init() {
        // Delete old messages every hour.
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        Runnable runnable = new Runnable() {
            public void run() {
                removeOldMessages();
            }
        };
        executor.scheduleWithFixedDelay(runnable, 0L, 3600L, TimeUnit.SECONDS);
    }

    private synchronized void removeOldMessages() {
        long now = System.currentTimeMillis();
        for (Iterator<Message> iterator = messages.iterator(); iterator.hasNext();) {
            Message message = iterator.next();
            if (now - message.getDate().getTime() > TTL_MILLIS) {
                iterator.remove();
                revision++;
            }
        }
    }

    public synchronized void addMessage(String message) {
        WebContext webContext = WebContextFactory.get();
        doAddMessage(message, webContext.getHttpServletRequest());
    }

    public synchronized void doAddMessage(String message, HttpServletRequest request) {

        String user = securityService.getCurrentUsername(request);
        message = StringUtils.trimToNull(message);
        if (message != null && user != null) {
            messages.addFirst(new Message(message, user, new Date()));
            revision++;
        }
    }

    public synchronized void clearMessages() {
        messages.clear();
        revision++;
    }

    /**
     * Returns all messages, but only if the given revision is different from the
     * current revision.
     */
    public synchronized Messages getMessages(long revision) {
        if (this.revision != revision) {
            return new Messages(new ArrayList<Message>(messages), this.revision);
        }
        return null;
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }

    public static class Messages implements Serializable {

        private static final long serialVersionUID = -752602719879818165L;
        private final  List<Message> messages;
        private final long revision;

        public Messages(List<Message> messages, long revision) {
            this.messages = messages;
            this.revision = revision;
        }

        public List<Message> getMessages() {
            return messages;
        }

        public long getRevision() {
            return revision;
        }
    }

    public static class Message implements Serializable {

        private static final long serialVersionUID = -1907101191518133712L;
        private final String content;
        private final String username;
        private final Date date;

        public Message(String content, String username, Date date) {
            this.content = content;
            this.username = username;
            this.date = date;
        }

        public String getContent() {
            return content;
        }

        public String getUsername() {
            return username;
        }

        public Date getDate() {
            return date;
        }

    }
}
