/**
 * 
 */
package org.commcare.util;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.commcare.resources.model.Resource;
import org.commcare.resources.model.ResourceInitializationException;
import org.commcare.resources.model.ResourceLocation;
import org.commcare.resources.model.ResourceTable;
import org.commcare.resources.model.UnresolvedResourceException;
import org.commcare.suite.model.Entry;
import org.commcare.suite.model.Profile;
import org.commcare.suite.model.Suite;
import org.commcare.xml.util.UnfullfilledRequirementsException;
import org.javarosa.core.services.Logger;
import org.javarosa.core.services.storage.IStorageUtility;
import org.javarosa.core.services.storage.StorageFullException;
import org.javarosa.core.services.storage.StorageManager;

/**
 * TODO: This isn't really a great candidate for a 
 * singleton interface. It should almost certainly be
 * a more broad code-based installer/registration
 * process or something.
 * 
 * Also: It shares a lot of similarities with the 
 * Context app object in j2me. Maybe we should roll
 * some of that in.
 * 
 * @author ctsims
 *
 */
public class CommCarePlatform implements CommCareInstance {
	//TODO: We should make this unique using the parser to invalidate this ID or something
	public static final String APP_PROFILE_RESOURCE_ID = "commcare-application-profile";
	
	private Vector<Integer> suites;
	private int profile;
	
	private int majorVersion;
	private int minorVersion;
	
	public CommCarePlatform(int majorVersion, int minorVersion) {
		profile = -1;
		suites = new Vector<Integer>();
		this.majorVersion = majorVersion;
		this.minorVersion = minorVersion;
	}
	
