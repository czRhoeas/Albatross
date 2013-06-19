// Albatross Sampling Algorithm in HotPlanet'11
// Author: Long Jin(Tsinghua University)

// Input:  1st line is the number of vertices
//         2nd line is the number of edges
//         From 3rd line, edge information is followed in the format (FromNode, EndNode)
// Output: CDF, NMSE of Degree Distribution
//         Mixing Time

// Init(): Initialize the parameters in graph sampling process
// MHRW(): Implement Metropolis-Hasting Random Walk algorithm
// BFS():  Implement Breadth-First Sampling
// AS():   Implement Albatross Sampling

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Scanner;

/**
 * Albatross Sampling Algorithm.
 * This is a straight conversion from
 * the C# source code available at <a href="https://code.google.com/p/sampling-social-graphs/">this page</a>.
 * <br/>
 * <b>Note:</b> in the input file, node numbering
 * must start from 0.
 * 
 * @author Long Jin (Original C# source code)
 * @author Vincent Labatut (Java port)
 */
class AlbatrossSampling
{
	static List<List<Integer>> outLinks;		// Original Graph: out degree
	static List<List<Integer>> inLinks;			// Original Graph: in degree
	static List<List<Integer>> allLinks;   		// Original Graph -> Undirected Graph
	static double[] percentIn;					// True Value
	static double[] percentOut;
	static double[] percent1In;					// CDF
	static double[] percent1Out;
	static double[] percent2In;					// NMSE
	static double[] percent2Out;
	static double[] percent3In;					// Each Time's Estimation
	static double[] percent3Out;
	static boolean[] node;
	static int maxDegreeIn;
	static int maxDegreeOut;
	static int nodeNumber;
	static int realNodeNumber = 0;
	static int edgeNumber;
	static int simulation = 1;					// TODO number of repetitions of the sampling process (1000 in the original version)
	static int sampleSize;
	static double alpha = 0.02;					// Jump Probability in AS
	static int jumpBudget = 10;					// Set Jump-Cost
//	static String path = "data/";				// TODO Fill in the file path
	static String path = "/home/vlabatut/eclipse/workspaces/Extraction/Database/googleplus/";
//	static String filename = "kdd03.txt";		// TODO Fill in the file name
	static String filename = "giantcomp.network";
//	static int sizeFactor = 20;				// TODO size of the original network divided by this value (20 in the original version)
	static int sizeFactor = 1000;

