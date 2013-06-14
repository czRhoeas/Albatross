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



using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.IO;

namespace multi_sampling
{
    class Program
    {
        static List<int>[] list;              // Original Graph: out degree
        static List<int>[] list_in;           // Original Graph: in degree
        static List<int>[] list_undirected;   // Original Graph -> Undirected Graph
        static double[] percent_in;           // True Value
        static double[] percent_out;
        static double[] percent_1_in;         // CDF
        static double[] percent_1_out;
        static double[] percent_2_in;         // NMSE
        static double[] percent_2_out;
        static double[] percent_3_in;         // Each Time's Estimation
        static double[] percent_3_out;
        static bool[] node;
        static int max_degree_in;
        static int max_degree_out;
        static int node_number;
        static int real_node_number = 0;
        static int edge_number;
        static int simulation = 1000;
        static int sample_size;
        static double alpha = 0.02;           // Jump Probability in AS
        static int jump_budget = 10;          // Set Jump-Cost
        static string path = "";              // Fill in the file path
        static string filename = "";          // Fill in the file name

        static void Init()
        {
            StreamReader sr = new StreamReader(new FileStream(path + filename, FileMode.Open));
            node_number = int.Parse(sr.ReadLine());
            edge_number = int.Parse(sr.ReadLine());
            list = new List<int>[node_number];
            list_in = new List<int>[node_number];
            list_undirected = new List<int>[node_number];
            node = new bool[node_number + 1];
            for (int i = 0; i < node_number; i++)
            {
                list[i] = new List<int>();
                list_in[i] = new List<int>();
                list_undirected[i] = new List<int>();
            }
            const string split_flag = "\t";
            int FromNode = 0, ToNode = 0;
            int edge_count_1 = 0;
            int edge_count_2 = 0;
            while (edge_count_1+edge_count_2 < edge_number)
            {
                string str = sr.ReadLine();
                string[] split = str.Split(split_flag.ToCharArray());
                FromNode = int.Parse(split[0]);
                ToNode = int.Parse(split[1]);
                if (FromNode == ToNode)
                {
                    edge_count_2++;
                    continue;
                }
                list[FromNode].Add(ToNode);
                list_in[ToNode].Add(FromNode);
                if (!list_undirected[FromNode].Contains(ToNode))
                    list_undirected[FromNode].Add(ToNode);
                if (!list_undirected[ToNode].Contains(FromNode))
                    list_undirected[ToNode].Add(FromNode);
                node[FromNode] = true;
                node[ToNode] = true;
                edge_count_1++;
            }
            sr.Close();

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
                if (list_in[i].Count > max_degree_in)
                    max_degree_in = list_in[i].Count;
                if (list[i].Count > max_degree_out)
                    max_degree_out = list[i].Count;
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
                percent_in[list_in[i].Count] = percent_in[list_in[i].Count] + 1;
                percent_out[list[i].Count] = percent_out[list[i].Count] + 1;
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

            Console.WriteLine("Test File: " + filename);
            Console.WriteLine("Test Path: " + path);
            Console.WriteLine("Average Degree = " + (double)edge_count_1 / (double)real_node_number);
            Console.WriteLine("Simulation Times = " + simulation);
            Console.WriteLine("Node Number = " + node_number);
            Console.WriteLine("Real Node Number = " + real_node_number);
            Console.WriteLine("Sample Budget = " + sample_size);
            Console.WriteLine("Jump Alpha = " + alpha);
            Console.WriteLine("");

            FileStream fs = new FileStream(path + "Original_graph_in_degree_distribution.txt", FileMode.Create);
            StreamWriter sw = new StreamWriter(fs);
            for (int i = 0; i < max_degree_in; i++)
            {
                sw.WriteLine(percent_in[i].ToString());
            }
            sw.Close();

            fs = new FileStream(path + "Original_graph_out_degree_distribution.txt", FileMode.Create);
            sw = new StreamWriter(fs);
            for (int i = 0; i < max_degree_out; i++)
            {
                sw.WriteLine(percent_out[i].ToString());
            }
            sw.Close();
        }

