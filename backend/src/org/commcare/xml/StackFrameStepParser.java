/**
 * 
 */
package org.commcare.xml;

import java.io.IOException;

import org.commcare.suite.model.StackFrameStep;
import org.commcare.util.SessionFrame;
import org.commcare.xml.util.InvalidStructureException;
import org.javarosa.xpath.parser.XPathSyntaxException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * @author ctsims
 *
 */
public class StackFrameStepParser extends ElementParser<StackFrameStep> {
	
	public StackFrameStepParser(KXmlParser parser) {
		super(parser);
	}

	/* (non-Javadoc)
	 * @see org.commcare.xml.ElementParser#parse()
	 */
	public StackFrameStep parse() throws InvalidStructureException, IOException, XmlPullParserException {
		String operation = parser.getName();
		
		if("datum".equals(operation)) {
			String id = parser.getAttributeValue(null, "id");
			return parseValue(SessionFrame.STATE_DATUM_VAL, id);
		} else if("command".equals(operation)) {
			return parseValue(SessionFrame.STATE_COMMAND_ID, null);
		} else {
			throw new InvalidStructureException("<" + operation + "> is not a valid stack frame element!", this.parser);
		}
	}

	private StackFrameStep parseValue(String type, String datumId) throws XmlPullParserException, IOException, InvalidStructureException {
		String value = parser.getAttributeValue(null, "value");
		boolean valueIsXpath = false;
		if(value == null) {
			//must have a child
			value = parser.nextText().trim();
			//Can we get here, or would this have caused an exception?
			if(value ==null) {
				throw new InvalidStructureException("Stack frame element must define a value expression or have a direct value", parser);
			}
			valueIsXpath = false;
		} else {
			//parse out the xpath value to double check for errors
			valueIsXpath = true;
		}
		try {
			return new StackFrameStep(type, datumId, value, valueIsXpath);
		} catch (XPathSyntaxException e) {
			throw new InvalidStructureException("Invalid expression for stack frame step definition: " + value + ".\n" + e.getMessage(), parser);
		}
		
	}
}
