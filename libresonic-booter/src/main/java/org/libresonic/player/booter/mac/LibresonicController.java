package org.libresonic.player.booter.mac;

import org.libresonic.player.booter.deployer.LibresonicDeployerService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.net.URL;
import java.net.URI;

/**
 * Controller for the Mac booter.
 *
 * @author Sindre Mehus
 */
public class LibresonicController {

    private final LibresonicDeployerService deployer;
    private final LibresonicFrame frame;
    private Action openAction;
    private Action controlPanelAction;
    private Action quitAction;

    public LibresonicController(LibresonicDeployerService deployer, LibresonicFrame frame) {
        this.deployer = deployer;
        this.frame = frame;
        createActions();
        createComponents();
    }

    private void createActions() {
        openAction = new AbstractAction("Open Libresonic Web Page") {
            public void actionPerformed(ActionEvent e) {
                openBrowser();
            }
        };

        controlPanelAction = new AbstractAction("Libresonic Control Panel") {
            public void actionPerformed(ActionEvent e) {
                frame.setActive(false);
                frame.setActive(true);
            }
        };

        quitAction = new AbstractAction("Quit Libresonic") {
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        };
    }

    private void createComponents() {
        PopupMenu menu = new PopupMenu();
        menu.add(createMenuItem(openAction));
        menu.add(createMenuItem(controlPanelAction));
        menu.addSeparator();
        menu.add(createMenuItem(quitAction));

        URL url = getClass().getResource("/images/libresonic-21.png");
        Image image = Toolkit.getDefaultToolkit().createImage(url);
        TrayIcon trayIcon = new TrayIcon(image, "Libresonic Music Streamer", menu);
        trayIcon.setImageAutoSize(false);

        try {
            SystemTray.getSystemTray().add(trayIcon);
        } catch (Throwable x) {
            System.err.println("Failed to add tray icon.");
        }
    }

    private MenuItem createMenuItem(Action action) {
        MenuItem menuItem = new MenuItem((String) action.getValue(Action.NAME));
        menuItem.addActionListener(action);
        return menuItem;
    }

    private void openBrowser() {
        String url = deployer.getDeploymentInfo().getURL();
        if (url == null) {
            return;
        }
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (Throwable x) {
            x.printStackTrace();
        }
    }

}