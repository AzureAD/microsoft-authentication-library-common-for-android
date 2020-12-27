using PerfClTool.Measurement;
using System;
using System.Collections.Generic;
using System.Dynamic;
using System.Text;

namespace ConsoleApp1
{
    class PerfMeasurementConfigurationsProvider
    {
        List<PerfMeasurementConfiguration> measurementConfigurations;
        List<string> measurementNames = new List<string>();
        private string scenarioName;

        public PerfMeasurementConfigurationsProvider(string appName, string scenario)
        {
            this.scenarioName = scenario;
            measurementConfigurations = MeasurementsConfiguration.GetMeasurementConfigurations(appName, scenarioName);
            foreach (PerfMeasurementConfiguration config in measurementConfigurations)
            {
                measurementNames.Add(config.Name);
            }
        }

        public string getScenarioName()
        {
            return scenarioName;
        }

        public List<PerfMeasurementConfiguration> getActiveMeasurements()
        {
            return measurementConfigurations;
        }

        public List<string> getActiveMeasurementNames()
        {
            return measurementNames;
        }
    }
}
