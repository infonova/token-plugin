package org.jenkinsci.plugins.tokenplugin;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.FreeStyleProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

import java.io.IOException;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

/**
 *
 * @author Stefan Eder
 */
public class ManageTokenBuilder extends Builder {

    private static final String UNLOCK_ACTION = "unlock";
    private static final String LOCK_ACTION = "lock";
    private static final String DELETE_ACTION = "delete";
    private static final String SET_HEADERLINK_ACTION = "setHeaderLink";
    private static final String LOCK_AND_SET_HEADERLINK_ACTION = "lockAndSetHeaderLink";
    private static final String UNLOCK_AND_RESET_HEADERLINK_ACTION = "unlockAndResetHeaderLink";
    private static final String SYSTEM_REGEX = "^[a-zA-Z0-9_\\$\\{\\}]*$";

    private final String systemName;
    private final String headerLink;
    private final String tokenAction;
    private final boolean forceAction;
    private final String notice;

    @DataBoundConstructor
    public ManageTokenBuilder(String systemName, String tokenAction, String headerLink, boolean forceAction, String notice) {
        this.systemName = systemName;
        this.tokenAction = tokenAction;
        this.headerLink = headerLink;
        this.forceAction = forceAction;
        this.notice = notice;
    }

    public String getSystemName() {
        return systemName;
    }

    public String getTokenAction() {
        return tokenAction;
    }

    public boolean getForceAction() {
        return forceAction;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {
        listener.getLogger();

        return TokenManager.getInstance().manageToken(build, listener, systemName, headerLink, tokenAction, forceAction, notice);

    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {


        public DescriptorImpl() {
            load();
        }

        public String getDisplayName() {
            return "Delete, Lock or Unlock System";
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

        public ListBoxModel doFillTokenActionItems() {
            ListBoxModel tokenActionListBox = new ListBoxModel(3);
            tokenActionListBox.add("Lock System", LOCK_ACTION);
            tokenActionListBox.add("Unlock System", UNLOCK_ACTION);
            tokenActionListBox.add("Set Header Link", SET_HEADERLINK_ACTION);
            tokenActionListBox.add("Lock System and Set Header Link", LOCK_AND_SET_HEADERLINK_ACTION);
            tokenActionListBox.add("Unlock System and Reset Header Link", UNLOCK_AND_RESET_HEADERLINK_ACTION);
            tokenActionListBox.add("Delete Tokens", DELETE_ACTION);
            return tokenActionListBox;
        }

    }

    public String getHeaderLink() {
        return headerLink;
    }

	public String getNotice() {
		return notice;
	}
}
