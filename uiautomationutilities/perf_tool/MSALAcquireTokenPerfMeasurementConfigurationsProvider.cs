using ConsoleApp1.measurement_definitions;
using PerfClTool.Measurement;
using System;
using System.Collections.Generic;
using System.Dynamic;
using System.Text;

namespace ConsoleApp1
{
    class MSALAcquireTokenPerfMeasurementConfigurationsProvider
    {
        List<string> measurements = new List<string>();

        public string getScenarioName()
        {
            return "MSALAcquireToken";
        }

        public List<PerfMeasurementConfiguration> getActiveMeasurements()
        {
            List<PerfMeasurementConfiguration> measurementConfigurations = new List<PerfMeasurementConfiguration>();
            measurementConfigurations.Add(new PerfMeasurementConfiguration(new AcquireTokenSilentlyMeasurement().GetMeasurementConfiguration(measurements), false));
            measurementConfigurations.Add(new PerfMeasurementConfiguration(new SilentExecutorMeasurement().GetMeasurementConfiguration(measurements), false));
            return measurementConfigurations;
        }

        public List<string> getActiveMeasurementNames()
        {
            return measurements;
        }
    }
}
