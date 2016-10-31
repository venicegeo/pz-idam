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

import java.util.Timer;
import java.util.TimerTask;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.venice.piazza.idam.authz.Authorizer;
import org.venice.piazza.idam.model.AuthResponse;
import org.venice.piazza.idam.model.authz.AuthorizationCheck;

/**
 * Authorizer that determines if a specified user action will be prevented due to excessive use of that action
 * (throttling). This will use Throttle information in the Mongo instance to determine if a user should be throttled or
 * not.
 * 
 * @author Patrick.Doody
 *
 */
@Component
public class ThrottleAuthorizer implements Authorizer {
	@Value("${throttle.frequency.interval}")
	private Integer THROTTLE_FREQUENCY_INTERVAL;

	@Override
	public AuthResponse canUserPerformAction(AuthorizationCheck authorizationCheck) {
		// TODO
		return null;
	}

	/**
	 * Reads the latest throttles from the MongoDB persistence at a regular interval. TODO: Not used right now
	 */
	@PostConstruct
	private void runUpdateSchedule() {
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				/// sync();
			}
		}, 0, THROTTLE_FREQUENCY_INTERVAL);
	}

}
