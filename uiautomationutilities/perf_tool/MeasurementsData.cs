using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace PerfDiffResultMailer
{
    public class MeasurementsData : IEquatable<MeasurementsData>
    {
        public string ScenarioName { get; set; }
        public string ActivityName { get; set; }
        public string MeasurementName { get; set; }

        public override bool Equals(object obj)
        {
            return this.Equals(obj as MeasurementsData);
        }
        public override int GetHashCode()
        {
            string x = this.MeasurementName + this.ScenarioName + this.ActivityName;
            return x.GetHashCode();
        }
        public bool Equals(MeasurementsData other)
        {
            if (other == null)
                return false;

            return (this.ScenarioName.Equals(other.ScenarioName)) &&
                (this.ActivityName.Equals(other.ActivityName)) && (this.MeasurementName.Equals(other.MeasurementName));                
        }
    }
}
