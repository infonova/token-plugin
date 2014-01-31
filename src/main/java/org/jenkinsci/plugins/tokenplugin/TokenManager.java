package org.jenkinsci.plugins.tokenplugin;

import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.model.Cause;
import hudson.model.Cause.UpstreamCause;
import hudson.model.Cause.UserIdCause;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.tasks.BuildStep;
import hudson.triggers.TimerTrigger.TimerTriggerCause;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;
import org.jenkinsci.plugins.tokenplugin.SystemStatusInformation.Status;


public class TokenManager {

    private static final String UNLOCK_ACTION = "unlock";
    private static final String LOCK_ACTION = "lock";
    private static final String SET_HEADERLINK_ACTION = "setHeaderLink";
    private static final String LOCK_AND_SET_HEADERLINK_ACTION = "lockAndSetHeaderLink";
    private static final String UNLOCK_AND_RESET_HEADERLINK_ACTION = "unlockAndResetHeaderLink";
    private static final String TIMER_USERID = "timer";

    private static class Systems {

        private static Map<String, SystemStatusInformation> systems;

        public static Map<String, SystemStatusInformation> getInstance() {

            if (systems == null) {
                systems = new TreeMap<String, SystemStatusInformation>();
            }
            return systems;
        }
    }

    public static boolean manageToken(AbstractBuild<?, ?> build, TaskListener listener,
            Descriptor<? extends BuildStep> descriptor, String systemName, String headerLink, String action,
            boolean forceAction) throws IOException, InterruptedException {

        final PrintStream logger = listener.getLogger();
        boolean continueBuild = true;

        String expandedSystemName;
        try {
            expandedSystemName = TokenMacro.expandAll(build, listener, systemName);
        } catch (MacroEvaluationException e) {
            logger.println("Could not expand name of system '" + systemName + "'");
            return false;
        }

        String expandedHeaderLink;
        try {
            expandedHeaderLink = TokenMacro.expandAll(build, listener, headerLink);
        } catch (MacroEvaluationException e) {
            logger.println("Could not expand name of system '" + systemName + "'");
            return false;
        }

        final Run<?, ?> rootBuild = getRootBuild(build);

        if (StringUtils.isBlank(expandedSystemName)) {
            logger.println("Please enter a name for the system");
            continueBuild = false;
        } else if (buildIsCausedBy(TimerTriggerCause.class, rootBuild) && !forceAction) {
            logger.println("Was triggered by timer and job is not configured to force action. Unlocking system '" + expandedSystemName + "' by default.");
            continueBuild = unlockSystem(expandedSystemName, "system", logger, descriptor);
        } else {
            final UserIdCause userIdCause = rootBuild.getCause(UserIdCause.class);
            String userId = null;

            if (userIdCause != null) {
                // default if blank in case anonymous user triggered job
                userId = StringUtils.defaultIfBlank(userIdCause.getUserId(), userIdCause.getUserName());
            } else if (buildIsCausedBy(TimerTriggerCause.class, rootBuild) && forceAction) {
                userId = TIMER_USERID;
                logger.println("Was triggered by timer and job is configured to force action.");
            }

            if (!StringUtils.isEmpty(userId)) {
                if (StringUtils.equals(UNLOCK_ACTION, action)) {
                    continueBuild = unlockSystem(expandedSystemName, userId, logger, descriptor);
                } else if (StringUtils.equals(LOCK_ACTION, action)) {
                    continueBuild = lockSystem(expandedSystemName, userId, forceAction, logger, descriptor);
                } else if (StringUtils.equals(SET_HEADERLINK_ACTION, action)) {
                    setHeaderLink(expandedSystemName, expandedHeaderLink, logger, descriptor);
                } else if (StringUtils.equals(LOCK_AND_SET_HEADERLINK_ACTION, action)) {
                    continueBuild = lockSystem(expandedSystemName, userId, forceAction, logger, descriptor);
                    setHeaderLink(expandedSystemName, expandedHeaderLink, logger, descriptor);
                } else if (StringUtils.equals(UNLOCK_AND_RESET_HEADERLINK_ACTION, action)) {
                    continueBuild = unlockSystem(expandedSystemName, userId, logger, descriptor);
                    setHeaderLink(expandedSystemName, null, logger, descriptor);
                } else {
                    logger.println("unknown action");
                    continueBuild = false;
                }
            } else {
                logger.println("(Upstream) Build was not caused by Timer or User. Build-Cause(s): " + rootBuild.getCauses());
                continueBuild = false;
            }
        }

        return continueBuild;
    }

