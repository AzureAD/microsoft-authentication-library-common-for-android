Perf tool walk through recording: https://msit.microsoftstream.com/video/8930a1ff-0400-9887-b080-f1eb55ce8803


Source code of C# tool: 
- Repo: https://github.com/AzureAD/microsoft-authentication-library-common-for-android
- Location: /uiautomationutilities/perf_tool


Pipeline information: https://dev.azure.com/IdentityDivision/IDDP/_build?definitionId=1254


The source / path of the files:
- New files:
	- command.cs
	- PerfDataConfiguration.xml
	- PerfMeasurementConfigurationsProvider.cs
	- Program.cs
- PerfCL files:
	- MathUtils.cs: 
	- MeasurementsConfiguration.cs
	- MeasurementsStore.cs
	- PerfData.cs
	- PerfDataConfiguration.cs
	- PerfDataRecord.cs
	- PerfMeasurement.cs
	- PerfMeasurementsSet.cs
	- PerformanceMetric.cs
	- ReportHelper.cs
	- XMLUtility.cs
- PerfDiffResultMailer files:
	- Gen3MeasurementsData.cs -> Renamed to -> MeasurementsData.cs
	- Parameter.cs
	- Task.cs
	- View.cs


Source details of the files: 
- New files: 
	- Source: None
	- Contact: Neerav Agarwal (Neerav.Agarwal@microsoft.com)
- PerfCL files: 
	- Source: \\daddev\office\16.0\13328.10000\src\toolsrc\PerfCLTool
	- Contact: Arpit Aggarwal (Aggarwal.Arpit@microsoft.com)
- PerfDiffResultMailer files: 
	- Source: \\officestore\scratch\gargupta\PerfDiffResultMailer\
	- Contact: Garima Gupta (Garima.Gupta@microsoft.com)


Steps to modify the code to add a new marker and see it in the report:
- 1. If a new scenario has to be created, write a new test case in MSAL native codebase, see existing test case TestCasePerf.java. In the new test case, before performing the particular scenario, add following code snippet
	// Enable the Code marker
	CodeMarkerManager.getInstance().setEnableCodeMarker(true);
	//Setting up scenario code with a 3 digit code. For example existing codes are 100 and 200, choose a new one.
	CodeMarkerManager.getInstance().setPrefixScenarioCode("100");
	- If we are having more than one iterations of same scenario, i.e. running same code multiple times, 
		- make sure to clear the previous run's markers in the start of the iteration as follows:
		  "CodeMarkerManager.getInstance().clearMarkers();"
		- Also write the content to the file using fileAppender.
	- Also add followings after performing the particular scenario:
	  // Clear the codemarkers.
	  CodeMarkerManager.getInstance().clearMarkers();
	  // Disable the codemarkers.
	  CodeMarkerManager.getInstance().setEnableCodeMarker(false);	
- 2. In the common codebase, Add the codemarker definitions in file "CodeMarkersConstants.java" and add codemarker events (e.g. start of the activity to be measured and the end of the activity to be measured) at desired places. See example in commandDispatcher.java. Example events lines are given as follows:
	CodeMarkerManager.getInstance().markCode(CodeMarkersConstants.ACQUIRE_TOKEN_SILENT_START);
	CodeMarkerManager.getInstance().markCode(CodeMarkersConstants.ACQUIRE_TOKEN_SILENT_FUTURE_OBJECT_CREATION_END);
- 3. In the C# tool code, open the file "PerfDataConfiguration.xml" and perform following actions:
	- 1. In tag "MeasurementsConfigurations", add a new "MeasurementsConfiguration" with giving the startMarker and endMarker as given in the step 2. 

	IMPORTANT: A startmarker or endmarker should be combination of the scenario code and the codemarker provided in the common code. For example if the scenario code is 300 and a couple of code markers are 10088 and 10089 then the startMarker and endMarker can be 30010088 and 30010089.

	- 2. In tag, "Scenarios", add a new scenario with the Measurement ID of the MesurementConfiguration just added in previous sub-step.
- 4. Push the common code and msal native code to dev branch by raising PRs.
- 5. Push the C# changes of its repo (i.e. common) into dev branch by raising a PR.
- 6. Deploy C# tool into the pipeline. (Pipeline information given at the start of this doc). We can deploy C# tool even before merging of PR if need be. As this deployment is done after building at the local machine.


