
package com.cometway.om;

import com.cometway.ak.ServiceAgent;
import com.cometway.props.*;
import com.cometway.util.*;
import java.util.*;
import java.io.*;


/**
 * The FileObjectManager manages Props objects and persists them as files, using FilePropsContainers.
 * A Props object must first be created by calling createObject with an ObjectType to get 
 * an ObjectID which matches that props Object. The only ObjectType supported in this Manager
 * is the PropsType. Since only Props Objects are stored, using other types will fail.
 * The actual Props object can be fetched by calling getObject with the ObjectID.
 * The ObjectID associated with its props is: 
 * new ObjectID(props.getString("type"),props.getString("id"));
 * Props objects can be listed by calling listObjects with an Object which listObjects
 * will use as a queryObject. If this object is null, all ObjectID's will be listed. If this
 * Object is an instanceof ObjectType, then all the ObjectID's of the props objects of 
 * this type will be returned. If this Object is an instanceof Pair, then the ObjectID whos
 * props contains a property name that is the String stored in Pair.first() with a value 
 * that is equal to the String stored in Pair.second().
 * Props objects can be removed by calling removeObject with an ObjectID.
 * <p>
 * All the Props objects managed by this manager is cached in a hashtable called
 * 'allprops' with ObjectID's as keys associated with the Props object. 
 * The id property stored in the props object matches its persistent
 * filename as the number after \"prop#\", i.e. filename is \"prop\"+id.
 * This manager is passed a String or File that is the root directory to
 * store all props persistence files in. The props is written to the file
 * as a serialized Hashtable. This hashtable is kept in the FilePropsContainer
 * which keeps the file updated with its hashtable.
 * @see Props
 * @see FilePropsContainer
 * @see ObjectID
 * @see ObjectType
 *

 TODO:
 
 *
 */
public class FileObjectManager extends ServiceAgent implements IObjectManager
{
	protected Hashtable types;
	protected Hashtable allprops;
	protected long nextID;

	protected File rootdir;

	protected Object fsSync;

	/**
	 * Sets the service_name property to object_manager if a value
	 * was not provided.
	 */

	public void initProps()
	{
		setDefault("service_name", "object_manager");
		setDefault("root_dir", "om");
	}


	/**
	 * Starts and registers the object cache.
	 */

	public void start()
	{
		try
		{
			initialize();
			register();
		
		}
		catch (Exception e)
		{
			error("Could not initialize", e);
		}
	}


	/**
	 * Stops and unregisters the object cache.
	 */

	public void stop()
	{
		unregister();

		types = null;
		allprops = null;
	}



	/**
	 * This constructor creates all the directories that doesn't exist. Initialzes local
	 * fields, Reads all new props from root directory. 
	 * @exception IOException This exception is thrown if the root directory is invalid or could not be used.
	 */
	protected void initialize() throws IOException
	{
		File rootdir = new File(getString("root_dir"));
		types = new Hashtable();
		allprops = new Hashtable();
		nextID = System.currentTimeMillis();
		fsSync = new Object();
		
		this.rootdir = rootdir;
		if(!rootdir.exists()) {
			rootdir.mkdirs();
		}
		if(!rootdir.isDirectory()) {
			error("Root directory is not a directory: "+rootdir.toString()+", cannot continue.");
			throw(new IOException("Root directory is not a directory : "+rootdir.toString()));
		}
		if(!rootdir.canRead()) {
			error("Cannot read from root directory: "+rootdir.toString());
			throw(new IOException("Root directory is not readable: "+rootdir.toString()));
		}
		if(!rootdir.canWrite()) {
			error("Cannot write to root directory: "+rootdir.toString());
			throw(new IOException("Root directory is not writeable: "+rootdir.toString()));
		}

		try {
			String t;
			Vector v = new Vector();
			
			String s[] = rootdir.list();
			for(int x = 0;x < s.length;x++) 	{
				try {
					File tmpfile = new File(rootdir,s[x]);
					String type = s[x];
					if(tmpfile.isDirectory()) {
						String fs[] = tmpfile.list();
						for(int z=0;z<fs.length;z++) {
							try {
								FilePropsContainer fpc = new FilePropsContainer(new File(tmpfile,fs[z]));
								Props p = new Props(fpc);
								ObjectID tmpID = null;
								try {
									tmpID = new ObjectID(fpc.getObjectID());
								}
								catch(Exception e) {
									tmpID = new ObjectID(p.getString("type"),p.getString("id"));
								}
								allprops.put(tmpID,p);
								if(!types.containsKey(tmpID.getType())) {
									types.put(tmpID.getType(),new Hashtable());
									//new PropsType(tmpID.getType().substring(PropsType.TYPE_STR.length()+1)));
								}
								Hashtable h = (Hashtable)types.get(tmpID.getType());
								h.put(tmpID,p);
							}
							catch(Exception e) {
								error("Exception caught while reading Store directory"+fs[z],e);
							}
						}
					}
					else {
						warning("Unrecognized file in root directory: "+tmpfile);
					}
				}
				catch(Exception e)	{
					error("Unexpected exception caught while initializing root directory: "+rootdir,e);
				}
			}
		}
		catch(Exception e)	{
			error("Unexpected exception caught while initializing root directory: "+rootdir,e);
		}
	}




