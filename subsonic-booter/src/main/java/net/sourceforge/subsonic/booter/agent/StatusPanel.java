package net.sourceforge.subsonic.booter.agent;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.FormLayout;
import net.sourceforge.subsonic.booter.deployer.DeploymentStatus;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.util.Locale;

/**
 * Panel displaying the status of the Subsonic service.
 *
 * @author Sindre Mehus
 */
public class StatusPanel extends JPanel implements SubsonicListener {

    private static final DateFormat DATE_FORMAT = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, Locale.US);

    private final SubsonicAgent subsonicAgent;

    private JTextField statusTextField;
    private JTextField startedTextField;
    private JTextField memoryTextField;
    private JTextArea errorTextField;
    private JButton startButton;
    private JButton stopButton;
    private JButton urlButton;

    public StatusPanel(SubsonicAgent subsonicAgent) {
        this.subsonicAgent = subsonicAgent;
        createComponents();
        configureComponents();
        layoutComponents();
        addBehaviour();
        subsonicAgent.addListener(this);
    }

    private void createComponents() {
        statusTextField = new JTextField();
        startedTextField = new JTextField();
        memoryTextField = new JTextField();
        errorTextField = new JTextArea(3, 24);
        startButton = new JButton("Start");
        stopButton = new JButton("Stop");
        urlButton = new JButton();
    }

    private void configureComponents() {
        statusTextField.setEditable(false);
        startedTextField.setEditable(false);
        memoryTextField.setEditable(false);
        errorTextField.setEditable(false);

        errorTextField.setLineWrap(true);
        errorTextField.setBorder(startedTextField.getBorder());

        urlButton.setBorderPainted(false);
        urlButton.setContentAreaFilled(false);
        urlButton.setForeground(Color.BLUE.darker());
        urlButton.setHorizontalAlignment(SwingConstants.LEFT);
    }

    private void layoutComponents() {
        JPanel buttons = ButtonBarFactory.buildRightAlignedBar(startButton, stopButton);

        FormLayout layout = new FormLayout("right:d, 6dlu, max(d;30dlu):grow");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout, this);
        builder.append("Service status", statusTextField);
        builder.append("", buttons);
        builder.appendParagraphGapRow();
        builder.nextRow();
        builder.append("Started on", startedTextField);
        builder.append("Memory used", memoryTextField);
        builder.append("Error message", errorTextField);
        builder.append("Server address", urlButton);

        setBorder(Borders.DIALOG_BORDER);
    }

    private void addBehaviour() {
        urlButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                subsonicAgent.openBrowser();
            }
        });
        startButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                subsonicAgent.checkElevation("-start");
                subsonicAgent.startOrStopService(true);
            }
        });
        stopButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                subsonicAgent.checkElevation("-stop");
                subsonicAgent.startOrStopService(false);
            }
        });
    }

    public void notifyDeploymentStatus(DeploymentStatus status) {
        startedTextField.setText(status == null ? null : DATE_FORMAT.format(status.getStartTime()));
        memoryTextField.setText(status == null ? null : status.getMemoryUsed() + " MB");
        errorTextField.setText(status == null ? null : status.getErrorMessage());
        urlButton.setText(status == null ? null : status.getURL());
    }

    public void notifyServiceStatus(String serviceStatus) {
        statusTextField.setText(serviceStatus);
    }
}
