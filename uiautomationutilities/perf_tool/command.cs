/**
* C# script to print send Email! 
* 
* To rum this scrp run following 2 commands from powershell/command  prompt:
* csc Email.cs
* ./Email.exe senderEmailId recieverEmailId
* 
**/
using System; 
using System.Net.Mail;
using System.Net;
using System.IO;
using System.Net.Mime;
using PerfClTool.Measurement;
using System.Collections.Generic;
using System.Collections;
using PerfDiffResultMailer;
using PerfClTool;
using ConsoleApp1.measurement_definitions;
using ConsoleApp1;




// namespace declaration 
namespace TestScript { 
    
    // Class declaration 
    class SendEmail { 
        
        // Main Method - Takes 2 arguments sender's outlook email(from which report email needs to be sent) & reciever's outlook email
        public static void main(string[] args)
        {
            for (int i = 0; i < 14; i++)
                Console.WriteLine(args[i]);

            string inputFileLocation = args[0];
            string outputFileLocation = args[1];
            string codemarkerBaseFileNamePreFix = args[2];
            string codemarkerTargetFileNamePreFix = args[3];
            string outputFileNamePrefix = args[4];
            string jobID = args[5];
            string deviceModel = args[6];
            string OS = args[7];
            string baseBuild = args[8];
            string targetBuild = args[9];
            string appName = args[10];
            string fromAddress = args[11];
            string fromPassword = args[12];
            string emailToList = args[13];

            

            DateTime startTime = DateTime.MinValue;
            MSALAcquireTokenPerfMeasurementConfigurationsProvider configProvider = new MSALAcquireTokenPerfMeasurementConfigurationsProvider();
            List<PerfMeasurementConfiguration> msalAcquireTokenMeasurementConfigurations = configProvider.getActiveMeasurements();
            deletePreviousRunOutputCSVs(outputFileLocation, outputFileNamePrefix);

            string[] baseFileList = System.IO.Directory.GetFiles(inputFileLocation, codemarkerBaseFileNamePreFix + "*.txt");
            Dictionary<string, List<PerfMeasurementsSet>> baseMeasurements = measure(startTime, msalAcquireTokenMeasurementConfigurations, baseFileList, "Base", configProvider.getScenarioName());

            string[] targetFileList = System.IO.Directory.GetFiles(inputFileLocation, codemarkerTargetFileNamePreFix + "*.txt");
            Dictionary<string, List<PerfMeasurementsSet>> targetMeasurements = measure(startTime, msalAcquireTokenMeasurementConfigurations, targetFileList, "Target", configProvider.getScenarioName());

            List<string> htmlResult = new List<string>();

            List<string> jobInfoHtml = View.ResultInit();
            jobInfoHtml.AddRange(View.InfoInit(jobID, deviceModel, OS, "command"));

            List<Task> baseTasks = new List<Task>();
            baseTasks.Add(createTask(deviceModel, baseBuild, appName));

            List<Task> targetTasks = new List<Task>();
            targetTasks.Add(createTask(deviceModel, targetBuild, appName));

            jobInfoHtml.Add(View.CreateAppInfoTable(baseTasks, targetTasks, appName));

            List<Parameter> updatedParameters = new List<Parameter>();
            HashSet<string> primaryMeasurements = new HashSet<string>();
            HashSet<string> secondaryMeasurements = new HashSet<string>();
            string[] heading = { "Task Runs", baseTasks[0].AppName, baseTasks[0].Device };
            HashSet<string> activeScenarios = new HashSet<string>();
            HashSet<string> activeMeasurements = new HashSet<string>();

            Parameter task = new Parameter("Response Time(ms)", "NA", 3, "lemon");
            task.BaseCheckpoint = baseBuild;
            task.TargetCheckPoint = targetBuild;
            foreach (string key in baseMeasurements.Keys)
            {
                if (task.BaseScenarioToPerfValueMap == null)
                {
                    task.BaseScenarioToPerfValueMap = new Dictionary<string, Dictionary<string, double>>();
                }

                if (!task.BaseScenarioToPerfValueMap.ContainsKey(key))
                {
                    task.BaseScenarioToPerfValueMap.Add(key, new Dictionary<string, double>());
                }

                foreach (PerfMeasurementsSet pms in baseMeasurements[key])
                {
                    if (!task.BaseScenarioToPerfValueMap[key].ContainsKey(pms._measurementConfiguration.Name))
                    {
                        task.BaseScenarioToPerfValueMap[key].Add(pms._measurementConfiguration.Name, pms._average.ResponseTime.MeasurementValue);
                    }
                }
                //baseMeasurements[key];
            }

            foreach (string key in targetMeasurements.Keys)
            {
                if (task.TargetScenarioToPerfValueMap == null)
                {
                    task.TargetScenarioToPerfValueMap = new Dictionary<string, Dictionary<string, double>>();
                }

                if (!task.TargetScenarioToPerfValueMap.ContainsKey(key))
                {
                    task.TargetScenarioToPerfValueMap.Add(key, new Dictionary<string, double>());
                }

                foreach (PerfMeasurementsSet pms in targetMeasurements[key])
                {
                    if (!task.TargetScenarioToPerfValueMap[key].ContainsKey(pms._measurementConfiguration.Name))
                    {
                        task.TargetScenarioToPerfValueMap[key].Add(pms._measurementConfiguration.Name, pms._average.ResponseTime.MeasurementValue);
                    }
                }
                activeScenarios.Add(key);
            }
            updatedParameters.Add(task);
            
            foreach(String measurementName in configProvider.getActiveMeasurementNames()) { 
                secondaryMeasurements.Add(measurementName);
                activeMeasurements.Add(measurementName);
            }

            htmlResult.Add(View.CreateTableHtml(updatedParameters, primaryMeasurements, secondaryMeasurements,
                                        heading, activeScenarios, activeMeasurements));

            jobInfoHtml.Add(View.EndofJob());
            jobInfoHtml.AddRange(htmlResult);
            jobInfoHtml.Add(View.BuildEndOfHTML());

            File.WriteAllLines(outputFileLocation + "diff.html", jobInfoHtml);
            String emailBody = "";
            foreach (string s in jobInfoHtml)
            {
                emailBody += s;
            }
            ReportHelper.ShowResultNSendEmail(emailBody, fromAddress, fromPassword, emailToList);
        }

