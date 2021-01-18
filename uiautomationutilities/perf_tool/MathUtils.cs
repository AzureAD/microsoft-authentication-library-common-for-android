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
using System.Collections.Generic;
using System.Linq;

namespace PerfClTool.Measurement
{
    internal class MathUtils
    {
        public static double GetStdDeviation(List<double> values)
        {
            var average = values.Average();
            if (values.Count == 1)
            {
                return 0.0;
            }

            var stdev = 0.0;

            for (var n = 0; n < values.Count; n++)
            {
                stdev += Math.Pow(values[n] - average, 2);
            }

            stdev = Math.Sqrt(stdev / (values.Count - 1));

            return stdev;
        }
        public static double GetPercentile(List<double> values, int percentile)
        {
            if (percentile < 0 || percentile > 100)
            {
                throw new ArgumentException("Percentile can't be calculated for nonsensical index " + percentile);
            }

            if (values.Count == 1)
            {
                return values[0];
            }

            values.Sort();

            var n = percentile / 100.0 * (values.Count - 1) + 1.0;
            double left = 0.0, right = 0.0;

            if (n >= 1)
            {
                left = values[(int)Math.Floor(n) - 1];
                right = values[(int)Math.Floor(n)];
            }
            else
            {
                left = values[0];
                right = values[1];
            }

            if (left == right)
            {
                return left;
            }

            var part = n - Math.Floor(n);
            return left + part * (right - left);
        }

        public static List<double> GetMinSdSubset(List<double> values, int factor)
        {
            values.Sort();
            double average;
            if (values.Count == 1)
            {
                average = values[0];
            }

            if (factor <= 0 || factor > 100)
            {
                throw new ArgumentException("Closest can't be calculated for nonsensical index " + factor);
            }

            var setSize = (int)(0.5 + (float)values.Count * factor / 100);

            if (setSize <= 0)
            {
                throw new ArgumentException("Factor - {0} too small for {1} iterations " + factor,
                    values.Count.ToString());
            }

            var currLow = 0;
            var currHigh = setSize - 1;
            var globalLow = 0;
            var globalHigh = setSize - 1;
            double globalSd = int.MaxValue;

            while (currHigh < values.Count)
            {
                var currSd = GetStdDeviation(values.GetRange(currLow, setSize));
                if (currSd <= globalSd)
                {
                    globalSd = currSd;
                    globalLow = currLow;
                    globalHigh = currHigh;
                }
                currHigh++;
                currLow++;
            }
            return values.GetRange(globalLow, setSize);
        }
    }
}
