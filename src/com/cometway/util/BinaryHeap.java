
package com.cometway.util;

import java.util.*;


/**
 * BinaryHeap is a mostly vanilla implementation of the CLR binary heap (albeit an
 *   object-oriented implementation), with the addition of a Hashtable mapping
 *   IHeapItems to their respective indices in the heap Vector.  this allows for
 *   (nearly) constant time deletion of arbitrary IHeapItems at the cost of a
 *   bit of hashing (ohhh, sweet hash...)
 */

public class BinaryHeap implements IHeap
{		// this heap is stored as an array(Vector) of IHeapItems
	private Vector		heap;		// a hastable mapping IHeapItems to their respective heap Vector indices
	private Hashtable       indexMap;


	/**
	 * UNICONSTRUCTOR
	 */

	public BinaryHeap()
	{
		heap = new Vector();
		indexMap = new Hashtable();
	}


	/**
	 * insert: method adds a IHeapItem to this heap
	 */


	public synchronized boolean insert(IHeapItem newItem)
	{		// initially place the new item at the first free leaf in the heap
		heap.addElement(newItem);

		int		index = highestIndex();
		int		parentIndex = parent(index);
		IHeapItem       parent = (IHeapItem) heap.elementAt(parentIndex);		// shuffle the newItem up the heap from parent to parent until it is in


		// the proper spot

		while ((index > 0) && parent.greaterThan(newItem))
		{
			indexMap.remove(parent);
			indexMap.put(parent, Integer.valueOf(index));
			heap.setElementAt(parent, index);

			index = parentIndex;
			parentIndex = parent(index);
			parent = (IHeapItem) heap.elementAt(parentIndex);
		}		// JUST TO BE SURE...

		indexMap.remove(newItem);
		indexMap.put(newItem, Integer.valueOf(index));
		heap.setElementAt(newItem, index);

		if (indexMap.size() != heap.size())
		{
			System.out.println("HEAP: ERROR IN insert");
			print();
		}		// return true if there is a new min (there will be a new min if the


		// old min (at index 0) was removed)

		if (index == 0)
		{
			return true;
		}
		else
		{
			return false;
		}
	}


	/**
	 * delete
	 */


	public synchronized boolean delete(IHeapItem deleteItem)
	{
		boolean retval = false; /* {System.out.println("HEAP: BEGIN delete: " + deleteItem);
  System.out.println("HEAP: " + heap.toString() + " == " + heap.size() +
  ", MAP: " + indexMap.toString() + " == " + indexMap.size());} */

		if (heap.size() < 1)
		{
			throw new EmptyHeapException();
		}

		Integer indexInteger = (Integer) indexMap.remove(deleteItem);

		if (indexInteger != null)
		{
			int     index = indexInteger.intValue();
			int     highest = highestIndex(); /* now we have an empty node in the heap, so take a leaf (since our
			 *  heap is a Vector we need the last element) and put it at the empty
			 *  spot, then correct the heap (heapify) from there */

			if (heap.size() == 1)
			{
				heap.removeElementAt(0);
			}
			else
			{
				if (index != highest)
				{
					indexMap.remove(heap.elementAt(highest));
					indexMap.put(heap.elementAt(highest), indexInteger);
					heap.setElementAt(heap.elementAt(highest), index);
					heap.removeElementAt(highest);
					heapify(index);
				}
				else
				{
					heap.removeElementAt(highest);
				}
			}		// return true if there is a new min (there will be a new min if the


			// old min (at index 0) was removed)

			if (index == 0)
			{
				retval = true;
			}
		}

		if (indexMap.size() != heap.size())
		{
			System.out.println("HEAP: ERROR IN delete");
			print();
		}

		return retval;
	}


	/**
	 * findMin: method returns the minimum IHeapItem in this heap
	 */


	public synchronized IHeapItem findMin()
	{
		if (heap.size() < 1)
		{
			throw new EmptyHeapException();		// the min is always the root
		}

		return (IHeapItem) heap.elementAt(0);
	}


	/**
	 * deleteMin: method removes and returns the minimum IHeapItem in this heap
	 */