        private static Task createTask(string deviceModel, string baseBuild, string appName)
        {
            Task baseTask = new Task();
            baseTask.Checkpoint = baseBuild;
            baseTask.AppName = appName;
            baseTask.Device = deviceModel;
            baseTask.FeatureGateOverrides = new Dictionary<string, string>();
            return baseTask;
        }

        private static void deletePreviousRunOutputCSVs(string outputFileLocation, string outputFileNamePrefix)
        {
            string[] fileList = System.IO.Directory.GetFiles(outputFileLocation, outputFileNamePrefix + "*.csv");
            foreach (string file in fileList)
            {
                System.Diagnostics.Debug.WriteLine(file + "will be deleted");
                System.IO.File.Delete(file);
            }
        }

        private static Dictionary<string, List<PerfMeasurementsSet>> measure(DateTime startTime, List<PerfMeasurementConfiguration> measurementConfigurations, string[] files, string typeOfBuild, string scenario)
        {
            MeasurementsStore.clear();
            foreach (string file in files) { 
                PerfData perfData = new PerfData(file);
                perfData.AddPidCreationTime(startTime);
                perfData.AddActivityDisplayTime(0);

                MeasurementsStore.AddScenarioIterationMeasurements(scenario, perfData, measurementConfigurations);
                
                string perfDataModifiedFileOnHost = "generatedPerfData_" + scenario + "_" + file.Substring(file.Length-7,3) + "_" + typeOfBuild + ".csv";
                perfData.AddMarkerNames();
                perfData.AdjustTimeElapsed();
                PerfData.AppendAllHeadersToFile(perfDataModifiedFileOnHost);
                perfData.AppendMarkersDataToFile(perfDataModifiedFileOnHost);
                perfData.AppendMarkersDataToFile("generatedBeautified_" + scenario + "_" + file.Substring(file.Length - 7, 3) + "_" + typeOfBuild + ".csv");
            }
            MeasurementsStore.GenerateAggregateMeasurements();

            MeasurementsStore.DumpAllMeasurementsDataToFile("generatedDumpAllMeasurements" + scenario + "_" + typeOfBuild + ".csv");
            MeasurementsStore.DumpResponseTimeSummaryToFile("generatedDumpResponseTimeSummary" + scenario + "_" + typeOfBuild + ".csv");
            MeasurementsStore.DumpVssEndSummaryToFile("generatedDumpVssEndSummary" + scenario + "_" + typeOfBuild + ".csv");
            MeasurementsStore.DumpRssEndSummaryToFile("generatedDumpRssEndSummary" + scenario + "_" + typeOfBuild + ".csv");
            return MeasurementsStore.AllScenarioMeasurements;
        }
    } 
} 
