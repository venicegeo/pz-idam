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
package security.data;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
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

	private static final String FILE = "roles.txt";
	private static Path PATH;

	public Map<String, String> getUsersAndRoles() throws IOException {
		return Files.lines(getRoleFilePath()).filter(s -> s.matches("^\\w+:[\\w+,-]*(\\054\\w+[\\w+,-]*)*$"))
				.collect(Collectors.toMap(k -> k.split(":")[0], v -> (v.split(":").length > 1 ? v.split(":")[1] : "")));
	}

	public Set<String> getUsers() throws IOException {
		return getUsersAndRoles().keySet();
	}

	public List<String> getRolesForUser(String userid) throws IOException {
		if (userExists(userid)) {
			return new ArrayList<String>(Arrays.asList(getUsersAndRoles().get(userid.toLowerCase()).split(",")));
		}

		return new ArrayList<String>();
	}

	public boolean userExists(String userid) throws IOException {
		return getUsersAndRoles().containsKey(userid.toLowerCase());
	}

	public boolean roleExists(String userid, String role) throws IOException {
		for (String existingRole : getRolesForUser(userid)) {
			if (existingRole.equalsIgnoreCase(role)) {
				return true;
			}
		}

		return false;
	}

	public void addUsers(List<String> lines) throws IOException {
		for (String line : lines) {
			BufferedWriter bw = Files.newBufferedWriter(getRoleFilePath(), StandardOpenOption.APPEND);
			bw.newLine();
			bw.close();
			Files.write(getRoleFilePath(), line.toLowerCase().getBytes(), StandardOpenOption.APPEND);
		}
	}

	public void updateUserRoles(String userid, List<String> roles) throws IOException {
		Map<String, String> mapFromFile = getUsersAndRoles();
		mapFromFile.put(userid.toLowerCase(), formatRoles(roles));
		writeToFile(mapFromFile);
	}

	public void removeRole(String userid, String role) throws IOException {
		List<String> roles = getRolesForUser(userid);
		roles.remove(role);

		Map<String, String> mapFromFile = getUsersAndRoles();
		mapFromFile.put(userid.toLowerCase(), formatRoles(roles));
		writeToFile(mapFromFile);
	}

	public void removeAllRoles(String userid) throws IOException {
		Map<String, String> mapFromFile = getUsersAndRoles();
		mapFromFile.put(userid.toLowerCase(), "");
		writeToFile(mapFromFile);
	}

	public void removeUser(String userid) throws IOException {
		Map<String, String> mapFromFile = getUsersAndRoles();
		mapFromFile.remove(userid);
		writeToFile(mapFromFile);
	}

	private void writeToFile(Map<String, String> lines) throws IOException {
		boolean first = true;
		for (Map.Entry<String, String> entry : lines.entrySet()) {
			byte[] line = (entry.getKey() + ":" + entry.getValue()).getBytes();
			if (first) {
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

	private Path getRoleFilePath() throws IOException {
		return (PATH != null ? PATH : Paths.get(resourceLoader.getResource("classpath:" + FILE).getFile().getPath()));
	}
}