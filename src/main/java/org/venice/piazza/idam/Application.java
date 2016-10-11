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
package org.venice.piazza.idam;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.Arrays;

import javax.net.ssl.SSLContext;

import java.security.cert.CertificateException;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContexts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.access.channel.ChannelProcessingFilter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.venice.piazza.idam.authn.GxAuthenticator;
import org.venice.piazza.idam.authn.LDAPAuthenticator;
import org.venice.piazza.idam.authn.PiazzaAuthenticator;

@SpringBootApplication
@ComponentScan({ "org.venice.piazza.idam, util" })
public class Application extends SpringBootServletInitializer {

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
		return builder.sources(Application.class);
	}

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args); // NOSONAR
	}

	@Configuration
	protected static class ApplicationSecurity extends WebSecurityConfigurerAdapter {

		@Override
		protected void configure(HttpSecurity http) throws Exception {
			http.addFilterBefore(corsFilter(), ChannelProcessingFilter.class).csrf().disable();
		}

		@Bean
		public CorsFilter corsFilter() {
			UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
			CorsConfiguration config = new CorsConfiguration();
			config.addAllowedOrigin("*");
			config.addAllowedHeader("*");
			config.addAllowedMethod("OPTIONS");
			config.addAllowedMethod("GET");
			config.addAllowedMethod("PUT");
			config.addAllowedMethod("POST");
			config.addAllowedMethod("DELETE");
			source.registerCorsConfiguration("/**", config);
			return new CorsFilter(source);
		}
	}

	@Configuration
	@Profile({ "ldap" })
	protected static class LDAPConfig {

		@Bean
		public PiazzaAuthenticator piazzaAuthenticator() {
			return new LDAPAuthenticator();
		}
	}

	@Configuration
	@Profile({ "geoaxis" })
	protected static class GxConfig {

		@Value("${http.max.total}")
		private int httpMaxTotal;

		@Value("${http.max.route}")
		private int httpMaxRoute;

		@Value("${vcap.services.geoaxis.credential.keystore.path}")
		private String keystorePath;

		@Value("${vcap.services.geoaxis.credential.keystore.passphrase}")
		private String keystorePassphrase;

		@Value("${vcap.services.geoaxis.credential.piazza.key.passphrase}")
		private String piazzaKeyPassphrase;
		
		@Bean
		public PiazzaAuthenticator piazzaAuthenticator() {
			return new GxAuthenticator();
		}

		@Bean
		public RestTemplate restTemplate() throws KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException {
			SSLContext sslContext = SSLContexts.custom().loadKeyMaterial(getStore(), piazzaKeyPassphrase.toCharArray())
					.loadTrustMaterial(getStore(), new TrustSelfSignedStrategy()).useProtocol("TLS").build();
			HttpClient httpClient = HttpClientBuilder.create().setMaxConnTotal(httpMaxTotal).setSSLContext(sslContext).setMaxConnPerRoute(httpMaxRoute).build();

			RestTemplate restTemplate = new RestTemplate();
			restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory(httpClient));
			restTemplate.setMessageConverters(Arrays.asList(new MappingJackson2HttpMessageConverter())); // Why is this required?
			return restTemplate;
		}

		protected KeyStore getStore() throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
			final KeyStore store = KeyStore.getInstance(KeyStore.getDefaultType());
			InputStream inputStream = new FileInputStream(keystorePath);
			try {
				store.load(inputStream, keystorePassphrase.toCharArray());
			} finally {
				inputStream.close();
			}

			return store;
		}
	}
}