        static void MHRW()
        {
            Random ra = new Random();
            Queue<int> sampled_node = new Queue<int>();
            Queue<int> query_node = new Queue<int>();
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
                v = ra.Next() % list_undirected.Length;
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
                    if (list_undirected[v].Count == 0)
                    {
                        v = ra.Next() % (list_undirected.Length);
                        single_sample++;
                        sampled_node.Enqueue(v);
                        if (!query_node.Contains(v))
                        {
                            i++;
                            query_node.Enqueue(v);
                        }
                        for (int m = list_in[v].Count; m <= max_degree_in; m++)
                        {
                            percent_3_in[m] = percent_3_in[m] + 1.0;
                        }
                        for (int m = list[v].Count; m <= max_degree_out; m++)
                        {
                            percent_3_out[m] = percent_3_out[m] + 1.0;
                        }
                        mix = 0;
                        for (mix = 0; mix < max_degree_in + 1; mix++)
                        {
                            if (Math.Abs(percent_3_in[mix] / single_sample - percent_in[mix]) > 0.25)
                                break;
                        }
                        if (mix == max_degree_in + 1 && mixing_time_in[count] == sample_node_number)
                            mixing_time_in[count] = i;
                        else if (mix != max_degree_in + 1)
                            mixing_time_in[count] = sample_node_number;

                        mix = 0;
                        for (mix = 0; mix < max_degree_out + 1; mix++)
                        {
                            if (Math.Abs(percent_3_out[mix] / single_sample - percent_out[mix]) > 0.25)
                                break;
                        }
                        if (mix == max_degree_out + 1 && mixing_time_out[count] == sample_node_number)
                            mixing_time_out[count] = i;
                        else if (mix != max_degree_out + 1)
                            mixing_time_out[count] = sample_node_number;
                        continue;
                    }
                    w = list_undirected[v].ElementAt(ra.Next(0, list_undirected[v].Count));
                    double p = ra.NextDouble();
                    if (p <= (double)list_undirected[v].Count / (double)list_undirected[w].Count)
                    {
                        v = w;
                        single_sample++;
                        sampled_node.Enqueue(v);
                    }
                    else
                    {
                        single_sample++;
                        sampled_node.Enqueue(v);
                    }

                    if (!query_node.Contains(w))
                    {
                        i++;
                        query_node.Enqueue(w);
                    }

                    for (int m = list_in[v].Count; m <= max_degree_in; m++)
                    {
                        percent_3_in[m] = percent_3_in[m] + 1.0;
                    }
                    for (int m = list[v].Count; m <= max_degree_out; m++)
                    {
                        percent_3_out[m] = percent_3_out[m] + 1.0;
                    }
                    mix = 0;
                    for (mix = 0; mix < max_degree_in + 1; mix++)
                    {
                        if (Math.Abs(percent_3_in[mix] / single_sample - percent_in[mix]) > 0.25)
                            break;
                    }
                    if (mix == max_degree_in + 1 && mixing_time_in[count] == sample_node_number)
                        mixing_time_in[count] = i;
                    else if (mix != max_degree_in + 1)
                        mixing_time_in[count] = sample_node_number;

                    mix = 0;
                    for (mix = 0; mix < max_degree_out + 1; mix++)
                    {
                        if (Math.Abs(percent_3_out[mix] / single_sample - percent_out[mix]) > 0.25)
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
                sampled_node.Clear();
                query_node.Clear();
            }
            avg_degree = temp / (double)(total_sample);
            avg_degree_in = temp_in / (double)(total_sample);

            for (int m = 0; m < max_degree_in; m++)
            {
                percent_1_in[m] = percent_1_in[m] / (double)(simulation);
                if (percent_in[m] != 0)
                    percent_2_in[m] = Math.Sqrt(percent_2_in[m] / (double)(simulation)) / percent_in[m];
                else
                    percent_2_in[m] = 0;
            }
            for (int m = 0; m < max_degree_out; m++)
            {
                percent_1_out[m] = percent_1_out[m] / (double)(simulation);
                if (percent_out[m] != 0)
                    percent_2_out[m] = Math.Sqrt(percent_2_out[m] / (double)(simulation)) / percent_out[m];
                else
                    percent_2_out[m] = 0;
            }

            FileStream fs = new FileStream(path + "MHRW_in_degree_distribution.txt", FileMode.Create);
            StreamWriter sw = new StreamWriter(fs);
            for (int m = 0; m < max_degree_in; m++)
            {
                sw.WriteLine(percent_1_in[m].ToString());
            }
            sw.Close();

            fs = new FileStream(path + "MHRW_out_degree_distribution.txt", FileMode.Create);
            sw = new StreamWriter(fs);
            for (int m = 0; m < max_degree_out; m++)
            {
                sw.WriteLine(percent_1_out[m].ToString());
            }
            sw.Close();

            fs = new FileStream(path + "MHRW_in_degree_NMSE.txt", FileMode.Create);
            sw = new StreamWriter(fs);
            for (int m = 0; m < max_degree_in; m++)
            {
                sw.WriteLine(percent_2_in[m].ToString());
            }
            sw.Close();

            fs = new FileStream(path + "MHRW_out_degree_NMSE.txt", FileMode.Create);
            sw = new StreamWriter(fs);
            for (int m = 0; m < max_degree_out; m++)
            {
                sw.WriteLine(percent_2_out[m].ToString());
            }
            sw.Close();

            Console.WriteLine("MHRW: Average In Degree = " + avg_degree_in);
            Console.WriteLine("MHRW: Average Out Degree = " + avg_degree);
            Console.WriteLine("MHRW: Average Sample Number = " + total_sample / simulation);

            double mix_in = 0.0;
            double mix_out = 0.0;
            for (int m = 0; m < simulation;m++ )
            {
                mix_in = mix_in + mixing_time_in[m];
                mix_out = mix_out + mixing_time_out[m];
            }
            Console.WriteLine("MHRW: Average Mixing Time (In) = " + mix_in / simulation);
            Console.WriteLine("MHRW: Average Mixing Time (Out) = " + mix_out / simulation);
        }

