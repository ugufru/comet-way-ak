package com.cometway.io;

import java.io.*;

/**
 * This extension of InputStream is meant to be a J2ME compatible version of the J2SE
 * PushBackInputStream and BufferedInputStream. If you are running J2SE, you are better
 * off using java.io.BufferedInputStream and java.io.PushBackInputStream since they
 * work natively and this class does not.<p>
 *
 * This class requires a Thread to do that actual reading of the parent InputStream
 * and buffer it accordingly. It will block if the buffer is full and wait for read()
 * to be called to free up more buffer space. The number of times it will wait is
 * determined by the <b>attemptsBeforeFlushBuffer</b> field, which is by default set to
 * 1. The danger of waiting for read() to be called in order to free up buffer space is
 * incoming data through the parent InputStream may be lost. If this class waits the
 * proper number of times and the buffer is still full, 1/3 of the front of the buffer 
 * will be flushed (thus the data will be lost). Uses of this class must understand 
 * how the parent InputStream works and how data is to be read from this InputStream.
 * If data is sent quickly to the parent InputStream but data read from this class is
 * slow, a large buffer and many attemptsBeforeFlushBuffer may be required for things
 * to run smoothly. If it is expected that the calling thread of read() of this InputStream
 * occurs more frequently than data is written to the parent InputStream, a small buffer_size
 * and few attemptsBeforeFlushBuffer will suffice. If large number of bytes is expected
 * to be pushed back (unread()), a larger buffer_size may be required. The push back 
 * mechanism will flush whatever data is at the end of the buffer when data is pushed
 * back. The maximum memory usage for buffer arithemetic, where N is the buffer size, will
 * not exceed 3N and will always be at least (5/4)N. The <b>closeOnEOF</b> field tells
 * this InputStream to stop reading from the parent if it has reached EOF. Otherwise
 * the Thread will continue to block until PushBackBufferedInputStream.close() is called.
 *
 * @see java.io.BufferedInputStream
 * see also java.io.PushBackInputStream
 */
public class PushBackBufferedInputStream extends InputStream implements Runnable
{
	/** This is the default buffer size. This size is used if the buffer size is not provided */
	public static final int DEFAULT_BUFFER_SIZE = 10240;

	/** THis field tells the buffering mechanism how many times to wait for the buffer to be read from before it flushes the buffer */
	public int attemptsBeforeFlushBuffer;
	/** This field tells the buffering mechanism to stop reading from the parent InputStream if an EOF is encountered */
	public boolean closeOnEOF;

	// This is the local buffer
	private byte[] buffer;
	// This is the length of the buffer which contains valid data
	private int buffer_end;
	// This is the parent InputStream
	private InputStream in;
	// This is the object used for local synchronization
	private Object syncObject;
	// This is a local flag which tells the run() method to stop 
	private boolean stopRunning;
	// THis is the timeout used to Object.wait() calls.
	private int time_out;
	// This is the number of bytes to flush from the buffer when a flush is necessary
	private int flushSize;

	/**
	 * Construct a PushBackBufferedInputStream with the given parent InputStream, with
	 * a buffer whose size will be <b>buffer_size</b> and with a read() timeout of 
	 * <b>readTimeOut</b>.
	 */
	public PushBackBufferedInputStream(InputStream input, int buffer_size, int readTimeOut)
	{
		in = input;
		buffer = new byte[buffer_size];
		time_out = readTimeOut;
		buffer_end = 0;
		flushSize = (int)((buffer_size*2)/3);
		attemptsBeforeFlushBuffer = 1;
		closeOnEOF = true;
		syncObject = new Object();
	}

	/**
	 * Construct a PushBackBufferedInputStream with default parameters and the given
	 * parent InputStream.
	 */
	public PushBackBufferedInputStream(InputStream input)
	{
		this(input,DEFAULT_BUFFER_SIZE,0);
	}

