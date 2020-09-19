package uk.co.furniss.draw.dom;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.namespace.NamespaceContext;

public class NamespaceContextImpl implements NamespaceContext {

	private final Map<String, String> prefixes = new HashMap<>();
	
	public NamespaceContextImpl() {
		// start with nothing
	}
	
	public void addPrefix(String prefix, String uri) {
		prefixes.put(prefix,  uri);
	}
	
	@Override
	public String getNamespaceURI( String prefix ) {
		return prefixes.get(prefix);
	}

	@Override
	public String getPrefix( String namespaceURI ) {
		// this isn't needed for our purposes
		return null;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Iterator getPrefixes( String namespaceURI ) {
		// this isn't needed for our purposes
		return null;
	}

}
