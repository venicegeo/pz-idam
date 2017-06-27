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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.venice.piazza.idam.authz.Authorizer;
import org.venice.piazza.idam.authz.ProfileTemplateFactory;

import model.response.AuthResponse;
import model.security.authz.AuthorizationCheck;
import util.PiazzaLogger;

/**
 * Authorizer that determines if a specified user action will be prevented due to restricted access to a particular
 * endpoint in the Piazza API.
 * 
 * @author Patrick.Doody
 *
 */
@Component
public class EndpointAuthorizer implements Authorizer {
	@Autowired
	private PiazzaLogger pzLogger;
	@Autowired
	private ProfileTemplateFactory profileTemplateFactory;

	/**
	 * TODO: This is a placeholder. We will eventually delegate this call to GeoAxis. No need to use Piazza code for
	 * this, for now.
	 */
	@Override
	public AuthResponse canUserPerformAction(AuthorizationCheck authorizationCheck) {
		return new AuthResponse(true);
	}
}
