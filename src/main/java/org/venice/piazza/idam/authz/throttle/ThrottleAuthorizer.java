/**
 * Copyright 2016, RadiantBlue Technologies, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package org.venice.piazza.idam.authz.throttle;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.venice.piazza.idam.authz.Authorizer;
import org.venice.piazza.idam.data.DatabaseAccessor;

import model.logger.Severity;
import model.response.AuthResponse;
import model.security.authz.AuthorizationCheck;
import model.security.authz.Permission;
import util.PiazzaLogger;

/**
 * Authorizer that determines if a specified user action will be prevented due to excessive use of that action
 * (throttling). This will use Throttle information in the DB instance to determine if a user should be throttled or
 * not.
 * 
 * @author Patrick.Doody
 *
 */
@Component
public class ThrottleAuthorizer implements Authorizer {
	@Autowired
	private DatabaseAccessor accessor;
	@Autowired
	private PiazzaLogger pzLogger;
	@Value("${throttle.frequency.interval}")
	private Integer THROTTLE_FREQUENCY_INTERVAL;

	private static final List<String> THROTTLED_POST_ENDPOINTS = Arrays.asList("data", "job", "data/file", "deployment");
	private static final Logger LOGGER = LoggerFactory.getLogger(ThrottleAuthorizer.class);

	@Override
	public AuthResponse canUserPerformAction(AuthorizationCheck authorizationCheck) {
		// Check if the user is trying to perform a Piazza Job, which is subject to throttling
		Permission action = authorizationCheck.getAction();
		if (isJobThrottlable(action)) {
			// Subject to throttling. Perform a lookup in the Jobs table.
			Integer invocations = null;
			try {
				// Get the number of invocations for this user
				invocations = accessor.getInvocationsForUserThrottle(authorizationCheck.getUsername(),
						model.security.authz.Throttle.Component.JOB);
				// Determine if the number of invocations exceeds the limit
				if (isThrottleInvocationsExceeded(invocations, authorizationCheck.getUsername())) {
					String message = String.format("Number of Jobs for user %s has been exceeded (%s). Please try again tomorrow.",
							authorizationCheck.getUsername(), invocations);
					return new AuthResponse(false, message);
				} else {
					return new AuthResponse(true);
				}
			} catch (Exception exception) {
				String error = String.format(
						"Error getting number of invocations for Auth Check %s. %s. Throttle authorization checks may not be functioning correctly.",
						authorizationCheck.toString(), exception.getMessage());
				LOGGER.error(error, exception);
				pzLogger.log(error, Severity.ERROR);
				// Currently, do not deny this request. The database is not properly working and we don't want to
				// blacklist everything if the database can't be reached.
			}
		}

		return new AuthResponse(true);
	}

	/**
	 * Determines if the number of invocations for a Job Component throttle exceeds the throttle limit.
	 * 
	 * @param invocations
	 *            The number of current invocations
	 * @param username
	 *            The username
	 * @return True if the throttle has been exceeded (denied!), false if not
	 */
	private boolean isThrottleInvocationsExceeded(Integer invocations, String username) {
		// TODO: Tie in some group management, roles, access, rules. Lots of stuff.
		// This will be handled by GeoAxis.
		if (invocations > 10000) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Determines if the Action is a Job subject to throttling or not, based on the endpoint the user is trying to
	 * access.
	 * 
	 * @param action
	 *            The action the user wishes to perform
	 * @return True if the action is subject to throttling, false if not
	 */
	private boolean isJobThrottlable(Permission action) {
		if ((THROTTLED_POST_ENDPOINTS.contains(action.getUri())) && (action.getRequestMethod().equals(HttpMethod.POST.toString()))) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Every interval, clear the existing throttles from the system. Currently will run every day at 3am.
	 */
	@Scheduled(cron = "0 0 3 * * ?")
	private void clearThrottles() {
		accessor.clearThrottles();
	}
}
