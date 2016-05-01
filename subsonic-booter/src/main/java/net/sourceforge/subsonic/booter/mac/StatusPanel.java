package net.sourceforge.subsonic.booter.mac;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.text.DateFormat;
import java.util.Locale;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.Timer;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.FormLayout;

import net.sourceforge.subsonic.booter.deployer.DeploymentStatus;
import net.sourceforge.subsonic.booter.deployer.SubsonicDeployerService;

/**
 * Panel displaying the status of the Subsonic service.
 *
 * @author Sindre Mehus
 */
public class StatusPanel extends JPanel {

    private static final DateFormat DATE_FORMAT = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, Locale.US);

    private final SubsonicDeployerService deployer;

    private JTextField startedTextField;
    private JTextField memoryTextField;
    private JTextArea errorTextField;
    private JButton urlButton;

    public StatusPanel(SubsonicDeployerService deployer) {
        this.deployer = deployer;
        createComponents();
        configureComponents();
        layoutComponents();
        addBehaviour();
    }

    private void createComponents() {
        startedTextField = new JTextField();
        memoryTextField = new JTextField();
        errorTextField = new JTextArea(3, 24);
        urlButton = new JButton();
    }

    private void configureComponents() {
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
        FormLayout layout = new FormLayout("right:d, 6dlu, max(d;30dlu):grow");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout, this);
        builder.append("Started on", startedTextField);
        builder.append("Memory used", memoryTextField);
        builder.append("Error message", errorTextField);
        builder.append("Server address", urlButton);

        setBorder(Borders.DIALOG_BORDER);
    }

    private void addBehaviour() {
        urlButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                openBrowser();
            }
        });

        Timer timer = new Timer(3000, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateStatus(deployer.getDeploymentInfo());
            }
        });
        updateStatus(deployer.getDeploymentInfo());
        timer.start();
    }

    private void openBrowser() {
        String url = urlButton.getText();
        if (url == null) {
            return;
        }
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (Throwable x) {
            x.printStackTrace();
        }
    }

    private void updateStatus(DeploymentStatus status) {
        startedTextField.setText(status == null ? null : DATE_FORMAT.format(status.getStartTime()));
        memoryTextField.setText(status == null ? null : status.getMemoryUsed() + " MB");
        errorTextField.setText(status == null ? null : status.getErrorMessage());
        urlButton.setText(status == null ? null : status.getURL());
    }
}