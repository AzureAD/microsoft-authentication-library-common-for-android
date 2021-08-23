//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.

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
using IdentityPerfTestApp;

/**
* C# script to process codemarkers and send the perf diff report! 
* 
* To rum this scrp follow the steps provided in file readme.txt
* 
**/

// namespace declaration 
namespace TestScript
{

    // Class declaration 
    class SendEmail
    {
        // Few constants:
        public const string codemarkerBaseFileNamePreFix = "PerfData"; // Prefix of the files which should be taken as base PerfData files for raw data.
        public const string codemarkerTargetFileNamePreFix = "PerfData"; // Prefix of the files which should be taken as target PerfData files for raw data.
        public const string outputFileLocation = "."; // Directory where target files all interim reports and final diff reports are desired. Example value: "C:\output\"
        public const string outputFileNamePrefix = ""; // Prefix of the file names to be generated. Example value: "run_output"

        public static List<string> GenerateReportBody(ReportParams reportParams)
        {

            HashSet<string> primaryMeasurements = new HashSet<string>();
            HashSet<string> secondaryMeasurements = new HashSet<string>();
            HashSet<string> activeScenarios = new HashSet<string>();
            HashSet<string> activeMeasurements = new HashSet<string>();

            DateTime startTime = DateTime.MinValue;
            PerfMeasurementConfigurationsProvider configProvider;


            string[] baseFileList = System.IO.Directory.GetFiles(reportParams.InputBaseFileLocation, codemarkerBaseFileNamePreFix + "*.txt");
            string[] targetFileList = System.IO.Directory.GetFiles(reportParams.InputTargetFileLocation, codemarkerTargetFileNamePreFix + "*.txt");
            deletePreviousRunOutputCSVs(outputFileLocation, outputFileNamePrefix);


            MeasurementsStore.clear();
            Dictionary<string, List<PerfMeasurementsSet>> baseMeasurements = new Dictionary<string, List<PerfMeasurementsSet>>();
            Dictionary<string, List<PerfMeasurementsSet>> targetMeasurements = new Dictionary<string, List<PerfMeasurementsSet>>();
            foreach (string s in MeasurementsConfiguration.getAllScenarioNames())
            {
                configProvider = new PerfMeasurementConfigurationsProvider(reportParams.AppName, s);
                foreach (String measurementName in configProvider.getActiveMeasurementNames())
                {
                    secondaryMeasurements.Add(measurementName);
                    activeMeasurements.Add(measurementName);
                }
                foreach (KeyValuePair<string, List<PerfMeasurementsSet>> pair in measure(startTime, baseFileList, "Base", configProvider))
                {
                    if (!baseMeasurements.ContainsKey(pair.Key))
                        baseMeasurements.Add(pair.Key, pair.Value);
                }
                foreach (KeyValuePair<string, List<PerfMeasurementsSet>> pair in measure(startTime, targetFileList, "Target", configProvider))
                {
                    if (!targetMeasurements.ContainsKey(pair.Key))
                        targetMeasurements.Add(pair.Key, pair.Value);
                }
            }

            List<string> htmlResult = new List<string>();

            List<Parameter> updatedParameters = new List<Parameter>();

            // Primary measurements are those which are necessary to produce a result to pass the scenario run.
            // If there is starting point of a primary measurement available but not the endpoint of the measurement in the PerfData file, the test will fail. 
            // However, if starting point or end point of a secondary measurement is missing, the test run will pass with ignoring the particular measurement.
            string[] heading = { "Task Runs", reportParams.AppName, reportParams.DeviceModel };

            // Starting creating object Parameter task so that it has all the information to be written to the HTML and Email.
            Parameter task = new Parameter("Response Time(seconds)", "NA", 3, "lemon");
            task.BaseCheckpoint = reportParams.BaseJobId;
            task.TargetCheckPoint = reportParams.CurrentJobId;
            task.BaseLogDir = reportParams.BaseJobArtifactURL;
            task.TargetLogDir = reportParams.TargetJobArtifactURL;

            task.BaseScenarioToPerfValueMap = new Dictionary<string, Dictionary<string, double>>();
            task.TargetScenarioToPerfValueMap = new Dictionary<string, Dictionary<string, double>>();

            foreach (string key in baseMeasurements.Keys)
            {

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

                if (!task.TargetScenarioToPerfValueMap.ContainsKey(key))
                {
                    task.TargetScenarioToPerfValueMap.Add(key, new Dictionary<string, double>());
                }

                foreach (PerfMeasurementsSet pms in targetMeasurements[key])
                {
                    if (!task.TargetScenarioToPerfValueMap[key].ContainsKey(pms._measurementConfiguration.Name))
                    {
                        // Below, we can take MeasurementValue of any kind like average, best75 percentile, 75 percentile, min, max or any other one available in pms object.
                        task.TargetScenarioToPerfValueMap[key].Add(pms._measurementConfiguration.Name, pms._average.ResponseTime.MeasurementValue);
                    }
                }
                activeScenarios.Add(key);
            }
            updatedParameters.Add(task);

            htmlResult.Add(View.CreateTableHtml(updatedParameters, primaryMeasurements, secondaryMeasurements,
                                        heading, activeScenarios, activeMeasurements, reportParams.CurrentJobId, reportParams.RunName));

            return htmlResult;
        }

        //delete previous csvs
        private static void deletePreviousRunOutputCSVs(string outputFileLocation, string outputFileNamePrefix)
        {
            string[] fileList = System.IO.Directory.GetFiles(outputFileLocation, outputFileNamePrefix + "*.csv");
            foreach (string file in fileList)
            {
                System.Diagnostics.Debug.WriteLine(file + "will be deleted");
                System.IO.File.Delete(file);
            }
        }

        // do measurement
        private static Dictionary<string, List<PerfMeasurementsSet>> measure(DateTime startTime, string[] files,
            string typeOfBuild, PerfMeasurementConfigurationsProvider configProvider)
        {
            string scenario = configProvider.getScenarioName();
            List<PerfMeasurementConfiguration> measurementConfigurations = configProvider.getActiveMeasurements();
            MeasurementsStore.clear();
            foreach (string file in files)
            {
                PerfData perfData = new PerfData(file);
                perfData.AddPidCreationTime(startTime);
                perfData.AddActivityDisplayTime(0);

                MeasurementsStore.AddScenarioIterationMeasurements(scenario, perfData, measurementConfigurations);

                string perfDataModifiedFileOnHost = "generatedPerfData_" + scenario + "_" + file.Substring(file.Length - 7, 3) + "_" + typeOfBuild + ".csv";
                perfData.AddMarkerNames();
                perfData.AdjustTimeElapsed();
            }
            MeasurementsStore.GenerateAggregateMeasurements();

            MeasurementsStore.DumpAllMeasurementsDataToFile("generatedDumpAllMeasurements" + scenario + "_" + typeOfBuild + ".csv");
            MeasurementsStore.DumpResponseTimeSummaryToFile("generatedDumpResponseTimeSummary" + scenario + "_" + typeOfBuild + ".csv");

            return MeasurementsStore.AllScenarioMeasurements;
        }
    }
}
