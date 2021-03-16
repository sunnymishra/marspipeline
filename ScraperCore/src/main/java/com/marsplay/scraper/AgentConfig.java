package com.marspipeline.scraper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.marspipeline.scraper.agents.Agent;
import com.marspipeline.scraper.lib.Constants;
import com.marspipeline.scraper.lib.Constants.Endsites;

@Configuration
public class AgentConfig {
	private static final Logger LOGGER = LoggerFactory
			.getLogger(AgentConfig.class);
	private Properties applicationProps = Constants.getApplicationProps();

	@Autowired
	@Qualifier("myntraAgent")
	private Agent myntraAgent;

	@Autowired
	@Qualifier("amazonAgent")
	private Agent amazonAgent;
	
	@Autowired
	@Qualifier("flipkartAgent")
	private Agent flipkartAgent;

	@Autowired
	@Qualifier("koovsAgent")
	private Agent koovsAgent;
	
	@Bean("agents")
	public List<Callable<String>> getAgents() {
		String activeAgentsStr = applicationProps.getProperty("scraper.agent.active");
		if(StringUtils.isEmpty(activeAgentsStr))
			return null;
		List<String> agents = Arrays.asList(activeAgentsStr.trim().split(","));
		List<Callable<String>> agentList = new ArrayList<Callable<String>>();
		List<Endsites> agentsAccepted=new ArrayList<Endsites>();
		for(String agentString: agents){
			Endsites agent=null;
			try {
				agent = Endsites.valueOf(agentString);
			} catch (IllegalArgumentException e) {
				LOGGER.error("#### ILLEGAL_ENUM_VALUE:{}",agentString,e.getMessage());
				continue;
			}
			
			switch(agent){
				case MYNTRA: agentList.add(myntraAgent);agentsAccepted.add(Endsites.MYNTRA);break;
				case AMAZON: agentList.add(amazonAgent);agentsAccepted.add(Endsites.AMAZON);break;
				case FLIPKART: agentList.add(flipkartAgent);agentsAccepted.add(Endsites.FLIPKART);break;
				case KOOVS: agentList.add(koovsAgent);agentsAccepted.add(Endsites.KOOVS);break;
			}
		}
		LOGGER.info("#### AGENTS_ACCEPTED:{}",agentsAccepted.toString());
		return agentList;
	}
}