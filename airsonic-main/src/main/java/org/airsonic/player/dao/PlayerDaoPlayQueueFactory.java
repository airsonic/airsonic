package org.airsonic.player.dao;

import org.airsonic.player.domain.PlayQueue;
import org.springframework.stereotype.Component;

@Component
public class PlayerDaoPlayQueueFactory {

    public PlayQueue createPlayQueue() {
        return new PlayQueue();
    }
}
