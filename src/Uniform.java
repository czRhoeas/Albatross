import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Scanner;


/**
 * Uniform sampling of a large network.
 * The network takes the form of an
 * edge list. 
 * 
 * @author Vincent Labatut
 */
public class Uniform
{
	/////////////////////////////////////////////////////////////////
	// MAIN				/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	public static void main(String[] args) throws FileNotFoundException
	{	// set file names
		String path = "~/eclipse/workspaces/Networks/Orleans/data/";
		String networkFile = "xxxxx.edgelist";
		String sampleFile = "sample.txt";
		
		// read sample
		List<Long> sample = loadSample(sampleFile);
		
		// open input file
		String inFile = path + networkFile;
		Scanner scanner = openInputFile(inFile);
		
		// open output file
		String outFile = path + networkFile + ".sampled";
		PrintWriter printWriter = openOutputFile(outFile);

		// date formatting stuff
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Calendar cal = Calendar.getInstance();
		
		// process file
		System.out.println("Extracting sample from network");
		int count = 0;
		while(scanner.hasNextLine())
		{	if(count%100000==0)
				System.out.println(".. "+dateFormat.format(cal.getTime())+": "+count+" nodes processed");
			String line = scanner.nextLine();
			String[] str = line.split("\t");
			long from = Long.parseLong(str[0]);
			if(sample.contains(from))
			{	long to = Long.parseLong(str[1]);
				if(sample.contains(to))
				{	printWriter.print(line);
				}
			}
		}
		
		// close streams
		scanner.close();
		printWriter.close();
	}

	/**
	 * Reads the file containing the numbers of
	 * the nodes to be sampled.
	 * 
	 * @param file
	 * 		Sample file.
	 * @return
	 * 		List containing the sampled nodes.
	 * 
	 * @throws FileNotFoundException
	 * 		Problem while accessing the file.
	 */
	private static List<Long> loadSample(String file) throws FileNotFoundException
	{	System.out.println("Reading sample");
		Scanner scanner = openInputFile(file);
		List<Long> result = new ArrayList<Long>();
		while(scanner.hasNextLong())
		{	long value = scanner.nextLong();
			result.add(value);
		}
		scanner.close();
		System.out.println("..Number of values read: "+result.size());
		return result;
	}
	
	/**
	 * Open a file in read mode.
	 * 
	 * @param file
	 * 		File to be opened.
	 * @return
	 * 		Scanner object pointing at the begining of the file.
	 * 
	 * @throws FileNotFoundException
	 * 		Problem while accessing the file.
	 */
	private static Scanner openInputFile(String file) throws FileNotFoundException
	{	FileInputStream fileIn = new FileInputStream(file);
		BufferedInputStream buffIn = new BufferedInputStream(fileIn);
		InputStreamReader reader = new InputStreamReader(buffIn);
		Scanner result = new Scanner(reader);
		return result;
	}
	
	/**
	 * Open a file in write mode.
	 * 
	 * @param file
	 * 		File to be opened.
	 * @return
	 * 		PrintWriter object pointing on an empty file.
	 * 
	 * @throws FileNotFoundException
	 * 		Problem while accessing the file.
	 */
	private static PrintWriter openOutputFile(String file) throws FileNotFoundException
	{	FileOutputStream fileOut = new FileOutputStream("resources/exemple.dat");
		BufferedOutputStream buffOut = new BufferedOutputStream(fileOut);
		OutputStreamWriter writer = new OutputStreamWriter(buffOut);
		PrintWriter result = new PrintWriter(writer);
		return result;
	}
}
