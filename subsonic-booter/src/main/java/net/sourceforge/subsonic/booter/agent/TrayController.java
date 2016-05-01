package net.sourceforge.subsonic.booter.agent;

import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.net.URL;

import javax.swing.AbstractAction;
import javax.swing.Action;

import net.sourceforge.subsonic.booter.deployer.DeploymentStatus;

/**
 * Controls the Subsonic tray icon.
 *
 * @author Sindre Mehus
 */
public class TrayController implements SubsonicListener {

    private final SubsonicAgent subsonicAgent;
    private TrayIcon trayIcon;

    private Action openAction;
    private Action controlPanelAction;
    private Action hideAction;
    private Image startedImage;
    private Image stoppedImage;

    public TrayController(SubsonicAgent subsonicAgent) {
        this.subsonicAgent = subsonicAgent;
        try {
            createActions();
            createComponents();
            addBehaviour();
            installComponents();
            subsonicAgent.addListener(this);
        } catch (Throwable x) {
            System.err.println("Disabling tray support.");
        }
    }

    public void showMessage() {
        trayIcon.displayMessage("Subsonic", "Subsonic is now running. Click this balloon to get started.",
                TrayIcon.MessageType.INFO);
    }

    private void createActions() {
        openAction = new AbstractAction("Open Subsonic in Browser") {
            public void actionPerformed(ActionEvent e) {
                subsonicAgent.openBrowser();
            }
        };

        controlPanelAction = new AbstractAction("Subsonic Control Panel") {
            public void actionPerformed(ActionEvent e) {
                subsonicAgent.showStatusPanel();
            }
        };


        hideAction = new AbstractAction("Hide Tray Icon") {
            public void actionPerformed(ActionEvent e) {
                subsonicAgent.exit();
            }
        };
    }

    private void createComponents() {
        startedImage = createImage("/images/subsonic-started-16.png");
        stoppedImage = createImage("/images/subsonic-stopped-16.png");

        PopupMenu menu = new PopupMenu();
        menu.add(createMenuItem(openAction));
        menu.add(createMenuItem(controlPanelAction));
        menu.addSeparator();
        menu.add(createMenuItem(hideAction));

        trayIcon = new TrayIcon(stoppedImage, "Subsonic Music Streamer", menu);
    }

    private Image createImage(String resourceName) {
        URL url = getClass().getResource(resourceName);
        return Toolkit.getDefaultToolkit().createImage(url);
    }

    private MenuItem createMenuItem(Action action) {
        MenuItem menuItem = new MenuItem((String) action.getValue(Action.NAME));
        menuItem.addActionListener(action);
        return menuItem;
    }

    private void addBehaviour() {
        trayIcon.addActionListener(controlPanelAction);
    }

    private void installComponents() throws Throwable {
        SystemTray.getSystemTray().add(trayIcon);
    }

    public void uninstallComponents() {
        try {
            SystemTray.getSystemTray().remove(trayIcon);
        } catch (Throwable x) {
            System.err.println("Disabling tray support.");
        }
    }

    private void setTrayImage(Image image) {
        if (trayIcon.getImage() != image) {
            trayIcon.setImage(image);
        }
    }

    public void notifyDeploymentStatus(DeploymentStatus deploymentStatus) {
        setTrayImage(deploymentStatus == null ? stoppedImage : startedImage);
    }

    public void notifyServiceStatus(String serviceStatus) {
        // Nothing here, but could potentially change tray icon and menu.
    }
}
