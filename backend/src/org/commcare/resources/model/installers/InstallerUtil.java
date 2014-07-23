package org.commcare.resources.model.installers;

import java.io.IOException;

import org.commcare.resources.model.MissingMediaException;
import org.commcare.resources.model.Resource;
import org.javarosa.core.reference.InvalidReferenceException;
import org.javarosa.core.reference.Reference;
import org.javarosa.core.reference.ReferenceManager;
import org.javarosa.core.util.SizeBoundUniqueVector;

public class InstallerUtil {
	
	public static enum MediaType{
		IMAGE, AUDIO, VIDEO
	}
	
	public static void checkMedia(Resource r, String filePath, SizeBoundUniqueVector<MissingMediaException> problems, MediaType mt){
		try{
			
			System.out.println("722 checking media");
			
			Reference ref = ReferenceManager._().DeriveReference(filePath);
			String localName = ref.getLocalURI();
			boolean successfulAdd;
			try {
				if(!ref.doesBinaryExist()) {
					
					System.out.println("722 ref doesn't exist for: " + r.getDescriptor() + " resource with filepath: " + filePath + " Media Type: " + mt.toString());
					
					successfulAdd = problems.add(new MissingMediaException(r,"Missing external media: " + localName, filePath));
					if(successfulAdd){
						
						switch (mt){
						case IMAGE:
							problems.addBadImageReference();
							break;
						
						case AUDIO:
							problems.addBadAudioReference();
							break;
						
						case VIDEO:
							problems.addBadVideoReference();
							break;
						
						default:
							break;	
						}
					}
				}
			} catch (IOException e) {
				problems.addElement(new MissingMediaException(r,"Problem reading external media: " + localName, filePath));
			} 
		} catch (InvalidReferenceException e) {
			//So the problem is that this might be a valid entry that depends on context
			//in the form, so we'll ignore this situation for now.
		}
	}
}
