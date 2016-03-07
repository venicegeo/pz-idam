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
package security.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SecurityController {

    @Autowired
    private ResourceLoader resourceLoader;
    
	@RequestMapping(value = "/roles/{userid}", method = RequestMethod.GET, produces="application/json")
	@ResponseBody
	public List<String> roles(@PathVariable(value = "userid") String userid) {
		try {
			return getRoles(userid);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	private List<String> getRoles(String userid) throws IOException  {

		Map<String,String> mapFromFile = 
			Files.lines( Paths.get(resourceLoader.getResource("classpath:roles.txt").getFile().getPath()) )
		    .filter(s -> s.matches("^\\w+:\\w+[\\w+,-]*(\\054\\w+[\\w+,-]*)*$"))
		    .collect(Collectors.toMap(k -> k.split(":")[0], v -> v.split(":")[1]));
		
		if( mapFromFile.containsKey(userid) && mapFromFile.get(userid).length() > 0) {
			return Arrays.asList(mapFromFile.get(userid).split(","));
		}
		
		return new ArrayList<String>();
	}
}