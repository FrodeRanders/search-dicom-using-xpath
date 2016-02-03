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

This is a finding tool. Encapsulated within each DicomElement object 
you'll find the dcm4che3 org.dcm4che3.data.Attributes object that has
all relevant information. The value representation in the wrapper
objects are relatively rudimentary and only exists to be able to form
XPath expressions. 

The idea is to use this tool to find the relevant pieces you are interested
in and then dive into the org.dcm4che3.data.Attributes object to pull
the bits an pieces you need.

Searching for a DICOM tag is currently done via it's name and not
it's id number. Using the composite (group.element) clashes with 
the XPath parser, but using the plain number could be an alternative

Using the DICOM Visualizr (a sibling project), this is the tree that
we are searching in and the lone DicomElement we are searching for:
![Image](doc/screencapture.png?raw=true)

## Tests
The test program runs against test-data (found among the resources) and tests these things:

* Using XPath expression: `/`
```
Found DicomElement {DICOMDIR}
```

* Using XPath expression: `//ConceptNameCodeSequence`
```
Found DicomElement {(0040,A043) ConceptNameCodeSequence}
```

* Using XPath expression: `//ConceptNameCodeSequence/@CodeValue`
```
Found DicomAttribute {(0008,0100) CodeValue vr=SH value="45_01004001"}
```

* Using XPath expression:
`//ConceptNameCodeSequence[@CodeValue='45_01004001']`
```
Found DicomElement {(0040,A043) ConceptNameCodeSequence}
```

* Using XPath expression:
`//ConceptNameCodeSequence[@CodeValue='45_01004001']/@CodeValue`
```
Found DicomAttribute {(0008,0100) CodeValue vr=SH value="45_01004001"}
```

* Using XPath expression:
`//ConceptNameCodeSequence[@CodeValue='45_01004001' and @CodingSchemeDesignator='99_PHILIPS']/@CodeValue`
```
Found DicomAttribute {(0008,0100) CodeValue vr=SH value="45_01004001"}
```

## Dependencies
There are some simple dependencies on the ensure project (https://github.com/FrodeRanders/ensure.git),
mainly for setting up logging and such.




