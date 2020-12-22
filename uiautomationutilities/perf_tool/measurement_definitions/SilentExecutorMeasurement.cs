using System;
using System.Collections.Generic;
using System.Text;
using System.Transactions;

namespace ConsoleApp1.measurement_definitions
{
    class SilentExecutorMeasurement : BaseMeasurement
    {
        public SilentExecutorMeasurement() { 
        StartMarker = "10012";
        Startskip = "0";
        Name = "execution time";
        Id = "400650";
        EndSkip = "0";
        EndMarker = "10014";
        }
    }
}
