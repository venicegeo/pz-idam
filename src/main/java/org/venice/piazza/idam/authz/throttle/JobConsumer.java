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
package org.venice.piazza.idam.authz.throttle;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.venice.piazza.idam.data.DatabaseAccessor;

import com.fasterxml.jackson.databind.ObjectMapper;

import messaging.job.JobMessageFactory;
import model.job.Job;
import model.logger.Severity;
import util.PiazzaLogger;

/**
 * Message Receiver class that listens for all Jobs as they are created throughout Piazza, and records the username and
 * job type information into the throttle table. This information is later used when determining when a user should be
 * throttled.
 * 
 * @author Patrick.Doody
 *
 */
@Component
public class JobConsumer {
	@Autowired
	private PiazzaLogger pzLogger;
	@Autowired
	private DatabaseAccessor accessor;
	@Value("${SPACE}")
	private String space;

	private ObjectMapper mapper = new ObjectMapper();
	private static final Logger LOGGER = LoggerFactory.getLogger(JobConsumer.class);

	/**
	 * Process a Job Message coming through the Message Bus and determine if it represents a throttable Job
	 * 
	 * @param jobTypeString
	 *            The serialized PiazzaJobType Model
	 */
	@RabbitListener(bindings = @QueueBinding(key = "IngestJob-${SPACE}", value = @Queue(value = "IDAMThrottles-${SPACE}", autoDelete = "false", durable = "true"), exchange = @Exchange(value = JobMessageFactory.PIAZZA_EXCHANGE_NAME, autoDelete = "false", durable = "true")))
	@RabbitListener(bindings = @QueueBinding(key = "AccessJob-${SPACE}", value = @Queue(value = "IDAMThrottles-${SPACE}", autoDelete = "false", durable = "true"), exchange = @Exchange(value = JobMessageFactory.PIAZZA_EXCHANGE_NAME, autoDelete = "false", durable = "true")))
	@RabbitListener(bindings = @QueueBinding(key = "ExecuteServiceJob-${SPACE}", value = @Queue(value = "IDAMThrottles-${SPACE}", autoDelete = "false", durable = "true"), exchange = @Exchange(value = JobMessageFactory.PIAZZA_EXCHANGE_NAME, autoDelete = "false", durable = "true")))
	public void processJobMessage(String jobTypeString) {
		try {
			// Deserialize the Job
			Job job = mapper.readValue(jobTypeString, Job.class);
			// Process it
			processThrottle(job);
			LOGGER.info("Throttle Processed");
		} catch (IOException exception) {
			String error = String.format("Error Reading Job Message from Queue %s", exception.getMessage());
			LOGGER.error(error, exception);
			pzLogger.log(error, Severity.ERROR);
		}
	}

	/**
	 * Adds information to the throttle table for an incoming job request.
	 * 
	 * @param jobRequest
	 *            The job request
	 */
	private void processThrottle(Job job) {
		String username = job.getCreatedBy();
		model.security.authz.Throttle.Component component = model.security.authz.Throttle.Component.JOB;
		// Update persistence
		try {
			accessor.incrementUserThrottles(username, component);
		} catch (Exception exception) {
			String error = String.format(
					"Error updating Throttle for Component %s for User %s : %s. The users Throttles could not be updated.", component,
					username, exception.getMessage());
			LOGGER.error(error, exception);
			pzLogger.log(error, Severity.ERROR);
		}
	}
}
