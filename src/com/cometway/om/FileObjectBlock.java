
package com.cometway.om;

import java.io.*;
import java.util.*;
import com.cometway.util.*;


/**
 * This class is used to store Objects into a file prepared by FileObjectStore.
 * The store returns a reference to a block in the file. Objects can be written
 * and read from this block. A FileObjectBlock can be given a header description
 * (an Object) as well as the Object stored in the body of the block. If the
 * Object stored in the body of the block exceeds the length of the block, a new
 * block will be allocated and the remaining bytes stored there. If the next block
 * is used up, it will automatically allocate a new one from the store, and so on.
 * When these extra blocks in chain of blocks are no longer needed, they are 
 * automatically disposed of. 
 * @see FileObjectStore
 *
 * @author jonlin@andrew.cmu.edu
 * @version 1.0A
 */
public class FileObjectBlock
{
	final static boolean debug = true;
	final static boolean verbose = true;
	final static boolean printStackTraces = true;

	// This is how the physical BLOCK is mapped. 
	//     
	//       /-------------- HEADER_TAG
	//       | /------------ HEADER_PARENT_INDEX
	//       | | /---------- HEADER_NEXT_INDEX
	//       | | | /-------- HEADER_DATA
	//       | | | |
	//      _|_|_|_|__________________________________________________
	//      |_|_|_|_______________|_|_|_|________DATA________________|
	//     /\                    / | | |
	//    /  |-- headerLength --|  | | \-------- BODY_EXTENDED_DATA
	//   /                         | \---------- BODY_DATA
	//   |-- getOffset()           \-------------BODY_TAG
	//
	// These are offset from the beginning of the block (getOffset())
	protected final static int HEADER_TAG_OFFSET = 0;
	protected final static int HEADER_PARENT_INDEX_OFFSET = 8;
	protected final static int HEADER_NEXT_INDEX_OFFSET = 12;
	protected final static int HEADER_DATA_OFFSET = 16;

	// THese are offset from the headerLength
	protected final static int BODY_TAG_OFFSET = 0;
	protected final static int BODY_DATA_OFFSET = 8;
	protected final static int BODY_EXTENDED_DATA_OFFSET = 16;
	

	// This is a fixed length allocated to the header. The default size is 1/16 the BLOCK_SIZE
	protected int headerLength;
	// This is an optimization flag, tells this block not to bother reading anything (i.e. moving file pointer)
	protected boolean isEmpty;

	// This is the index into the store file which this FileObjectBlock reads and writes to.
	int index;
	// This is the store which created this Block
	FileObjectStore store;

	// This is the block which has continuing data
	protected FileObjectBlock nextBlock;
	// This is the block which data is continued from
	protected int parentBlockIndex = -1;

	/** 
	 * When this flag is set to <b>FALSE</b> this FileObjectBlock 
	 * will be slightly more inventive when encountering 
	 * unexpected errors rather than just give up.
	 */
	public boolean conservativeWithErrors = true;

	// This is the synchronization object to synchronize all writes to the store file
	private Object WRITE_SYNC;

	/**
	 * Creates an instance of FileObjectBlock. 
	 * @param store THis is the FileObjectStore which this block will read and write to. 
	 * @param index This is the index into the store file which this block will read and write to.
	 */
	protected FileObjectBlock(FileObjectStore store, int index)
	{
		this.store = store;
		this.index = index;
		WRITE_SYNC = new Object();
		//		READ_SYNC = new Object();
		headerLength = (int)store.BLOCK_SIZE / 16;
		if(headerLength<(HEADER_DATA_OFFSET+32)) {
			headerLength = HEADER_DATA_OFFSET+32;
			if(headerLength>store.BLOCK_SIZE) {
				error("Block size is too small for a header: "+store.BLOCK_SIZE);
			}
		}
	}


