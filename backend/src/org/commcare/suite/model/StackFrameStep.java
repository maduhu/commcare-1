/**
 * 
 */
package org.commcare.suite.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.commcare.util.SessionFrame;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xpath.XPathException;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.XPathFuncExpr;
import org.javarosa.xpath.parser.XPathSyntaxException;

/**
 * TODO: Replace our sketchy string[] steps with this more broadly and re-use 
 * the functionality there?
 * 
 * @author ctsims
 *
 */
public class StackFrameStep implements Externalizable {
	//Share the types with the commands
	String elementType;
	String id;
	String value;
	boolean valueIsXpath;
	
	/**
	 * Serialization Only
	 */
	public StackFrameStep() {
		
	}
	
	public StackFrameStep(String type, String id, String value, boolean valueIsXpath) throws XPathSyntaxException {
		this.elementType = type;
		this.id = id;
		this.value = value;
		this.valueIsXpath = valueIsXpath;
		
		if(valueIsXpath) {
			//Run the parser to ensure that we will fail fast when _creating_ the step, not when 
			//running it
			XPathParseTool.parseXPath(value);
		}
	}
	
	/**
	 * Get a performed step to pass on to an actual frame 
	 * 
	 * @param ec Context to evaluate any parameters with
	 * @return A step that can be added to a session frame
	 */
	public String[] defineStep(EvaluationContext ec) {
		String finalValue;
		if(!valueIsXpath) {
			finalValue = value;
		} else {
			try {
				finalValue = XPathFuncExpr.toString(XPathParseTool.parseXPath(value).eval(ec));
			} catch (XPathSyntaxException e) {
				//This error makes no sense, since we parse the input for
				//validation when we create it!
				throw new XPathException(e.getMessage());
			}
		}
		
		//figure out how to structure the step
		if(elementType.equals(SessionFrame.STATE_DATUM_VAL)) {
			return new String[] {SessionFrame.STATE_DATUM_VAL, id, finalValue};
		} else if(elementType.equals(SessionFrame.STATE_COMMAND_ID)) {
			return new String[] {SessionFrame.STATE_COMMAND_ID, finalValue};
		} else if(elementType.equals(SessionFrame.STATE_FORM_XMLNS)) {
			throw new RuntimeException("Form Definitions in Steps are not yet supported!");
		} else {
			throw new RuntimeException("Invalid step [" + elementType + "] declared when constructing a new frame step");
		}
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.util.externalizable.Externalizable#readExternal(java.io.DataInputStream, org.javarosa.core.util.externalizable.PrototypeFactory)
	 */
	public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
		this.elementType = ExtUtil.readString(in);
		this.id = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
		this.value = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
		this.valueIsXpath = ExtUtil.readBool(in);
	}

	/* (non-Javadoc)
	 * @see org.javarosa.core.util.externalizable.Externalizable#writeExternal(java.io.DataOutputStream)
	 */
	public void writeExternal(DataOutputStream out) throws IOException {
		ExtUtil.writeString(out, elementType);
		ExtUtil.writeString(out, ExtUtil.emptyIfNull(id));
		ExtUtil.writeString(out, ExtUtil.emptyIfNull(value));
		ExtUtil.writeBool(out, valueIsXpath);
	}

}
