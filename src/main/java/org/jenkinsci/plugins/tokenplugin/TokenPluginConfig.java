package org.jenkinsci.plugins.tokenplugin;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.util.FormValidation;

import javax.annotation.CheckForNull;

import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import jenkins.model.GlobalConfiguration;

@Extension
public class TokenPluginConfig extends GlobalConfiguration {

	public TokenPluginConfig() {
		// load configfile
		load();

		// create Configfile if not exists
		if (linkToUtilityDeleteJob == null
				&& linkToUtilitySetheaderlinkJob == null
				&& linkToUtilityUnlockSystemJob == null
				&& linkToUtilityLockSystemJob == null) {
			linkToUtilityDeleteJob = "deletejob not set in config";
			linkToUtilitySetheaderlinkJob = "setheaderjob not set in config";
			linkToUtilityUnlockSystemJob = "unlockjob not set in config";
			linkToUtilityLockSystemJob = "lockjob not set in config";
			save();
		}
	}

	@Override
	public boolean configure(StaplerRequest req, JSONObject json)
			throws Descriptor.FormException {
		req.bindJSON(this, json);
		save();
		return true;
	}

	@CheckForNull
	private String linkToUtilityDeleteJob;
	@CheckForNull
	private String linkToUtilitySetheaderlinkJob;
	@CheckForNull
	private String linkToUtilityUnlockSystemJob;
	@CheckForNull
	private String linkToUtilityLockSystemJob;

	public String getLinkToUtilityDeleteJob() {
		return linkToUtilityDeleteJob;
	}

	public void setLinkToUtilityDeleteJob(
			@CheckForNull String linkToUtilityDeleteJob) {
		this.linkToUtilityDeleteJob = linkToUtilityDeleteJob;
	}

	public String getLinkToUtilitySetheaderlinkJob() {
		return linkToUtilitySetheaderlinkJob;
	}

	public void setLinkToUtilitySetheaderlinkJob(
			@CheckForNull String linkToUtilitySetheaderlinkJob) {
		this.linkToUtilitySetheaderlinkJob = linkToUtilitySetheaderlinkJob;
	}

	public String getLinkToUtilityUnlockSystemJob() {
		return linkToUtilityUnlockSystemJob;
	}

	public void setLinkToUtilityUnlockSystemJob(
			@CheckForNull String linkToUtilityUnlockSystemJob) {
		this.linkToUtilityUnlockSystemJob = linkToUtilityUnlockSystemJob;
	}

	public String getLinkToUtilityLockSystemJob() {
		return linkToUtilityLockSystemJob;
	}

	public void setLinkToUtilityLockSystemJob(
			@CheckForNull String linkToUtilityLockSystemJob) {
		this.linkToUtilityLockSystemJob = linkToUtilityLockSystemJob;
	}

	public FormValidation doCheckLinkToUtilityUnlockSystemJob(
			@QueryParameter String linkToUtilityUnlockSystemJob) {
		return FormValidation.ok();
	}
	public FormValidation doCheckLinkToUtilityLockSystemJob(
			@QueryParameter String linkToUtilityLockSystemJob) {
		return FormValidation.ok();
	}
	public FormValidation doCheckLinkToUtilitySetheaderlinkJob(
			@QueryParameter String linkToUtilitySetheaderlinkJob) {
		return FormValidation.ok();
	}
	public FormValidation doCheckLinkToUtilityDeleteJob(
			@QueryParameter String linkToUtilityDeleteJob) {
		return FormValidation.ok();
	}
}
