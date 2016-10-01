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

package org.venice.piazza.idam.data;

public class Stats {

	int numUsers;
	int numRoles;
	int numUsersWithNoRoles;
	int numUsersWithAllRoles;

	public Stats(int numUsers, int numRoles, int numUsersWithNoRoles, int numUsersWithAllRoles) {
		setNumUsers(numUsers);
		setNumRoles(numRoles);
		setNumUsersWithNoRoles(numUsersWithNoRoles);
		setNumUsersWithAllRoles(numUsersWithAllRoles);
	}

	public int getNumUsers() {
		return numUsers;
	}

	public void setNumUsers(int numUsers) {
		this.numUsers = numUsers;
	}

	public int getNumRoles() {
		return numRoles;
	}

	public void setNumRoles(int numRoles) {
		this.numRoles = numRoles;
	}

	public int getNumUsersWithNoRoles() {
		return numUsersWithNoRoles;
	}

	public void setNumUsersWithNoRoles(int numUsersWithNoRoles) {
		this.numUsersWithNoRoles = numUsersWithNoRoles;
	}

	public int getNumUsersWithAllRoles() {
		return numUsersWithAllRoles;
	}

	public void setNumUsersWithAllRoles(int numUsersWithAllRoles) {
		this.numUsersWithAllRoles = numUsersWithAllRoles;
	}
}
