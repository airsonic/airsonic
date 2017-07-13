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
 Based upon Subsonic, Copyright 2013 (C) Sindre Mehus
 */
package org.airsonic.player.controller;

import org.apache.commons.lang.RandomStringUtils;

import javax.swing.*;

import java.awt.*;
import java.io.IOException;

/**
 * @author Sindre Mehus
 * @version $Id$
 */
public class AutoCoverDemo {

    public static void main(String[] args) throws IOException {
        JFrame frame = new JFrame();
        JPanel panel = new JPanel();
        panel.add(new AlbumComponent(110, 110));
        panel.add(new AlbumComponent(150, 150));
        panel.add(new AlbumComponent(200, 200));
        panel.add(new AlbumComponent(300, 300));
        panel.add(new AlbumComponent(400, 240));
        panel.add(new AlbumComponent(240, 400));

        panel.setBackground(Color.LIGHT_GRAY);
        frame.add(panel);
        frame.setSize(1000, 800);
        frame.setVisible(true);
    }

    private static class AlbumComponent extends JComponent {
        private final int width;
        private final int height;

        public AlbumComponent(int width, int height) {
            this.width = width;
            this.height = height;
            setPreferredSize(new Dimension(width, height));
        }

        @Override
        protected void paintComponent(Graphics g) {
            String key = RandomStringUtils.random(5);
            new CoverArtController.AutoCover((Graphics2D) g, key, "Artist with a very long name", "Album", width, height).paintCover();
        }
    }
}