Steps to deploy latest version of C# tool to pipeline:
- 1. Take a checkout of C# tool repo (Details above)
- 2. Open the Location of the perf tool (Location details given above) in Visual studio.
- 3. Build Solution by clicking on the Build -> Build Solution. Make sure Release build is chosen for deployment.
- 4. Bundle following files into one zip file named "PerfRunnables.zip":
	- a. Files "PerfIdentity.exe", "PerfIdentity.runtimeconfig.json", "PerfIdentity.dll" and "PerfIdentity.deps.json" from location  "bin\Release\netcoreapp3.1" and file "PerfDataConfiguration.xml" from root location of the project
- 5. Go to the pipeline and then from the left side options, Open Library page and upload the file "PerfRunnables.zip" after deleting previous one (if exists)
- 6. Go to the pipeline definition and make change in the task "Download Perf runnable package" by selecting the file name which was uploaded in the "secure file" drop down.
- 7. Save the build definition. 


Steps to baseline a build number:
- 1. Go to the pipeline page where we can see the previous runs.
- 2. From the list of the runs, Click the particular run which we want to choose as the baseline.
- 3. From the URL, find the numberic value of the buildId and note it. For example buildId will be 708340 for following URL:
	https://dev.azure.com/IdentityDivision/IDDP/_build/results?buildId=708340&view=results
- 4. Go to the run page of the pipeline again.
- 5. Go to the definition of the pipeline by clicking on the "Edit" button on the top right.
- 6. Click on variables.
- 7. Change the value of the variable baseBuildID with the value we have noted in the steps above.
- 8. Save the pipeline definition.


Steps to run on the pipeline:
- 1. Go to the pipeline
- 2. Click on "Run pipeline"
- 3. Click on "Run"


Steps to run on the pipeline when Basefiles are not available or are corrupted.
- 1. Firt try to change the baseline build  to such a build which has target files available in the artifacts (as given steps in another section) and it should work. If this does not work then follow next steps.
- 2. If you have handful perfData txt files which you want to make basefiles, zip these files. The file names should start with PerfData and extension should be txt.
- 3. Upload this zip file in the library by following these steps:
	- a. Go to the pipeline
	- b. Click on Library on the left menu.
	- c. Click on Secure files
	- b. Upload the zip file here by clicking "+ Secure file" and following steps.
- 4. Go to the pipeline and click on Edit
- 5. Temporarily disable "Download Base file(s)" task
- 6. Enable "Download Perf Base files" task
- 7. Enable "Unzip Perfbase files" task
- 8. In task Download PerfBase files choose a secure File (the zip file which was uploaded in previous steps) which will be considered as a bundle of the basefiles.
- 9. Save and run the pipeline.
-10. After the successful run, make sure to disable "Download Perf Base files" and "Unzip Perfbase files" and also make sure to enable "Download Base file(s)"


Steps to run on local machine:
- 1. Checkout msal native code recursively. 
- 2. Open the msal native code in Android studio and point the dependency builds to local variants.
- 3. Run one of the Test case having the codemarkers enabled. e.g. "TestCasePerf.java", "TestCasePerfBrokerHost.java" on an Android device.
- 4. On local windows machine create directories to keep your base and target codemarker files. e.g. "C:\testdata\basefiles" and "C:\testdata\targetfiles"
- 5. Download base files from any of the build run by going to the artifacts, clicking on Perf and then clicking on basefiles. For example go to "https://dev.azure.com/IdentityDivision/IDDP/_build/results?buildId=705958&view=results", click on artifact button "2 published; 1 consumed", click on Perf and then download basefiles. 
- 6. Extract and put the raw basefiles in your local base file folder of your machine.
- 7. On local windows machine go to your target codemarker files location and download the codemarker files named as "PerfData*.txt" from the android device from location "/sdcard" : (Note: make sure that the test case has completed before running the download command)
- 8. Open the Perf C# tool in visual studio by opening the solution file "PerfIdentity.sln"
- 9. Build the project by clicking on Build -> Build Solution
-10. Copy PerfDataConfiguration.xml into the build folder e.g. "bin\Release\netcoreapp3.1"
-11. Run the application. Arguments definition can be seen in command.cs file.


Where is the raw Perfdata.txt stored in local run as well as in pipeline run
- In local run, the Perfdata txt files can be stored on any directory which should be given to the C# tool as an argument. e.g. "C:\testdata\basefiles" and "C:\testdata\targetfiles"
- On pipeline, we can find the txt files from artifact "Perf -> basefiles" or "Perf -> targetfiles"