## Search DICOM files using XPath expressions
This project wraps functionality for searching DICOM files using XPath expressions. 

The expressions are XPath alright (using the Jaxen parser and
machinery), but in order to adapt to the DICOM concepts, I had to
implement an XML-ish model onto DICOM. 

You will find a DicomDocument, which corresponds to a DICOM file such
as DICOMDIR. You will further find a DicomElement, which corresponds
to individual sequences in the DICOM file. I also had to map
individual DICOM tags onto the XML attribute concept, so if this had
been an XML file then all information would be kept in attributes.

## Tests
The test program runs against test-data (found among the resources) and tests these things:

* Using XPath expression: /
```
Found DicomElement {DICOMDIR}
```

* Using XPath expression: //ConceptNameCodeSequence
```
Found DicomElement {(0040,A043) ConceptNameCodeSequence}
```

* Using XPath expression: //ConceptNameCodeSequence/@CodeValue
```
Found DicomAttribute {(0008,0100) CodeValue vr=SH value="45_01004001"}
```

* Using XPath expression:
//ConceptNameCodeSequence[@CodeValue='45_01004001']
```
Found DicomElement {(0040,A043) ConceptNameCodeSequence}
```

* Using XPath expression:
//ConceptNameCodeSequence[@CodeValue='45_01004001']/@CodeValue
```
Found DicomAttribute {(0008,0100) CodeValue vr=SH value="45_01004001"}
```

* Using XPath expression:
//ConceptNameCodeSequence[@CodeValue='45_01004001' and @CodingSchemeDesignator='99_PHILIPS']/@CodeValue
```
Found DicomAttribute {(0008,0100) CodeValue vr=SH value="45_01004001"}
```

## Dependencies
There are some simple dependencies on the ensure project (https://github.com/FrodeRanders/ensure.git),
mainly for setting up logging and such.