	public ObjectID createObject(ObjectType type)
	{
		ObjectID rval = null;
		File typeroot = new File(rootdir,type.toString());
		if(type!=null) {
			if(type instanceof com.cometway.om.PropsType) {
				if(!types.containsKey(type.toString())) {
					//debug("Making directory for props_type: "+type);
					synchronized (fsSync) {
						if(!types.containsKey(type.toString())) {
							typeroot.mkdirs();
							types.put(type.toString(),new Hashtable());
						}
					}
				}
				
				ObjectID newID = new ObjectID(type.toString(),(new java.rmi.dgc.VMID()).toString());
				File propsFile = new File(typeroot,"prop"+(++nextID));
				if(propsFile.exists()) {
					propsFile = new File(typeroot,"prop0"+(nextID));
					if(propsFile.exists()) {
						propsFile = new File(typeroot,"prop00"+(nextID));
						if(propsFile.exists()) {
							propsFile = new File(typeroot,"prop000"+(nextID));
						}
						if(propsFile.exists()) {
							warning("Cannot create new FileObject, ID numbers are inconsistent. Please check your system clock and try again.");
							propsFile = null;
						}
					}
				}
				if(propsFile!=null) {
					try {
						//debug("Making new FilePropsContainer for propsFile: "+propsFile);
						FilePropsContainer fpc = new FilePropsContainer(propsFile);
						Props p = new Props(fpc);
						//debug("Setting FilePropsContainer ObjectID to "+newID);
						fpc.setObjectID(newID.toString());
						p.setProperty("type",newID.getType());
						p.setProperty("id",newID.getID());
						allprops.put(newID,p);
						Hashtable h = (Hashtable)types.get(newID.getType());
						h.put(newID,p);
						rval = newID;
					}
					catch(Exception e) {
						error("Unexpected exception caught while initializing FileObject: "+propsFile);
					}
				}
			}
			else {
				error("Type '"+type+"' not supported by this ObjectManager.");
			}
		}
		else {
			error("Cannot create object with a null type.");
		}	

		return(rval);
	}


	public Object getObject(ObjectID id)
	{
		Object rval = null;
		
		if(id!=null) {
			rval = allprops.get(id);
		}				

		return(rval);
	}

