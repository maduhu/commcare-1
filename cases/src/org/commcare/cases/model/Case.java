/*
 * Copyright (C) 2009 JavaRosa
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

/**
 * 
 */
package org.commcare.cases.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.javarosa.core.model.data.UncastData;
import org.javarosa.core.model.utils.PreloadUtils;
import org.javarosa.core.services.storage.IMetaData;
import org.javarosa.core.services.storage.Persistable;
import org.javarosa.core.services.storage.Secure;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapList;
import org.javarosa.core.util.externalizable.ExtWrapMapPoly;
import org.javarosa.core.util.externalizable.ExtWrapNullable;
import org.javarosa.core.util.externalizable.PrototypeFactory;

/**
 * NOTE: All new fields should be added to the case class using the "data" class,
 * as it demonstrated by the "userid" field. This prevents problems with datatype
 * representation across versions.
 * 
 * @author Clayton Sims
 * @date Mar 19, 2009 
 *
 */
public class Case implements Persistable, IMetaData, Secure {
	public static String STORAGE_KEY = "CASE";
	
	public static String INDEX_CASE_ID = "case-id";
	public static String INDEX_CASE_TYPE = "case-type";
	public static String INDEX_CASE_STATUS = "case-status";
	public static String INDEX_CASE_INDEX_PRE = "case-in-";
	
	private String typeId;
	private String id;
	private String name;
	
	private boolean closed = false;
	
	private Date dateOpened;
	
	int recordId;

	Hashtable data = new Hashtable();
	
	Vector<CaseIndex> indices = new Vector<CaseIndex>();
	
	/**
	 * NOTE: This constructor is for serialization only.
	 */
	public Case() {
		dateOpened = new Date();
	}
	
	public Case(String name, String typeId) {
		setID(-1);
		this.name = name;
		this.typeId = typeId;
		dateOpened = new Date();
		setLastModified(dateOpened);
	}
	
	/**
	 * @return the typeId
	 */
	public String getTypeId() {
		return typeId;
	}

	/**
	 * @param typeId the typeId to set
	 */
	public void setTypeId(String typeId) {
		this.typeId = typeId;
	}

	/**
	 * @return The name of this case
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param The name of this case
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return True if this case is closed, false otherwise.
	 */
	public boolean isClosed() {
		return closed;
	}

	/**
	 * @param Whether or not this case should be recorded as closed
	 */
	public void setClosed(boolean closed) {
		this.closed = closed;
	}

	/**
	 * @return the recordId
	 */
	public int getID() {
		return recordId;
	}
	
	public void setID (int id) {
		this.recordId = id;
	}
		
	public String getUserId() {
		return (String)data.get(org.javarosa.core.api.Constants.USER_ID_KEY);
	}
	
	public void setUserId(String id) {
		data.put(org.javarosa.core.api.Constants.USER_ID_KEY, id);
	}

	/**
	 * @param id the id to set
	 */
	public void setCaseId(String id) {
		this.id = id;
	}
	
	public String getCaseId() {
		return id;
	}
	
	/**
	 * @return the dateOpened
	 */
	public Date getDateOpened() {
		return dateOpened;
	}

