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
package org.venice.piazza.idam.authz.endpoint;

import org.springframework.stereotype.Component;
import org.venice.piazza.idam.authz.Authorizer;
import org.venice.piazza.idam.model.AuthResponse;
import org.venice.piazza.idam.model.authz.AuthorizationCheck;

/**
 * Authorizer that determines if a specified user action will be prevented due to restricted access to a particular
 * endpoint in the Piazza API.
 * 
 * @author Patrick.Doody
 *
 */
@Component
public class EndpointAuthorizer implements Authorizer {

	@Override
	public AuthResponse canUserPerformAction(AuthorizationCheck authorizationCheck) {
		// TODO
		return null;
	}
}