	/**
	 * This method updates all the local instance fields from information stored in the file.
	 */
 	public void updateFromFile()
	{
		try {
			long tmp = -124457;
			try {
				tmp = store.readLong(getOffset()+HEADER_TAG_OFFSET);
			}
			catch(IOException e) {
				;
			}

			if(tmp == -1) {
				parentBlockIndex = store.readInt(getOffset()+HEADER_PARENT_INDEX_OFFSET);
				isEmpty=false;
			}
			else if (tmp == -2) {
				isEmpty=true;
			}
			else {
				isEmpty = false;
			}
			
			try {
				tmp = store.readLong(getOffset()+headerLength+BODY_TAG_OFFSET);
			}
			catch(IOException e) {
				tmp = -149835;
			}

			if(tmp<0 && tmp>-3) {
				isEmpty=false;
				nextBlock = store.getBlock(store.readInt(getOffset()+HEADER_NEXT_INDEX_OFFSET));
			}
		}
		catch(Exception e) {
			error("Exception caught while initializing block with index: "+index,e);
			isEmpty = !conservativeWithErrors;
		} 
	}		

	/**
	 * This method returns the Object stored in the header of this block. 
	 * If the Object's size exceeds the fixed size of the header, it may not
	 * be unserializable. If no object was found or it could not be unserialized,
	 * null is returned.
	 * @return Returns the Object stored in this Block's header, or null if nothing was found or the data was unserializable.
	 */
	public Object readHeader()
	{
		Object rval = null;
		if(!isEmpty) {
			try {
				/*
				if(nextBlock==null) {
					try {
						int nextBlockIndex = store.readInt(getOffset()+HEADER_NEXT_INDEX_OFFSET);
						nextBlock = store.getBlock(nextBlockIndex);
					}
					catch(Exception e) {;}
				}
				*/

				long length = store.readLong(getOffset()+HEADER_TAG_OFFSET);
				if(length==-1) {
					parentBlockIndex = store.readInt(getOffset()+HEADER_PARENT_INDEX_OFFSET);
					try {
						rval = store.getBlock(parentBlockIndex);//store.blocks.elementAt(store.blocks.indexOf(new FileObjectBlock(store,parentBlockIndex)));
						if(rval==null) {
							parentBlockIndex=-1;
							error("WARNING: parent block not found in store file, assuming unlinked node and disposing myself.");
							dispose();
							return(null);
						}
					}
					catch(Exception e) {
						parentBlockIndex=-1;
						dispose();
						return(null);
					}
				}
				else if(length==-2) {
					//					dispose();
					return(null);
				}
				else {
					try {
						rval = ObjectSerializer.unserialize(store.readBytes(getOffset()+HEADER_DATA_OFFSET,getOffset()+HEADER_DATA_OFFSET+length));
					}
					catch(Exception e) {
						rval = null;
					}
				}
			}
			catch(Exception e) {
				error("Exception caught while reading header.",e);
			}
		}
		return(rval);
	}

	/**
	 * This method writes an Object in the header of this Block. If the
	 * object was successfully written, TRUE will be returned. 
	 * @param data This Object will be stored in the header of this Block
	 * @return Returns true IFF the Object was written successfully.
	 */
	public boolean writeHeader(Object data)
	{
		try {
			byte[] object = ObjectSerializer.serialize(data);
			if(object.length > headerLength-HEADER_DATA_OFFSET) {
				error("WARNING: header data exceeds the header length. Fixed header size is: "+(headerLength-HEADER_DATA_OFFSET)+".");
				return(false);
			}
			synchronized (WRITE_SYNC) {
				store.writeLong(object.length,getOffset()+HEADER_TAG_OFFSET);
				store.writeBytes(object,getOffset()+HEADER_DATA_OFFSET);
			}
		}
		catch(Exception e) {
			error("Exception caught while writing header.",e);
			return(false);
		}

      isEmpty = false;
		return(true);
	}

