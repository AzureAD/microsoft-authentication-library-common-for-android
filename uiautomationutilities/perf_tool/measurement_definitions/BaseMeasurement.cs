using PerfClTool.Measurement;
using System;
using System.Collections.Generic;
using System.Text;

namespace ConsoleApp1.measurement_definitions
{
    class BaseMeasurement
    {
        public String StartMarker;
        public String Startskip;
        public String Name;
        public String Id;
        public String EndSkip;
        public String EndMarker;

        public MeasurementConfiguration GetMeasurementConfiguration(List<string> measurements)
        {
            measurements.Add(Name);
            MeasurementConfiguration measurementConfiguration = new MeasurementConfiguration();
            measurementConfiguration.StartMarker = StartMarker;
            measurementConfiguration.Startskip = Startskip;
            measurementConfiguration.Name = Name;
            measurementConfiguration.Id = Id;
            measurementConfiguration.EndSkip = EndSkip;
            measurementConfiguration.EndMarker = EndMarker;
            return measurementConfiguration;
        }
    }
}
