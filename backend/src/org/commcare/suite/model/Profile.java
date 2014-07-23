package org.commcare.suite.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

import org.javarosa.core.reference.RootTranslator;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.services.storage.Persistable;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapList;
import org.javarosa.core.util.externalizable.ExtWrapMap;
import org.javarosa.core.util.externalizable.PrototypeFactory;

/**
 * <p>
 * Profile is a model which defines the operating profile
 * of a CommCare application. An applicaiton's profile
 * defines what CommCare features should be activated,
 * certain properties which should be defined, and 
 * any JavaRosa URI reference roots which should be
 * available.</p>
 * 
 * 
 * @author ctsims
 *
 */

public class Profile implements Persistable {
	
	public static final String STORAGE_KEY = "PROFILE";
	
	public static final String FEATURE_REVIEW = "checkoff";

	public static final String FEATURE_USERS = "users";
	
	int recordId = -1;
	int version;
	String authRef;
	Vector<PropertySetter> properties;
	Vector<RootTranslator> roots;
	
	Hashtable<String,Boolean> featureStatus;
	
	/**
	 * Serialization Only!
	 */
	public Profile() {
		
	}
	
	/**
	 * Creates an application profile with the provided
	 * version and authoritative reference URI. 
	 * 
	 * @param version The version of this profile which
	 * is represented by this definition.
	 * 
	 * @param authRef A URI which represents the authoritative
	 * source of this profile's master definition. If the 
	 * profile definition read at this URI claims a higher
	 * version number than this profile's version, this profile
	 * is obsoleted by it. 
	 * 
	 */
	public Profile(int version, String authRef) {
		this.version = version;
		this.authRef = authRef;
		properties = new Vector<PropertySetter>();
		roots = new Vector<RootTranslator>();
		featureStatus = new Hashtable<String, Boolean>();
		
		//turn on default features
		featureStatus.put("users",new Boolean(true));
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.javarosa.core.services.storage.Persistable#getID()
	 */
	public int getID() {
		return recordId;
	}

	/*
	 * (non-Javadoc)
	 * @see org.javarosa.core.services.storage.Persistable#setID(int)
	 */
	public void setID(int ID) {
		recordId = ID;
	}
	
	/**
	 * @return The version of this profile which
	 * is represented by this definition.
	 */
	public int getVersion() {
		return version;
	}
	
	/**
	 * @return A URI which represents the authoritative
	 * source of this profile's master definition. If the 
	 * profile definition read at this URI claims a higher
	 * version number than this profile's version, this profile
	 * is obsoleted by it. 
	 */
	public String getAuthReference() {
		return authRef;
	}
	
	
	/**
	 * Determines whether or not a specific CommCare feature should
	 * be active in the current application. 
	 *  
	 * @param feature The key of the feature being requested.
	 * @return Whether or not in the application being defined
	 * by this profile the feature requested should be made available
	 * to end users.
	 */
	public boolean isFeatureActive(String feature) {
		if(!featureStatus.containsKey(feature)) { return false; }
		return featureStatus.get(feature).booleanValue();
	}
	
	// The below methods should all be replaced by a model builder
	// or a change to how the profile parser works
	
	public void addRoot(RootTranslator r) {
		this.roots.addElement(r);
	}
	
	public void addPropertySetter(String key, String value) {
		this.addPropertySetter(key,value,false);
	}
	
	public void addPropertySetter(String key, String value, boolean force) {
		properties.addElement(new PropertySetter(key,value,force));
	}
	
	public PropertySetter[] getPropertySetters() {
		PropertySetter[] setters = new PropertySetter[properties.size()];
		for(int i = 0 ; i < properties.size() ; ++i ) {
			setters[i] = properties.elementAt(i);
		}
		return setters;
	}
	
	public void setFeatureActive(String feature, boolean active) {
		this.featureStatus.put(feature, new Boolean(active));
	}
	
	/**
	 * A helper method which initializes the properties specified 
	 * by this profile definition.
	 * 
	 * Note: This should probably be stored elsewhere, since the operation
	 * mutates the model by removing the properties afterwards. Probably
	 * in the property installer?
	 * 
	 * NOTE: Moving at earliest opportunity to j2me profile installer
	 */
	public void initializeProperties(boolean enableForce) {
		for(PropertySetter setter : properties) {
			String property = PropertyManager._().getSingularProperty(setter.getKey());
			//We only want to set properties which are undefined or are forced
			if(property == null || (enableForce && setter.force)) {
				PropertyManager._().setProperty(setter.getKey(), setter.getValue());
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.javarosa.core.util.externalizable.Externalizable#readExternal(java.io.DataInputStream, org.javarosa.core.util.externalizable.PrototypeFactory)
	 */
	public void readExternal(DataInputStream in, PrototypeFactory pf)
			throws IOException, DeserializationException {
		recordId = ExtUtil.readInt(in);
		version = ExtUtil.readInt(in);
		authRef = ExtUtil.readString(in);
		
		properties = (Vector<PropertySetter>)ExtUtil.read(in, new ExtWrapList(PropertySetter.class),pf);
		roots = (Vector<RootTranslator>)ExtUtil.read(in, new ExtWrapList(RootTranslator.class),pf);
		featureStatus = (Hashtable<String, Boolean>)ExtUtil.read(in, new ExtWrapMap(String.class, Boolean.class),pf);
	}

	/*
	 * (non-Javadoc)
	 * @see org.javarosa.core.util.externalizable.Externalizable#writeExternal(java.io.DataOutputStream)
	 */
	public void writeExternal(DataOutputStream out) throws IOException {
		ExtUtil.writeNumeric(out, recordId);
		ExtUtil.writeNumeric(out, version);
		ExtUtil.writeString(out, authRef);
		
		ExtUtil.write(out, new ExtWrapList(properties));
		ExtUtil.write(out, new ExtWrapList(roots));
		ExtUtil.write(out, new ExtWrapMap(featureStatus));
	}
}
