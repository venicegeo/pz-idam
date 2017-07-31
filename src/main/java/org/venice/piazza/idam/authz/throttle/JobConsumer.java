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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PostConstruct;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.venice.piazza.idam.data.DatabaseAccessor;

import com.fasterxml.jackson.databind.ObjectMapper;

import messaging.job.KafkaClientFactory;
import model.job.Job;
import model.job.type.AbortJob;
import model.job.type.AccessJob;
import model.job.type.ExecuteServiceJob;
import model.job.type.IngestJob;
import model.job.type.RepeatJob;
import model.logger.Severity;
import util.PiazzaLogger;

/**
 * Kafka Consumer class that listens for all Jobs as they are created throughout Piazza, and records the username and
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
	@Value("${vcap.services.pz-kafka.credentials.host}")
	private String kafkaHosts;
	@Value("#{'${kafka.group}' + '-' + '${SPACE}'}")
	private String kafkaGroup;

	List<String> topics = new ArrayList<>();
	private ObjectMapper mapper = new ObjectMapper();
	private final AtomicBoolean closed = new AtomicBoolean(false);
	private static final Logger LOGGER = LoggerFactory.getLogger(JobConsumer.class);

	/**
	 * Initializes consumer listeners for the thread pool workers.
	 */
	@PostConstruct
	public void initialize() {
		// Get all the Job Type topics that Piazza can fire. This consumer will record each of those actions.
		List<Class<?>> jobTypes = Arrays.asList(AbortJob.class, IngestJob.class, AccessJob.class, ExecuteServiceJob.class, RepeatJob.class);
		for (Class<?> jobType : jobTypes) {
			topics.add(String.format("%s-%s", jobType.getSimpleName(), space));
		}

		// Start polling for Kafka Jobs on the Group Consumer.
		// Occurs on a separate Thread to not block Spring.
		new Thread(() -> pollForJobs()).start();
	}

	/**
	 * Opens up a Kafka Consumer to poll for all Jobs that are being processed in the system. This information will be
	 * recorded in the Throttle table.
	 */
	public void pollForJobs() {

		Consumer<String, String> consumer = null;

		try {
			// Create the General Group Consumer
			consumer = KafkaClientFactory.getConsumer(kafkaHosts, kafkaGroup);
			consumer.subscribe(topics);

			// Poll
			while (!closed.get()) {
				ConsumerRecords<String, String> consumerRecords = consumer.poll(1000);
				// Handle new Messages on this topic.
				handleNewMessage(consumerRecords);
			}
		} catch (WakeupException exception) {
			String error = String.format("Polling Thread forcefully closed: %s", exception.getMessage());
			LOGGER.error(error, exception);
			pzLogger.log(error, Severity.ERROR);
		} finally {
			if (consumer != null) {
				consumer.close();
			}
		}
	}

	private void handleNewMessage(final ConsumerRecords<String, String> consumerRecords) {
		for (ConsumerRecord<String, String> consumerRecord : consumerRecords) {
			// Record the Job Type and the user requesting that Job
			try {
				Job job = mapper.readValue(consumerRecord.value(), Job.class);
				processThrottle(job);
				LOGGER.info("Throttle Processed");
			} catch (IOException exception) {
				String error = String.format(
						"Error Deserializing Job Request Message for Job ID %s : %s. Could not record this Job to Throttle table.",
						consumerRecord.key(), exception.getMessage());
				LOGGER.error(error, exception);
				pzLogger.log(error, Severity.ERROR);
			}
		}
	}

	/**
	 * Stops all polling
	 */
	public void stopPolling() {
		this.closed.set(true);
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