	public Vector listObjects(Object objectQuery)
	{
		//		debug("listObjects: "+objectQuery);

		Vector rval = new Vector();
		
		try {

		// objectQuery is null : list all the objects' ObjectIDs of all types
		if(objectQuery == null) {
			Enumeration ids = allprops.keys();
			while(ids.hasMoreElements()) {
				rval.addElement(ids.nextElement());
			}
		}
		
		// objectQuery is ObjectType : list all ObjectIDs of this type
		else if(objectQuery instanceof ObjectType) {
			ObjectType type = (ObjectType)objectQuery;
			if(types.containsKey(type.getType())) {
				Hashtable h = (Hashtable)types.get(type.getType());
				Enumeration ids = h.keys();
				while(ids.hasMoreElements()) {
					//					ObjectID id = (ObjectID)ids.nextElement();
					rval.addElement(ids.nextElement());
				}
			}
		}

		// objectQuery is PropsQuery : list all ObjectIDs matching the PropsQuery's type and having its key=value match
		else if(objectQuery instanceof PropsQuery) {
			PropsQuery query = (PropsQuery)objectQuery;
			Hashtable type = (Hashtable)types.get(query.typeName);
			
			if(type!=null) {

				//				debug("type size: "+type.size());
				
				// This is split up and more code is duplicated in order to be 
				// efficient when managing LARGE numbers of objects
				
				// If PropsQuery's key value is null, all matching types are included in results
				if(query.key==null) {
					Enumeration e = type.keys();
					while(e.hasMoreElements()) {
						rval.addElement(e.nextElement());
					}
				}
				// If PropsQuery's key value is null, all matching types are included in results
				else if(query.value==null) {
					Enumeration e = type.keys();
					while(e.hasMoreElements()) {
						ObjectID tmpID = (ObjectID)e.nextElement();
						Props p = (Props)allprops.get(tmpID);
						//debug("Checking Props with ID '"+tmpID+"' for property '"+query.key+"'");
						if(p.hasProperty(query.key)) {
							//debug("Props has property");
							rval.addElement(e.nextElement());
						}
					}
				}
						
				// If PropsQuery has a non-null key and value, match both key and value for results
				else {
					Enumeration e = type.keys();
					while(e.hasMoreElements()) {
						ObjectID tmpID = (ObjectID)e.nextElement();
						Props p = (Props)allprops.get(tmpID);
						//debug("Checking Props with ID '"+tmpID+"' for property '"+query.key+"' and if it equals '"+query.value+"'");
						if(p.hasProperty(query.key) && p.getProperty(query.key).equals(query.value)) {
							//debug("Props has property and correct value.");
							rval.addElement(tmpID);
						}
					}
				}
			}
		}

		// objectQuery is String : String is either LIST_TYPES (all types managed by this ObjectManager) 
		//                         or LIST_SUPPORTED_TYPES (all types supported by createObject())
		else if(objectQuery instanceof String) {
			if(LIST_TYPES.equals((String)objectQuery)) {
				Enumeration en = types.keys();
				while(en.hasMoreElements()) {
					rval.addElement(new ObjectType(en.nextElement().toString()));
				}
			}
			else if(LIST_SUPPORTED_TYPES.equals((String)objectQuery)) {
				rval.addElement((new PropsType("blah")).getClass().toString());
			}
		}

		}
		catch(Exception asdad) {
			error("Exception caught while listing Objects.",asdad);
		}

		//		debug("listObjects() returnning: "+rval.size()+" elements for query: "+objectQuery+" : "+rval);

		return(rval);
	}

	public boolean removeObject(ObjectID id)
	{
		boolean rval = false;
		
		try {
			Props p = (Props)allprops.remove(id.toString());
			FilePropsContainer container = (FilePropsContainer)p.getPropsContainer();
			container.dispose();
			Hashtable h = (Hashtable)types.get(id.getType());
			h.remove(id.toString());
			if(h.size()==0) {
				types.remove(id.getType());
				try {
					File f = new File(rootdir,id.getType());
					f.delete();
				}
				catch(Exception ex) {
					error("Attempted to delete unused ObjectType : "+id.getType()+" from the root store directory: "+rootdir,ex);
				}
			}
			rval = true;
		}
		catch(Exception e) {
			;
		}			

		return(rval);
	}

	public synchronized boolean changeObjectID(ObjectID oldID, ObjectID newID)
	{
		boolean rval = false;
		try {
			Props p = (Props)getObject(oldID);
			FilePropsContainer fpc = (FilePropsContainer)p.getPropsContainer();
debug("Setting FilePropsContainer objectID to "+newID);
			fpc.setObjectID(newID.toString());
			p.setProperty("type",newID.getType());
			p.setProperty("id",newID.getID());
			rval = true;
		}
		catch(Exception e) {
			error("Exception caught while changing ObjectID: oldID="+oldID+", newID="+newID,e);
		}
		return(rval);
	}
}
