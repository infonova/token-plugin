package org.jenkinsci.plugins.tokenplugin;

import hudson.model.Descriptor;

import javax.annotation.CheckForNull;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.StaplerRequest;

import jenkins.model.GlobalConfiguration;

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
}
