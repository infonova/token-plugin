package org.jenkinsci.plugins.tokenplugin;

import hudson.Extension;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.model.Cause;
import hudson.model.Cause.UpstreamCause;
import hudson.model.Cause.UserIdCause;
import hudson.model.Hudson;
import hudson.model.Run;
import hudson.triggers.TimerTrigger.TimerTriggerCause;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import jenkins.model.GlobalConfiguration;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;
import org.jenkinsci.plugins.tokenplugin.SystemStatusInformation.Status;
import org.springframework.beans.factory.config.PropertyPathFactoryBean;


@Extension
public class TokenManager extends GlobalConfiguration {

	private static final String UNLOCK_ACTION = "unlock";
	private static final String LOCK_ACTION = "lock";
	private static final String DELETE_ACTION = "delete";
	private static final String SET_HEADERLINK_ACTION = "setHeaderLink";
	private static final String LOCK_AND_SET_HEADERLINK_ACTION = "lockAndSetHeaderLink";
	private static final String UNLOCK_AND_RESET_HEADERLINK_ACTION = "unlockAndResetHeaderLink";
	private static final String TIMER_USERID = "timer";

	private Map<String, SystemStatusInformation> systems;

	public TokenManager() {
		load();

		if (systems == null) {
			systems = new TreeMap<String, SystemStatusInformation>();
		}
	}

	public static TokenManager getInstance() {
		return Hudson.getInstance().getDescriptorByType(TokenManager.class);
	}

	public boolean manageToken(AbstractBuild<?, ?> build,
			TaskListener listener, String systemName, String headerLink,
			String tokenAction, boolean forceAction, String notice)
			throws IOException, InterruptedException {

		final PrintStream logger = listener.getLogger();
		boolean continueBuild = true;

		String expandedSystemName;
		String expandedHeaderLink;
		String expandedNotice;

		try {
			expandedSystemName = TokenMacro.expandAll(build, listener,
					systemName);
			expandedHeaderLink = TokenMacro.expandAll(build, listener,
					headerLink);
			expandedNotice = TokenMacro.expandAll(build, listener, notice);
		} catch (MacroEvaluationException e) {
			logger.println(e.getMessage());
			return false;
		}

		final Run<?, ?> rootBuild = getRootBuild(build);

		if (StringUtils.isBlank(expandedSystemName)) {
			logger.println("Please enter a name for the system");
			continueBuild = false;
		} else if (buildIsCausedBy(TimerTriggerCause.class, rootBuild)
				&& !forceAction) {
			logger.println("Was triggered by timer and job is not configured to force action. Unlocking system '"
					+ expandedSystemName + "' by default.");
			continueBuild = unlockSystem(expandedSystemName, "system", logger,
					expandedNotice);
		} else {
			final UserIdCause userIdCause = rootBuild
					.getCause(UserIdCause.class);
			String userId = null;

			if (userIdCause != null) {
				// default if blank in case anonymous user triggered job
				userId = StringUtils.defaultIfBlank(userIdCause.getUserId(),
						userIdCause.getUserName());
			} else if (buildIsCausedBy(TimerTriggerCause.class, rootBuild)
					&& forceAction) {
				userId = TIMER_USERID;
				logger.println("Was triggered by timer and job is configured to force action.");
			}

			if (!StringUtils.isEmpty(userId)) {
				if (StringUtils.equals(UNLOCK_ACTION, tokenAction)) {
					continueBuild = unlockSystem(expandedSystemName, userId,
							logger, expandedNotice);
				} else if (StringUtils.equals(LOCK_ACTION, tokenAction)) {
					continueBuild = lockSystem(expandedSystemName, userId,
							forceAction, logger, expandedNotice);
				} else if (StringUtils.equals(DELETE_ACTION, tokenAction)) {
					continueBuild = deleteSystemsWithRegEx(expandedNotice, logger);
				} else if (StringUtils.equals(SET_HEADERLINK_ACTION, tokenAction)) {
					setHeaderLink(expandedSystemName, expandedHeaderLink,
							logger);
				} else if (StringUtils.equals(LOCK_AND_SET_HEADERLINK_ACTION,
						tokenAction)) {
					continueBuild = lockSystem(expandedSystemName, userId,
							forceAction, logger, expandedNotice);
					setHeaderLink(expandedSystemName, expandedHeaderLink,
							logger);
				} else if (StringUtils.equals(
						UNLOCK_AND_RESET_HEADERLINK_ACTION, tokenAction)) {
					continueBuild = unlockSystem(expandedSystemName, userId,
							logger, expandedNotice);
					setHeaderLink(expandedSystemName, null, logger);
				} else {
					logger.println("unknown action");
					continueBuild = false;
				}
			} else {
				logger.println("(Upstream) Build was not caused by Timer or User. Build-Cause(s): "
						+ rootBuild.getCauses());
				continueBuild = false;
			}
		}

		return continueBuild;
	}

