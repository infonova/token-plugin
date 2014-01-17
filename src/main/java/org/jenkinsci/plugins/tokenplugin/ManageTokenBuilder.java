package org.jenkinsci.plugins.tokenplugin;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Cause;
import hudson.model.Cause.UpstreamCause;
import hudson.model.Cause.UserIdCause;
import hudson.model.FreeStyleProject;
import hudson.model.Run;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.triggers.TimerTrigger.TimerTriggerCause;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;
import org.jenkinsci.plugins.tokenplugin.SystemStatusInformation.Status;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

/**
 *
 * @author Stefan Eder
 */
public class ManageTokenBuilder extends Builder {

    private static final String UNLOCK_ACTION = "unlock";
    private static final String LOCK_ACTION = "lock";
    private static final String SET_HEADERLINK_ACTION = "setHeaderLink";
    private static final String LOCK_AND_SET_HEADERLINK_ACTION = "lockAndSetHeaderLink";
    private static final String UNLOCK_AND_RESET_HEADERLINK_ACTION = "unlockAndResetHeaderLink";    
    private static final String TIMER_USERID = "timer";

    private static final String SYSTEM_REGEX = "^[a-zA-Z0-9_\\$\\{\\}]*$";

    private final String systemName;
    private final String headerLink;
    private final String action;
    private final boolean forceAction;

    @DataBoundConstructor
    public ManageTokenBuilder(String systemName, String action, String headerLink, boolean forceAction) {
        this.systemName = systemName;
        this.action = action;
        this.headerLink = headerLink;
        this.forceAction = forceAction;
    }

    public String getSystemName() {
        return systemName;
    }

    public String getAction() {
        return action;
    }

    public boolean getForceAction() {
	return forceAction;
    }

    private boolean buildIsCausedBy(Class<? extends Cause> clazz, Run<?, ?> build) {
        final List<Cause> causes = build.getCauses();

        for (final Cause cause : causes) {
            if (StringUtils.equals(cause.getClass().getName(), clazz.getName())) {
                return true;
            }
        }

        return false;
    }

 private Run<?,?> getRootBuild(Run<?,?> build) {
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
        
    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
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
            continueBuild = getDescriptor().unlockSystem(expandedSystemName, "system", logger);
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
                    continueBuild = getDescriptor().unlockSystem(expandedSystemName, userId, logger);
                } else if (StringUtils.equals(LOCK_ACTION, action)) {
                    continueBuild = getDescriptor().lockSystem(expandedSystemName, userId, forceAction, logger);
                } else if (StringUtils.equals(SET_HEADERLINK_ACTION, action)) {
                    getDescriptor().setHeaderLink(expandedSystemName, expandedHeaderLink, logger);
                } else if (StringUtils.equals(LOCK_AND_SET_HEADERLINK_ACTION, action)) {
                    continueBuild = getDescriptor().lockSystem(expandedSystemName, userId, forceAction, logger);
                    getDescriptor().setHeaderLink(expandedSystemName, expandedHeaderLink, logger);                    
                } else if (StringUtils.equals(UNLOCK_AND_RESET_HEADERLINK_ACTION, action)) {
                    continueBuild = getDescriptor().unlockSystem(expandedSystemName, userId, logger);
                    getDescriptor().setHeaderLink(expandedSystemName, null, logger);
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

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        private Map<String, SystemStatusInformation> systems;

        public DescriptorImpl() {
            load();

            if (systems == null) {
                systems = new TreeMap<String, SystemStatusInformation>();
            }
        }

        public String getDisplayName() {
            return "Lock or Unlock System";
        }

        public Map<String, SystemStatusInformation> getSystems() {
            return Collections.unmodifiableMap(systems);
        }

        @SuppressWarnings("rawtypes")
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return FreeStyleProject.class.isAssignableFrom(jobType);
        }

        public FormValidation doCheckSystemName(@QueryParameter String value) {
            if (StringUtils.isBlank(value)) {
                return FormValidation.error("Please enter the name of the system");
            } else if (!Pattern.matches(SYSTEM_REGEX, value)) {
                return FormValidation
                                .error("Please enter a valid name of the system (may only contain letters, numbers, and _)");
            }
            return FormValidation.ok();
        }

        public ListBoxModel doFillActionItems() {
            ListBoxModel actionListBox = new ListBoxModel(3);
            actionListBox.add("Lock System", LOCK_ACTION);
            actionListBox.add("Unlock System", UNLOCK_ACTION);
            actionListBox.add("Set Header Link", SET_HEADERLINK_ACTION);
            actionListBox.add("Lock System and Set Header Link", LOCK_AND_SET_HEADERLINK_ACTION);
            actionListBox.add("Unlock System and Reset Header Link", UNLOCK_AND_RESET_HEADERLINK_ACTION);            
            return actionListBox;
        }

        private void setHeaderLink(final String systemName, final String headerLink, final PrintStream logger) {
            SystemStatusInformation systemStatusInformation = systems.get(systemName);
            logger.println(String.format("setting headerLink for system '%s' from '%s' to '%s' ", systemName,
                systemStatusInformation.getHeaderLink(), headerLink));
            systemStatusInformation.setHeaderLink(headerLink);
//            systems.put(systemName, systemStatusInformation);
            save();
        }

        private boolean lockSystem(final String systemName, final String userId, final boolean forceAction, final PrintStream logger) {
            logger.println("User '" + userId + "' is trying to lock system '" + systemName + "'");

            final SystemStatusInformation systemInformation = systems.get(systemName);
            
            if (systemUnlockedOrNew(systemInformation) || jobStartedByTimerAndForceActionIsTrue(userId, forceAction)) {
                updateLockStatus(systemName, systemInformation, userId, Status.LOCKED);
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

        private void updateLockStatus(String systemName, SystemStatusInformation systemInformation, String userId, Status status) {
            SystemStatusInformation updatedSystemInfo;
            if (systemInformation != null) {
                updatedSystemInfo = new SystemStatusInformation(systemInformation);
                updatedSystemInfo.setUserId(userId);
                updatedSystemInfo.setStatus(status);
            } else {
                updatedSystemInfo = new SystemStatusInformation(userId, status);
            }
            systems.put(systemName, updatedSystemInfo);
            save();
        }

        private boolean systemUnlockedOrNew(final SystemStatusInformation systemInformation) {
            return (systemInformation == null) || Status.UNLOCKED.equals(systemInformation.getStatus());
        }
        
        private boolean jobStartedByTimerAndForceActionIsTrue(final String userId, final boolean forceAction) {
            return (TIMER_USERID.equals(userId) && forceAction);
        }

        private boolean unlockSystem(final String systemName, final String userId, final PrintStream logger) {
            logger.println("User '" + userId + "' is trying to unlock system '" + systemName + "'");

            final SystemStatusInformation systemInformation = systems.get(systemName);

            if (systemUnlockedOrNew(systemInformation)) {
                logger.println("System '" + systemName + "' is not locked");
            } else {
                updateLockStatus(systemName, systemInformation, userId, Status.UNLOCKED);
                logger.println("System '" + systemName + "' was unlocked by user '" + userId + "'");
            }

            // in case the unlock can fail in the future
            return true;
        }

    }


    public String getHeaderLink() {
        return headerLink;
    }
}