	// This is called by the FileObjectBlock when it needs another Block to write data to.
	// THis method should not be called by anyone else
	boolean writeHeader(int parentIndex)
	{
		try {
			synchronized (WRITE_SYNC) {
				store.writeLong(-1,getOffset()+HEADER_TAG_OFFSET);
				store.writeInt(parentIndex,getOffset()+HEADER_DATA_OFFSET);
			}
			parentBlockIndex = parentIndex;
		}
		catch(Exception e) {
			error("Exception caught while writing header.",e);
			return(false);
		}

      isEmpty = false;
		return(true);
	}

	/**
	 * This method reads the Object stored in the body of this Block. 
	 * If the Object's data exceeds the size of this Block, the next block(s) in 
	 * the chain will be read from until the entire Object has been reconstructed.
	 * If the Object could not be read, null will be returned.
	 * @return Returns the Object stored in the Body of this Block, null if an error occurred or nothing was stored.
	 */
	public Object readObject()
	{
		Object rval = null;
		if(!isEmpty) {
			try {
				long length = store.readLong(getOffset()+headerLength+BODY_TAG_OFFSET);
				if(length==-3) {
					error("Cannot read an Object from a block at the end of a chain, index="+index);
				}
				else if(length==-2) {
					error("Cannot read an Object from a block in the middle of a chain, index="+index);
				}
				else if(length==-1) {
					length = store.readLong(getOffset()+headerLength+BODY_DATA_OFFSET);
					byte[] data = new byte[(int)length];
					byte[] blockData = store.readBytes(getOffset()+headerLength+BODY_EXTENDED_DATA_OFFSET,getOffset()+store.BLOCK_SIZE);
					System.arraycopy(blockData,0,data,0,blockData.length);
					if(nextBlock!=null) {
						if(nextBlock.readBytes(data,blockData.length)) {
							rval = ObjectSerializer.unserialize(data);
						}
						else {
							error("Cannot read the Object data in the next Block with index "+nextBlock.index+".");
						}
					}			
					else {
						int nextBlockIndex = store.readInt(getOffset()+HEADER_NEXT_INDEX_OFFSET);
						try {
							nextBlock = store.getBlock(nextBlockIndex);
						}
						catch(IndexOutOfBoundsException e) {
							nextBlock = null;
						}
						if(nextBlock!=null) {
							if(nextBlock.readBytes(data,blockData.length)) {
								rval = ObjectSerializer.unserialize(data);
							}
							else {
								error("Cannot read the Object data in the next Block with index "+nextBlock.index+".");
							}
						}			
						else {
							error("Cannot find the next Block with the remaining Object data, this index is: "+index);
						}
					}
				}
				else if(length>0 && length<(store.BLOCK_SIZE-(headerLength+BODY_DATA_OFFSET))) {
					//debug("Data length = "+length);
					byte[] data = store.readBytes(getOffset()+headerLength+BODY_DATA_OFFSET,getOffset()+headerLength+BODY_DATA_OFFSET+length);
					rval = ObjectSerializer.unserialize(data);
				}				
				else {
					rval = null;
				}
			}
			catch(Exception e) {
				error("Exception caught while reading object.",e);
			}
		}
		return(rval);
	}

