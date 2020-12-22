//---------------------------------------------------------------------------
// <summary>
// This File contains different Classes for XML Serialization and Deserialization
// </summary>
// <copyright file="XMLUtility.cs" company="Microsoft">
// Copyright (c) Microsoft Corporation.  All rights reserved.
// </copyright>
// <owner>
// omkrishhn
// </owner>
//-------------------------------------------------------------------------------

using System.IO;
using System.Xml;
using System.Xml.Serialization;

namespace PerfClTool
{
    /// <summary>
    ///     Class for XML Utility methods
    /// </summary>
    internal class XmlUtility
    {
        /// <summary>
        ///     Method to deserialize an XML file to type
        /// </summary>
        /// <typeparam name="T"></typeparam>
        /// <param name="filePath"></param>
        /// <returns></returns>
        public static T DeSerialize<T>(string filePath)
        {
            T pmConfig;

            Stream stream = null;
            try
            {
                stream = new FileStream(filePath, FileMode.Open, FileAccess.Read);
                using (var xmlReader = new XmlTextReader(stream))
                {
                    stream = null;
                    xmlReader.DtdProcessing = DtdProcessing.Prohibit;
                    var xmlSerializer = new XmlSerializer(typeof(T));
                    pmConfig = (T) xmlSerializer.Deserialize(xmlReader);
                }
            }
            finally
            {
                if (stream != null)
                    stream.Dispose();
            }
            return pmConfig;
        }

        /// <summary>
        ///     Serializes an object of Type T to the specified file-path
        /// </summary>
        /// <typeparam name="T"></typeparam>
        /// <param name="obj"></param>
        /// <param name="filePath"></param>
        public static void Serialize<T>(T obj, string filePath)
        {
            var xmlSerializer = new XmlSerializer(obj.GetType());
            using (var writer = new StreamWriter(filePath))
            {
                xmlSerializer.Serialize(writer, obj);
            }
        }
    }
}
