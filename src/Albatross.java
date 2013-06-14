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
	static List<List<Integer>> list;              // Original Graph: out degree
	static List<List<Integer>> list_in;           // Original Graph: in degree
	static List<List<Integer>> list_undirected;   // Original Graph -> Undirected Graph
	static double[] percent_in;           // True Value
	static double[] percent_out;
	static double[] percent_1_in;         // CDF
	static double[] percent_1_out;
	static double[] percent_2_in;         // NMSE
	static double[] percent_2_out;
	static double[] percent_3_in;         // Each Time's Estimation
	static double[] percent_3_out;
	static boolean[] node;
	static int max_degree_in;
	static int max_degree_out;
	static int node_number;
	static int real_node_number = 0;
	static int edge_number;
	static int simulation = 1000;		// number of repetitions of the sampling process
	static int sample_size;
	static double alpha = 0.02;           // Jump Probability in AS
	static int jump_budget = 10;          // Set Jump-Cost
	static String path = "data/";              // Fill in the file path
	static String filename = "kdd03.txt";          // Fill in the file name

	private static void Init() throws FileNotFoundException
	{
		FileInputStream fileIn = new FileInputStream(path + filename);
		InputStreamReader reader = new InputStreamReader(fileIn);
		Scanner sr = new Scanner(reader);
		node_number = Integer.parseInt(sr.nextLine());
		edge_number = Integer.parseInt(sr.nextLine());
		list = new ArrayList<List<Integer>>(node_number);
		list_in = new ArrayList<List<Integer>>(node_number);
		list_undirected = new ArrayList<List<Integer>>(node_number);
		node = new boolean[node_number + 1];
		for (int i = 0; i < node_number; i++)
		{
			list.add(new ArrayList<Integer>());
			list_in.add(new ArrayList<Integer>());
			list_undirected.add(new ArrayList<Integer>());
		}
		final String split_flag = "\t";
		int FromNode = 0, ToNode = 0;
		int edge_count_1 = 0;
		int edge_count_2 = 0;
		while (edge_count_1+edge_count_2 < edge_number)
		{
			String str = sr.nextLine();
			String[] split = str.split(split_flag);
			FromNode = Integer.parseInt(split[0]);
			ToNode = Integer.parseInt(split[1]);
			if (FromNode == ToNode)
			{	
				edge_count_2++;
				continue;
			}
			list.get(FromNode).add(ToNode);
			list_in.get(ToNode).add(FromNode);
			if (!list_undirected.get(FromNode).contains(ToNode))
				list_undirected.get(FromNode).add(ToNode);
			if (!list_undirected.get(ToNode).contains(FromNode))
				list_undirected.get(ToNode).add(FromNode);
			node[FromNode] = true;
			node[ToNode] = true;
			edge_count_1++;
		}
		sr.close();

		for (int i = 0; i < node_number; i++)
		{
			if (node[i] == true)
				real_node_number++;
		}
		sample_size = real_node_number / 20;  // Set Total-Cost

		max_degree_in = 0;
		max_degree_out = 0;
		for (int i = 0; i < node_number; i++)
		{
			if (list_in.get(i).size() > max_degree_in)
				max_degree_in = list_in.get(i).size();
			if (list.get(i).size() > max_degree_out)
				max_degree_out = list.get(i).size();
		}
		percent_in = new double[max_degree_in + 1];
		percent_out = new double[max_degree_out + 1];
		percent_1_in = new double[max_degree_in + 1];
		percent_1_out = new double[max_degree_out + 1];
		percent_2_in = new double[max_degree_in + 1];
		percent_2_out = new double[max_degree_out + 1];
		percent_3_in = new double[max_degree_in + 1];
		percent_3_out = new double[max_degree_out + 1];
		for (int i = 0; i < node_number; i++)
		{
			percent_in[list_in.get(i).size()] = percent_in[list_in.get(i).size()] + 1;
			percent_out[list.get(i).size()] = percent_out[list.get(i).size()] + 1;
		}

		percent_in[0] = percent_in[0] - (node_number - real_node_number);
		percent_out[0] = percent_out[0] - (node_number - real_node_number);
		percent_in[0] = percent_in[0] / (double)real_node_number;
		percent_out[0] = percent_out[0] / (double)real_node_number;

		for (int i = 1; i <= max_degree_in; i++)
		{
			percent_in[i] = percent_in[i] / (double)real_node_number;
			percent_in[i] = percent_in[i] + percent_in[i - 1];
		}
		for (int i = 1; i <= max_degree_out; i++)
		{
			percent_out[i] = percent_out[i] / (double)real_node_number;
			percent_out[i] = percent_out[i] + percent_out[i - 1];
		}

		System.out.println("Test File: " + filename);
		System.out.println("Test Path: " + path);
		System.out.println("Average Degree = " + (double)edge_count_1 / (double)real_node_number);
		System.out.println("Simulation Times = " + simulation);
		System.out.println("Node Number = " + node_number);
		System.out.println("Real Node Number = " + real_node_number);
		System.out.println("Sample Budget = " + sample_size);
		System.out.println("Jump Alpha = " + alpha);
		System.out.println("");

		FileOutputStream fileOut = new FileOutputStream(path + "Original_graph_in_degree_distribution.txt");
		OutputStreamWriter writer = new OutputStreamWriter(fileOut);
		PrintWriter sw = new PrintWriter(writer);
		for (int i = 0; i < max_degree_in; i++)
		{
			sw.println(Double.toString(percent_in[i]));
		}
		sw.close();

		fileOut = new FileOutputStream(path + "Original_graph_out_degree_distribution.txt");
		writer = new OutputStreamWriter(fileOut);
		sw = new PrintWriter(writer);
		for (int i = 0; i < max_degree_out; i++)
		{
			sw.println(Double.toString(percent_out[i]));
		}
		sw.close();
	}

	private static void MHRW() throws FileNotFoundException
	{
		Random ra = new Random();
		Queue<Integer> sampled_node = new LinkedList<Integer>();
		Queue<Integer> query_node = new LinkedList<Integer>();
		int w = 0;
		int v = 0;
		int sample_node_number = sample_size;
		int i = 0; 
		double avg_degree = 0.0;
		double avg_degree_in = 0.0;
		double temp = 0.0;
		double temp_in = 0.0;
		int count;
		int single_sample = 0;
		int total_sample = 0;
		for (int m = 0; m < max_degree_in; m++)
		{
			percent_1_in[m] = 0.0;
			percent_2_in[m] = 0.0;
			percent_3_in[m] = 0.0;
		}
		for (int m = 0; m < max_degree_out; m++)
		{
			percent_1_out[m] = 0.0;
			percent_2_out[m] = 0.0;
			percent_3_out[m] = 0.0;
		}

		int[] mixing_time_in = new int[simulation];
		int[] mixing_time_out = new int[simulation];
		for(int m=0;m<simulation;m++)
		{
			mixing_time_in[m] = sample_node_number;
			mixing_time_out[m] = sample_node_number;
		}
		int mix = 0;

		for (count = 0; count < simulation; count++)
		{
			i = 0;
			single_sample = 0;
			v = ra.nextInt(Integer.MAX_VALUE) % list_undirected.size();
			for (int m = 0; m < max_degree_in+1; m++)
			{
				percent_3_in[m] = 0.0;
			}
			for (int m = 0; m < max_degree_out+1; m++)
			{
				percent_3_out[m] = 0.0;
			}
			while (i < sample_node_number)
			{
				if (list_undirected.get(v).size() == 0)
				{
					v = ra.nextInt(Integer.MAX_VALUE) % (list_undirected.size());
					single_sample++;
					sampled_node.offer(v);
					if (!query_node.contains(v))
					{
						i++;
						query_node.offer(v);
					}
					for (int m = list_in.get(v).size(); m <= max_degree_in; m++)
					{
						percent_3_in[m] = percent_3_in[m] + 1.0;
					}
					for (int m = list.get(v).size(); m <= max_degree_out; m++)
					{
						percent_3_out[m] = percent_3_out[m] + 1.0;
					}
					mix = 0;
					for (mix = 0; mix < max_degree_in + 1; mix++)
					{
						if (Math.abs(percent_3_in[mix] / single_sample - percent_in[mix]) > 0.25)
							break;
					}
					if (mix == max_degree_in + 1 && mixing_time_in[count] == sample_node_number)
						mixing_time_in[count] = i;
					else if (mix != max_degree_in + 1)
						mixing_time_in[count] = sample_node_number;

					mix = 0;
					for (mix = 0; mix < max_degree_out + 1; mix++)
					{
						if (Math.abs(percent_3_out[mix] / single_sample - percent_out[mix]) > 0.25)
							break;
					}
					if (mix == max_degree_out + 1 && mixing_time_out[count] == sample_node_number)
						mixing_time_out[count] = i;
					else if (mix != max_degree_out + 1)
						mixing_time_out[count] = sample_node_number;
					continue;
				}
				w = list_undirected.get(v).get(ra.nextInt(list_undirected.get(v).size()));
				double p = ra.nextDouble();
				if (p <= (double)list_undirected.get(v).size() / (double)list_undirected.get(w).size())
				{
					v = w;
					single_sample++;
					sampled_node.offer(v);
				}
				else
				{
					single_sample++;
					sampled_node.offer(v);
				}

				if (!query_node.contains(w))
				{
					i++;
					query_node.offer(w);
				}

				for (int m = list_in.get(v).size(); m <= max_degree_in; m++)
				{
					percent_3_in[m] = percent_3_in[m] + 1.0;
				}
				for (int m = list.get(v).size(); m <= max_degree_out; m++)
				{
					percent_3_out[m] = percent_3_out[m] + 1.0;
				}
				mix = 0;
				for (mix = 0; mix < max_degree_in + 1; mix++)
				{
					if (Math.abs(percent_3_in[mix] / single_sample - percent_in[mix]) > 0.25)
						break;
				}
				if (mix == max_degree_in + 1 && mixing_time_in[count] == sample_node_number)
					mixing_time_in[count] = i;
				else if (mix != max_degree_in + 1)
					mixing_time_in[count] = sample_node_number;

				mix = 0;
				for (mix = 0; mix < max_degree_out + 1; mix++)
				{
					if (Math.abs(percent_3_out[mix] / single_sample - percent_out[mix]) > 0.25)
						break;
				}
				if (mix == max_degree_out + 1 && mixing_time_out[count] == sample_node_number)
					mixing_time_out[count] = i;
				else if (mix != max_degree_out + 1)
					mixing_time_out[count] = sample_node_number;
			}
			total_sample = single_sample + total_sample;

			for (int m = 0; m < max_degree_in+1; m++)
			{
				percent_3_in[m] = percent_3_in[m] / (double)single_sample;
				percent_1_in[m] = percent_1_in[m] + percent_3_in[m];
				percent_2_in[m] = percent_2_in[m] + (percent_3_in[m] - percent_in[m]) * (percent_3_in[m] - percent_in[m]);
			}
			for (int m = 0; m < max_degree_out+1; m++)
			{
				percent_3_out[m] = percent_3_out[m] / (double)single_sample;
				percent_1_out[m] = percent_1_out[m] + percent_3_out[m];
				percent_2_out[m] = percent_2_out[m] + (percent_3_out[m] - percent_out[m]) * (percent_3_out[m] - percent_out[m]);
			}
	
// TODO on the first iteration, we record the sampled subnetwork
if(count==0)
	exportSampledNetworkAsPajek("MHRW",list,sampled_node);
			
			sampled_node.clear();
			query_node.clear();
		}
		/* 
		 * TODO bug: temp is never updated. 
		 * Same thing in other functions. 
		 * And for temp_in, too.
		 */ 
		avg_degree = temp / (double)(total_sample);			 
		avg_degree_in = temp_in / (double)(total_sample);

		for (int m = 0; m < max_degree_in; m++)
		{
			percent_1_in[m] = percent_1_in[m] / (double)(simulation);
			if (percent_in[m] != 0)
				percent_2_in[m] = Math.sqrt(percent_2_in[m] / (double)(simulation)) / percent_in[m];
			else
				percent_2_in[m] = 0;
		}
		for (int m = 0; m < max_degree_out; m++)
		{
			percent_1_out[m] = percent_1_out[m] / (double)(simulation);
			if (percent_out[m] != 0)
				percent_2_out[m] = Math.sqrt(percent_2_out[m] / (double)(simulation)) / percent_out[m];
			else
				percent_2_out[m] = 0;
		}

		FileOutputStream fileOut = new FileOutputStream(path + "MHRW_in_degree_distribution.txt");
		OutputStreamWriter writer = new OutputStreamWriter(fileOut);
		PrintWriter sw = new PrintWriter(writer);
		for (int m = 0; m < max_degree_in; m++)
		{
			sw.println(Double.toString(percent_1_in[m]));
		}
		sw.close();

		fileOut = new FileOutputStream(path + "MHRW_out_degree_distribution.txt");
		writer = new OutputStreamWriter(fileOut);
		sw = new PrintWriter(writer);
		for (int m = 0; m < max_degree_out; m++)
		{
			sw.println(Double.toString(percent_1_out[m]));
		}
		sw.close();

		fileOut = new FileOutputStream(path + "MHRW_in_degree_NMSE.txt");
		writer = new OutputStreamWriter(fileOut);
		sw = new PrintWriter(writer);
		for (int m = 0; m < max_degree_in; m++)
		{
			sw.println(Double.toString(percent_2_in[m]));
		}
		sw.close();

		fileOut = new FileOutputStream(path + "MHRW_out_degree_NMSE.txt");
		writer = new OutputStreamWriter(fileOut);
		sw = new PrintWriter(writer);
		for (int m = 0; m < max_degree_out; m++)
		{
			sw.println(Double.toString(percent_2_out[m]));
		}
		sw.close();

		System.out.println("MHRW: Average In Degree = " + avg_degree_in);
		System.out.println("MHRW: Average Out Degree = " + avg_degree);
		System.out.println("MHRW: Average Sample Number = " + total_sample / simulation);

		double mix_in = 0.0;
		double mix_out = 0.0;
		for (int m = 0; m < simulation;m++ )
		{
			mix_in = mix_in + mixing_time_in[m];
			mix_out = mix_out + mixing_time_out[m];
		}
		System.out.println("MHRW: Average Mixing Time (In) = " + mix_in / simulation);
		System.out.println("MHRW: Average Mixing Time (Out) = " + mix_out / simulation);
	}

	private static void BFS() throws FileNotFoundException
	{
		Random ra = new Random();
		Queue<Integer> sampled_node = new LinkedList<Integer>();
		Queue<Integer> waiting_node = new LinkedList<Integer>();
		Queue<Integer> query_node = new LinkedList<Integer>();
		int w = 0;
		int v = 0;
		int sample_node_number = sample_size;
		int i = 0;
		double avg_degree = 0.0;
		double avg_degree_in = 0.0;
		double temp = 0.0;
		double temp_in = 0.0;
		int count;
		int single_sample = 0;
		int total_sample = 0;
		boolean[] waiting_flag = new boolean[node_number + 1];
		boolean jump_flag = false;

		for (int m = 0; m < max_degree_in; m++)
		{
			percent_1_in[m] = 0.0;
			percent_2_in[m] = 0.0;
			percent_3_in[m] = 0.0;
		}
		for (int m = 0; m < max_degree_out; m++)
		{
			percent_1_out[m] = 0.0;
			percent_2_out[m] = 0.0;
			percent_3_out[m] = 0.0;
		}

		int[] mixing_time_in = new int[simulation];
		int[] mixing_time_out = new int[simulation];
		for (int m = 0; m < simulation; m++)
		{
			mixing_time_in[m] = sample_node_number;
			mixing_time_out[m] = sample_node_number;
		}
		int mix = 0;
		for (count = 0; count < simulation; count++)
		{
			i = 0;
			single_sample = 0;
			for (int m = 0; m < max_degree_in + 1; m++)
			{
				percent_3_in[m] = 0.0;
			}
			for (int m = 0; m < max_degree_out + 1; m++)
			{
				percent_3_out[m] = 0.0;
			}

			v = ra.nextInt(Integer.MAX_VALUE) % list_undirected.size();
			for (int m = 0; m < node_number + 1; m++)
			{
				waiting_flag[m] = false;
			}
			waiting_node.offer(v);
			waiting_flag[v] = true;
			jump_flag = false;
			while (i < sample_node_number)
			{
				if (waiting_node.size() > 0)
				{
					v = waiting_node.poll();
					single_sample++;
					sampled_node.offer(v);
					if (!query_node.contains(v))
					{
						if(jump_flag == false)
						{
							i++;
							query_node.offer(v);
						}
						else
						{
							i = i + jump_budget;
							query_node.offer(v);
							jump_flag = false;
						}
					}
					for (int en_count = 0; en_count < list_undirected.get(v).size(); en_count++)
					{
						w = list_undirected.get(v).get(en_count);
						if(waiting_flag[w] == false)
						{
							waiting_node.offer(w);
							waiting_flag[w] = true;
						}
					}
					for (int m = list_in.get(v).size(); m <= max_degree_in; m++)
					{
						percent_3_in[m] = percent_3_in[m] + 1.0;
					}
					for (int m = list.get(v).size(); m <= max_degree_out; m++)
					{
						percent_3_out[m] = percent_3_out[m] + 1.0;
					}
					mix = 0;
					for (mix = 0; mix < max_degree_in + 1; mix++)
					{
						if (Math.abs(percent_3_in[mix] / single_sample - percent_in[mix]) > 0.25)
							break;
					}
					if (mix == max_degree_in + 1 && mixing_time_in[count] == sample_node_number)
						mixing_time_in[count] = i;
					else if (mix != max_degree_in + 1)
						mixing_time_in[count] = sample_node_number;

					mix = 0;
					for (mix = 0; mix < max_degree_out + 1; mix++)
					{
						if (Math.abs(percent_3_out[mix] / single_sample - percent_out[mix]) > 0.25)
							break;
					}
					if (mix == max_degree_out + 1 && mixing_time_out[count] == sample_node_number)
						mixing_time_out[count] = i;
					else if (mix != max_degree_out + 1)
						mixing_time_out[count] = sample_node_number;
				}
				else
				{
					v = ra.nextInt(Integer.MAX_VALUE) % (list_undirected.size());
					waiting_node.offer(v);
					jump_flag = true;
				}
			}
			total_sample = single_sample + total_sample;

			for (int m = 0; m < max_degree_in+1; m++)
			{
				percent_3_in[m] = percent_3_in[m] / (double)single_sample;
				percent_1_in[m] = percent_1_in[m] + percent_3_in[m];
				percent_2_in[m] = percent_2_in[m] + (percent_3_in[m] - percent_in[m]) * (percent_3_in[m] - percent_in[m]);
			}
			for (int m = 0; m < max_degree_out+1; m++)
			{
				percent_3_out[m] = percent_3_out[m] / (double)single_sample;
				percent_1_out[m] = percent_1_out[m] + percent_3_out[m];
				percent_2_out[m] = percent_2_out[m] + (percent_3_out[m] - percent_out[m]) * (percent_3_out[m] - percent_out[m]);
			}

// TODO on the first iteration, we record the sampled subnetwork
if(count==0)
	exportSampledNetworkAsPajek("BFS",list,sampled_node);
						
			sampled_node.clear();
			query_node.clear();
			waiting_node.clear();
		}
		avg_degree = temp / (double)(total_sample);
		avg_degree_in = temp_in / (double)(total_sample);

		for (int m = 0; m < max_degree_in; m++)
		{
			percent_1_in[m] = percent_1_in[m] / (double)(simulation);
			if (percent_in[m] != 0)
				percent_2_in[m] = Math.sqrt(percent_2_in[m] / (double)(simulation)) / percent_in[m];
			else
				percent_2_in[m] = 0;
		}
		for (int m = 0; m < max_degree_out; m++)
		{
			percent_1_out[m] = percent_1_out[m] / (double)(simulation);
			if (percent_out[m] != 0)
				percent_2_out[m] = Math.sqrt(percent_2_out[m] / (double)(simulation)) / percent_out[m];
			else
				percent_2_out[m] = 0;
		}

		FileOutputStream fileOut = new FileOutputStream(path + "BFS_in_degree_distribution.txt");
		OutputStreamWriter writer = new OutputStreamWriter(fileOut);
		PrintWriter sw = new PrintWriter(writer);
		for (int m = 0; m < max_degree_in; m++)
		{
			sw.println(Double.toString(percent_1_in[m]));
		}
		sw.close();

		fileOut = new FileOutputStream(path + "BFS_out_degree_distribution.txt");
		writer = new OutputStreamWriter(fileOut);
		sw = new PrintWriter(writer);
		for (int m = 0; m < max_degree_out; m++)
		{
			sw.println(Double.toString(percent_1_out[m]));
		}
		sw.close();

		fileOut = new FileOutputStream(path + "BFS_in_degree_NMSE.txt");
		writer = new OutputStreamWriter(fileOut);
		sw = new PrintWriter(writer);
		for (int m = 0; m < max_degree_in; m++)
		{
			sw.println(Double.toString(percent_2_in[m]));
		}
		sw.close();

		fileOut = new FileOutputStream(path + "BFS_out_degree_NMSE.txt");
		writer = new OutputStreamWriter(fileOut);
		sw = new PrintWriter(writer);
		for (int m = 0; m < max_degree_out; m++)
		{
			sw.println(Double.toString(percent_2_out[m]));
		}
		sw.close();

		System.out.println("BFS: Average In Degree = " + avg_degree_in);
		System.out.println("BFS: Average Out Degree = " + avg_degree);
		System.out.println("BFS: Average Sample Number = " + total_sample / simulation);

		double mix_in = 0.0;
		double mix_out = 0.0;
		for (int m = 0; m < simulation; m++)
		{
			mix_in = mix_in + mixing_time_in[m];
			mix_out = mix_out + mixing_time_out[m];
		}
		System.out.println("BFS: Average Mixing Time (In) = " + mix_in / simulation);
		System.out.println("BFS: Average Mixing Time (Out) = " + mix_out / simulation);
	}

	private static void AS() throws FileNotFoundException
	{
		Random ra = new Random();
		Queue<Integer> sampled_node = new LinkedList<Integer>();
		Queue<Integer> query_node = new LinkedList<Integer>();
		int w = 0;
		int v = 0;
		int sample_node_number = sample_size;
		int i = 0;
		double avg_degree = 0.0;
		double avg_degree_in = 0.0;
		double temp = 0.0;
		double temp_in = 0.0;
		int count;
		int single_sample = 0;
		int total_sample = 0;
		for (int m = 0; m < max_degree_in; m++)
		{
			percent_1_in[m] = 0.0;
			percent_2_in[m] = 0.0;
			percent_3_in[m] = 0.0;
		}
		for (int m = 0; m < max_degree_out; m++)
		{
			percent_1_out[m] = 0.0;
			percent_2_out[m] = 0.0;
			percent_3_out[m] = 0.0;
		}

		int[] mixing_time_in = new int[simulation];
		int[] mixing_time_out = new int[simulation];
		for (int m = 0; m < simulation; m++)
		{
			mixing_time_in[m] = sample_node_number;
			mixing_time_out[m] = sample_node_number;
		}
		int mix = 0;
		for (count = 0; count < simulation; count++)
		{
			i = 0;
			single_sample = 0;
			v = ra.nextInt(Integer.MAX_VALUE) % list_undirected.size();

			for (int m = 0; m < max_degree_in + 1; m++)
			{
				percent_3_in[m] = 0.0;
			}
			for (int m = 0; m < max_degree_out + 1; m++)
			{
				percent_3_out[m] = 0.0;
			}
			while (i < sample_node_number)
			{
				double q = ra.nextDouble();
				if (q < alpha)
				{
					v = ra.nextInt(Integer.MAX_VALUE) % (list_undirected.size());
					single_sample++;
					sampled_node.offer(v);
					if (!query_node.contains(v))
					{
						i = i + jump_budget;
						query_node.offer(v);
					}

					for (int m = list_in.get(v).size(); m <= max_degree_in; m++)
					{
						percent_3_in[m] = percent_3_in[m] + 1.0;
					}
					for (int m = list.get(v).size(); m <= max_degree_out; m++)
					{
						percent_3_out[m] = percent_3_out[m] + 1.0;
					}
					mix = 0;
					for (mix = 0; mix < max_degree_in + 1; mix++)
					{
						if (Math.abs(percent_3_in[mix] / single_sample - percent_in[mix]) > 0.25)
							break;
					}
					if (mix == max_degree_in + 1 && mixing_time_in[count] == sample_node_number)
						mixing_time_in[count] = i;
					else if (mix != max_degree_in + 1)
						mixing_time_in[count] = sample_node_number;

					mix = 0;
					for (mix = 0; mix < max_degree_out + 1; mix++)
					{
						if (Math.abs(percent_3_out[mix] / single_sample - percent_out[mix]) > 0.25)
							break;
					}
					if (mix == max_degree_out + 1 && mixing_time_out[count] == sample_node_number)
						mixing_time_out[count] = i;
					else if (mix != max_degree_out + 1)
						mixing_time_out[count] = sample_node_number;
					continue;
				}
				if (list_undirected.get(v).size() == 0)
				{
					v = ra.nextInt(Integer.MAX_VALUE) % (list_undirected.size());
					single_sample++;
					sampled_node.offer(v);
					if (!query_node.contains(v))
					{
						i++;
						query_node.offer(v);
					}
					for (int m = list_in.get(v).size(); m <= max_degree_in; m++)
					{
						percent_3_in[m] = percent_3_in[m] + 1.0;
					}
					for (int m = list.get(v).size(); m <= max_degree_out; m++)
					{
						percent_3_out[m] = percent_3_out[m] + 1.0;
					}
					mix = 0;
					for (mix = 0; mix < max_degree_in + 1; mix++)
					{
						if (Math.abs(percent_3_in[mix] / single_sample - percent_in[mix]) > 0.25)
							break;
					}
					if (mix == max_degree_in + 1 && mixing_time_in[count] == sample_node_number)
						mixing_time_in[count] = i;
					else if (mix != max_degree_in + 1)
						mixing_time_in[count] = sample_node_number;

					mix = 0;
					for (mix = 0; mix < max_degree_out + 1; mix++)
					{
						if (Math.abs(percent_3_out[mix] / single_sample - percent_out[mix]) > 0.25)
							break;
					}
					if (mix == max_degree_out + 1 && mixing_time_out[count] == sample_node_number)
						mixing_time_out[count] = i;
					else if (mix != max_degree_out + 1)
						mixing_time_out[count] = sample_node_number;
					continue;
				}
				w = list_undirected.get(v).get(ra.nextInt(list_undirected.get(v).size()));
				double p = ra.nextDouble();
				if (p <= (double)list_undirected.get(v).size() / (double)list_undirected.get(w).size())
				{
					v = w;
					single_sample++;
					sampled_node.offer(v);
				}
				else
				{
					single_sample++;
					sampled_node.offer(v);
				}
				if (!query_node.contains(w))
				{
					i++;
					query_node.offer(w);
				}
				for (int m = list_in.get(v).size(); m <= max_degree_in; m++)
				{
					percent_3_in[m] = percent_3_in[m] + 1.0;
				}
				for (int m = list.get(v).size(); m <= max_degree_out; m++)
				{
					percent_3_out[m] = percent_3_out[m] + 1.0;
				}
				mix = 0;
				for (mix = 0; mix < max_degree_in + 1; mix++)
				{
					if (Math.abs(percent_3_in[mix] / single_sample - percent_in[mix]) > 0.25)
						break;
				}
				if (mix == max_degree_in + 1 && mixing_time_in[count] == sample_node_number)
					mixing_time_in[count] = i;
				else if (mix != max_degree_in + 1)
					mixing_time_in[count] = sample_node_number;

				mix = 0;
				for (mix = 0; mix < max_degree_out + 1; mix++)
				{
					if (Math.abs(percent_3_out[mix] / single_sample - percent_out[mix]) > 0.25)
						break;
				}
				if (mix == max_degree_out + 1 && mixing_time_out[count] == sample_node_number)
					mixing_time_out[count] = i;
				else if (mix != max_degree_out + 1)
					mixing_time_out[count] = sample_node_number;
			}
			total_sample = single_sample + total_sample;
			for (int m = 0; m < max_degree_in + 1; m++)
			{
				percent_3_in[m] = percent_3_in[m] / (double)single_sample;
				percent_1_in[m] = percent_1_in[m] + percent_3_in[m];
				percent_2_in[m] = percent_2_in[m] + (percent_3_in[m] - percent_in[m]) * (percent_3_in[m] - percent_in[m]);
			}
			for (int m = 0; m < max_degree_out + 1; m++)
			{
				percent_3_out[m] = percent_3_out[m] / (double)single_sample;
				percent_1_out[m] = percent_1_out[m] + percent_3_out[m];
				percent_2_out[m] = percent_2_out[m] + (percent_3_out[m] - percent_out[m]) * (percent_3_out[m] - percent_out[m]);
			}
			
// TODO on the first iteration, we record the sampled subnetwork
if(count==0)
	exportSampledNetworkAsPajek("AS",list,sampled_node);

						
			sampled_node.clear();
			query_node.clear();
		}
		avg_degree = temp / (double)(total_sample);
		avg_degree_in = temp_in / (double)(total_sample);
		for (int m = 0; m < max_degree_in; m++)
		{
			percent_1_in[m] = percent_1_in[m] / (double)(simulation);
			if (percent_in[m] != 0)
				percent_2_in[m] = Math.sqrt(percent_2_in[m] / (double)(simulation)) / percent_in[m];
			else
				percent_2_in[m] = 0;
		}
		for (int m = 0; m < max_degree_out; m++)
		{
			percent_1_out[m] = percent_1_out[m] / (double)(simulation);
			if (percent_out[m] != 0)
				percent_2_out[m] = Math.sqrt(percent_2_out[m] / (double)(simulation)) / percent_out[m];
			else
				percent_2_out[m] = 0;
		}

		FileOutputStream fileOut = new FileOutputStream(path + "AS_in_degree_distribution.txt");
		OutputStreamWriter writer = new OutputStreamWriter(fileOut);
		PrintWriter sw = new PrintWriter(writer);
		for (int m = 0; m < max_degree_in; m++)
		{
			sw.println(Double.toString(percent_1_in[m]));
		}
		sw.close();

		fileOut = new FileOutputStream(path + "AS_out_degree_distribution.txt");
		writer = new OutputStreamWriter(fileOut);
		sw = new PrintWriter(writer);
		for (int m = 0; m < max_degree_out; m++)
		{
			sw.println(Double.toString(percent_1_out[m]));
		}
		sw.close();

		fileOut = new FileOutputStream(path + "AS_in_degree_NMSE.txt");
		writer = new OutputStreamWriter(fileOut);
		sw = new PrintWriter(writer);
		for (int m = 0; m < max_degree_in; m++)
		{
			sw.println(Double.toString(percent_2_in[m]));
		}
		sw.close();

		fileOut = new FileOutputStream(path + "AS_out_degree_NMSE.txt");
		writer = new OutputStreamWriter(fileOut);
		sw = new PrintWriter(writer);
		for (int m = 0; m < max_degree_out; m++)
		{
			sw.println(Double.toString(percent_2_out[m]));
		}
		sw.close();

		System.out.println("AS: Average In Degree = " + avg_degree_in);
		System.out.println("AS: Average Out Degree = " + avg_degree);
		System.out.println("AS: Average Sample Number = " + total_sample / simulation);
		double mix_in = 0.0;
		double mix_out = 0.0;
		for (int m = 0; m < simulation; m++)
		{
			mix_in = mix_in + mixing_time_in[m];
			mix_out = mix_out + mixing_time_out[m];
		}
		System.out.println("AS: Average Mixing Time (In) = " + mix_in / simulation);
		System.out.println("AS: Average Mixing Time (Out) = " + mix_out / simulation);
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
	}
	
	public static void main(String[] args) throws FileNotFoundException
	{
		Init();
		
//		BFS();
//		MHRW();
		AS();
	}
}