	// This is called by the parent block when data is read.
	// This should only be called by FileObjectBlock
	boolean readBytes(byte[] data, int offset) 
	{
		boolean rval = false;
		try {
			long length = store.readLong(getOffset()+headerLength+BODY_TAG_OFFSET);
			if(length==-2) {
				length = store.readLong(getOffset()+headerLength+BODY_DATA_OFFSET);
				//debug("Data length = "+data.length+", block length = "+length+", offset = "+offset);

 
				if(data.length > length+offset) {
					byte[] blockData = store.readBytes(getOffset()+headerLength+BODY_EXTENDED_DATA_OFFSET,getOffset()+headerLength+BODY_EXTENDED_DATA_OFFSET+length);
					System.arraycopy(blockData,0,data,offset,blockData.length);
					if(nextBlock!=null) {
						if(nextBlock.readBytes(data,blockData.length+offset)) {
							rval = true;
						}
						else {
							error("Cannot read the Object data in the next Block with index "+nextBlock.index+".");
						}
					}			
					else {
						long nextBlockIndex = store.readLong(getOffset()+HEADER_NEXT_INDEX_OFFSET);
						try {
							nextBlock = store.getBlock((int)nextBlockIndex);
						}
						catch(IndexOutOfBoundsException e) {
							nextBlock = null;
						}
						if(nextBlock!=null) {
							if(nextBlock.readBytes(data,blockData.length+offset)) {
								rval = true;
							}
							else {
								error("Cannot read the Object data in the next Block with index "+nextBlock.index+".");
							}
						}			
						else {
							error("Cannot find the next Block with the remaining Object data, this index is: "+index);
						}
					}
				}
				else if(data.length==length+offset) {
					byte[] blockData = store.readBytes(getOffset()+headerLength+BODY_EXTENDED_DATA_OFFSET,getOffset()+headerLength+BODY_EXTENDED_DATA_OFFSET+length);
					System.arraycopy(blockData,0,data,offset,blockData.length);
					error("WARNING: Invalid block data header code: -2, should be -3. Attempting to correct...");
					if(nextBlock!=null) {
						nextBlock.dispose();
					}
					nextBlock=null;
					synchronized (WRITE_SYNC) {
						store.writeLong(-3,getOffset()+headerLength+BODY_TAG_OFFSET);
					}
					rval = true;
				}
				else if(data.length<length+offset) {
					byte[] blockData = store.readBytes(getOffset()+headerLength+BODY_EXTENDED_DATA_OFFSET,getOffset()+headerLength+BODY_EXTENDED_DATA_OFFSET+length);
					System.arraycopy(blockData,0,data,offset,data.length-offset);
					print("WARNING: Data length is less than the total length of bytes in chain. Tag=(-2), Index="+index);
					if(conservativeWithErrors) {
						error("Read failed! Corrupted Data Chain found! This index is: "+index);
						rval = false;
					}
					else {
						if(nextBlock!=null) {
							nextBlock.dispose();
						}
						nextBlock=null;
						synchronized (WRITE_SYNC) {
							store.writeLong(-3,getOffset()+headerLength+BODY_TAG_OFFSET);
						}
						rval = true;
					}
				} 
			}
			else if(length==-3) {
				length = store.readLong(getOffset()+headerLength+BODY_DATA_OFFSET);
				//debug("Data length = "+data.length+", block length = "+length+", offset = "+offset);

				// Data and datablock have aggreeing lengths
				if(data.length == length+offset) {
					byte[] blockData = store.readBytes(getOffset()+headerLength+BODY_EXTENDED_DATA_OFFSET,getOffset()+headerLength+BODY_EXTENDED_DATA_OFFSET+length);
					System.arraycopy(blockData,0,data,offset,blockData.length);
					rval = true;
				}
				// Data expects to be more but this block claims to have no more.
				else if(data.length > length+offset) {
					print("WARNING: Data length exceeds the total length of bytes in chain. Tag=(-3), Index="+index);

					// If this block is full then see if there is a next block.
					if(length == store.BLOCK_SIZE - (headerLength+BODY_EXTENDED_DATA_OFFSET)) {
						byte[] blockData = store.readBytes(getOffset()+headerLength+BODY_EXTENDED_DATA_OFFSET,getOffset()+headerLength+BODY_EXTENDED_DATA_OFFSET+length);
						System.arraycopy(blockData,0,data,offset,blockData.length);
						if(nextBlock!=null) {
							error("WARNING: Invalid block data header code: -3, should be -2. Attempting to correct.");
							synchronized (WRITE_SYNC) {
								store.writeLong(-2,getOffset()+headerLength+BODY_TAG_OFFSET);
							}
							if(nextBlock.readBytes(data,blockData.length+offset)) {
								rval = true;
							}
							else {
								error("Cannot read the Object data in the next Block with index "+nextBlock.index+".");
							}
						}			
						else {
							if(!conservativeWithErrors) {
								long nextBlockIndex = store.readLong(getOffset()+HEADER_NEXT_INDEX_OFFSET);
								try {
									nextBlock = store.getBlock((int)nextBlockIndex);
								}
								catch(IndexOutOfBoundsException e) {
									nextBlock = null;
								}
								if(nextBlock!=null) {
									error("WARNING: Invalid block data header code: -3, should be -2. Attempting to correct.");
									synchronized (WRITE_SYNC) {
										store.writeLong(-2,getOffset()+headerLength+BODY_TAG_OFFSET);
									}
									if(nextBlock.readBytes(data,blockData.length+offset)) {
										rval = true;
									}
									else {
										error("Cannot read the Object data in the next Block with index "+nextBlock.index+".");
									}
								}			
								else {
									error("Cannot find the next Block with the remaining Object data, this index is: "+index);
								}
							}
							else {
 								error("Read failed! Corrupted Data Chain found! This index is: "+index);
	 							rval = false;
							}
						}
					}
					// If block is NOT full, there must be a corrupted chain.
					else {
						error("Read failed! Corrupted Data Chain found! This index is: "+index);
						rval = false;
					}
				}
				// If data length is less than what this block claims it has.
				else if(data.length < length+offset) {
					print("WARNING: Data length is less than the total length of bytes in chain. Tag=(-3), Index="+index);
					if(conservativeWithErrors) {
						error("Read failed! Corrupted Data Chain found! This index is: "+index);
						rval = false;
					}
					else {
						byte[] blockData = store.readBytes(getOffset()+headerLength+BODY_EXTENDED_DATA_OFFSET,getOffset()+headerLength+BODY_EXTENDED_DATA_OFFSET+length);
						System.arraycopy(blockData,0,data,offset,data.length-offset);
						rval = true;
					}
				}
			}
			else {
				error("Invalid block data header code: "+length+", at block with index: "+index);
			}
		}
		catch(Exception e) {
			error("Exception caught while reading object.",e);
		}
		return(rval);
	}