	public synchronized IHeapItem deleteMin()
	{
		if (heap.size() < 1)
		{
			throw new EmptyHeapException();		// the min is always the root
		}

		IHeapItem       minItem = (IHeapItem) heap.elementAt(0);

		indexMap.remove(minItem);		// now the root is gone, so we place a leaf at the root and re-heap the


		// heap with heapify on the root

		int     highest = highestIndex();

		if (heap.size() == 1)
		{
			heap.removeElementAt(0);
		}
		else
		{
			indexMap.remove(heap.elementAt(highest));
			indexMap.put(heap.elementAt(highest), Integer.valueOf(0));
			heap.setElementAt(heap.elementAt(highest), 0);
			heap.removeElementAt(highest);
			heapify(0);
		}

		if (indexMap.size() != heap.size())
		{
			System.out.println("HEAP: ERROR IN deleteMin");
			print();
		}

		return minItem;
	}


	/**
	 * isEmpty: method returns true if this heap contains no elements and false
    *   otherwise
	 */


	public synchronized boolean isEmpty()
	{
		return (heap.size() == 0);
	}


	/**
	 * empty: method returns true if this heap contains no elements and false
    *   otherwise.  
    *
    *  note: this version of isEmpty is retained for compatibility with
    *   old code
	 */


	public synchronized boolean empty()
	{
		return isEmpty();
	}


	/**
	 * size: method returns the number of elements in this heap
	 */


	public synchronized int size()
	{
		return heap.size();
	} /* heapify: given an index, heapify moves the IHeapItem at index down through
    *  the heap as necessary until the heap property is true for this heap.
    *  heapify assumes that the left and the right children of the IHeapItem at
    *  index satisfy the heap property (so the idea is that the IHeapItem at
    *  index may invalidate the heap property for this heap) */


	private void heapify(int index)
	{ /* {System.out.println("HEAP: BEGIN heapify: " + index);
 System.out.println("HEAP: " + heap.toString() + " == " + heap.size() +
						  ", MAP: " + indexMap.toString() + " == " + indexMap.size());} */
		if (index <= highestIndex())
		{
			int		smallestIndex = index;
			int		leftIndex = left(index);
			int		rightIndex = right(index);
			IHeapItem       smallItem = (IHeapItem) heap.elementAt(index); /* shuffle an invalid IHeapItem down to the proper place in the heap by
			 *  finding the minimum IHeapItem at each index (among the IHeapItems at
			 *  index and at the left and right children at that index) and
			 *  exchanging as necessary, then recursing on the exchanged child index */

			if ((leftIndex <= highestIndex()) && smallItem.greaterThan((IHeapItem) heap.elementAt(leftIndex)))
			{
				smallestIndex = leftIndex;
				smallItem = (IHeapItem) heap.elementAt(leftIndex);
			}

			if ((rightIndex <= highestIndex()) && smallItem.greaterThan((IHeapItem) heap.elementAt(rightIndex)))
			{
				smallestIndex = rightIndex;
			}

			if (smallestIndex != index)
			{
				IHeapItem       temp = (IHeapItem) heap.elementAt(index);

				indexMap.remove(heap.elementAt(smallestIndex));
				indexMap.put(heap.elementAt(smallestIndex), Integer.valueOf(index));
				heap.setElementAt(heap.elementAt(smallestIndex), index); /* this put is unnecessary because upon recursing, if this element
				 *  still invalidates the heap property of this heap, it will be
				 *  moved again.  so long as a put is done on the map at the base
				 *  case we are ok leaving this one out */		// indexMap.put(temp, new Integer(smallestIndex));
				heap.setElementAt(temp, smallestIndex);
				heapify(smallestIndex);
			}
			else
			{		// now we have to take care of that put that was avoided so many


				// times (this is one of the base cases)

				indexMap.remove(heap.elementAt(index));
				indexMap.put(heap.elementAt(index), Integer.valueOf(index));
			}
		}

		if (indexMap.size() != heap.size())
		{
			System.out.println("HEAP: ERROR IN heapify");
			print();
		}
	}


	private int highestIndex()
	{
		return (heap.size() - 1);
	}		// representing a tree with an array 101-A:


	private int parent(int index)
	{
		int     retval = (int) Math.ceil(index / 2.0);

		if (retval > 0)
		{
			retval--;
		}

		return retval;
	}


	private int left(int index)
	{
		return (2 * index) + 1;
	}


	private int right(int index)
	{
		return ((2 * index) + 2);
	} /* // representing a tree with an array 101-A:
   private int parent(int index)
   {
      Float retFloat = new Float((index / 2));

      return retFloat.intValue();
   }

   private int left(int index)
   {
      return (2 * index);
   }

   private int right(int index)
   {
      return ((2 * index) + 1);
   } */


	public void print()
	{
		System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
		System.out.println("VECTOR SIZE IS: " + heap.size());
		System.out.println("HASH   SIZE IS: " + indexMap.size());
	}


}

