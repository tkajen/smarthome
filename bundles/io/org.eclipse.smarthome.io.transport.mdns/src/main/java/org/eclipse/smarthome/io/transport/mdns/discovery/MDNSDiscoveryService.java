/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.io.transport.mdns.discovery;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.io.transport.mdns.MDNSClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a {@link DiscoveryService} implementation, which can find mDNS services in the network.
 * Support for further devices can be added by implementing and registering a {@link MDNSDiscoveryParticipant}.
 *  
 * @author Tobias Br�utigam - Initial contribution
 *
 */
public class MDNSDiscoveryService extends AbstractDiscoveryService implements ServiceListener {
	private final Logger logger = LoggerFactory.getLogger(MDNSDiscoveryService.class);
	
	private Set<MDNSDiscoveryParticipant> participants = new CopyOnWriteArraySet<>();
	
	private MDNSClient mdnsClient;

	public MDNSDiscoveryService() {
		super(5);
	}			

	
	public void setMDNSClient(MDNSClient mdnsClient) {
		this.mdnsClient = mdnsClient;
		startScan();
	}
	
	public void unsetMDNSClient(MDNSClient mdnsClient) {
		this.mdnsClient = null;
	}
	
	protected void activate() {
		
	}

	@Override
	protected void startScan() {
		logger.debug("mDNS discovery service started");
		initializeParticipants();
	}
		
	protected void initializeParticipants() {
		for(MDNSDiscoveryParticipant participant : participants) {
			mdnsClient.getClient().removeServiceListener(participant.getServiceType(), this);
			ServiceInfo[] services = mdnsClient.getClient().list(participant.getServiceType());
			logger.debug(services.length+" services found for "+participant.getServiceType());
			for(ServiceInfo service : services) {
				participant.createResult(service);
			}
			mdnsClient.getClient().addServiceListener(participant.getServiceType(), this);
		}
	}
	
	protected void addMdnsDiscoveryParticipant(MDNSDiscoveryParticipant participant) {
		this.participants.add(participant);
		logger.debug("adding mDNS listener to type: "+participant.getServiceType());
		if (mdnsClient!=null && mdnsClient.getClient() != null) {
			
			ServiceInfo[] services = mdnsClient.getClient().list(participant.getServiceType());
			
			logger.debug(services.length+" services found");
			for(ServiceInfo service : services) {
				participant.createResult(service);
			}
			mdnsClient.getClient().addServiceListener(participant.getServiceType(), this);
		}
	}

	protected void removeMdnsDiscoveryParticipant(MDNSDiscoveryParticipant participant) {
		this.participants.remove(participant);
		mdnsClient.getClient().removeServiceListener(participant.getServiceType(), this);
	}
	
	@Override
	public Set<ThingTypeUID> getSupportedThingTypes() {
		Set<ThingTypeUID> supportedThingTypes = new HashSet<>();
		for(MDNSDiscoveryParticipant participant : participants) {
			supportedThingTypes.addAll(participant.getSupportedThingTypeUIDs());
		}
		return supportedThingTypes;
	}

	@Override
	public void serviceAdded(ServiceEvent serviceEvent) {
		for(MDNSDiscoveryParticipant participant : participants) {
			try {
				DiscoveryResult result = participant.createResult(serviceEvent.getInfo());
				if(result!=null) {
					thingDiscovered(result);
				}
			} catch(Exception e) {
				logger.error("Participant '{}' threw an exception", participant.getClass().getName(), e);
			}
		}	   
	}

	@Override
	public void serviceRemoved(ServiceEvent serviceEvent) {
		for(MDNSDiscoveryParticipant participant : participants) {
			try {
				ThingUID thingUID = participant.getThingUID(serviceEvent.getInfo());
				if(thingUID!=null) {
					thingRemoved(thingUID);
				}
			} catch(Exception e) {
				logger.error("Participant '{}' threw an exception", participant.getClass().getName(), e);
			}
		}
	}

	@Override
	public void serviceResolved(ServiceEvent serviceEvent) {
		
	}

}
