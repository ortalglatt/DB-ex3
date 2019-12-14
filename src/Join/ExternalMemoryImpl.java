package Join;

import java.io.*;

import java.util.*;

public class ExternalMemoryImpl extends IExternalMemory {

	final private int BLOCK_SIZE = 4096;
	final private double FREE_MEM_PERCENT = 0.2;
	final private int ID_END = 10;
	final private int STR1_START = 11;
	private Runtime run = Runtime.getRuntime();
	final private String TMP_SORTED_1 = "sorted1.txt";
	final private String TMP_SORTED_2 = "sorted2.txt";

	/**
	 * Read as much lines as possible from the given buffer. If the given substr isn't empty, it will return
	 * only the line that their first column contains the substr.
	 * @param inBuf buffer to read from.
	 * @param substr sub-string to check if the first column in each line contains.
	 * @return An array of all the lines it read from the buffer.
	 * @throws IOException If their was an IO problem.
	 */
	private List<String> readLines(BufferedReader inBuf, String substr) throws IOException {
		List<String> lines = new ArrayList<>();
		run.gc();
		while (run.freeMemory() / (double) run.totalMemory() > FREE_MEM_PERCENT)
		{
			String line = inBuf.readLine();
			if (line == null)
			{
				break;
			}
			if (substr.equals(""))
			{
				lines.add(line);
			}
			else
			{
				String firstCol = line.substring(0, ID_END);
				if (firstCol.contains(substr))
				{
					lines.add(line);
				}
			}
		}
		return lines;
	}

	/**
	 * write the given lines in the given tmpFilePath.
	 * @param tmpFilePath A path to the file to write the lines in.
	 * @param lines An array of lines to write in the given tmpFilePath.
	 * @throws IOException If their was an IO problem.
	 */
	private void createTmpFile(String tmpFilePath, List<String> lines) throws IOException {
		BufferedWriter tmpBuf = new BufferedWriter(new FileWriter(tmpFilePath));
		for (String line: lines)
		{
			tmpBuf.write(line + "\n");
		}
		tmpBuf.flush();
		tmpBuf.close();
	}

	/**
	 * Finds the index of the line that need to appear first in the sorted file.
	 * @param lines Array of lines to check.
	 * @return The index of the line that will be first.
	 */
	private int findMinIdx(List<String> lines) {
		int minIdx = 0;
		while (minIdx < lines.size() - 1 && lines.get(minIdx) == null)
		{
			minIdx += 1;
		}
		for (int i = minIdx + 1; i < lines.size(); i++)
		{
			if (lines.get(i) == null)
			{
				continue;
			}
			if (lines.get(minIdx).compareTo(lines.get(i)) > 0)
			{
				minIdx = i;
			}
		}
		return minIdx;
	}

	/**
	 * Merges all the lines in tmpBuffers to one file that will be written by the given outBuf.
	 * @param outBuf Buffer that will write the result.
	 * @param tmpBuffers An array of buffers to merge from.
	 * @throws IOException If their was an IO problem.
	 */
	private void merge(BufferedWriter outBuf, List<BufferedReader> tmpBuffers) throws IOException {
		List<String> curLines = new ArrayList<>();
		for (BufferedReader tmpBuf: tmpBuffers)
		{
			curLines.add(tmpBuf.readLine());
		}
		int minIdx = findMinIdx(curLines);
		while (curLines.get(minIdx) != null)
		{
			outBuf.write(curLines.get(minIdx) + "\n");
			curLines.set(minIdx, tmpBuffers.get(minIdx).readLine());
			minIdx = findMinIdx(curLines);
		}
		for (BufferedReader tmpBuf: tmpBuffers)
		{
			tmpBuf.close();
		}
		outBuf.close();
	}

	/**
	 * Create tmpBufferes from the given temporary filenames array - tmpFiles.
	 * @param tmpFiles An array of string to create reader buffers from.
	 * @return An array of the buffers that were created.
	 * @throws FileNotFoundException If one od the given filenames doesn't exist.
	 */
	private List<BufferedReader> createTmpBuffers(List<String> tmpFiles) throws FileNotFoundException {
		List<BufferedReader> tmpBuffers = new ArrayList<>();
		for (String tmpFile: tmpFiles)
		{
			FileReader tmpReader = new FileReader(tmpFile);
			tmpBuffers.add(new BufferedReader(tmpReader, BLOCK_SIZE));
		}
		return tmpBuffers;
	}

	/**
	 * Delete all the files in the given array.
	 * @param tmpFiles An array of filenames to delete.
	 */
	private void deleteFiles(List<String> tmpFiles)
	{
		for (String tmpFile: tmpFiles)
		{
			(new File(tmpFile)).deleteOnExit();
		}
	}

