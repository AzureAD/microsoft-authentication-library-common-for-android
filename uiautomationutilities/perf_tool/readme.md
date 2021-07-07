# Perf tool walk through

**Design document:** https://identitydivision.visualstudio.com/DevEx/_git/AuthLibrariesApiReview?path=%2FPerfMeasurement%2FPerfMeasurementForClientLibs.md

**Recording:** https://msit.microsoftstream.com/video/8930a1ff-0400-9887-b080-f1eb55ce8803

**Source code of C# tool:** 
- Repo: https://github.com/AzureAD/microsoft-authentication-library-common-for-android
- Location: https://github.com/AzureAD/microsoft-authentication-library-common-for-android/tree/paul/update-perf-testing-documentation/uiautomationutilities/perf_tool

**Pipeline information:** https://dev.azure.com/IdentityDivision/IDDP/_build?definitionId=1254

### Definition of terms:
[**PerfMarker / CodeMarker:**](https://github.com/AzureAD/microsoft-authentication-library-common-for-android/blob/dev/common/src/main/java/com/microsoft/identity/common/PerfConstants.java#L36) A marker in code that results in an event to capture timestamp and other details as needed (Memory, Thread ID, CorrelationID etc. and write to file). The data generated from a CodeMarker can be used for Perf, logging, Telemetry etc.

[**Measurement:**](https://github.com/AzureAD/microsoft-authentication-library-common-for-android/blob/dev/uiautomationutilities/perf_tool/PerfDataConfiguration.xml#L4) A pair of markers identified for a transaction. E.g. If can have marker M1 at the beginning of a silent request and another when we successfully received a token say M2. Now M1 and M2 can be paired together if we were interested in measuring time between these. Pairing is an outside concept and has no impact from a code perspective.

[**Scenario:**](https://github.com/AzureAD/microsoft-authentication-library-common-for-android/blob/dev/uiautomationutilities/perf_tool/PerfDataConfiguration.xml#L18-L25) An E2E scenario that we would be interested in measuring. E.g. Silent Token refresh on a 4G network when requesting a token for graph. A scenario can be associated with one or more measurements.

**Base build:** This is a build against which we compare performance of future builds. It would typically be the last release. It is configured on the Perf pipeline using the `baseBuildID` variable

**Basefiles:** These are the code marker files during the run of the base build and help us compare the current run against the base run

**Targetfiles:** These are the code marker files generated during the current run


### Steps to add a new marker and see it in the report:
- If a new scenario has to be created, come up with a scenario code comprised of 3 digits and add it to the codemarker definitions in file [PerfConstants.java](https://github.com/AzureAD/microsoft-authentication-library-common-for-android/blob/dev/common/src/main/java/com/microsoft/identity/common/PerfConstants.java), for example some of the existing scenario codes are 100 and 200.
- In the common codebase, and add codemarkers (e.g. start of the activity to be measured and the end of the activity to be measured) at desired places. See example in [CommandDispatcher.java](https://github.com/AzureAD/microsoft-authentication-library-common-for-android/blob/dev/common/src/main/java/com/microsoft/identity/common/internal/controllers/CommandDispatcher.java#L197). Example events lines are given as follows:
	- `CodeMarkerManager.getInstance().markCode(CodeMarkersConstants.ACQUIRE_TOKEN_SILENT_START);`
	- `CodeMarkerManager.getInstance().markCode(CodeMarkersConstants.ACQUIRE_TOKEN_SILENT_FUTURE_OBJECT_CREATION_END);`
- Write a new test case that targets the flow where code markers have been added, see existing test case [TestCasePerf.java](https://github.com/AzureAD/microsoft-authentication-library-for-android/blob/dev/msalautomationapp/src/androidTest/java/com/microsoft/identity/client/msal/automationapp/testpass/perf/TestCasePerf.java) for non-brokered auth and [TestCasePerfBrokered.java](https://github.com/AzureAD/microsoft-authentication-library-for-android/blob/dev/msalautomationapp/src/androidTest/java/com/microsoft/identity/client/msal/automationapp/testpass/perf/TestCasePerfBrokered.java) for brokered auth.
- In the new test case, before performing the particular scenario, add following code snippet
	- Enable the Code marker: `CodeMarkerManager.getInstance().setEnableCodeMarker(true);`
	- Setup the scenario code defined in step #1: `CodeMarkerManager.getInstance().setPrefixScenarioCode("100");`
	- If we are having more than one iterations of same scenario, i.e. running same code multiple times, 
		- Make sure to clear the previous run's markers in the start of the iteration as follows: `CodeMarkerManager.getInstance().clearMarkers();`
		- Also write the content to the file using [FileAppender](https://github.com/AzureAD/microsoft-authentication-library-common-for-android/blob/dev/uiautomationutilities/src/main/java/com/microsoft/identity/client/ui/automation/logging/appender/FileAppender.java) such as [here](https://github.com/AzureAD/microsoft-authentication-library-for-android/blob/dev/msalautomationapp/src/androidTest/java/com/microsoft/identity/client/msal/automationapp/testpass/perf/TestCasePerf.java#L119-L124)
	- Also add followings after performing the particular scenario:
		- Clear the codemarkers: `CodeMarkerManager.getInstance().clearMarkers();`
		- Disable the codemarkers: `CodeMarkerManager.getInstance().setEnableCodeMarker(false);`
- In the C# tool code, open the file [PerfDataConfiguration.xml](https://github.com/AzureAD/microsoft-authentication-library-common-for-android/blob/dev/uiautomationutilities/perf_tool/PerfDataConfiguration.xml) and perform following actions:
	- In tag `MeasurementsConfigurations`, add a new `MeasurementsConfiguration` with giving the startMarker and endMarker as given in the step 2. 
	- In tag, `Scenarios`, add a new scenario with the Measurement ID of the MesurementConfiguration just added in previous sub-step.
- Push the common code and msal native code to dev branch by raising PRs.
- Push the C# changes of its repo (i.e. common) into dev branch by raising a PR.
- Deploy C# tool into the pipeline. (Pipeline information given at the start of this doc). We can deploy C# tool even before merging of PR if need be. As this deployment is done after building at the local machine.


### Steps to deploy latest version of C# tool to pipeline:
- Install a recent version of [Visual Studio](https://visualstudio.microsoft.com/)
- Ensure that you have the following tools selected while installing Visual Studio:
	- .NET desktop environment
	- Desktop development using C++
	- .NET Core cross-platform development
- Open the [location of the perf tool](https://github.com/AzureAD/microsoft-authentication-library-common-for-android/tree/paul/update-perf-testing-documentation/uiautomationutilities/perf_tool) in Visual studio. You can this by opening [PerfIdentity.sln](https://github.com/AzureAD/microsoft-authentication-library-common-for-android/blob/paul/update-perf-testing-documentation/uiautomationutilities/perf_tool/PerfIdentity.sln) from Visual studio
- Build Solution by clicking on the Build -> Build Solution. Make sure Release build is chosen for deployment.
- Bundle following files into one zip file named `PerfRunnables.zip`:
	- a. Files `PerfIdentity.exe`, `PerfIdentity.runtimeconfig.json`, `PerfIdentity.dll` and `PerfIdentity.deps.json` from location  `bin\Release\netcoreapp3.1` and file `PerfDataConfiguration.xml` from root location of the project
- Go to the pipeline and then from the left side options, Open Library page and upload the file `PerfRunnables.zip` after deleting previous one (if exists)
- Go to the pipeline definition and make change in the task `Download Perf runnable package` by selecting the file name which was uploaded in the `secure file` drop down.
- Save the build definition. 


### Steps to baseline a build number:
- Go to the pipeline page where we can see the previous runs.
- From the list of the runs, Click the particular run which we want to choose as the baseline.
- From the URL, find the numberic value of the buildId and note it. For example buildId will be 708340 for following URL: https://dev.azure.com/IdentityDivision/IDDP/_build/results?buildId=708340&view=results
- Go to the run page of the pipeline again.
- Go to the definition of the pipeline by clicking on the `Edit` button on the top right.
- Click on variables.
- Change the value of the variable `baseBuildID` with the value we have noted in the steps above.
- Save the pipeline definition.
	
### Steps to run on the pipeline:
- Go to the pipeline
- Click on `Run pipeline`
- Choose the right branch (default dev) and click on `Run`
	
### Steps to run on the pipeline when Basefiles are not available or are corrupted.
- First try to change the baseline build to such a build which has target files available in the artifacts (as given steps in another section) and it should work. If this does not work then follow next steps.
- If you have handful perfData txt files which you want to make basefiles, zip these files. The file names should start with PerfData and extension should be txt.
- Upload this zip file in the library by following these steps:
	- Go to the pipeline
	- Click on Library on the left menu.
	- Click on Secure files
	- Upload the zip file here by clicking `+ Secure file` and following steps.
- Go to the pipeline and click on Edit
- Temporarily disable `Download Base file(s)` task
- Enable `Download Perf Base files` task
- Enable `Unzip Perfbase files` task
- In task Download PerfBase files choose a secure File (the zip file which was uploaded in previous steps) which will be considered as a bundle of the basefiles.
- Save and run the pipeline.
- After the successful run, make sure to disable `Download Perf Base files` and `Unzip Perfbase files` and also make sure to enable `Download Base file(s)`


### Steps to run on local machine:
- Checkout msal native code recursively. 
- Open the msal native code in Android studio and point the dependency builds to local variants.
- Run one of the Test case having the codemarkers enabled. e.g. `TestCasePerf.java`, `TestCasePerfBrokerHost.java` on an Android device.
- On local windows machine create directories to keep your base and target codemarker files. e.g. `C:\testdata\basefiles` and `C:\testdata\targetfiles`
- Download base files from any of the build run by going to the artifacts, clicking on Perf and then clicking on basefiles. For example go to "https://dev.azure.com/IdentityDivision/IDDP/_build/results?buildId=705958&view=results", click on artifact button "2 published; 1 consumed", click on Perf and then download basefiles. 
- Extract and put the raw basefiles in your local base file folder of your machine.
- On local windows machine go to your target codemarker files location and download the codemarker files named as "PerfData*.txt" from the android device from location "/sdcard" : (Note: make sure that the test case has completed before running the download command)
- Open the Perf C# tool in visual studio by opening the solution file `PerfIdentity.sln`
- Build the project by clicking on Build -> Build Solution
- Copy `PerfDataConfiguration.xml` into the build folder e.g. `bin\Release\netcoreapp3.1`
- Run the application. Arguments definition can be seen in `command.cs` file.


#### Where is the raw Perfdata.txt stored in local run as well as in pipeline run
- In local run, the Perfdata txt files can be stored on any directory which should be given to the C# tool as an argument. e.g. `C:\testdata\basefiles` and `C:\testdata\targetfiles`
- On pipeline, we can find the txt files from artifact "Perf -> basefiles" or "Perf -> targetfiles"


#### Where to find the intermediate files or output files
- If output directory is default i.e. "." then the output files will be created in the directory where the applicaiton is running.
- Default directory of output files: `bin\Release\netcoreapp3.1`


### Arguments to the tool:
- Directory where PerfData base files are present. Example value: `C:\testdata\basefiles`
- Directory where PerfData target files are present. Example value: `C:\testdata\targetfiles`
- Build id of the base task.
- Build id which should be written in the final Email report and used for going to artifact url. Example value: `1234`
- Device model to be written in the final Email report. Example value: `Pixel2`
- Device OS to be written in the final Email report. Example value: `API28`
- App name to be written in the Email report. Example value: `MSALTestApp`
- Email ID of the sender's account. Example value: `idlab1@msidlab4.onmicrosoft.com`
- Password of the sender's account.
- Email To list separated by comma

### Email report:
- At the moment the email report is quite simple, it's constructed within the [View.cs](https://github.com/AzureAD/microsoft-authentication-library-common-for-android/blob/paul/update-perf-testing-documentation/uiautomationutilities/perf_tool/View.cs) and then the [ReportHelper sends the email](https://github.com/AzureAD/microsoft-authentication-library-common-for-android/blob/paul/update-perf-testing-documentation/uiautomationutilities/perf_tool/ReportHelper.cs#L138-L158) using the credentials passed as arguments to the tool
