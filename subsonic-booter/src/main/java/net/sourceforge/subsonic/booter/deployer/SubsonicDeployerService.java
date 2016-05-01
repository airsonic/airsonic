package net.sourceforge.subsonic.booter.deployer;

/**
 * RMI interface implemented by the Subsonic deployer and used by the agent.
 *
 * @author Sindre Mehus
 */
public interface SubsonicDeployerService {

    /**
     * Returns information about the Subsonic deployment, such
     * as URL, memory consumption, start time etc.
     *
     * @return Deployment information.
     */
    DeploymentStatus getDeploymentInfo();
}
