package net.sourceforge.subsonic.booter.agent;

import java.awt.Desktop;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.JOptionPane;
import javax.swing.UIManager;

import org.apache.commons.io.IOUtils;

import com.jgoodies.looks.plastic.PlasticXPLookAndFeel;

import net.sourceforge.subsonic.booter.deployer.DeploymentStatus;
import net.sourceforge.subsonic.booter.deployer.SubsonicDeployerService;

/**
 * Responsible for deploying the Subsonic web app in
 * the embedded Jetty container.
 *
 * @author Sindre Mehus
 */
public class SubsonicAgent {

    private final List<SubsonicListener> listeners = new ArrayList<SubsonicListener>();
    private final TrayController trayController;
    private SubsonicFrame frame;
    private final SubsonicDeployerService service;
    private static final int POLL_INTERVAL_DEPLOYMENT_INFO_SECONDS = 5;
    private static final int POLL_INTERVAL_SERVICE_STATUS_SECONDS = 5;
    private String url;
    private boolean serviceStatusPollingEnabled;
    private boolean elevated;

    public SubsonicAgent(SubsonicDeployerService service) {
        this.service = service;
        setLookAndFeel();
        trayController = new TrayController(this);
        startPolling();
    }

    public void setFrame(SubsonicFrame frame) {
        this.frame = frame;
    }

    private void setLookAndFeel() {
        // Set look-and-feel.
        try {
            UIManager.setLookAndFeel(new PlasticXPLookAndFeel());
        } catch (Throwable x) {
            System.err.println("Failed to set look-and-feel.\n" + x);
        }
    }

    private void startPolling() {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
        Runnable runnable = new Runnable() {
            public void run() {
                try {
                    notifyDeploymentInfo(service.getDeploymentInfo());
                } catch (Throwable x) {
                    notifyDeploymentInfo(null);
                }
            }
        };
        executor.scheduleWithFixedDelay(runnable, 0, POLL_INTERVAL_DEPLOYMENT_INFO_SECONDS, TimeUnit.SECONDS);

        runnable = new Runnable() {
            public void run() {
                if (serviceStatusPollingEnabled) {
                    try {
                        notifyServiceStatus(getServiceStatus());
                    } catch (Throwable x) {
                        notifyServiceStatus(null);
                    }
                }
            }
        };
        executor.scheduleWithFixedDelay(runnable, 0, POLL_INTERVAL_SERVICE_STATUS_SECONDS, TimeUnit.SECONDS);
    }

    private String getServiceStatus() throws Exception {
        Process process = Runtime.getRuntime().exec("subsonic-service.exe -status");
        return IOUtils.toString(process.getInputStream());
    }

    public void setServiceStatusPollingEnabled(boolean enabled) {
        serviceStatusPollingEnabled = enabled;
    }

    public void startOrStopService(boolean start) {
        try {
            String cmd = "subsonic-service.exe " + (start ? "-start" : "-stop");
            System.err.println("Executing: " + cmd);

            Runtime.getRuntime().exec(cmd);
        } catch (Exception x) {
            x.printStackTrace();
        }
    }

    /**
     * If necessary, restart agent with elevated rights.
     */
    public void checkElevation(String... args) {

        if (isElevationNeeded() && !isElevated()) {
            try {
                List<String> command = new ArrayList<String>();
                command.add("cmd");
                command.add("/c");
                command.add("subsonic-agent-elevated.exe");
                command.addAll(Arrays.asList(args));

                ProcessBuilder builder = new ProcessBuilder();
                builder.command(command);
                System.err.println("Executing: " + command + " with current dir: " + System.getProperty("user.dir"));
                builder.start();
                System.exit(0);
            } catch (Exception x) {
                JOptionPane.showMessageDialog(frame, "Failed to elevate Subsonic Control Panel. " + x, "Error", JOptionPane.WARNING_MESSAGE);
                x.printStackTrace();
            }
        }
    }

    public void setElevated(boolean elevated) {
        this.elevated = elevated;
    }

    private boolean isElevated() {
        return elevated;
    }

    /**
     * Returns whether UAC elevation is necessary (to start/stop services etc).
     */
    private boolean isElevationNeeded() {

        String osVersion = System.getProperty("os.version");
        try {
            int majorVersion = Integer.parseInt(osVersion.substring(0, osVersion.indexOf(".")));

            // Elevation is necessary in Windows Vista (os.version=6.1) and later.
            return majorVersion >= 6;
        } catch (Exception x) {
            System.err.println("Failed to resolve OS version from '" + osVersion + "'\n" + x);
            return false;
        }
    }

    public void addListener(SubsonicListener listener) {
        listeners.add(listener);
    }

    private void notifyDeploymentInfo(DeploymentStatus status) {
        if (status != null) {
            url = status.getURL();
        }

        for (SubsonicListener listener : listeners) {
            listener.notifyDeploymentStatus(status);
        }
    }

    private void notifyServiceStatus(String status) {
        for (SubsonicListener listener : listeners) {
            listener.notifyServiceStatus(status);
        }
    }

    public void showStatusPanel() {
        frame.showStatusPanel();
    }

    public void showSettingsPanel() {
        frame.showSettingsPanel();
    }

    public void showTrayIconMessage() {
        trayController.showMessage();
    }

    public void exit() {
        trayController.uninstallComponents();
        System.exit(0);
    }

    public void openBrowser() {
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (Throwable x) {
            x.printStackTrace();
        }
    }
}