        static void BFS()
        {
            Random ra = new Random();
            Queue<int> sampled_node = new Queue<int>();
            Queue<int> waiting_node = new Queue<int>();
            Queue<int> query_node = new Queue<int>();
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
            bool[] waiting_flag = new bool[node_number + 1];
            bool jump_flag = false;

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

                v = ra.Next() % list_undirected.Length;
                for (int m = 0; m < node_number + 1; m++)
                {
                    waiting_flag[m] = false;
                }
                waiting_node.Enqueue(v);
                waiting_flag[v] = true;
                jump_flag = false;
                while (i < sample_node_number)
                {
                    if (waiting_node.Count > 0)
                    {
                        v = waiting_node.Dequeue();
                        single_sample++;
                        sampled_node.Enqueue(v);
                        if (!query_node.Contains(v))
                        {
                            if(jump_flag == false)
                            {
                                i++;
                                query_node.Enqueue(v);
                            }
                            else
                            {
                                i = i + jump_budget;
                                query_node.Enqueue(v);
                                jump_flag = false;
                            }
                        }
                        for (int en_count = 0; en_count < list_undirected[v].Count; en_count++)
                        {
                            w = list_undirected[v].ElementAt(en_count);
                            if(waiting_flag[w] == false)
                            {
                                waiting_node.Enqueue(w);
                                waiting_flag[w] = true;
                            }
                        }
                        for (int m = list_in[v].Count; m <= max_degree_in; m++)
                        {
                            percent_3_in[m] = percent_3_in[m] + 1.0;
                        }
                        for (int m = list[v].Count; m <= max_degree_out; m++)
                        {
                            percent_3_out[m] = percent_3_out[m] + 1.0;
                        }
                        mix = 0;
                        for (mix = 0; mix < max_degree_in + 1; mix++)
                        {
                            if (Math.Abs(percent_3_in[mix] / single_sample - percent_in[mix]) > 0.25)
                                break;
                        }
                        if (mix == max_degree_in + 1 && mixing_time_in[count] == sample_node_number)
                            mixing_time_in[count] = i;
                        else if (mix != max_degree_in + 1)
                            mixing_time_in[count] = sample_node_number;

                        mix = 0;
                        for (mix = 0; mix < max_degree_out + 1; mix++)
                        {
                            if (Math.Abs(percent_3_out[mix] / single_sample - percent_out[mix]) > 0.25)
                                break;
                        }
                        if (mix == max_degree_out + 1 && mixing_time_out[count] == sample_node_number)
                            mixing_time_out[count] = i;
                        else if (mix != max_degree_out + 1)
                            mixing_time_out[count] = sample_node_number;
                    }
                    else
                    {
                        v = ra.Next() % (list_undirected.Length);
                        waiting_node.Enqueue(v);
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

                sampled_node.Clear();
                query_node.Clear();
                waiting_node.Clear();
            }
            avg_degree = temp / (double)(total_sample);
            avg_degree_in = temp_in / (double)(total_sample);

            for (int m = 0; m < max_degree_in; m++)
            {
                percent_1_in[m] = percent_1_in[m] / (double)(simulation);
                if (percent_in[m] != 0)
                    percent_2_in[m] = Math.Sqrt(percent_2_in[m] / (double)(simulation)) / percent_in[m];
                else
                    percent_2_in[m] = 0;
            }
            for (int m = 0; m < max_degree_out; m++)
            {
                percent_1_out[m] = percent_1_out[m] / (double)(simulation);
                if (percent_out[m] != 0)
                    percent_2_out[m] = Math.Sqrt(percent_2_out[m] / (double)(simulation)) / percent_out[m];
                else
                    percent_2_out[m] = 0;
            }

            FileStream fs = new FileStream(path + "BFS_in_degree_distribution.txt", FileMode.Create);  //输出点和它们在原图中的度 degree
            StreamWriter sw = new StreamWriter(fs);
            for (int m = 0; m < max_degree_in; m++)
            {
                sw.WriteLine(percent_1_in[m].ToString());
            }
            sw.Close();

            fs = new FileStream(path + "BFS_out_degree_distribution.txt", FileMode.Create);  //输出点和它们在原图中的度 degree
            sw = new StreamWriter(fs);
            for (int m = 0; m < max_degree_out; m++)
            {
                sw.WriteLine(percent_1_out[m].ToString());
            }
            sw.Close();

            fs = new FileStream(path + "BFS_in_degree_NMSE.txt", FileMode.Create);
            sw = new StreamWriter(fs);
            for (int m = 0; m < max_degree_in; m++)
            {
                sw.WriteLine(percent_2_in[m].ToString());
            }
            sw.Close();

            fs = new FileStream(path + "BFS_out_degree_NMSE.txt", FileMode.Create);
            sw = new StreamWriter(fs);
            for (int m = 0; m < max_degree_out; m++)
            {
                sw.WriteLine(percent_2_out[m].ToString());
            }
            sw.Close();

            Console.WriteLine("BFS: Average In Degree = " + avg_degree_in);
            Console.WriteLine("BFS: Average Out Degree = " + avg_degree);
            Console.WriteLine("BFS: Average Sample Number = " + total_sample / simulation);

            double mix_in = 0.0;
            double mix_out = 0.0;
            for (int m = 0; m < simulation; m++)
            {
                mix_in = mix_in + mixing_time_in[m];
                mix_out = mix_out + mixing_time_out[m];
            }
            Console.WriteLine("BFS: Average Mixing Time (In) = " + mix_in / simulation);
            Console.WriteLine("BFS: Average Mixing Time (Out) = " + mix_out / simulation);
        }

        static void AS()
        {
            Random ra = new Random();
            Queue<int> sampled_node = new Queue<int>();
            Queue<int> query_node = new Queue<int>();
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
                v = ra.Next() % list_undirected.Length;

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
                    double q = ra.NextDouble();
                    if (q < alpha)
                    {
                        v = ra.Next() % (list_undirected.Length);
                        single_sample++;
                        sampled_node.Enqueue(v);
                        if (!query_node.Contains(v))
                        {
                            i = i + jump_budget;
                            query_node.Enqueue(v);
                        }

                        for (int m = list_in[v].Count; m <= max_degree_in; m++)
                        {
                            percent_3_in[m] = percent_3_in[m] + 1.0;
                        }
                        for (int m = list[v].Count; m <= max_degree_out; m++)
                        {
                            percent_3_out[m] = percent_3_out[m] + 1.0;
                        }
                        mix = 0;
                        for (mix = 0; mix < max_degree_in + 1; mix++)
                        {
                            if (Math.Abs(percent_3_in[mix] / single_sample - percent_in[mix]) > 0.25)
                                break;
                        }
                        if (mix == max_degree_in + 1 && mixing_time_in[count] == sample_node_number)
                            mixing_time_in[count] = i;
                        else if (mix != max_degree_in + 1)
                            mixing_time_in[count] = sample_node_number;

                        mix = 0;
                        for (mix = 0; mix < max_degree_out + 1; mix++)
                        {
                            if (Math.Abs(percent_3_out[mix] / single_sample - percent_out[mix]) > 0.25)
                                break;
                        }
                        if (mix == max_degree_out + 1 && mixing_time_out[count] == sample_node_number)
                            mixing_time_out[count] = i;
                        else if (mix != max_degree_out + 1)
                            mixing_time_out[count] = sample_node_number;
                        continue;
                    }
                    if (list_undirected[v].Count == 0)
                    {
                        v = ra.Next() % (list_undirected.Length);
                        single_sample++;
                        sampled_node.Enqueue(v);
                        if (!query_node.Contains(v))
                        {
                            i++;
                            query_node.Enqueue(v);
                        }
                        for (int m = list_in[v].Count; m <= max_degree_in; m++)
                        {
                            percent_3_in[m] = percent_3_in[m] + 1.0;
                        }
                        for (int m = list[v].Count; m <= max_degree_out; m++)
                        {
                            percent_3_out[m] = percent_3_out[m] + 1.0;
                        }
                        mix = 0;
                        for (mix = 0; mix < max_degree_in + 1; mix++)
                        {
                            if (Math.Abs(percent_3_in[mix] / single_sample - percent_in[mix]) > 0.25)
                                break;
                        }
                        if (mix == max_degree_in + 1 && mixing_time_in[count] == sample_node_number)
                            mixing_time_in[count] = i;
                        else if (mix != max_degree_in + 1)
                            mixing_time_in[count] = sample_node_number;

                        mix = 0;
                        for (mix = 0; mix < max_degree_out + 1; mix++)
                        {
                            if (Math.Abs(percent_3_out[mix] / single_sample - percent_out[mix]) > 0.25)
                                break;
                        }
                        if (mix == max_degree_out + 1 && mixing_time_out[count] == sample_node_number)
                            mixing_time_out[count] = i;
                        else if (mix != max_degree_out + 1)
                            mixing_time_out[count] = sample_node_number;
                        continue;
                    }
                    w = list_undirected[v].ElementAt(ra.Next(0, list_undirected[v].Count));
                    double p = ra.NextDouble();
                    if (p <= (double)list_undirected[v].Count / (double)list_undirected[w].Count)
                    {
                        v = w;
                        single_sample++;
                        sampled_node.Enqueue(v);
                    }
                    else
                    {
                        single_sample++;
                        sampled_node.Enqueue(v);
                    }
                    if (!query_node.Contains(w))
                    {
                        i++;
                        query_node.Enqueue(w);
                    }
                    for (int m = list_in[v].Count; m <= max_degree_in; m++)
                    {
                        percent_3_in[m] = percent_3_in[m] + 1.0;
                    }
                    for (int m = list[v].Count; m <= max_degree_out; m++)
                    {
                        percent_3_out[m] = percent_3_out[m] + 1.0;
                    }
                    mix = 0;
                    for (mix = 0; mix < max_degree_in + 1; mix++)
                    {
                        if (Math.Abs(percent_3_in[mix] / single_sample - percent_in[mix]) > 0.25)
                            break;
                    }
                    if (mix == max_degree_in + 1 && mixing_time_in[count] == sample_node_number)
                        mixing_time_in[count] = i;
                    else if (mix != max_degree_in + 1)
                        mixing_time_in[count] = sample_node_number;

                    mix = 0;
                    for (mix = 0; mix < max_degree_out + 1; mix++)
                    {
                        if (Math.Abs(percent_3_out[mix] / single_sample - percent_out[mix]) > 0.25)
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
                sampled_node.Clear();
                query_node.Clear();
            }
            avg_degree = temp / (double)(total_sample);
            avg_degree_in = temp_in / (double)(total_sample);
            for (int m = 0; m < max_degree_in; m++)
            {
                percent_1_in[m] = percent_1_in[m] / (double)(simulation);
                if (percent_in[m] != 0)
                    percent_2_in[m] = Math.Sqrt(percent_2_in[m] / (double)(simulation)) / percent_in[m];
                else
                    percent_2_in[m] = 0;
            }
            for (int m = 0; m < max_degree_out; m++)
            {
                percent_1_out[m] = percent_1_out[m] / (double)(simulation);
                if (percent_out[m] != 0)
                    percent_2_out[m] = Math.Sqrt(percent_2_out[m] / (double)(simulation)) / percent_out[m];
                else
                    percent_2_out[m] = 0;
            }

            FileStream fs = new FileStream(path + "AS_in_degree_distribution.txt", FileMode.Create);
            StreamWriter sw = new StreamWriter(fs);
            for (int m = 0; m < max_degree_in; m++)
            {
                sw.WriteLine(percent_1_in[m].ToString());
            }
            sw.Close();

            fs = new FileStream(path + "AS_out_degree_distribution.txt", FileMode.Create);
            sw = new StreamWriter(fs);
            for (int m = 0; m < max_degree_out; m++)
            {
                sw.WriteLine(percent_1_out[m].ToString());
            }
            sw.Close();

            fs = new FileStream(path + "AS_in_degree_NMSE.txt", FileMode.Create);
            sw = new StreamWriter(fs);
            for (int m = 0; m < max_degree_in; m++)
            {
                sw.WriteLine(percent_2_in[m].ToString());
            }
            sw.Close();

            fs = new FileStream(path + "AS_out_degree_NMSE.txt", FileMode.Create);
            sw = new StreamWriter(fs);
            for (int m = 0; m < max_degree_out; m++)
            {
                sw.WriteLine(percent_2_out[m].ToString());
            }
            sw.Close();

            Console.WriteLine("AS: Average In Degree = " + avg_degree_in);
            Console.WriteLine("AS: Average Out Degree = " + avg_degree);
            Console.WriteLine("AS: Average Sample Number = " + total_sample / simulation);
            double mix_in = 0.0;
            double mix_out = 0.0;
            for (int m = 0; m < simulation; m++)
            {
                mix_in = mix_in + mixing_time_in[m];
                mix_out = mix_out + mixing_time_out[m];
            }
            Console.WriteLine("AS: Average Mixing Time (In) = " + mix_in / simulation);
            Console.WriteLine("AS: Average Mixing Time (Out) = " + mix_out / simulation);
        }

        static void Main()
        {
            Init();
            BFS();
            MHRW();
            AS();
        }
    }
}