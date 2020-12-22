using System;
using System.Collections.Generic;
using System.Text;
using System.Transactions;

namespace ConsoleApp1.measurement_definitions
{
    class AcquireTokenSilentlyMeasurement : BaseMeasurement
    {
        public AcquireTokenSilentlyMeasurement() { 
        StartMarker = "10011";
        Startskip = "0";
        Name = "silent flow time";
        Id = "400649";
        EndSkip = "0";
        EndMarker = "10020";
        }
    }
}