	/**
	 * This method stores an Object in the body of this Block. If the Object's
	 * size exceeds the length of this Block, a new block will be allocated to 
	 * store the remaining bytes. If the Object was successfully written, true
	 * will be returned.
	 * @param data The Object to write in the body of this Block.
	 * @return Returns true IFF the object was written successfully.
	 */
	public boolean writeObject(Object data)
	{
		boolean rval = false;
		try {
			byte[] bytes = ObjectSerializer.serialize(data);
			long length = (long)bytes.length;
			if(length > (store.BLOCK_SIZE - (headerLength + BODY_DATA_OFFSET))) {
				byte[] thisBytes = new byte[(int)(store.BLOCK_SIZE - (headerLength + BODY_EXTENDED_DATA_OFFSET))];
				System.arraycopy(bytes,0,thisBytes,0,thisBytes.length);
				//debug("Writing -1 header flag. Bytes length = "+length);
				synchronized (WRITE_SYNC) {
					store.writeLong(-1,getOffset()+headerLength+BODY_TAG_OFFSET);
					store.writeLong((long)bytes.length,getOffset()+headerLength+BODY_DATA_OFFSET);
					store.writeBytes(thisBytes,getOffset()+headerLength+BODY_EXTENDED_DATA_OFFSET);
				}					
				if(nextBlock==null) {
					nextBlock = store.getNextFreeBlock();
				}
				if(nextBlock==null) {
					error("Cannot get a free block to write Object in, aborting.");
				}
				else {
					synchronized (WRITE_SYNC) {
						store.writeInt(nextBlock.index,getOffset()+HEADER_NEXT_INDEX_OFFSET);
					}
					if(nextBlock.writeHeader(index)) {
						if(nextBlock.writeObject(bytes,thisBytes.length)) {
							rval = true;
							isEmpty = false;
						}
						else {
							error("Cannot write Object data to next Block with index: "+nextBlock.index);
						}
					}
					else {
						error("Cannot write header data to next Block with index: "+nextBlock.index);
					}
				}
			}
			else {
				//debug("Writing length header. Bytes length = "+length);
				synchronized (WRITE_SYNC) {
					store.writeLong((long)bytes.length,getOffset()+headerLength+BODY_TAG_OFFSET);
					store.writeBytes(bytes,getOffset()+headerLength+BODY_DATA_OFFSET);
				}
				if(nextBlock!=null) {
					nextBlock.dispose();
					nextBlock=null;
				}
				rval = true;
				isEmpty = false;
			}
		}
		catch(Exception e) {
			error("Exception caught while writing Object data to file.",e);
		}
		return(rval);
	}