	/**
	 * Read a single byte from the local buffer.
	 */
	public int read() throws IOException, InterruptedIOException
	{
		int rval = 0;
		try {
			if(buffer_end>0) {
				rval = ((int)buffer[0]);
				pullBuffer(1);
			}
			else {
				synchronized(syncObject) {
					syncObject.wait(time_out);
				}
				if(buffer_end>0) {
					rval = ((int)buffer[0]);
					pullBuffer(1);
				}
				else {
					synchronized(syncObject) {
						syncObject.wait(time_out);
						if(buffer_end>0) {
							rval = ((int)buffer[0]);
							pullBuffer(1);
						}
						else {
							//							System.out.println("buffer_end="+buffer_end);
							throw(new InterruptedIOException("Reading from this PushBackBufferedInputStream has timed out"));
						}
					}
				}
			}
		}
		catch(Exception e) {
			throw(new IOException(e.toString()));
		}				
		return(rval);
	}

	/**
	 * Read an array of bytes from the buffer (note: the number of bytes read will
	 * never exceed the size of the buffer).
	 * @return the number of bytes that were read.
	 */
	public int read(byte[] readBuffer) throws IOException
	{
		int rval = 0;
		try {
			int tmp = buffer_end;
			if(tmp>0) {
				if(tmp <= readBuffer.length) {
					System.arraycopy(buffer,0,readBuffer,0,tmp);
					rval = tmp;
					pullBuffer(tmp);
				}
				else {
					System.arraycopy(buffer,0,readBuffer,0,readBuffer.length);
					rval = readBuffer.length;
					pullBuffer(readBuffer.length);
				}
			}
		}
		catch(Exception e) {
			throw(new IOException(e.toString()));
		}
		return(rval);
	}

	/**
	 * Read an array of bytes form the buffer with a starting position and length. 
	 * (note: the number of bytes read will never exceed the size of the buffer).
	 * @return the number of bytes that were read.
	 */
	public int read(byte[] readBuffer, int start, int length) throws IOException
	{
		int rval = 0;
		try {
			int tmp = buffer_end;
			if(tmp>0) {
				if(tmp <= length) {
					System.arraycopy(buffer,0,readBuffer,start,tmp);
					rval = tmp;
					pullBuffer(tmp);
				}
				else {
					System.arraycopy(buffer,0,readBuffer,start,length);
					rval = length;
					pullBuffer(length);
				}
			}
		}
		catch(Exception e) {
			throw(new IOException(e.toString()));
		}
		return(rval);
	}

	/**
	 * Push a byte back into the buffer. If the buffer is currently full, the last byte
	 * in the buffer is flushed.
	 */
	public void unread(int b) throws IOException
	{
		try {
			synchronized(syncObject) {
				byte[] tmp = new byte[buffer.length];
				tmp[0] = (byte)b;
				System.arraycopy(buffer,0,tmp,1,buffer.length-1);
				buffer_end++;
				if(buffer_end>buffer.length) {
					buffer_end = buffer.length;
				}
				buffer = tmp;
			}
		}
		catch(Exception e) {
			throw(new IOException(e.toString()));
		}
	}
				
	/**
	 * Push an array of bytes back into the buffer. The bytes at the end of the buffer
	 * will be flushed (if the length of the array is greater than the amount of space
	 * left in the buffer).
	 */
	public void unread(byte[] b) throws IOException
	{
		try {
			synchronized(syncObject) {
				byte[] tmp = new byte[buffer.length];
				System.arraycopy(b,0,tmp,0,b.length);
				System.arraycopy(buffer,0,tmp,b.length,buffer.length-b.length);
				buffer_end = buffer_end + b.length;
				if(buffer_end>buffer.length) {
					buffer_end = buffer.length;
				}
				buffer = tmp;
			}
		}
		catch(Exception e) {
			throw(new IOException(e.toString()));
		}
	}

	/**
	 * Push an array of bytes back into the buffer. The bytes at the end of the buffer
	 * will be flushed (if the length of the array is greater than the amount of space
	 * left in the buffer).
	 */
	public void unread(byte[] b, int start, int length) throws IOException
	{
		try {
			synchronized(syncObject) {
				byte[] tmp = new byte[buffer.length];
				System.arraycopy(b,start,tmp,0,length);
				System.arraycopy(buffer,0,tmp,length,buffer.length-length);
				buffer_end = buffer_end + length;
				if(buffer_end>buffer.length) {
					buffer_end = buffer.length;
				}
				buffer = tmp;
			}
		}
		catch(Exception e) {
			throw(new IOException(e.toString()));
		}
	}