	private static void Init() throws FileNotFoundException
	{
		System.out.println("Loading "+path);
		FileInputStream fileIn = new FileInputStream(path + filename);
		InputStreamReader reader = new InputStreamReader(fileIn);
		Scanner sr = new Scanner(reader);
		
		String str = sr.nextLine();
str = str.split(" ")[1];
sr.nextLine();
		nodeNumber = Integer.parseInt(str);
//		edgeNumber = Integer.parseInt(sr.nextLine());
		outLinks = new ArrayList<List<Integer>>(nodeNumber);
		inLinks = new ArrayList<List<Integer>>(nodeNumber);
		allLinks = new ArrayList<List<Integer>>(nodeNumber);
		node = new boolean[nodeNumber + 1];
		for (int i = 0; i < nodeNumber; i++)
		{
			outLinks.add(new ArrayList<Integer>());
			inLinks.add(new ArrayList<Integer>());
			allLinks.add(new ArrayList<Integer>());
		}
//		final String splitFlag = "\t";
final String splitFlag = " ";
		int fromNode = 0, toNode = 0;
		int edgeCount1 = 0;
		int edgeCount2 = 0;
//		while (edgeCount1+edgeCount2 < edgeNumber)
while (sr.hasNextLine())
		{
edgeNumber++;
if(edgeNumber%100000==0)
	System.out.println("..edges loaded: "+edgeNumber);
			str = sr.nextLine();
			String[] split = str.split(splitFlag);
			fromNode = Integer.parseInt(split[0]);
			toNode = Integer.parseInt(split[1]);
			if (fromNode == toNode)
			{	
				edgeCount2++;
				continue;
			}
			outLinks.get(fromNode).add(toNode);
			inLinks.get(toNode).add(fromNode);
			if (!allLinks.get(fromNode).contains(toNode))
				allLinks.get(fromNode).add(toNode);
			if (!allLinks.get(toNode).contains(fromNode))
				allLinks.get(toNode).add(fromNode);
			node[fromNode] = true;
			node[toNode] = true;
			edgeCount1++;
		}
		sr.close();

		for (int i = 0; i < nodeNumber; i++)
		{
			if (node[i] == true)
				realNodeNumber++;
		}
		sampleSize = realNodeNumber / sizeFactor;  // Set Total-Cost

		maxDegreeIn = 0;
		maxDegreeOut = 0;
		for (int i = 0; i < nodeNumber; i++)
		{
			if (inLinks.get(i).size() > maxDegreeIn)
				maxDegreeIn = inLinks.get(i).size();
			if (outLinks.get(i).size() > maxDegreeOut)
				maxDegreeOut = outLinks.get(i).size();
		}
		percentIn = new double[maxDegreeIn + 1];
		percentOut = new double[maxDegreeOut + 1];
		percent1In = new double[maxDegreeIn + 1];
		percent1Out = new double[maxDegreeOut + 1];
		percent2In = new double[maxDegreeIn + 1];
		percent2Out = new double[maxDegreeOut + 1];
		percent3In = new double[maxDegreeIn + 1];
		percent3Out = new double[maxDegreeOut + 1];
		for (int i = 0; i < nodeNumber; i++)
		{
			percentIn[inLinks.get(i).size()] = percentIn[inLinks.get(i).size()] + 1;
			percentOut[outLinks.get(i).size()] = percentOut[outLinks.get(i).size()] + 1;
		}

		percentIn[0] = percentIn[0] - (nodeNumber - realNodeNumber);
		percentOut[0] = percentOut[0] - (nodeNumber - realNodeNumber);
		percentIn[0] = percentIn[0] / (double)realNodeNumber;
		percentOut[0] = percentOut[0] / (double)realNodeNumber;

		for (int i = 1; i <= maxDegreeIn; i++)
		{
			percentIn[i] = percentIn[i] / (double)realNodeNumber;
			percentIn[i] = percentIn[i] + percentIn[i - 1];
		}
		for (int i = 1; i <= maxDegreeOut; i++)
		{
			percentOut[i] = percentOut[i] / (double)realNodeNumber;
			percentOut[i] = percentOut[i] + percentOut[i - 1];
		}

		System.out.println("Test File: " + filename);
		System.out.println("Test Path: " + path);
		System.out.println("Average Degree = " + (double)edgeCount1 / (double)realNodeNumber);
		System.out.println("Simulation Times = " + simulation);
		System.out.println("Node Number = " + nodeNumber);
		System.out.println("Real Node Number = " + realNodeNumber);
		System.out.println("Sample Budget = " + sampleSize);
		System.out.println("Jump Alpha = " + alpha);
		System.out.println("");

		FileOutputStream fileOut = new FileOutputStream(path + "Original_graph_in_degree_distribution.txt");
		OutputStreamWriter writer = new OutputStreamWriter(fileOut);
		PrintWriter pw = new PrintWriter(writer);
		for (int i = 0; i < maxDegreeIn; i++)
		{
			pw.println(Double.toString(percentIn[i]));
		}
		pw.close();

		fileOut = new FileOutputStream(path + "Original_graph_out_degree_distribution.txt");
		writer = new OutputStreamWriter(fileOut);
		pw = new PrintWriter(writer);
		for (int i = 0; i < maxDegreeOut; i++)
		{
			pw.println(Double.toString(percentOut[i]));
		}
		pw.close();
		System.out.println("Loading complete ("+path+")");
	}

