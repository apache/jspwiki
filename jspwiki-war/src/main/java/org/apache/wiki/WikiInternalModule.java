package org.apache.wiki;

import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.wiki.api.exceptions.WikiException;
import org.apache.wiki.modules.InternalModule;

public class WikiInternalModule implements InternalModule {

	protected Logger log = Logger.getLogger( getClass() );
	
	protected boolean initialized = false;
	protected WikiEngine m_engine;
	protected Properties m_properties;
	
	@Override
	public void initialize(WikiEngine engine, Properties props) throws WikiException {
		m_engine = engine;
		m_properties = props;
		initialized = true;
	}

	@Override
	public boolean isInitialized() {
		return initialized;
	}

	
}
