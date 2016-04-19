package org.venice.piazza.security.data;

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
