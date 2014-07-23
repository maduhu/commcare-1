/**
 * 
 */
package org.commcare.xml;

import java.io.IOException;

import org.commcare.suite.model.SessionDatum;
import org.commcare.xml.util.InvalidStructureException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * @author ctsims
 *
 */
public class SessionDatumParser extends ElementParser<SessionDatum> {

	public SessionDatumParser(KXmlParser parser) {
		super(parser);
	}

	public SessionDatum parse() throws InvalidStructureException, IOException, XmlPullParserException {
		if((!"datum".equals(this.parser.getName())) && !("form".equals(this.parser.getName()))) {
			throw new InvalidStructureException("Expected <datum> or <form> data in <session> block, instead found " + this.parser.getName() + ">", this.parser);
		}
		
		String id = parser.getAttributeValue(null, "id");
		
		String calculate = parser.getAttributeValue(null, "function");
		
		SessionDatum datum;
		if(calculate == null) {
			String nodeset = parser.getAttributeValue(null, "nodeset");
			String shortDetail = parser.getAttributeValue(null, "detail-select");
			String longDetail = parser.getAttributeValue(null, "detail-confirm");
			String value = parser.getAttributeValue(null, "value");
			
			if(nodeset == null) {
				throw new InvalidStructureException("Expected @nodeset in " + id +" <datum> definition", this.parser);
			}
			
			datum = new SessionDatum(id, nodeset, shortDetail, longDetail, value);
		} else {
			if("form".equals(this.parser.getName())) {
				datum = SessionDatum.FormIdDatum(calculate);
			} else {
				datum = new SessionDatum(id, calculate);
			}
		}
		
		while(parser.next() == KXmlParser.TEXT);
		
		return datum;
	}

}
