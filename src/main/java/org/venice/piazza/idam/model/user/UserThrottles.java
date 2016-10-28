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
package org.venice.piazza.idam.model.user;

import java.util.HashMap;
import java.util.Map;

import model.security.authz.Throttle;

/**
 * Throttle metadata for a user that tracks that users activity with Piazza jobs. Will keep a record count of all Piazza
 * Jobs a user has performed in the last period of activity. This user is collected in order to determine if a user
 * should eventually be throttled or not due to excessive activity.
 * 
 * @author Patrick.Doody
 *
 */
public class UserThrottles {
	/**
	 * Associated a Throttle type with the current number of occurrences of that instance for the current time period
	 */
	public Map<Throttle, Integer> throttles = new HashMap<Throttle, Integer>();
}