	/**
	 * This method reads bytes from the parent InputStream and buffers it accordingly.
	 * If the buffer is full, it will block until bytes are read from the buffer.
	 * If the buffer is STILL full, it will continue to block <b>attemptsBeforeFlushBuffer</b>
	 * times. After this, if the buffer is STILL full, 1/3 of the front of the buffer (the
	 * least recently read bytes) will be flushed and that data will be lost.
	 */
	public void run()
	{
		while(!stopRunning) {
			try {
				int i = in.available();
				//				System.out.println("DEBUG: I have "+i+" bytes available");

				// first we check if bytes are available
				if(i>0) {
					// bytes are available, read some
					byte[] tmp = new byte[(int)(buffer.length/4)];
					int readBytes = in.read(tmp);
					//					System.out.println("DEBUG: I read "+readBytes+" bytes");
					// check if we read any bytes
					if(readBytes>0) {
						byte[] tmp2 = new byte[buffer.length];
						// check if there is room in the buffer to store the bytes read
						if(buffer.length-buffer_end > readBytes) {
							synchronized(syncObject) {
								System.arraycopy(buffer,0,tmp2,0,buffer_end);
								System.arraycopy(tmp,0,tmp2,buffer_end,readBytes);
								buffer_end = buffer_end + readBytes;
								buffer = tmp2;
								//								System.out.println("DEBUG: buffer.length="+buffer.length+", buffer_end="+buffer_end);
								syncObject.notify();
							}
						}
						else {
							// no room in buffer, we need to block a little to see if the buffer gets read
							for(int z=0;z<attemptsBeforeFlushBuffer;z++) {
								synchronized(syncObject) {
									//									System.out.println("DEBUG: Buffer is filled... SYNCing and waiting for "+time_out+" ms");
									syncObject.wait(time_out);
								}
								if(stopRunning) {
									break;
								}
								if(buffer.length-buffer_end > readBytes) {
									break;
								}
							}
							if(stopRunning) {
								break;
							}
							// check again if there is room in the buffer to store the bytes read
							if(buffer.length-buffer_end > readBytes) {
								synchronized(syncObject) {
									System.arraycopy(buffer,0,tmp2,0,buffer_end);
									System.arraycopy(tmp,0,tmp2,buffer_end,readBytes);
									buffer_end = buffer_end + readBytes;
									//									System.out.println("DEBUG: buffer.length="+buffer.length+", buffer_end="+buffer_end);
									buffer = tmp2;
									syncObject.notify();
								}
							}
							else {
								// we have no choice but to flush some bytes from the buffer
								flushBuffer();
								synchronized(syncObject) {
									System.arraycopy(buffer,0,tmp2,0,buffer_end);
									System.arraycopy(tmp,0,tmp2,buffer_end,readBytes);
									buffer_end = buffer_end + readBytes;
									//									System.out.println("DEBUG: buffer.length="+buffer.length+", buffer_end="+buffer_end);
									buffer = tmp2;
									syncObject.notify();
								}
							}
						}
					}
				}
				else {
					// No bytes available, we try reading in just one byte and block on the read.
					//					System.out.println("DEBUG: No bytes available, blocking on read()");
					int b = in.read();
					if(closeOnEOF) {
						if(b==-1) {
							//							System.out.println("DEBUG: reached EOF, stopping");
							break;
						}
					}
					//					System.out.println("DEBUG: READ 1 byte");
					// check if there is room in the buffer to store this one byte
					if(buffer_end==buffer.length) {
						// no room in buffer, we wait a little to see if buffer gets read
						for(int z=0;z<attemptsBeforeFlushBuffer;z++) {
							synchronized(syncObject) {
								syncObject.wait(time_out);
							}
							if(stopRunning) {
								break;
							}
							if(buffer_end<buffer.length) {
								break;
							}
						}
						if(stopRunning) {
							break;
						}
						if(buffer_end<buffer.length) {
							synchronized(syncObject) {
								byte[] tmp = new byte[buffer.length];
								tmp[0] = (byte)b;
								System.arraycopy(buffer,0,tmp,1,buffer_end);
								buffer_end++;
								buffer = tmp;
								syncObject.notify();
							}
						}
						else {
							// we have no choice but to flush some of the bytes from the buffer
							flushBuffer();
							synchronized(syncObject) {
								byte[] tmp = new byte[buffer.length];
								tmp[0] = (byte)b;
								System.arraycopy(buffer,0,tmp,1,buffer_end);
								buffer_end++;
								buffer = tmp;
								syncObject.notify();
							}
						}
					}
					else {
						synchronized(syncObject) {
							byte[] tmp = new byte[buffer.length];
							tmp[0] = (byte)b;
							
							System.arraycopy(buffer,0,tmp,1,buffer_end);	
							buffer_end++;
							buffer = tmp;
							syncObject.notify();
						}
					}
				}
			}
			catch(Exception e) {	
				System.out.println("Error in run(): "+e);
				e.printStackTrace();
			}
		}
	}











