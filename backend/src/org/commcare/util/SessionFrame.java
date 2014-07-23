/**
 * 
 */
package org.commcare.util;

import java.util.Vector;

/**
 * @author ctsims
 *
 */
public class SessionFrame {	
	/** CommCare needs a Command (an entry, view, etc) to proceed. Generally sitting on a menu screen. */
    public static final String STATE_COMMAND_ID = "COMMAND_ID";
    /** CommCare needs the ID of a Case to proceed **/
    public static final String STATE_DATUM_VAL = "CASE_ID";
    /** Computed Value **/
    public static final String STATE_DATUM_COMPUTED = "COMPTUED_DATUM";
    /** CommCare needs the XMLNS of the form to be entered to proceed **/
    public static final String STATE_FORM_XMLNS = "FORM_XMLNS";
	
	public static final String ENTITY_NONE = "NONE";
	
	private String frameId;
	protected Vector<String[]> steps = new Vector<String[]>();
	
	protected Vector<String[]> snapshot;
	
	/**
	 * Create a new, un-id'd session frame
	 */
	public SessionFrame() {
		
	}

	
	public SessionFrame(String frameId) {
		this.frameId = frameId;
	}



	public Vector<String[]>  getSteps() {
		return steps;
	}



	public String[] popStep() {
		String[] recentPop = null;
		
		if(steps.size() > 0) {
			recentPop = steps.elementAt(steps.size() -1);
			steps.removeElementAt(steps.size() - 1);
		}
		return recentPop;
	}



	public void pushStep(String[] step) {
		steps.addElement(step);
	}



	public String getFrameId() {
		return frameId;
	}
	
	/**
	 * Requests that the frame capture an original snapshot of its state.
	 * This snapshot can be referenced later to compare the eventual state
	 * of the frame to an earlier point 		
	 */
	public void captureSnapshot() {
		synchronized(steps) {
			snapshot = new Vector<String[]>();
			for(String[] s : steps) {
				snapshot.addElement(s);
			}
		}
	}
	
	/**
	 * Determines whether the current frame state is incompatible with
	 * a previously snapshotted frame state, if one exists. If no snapshot
	 * exists, this method will return false. 
	 * 
	 * Compatibility is determined by checking that each step in the previous
	 * snapshot is matched by an identical step in the current snapshot.
	 * 
	 * @return
	 */
	public boolean isSnapshotIncompatible() {
		synchronized(steps) {
			//No snapshot, can't be incompatible.
			if(snapshot == null) { return false; }
			
			if(snapshot.size() > steps.size()) { return true; }
			
			//Go through each step in the snapshot
			for(int i = 0 ; i < snapshot.size() ; ++i ) {
				String[] snapStep = snapshot.elementAt(i);
				String[] curStep = steps.elementAt(i);
				
				//Make sure they're the same length, otherwise they can't be equal
				if(snapStep.length != curStep.length) { return true; }
				
				//Make sure it's the same step, otherwise the snapshot is incompatible
				for(int j = 0 ; j < snapStep.length ; ++j) {
					if(!snapStep[j].equals(curStep[j])) { return true; }
				}
			}
			
			//Id we didn't find anything wrong, we're good to go!
			return false;
		}
	}


	public void clearSnapshot() {
		synchronized(steps) {
			this.snapshot = null;
		}
	}
}