	private static void MHRW() throws FileNotFoundException
	{
		Random ra = new Random();
		Queue<Integer> sampledNodes = new LinkedList<Integer>();
		Queue<Integer> queryNodes = new LinkedList<Integer>();
		int w = 0;
		int v = 0;
		int sampleNodeNumber = sampleSize;
		int i = 0; 
		double avgDegree = 0.0;
		double avgDegreeIn = 0.0;
		double temp = 0.0;
		double tempIn = 0.0;
		int count;
		int singleSample = 0;
		int totalSample = 0;
		for (int m = 0; m < maxDegreeIn; m++)
		{
			percent1In[m] = 0.0;
			percent2In[m] = 0.0;
			percent3In[m] = 0.0;
		}
		for (int m = 0; m < maxDegreeOut; m++)
		{
			percent1Out[m] = 0.0;
			percent2Out[m] = 0.0;
			percent3Out[m] = 0.0;
		}

		int[] mixingTimeIn = new int[simulation];
		int[] mixingTimeOut = new int[simulation];
		for(int m=0;m<simulation;m++)
		{
			mixingTimeIn[m] = sampleNodeNumber;
			mixingTimeOut[m] = sampleNodeNumber;
		}
		int mix = 0;

		for (count = 0; count < simulation; count++)
		{
			i = 0;
			singleSample = 0;
			v = ra.nextInt(Integer.MAX_VALUE) % allLinks.size();
			for (int m = 0; m < maxDegreeIn+1; m++)
			{
				percent3In[m] = 0.0;
			}
			for (int m = 0; m < maxDegreeOut+1; m++)
			{
				percent3Out[m] = 0.0;
			}
			while (i < sampleNodeNumber)
			{
				if (allLinks.get(v).size() == 0)
				{
					v = ra.nextInt(Integer.MAX_VALUE) % (allLinks.size());
					singleSample++;
					sampledNodes.offer(v);
					if (!queryNodes.contains(v))
					{
						i++;
						queryNodes.offer(v);
					}
					for (int m = inLinks.get(v).size(); m <= maxDegreeIn; m++)
					{
						percent3In[m] = percent3In[m] + 1.0;
					}
					for (int m = outLinks.get(v).size(); m <= maxDegreeOut; m++)
					{
						percent3Out[m] = percent3Out[m] + 1.0;
					}
					mix = 0;
					for (mix = 0; mix < maxDegreeIn + 1; mix++)
					{
						if (Math.abs(percent3In[mix] / singleSample - percentIn[mix]) > 0.25)
							break;
					}
					if (mix == maxDegreeIn + 1 && mixingTimeIn[count] == sampleNodeNumber)
						mixingTimeIn[count] = i;
					else if (mix != maxDegreeIn + 1)
						mixingTimeIn[count] = sampleNodeNumber;

					mix = 0;
					for (mix = 0; mix < maxDegreeOut + 1; mix++)
					{
						if (Math.abs(percent3Out[mix] / singleSample - percentOut[mix]) > 0.25)
							break;
					}
					if (mix == maxDegreeOut + 1 && mixingTimeOut[count] == sampleNodeNumber)
						mixingTimeOut[count] = i;
					else if (mix != maxDegreeOut + 1)
						mixingTimeOut[count] = sampleNodeNumber;
					continue;
				}
				w = allLinks.get(v).get(ra.nextInt(allLinks.get(v).size()));
				double p = ra.nextDouble();
				if (p <= (double)allLinks.get(v).size() / (double)allLinks.get(w).size())
				{
					v = w;
					singleSample++;
					sampledNodes.offer(v);
				}
				else
				{
					singleSample++;
					sampledNodes.offer(v);
				}

				if (!queryNodes.contains(w))
				{
					i++;
					queryNodes.offer(w);
				}

				for (int m = inLinks.get(v).size(); m <= maxDegreeIn; m++)
				{
					percent3In[m] = percent3In[m] + 1.0;
				}
				for (int m = outLinks.get(v).size(); m <= maxDegreeOut; m++)
				{
					percent3Out[m] = percent3Out[m] + 1.0;
				}
				mix = 0;
				for (mix = 0; mix < maxDegreeIn + 1; mix++)
				{
					if (Math.abs(percent3In[mix] / singleSample - percentIn[mix]) > 0.25)
						break;
				}
				if (mix == maxDegreeIn + 1 && mixingTimeIn[count] == sampleNodeNumber)
					mixingTimeIn[count] = i;
				else if (mix != maxDegreeIn + 1)
					mixingTimeIn[count] = sampleNodeNumber;

				mix = 0;
				for (mix = 0; mix < maxDegreeOut + 1; mix++)
				{
					if (Math.abs(percent3Out[mix] / singleSample - percentOut[mix]) > 0.25)
						break;
				}
				if (mix == maxDegreeOut + 1 && mixingTimeOut[count] == sampleNodeNumber)
					mixingTimeOut[count] = i;
				else if (mix != maxDegreeOut + 1)
					mixingTimeOut[count] = sampleNodeNumber;
			}
			totalSample = singleSample + totalSample;

			for (int m = 0; m < maxDegreeIn+1; m++)
			{
				percent3In[m] = percent3In[m] / (double)singleSample;
				percent1In[m] = percent1In[m] + percent3In[m];
				percent2In[m] = percent2In[m] + (percent3In[m] - percentIn[m]) * (percent3In[m] - percentIn[m]);
			}
			for (int m = 0; m < maxDegreeOut+1; m++)
			{
				percent3Out[m] = percent3Out[m] / (double)singleSample;
				percent1Out[m] = percent1Out[m] + percent3Out[m];
				percent2Out[m] = percent2Out[m] + (percent3Out[m] - percentOut[m]) * (percent3Out[m] - percentOut[m]);
			}
	
// TODO on the first iteration, we record the sampled subnetwork
if(count==0)
	exportSampledNetworkAsPajek("MHRW",outLinks,sampledNodes);
			
			sampledNodes.clear();
			queryNodes.clear();
		}
		/* 
		 * TODO bug: temp is never updated. 
		 * Same thing in other functions. 
		 * And for temp_in, too.
		 */ 
		avgDegree = temp / (double)(totalSample);			 
		avgDegreeIn = tempIn / (double)(totalSample);

		for (int m = 0; m < maxDegreeIn; m++)
		{
			percent1In[m] = percent1In[m] / (double)(simulation);
			if (percentIn[m] != 0)
				percent2In[m] = Math.sqrt(percent2In[m] / (double)(simulation)) / percentIn[m];
			else
				percent2In[m] = 0;
		}
		for (int m = 0; m < maxDegreeOut; m++)
		{
			percent1Out[m] = percent1Out[m] / (double)(simulation);
			if (percentOut[m] != 0)
				percent2Out[m] = Math.sqrt(percent2Out[m] / (double)(simulation)) / percentOut[m];
			else
				percent2Out[m] = 0;
		}

		FileOutputStream fileOut = new FileOutputStream(path + "MHRW_in_degree_distribution.txt");
		OutputStreamWriter writer = new OutputStreamWriter(fileOut);
		PrintWriter pw = new PrintWriter(writer);
		for (int m = 0; m < maxDegreeIn; m++)
		{
			pw.println(Double.toString(percent1In[m]));
		}
		pw.close();

		fileOut = new FileOutputStream(path + "MHRW_out_degree_distribution.txt");
		writer = new OutputStreamWriter(fileOut);
		pw = new PrintWriter(writer);
		for (int m = 0; m < maxDegreeOut; m++)
		{
			pw.println(Double.toString(percent1Out[m]));
		}
		pw.close();

		fileOut = new FileOutputStream(path + "MHRW_in_degree_NMSE.txt");
		writer = new OutputStreamWriter(fileOut);
		pw = new PrintWriter(writer);
		for (int m = 0; m < maxDegreeIn; m++)
		{
			pw.println(Double.toString(percent2In[m]));
		}
		pw.close();

		fileOut = new FileOutputStream(path + "MHRW_out_degree_NMSE.txt");
		writer = new OutputStreamWriter(fileOut);
		pw = new PrintWriter(writer);
		for (int m = 0; m < maxDegreeOut; m++)
		{
			pw.println(Double.toString(percent2Out[m]));
		}
		pw.close();

		System.out.println("MHRW: Average In Degree = " + avgDegreeIn);
		System.out.println("MHRW: Average Out Degree = " + avgDegree);
		System.out.println("MHRW: Average Sample Number = " + totalSample / simulation);

		double mix_in = 0.0;
		double mix_out = 0.0;
		for (int m = 0; m < simulation;m++ )
		{
			mix_in = mix_in + mixingTimeIn[m];
			mix_out = mix_out + mixingTimeOut[m];
		}
		System.out.println("MHRW: Average Mixing Time (In) = " + mix_in / simulation);
		System.out.println("MHRW: Average Mixing Time (Out) = " + mix_out / simulation);
	}