	/**
	 * This method returns the number of available bytes that can be read from the buffer.
	 */
	public int available()
	{
		return(buffer_end);
	}

	/**
	 * This method stops the reading of the parent InputStream and causes the run() method
	 * to return. The parent InputStream will also be closed.
	 */
	public void close() throws IOException
	{
		stopRunning = true;
		try {
			synchronized(syncObject) {
				syncObject.notifyAll();
			}
		}
		catch(Exception e) {;}
		in.close();
	}
	
	/**
	 * This functionality is not supported.
	 */
	public void mark(int i)
	{
		// mark not supported
		;
	}

	/**
	 * THis funcitonality is not supported.
	 */
	public void reset()
	{
		// reset not supported
		;
	}

	/**
	 * This method will always return true
	 */
	public boolean markSupported()
	{
		return(false);
	}

	/**
	 * This method will cause the run() method to return, thus freeing the thread that this
	 * instance of PushBackBufferedInputStream was using. The parent InputStream will not be
	 * closed.
	 */
	public void stop()
	{
		stopRunning = true;
		try {
			synchronized(syncObject) {
				syncObject.notifyAll();
			}
		}
		catch(Exception e) {;}
	}





	private void pullBuffer(int length)
	{
		synchronized(syncObject) {
			byte[] tmp = new byte[buffer.length];
			System.arraycopy(buffer,length,tmp,0,buffer.length-length);
			buffer_end = buffer_end - length;
			buffer = tmp;
			syncObject.notify();
		}
	}

	private void flushBuffer()
	{
		//		System.out.println("DEBUG: I am FLUSHING THE BUFFER!");
		synchronized(syncObject) {
			byte[] tmp = new byte[buffer.length];
			System.arraycopy(buffer,(buffer.length-flushSize),tmp,0,flushSize);
			buffer = tmp;
			buffer_end = flushSize;
		}
		//		System.out.println("DEBUG: "+(buffer.length-flushSize)+" bytes flushed from buffer");
	}








	public static void main(String[] args)
	{
		try {
			//			File f = new File(args[0]);
			//			FileInputStream fis = new FileInputStream(f);
			//			java.net.Socket sock = new java.net.Socket("comet.tesuji.org",9999);
			//			PushBackBufferedInputStream pbbis = new PushBackBufferedInputStream(sock.getInputStream(),DEFAULT_BUFFER_SIZE,5000);
			//			pbbis.attemptsBeforeFlushBuffer = 10;
			//			Thread s = new Thread(pbbis);
			//			s.start();

			//			java.util.Random r = new java.util.Random();
			//			while(true) {
				//				System.out.println("BUFFER: "+(new String(pbbis.buffer,0,pbbis.buffer_end)));

				//				Thread.sleep(Math.abs(r.nextInt()%3)+2);
				//				byte[] tmp = new byte[1024];
				//				int bytesRead = pbbis.read(tmp);
				//				System.out.println("I read "+bytesRead+" bytes");
				//				System.out.println((new String(tmp,0,bytesRead)));

			//								int b = pbbis.read();
								//								System.out.println((char)b);

								//				if(r.nextInt()%5==0) {
								//					System.out.println("UNREADING "+bytesRead+" bytes");
								//					pbbis.unread(tmp,0,bytesRead);
								//				}
			//			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}




}
