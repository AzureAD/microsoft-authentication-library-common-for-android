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
using TestScript;
using System.IO;
using System.Collections.Generic;
using PerfDiffResultMailer;
using PerfClTool;

namespace IdentityPerfTestApp
{
    class Program
    {

        /// <summary>
        /// Main Method - takes several arguments elaborated below that configure different aspects of the tool
        /// </summary>
        /// <param name="args"></param>
        static void Main(string[] args)
        {
            int i;
            for (i = 0; i < args.Length; i++)
            {
                Console.WriteLine(args[i]);
            }
            i = 0;
            string inputBaseFileLocation = args[i++]; // Directory where PerfData base files are present. Example value: "C:\testdata\basefiles" 
            string inputLatestFileLocation = args[i++]; // Directory where PerfData base files for latest run are present. Example value: "C:\testdata\basefiles_latest" 
            string inputTargetFileLocation = args[i++]; // Directory where PerfData target files are present. Example value: "C:\testdata\targetfiles" 
            string baseJobId = args[i++]; // Build id of the base task.
            string latestJobId = args[i++]; // Build id of the latest task.
            string currentJobId = args[i++]; // Build id which should be written in the final Email report and used for going to artifact url. Example value: "1234"
            string deviceModel = args[i++]; // Device model to be written in the final Email report. Example value: "Pixel2"
            string deviceOs = args[i++]; // Device OS to be written in the final Email report. Example value: "API28"
            string appName = args[i++]; // App name to be written in the Email report. Example value: "MSALTestApp"
            string fromAddress = args[i++]; // Email ID of the sender's account. Example value: "idlab1@msidlab4.onmicrosoft.com"
            string fromPassword = args[i++]; // Password of the sender's account.
            string emailToList = args[i++]; // Email To list separated by comma

            ReportParams reportParams = new ReportParams(inputBaseFileLocation, inputTargetFileLocation, baseJobId, currentJobId,
                deviceModel, deviceOs, appName, fromAddress, fromPassword, emailToList, "Last Released Build");

            List<string> reportHtml = View.ResultInit();
            reportHtml.AddRange(View.ReportSummaryHeaders());

            List<Task> baseTasks = new List<Task>();
            baseTasks.Add(CreateTask(deviceModel, baseJobId, appName, currentJobId, reportParams.BaseJobArtifactURL));

            List<Task> latestTasks = new List<Task>();
            latestTasks.Add(CreateTask(deviceModel, latestJobId, appName, currentJobId, ReportParams.GetJobArtifactURL(latestJobId)));

            List<Task> targetTasks = new List<Task>();
            targetTasks.Add(CreateTask(deviceModel, currentJobId, appName, currentJobId, reportParams.TargetJobArtifactURL));

            reportHtml.Add(View.ReportSummaryHeaderValues(baseTasks, latestTasks, targetTasks, appName, deviceModel, deviceOs));

            reportHtml.Add(View.EndofJobDetailsTable());

            List<string> baseBody = SendEmail.GenerateReportBody(reportParams);
            reportHtml.AddRange(baseBody);

            reportParams.BaseJobId = latestJobId;
            reportParams.BaseJobArtifactURL = ReportParams.GetJobArtifactURL(latestJobId);
            reportParams.InputBaseFileLocation = inputLatestFileLocation;
            reportParams.RunName = "Last Dev Build";
            List<string> latestBody = SendEmail.GenerateReportBody(reportParams);
            reportHtml.AddRange(latestBody);

            reportHtml.Add(View.BuildEndOfHTML());

            File.WriteAllLines(SendEmail.outputFileLocation + "diff.html", reportHtml);
            String emailBody = "";
            foreach (string s in reportHtml)
            {
                emailBody += s;
            }
            ReportHelper.ShowResultNSendEmail(emailBody, fromAddress, fromPassword, emailToList);
        }

        //create a task
        private static Task CreateTask(string deviceModel, string baseBuild, string appName, string id, string artifactURL)
        {
            Task task = new Task();
            task.Checkpoint = baseBuild;
            task.AppName = appName;
            task.Device = deviceModel;
            task.FeatureGateOverrides = new Dictionary<string, string>();
            task.Id = id;
            task.LogsDir = artifactURL;
            return task;
        }
    }

    /// <summary>
    /// A class that encapsulates the parameters needed to run the pipeline
    /// </summary>
    class ReportParams
    {

        public string InputBaseFileLocation { get; set; }
        public string InputTargetFileLocation { get; set; }
        public string BaseJobId { get; set; }
        public string CurrentJobId { get; set; }
        public string DeviceModel { get; set; }
        public string DeviceOs { get; set; }
        public string AppName { get; set; }
        public string FromAddress { get; set; }
        public string FromPassword { get; set; }
        public string EmailToList { get; set; }
        public string BaseJobArtifactURL { get; set; }
        public string TargetJobArtifactURL { get; private set; }
        public string RunName { get; set; }

        public ReportParams(string inputBaseFileLocation, string inputTargetFileLocation,
            string baseJobId, string currentJobId, string deviceModel, string deviceOs,
            string appName, string fromAddress, string fromPassword, string emailToList, string runName)
        {
            InputBaseFileLocation = inputBaseFileLocation;
            InputTargetFileLocation = inputTargetFileLocation;
            BaseJobId = baseJobId;
            CurrentJobId = currentJobId;
            DeviceModel = deviceModel;
            DeviceOs = deviceOs;
            AppName = appName;
            FromAddress = fromAddress;
            FromPassword = fromPassword;
            EmailToList = emailToList;
            BaseJobArtifactURL = GetJobArtifactURL(baseJobId);
            TargetJobArtifactURL = GetJobArtifactURL(currentJobId);
            RunName = runName;
        }

        public static string GetJobArtifactURL(string jobId) => "https://dev.azure.com/IdentityDivision/IDDP/_build/results?buildId="
               + jobId + "&view=artifacts&pathAsName=false&type=publishedArtifacts";
    }
}
