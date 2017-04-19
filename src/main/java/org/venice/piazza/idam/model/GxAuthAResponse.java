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
package org.venice.piazza.idam.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GxAuthAResponse {
	
	// Admin Code and Duty Code for Customer Users
	private List<String> serviceoragency;
	
	// Admin Code for Non-Customer Users
	private List<String> gxadministrativeorganizationcode;
	
	// Duty Code for Non-Customer Users
	private List<String> gxdutydodoccupationcode;
	
	// Country for both Customer and Non-Customer Users
	private List<String> nationalityextended;

	public List<String> getServiceoragency() {
		return serviceoragency;
	}

	public void setServiceoragency(final List<String> serviceoragency) {
		this.serviceoragency = serviceoragency;
	}

	public List<String> getGxadministrativeorganizationcode() {
		return gxadministrativeorganizationcode;
	}

	public void setGxadministrativeorganizationcode(final List<String> gxadministrativeorganizationcode) {
		this.gxadministrativeorganizationcode = gxadministrativeorganizationcode;
	}

	public List<String> getGxdutydodoccupationcode() {
		return gxdutydodoccupationcode;
	}

	public void setGxdutydodoccupationcode(final List<String> gxdutydodoccupationcode) {
		this.gxdutydodoccupationcode = gxdutydodoccupationcode;
	}

	public List<String> getNationalityextended() {
		return nationalityextended;
	}

	public void setNationalityextended(final List<String> country) {
		this.nationalityextended = country;
	}
}