    private static boolean lockSystem(final String systemName, final String userId, final boolean forceAction, final PrintStream logger, Descriptor<? extends BuildStep> descriptor) {
        logger.println("User '" + userId + "' is trying to lock system '" + systemName + "'");

        final SystemStatusInformation systemInformation = Systems.getInstance().get(systemName);

        if (systemUnlockedOrNew(systemInformation) || jobStartedByTimerAndForceActionIsTrue(userId, forceAction)) {
            updateLockStatus(systemName, systemInformation, userId, Status.LOCKED, descriptor);
            logger.println("System '" + systemName + "' locked");
        } else {
            final String lockeeUserId = systemInformation.getUserId();
            logger.println("System '" + systemName + "' was already locked by '" + lockeeUserId + "'");

            if (!StringUtils.equals(userId, lockeeUserId)) {
                logger.println("Lockee '" + lockeeUserId + "' is not current user '" + userId
                    + "'. Aborting build...");
                return false;
            }
        }
        return true;
    }

    private static void setHeaderLink(final String systemName, final String headerLink, final PrintStream logger, Descriptor<? extends BuildStep> descriptor) {
        SystemStatusInformation systemStatusInformation = Systems.getInstance().get(systemName);
        logger.println(String.format("setting headerLink for system '%s' from '%s' to '%s' ", systemName,
            systemStatusInformation.getHeaderLink(), headerLink));
        systemStatusInformation.setHeaderLink(headerLink);
        //        Systems.getInstance().put(systemName, systemStatusInformation);
        descriptor.save();
    }

    private static Run<?,?> getRootBuild(Run<?,?> build) {
        Run<?,?> result;
        if (buildIsCausedBy(UpstreamCause.class, build)) {
            final UpstreamCause upstreamCause = build.getCause(UpstreamCause.class);
            final Run<?, ?> upstreamBuild = upstreamCause.getUpstreamRun();

            if (buildIsCausedBy(UpstreamCause.class, upstreamBuild)) {
                result = getRootBuild(upstreamBuild);
            } else {
                result = upstreamBuild;
            }
        } else {
            result = build;
        }
        return result;
    }

    private static void updateLockStatus(String systemName, SystemStatusInformation systemInformation, String userId, Status status, Descriptor<? extends BuildStep> descriptor) {
        SystemStatusInformation updatedSystemInfo;
        if (systemInformation != null) {
            updatedSystemInfo = new SystemStatusInformation(systemInformation);
            updatedSystemInfo.setUserId(userId);
            updatedSystemInfo.setStatus(status);
        } else {
            updatedSystemInfo = new SystemStatusInformation(userId, status);
        }
        Systems.getInstance().put(systemName, updatedSystemInfo);
        descriptor.save();
    }

    private static boolean systemUnlockedOrNew(final SystemStatusInformation systemInformation) {
        return (systemInformation == null) || Status.UNLOCKED.equals(systemInformation.getStatus());
    }

    private static boolean jobStartedByTimerAndForceActionIsTrue(final String userId, final boolean forceAction) {
        return (TIMER_USERID.equals(userId) && forceAction);
    }

    private static boolean unlockSystem(final String systemName, final String userId, final PrintStream logger, Descriptor<? extends BuildStep> descriptor) {
        logger.println("User '" + userId + "' is trying to unlock system '" + systemName + "'");

        final SystemStatusInformation systemInformation = Systems.getInstance().get(systemName);

        if (systemUnlockedOrNew(systemInformation)) {
            logger.println("System '" + systemName + "' is not locked");
        } else {
            updateLockStatus(systemName, systemInformation, userId, Status.UNLOCKED, descriptor);
            logger.println("System '" + systemName + "' was unlocked by user '" + userId + "'");
        }

        // in case the unlock can fail in the future
        return true;
    }

    private static boolean buildIsCausedBy(Class<? extends Cause> clazz, Run<?, ?> build) {
        final List<Cause> causes = build.getCauses();

        for (final Cause cause : causes) {
            if (StringUtils.equals(cause.getClass().getName(), clazz.getName())) {
                return true;
            }
        }

        return false;
    }

    public static Map<String, SystemStatusInformation> getSystems() {
        return Collections.unmodifiableMap(Systems.getInstance());
    }
}