	/**
	 * Sorts the in file and save the result in the out file. If the substr isn't empty, it will sort only
	 * lines that their first column contains the substr.
	 * @param in Filename to sort.
	 * @param out Filename to save the sorted file.
	 * @param tmpPath Path to a directory to save the temporary files.
	 * @param substr sub-string to check if the first column in each line contains. if it's empty - sort all
	 *               lines, otherwise - sort only the lines that contains the substr.
	 * @throws IOException If their was an IO problem.
	 */
	private void sortAndSelect(String in, String out, String tmpPath, String substr) throws IOException {
		BufferedReader inBuf = new BufferedReader(new FileReader(in));
		List<String> tmpFiles = new ArrayList<>();
		int tmpCounter = 0;
		while (inBuf.ready())
		{
			List<String> lines = readLines(inBuf, substr);
			Collections.sort(lines);
			String tmpFilePath = tmpPath + "\\" + tmpCounter + ".txt";
			tmpCounter++;
			createTmpFile(tmpFilePath, lines);
			tmpFiles.add(tmpFilePath);
		}
		inBuf.close();

		BufferedWriter outBuf = new BufferedWriter(new FileWriter(out), BLOCK_SIZE);
		List<BufferedReader> tmpBuffers = createTmpBuffers(tmpFiles);
		merge(outBuf, tmpBuffers);
		deleteFiles(tmpFiles);
	}

	/**
	 * Sorts the in file and save the result in the out file.
	 * @param in Filename to sort.
	 * @param out Filename to save the sorted file.
	 * @param tmpPath Path to a directory to save the temporary files.
	 */
	@Override
	public void sort(String in, String out, String tmpPath) {
		try
		{
			sortAndSelect(in, out, tmpPath, "");
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Join the two given files (in1, in2) that are sorted, and save the result in the out file.
	 * @param in1 The first sorted filename to join.
	 * @param in2 The second sorted filename to join.
	 * @param out Filename to save the joined file.
	 * @param tmpPath Path to a directory to save the temporary files.
	 */
	@Override
	protected void join(String in1, String in2, String out, String tmpPath) {
		try
		{
			BufferedReader in1Buf = new BufferedReader(new FileReader(in1));
			BufferedReader in2Buf = new BufferedReader(new FileReader(in2));
			BufferedWriter outBuf = new BufferedWriter(new FileWriter(out));
			String line1 = in1Buf.readLine();
			String line2 = in2Buf.readLine();
			while (line1 != null && line2 != null)
			{
				String lastId = line1.substring(0, ID_END);
				while (line2 != null && lastId.equals(line2.substring(0, ID_END)))
				{
					outBuf.write(line1 + " " + line2.substring(STR1_START) + "\n");
					line2 = in2Buf.readLine();
				}
				line1 = in1Buf.readLine();
			}
			in1Buf.close();
			in2Buf.close();
			outBuf.flush();
			outBuf.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Selects from the in filename the lines that their first column contain the substrSelect, and save it
	 * in the out file.
	 * @param in Filename to select from.
	 * @param out Filename to save the selected file.
	 * @param substrSelect Sub-string to check if the first column in each line contains.
	 * @param tmpPath Path to a directory to save the temporary files.
	 */
	@Override
	protected void select(String in, String out, String substrSelect, String tmpPath) {
		try
		{
			BufferedReader inBuf = new BufferedReader(new FileReader(in));
			BufferedWriter outBuf = new BufferedWriter(new FileWriter(out));
			String line = inBuf.readLine();
			while (line != null)
			{
				String firstCol = line.substring(0, ID_END);
				if (firstCol.contains(substrSelect))
				{
					outBuf.write(line + "\n");
				}
				line = inBuf.readLine();
			}
			outBuf.flush();
			outBuf.close();
			inBuf.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Sort both in files (in1, in2) with the substr condition and than join them. It will save the result in
	 * the out file.
	 * @param in1 The first filename.
	 * @param in2 The second filename .
	 * @param out Filename to save the selected file.
	 * @param substrSelect ub-string to check if the first column in each line contains.
	 * @param tmpPath Path to a directory to save the temporary files.
	 */
	@Override
	public void joinAndSelectEfficiently(String in1, String in2, String out,
			String substrSelect, String tmpPath) {
		try
		{
			String sorted1 = tmpPath + "\\" + TMP_SORTED_1;
			String sorted2 = tmpPath + "\\" + TMP_SORTED_2;
			sortAndSelect(in1, sorted1, tmpPath, substrSelect);
			sortAndSelect(in2, sorted2, tmpPath, substrSelect);
			join(sorted1, sorted2, out, tmpPath);
			(new File(sorted1)).deleteOnExit();
			(new File(sorted2)).deleteOnExit();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}

