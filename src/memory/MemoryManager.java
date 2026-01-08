package memory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import java.util.*;
import java.util.function.LongFunction;


public class MemoryManager {

	private int myNumberOfPages;
	private int myPageSize; // In bytes
	private int myNumberOfFrames;
	private int[] myPageTable; // -1 if page is not in physical memory
	private byte[] myRAM; // physical memory RAM
	private RandomAccessFile myPageFile;
	private int myNextFreeFramePosition = 0;
	private int myNumberOfpageFaults = 0;
	private int myPageReplacementAlgorithm = 0;
	Queue<Integer> queue = new LinkedList<>();
    LRUcache<Integer,Integer> LRU ;
	public MemoryManager(int numberOfPages, int pageSize, int numberOfFrames, String pageFile,
						 int pageReplacementAlgorithm) {
		LRU = new LRUcache<>(numberOfFrames);
		myNumberOfPages = numberOfPages;
		myPageSize = pageSize;
		myNumberOfFrames = numberOfFrames;
		myPageReplacementAlgorithm = pageReplacementAlgorithm;

		initPageTable();
		myRAM = new byte[myNumberOfFrames * myPageSize];

		try {

			myPageFile = new RandomAccessFile(pageFile, "r");

		} catch (FileNotFoundException ex) {
			System.out.println("Can't open page file: " + ex.getMessage());
		}
	}

	private void initPageTable() {
		myPageTable = new int[myNumberOfPages];
		for (int n = 0; n < myNumberOfPages; n++) {
			myPageTable[n] = -1;
		}
	}

	public byte readFromMemory(int logicalAddress) {
		int pageNumber = getPageNumber(logicalAddress);
		int offset = getPageOffset(logicalAddress);

		if (myPageTable[pageNumber] == -1) {
			pageFault(pageNumber);
		}

		int frame = myPageTable[pageNumber];
		LRU.get(pageNumber);
		int physicalAddress = frame * myPageSize + offset;
		byte data = myRAM[physicalAddress];

//		System.out.print("Virtual address: " + logicalAddress);
//		System.out.print(" Physical address: " + physicalAddress);
//		System.out.println(" Value: " + data);
		return data;
	}

	private int getPageNumber(int logicalAddress) {
		return logicalAddress / myPageSize;
	}

	private int getPageOffset(int logicalAddress) {
		return logicalAddress % myPageSize;
	}

	private void pageFault(int pageNumber) {
		if (myPageReplacementAlgorithm == Seminar3.NO_PAGE_REPLACEMENT)
			handlePageFault(pageNumber);

		if (myPageReplacementAlgorithm == Seminar3.FIFO_PAGE_REPLACEMENT)
			handlePageFaultFIFO(pageNumber);

		if (myPageReplacementAlgorithm == Seminar3.LRU_PAGE_REPLACEMENT)
			handlePageFaultLRU(pageNumber);

		readFromPageFileToMemory(pageNumber);
	}

	private void readFromPageFileToMemory(int pageNumber) {
		try {
			int frame = myPageTable[pageNumber];
//			Arrays.stream(myPageTable).forEach(System.out::println);
			myPageFile.seek(pageNumber * myPageSize);
			for (int b = 0; b < myPageSize; b++)
				myRAM[frame * myPageSize + b] = myPageFile.readByte();
		} catch (IOException ex) {

		}
	}

	public int getNumberOfPageFaults() {
		return myNumberOfpageFaults;
	}

	private void handlePageFault(int pageNumber) {
		// Implement by student in task one
		// This is the simple case where we assume same size of physical and logical
		// memory
		// nextFreeFramePosition is used to point to next free frame position
		myNumberOfpageFaults++;
		if (myNextFreeFramePosition >= myNumberOfFrames) {
			throw new IllegalStateException("No more physical memory left");
		}

		myPageTable[pageNumber] = myNextFreeFramePosition;
		myNextFreeFramePosition++;

	}

	private void handlePageFaultFIFO(int pageNumber) {
		myNumberOfpageFaults++;
		if (myNextFreeFramePosition < myNumberOfFrames) {
			myPageTable[pageNumber] = myNextFreeFramePosition;
			myNextFreeFramePosition++;
		} else if (!queue.isEmpty()) {
			int oldestPage = queue.poll();
			int oldMemoryAddress = myPageTable[oldestPage];
			myPageTable[oldestPage] = -1;
			myPageTable[pageNumber] = oldMemoryAddress;
		}
		queue.offer(pageNumber);
	}

	private void handlePageFaultLRU(int pageNumber) {
		myNumberOfpageFaults++;
		if (myNextFreeFramePosition < myNumberOfFrames){
			myPageTable[pageNumber] = myNextFreeFramePosition;
			LRU.put(pageNumber,myNextFreeFramePosition);
			myNextFreeFramePosition++;
			return;
		}
			int oldest = LRU.getEldest().getKey();
			LRU.remove(oldest);
			int oldMemoryAddress = myPageTable[oldest];
			myPageTable[oldest] = -1;
			myPageTable[pageNumber] = oldMemoryAddress;
			LRU.put(pageNumber,myNextFreeFramePosition);

	}


	class LRUcache<K, V> extends LinkedHashMap<K, V> {
		private Map.Entry<K,V> eldest;
		LRUcache(int capacity) {
			super(capacity, 0.75f, true);
		}
		public Map.Entry<K,V> getEldest(){
			return this.entrySet().iterator().next();
		}

	}
}