	private boolean lockSystem(final String systemName, final String userId,
			final boolean forceAction, final PrintStream logger,
			final String notice) {
		logger.println("User '" + userId + "' is trying to lock system '"
				+ systemName + "'");

		final SystemStatusInformation systemInformation = systems
				.get(systemName);

		if (isCanAndShouldLockSystem(userId, forceAction, systemInformation)) {
			updateLockStatus(systemName, systemInformation, userId,
					Status.LOCKED, notice);
			logger.println("System '" + systemName + "' locked");
		} else {
			final String lockeeUserId = systemInformation.getUserId();
			logger.println("System '" + systemName
					+ "' was already locked by '" + lockeeUserId + "'");

			if (!StringUtils.equals(userId, lockeeUserId)) {
				logger.println("Lockee '" + lockeeUserId
						+ "' is not current user '" + userId
						+ "'. Aborting build...");
				return false;
			}
		}
		return true;
	}

	private void setHeaderLink(final String systemName,
			final String headerLink, final PrintStream logger) {
		SystemStatusInformation systemStatusInformation = systems
				.get(systemName);
		logger.println(String.format(
				"setting headerLink for system '%s' from '%s' to '%s' ",
				systemName, systemStatusInformation.getHeaderLink(), headerLink));
		systemStatusInformation.setHeaderLink(headerLink);
		// systems.put(systemName, systemStatusInformation);
		save();
	}

	private Run<?, ?> getRootBuild(Run<?, ?> build) {
		Run<?, ?> result;
		if (buildIsCausedBy(UpstreamCause.class, build)) {
			final UpstreamCause upstreamCause = build
					.getCause(UpstreamCause.class);
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

	private void updateLockStatus(String systemName,
			SystemStatusInformation systemInformation, String userId,
			Status status, String notice) {
		SystemStatusInformation updatedSystemInfo;
		if (systemInformation != null) {
			updatedSystemInfo = new SystemStatusInformation(systemInformation);
			updatedSystemInfo.setUserId(userId);
			updatedSystemInfo.setStatus(status);
			updatedSystemInfo.setNotice(notice);
		} else {
			updatedSystemInfo = new SystemStatusInformation(userId, status,
					notice);
		}

		systems.put(systemName, updatedSystemInfo);
		save();
	}

	private boolean systemUnlockedOrNew(
			final SystemStatusInformation systemInformation) {
		return (systemInformation == null)
				|| Status.UNLOCKED.equals(systemInformation.getStatus());
	}

	private boolean isCanAndShouldLockSystem(final String userId,
			final boolean forceAction,
			SystemStatusInformation systemStatusInformation) {
		return systemUnlockedOrNew(systemStatusInformation)
				|| (TIMER_USERID.equals(userId) && forceAction);
	}

	private boolean unlockSystem(final String systemName, final String userId,
			final PrintStream logger, final String notice) {
		logger.println("User '" + userId + "' is trying to unlock system '"
				+ systemName + "'");

		final SystemStatusInformation systemInformation = systems
				.get(systemName);

		if (systemUnlockedOrNew(systemInformation)) {
			logger.println("System '" + systemName + "' is not locked");
		} else {
			systemInformation.setNotice(null);
			updateLockStatus(systemName, systemInformation, userId,
					Status.UNLOCKED, notice);
			logger.println("System '" + systemName + "' was unlocked by user '"
					+ userId + "'");
		}

		// in case the unlock can fail in the future
		return true;
	}

	private boolean deleteSystemsWithRegEx(final String regEx,
			final PrintStream logger) {
		if (regEx == null) {
			logger.println(String.format("ERROR: regEx is null!"));
			return false;
		}

		List<String> keysList = new LinkedList<String>();
		for (String sys : systems.keySet()) {
			if (sys.matches(regEx)) {
				keysList.add(sys);
			}
		}

		for (String key : keysList) {
			systems.remove(key);
		}
		save();

		return true;
	}

	private boolean buildIsCausedBy(Class<? extends Cause> clazz,
			Run<?, ?> build) {
		final List<Cause> causes = build.getCauses();

		for (final Cause cause : causes) {
			if (StringUtils.equals(cause.getClass().getName(), clazz.getName())) {
				return true;
			}
		}
		return false;
	}

	public Map<String, SystemStatusInformation> getSystems() {
		return Collections.unmodifiableMap(systems);
	}

	@Override
	public String getDisplayName() {
		return "TokenManager";
	}
}
