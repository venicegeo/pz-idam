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

import java.util.HashMap;
import java.util.Map;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.venice.piazza.idam.model.user.UserThrottles;

/**
 * In-memory throttle table that live lookups will be performed against - to prevent excessive DB Reads/writes as Piazza
 * Jobs pass through the system. Frequently will update the Mongo persistence so as to keep the table data persisted.
 * 
 * @author Patrick.Doody
 *
 */
@Component
public class MemoryThrottleTable {
	private Map<String, UserThrottles> throttles = new HashMap<String, UserThrottles>();

	/**
	 * Checks if the user is able to perform the specified action
	 * 
	 * @param username
	 *            The username
	 * @param action
	 *            The action describing the Piazza Job
	 * @return True if the user is throttled and the action should be denied.
	 */
	public boolean isUserThrottled(String username, Object action) {
		return false;
	}

	/**
	 * Updates the MongoDB instance with the current throttle information
	 */
	public void updatePersistence() {

	}

	/**
	 * Removes all stale MongoDB persistence information (older than the last period of activity) from the persisted
	 * throttle table - as this information is no longer relevant in determining throttle status. This will run once per
	 * day.
	 */
	@Scheduled(cron = "0 0 3 * * ?")
	public void clearStalePersistenceData() {

	}
}