	// This is called by the parent block when an object is written
	// This method should not be called by anyone else.
	boolean writeObject(byte[] data, int offset)
	{
		boolean rval = false;
		try {
			long length = (long)data.length - offset;
			if(length > (store.BLOCK_SIZE - (headerLength+BODY_EXTENDED_DATA_OFFSET))) {
				byte[] thisBytes = new byte[(int)(store.BLOCK_SIZE - (headerLength + BODY_EXTENDED_DATA_OFFSET))];
				System.arraycopy(data,offset,thisBytes,0,thisBytes.length);
				//debug("Writing -2 header flag. Bytes length = "+data.length);
				synchronized (WRITE_SYNC) {
					store.writeLong(-2,getOffset()+headerLength+BODY_TAG_OFFSET);
					store.writeLong((long)thisBytes.length,getOffset()+headerLength+BODY_DATA_OFFSET);
					store.writeBytes(thisBytes,getOffset()+headerLength+BODY_EXTENDED_DATA_OFFSET);
				}					
				if(nextBlock==null) {
					nextBlock = store.getNextFreeBlock();
				}
				if(nextBlock==null) {
					error("Cannot get a free block to write Object in, aborting.");
				}
				else {
					synchronized (WRITE_SYNC) {
						store.writeInt(nextBlock.index,getOffset()+HEADER_NEXT_INDEX_OFFSET);
					}
					if(nextBlock.writeHeader(index)) {
						if(nextBlock.writeObject(data,(int)(thisBytes.length+offset))) {
							rval = true;
							isEmpty = false;
						}
						else {
							error("Cannot write Object data to next Block with index: "+nextBlock.index);
						}
					}
					else {
						error("Cannot write header data to next Block with index: "+nextBlock.index);
					}
				}
			}
			else {
				byte[] thisBytes = new byte[(int)(data.length-offset)];
				System.arraycopy(data,offset,thisBytes,0,thisBytes.length);
				//debug("Writing -3 header flag. Bytes length = "+length);
				synchronized (WRITE_SYNC) {
					store.writeLong(-3,getOffset()+headerLength+BODY_TAG_OFFSET);
					store.writeLong((long)thisBytes.length,getOffset()+headerLength+BODY_DATA_OFFSET);
					store.writeBytes(thisBytes,getOffset()+headerLength+BODY_EXTENDED_DATA_OFFSET);
				}
				if(nextBlock!=null) {
					nextBlock.dispose();
					nextBlock=null;
				}
				rval = true;
				isEmpty = false;
			}
		}
		catch(Exception e) {
			error("Exception caught while writing Object data to file.",e);
		}
		return(rval);
	}

				