	private static void BFS() throws FileNotFoundException
	{
		Random ra = new Random();
		Queue<Integer> sampledNodes = new LinkedList<Integer>();
		Queue<Integer> waitingNodes = new LinkedList<Integer>();
		Queue<Integer> queryNodes = new LinkedList<Integer>();
		int w = 0;
		int v = 0;
		int sample_node_number = sampleSize;
		int i = 0;
		double avgDegree = 0.0;
		double avgDegree_in = 0.0;
		double temp = 0.0;
		double temp_in = 0.0;
		int count;
		int singleSample = 0;
		int totalSample = 0;
		boolean[] waitingFlag = new boolean[nodeNumber + 1];
		boolean jumpFlag = false;

		for (int m = 0; m < maxDegreeIn; m++)
		{
			percent1In[m] = 0.0;
			percent2In[m] = 0.0;
			percent3In[m] = 0.0;
		}
		for (int m = 0; m < maxDegreeOut; m++)
		{
			percent1Out[m] = 0.0;
			percent2Out[m] = 0.0;
			percent3Out[m] = 0.0;
		}

		int[] mixingTimeIn = new int[simulation];
		int[] mixingTimeOut = new int[simulation];
		for (int m = 0; m < simulation; m++)
		{
			mixingTimeIn[m] = sample_node_number;
			mixingTimeOut[m] = sample_node_number;
		}
		int mix = 0;
		for (count = 0; count < simulation; count++)
		{
			i = 0;
			singleSample = 0;
			for (int m = 0; m < maxDegreeIn + 1; m++)
			{
				percent3In[m] = 0.0;
			}
			for (int m = 0; m < maxDegreeOut + 1; m++)
			{
				percent3Out[m] = 0.0;
			}

			v = ra.nextInt(Integer.MAX_VALUE) % allLinks.size();
			for (int m = 0; m < nodeNumber + 1; m++)
			{
				waitingFlag[m] = false;
			}
			waitingNodes.offer(v);
			waitingFlag[v] = true;
			jumpFlag = false;
			while (i < sample_node_number)
			{
				if (waitingNodes.size() > 0)
				{
					v = waitingNodes.poll();
					singleSample++;
					sampledNodes.offer(v);
					if (!queryNodes.contains(v))
					{
						if(jumpFlag == false)
						{
							i++;
							queryNodes.offer(v);
						}
						else
						{
							i = i + jumpBudget;
							queryNodes.offer(v);
							jumpFlag = false;
						}
					}
					for (int en_count = 0; en_count < allLinks.get(v).size(); en_count++)
					{
						w = allLinks.get(v).get(en_count);
						if(waitingFlag[w] == false)
						{
							waitingNodes.offer(w);
							waitingFlag[w] = true;
						}
					}
					for (int m = inLinks.get(v).size(); m <= maxDegreeIn; m++)
					{
						percent3In[m] = percent3In[m] + 1.0;
					}
					for (int m = outLinks.get(v).size(); m <= maxDegreeOut; m++)
					{
						percent3Out[m] = percent3Out[m] + 1.0;
					}
					mix = 0;
					for (mix = 0; mix < maxDegreeIn + 1; mix++)
					{
						if (Math.abs(percent3In[mix] / singleSample - percentIn[mix]) > 0.25)
							break;
					}
					if (mix == maxDegreeIn + 1 && mixingTimeIn[count] == sample_node_number)
						mixingTimeIn[count] = i;
					else if (mix != maxDegreeIn + 1)
						mixingTimeIn[count] = sample_node_number;

					mix = 0;
					for (mix = 0; mix < maxDegreeOut + 1; mix++)
					{
						if (Math.abs(percent3Out[mix] / singleSample - percentOut[mix]) > 0.25)
							break;
					}
					if (mix == maxDegreeOut + 1 && mixingTimeOut[count] == sample_node_number)
						mixingTimeOut[count] = i;
					else if (mix != maxDegreeOut + 1)
						mixingTimeOut[count] = sample_node_number;
				}
				else
				{
					v = ra.nextInt(Integer.MAX_VALUE) % (allLinks.size());
					waitingNodes.offer(v);
					jumpFlag = true;
				}
			}
			totalSample = singleSample + totalSample;

			for (int m = 0; m < maxDegreeIn+1; m++)
			{
				percent3In[m] = percent3In[m] / (double)singleSample;
				percent1In[m] = percent1In[m] + percent3In[m];
				percent2In[m] = percent2In[m] + (percent3In[m] - percentIn[m]) * (percent3In[m] - percentIn[m]);
			}
			for (int m = 0; m < maxDegreeOut+1; m++)
			{
				percent3Out[m] = percent3Out[m] / (double)singleSample;
				percent1Out[m] = percent1Out[m] + percent3Out[m];
				percent2Out[m] = percent2Out[m] + (percent3Out[m] - percentOut[m]) * (percent3Out[m] - percentOut[m]);
			}

// TODO on the first iteration, we record the sampled subnetwork
if(count==0)
	exportSampledNetworkAsPajek("BFS",outLinks,sampledNodes);
						
			sampledNodes.clear();
			queryNodes.clear();
			waitingNodes.clear();
		}
		avgDegree = temp / (double)(totalSample);
		avgDegree_in = temp_in / (double)(totalSample);

		for (int m = 0; m < maxDegreeIn; m++)
		{
			percent1In[m] = percent1In[m] / (double)(simulation);
			if (percentIn[m] != 0)
				percent2In[m] = Math.sqrt(percent2In[m] / (double)(simulation)) / percentIn[m];
			else
				percent2In[m] = 0;
		}
		for (int m = 0; m < maxDegreeOut; m++)
		{
			percent1Out[m] = percent1Out[m] / (double)(simulation);
			if (percentOut[m] != 0)
				percent2Out[m] = Math.sqrt(percent2Out[m] / (double)(simulation)) / percentOut[m];
			else
				percent2Out[m] = 0;
		}

		FileOutputStream fileOut = new FileOutputStream(path + "BFS_in_degree_distribution.txt");
		OutputStreamWriter writer = new OutputStreamWriter(fileOut);
		PrintWriter pw = new PrintWriter(writer);
		for (int m = 0; m < maxDegreeIn; m++)
		{
			pw.println(Double.toString(percent1In[m]));
		}
		pw.close();

		fileOut = new FileOutputStream(path + "BFS_out_degree_distribution.txt");
		writer = new OutputStreamWriter(fileOut);
		pw = new PrintWriter(writer);
		for (int m = 0; m < maxDegreeOut; m++)
		{
			pw.println(Double.toString(percent1Out[m]));
		}
		pw.close();

		fileOut = new FileOutputStream(path + "BFS_in_degree_NMSE.txt");
		writer = new OutputStreamWriter(fileOut);
		pw = new PrintWriter(writer);
		for (int m = 0; m < maxDegreeIn; m++)
		{
			pw.println(Double.toString(percent2In[m]));
		}
		pw.close();

		fileOut = new FileOutputStream(path + "BFS_out_degree_NMSE.txt");
		writer = new OutputStreamWriter(fileOut);
		pw = new PrintWriter(writer);
		for (int m = 0; m < maxDegreeOut; m++)
		{
			pw.println(Double.toString(percent2Out[m]));
		}
		pw.close();

		System.out.println("BFS: Average In Degree = " + avgDegree_in);
		System.out.println("BFS: Average Out Degree = " + avgDegree);
		System.out.println("BFS: Average Sample Number = " + totalSample / simulation);

		double mixIn = 0.0;
		double mixOut = 0.0;
		for (int m = 0; m < simulation; m++)
		{
			mixIn = mixIn + mixingTimeIn[m];
			mixOut = mixOut + mixingTimeOut[m];
		}
		System.out.println("BFS: Average Mixing Time (In) = " + mixIn / simulation);
		System.out.println("BFS: Average Mixing Time (Out) = " + mixOut / simulation);
	}