	/**
	 * @param dateOpened the dateOpened to set
	 */
	public void setDateOpened(Date dateOpened) {
		this.dateOpened = dateOpened;
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.util.externalizable.Externalizable#readExternal(java.io.DataInputStream, org.javarosa.core.util.externalizable.PrototypeFactory)
	 */
	public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
		typeId = ExtUtil.readString(in);
		id = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
		name = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
		closed = ExtUtil.readBool(in);
		dateOpened = (Date)ExtUtil.read(in, new ExtWrapNullable(Date.class), pf);
		recordId = ExtUtil.readInt(in);
		indices = (Vector<CaseIndex>)ExtUtil.read(in, new ExtWrapList(CaseIndex.class));
		data = (Hashtable)ExtUtil.read(in, new ExtWrapMapPoly(String.class, true), pf);
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.util.externalizable.Externalizable#writeExternal(java.io.DataOutputStream)
	 */
	public void writeExternal(DataOutputStream out) throws IOException {
		ExtUtil.writeString(out, typeId);
		ExtUtil.writeString(out, ExtUtil.emptyIfNull(id));
		ExtUtil.writeString(out, ExtUtil.emptyIfNull(name));
		ExtUtil.writeBool(out, closed);
		ExtUtil.write(out, new ExtWrapNullable(dateOpened));
		ExtUtil.writeNumeric(out, recordId);
		ExtUtil.write(out, new ExtWrapList(indices));
		ExtUtil.write(out, new ExtWrapMapPoly(data));
	}
	
	public void setProperty(String key, Object value) {
		this.data.put(key, value);
	}
	
	public Object getProperty(String key) {
		if("case-id".equals(key)) {
			return id;
		}
		return data.get(key);
	}
	
	public String getPropertyString(String key) {
		Object o = this.getProperty(key);
		if(o instanceof String) {
			return (String)o;
		} else {
			//This is not good, but it's also the uniform matching that's used in the 
			//xml transform, essentially.
			return PreloadUtils.wrapIndeterminedObject(o).uncast().getString();
		}

	}
	
	public Hashtable getProperties() {
		return data;
	}

	public String getRestorableType() {
		return "case";
	}

	public Hashtable getMetaData() {
		Hashtable h = new Hashtable();
		String[] fields = getMetaDataFields();
		for (int i = 0; i < fields.length; i++) {
			String field = fields[i];
			Object value = getMetaData(field);
			if (value != null) {
				h.put(field, value);
			}
		}
		return h;
	}

	public Object getMetaData(String fieldName) {
		if (fieldName.equals(INDEX_CASE_ID)) {
			return id;
		} else if (fieldName.equals("case-type")) {
			return typeId;
		} else if (fieldName.equals(INDEX_CASE_STATUS)) {
			return closed ? "closed" : "open";
		} else if (fieldName.startsWith(INDEX_CASE_INDEX_PRE)) {
			String name = fieldName.substring(fieldName.lastIndexOf('-') + 1, fieldName.length());
			
			for(CaseIndex index : this.getIndices()) {
				if(index.getName().equals(name)) {
					return index.getTarget();
				}
			}
			return "";
		} else {
			throw new IllegalArgumentException("No metadata field " + fieldName  + " in the case storage system");
		}
	}

	public String[] getMetaDataFields() {
		return new String[] {INDEX_CASE_ID, INDEX_CASE_TYPE, INDEX_CASE_STATUS};
	}

	public void setIndex(String indexName, String caseType, String indexValue) {
		CaseIndex index = new CaseIndex(indexName, caseType, indexValue);
		//remove existing indices at this name
		for(CaseIndex i : this.indices) {
			if(i.getName().equals(indexName)) {
				this.indices.removeElement(i);
				break;
			}
		}
		this.indices.addElement(index);
	}
	
	public Vector<CaseIndex> getIndices() {
		return indices;
	}

	private static final String ATTACHMENT_PREFIX = "attachmentdata";
	public void updateAttachment(String attachmentName, String reference) {
		data.put(ATTACHMENT_PREFIX + attachmentName, reference);
	}
	
	public String getAttachmentSource(String attachmentName) {
		return (String)data.get(ATTACHMENT_PREFIX + attachmentName);
	}

	//this is so terrible it hurts. We'll be redoing this
	public Vector<String> getAttachments() {
		Vector<String> attachments = new Vector<String>();
		for(Enumeration en = data.keys() ; en.hasMoreElements() ;) {
			String name = (String)en.nextElement();
			if(name.startsWith(ATTACHMENT_PREFIX)) {
				attachments.addElement(name.substring(ATTACHMENT_PREFIX.length()));
			}
		}
		return attachments;
	}

	public void removeAttachment(String attachmentName) {
		data.remove(ATTACHMENT_PREFIX + attachmentName);
	}
	
	// ugh, adding stuff to case models sucks. Need to code up a transition scheme in android so we
	// can stop having shitty models.
	
	private static final String LAST_MODIFIED = "last_modified";
	
	/**
	 * 
	 * @param lastModified
	 */
	public void setLastModified(Date lastModified) {
		if(lastModified == null) { throw new NullPointerException("Case date last modified cannot be null"); }
		data.put(LAST_MODIFIED, lastModified);
	}
	
	public Date getLastModified() {
		if(!data.containsKey(LAST_MODIFIED)) { return getDateOpened();}
		return (Date)data.get(LAST_MODIFIED);
	}
}
