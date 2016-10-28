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
package org.venice.piazza.idam.model.authz;

import org.venice.piazza.idam.model.AuthResponse;

/**
 * An authorization exception.
 * 
 * @author Patrick.Doody
 *
 */
public class AuthorizationException extends Exception {
	private static final long serialVersionUID = 1L;
	private AuthResponse response;

	public AuthorizationException(String message) {
		super(message);
	}

	public AuthorizationException(String message, AuthResponse response) {
		this(message);
		this.response = response;
	}

	public AuthResponse getResponse() {
		return response;
	}
}