	private static void AS() throws FileNotFoundException
	{
System.out.println("Starting sampling");
		Random ra = new Random();
		Queue<Integer> sampledNode = new LinkedList<Integer>();
		Queue<Integer> queryNode = new LinkedList<Integer>();
		int w = 0;
		int v = 0;
		int sampleNodeNumber = sampleSize;
		int i = 0;
		double avgDegree = 0.0;
		double avgDegreeIn = 0.0;
		double temp = 0.0;
		double tempIn = 0.0;
		int count;
		int singleSample = 0;
		int totalSample = 0;
		for (int m = 0; m < maxDegreeIn; m++)
		{
			percent1In[m] = 0.0;
			percent2In[m] = 0.0;
			percent3In[m] = 0.0;
		}
		for (int m = 0; m < maxDegreeOut; m++)
		{
			percent1Out[m] = 0.0;
			percent2Out[m] = 0.0;
			percent3Out[m] = 0.0;
		}

		int[] mixingTime_in = new int[simulation];
		int[] mixingTime_out = new int[simulation];
		for (int m = 0; m < simulation; m++)
		{
			mixingTime_in[m] = sampleNodeNumber;
			mixingTime_out[m] = sampleNodeNumber;
		}
		int mix = 0;
		for (count = 0; count < simulation; count++)
		{
			i = 0;
			singleSample = 0;
			v = ra.nextInt(Integer.MAX_VALUE) % allLinks.size();

			for (int m = 0; m < maxDegreeIn + 1; m++)
			{
				percent3In[m] = 0.0;
			}
			for (int m = 0; m < maxDegreeOut + 1; m++)
			{
				percent3Out[m] = 0.0;
			}
			while (i < sampleNodeNumber)
			{
if(i%1000==0)				
	System.out.println("..Sampled nodes: "+i+"/"+sampleNodeNumber);				
				double q = ra.nextDouble();
				if (q < alpha)
				{
					v = ra.nextInt(Integer.MAX_VALUE) % (allLinks.size());
					singleSample++;
					sampledNode.offer(v);
					if (!queryNode.contains(v))
					{
						i = i + jumpBudget;
						queryNode.offer(v);
					}

					for (int m = inLinks.get(v).size(); m <= maxDegreeIn; m++)
					{
						percent3In[m] = percent3In[m] + 1.0;
					}
					for (int m = outLinks.get(v).size(); m <= maxDegreeOut; m++)
					{
						percent3Out[m] = percent3Out[m] + 1.0;
					}
					mix = 0;
					for (mix = 0; mix < maxDegreeIn + 1; mix++)
					{
						if (Math.abs(percent3In[mix] / singleSample - percentIn[mix]) > 0.25)
							break;
					}
					if (mix == maxDegreeIn + 1 && mixingTime_in[count] == sampleNodeNumber)
						mixingTime_in[count] = i;
					else if (mix != maxDegreeIn + 1)
						mixingTime_in[count] = sampleNodeNumber;

					mix = 0;
					for (mix = 0; mix < maxDegreeOut + 1; mix++)
					{
						if (Math.abs(percent3Out[mix] / singleSample - percentOut[mix]) > 0.25)
							break;
					}
					if (mix == maxDegreeOut + 1 && mixingTime_out[count] == sampleNodeNumber)
						mixingTime_out[count] = i;
					else if (mix != maxDegreeOut + 1)
						mixingTime_out[count] = sampleNodeNumber;
					continue;
				}
				if (allLinks.get(v).size() == 0)
				{
					v = ra.nextInt(Integer.MAX_VALUE) % (allLinks.size());
					singleSample++;
					sampledNode.offer(v);
					if (!queryNode.contains(v))
					{
						i++;
						queryNode.offer(v);
					}
					for (int m = inLinks.get(v).size(); m <= maxDegreeIn; m++)
					{
						percent3In[m] = percent3In[m] + 1.0;
					}
					for (int m = outLinks.get(v).size(); m <= maxDegreeOut; m++)
					{
						percent3Out[m] = percent3Out[m] + 1.0;
					}
					mix = 0;
					for (mix = 0; mix < maxDegreeIn + 1; mix++)
					{
						if (Math.abs(percent3In[mix] / singleSample - percentIn[mix]) > 0.25)
							break;
					}
					if (mix == maxDegreeIn + 1 && mixingTime_in[count] == sampleNodeNumber)
						mixingTime_in[count] = i;
					else if (mix != maxDegreeIn + 1)
						mixingTime_in[count] = sampleNodeNumber;

					mix = 0;
					for (mix = 0; mix < maxDegreeOut + 1; mix++)
					{
						if (Math.abs(percent3Out[mix] / singleSample - percentOut[mix]) > 0.25)
							break;
					}
					if (mix == maxDegreeOut + 1 && mixingTime_out[count] == sampleNodeNumber)
						mixingTime_out[count] = i;
					else if (mix != maxDegreeOut + 1)
						mixingTime_out[count] = sampleNodeNumber;
					continue;
				}
				w = allLinks.get(v).get(ra.nextInt(allLinks.get(v).size()));
				double p = ra.nextDouble();
				if (p <= (double)allLinks.get(v).size() / (double)allLinks.get(w).size())
				{
					v = w;
					singleSample++;
					sampledNode.offer(v);
				}
				else
				{
					singleSample++;
					sampledNode.offer(v);
				}
				if (!queryNode.contains(w))
				{
					i++;
					queryNode.offer(w);
				}
				for (int m = inLinks.get(v).size(); m <= maxDegreeIn; m++)
				{
					percent3In[m] = percent3In[m] + 1.0;
				}
				for (int m = outLinks.get(v).size(); m <= maxDegreeOut; m++)
				{
					percent3Out[m] = percent3Out[m] + 1.0;
				}
				mix = 0;
				for (mix = 0; mix < maxDegreeIn + 1; mix++)
				{
					if (Math.abs(percent3In[mix] / singleSample - percentIn[mix]) > 0.25)
						break;
				}
				if (mix == maxDegreeIn + 1 && mixingTime_in[count] == sampleNodeNumber)
					mixingTime_in[count] = i;
				else if (mix != maxDegreeIn + 1)
					mixingTime_in[count] = sampleNodeNumber;

				mix = 0;
				for (mix = 0; mix < maxDegreeOut + 1; mix++)
				{
					if (Math.abs(percent3Out[mix] / singleSample - percentOut[mix]) > 0.25)
						break;
				}
				if (mix == maxDegreeOut + 1 && mixingTime_out[count] == sampleNodeNumber)
					mixingTime_out[count] = i;
				else if (mix != maxDegreeOut + 1)
					mixingTime_out[count] = sampleNodeNumber;
			}
			totalSample = singleSample + totalSample;
			for (int m = 0; m < maxDegreeIn + 1; m++)
			{
				percent3In[m] = percent3In[m] / (double)singleSample;
				percent1In[m] = percent1In[m] + percent3In[m];
				percent2In[m] = percent2In[m] + (percent3In[m] - percentIn[m]) * (percent3In[m] - percentIn[m]);
			}
			for (int m = 0; m < maxDegreeOut + 1; m++)
			{
				percent3Out[m] = percent3Out[m] / (double)singleSample;
				percent1Out[m] = percent1Out[m] + percent3Out[m];
				percent2Out[m] = percent2Out[m] + (percent3Out[m] - percentOut[m]) * (percent3Out[m] - percentOut[m]);
			}
System.out.println("Sampling complete");
			
// TODO on the first iteration, we record the sampled subnetwork
if(count==0)
	exportSampledNetworkAsPajek("AS",outLinks,sampledNode);

						
			sampledNode.clear();
			queryNode.clear();
		}
		avgDegree = temp / (double)(totalSample);
		avgDegreeIn = tempIn / (double)(totalSample);
		for (int m = 0; m < maxDegreeIn; m++)
		{
			percent1In[m] = percent1In[m] / (double)(simulation);
			if (percentIn[m] != 0)
				percent2In[m] = Math.sqrt(percent2In[m] / (double)(simulation)) / percentIn[m];
			else
				percent2In[m] = 0;
		}
		for (int m = 0; m < maxDegreeOut; m++)
		{
			percent1Out[m] = percent1Out[m] / (double)(simulation);
			if (percentOut[m] != 0)
				percent2Out[m] = Math.sqrt(percent2Out[m] / (double)(simulation)) / percentOut[m];
			else
				percent2Out[m] = 0;
		}

		FileOutputStream fileOut = new FileOutputStream(path + "AS_in_degree_distribution.txt");
		OutputStreamWriter writer = new OutputStreamWriter(fileOut);
		PrintWriter pw = new PrintWriter(writer);
		for (int m = 0; m < maxDegreeIn; m++)
		{
			pw.println(Double.toString(percent1In[m]));
		}
		pw.close();

		fileOut = new FileOutputStream(path + "AS_out_degree_distribution.txt");
		writer = new OutputStreamWriter(fileOut);
		pw = new PrintWriter(writer);
		for (int m = 0; m < maxDegreeOut; m++)
		{
			pw.println(Double.toString(percent1Out[m]));
		}
		pw.close();

		fileOut = new FileOutputStream(path + "AS_in_degree_NMSE.txt");
		writer = new OutputStreamWriter(fileOut);
		pw = new PrintWriter(writer);
		for (int m = 0; m < maxDegreeIn; m++)
		{
			pw.println(Double.toString(percent2In[m]));
		}
		pw.close();

		fileOut = new FileOutputStream(path + "AS_out_degree_NMSE.txt");
		writer = new OutputStreamWriter(fileOut);
		pw = new PrintWriter(writer);
		for (int m = 0; m < maxDegreeOut; m++)
		{
			pw.println(Double.toString(percent2Out[m]));
		}
		pw.close();

		System.out.println("AS: Average In Degree = " + avgDegreeIn);
		System.out.println("AS: Average Out Degree = " + avgDegree);
		System.out.println("AS: Average Sample Number = " + totalSample / simulation);
		double mixIn = 0.0;
		double mixOut = 0.0;
		for (int m = 0; m < simulation; m++)
		{
			mixIn = mixIn + mixingTime_in[m];
			mixOut = mixOut + mixingTime_out[m];
		}
		System.out.println("AS: Average Mixing Time (In) = " + mixIn / simulation);
		System.out.println("AS: Average Mixing Time (Out) = " + mixOut / simulation);
	}

