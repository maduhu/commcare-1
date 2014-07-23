/**
 * 
 */
package org.commcare.suite.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapList;
import org.javarosa.core.util.externalizable.ExtWrapMap;
import org.javarosa.core.util.externalizable.ExtWrapNullable;
import org.javarosa.core.util.externalizable.ExtWrapTagged;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;

/**
 * <p>An Entry definition describes a user 
 * initiated form entry action, what information
 * needs to be collected before that action can
 * begin, and what the User Interface should 
 * present to the user regarding these actions</p>
 * 
 * @author ctsims
 *
 */
public class Entry implements Externalizable{
	
	private String xFormNamespace;
	Vector<SessionDatum> data;
	private Text commandText;
	private String commandId;
	private String imageResource;
	private String audioResource;
	Hashtable<String, DataInstance> instances;
	Vector<StackOperation> stackOperations;
	AssertionSet assertions;
	
	/**
	 * Serialization only!
	 */
	public Entry() {
		
	}
	
	public Entry(String commandId, Text commandText, Vector<SessionDatum> data,
			String formNamespace, String imageResource, String audioResource, Hashtable<String, DataInstance> instances,
			Vector<StackOperation> stackOperations, AssertionSet assertions) {
		this.commandId = commandId  == null ? "" : commandId;
		this.commandText = commandText;
		this.data = data;
		xFormNamespace = formNamespace;
		this.imageResource = imageResource == null ? "" : imageResource;
		this.audioResource = audioResource == null ? "" : audioResource;
		this.instances = instances;
		this.stackOperations = stackOperations;
		this.assertions = assertions;
	}
	
	/**
	 * @return the ID of this entry command. Used by Menus to determine
	 * where the command should be located.
	 */
	public String getCommandId() {
		return commandId;
	}
	
	/**
	 * @return A text whose evaluated string should be presented to the
	 * user as the entry point for this operation
	 */
	public Text getText() {
		return commandText;
	}
	
	/**
	 * @return The XForm Namespce of the form which should be filled out in
	 * the form entry session triggered by this action. null if no entry
	 * should occur [HACK]. 
	 */
	public String getXFormNamespace() {
		return xFormNamespace;
	}
	
	/**
	 * @return the URI of an optionally specified image resource to be used in the
	 * view displaying all xform entries.
	 */
	public String getImageURI(){
		return imageResource;
	}
	
	/**
	 * @return the URI of an optional audio resource to be used in the view displaying all xform entries
	 */
	public String getAudioURI(){
		return audioResource;
	}
	
	public Vector<SessionDatum> getSessionDataReqs() {
		return data;
	}

	public Hashtable<String, DataInstance> getInstances() {
		//return instances;
		
		Hashtable<String, DataInstance> copy = new Hashtable<String, DataInstance>();
		for(Enumeration en = instances.keys(); en.hasMoreElements();){
			String key = (String)en.nextElement();
			
			//This is silly, all of these are externaldata instances. TODO: save their
			//construction details instead.
			DataInstance cur = instances.get(key);
			if(cur instanceof ExternalDataInstance) {
				//Copy the EDI so when it gets populated we don't keep it dependent on this object's lifecycle!!
				copy.put(key, new ExternalDataInstance(((ExternalDataInstance)cur).getReference(), cur.getInstanceId()));
			} else {
				copy.put(key, cur);
			}
		}
		
		return copy;
	}
	
	public AssertionSet getAssertions() {
		return assertions == null ? new AssertionSet(new Vector<String>() ,new Vector<Text>()) : assertions;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.javarosa.core.util.externalizable.Externalizable#readExternal(java.io.DataInputStream, org.javarosa.core.util.externalizable.PrototypeFactory)
	 */
	public void readExternal(DataInputStream in, PrototypeFactory pf)
			throws IOException, DeserializationException {
		this.xFormNamespace = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
		this.commandId = ExtUtil.readString(in);
		this.commandText = (Text)ExtUtil.read(in, Text.class, pf);
		this.imageResource = ExtUtil.readString(in);
		this.audioResource = ExtUtil.readString(in);
		
		data = (Vector<SessionDatum>)ExtUtil.read(in, new ExtWrapList(SessionDatum.class), pf);
		instances = (Hashtable<String, DataInstance>)ExtUtil.read(in, new ExtWrapMap(String.class, new ExtWrapTagged()));
		stackOperations = (Vector<StackOperation>)ExtUtil.read(in, new ExtWrapList(StackOperation.class), pf);
		assertions = (AssertionSet)ExtUtil.read(in, new ExtWrapNullable(AssertionSet.class));
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.javarosa.core.util.externalizable.Externalizable#writeExternal(java.io.DataOutputStream)
	 */
	public void writeExternal(DataOutputStream out) throws IOException {
		ExtUtil.writeString(out, ExtUtil.emptyIfNull(xFormNamespace));
		ExtUtil.writeString(out,commandId);
		ExtUtil.write(out,commandText);
		ExtUtil.write(out, imageResource);
		ExtUtil.write(out, audioResource);
		ExtUtil.write(out, new ExtWrapList(data));
		ExtUtil.write(out, new ExtWrapMap(instances, new ExtWrapTagged()));
		ExtUtil.write(out, new ExtWrapList(stackOperations));
		ExtUtil.write(out, new ExtWrapNullable(assertions));
	}

	public Vector<StackOperation> getPostEntrySessionOperations() {
		return stackOperations;
	}
}