	/**
	 * This method is called when the Block is no longer needed and should be considered 
	 * a FREE BLOCK, to be reused. This method will not reduce the size of the store file.
	 * This method will dispose the next block if one is allocated.
	 */
	public void dispose()
	{
		//debug("Disposing...");
		isEmpty = true;
		parentBlockIndex = -1;
		
		try {
			long tag = store.readLong(getOffset()+headerLength+BODY_TAG_OFFSET);
			if((tag==-1)||(tag==-2)) {
				if(nextBlock!=null) {
					try {
						nextBlock.dispose();
					}
					catch(Exception e) {
						error("Exception caught while disposing next Block.",e);
					}
					nextBlock = null;
				}
			}
		}
		catch(Exception e) {
			if(conservativeWithErrors) {
				nextBlock=null;
			}
			else {
				if(nextBlock!=null) {
					nextBlock.dispose();
					nextBlock=null;
				}
			}
		}

		try {
			synchronized (WRITE_SYNC) {
				store.writeLong(-2,getOffset()+HEADER_TAG_OFFSET);
			}
		}
		catch(IOException ioe) {
			error("Exception caught while writing dirty flag.",ioe);
			if(conservativeWithErrors) {
				return;
			}
		}
		store.freeBlocks.addElement(this);
	}


	/**
	 * This method writes the physical space of the store file so that
	 * Objects can be written and read from this Block. Doing this after
	 * writing information to this Block will destroy the previous data.
	 * This method also writes the dirty flag to the HEADER_TAG of this
	 * block's header, allowing it to be reused.
	 * @exception IOException Throws the IOException caused when writing to the store file.
	 */
	protected void format() throws IOException
	{
		//debug("Formatting...");
		byte[] data = new byte[(int)store.BLOCK_SIZE];
		synchronized (WRITE_SYNC) {
			store.writeBytes(data, getOffset()+HEADER_TAG_OFFSET);
			store.writeLong(-2,getOffset()+HEADER_TAG_OFFSET);
		}
		isEmpty = true;
	}


	/**
	 * This method reads the dirty flag in the store file for this Block
	 * and returns true if it reports itself to be FREE.
	 */
   public boolean isFreeBlock()
	{
		try {
			long headerTag = store.readLong(getOffset()+HEADER_TAG_OFFSET);
			return(headerTag == -2);
		}
		catch(IOException e) {
			error("Exception caught while reading dirty flag.",e);
		}
		return(!conservativeWithErrors);
	}

	/**
	 * This returns the dynamic offset of the beginning of this block in the store file
	 * @return Returns the offset starting from the beginning of the store file for the beginning of this block.
	 */
	protected long getOffset()
	{
		return(store.headerLength+(store.BLOCK_SIZE*index));
	}






	/**
	 * Overrides Object.finalize(). 
	 * This method makes sure that the store is unreferenced.
	 */
	@Deprecated
	protected void finalize() throws Throwable
 	{
		super.finalize();
		store = null;
	}
	
	/**
	 * Overrides Object.equals(Object).
	 * This makes the FileObjectStore and the local index an equivalence relation.
	 */
	public boolean equals(Object o)
	{
		boolean rval = false;

		if(o instanceof FileObjectBlock) {
			FileObjectBlock block = (FileObjectBlock)o;
			if(block.store.equals(this.store)) {
				if(block.index == this.index) {
					if(block.headerLength == this.headerLength) {
						rval = true;
					}
				}
			}
		}
		
		return(rval);
	}

	/**
	 * Overrides Object.hashCode().
	 * Uses the hashcode of the String constructed using the FileObjectStore and the local index.
	 */
	public int hashCode()
	{
		return((""+store+index).hashCode());
	}



	// Runtime Status methods...

	protected void print(String s)
	{
		if(verbose) {
			System.out.println("[FileObjectBlock("+index+")] "+s);
		}
	}

	protected void error(String s)
	{
		System.err.println("{FileObjectBlock("+index+")} ERROR: "+s);
	}

	protected void error(String s, Exception e)
	{
		System.err.println("{FileObjectBlock("+index+")} "+e+" : "+s);
		if(printStackTraces) {
			e.printStackTrace(System.err);
		}
	}

	protected void debug(String s)
	{
		if(debug) {
			System.out.println("[FileObjectBlock("+index+")] DEBUG: "+s);
		}
	}
}
