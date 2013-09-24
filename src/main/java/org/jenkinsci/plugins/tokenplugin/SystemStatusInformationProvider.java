package org.jenkinsci.plugins.tokenplugin;

import hudson.Extension;
import hudson.FilePath;
import hudson.Functions;
import hudson.PluginWrapper;
import hudson.model.RootAction;
import hudson.model.AbstractModelObject;
import hudson.model.Hudson;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.tokenplugin.SystemStatusInformation.Status;
import org.jenkinsci.plugins.tokenplugin.ManageTokenBuilder.DescriptorImpl;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/** Maybe this would be nice(r):
 *  https://wiki.jenkins-ci.org/display/JENKINS/Sectioned+View+Plugin#SectionedViewPlugin-Extensions */
@Extension
public class SystemStatusInformationProvider extends AbstractModelObject implements RootAction {

    // private static final Logger LOGGER =
    // Logger.getLogger(SystemInformationProvider.class.getName());

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    // public void doUpdateCache(StaplerRequest req, StaplerResponse rsp) {
    public void doGetSystemInformation(@QueryParameter(required = true) String systemName, StaplerRequest request,
                    StaplerResponse response) throws IOException {
        // DescriptorImpl descriptor = (DescriptorImpl)
        // Hudson.getInstance().getDescriptor(ManageTokenBuilder.class);
        DescriptorImpl descriptor = Hudson.getInstance().getDescriptorByType(DescriptorImpl.class);

        final Map<String, SystemStatusInformation> systems = descriptor.getSystems();
        final SystemStatusInformation systemInformation = systems.get(systemName);

        if (systemInformation != null) {
            final String dateString = DATE_FORMAT.format(systemInformation.getChangeDate());
            final String styleSheetPath = Functions.getResourcePath() + "/css/style.css";
            final String userId = systemInformation.getUserId();

            String statusImagePath = null;
            if (Status.LOCKED.equals(systemInformation.getStatus())) {
                final FilePath userContentImage = Hudson.getInstance().getRootPath()
                                .child("userContent/token-images/" + userId + ".gif");
                try {
                    if (userContentImage.exists() && !userContentImage.isDirectory()) {
                        statusImagePath = "/userContent/token-images/" + userId + ".gif";
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if (StringUtils.isBlank(statusImagePath)) {
                statusImagePath = Functions.getResourcePath() + //
                                "/plugin/token-plugin/images/" + systemInformation.getStatus() + ".gif";
            }

            // FilePath file =
            // Hudson.getInstance().getRootPath().child("plugins/token-plugin/status_template.html");
            PluginWrapper wrapper = Hudson.getInstance().getPluginManager().getPlugin("token-plugin");
            URL templateURL = new URL(wrapper.baseResourceURL + "status_template.html");

            final String htmlTemplate;
            InputStream inputStream = null;
            try {
                inputStream = templateURL.openStream();
                htmlTemplate = IOUtils.toString(inputStream);
            } finally {
                IOUtils.closeQuietly(inputStream);
            }

            String responseContent = StringUtils.replace(htmlTemplate, "$styleSheetPath", styleSheetPath);
            responseContent = StringUtils.replace(responseContent, "$statusImagePath", statusImagePath);
            responseContent = StringUtils.replace(responseContent, "$userId", userId);
            responseContent = StringUtils.replace(responseContent, "$changeDate", dateString);

            response.getWriter().write(responseContent);
        } else {
            response.getWriter().write("<p><b>System not found.</b></p><p>(Needs to be locked at least once.)</p>");
        }

        response.setContentType("text/html");
        response.setStatus(StaplerResponse.SC_OK);

    }

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public String getUrlName() {
        return "system-status-information";
    }

    @Override
    public String getSearchUrl() {
        // TODO Auto-generated method stub
        return null;
    }

}
