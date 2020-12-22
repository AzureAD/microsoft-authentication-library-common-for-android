using System.Collections.Generic;
using System.Xml.Serialization;

namespace PerfClTool.Measurement
{
    [XmlRoot(ElementName = "MeasurementConfiguration")]
    public class MeasurementConfiguration
    {
        [XmlAttribute(AttributeName = "Id")]
        public string Id { get; set; }
        [XmlAttribute(AttributeName = "StartMarker")]
        public string StartMarker { get; set; }
        [XmlAttribute(AttributeName = "EndMarker")]
        public string EndMarker { get; set; }
        [XmlAttribute(AttributeName = "startskip")]
        public string Startskip { get; set; }
        [XmlAttribute(AttributeName = "EndSkip")]
        public string EndSkip { get; set; }
        [XmlAttribute(AttributeName = "Name")]
        public string Name { get; set; }
    }

    [XmlRoot(ElementName = "MeasurementsConfigurations")]
    public class MeasurementsConfigurations
    {
        [XmlElement(ElementName = "MeasurementConfiguration")]
        public List<MeasurementConfiguration> MeasurementConfiguration { get; set; }
    }

    [XmlRoot(ElementName = "Measurement")]
    public class Measurement
    {
        [XmlAttribute(AttributeName = "Id")]
        public string Id { get; set; }
        [XmlAttribute(AttributeName = "IsPrimary")]
        public string IsPrimary { get; set; }
    }

    [XmlRoot(ElementName = "Measurements")]
    public class Measurements
    {
        [XmlElement(ElementName = "Measurement")]
        public List<Measurement> Measurement { get; set; }
    }

    [XmlRoot(ElementName = "Scenario")]
    public class Scenario
    {
        [XmlElement(ElementName = "Name")]
        public string Name { get; set; }
        [XmlElement(ElementName = "Measurements")]
        public Measurements Measurements { get; set; }
    }

    [XmlRoot(ElementName = "Scenarios")]
    public class Scenarios
    {
        [XmlElement(ElementName = "Scenario")]
        public List<Scenario> Scenario { get; set; }
    }

    [XmlRoot(ElementName = "App")]
    public class App
    {
        [XmlElement(ElementName = "Name")]
        public string Name { get; set; }
        [XmlElement(ElementName = "Scenarios")]
        public Scenarios Scenarios { get; set; }
    }

    [XmlRoot(ElementName = "Apps")]
    public class Apps
    {
        [XmlElement(ElementName = "App")]
        public List<App> App { get; set; }
    }

    [XmlRoot(ElementName = "ScenarioMeasurementsMapping")]
    public class ScenarioMeasurementsMapping
    {
        [XmlElement(ElementName = "Apps")]
        public Apps Apps { get; set; }
    }

    [XmlRoot(ElementName = "PerfDataConfiguration")]
    public class PerfDataConfiguration
    {
        [XmlElement(ElementName = "MeasurementsConfigurations")]
        public MeasurementsConfigurations MeasurementsConfigurations { get; set; }
        [XmlElement(ElementName = "ScenarioMeasurementsMapping")]
        public ScenarioMeasurementsMapping ScenarioMeasurementsMapping { get; set; }
    }


}
