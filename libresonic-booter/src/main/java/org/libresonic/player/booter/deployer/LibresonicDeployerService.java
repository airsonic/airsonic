package org.libresonic.player.booter.deployer;

/**
 * RMI interface implemented by the Libresonic deployer and used by the agent.
 *
 * @author Sindre Mehus
 */
public interface LibresonicDeployerService {

    /**
     * Returns information about the Libresonic deployment, such
     * as URL, memory consumption, start time etc.
     *
     * @return Deployment information.
     */
    DeploymentStatus getDeploymentInfo();
}
