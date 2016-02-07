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

## Really simple examples
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

## A somewhat more complex example
Consider this tree-structured snippet of a structured report:
```
        [(0040,A730) ContentSequence]
            (0040,A010) RelationshipType :: CONTAINS
            (0040,A040) ValueType :: CONTAINER
            (0040,A050) ContinuityOfContent :: SEPARATE

            [(0040,A043) ConceptNameCodeSequence ConceptNameCodeSequence]
                (0008,0100) CodeValue :: F-01710
                (0008,0102) CodingSchemeDesignator :: SRT
                (0008,0103) CodingSchemeVersion :: 1.0
                (0008,0104) CodeMeaning :: Breast Composition

            [(0040,A730) ContentSequence]
                (0040,A010) RelationshipType :: CONTAINS
                (0040,A040) ValueType :: CODE

                [(0040,A043) ConceptNameCodeSequence]
                    (0008,0100) CodeValue :: F-01710
                    (0008,0102) CodingSchemeDesignator :: SRT
                    (0008,0103) CodingSchemeVersion :: 1.0
                    (0008,0104) CodeMeaning :: Breast Composition

                [(0040,A168) ConceptCodeSequence]
                    (0008,0100) CodeValue :: F-01713
                    (0008,0102) CodingSchemeDesignator :: SRT
                    (0008,0103) CodingSchemeVersion :: 1.0
                    (0008,0104) CodeMeaning :: ACR3

                [(0040,A730) ContentSequence]
                    (0040,A010) RelationshipType :: HAS CONCEPT MOD
                    (0040,A040) ValueType :: CODE

                    [(0040,A043) ConceptNameCodeSequence]
                        (0008,0100) CodeValue :: G-C171
                        (0008,0102) CodingSchemeDesignator :: SRT
                        (0008,0103) CodingSchemeVersion :: 1.0
                        (0008,0104) CodeMeaning :: Laterality

                    [(0040,A168) ConceptCodeSequence]
                        (0008,0100) CodeValue :: T-04020
                        (0008,0102) CodingSchemeDesignator :: SNM3
                        (0008,0103) CodingSchemeVersion :: 1.0
                        (0008,0104) CodeMeaning :: Right breast
```

We want to extract the (estimated) overall breast composition using the SNOMED-RT vocabulary.
This XPath expression would be accurate, matching a great many details from the tree above
(and I am really sorry that it does not wrap :)
```
//ConceptCodeSequence[(../../ConceptNameCodeSequence[@CodingSchemeDesignator='SRT' and @CodeValue='F-01710']) and (../ConceptNameCodeSequence[@CodingSchemeDesignator='SRT' and @CodeValue='F-01710']) and (../ContentSequence/ConceptCodeSequence[@CodingSchemeDesignator='SNM3' and @CodeValue='T-04020'])]
```

This expression can be simplified, but the more you remove from the expression, the
higher the risk of getting matches in other parts of the tree. I think, the following
expression would suffice:
```
//ConceptCodeSequence[(../ConceptNameCodeSequence[@CodingSchemeDesignator='SRT' and @CodeValue='F-01710']) and (../ContentSequence/ConceptCodeSequence[@CodingSchemeDesignator='SNM3' and @CodeValue='T-04020'])]
```

Going into some detail:
1. The term ```//ConceptCodeSequence``` matches any DicomElement (in project lingo), 
2. so we add a predicate using the ```[ predicate ]```, which consists of two demands
3. ```../ConceptNameCodeSequence[@CodingSchemeDesignator='SRT' and @CodeValue='F-01710']``` must match sibling element
  1. having two matching attributes
    - ```@CodingSchemeDesignator='SRT'``` and 
    - ```@CodeValue='F-01710'```
4. ```../ContentSequence/ConceptCodeSequence[@CodingSchemeDesignator='SNM3' and @CodeValue='T-04020']`` must match sibling element
  1. having to matching attributes
    - ```@CodingSchemeDesignator='SNM3'``` and
    - ```@CodeValue='T-04020'```
    
For more details on how to form XPath expressions, I kindly refer you to [Google](http://lmgtfy.com/?q=XPath+expressions).

## Java code
Locate the DicomElement containing the breast composition (SRT:F-01713) attribute (and not the attribute itself).
```java
DicomLoader loader = new DicomLoader();

File sr = new File("/path/to/SR00003.DCM");
loader.load(sr);

DicomDocument doc = loader.getDicomDocument();
DicomElement rootElement = doc.getDicomObject();

String expr = "//ConceptCodeSequence[(../../ConceptNameCodeSequence[@CodingSchemeDesignator='SRT' and @CodeValue='F-01710']) and (../ConceptNameCodeSequence[@CodingSchemeDesignator='SRT' and @CodeValue='F-01710']) and (../ContentSequence/ConceptCodeSequence[@CodingSchemeDesignator='SNM3' and @CodeValue='T-04020'])]";
XPath xpath = new XPath(expr);
System.out.println("Seaching right breast density using: " + xpath.toString() + "\n -> " + xpath.debug());
List nodes = xpath.selectNodes(rootElement);
for (Object node : nodes) {
   // We are matching on a DicomElement and not an individual attribute, so
   System.out.println(((DicomElement)node).asText(/* recurse? */ false);
   
   // From here, we can switch to the dcm4che realm completely
   Attributes attributes = ((DicomElement)node).getAttributes();
   System.out.println("Corresponding raw dcm4che3 Attributes:");
   System.out.println(attributes);
}
```

Output:
```
Seaching right breast density using: //ConceptCodeSequence[(../../ConceptNameCodeSequence[@CodingSchemeDesignator='SRT' and @CodeValue='F-01710']) and (../ConceptNameCodeSequence[@CodingSchemeDesignator='SRT' and @CodeValue='F-01710']) and (../ContentSequence/ConceptCodeSequence[@CodingSchemeDesignator='SNM3' and @CodeValue='T-04020'])]
 -> [(DefaultXPath): [(DefaultAbsoluteLocationPath): [(DefaultAllNodeStep): descendant-or-self]/[(DefaultNameStep): ConceptCodeSequence]]]
[(0040,A168) ConceptCodeSequence]
    (0008,0100) CodeValue :: F-01713
    (0008,0102) CodingSchemeDesignator :: SRT
    (0008,0103) CodingSchemeVersion :: 1.0
    (0008,0104) CodeMeaning :: ACR3

Corresponding raw dcm4che3 Attributes:
(0008,0100) SH [F-01713] CodeValue
(0008,0102) SH [SRT] CodingSchemeDesignator
(0008,0103) SH [1.0] CodingSchemeVersion
(0008,0104) LO [ACR3] CodeMeaning

```

If we instead were to locate the attribute itself, we could do like this:
```java
// code continues from section above...
String expr2 = expr + "/@CodeValue";
xpath = new XPath(expr2);
nodes = xpath.selectNodes(rootElement);
for (Object node : nodes) {
  // We are matching on a DicomAttribute and not the encompassing DicomElement, so
  System.out.println(((DicomAttribute)node).asText();
}
```

Output:
```
(0008,0100) CodeValue :: F-01713
```

We have transformed the trouble of navigating the DICOM tree to composing XPath expressions!

## Dependencies
There are some simple dependencies on the ensure project (https://github.com/FrodeRanders/ensure.git),
mainly for setting up logging and such.