	public void init(String profileReference, ResourceTable global, boolean forceInstall) throws UnfullfilledRequirementsException,  UnresolvedResourceException{
		try {

			if (!global.isReady()) {
				global.prepareResources(null, this);
			}
			
			// First, see if the appropriate profile exists
			Resource profile = global.getResourceWithId(APP_PROFILE_RESOURCE_ID);
			
			//If it does not, we need to grab it locally, and get parsing...
			if (profile == null) {

				Vector<ResourceLocation> locations = new Vector<ResourceLocation>();
				locations.addElement(new ResourceLocation(Resource.RESOURCE_AUTHORITY_LOCAL, profileReference));
				
				//We need a way to identify this version...
				Resource r = new Resource(Resource.RESOURCE_VERSION_UNKNOWN, APP_PROFILE_RESOURCE_ID , locations, "Application Descriptor");
				
				global.addResource(r, global.getInstallers().getProfileInstaller(forceInstall), "");
				global.prepareResources(null, this);
			} else{
				//Assuming it does exist, we might want to do an automatic
				//upgrade here, leaving that for a future date....
			}
		} catch (StorageFullException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public int getMajorVersion() {
		return majorVersion;
	}
	
	public int getMinorVersion() {
		return minorVersion;
	}
	
	public ResourceTable stageUpgradeTable(ResourceTable global, ResourceTable incoming, boolean clearProgress) throws UnfullfilledRequirementsException, StorageFullException, UnresolvedResourceException {
		Profile current = getCurrentProfile();
		return stageUpgradeTable(global, incoming, current.getAuthReference(), clearProgress);
	}
	
	public ResourceTable stageUpgradeTable(ResourceTable global, ResourceTable incoming, String profileRef, boolean clearProgress) throws UnfullfilledRequirementsException, StorageFullException, UnresolvedResourceException {
		//Make sure everything's in a good state
		if(global.getTableReadiness() != ResourceTable.RESOURCE_TABLE_INSTALLED) {
			throw new IllegalArgumentException("Global resource table was not ready for upgrading");
		}
		
		if(clearProgress) {
			//In the future: Continuable upgrades. Now: Clear old upgrade info
			incoming.clear();
		}

		Vector<ResourceLocation> locations = new Vector<ResourceLocation>();
		locations.addElement(new ResourceLocation(Resource.RESOURCE_AUTHORITY_REMOTE, profileRef));
			
		Resource r = new Resource(Resource.RESOURCE_VERSION_UNKNOWN, APP_PROFILE_RESOURCE_ID , locations, "Application Descriptor");
		
		incoming.addResource(r, incoming.getInstallers().getProfileInstaller(false), null);
		
		incoming.prepareResources(global, this, APP_PROFILE_RESOURCE_ID);
		
		return incoming;

	}
	
	public void upgrade(ResourceTable global, ResourceTable incoming, ResourceTable recovery) throws UnfullfilledRequirementsException, UnresolvedResourceException, IllegalArgumentException {

		if(global.getTableReadiness() != ResourceTable.RESOURCE_TABLE_INSTALLED) {
			throw new IllegalArgumentException("Global resource table was not ready for upgrading");
		}
		
		//TODO: Figure out more cleanly what the acceptable states are here
		int incomingState = incoming.getTableReadiness();
		if(incomingState == ResourceTable.RESOURCE_TABLE_UNCOMMITED || incomingState == ResourceTable.RESOURCE_TABLE_UNSTAGED || incomingState == ResourceTable.RESOURCE_TABLE_EMPTY) {
			throw new IllegalArgumentException("Upgrade table is not in an appropriate state");
		}
		
		//Wipe out any existing records in the recovery table. If there's _anything_ in there and
		//the app isn't in the install state, that's a signal to recover.
		recovery.destroy();
		
		//Fetch and prepare all resources (Likely exit point here if a resource can't be found)
		incoming.prepareResources(global, this);
		
		boolean upgradeSuccess = false;
		try {
			Logger.log("Resource", "Upgrade table fetched, beginning upgrade");
			//Try to stage the upgrade table to replace the incoming table
			if(!global.upgradeTable(incoming) || incoming.getTableReadiness() != ResourceTable.RESOURCE_TABLE_INSTALLED) {
				throw new RuntimeException("Implement me!!!");
			} else {
				//otherwise keep going
				Logger.log("Resource", "Global table unstaged, upgrade table ready");
			}
			
			//Now we basically want to replace the global resource table with the upgrade table
			
			//ok, so temporary should now be fully installed.
			//make a copy of our table just in case.
				
			Logger.log("Resource", "Copying global resources to recovery area");
			try { 
				global.copyToTable(recovery);
			} catch(RuntimeException e) {
				//The _only_ time the recovery table should have data is if 
				//we were in the middle of an install. Since global hasn't been
				//modified if there is a problem here we want to wipe out the 
				//recovery stub
				
				//TODO: If this fails? Oof.
				recovery.destroy();
				throw e;
			}
			
			Logger.log("Resource", "Wiping global");
			//now clear the global table to make room (but not the data, just the records) 
			global.destroy();
			
			Logger.log("Resource", "Moving update resources");
			//Now copy the upgrade table to take its place
			incoming.copyToTable(global);
			
			//Success! The global table should be ready to go now.
			upgradeSuccess = true;
			
			Logger.log("Resource", "Upgrade Succesful!");
			
			//Now we need to do cleanup. Wipe out the upgrade table and finalize
			//removing the original resources which are no longer needed.
			
			//Wipe the incoming (we need to do nothing with its resources)
			Logger.log("Resource", "Wiping redundant update table");
			incoming.destroy();
			
			//Uninstall old resources
			Logger.log("Resource", "Clearing out old resources");
			recovery.flagForDeletions(global);
			recovery.completeUninstall();
			
			//good to go.
			
		} finally {
			if(!upgradeSuccess) {
				repair(global, incoming, recovery);
			}
			//Clear out any app state
			profile = -1;
			suites.removeAllElements();
			
			//Is it really possible to verify that we've un-registered everything here? Locale files are 
			//registered elsewhere, and we can't guarantee we're the only thing in there, so we can't
			//straight up clear it...
			initialize(global);
		}
	}
	
	/**
	 * This method is responsible for recovering the state of the application to installed after anything
	 * happens during an upgrade. After it is finished, the global resource table should be valid.
	 * 
	 * NOTE: this does not currently repair resources which have been corrupted, merely returns all of the
	 * tables to the appropriate states
	 * 
	 * @param global
	 * @param incoming
	 * @param recovery
	 */
	private void repair(ResourceTable global, ResourceTable incoming, ResourceTable recovery) {
		//First we need to figure out what state we're in currently. There are a few possibilities
		
		//TODO: Handle: Upgrade complete (upgrade table empty, all resources pushed to global), recovery table
		//not empty		
		
		//First possibility is needing to restore from the recovery table.
		if(!recovery.isEmpty()) {
			//If the recovery table isn't empty, we're likely restoring from there. We need to check first whether
			//the global table has the same profile, or the recovery table simply doesn't have one in which case 
			//the recovery table didn't get copied correctly.
			if(recovery.getResourceWithId(APP_PROFILE_RESOURCE_ID) == null || (global.getResourceWithId(APP_PROFILE_RESOURCE_ID).getVersion() == recovery.getResourceWithId(APP_PROFILE_RESOURCE_ID).getVersion())) {
				Logger.log("resource", "Invalid recovery table detected. Wiping recovery table");
				//This means the recovery table should be empty. Invalid copy.
				recovery.destroy();
			} else {
				//We need to recover the global resources from the recovery table.
				Logger.log("resource", "Recovering global resources from recovery table");
				
				global.destroy();
				recovery.copyToTable(global);
				
				Logger.log("resource", "Global resources recovered. Wiping recovery table");
				recovery.destroy();
			}
		}
		
		//Ok, so global and incoming are now in the right places. Make sure we have no uncommitted resources
		
		if(global.getTableReadiness() == ResourceTable.RESOURCE_TABLE_UNCOMMITED) {
			global.rollbackCommits();
		}
		
		
		if(incoming.getTableReadiness() == ResourceTable.RESOURCE_TABLE_UNCOMMITED) {
			incoming.rollbackCommits();
		}

		
		//Ok, so now if the global table needed to be recovered from the recovery table, it has. There are 
		//now two states: Either the global table is fully installed (no conflicts with the upgrade table)
		//or it has unstaged resources to restage
		
		if(global.getTableReadiness() == ResourceTable.RESOURCE_TABLE_INSTALLED) {
			Logger.log("resource", "Global table in fully installed mode. Repair complete");
			//We're actually just good to go, here. 
		} else if(global.getTableReadiness() == ResourceTable.RESOURCE_TABLE_UNSTAGED){
			//The global table needs to restage itself. 
			Logger.log("resource", "Global table needs to restage some resources");
			global.repairTable(incoming);
		}
		
	}
	
	public Profile getCurrentProfile() {
		return (Profile)(StorageManager.getStorage(Profile.STORAGE_KEY).read(profile));
	}
	
	public Vector<Suite> getInstalledSuites() {
		Vector<Suite> installedSuites = new Vector<Suite>();
		IStorageUtility utility = StorageManager.getStorage(Suite.STORAGE_KEY);
		for(Integer i : suites) {
			installedSuites.addElement((Suite)(utility.read(i.intValue())));
		}
		return installedSuites;
	}
	
	public void setProfile(Profile p) {
		this.profile = p.getID();
	}
	
	
	public void registerSuite(Suite s) {
		this.suites.addElement(new Integer(s.getID()));
	}
	
	public void initialize(ResourceTable global) {
		try {
			global.initializeResources(this);
		} catch (ResourceInitializationException e) {
			e.printStackTrace();
			throw new RuntimeException("Error initializing Resource! "+ e.getMessage());
		}
	}
	
	public Hashtable<String, Entry> getMenuMap() {
		Vector<Suite> installed = getInstalledSuites();
		Hashtable<String, Entry> merged = new Hashtable<String, Entry>();
		
		for(Suite s : installed) {
			Hashtable<String, Entry> table = s.getEntries();
			for(Enumeration en = table.keys() ; en.hasMoreElements() ; ) {
				String key = (String)en.nextElement();
				merged.put(key, table.get(key));
			}
		}
		return merged;
	}
	
	public static Vector<Resource> getResourceListFromProfile(ResourceTable master) {
		Vector<Resource> unresolved = new Vector<Resource>();
		Vector<Resource> resolved = new Vector<Resource>();
		Resource r = master.getResourceWithId(APP_PROFILE_RESOURCE_ID);
		if(r == null) {
			return resolved;
		}
		unresolved.addElement(r);
		while(unresolved.size() > 0) {
			Resource current = unresolved.firstElement();
			unresolved.removeElement(current);
			resolved.addElement(current);
			Vector<Resource> children = master.getResourcesForParent(current.getRecordGuid());
			for(Resource child : children) {
				unresolved.addElement(child);
			}
		}
		return resolved;
	}
}
