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
package org.venice.piazza.security.data;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

/**
 * Class for abstracting the File handling details from the ServiceController
 * 
 * @author Russell.Orf
 */
@Component
public class FileAccessor {

	@Autowired
	private ResourceLoader resourceLoader;

	@Value("${pz.security.fileurl}")
	private String FILE;

	private static Path PATH;

	public Map<String, String> getUsersAndRoles() throws IOException {
		return Files.lines(getRoleFilePath()).filter(s -> s.matches("^\\w+:\\w+:[\\w+,-]*(\\054\\w+[\\w+,-]*)*$"))
				.collect(Collectors.toMap(k -> k.split(":")[0], v -> (v.split(":").length > 2 ? v.split(":")[2] : "")));
	}
	
	public Map<String, String> getUsersAndCredentials() throws IOException {
		return Files.lines(getRoleFilePath()).filter(s -> s.matches("^\\w+:\\w+:[\\w+,-]*(\\054\\w+[\\w+,-]*)*$"))
				.collect(Collectors.toMap(k -> k.split(":")[0], v -> (v.split(":").length > 2 ? v.split(":")[1] : "")));
	}	

	public Map<String, String> getUsersAndCredentialsAndRoles() throws IOException {
		return Files.lines(getRoleFilePath()).filter(s -> s.matches("^\\w+:\\w+:[\\w+,-]*(\\054\\w+[\\w+,-]*)*$"))
				.collect(Collectors.toMap(k -> k.split(":")[0], v -> (v.split(":").length > 2 ? v.split(":")[1] + ":" + v.split(":")[2] : v.split(":")[1] + ":")));
	}
	
	public Set<String> getUsers() throws IOException {
		return getUsersAndRoles().keySet();
	}

	public String getCredentialForUser(String userid) throws IOException {
		if (userExists(userid)) {
			return getUsersAndCredentials().get(userid.toLowerCase());
		}

		return new String();
	}
	
	public List<String> getRolesForUser(String userid) throws IOException {
		if (userExists(userid)) {
			return new ArrayList<String>(Arrays.asList(getUsersAndRoles().get(userid.toLowerCase()).split(",")));
		}

		return new ArrayList<String>();
	}
	
	public boolean userExists(String userid) throws IOException {
		return getUsers().contains(userid.toLowerCase());
	}

	public boolean roleExists(String userid, String role) throws IOException {
		for (String existingRole : getRolesForUser(userid)) {
			if (existingRole.equalsIgnoreCase(role)) {
				return true;
			}
		}

		return false;
	}

	public void addUsers(Map<String, String> usersToAdd) throws IOException {
		writeToFile(usersToAdd, false);
	}

	public void updateUserRoles(String userid, List<String> roles) throws IOException {
		Map<String, String> mapFromFile = getUsersAndCredentialsAndRoles();
		mapFromFile.put(userid.toLowerCase(), getCredentialForUser(userid) + ":" + formatRoles(roles));
		writeToFile(mapFromFile, true);
	}

	public void removeRole(String userid, String role) throws IOException {
		List<String> roles = getRolesForUser(userid);
		roles.remove(role);

		Map<String, String> mapFromFile = getUsersAndCredentialsAndRoles();
		mapFromFile.put(userid.toLowerCase(), getCredentialForUser(userid) + ":" + formatRoles(roles));
		writeToFile(mapFromFile, true);
	}

	public void removeAllRoles(String userid) throws IOException {
		Map<String, String> mapFromFile = getUsersAndCredentialsAndRoles();
		mapFromFile.put(userid.toLowerCase(), getCredentialForUser(userid) + ":");
		writeToFile(mapFromFile, true);
	}

	public void removeUser(String userid) throws IOException {
		Map<String, String> mapFromFile = getUsersAndCredentialsAndRoles();
		mapFromFile.remove(userid);
		writeToFile(mapFromFile, true);
	}

	public Stats getStats() throws IOException {
		return new Stats(getUsers().size(), getNumRoles(), getNumUsersWithNoRoles(), getNumUsersWithAllRoles());
	}

	private synchronized void writeToFile(Map<String, String> lines, Boolean wholesaleReplace) throws IOException {
		boolean first = true;
		for (Map.Entry<String, String> entry : lines.entrySet()) {
			byte[] line = (entry.getKey() + ":" + entry.getValue()).getBytes();
			if (first && wholesaleReplace) {
				Files.write(getRoleFilePath(), line, StandardOpenOption.TRUNCATE_EXISTING);
				first = false;
			} else {
				BufferedWriter bw = Files.newBufferedWriter(getRoleFilePath(), StandardOpenOption.APPEND);
				bw.newLine();
				bw.close();
				Files.write(getRoleFilePath(), line, StandardOpenOption.APPEND);
			}
		}
	}

	private String formatRoles(List<String> roles) {
		StringBuilder sb = new StringBuilder();
		List<String> addedRoles = new ArrayList<String>();
		for (int i = 0; i < roles.size(); i++) {
			String role = roles.get(i).toLowerCase();
			if (!addedRoles.contains(role)) {
				sb.append(role);
				addedRoles.add(role);
				if (i < roles.size() - 1) {
					sb.append(",");
				}
			}
		}
		return sb.toString();
	}

	public int getNumRoles() throws IOException {
		Set<String> roles = new HashSet<String>();
		for (String user : getUsers()) {
			roles.addAll(getRolesForUser(user));
		}
		return roles.size();
	}

	public int getNumUsersWithNoRoles() throws IOException {
		Set<String> usersNoRoles = new HashSet<String>();
		for (String user : getUsers()) {
			if (getRolesForUser(user).size() == 1 && getRolesForUser(user).get(0).length() == 0) {
				usersNoRoles.add(user);
			}
		}
		return usersNoRoles.size();
	}

	public int getNumUsersWithAllRoles() throws IOException {
		Set<String> allRoles = new HashSet<String>();
		for (String user : getUsers()) {
			allRoles.addAll(getRolesForUser(user));
		}
		Set<String> usersWithAllRoles = new HashSet<String>();
		for (String user : getUsers()) {
			if (getRolesForUser(user).containsAll(allRoles)) {
				usersWithAllRoles.add(user);
			}
		}
		return usersWithAllRoles.size();
	}

	private Path getRoleFilePath() throws IOException {
		return (PATH != null ? PATH : Paths.get(resourceLoader.getResource(FILE).getFile().getPath()));
	}
}