	/**
	 * TODO
	 * Records the sampled network, for further use.
	 * We use the Pajek {@code .net} format: first
	 * the list of nodes with their id in the new
	 * network and their original id as a label, and 
	 * second the list of links, using pairs of (new) ids.
	 * Nodes are numbered starting from 1.
	 * 
	 * @param filename
	 * 		Name of the network file to be created.
	 * 
	 * @throws FileNotFoundException 
	 * 		Problem while recording the file.
	 * 
	 * @author
	 * 		Vincent Labatut
	 */
	private static void exportSampledNetworkAsPajek(String algo, List<List<Integer>> outgoingLinks, Queue<Integer> sampledNodes) throws FileNotFoundException
	{	// open file
		String filename = path + File.separator + algo + "_sample.net";
		System.out.println("Starting ecording sample ("+filename+")");
		FileOutputStream fileOut = new FileOutputStream(filename);
		OutputStreamWriter writer = new OutputStreamWriter(fileOut);
		PrintWriter sw = new PrintWriter(writer);
		
		// write nodes
		Map<Integer,Integer> nodeMap;
		{	int size = sampledNodes.size();
			sw.println("*vertices "+size);
			nodeMap = new HashMap<Integer, Integer>(size);
			int nouv = 1;
			for(Integer old: sampledNodes)
			{	nodeMap.put(old,nouv);
				sw.println(nouv+" \""+old+"\"");
				nouv++;
			}
		}
		
		sw.println();
		
		// write links
		{	sw.println("*arcs"); //"*edges" is for undirected networks
			int old1 = 0;
			for(List<Integer> neigh: outgoingLinks)
			{	Integer nouv1 = nodeMap.get(old1);
				if(nouv1!=null)
				{	for(int old2: neigh)
					{	Integer nouv2 = nodeMap.get(old2);
						if(nouv2!=null)
							sw.println(nouv1+" "+nouv2);
					}
				}
				old1++;
			}
		}
		
		// close file
		sw.close();
		System.out.println("Recording complete");
	}
	
	public static void main(String[] args) throws FileNotFoundException
	{
		Init();
		
//		BFS();
//		MHRW();
		AS();
	}
}
