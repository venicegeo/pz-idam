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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GxOAuthResponse {
	
	private String administrativeOrganizationCode;
	private String dn;
	private String id;
	private String personaUID;
	private String commonname;
	private String email;
	private String firstname;
	private String lastname;
	private String login;
	private String mail;
	private String memberof;
	private String personatypecode;
	private String serviceOrAgency;
	private String uid;
	private String uri;
	private String username;

	public String getAdministrativeOrganizationCode() {
		return administrativeOrganizationCode;
	}

	@JsonSetter("AdministrativeOrganizationCode")
	public void setAdministrativeOrganizationCode(String administrativeOrganizationCode) {
		this.administrativeOrganizationCode = administrativeOrganizationCode;
	}

	public String getDn() {
		return dn;
	}

	@JsonSetter("DN")
	public void setDn(String dn) {
		this.dn = dn;
	}

	public String getId() {
		return id;
	}

	@JsonSetter("ID")
	public void setId(String id) {
		this.id = id;
	}

	public String getPersonaUID() {
		return personaUID;
	}

	@JsonSetter("PersonaUID")
	public void setPersonaUID(String personaUID) {
		this.personaUID = personaUID;
	}

	public String getCommonname() {
		return commonname;
	}

	public void setCommonname(String commonname) {
		this.commonname = commonname;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getFirstname() {
		return firstname;
	}

	public void setFirstname(String firstname) {
		this.firstname = firstname;
	}

	public String getLastname() {
		return lastname;
	}

	public void setLastname(String lastname) {
		this.lastname = lastname;
	}

	public String getLogin() {
		return login;
	}

	public void setLogin(String login) {
		this.login = login;
	}

	public String getMail() {
		return mail;
	}

	public void setMail(String mail) {
		this.mail = mail;
	}

	public String getMemberof() {
		return memberof;
	}

	public void setMemberof(String memberof) {
		this.memberof = memberof;
	}

	public String getPersonatypecode() {
		return personatypecode;
	}

	public void setPersonatypecode(String personatypecode) {
		this.personatypecode = personatypecode;
	}

	public String getServiceOrAgency() {
		return serviceOrAgency;
	}

	public void setServiceOrAgency(String serviceOrAgency) {
		this.serviceOrAgency = serviceOrAgency;
	}

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	@Override
	public String toString() {
		return "GxOAuthResponse{" +
				"administrativeOrganizationCode='" + administrativeOrganizationCode + '\'' +
				", dn='" + dn + '\'' +
				", id='" + id + '\'' +
				", personaUID='" + personaUID + '\'' +
				", commonname='" + commonname + '\'' +
				", email='" + email + '\'' +
				", firstname='" + firstname + '\'' +
				", lastname='" + lastname + '\'' +
				", login='" + login + '\'' +
				", mail='" + mail + '\'' +
				", memberof='" + memberof + '\'' +
				", personatypecode='" + personatypecode + '\'' +
				", serviceOrAgency='" + serviceOrAgency + '\'' +
				", uid='" + uid + '\'' +
				", uri='" + uri + '\'' +
				", username='" + username + '\'' +
				'}';
	}
}