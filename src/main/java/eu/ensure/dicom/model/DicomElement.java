/*
 * Copyright (C) 2016 Frode Randers
 * All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.ensure.dicom.model;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dcm4che3.data.*;
import org.dcm4che3.util.TagUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by froran on 2016-01-28.
 */
public class DicomElement {
    private static final Logger log = LogManager.getLogger(DicomElement.class);
    private static final int NO_TAG = -1;

    private final String id;  // Id if object is wrapped in a sequence, "" otherwise
    private final String name;  // Name of sequence if wrapped -- otherwise name of file
    private String description; // Heuristic description of sequence if wrapped
    private final Attributes attributes;
    private final DicomElement owner;

    private final List<DicomElement> children = new ArrayList<>();
    private final List<DicomAttribute> dicomAttributes = new ArrayList<>();

    public DicomElement(int tag, String name, Attributes attributes, DicomElement owner) {
        if (tag > 0) {
            id = TagUtils.toString(tag);
        } else {
            id = "";
        }
        this.name = name;
        this.attributes = attributes;
        this.owner = owner;

        String sopClassUID = sopClassUID(attributes);
        if (null == sopClassUID) {
            // We have no SOP class UID. If we are a DICOMDIR, we should have a
            // directory record type instead.
            String recordType = directoryRecordType(attributes);
            if (null != recordType && recordType.length() > 0) {
                description = recordType;
            } else {
                description = id;
                if (description.length() > 0) {
                    description += " ";
                }
                description += name;
            }
        } else {
            DicomDocument.Type type = DicomDocument.Type.find(sopClassUID);
            description = type.getDescription();
        }

        //
        populate(dicomAttributes);
    }

    public DicomElement(String name, Attributes attributes, DicomElement owner) {
        this(NO_TAG, name, attributes, owner);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<DicomElement> getChildren() {
        return children;
    }

    public Attributes getAttributes() {
        return attributes;
    }

    public Iterator<DicomElement> getChildIterator() {
        return children.iterator();
    }

    public DicomElement getOwner() {
        return owner;
    }

    private String valueToString(Object actualValue) {
        return actualValue.toString();
    }

    public String getSopClassUID() {
        return sopClassUID(attributes);
    }


    public List<DicomAttribute> getDicomElements() {
        return dicomAttributes;
    }

    public static String directoryRecordType(final Attributes dataset) {
        String directoryRecordType = dataset.getString(TagUtils.toTag(0x0004, 0x1430));
        if (log.isTraceEnabled() && null != directoryRecordType) {
            log.trace("directoryRecordType = " + directoryRecordType);
        }
        return directoryRecordType;
    }

    public static String patientID(final Attributes dataset) {
        String patientId = dataset.getString(TagUtils.toTag(0x0010, 0x0020));
        if (log.isTraceEnabled() && null != patientId) {
            log.trace("patientID = " + patientId);
        }
        return patientId;
    }

    public static String studyInstanceUID(final Attributes dataset) {
        String studyInstanceUid = dataset.getString(TagUtils.toTag(0x0020, 0x000D));
        if (log.isTraceEnabled() && null != studyInstanceUid) {
            log.trace("studyInstanceUID = " + studyInstanceUid);
        }
        return studyInstanceUid;
    }

    public static String seriesInstanceUID(final Attributes dataset) {
        String seriesInstanceUid = dataset.getString(TagUtils.toTag(0x0020, 0x000E));
        if (log.isTraceEnabled() && null != seriesInstanceUid) {
            log.trace("seriesInstanceUID = " + seriesInstanceUid);
        }
        return seriesInstanceUid;
    }

    public static String seriesDescription(final Attributes dataset) {
        String seriesDescription = dataset.getString(TagUtils.toTag(0x0008, 0x103E));
        if (log.isTraceEnabled() && null != seriesDescription) {
            log.trace("seriesDescription = " + seriesDescription);
        }
        return seriesDescription;
    }

    public static String sopInstanceUID(final Attributes dataset) {
        String sopInstanceUid = dataset.getString(TagUtils.toTag(0x0008, 0x0018));
        if (log.isTraceEnabled() && null != sopInstanceUid) {
            log.trace("SOPInstanceUID = " + sopInstanceUid);
        }
        return sopInstanceUid;
    }

    public static String sopClassUID(final Attributes dataset) {
        String sopClassUid = dataset.getString(TagUtils.toTag(0x0008, 0x0016));
        if (log.isTraceEnabled() && null != sopClassUid) {
            log.trace("SOPClassUID = " + sopClassUid);
        }
        return sopClassUid;
    }

    public static String modality(final Attributes dataset) {
        String modality = dataset.getString(TagUtils.toTag(0x0008, 0x0060));
        if (log.isTraceEnabled() && null != modality) {
            log.trace("modality = " + modality);
        }
        return modality;
    }

    public static String performingPhysicianName(final Attributes dataset) {
        String performingPhysicianName = dataset.getString(TagUtils.toTag(0x0008, 0x1050));
        if (log.isTraceEnabled() && null != performingPhysicianName) {
            log.trace("performingPhysicianName = " + performingPhysicianName);
        }
        return performingPhysicianName;
    }

    private void populate(List<DicomAttribute> elementList) {
        SpecificCharacterSet characterSet = attributes.getSpecificCharacterSet();
        ElementDictionary dict = ElementDictionary.getStandardElementDictionary();
        boolean isBE = attributes.bigEndian();

        final DicomElement currentObject = this;

        try {
            attributes.accept((attributes1, tag, vr, _value) -> {

                boolean isNull = (_value instanceof Value && _value == Value.NULL);

                if (isNull) {
                    elementList.add(new DicomAttribute(tag, vr, "<null>", /* owner */ currentObject));
                    return true;
                }

                String value = "";
                try {
                    switch (vr) {
                        case AE: // Application Entity
                            // Character data [Naming devices, people, and instances]
                            // StringValueType.ASCII
                            value = valueToString(vr.toStrings(_value, isBE, characterSet));
                            break;

                        case AS: // Age string
                            // Character data [Date and time]
                            // Format: nnnW or nnnM or nnnY
                            // StringValueType.ASCII
                            value = valueToString(vr.toStrings(_value, isBE, characterSet));
                            break;

                        case AT: // Attribute tag
                            // Two 2-byte integers [Numbers in binary format]
                            // Format: gggg,eeee
                            // BinaryValueType.TAG
                        {
                            Object o = vr.toStrings(_value, isBE, characterSet);
                            if (o instanceof String[]) {
                                for (String s : (String[]) o) {
                                    int _tag = Integer.parseInt(s, 16);
                                    value += TagUtils.toString(_tag);
                                    value += " " + dict.keywordOf(_tag) + ", ";
                                }
                            } else {
                                int _tag = Integer.parseInt((String) o, 16);
                                value += TagUtils.toString(_tag);
                                value += " (" + dict.keywordOf(_tag) + ")";
                            }
                        }
                        break;

                        case DT: // Date time
                            // Character data [Date and time]
                            // Format: YYYYMMDDHHMMSS.FFFFFF&ZZZZ (&ZZZ is optional & = + or -)
                            // StringValueType.DT
                            value = valueToString(vr.toStrings(_value, isBE, characterSet));
                            break;

                        case IS: // Integer string
                            // Character data [Numbers in text format]
                            // Integer encoded as string. May be padded
                            // StringValueType.IS
                        {
                            Object o = vr.toStrings(_value, isBE, characterSet);
                            if (o instanceof String[]) {
                                for (String s : (String[]) o) {
                                    value += s + ", ";
                                }
                            } else {
                                value += o;
                            }
                        }
                        break;

                        case LO: // Long string
                            // Character data, Max length: 64 [Text]
                            // Character string. Can be padded.
                            // NOTE: May not contain \ or any control chars except ESC
                            // StringValueType.STRING
                        {
                            Object o = vr.toStrings(_value, isBE, characterSet);
                            if (o instanceof String[]) {
                                for (String s : (String[]) o) {
                                    value += s + ", ";
                                }
                            } else {
                                value += o;
                            }
                        }
                        break;

                        case LT: // Long text
                            // Character data, Max length: 10,240 [Text]
                            // NOTE: Leading spaces are significant, trailing spaces are not
                            // StringValueType.TEXT
                            value = valueToString(vr.toStrings(_value, isBE, characterSet));
                            break;

                        case SH: // Short string
                            // Character data, Max length: 16 [Text]
                            // NOTE: may be padded
                            // StringValueType.STRING
                            value = valueToString(vr.toStrings(_value, isBE, characterSet));
                            break;

                        case TM: // Time
                            // Format: hhmmss.frac (or older format: hh:mm:ss.frac)
                            // StringValueType.TM
                            value = valueToString(vr.toStrings(_value, isBE, characterSet));
                            break;

                        case UI: // Unique identifier (UID)
                            // Character data [Naming devices, people, and instances]
                            // Format: delimiter = ., 0-9 characters only, trailing space to make even number
                            // StringValueType.ASCII
                            value = valueToString(vr.toStrings(_value, isBE, characterSet));
                            break;

                        case UT: // Unlimited text
                            // Character data, Max length: 4,294,967,294 [Text]
                            // NOTE: Trailing spaces ignored
                            // StringValueType.TEXT
                            value = valueToString(vr.toStrings(_value, isBE, characterSet));
                            break;

                        case UC: // Unlimited characters
                            // StringValueType.STRING
                            value = valueToString(vr.toStrings(_value, isBE, characterSet));
                            break;

                        case UR: // URI or URL
                            // StringValueType.UR
                            value = valueToString(vr.toStrings(_value, isBE, characterSet));
                            break;

                        case CS: // Code string
                            // Character data, Max length: 16
                            // Note: Only upper-case letters, 0-9, ' ' and '_' allowed
                            // StringValueType.ASCII
                        {
                                    /*
                                    int multiplicity = vr.vmOf(_value);
                                    for (int i = 0; i < multiplicity; i++) {
                                        vr.toString(_value, isBE, i, "");
                                    }
                                    */
                            Object o = vr.toStrings(_value, isBE, characterSet);
                            if (o instanceof String[]) {
                                for (String s : (String[]) o) {
                                    value += s + ", ";
                                }
                            } else {
                                value += o;
                            }
                        }
                        break;

                        case PN: // Person name
                            // Character data [Naming devices, people, and instances]
                            // NOTE: 64 byte max per component, 5 components with delimiter = ^
                            // StringValueType.PN
                            value = valueToString(vr.toStrings(_value, isBE, characterSet));
                            break;

                        case ST: // Short text
                            // Character data, Max length: 1024 [Text]
                            // StringValueType.TEXT
                            value = valueToString(vr.toStrings(_value, isBE, characterSet));
                            break;

                        /****************
                         * Date
                         ****************/
                        case DA: // Date
                            // Eight characters [Date and time]
                            // Format: yyyymmdd (check for yyyy.mm.dd also and convert)
                            // StringValueType.DA
                            value = valueToString(vr.toStrings(_value, isBE, characterSet));
                            break;

                        /****************
                         * double[]
                         ****************/
                        case DS: // Decimal string
                            // Character data [Numbers in text format]
                            // NOTE: may start with + or - and may be padded with l or t space
                            // StringValueType.DS
                        {
                            Object o = vr.toStrings(_value, isBE, characterSet);
                            if (o instanceof String[]) {
                                for (String s : (String[]) o) {
                                    value += s + ", ";
                                }
                            } else {
                                value += o;
                            }
                        }
                        break;

                        /****************
                         * double
                         ****************/
                        case FD: // Floating point double
                            // 8-byte floating point [Numbers in binary format]
                            // Double precision floating point number (double)
                            // BinaryValeuType.DOUBLE
                            value = valueToString(vr.toStrings(_value, isBE, characterSet));
                            break;

                        /****************
                         * float
                         ****************/
                        case FL: // Floating point single
                            // 4-byte floating point [Numbers in binary format]
                            // Single precision floating point number (float)
                            // BinaryValueType.FLOAT
                        {
                            Object o = vr.toStrings(_value, isBE, characterSet);
                            if (o instanceof String[]) {
                                for (String s : (String[]) o) {
                                    value += s + ", ";
                                }
                            } else {
                                value += o;
                            }
                        }
                        break;

                        /****************
                         * int
                         ****************/
                        case SL: // Signed long
                            // 4-byte integer [Numbers in binary format]
                            // BinaryValueType.INT
                        {
                            Object o = vr.toStrings(_value, isBE, characterSet);
                            if (o instanceof String[]) {
                                for (String s : (String[]) o) {
                                    value += s + ", ";
                                }
                            } else {
                                value += o;
                            }
                        }
                        break;

                        case US: // Unsigned short
                            // 2-byte integer [Numbers in binary format]
                            // BinaryValueType.USHORT
                        {
                            byte[] _us = vr.toBytes(_value, characterSet);
                            value = "";
                            if (_us.length <= 80) {
                                for (byte b : _us) {
                                    value += "" + b + ", ";
                                }
                            } else {
                                value = "<data size=" + eu.ensure.commons.lang.Number.asHumanApproximate(_us.length) + ">";
                            }
                        }
                        break;

                        /****************
                         * short
                         ****************/
                        case SS: // Signed short
                            // 2-byte integer [Numbers in binary format]
                            // BinaryValueType.SHORT
                            value = valueToString(vr.toStrings(_value, isBE, characterSet));
                            break;

                        /****************
                         * long
                         ****************/
                        case UL: // Unsigned long
                            // 4-byte integer [Numbers in binary format]
                            // BinaryValueType.INT
                        {
                            int[] _ul = vr.toInts(_value, isBE);
                            value = "";
                            if (_ul.length <= 80) {
                                for (int i : _ul) {
                                    value += "" + i + ", ";
                                }
                            } else {
                                value = "<data size=" + eu.ensure.commons.lang.Number.asHumanApproximate(_ul.length) + ">";
                            }
                        }
                        break;

                        /****************
                         * NO VALUE!
                         ****************/
                        case OB: // Other byte string
                            // 1-byte integers [Numbers in binary format]
                            // NOTE: Has single trailing 0x00 to make even number of bytes. Transfer Syntax determines length
                            // BinaryValueType.BYTE
                        {
                            byte[] _ob = vr.toBytes(_value, characterSet);
                            value = "";
                            if (_ob.length <= 80) {
                                for (byte b : _ob) {
                                    value += "" + b + ", ";
                                }
                            } else {
                                value = "<data size=" + eu.ensure.commons.lang.Number.asHumanApproximate(_ob.length) + ">";
                            }
                        }
                        break;

                        case OD: // Other double string
                            // BinaryValyeType.DOUBLE
                            value = valueToString(vr.toStrings(_value, isBE, characterSet));
                            break;

                        case OF: // Other float string
                            // 4-byte floating point [Numbers in binary format]
                            // BinaryValueType.FLOAT
                            value = valueToString(vr.toStrings(_value, isBE, characterSet));
                            break;

                        //case OL: // Other long string
                        // BinaryValueType.INT
                        //    value = "Other long} " + (isNull ? "<null>" : _value)
                        //     break;

                        case OW: // Other word string
                            // 2-byte integers [Numbers in binary format]
                            // Max length: -
                            // BinaryValueType.SHORT
                        {
                            byte[] _ow = vr.toBytes(_value, characterSet);
                            value = "";
                            if (_ow.length <= 80) {
                                for (byte b : _ow) {
                                    value += "" + b + ", ";
                                }
                            } else {
                                value = "<data size=" + eu.ensure.commons.lang.Number.asHumanApproximate(_ow.length) + ">";
                            }
                        }
                        break;

                        case SQ: // Sequence of items
                            // zero or more items
                            // SequenceValueType.SQ
                        {
                            Sequence sequence = (Sequence) _value;
                            String name1 = dict.keywordOf(tag);
                            if (log.isTraceEnabled()) {
                                log.trace("Found sequence: " + name1);
                            }
                            for (Attributes sequenceAttributes : sequence) {
                                if (log.isTraceEnabled()) {
                                    log.trace("Loading sequence (#" + (children.size() + 1) + ")");
                                }
                                DicomElement subObject = new DicomElement(tag, name1, sequenceAttributes, currentObject);
                                children.add(subObject);
                            }
                        }
                        return true;

                        case UN: // Unknown
                            // BinaryValueType.BYTE
                        {
                            byte[] _un = vr.toBytes(_value, characterSet);
                            if (_un.length <= 80) {
                                value = new String(_un, "ISO-8859-1");
                            } else {
                                value = "<data size=" + eu.ensure.commons.lang.Number.asHumanApproximate(_un.length) + ">";
                            }
                        }
                        break;

                        default: //
                            value = valueToString(vr.toStrings(_value, isBE, characterSet));
                            break;
                    }
                } catch (Throwable t) {
                    // If we end up here, then some of the transformations of value(s) above
                    // may be incorrect.
                    String info = "Could not determine value of tag " + TagUtils.toString(tag) + " " + dict.keywordOf(tag) + " (" + vr.name() + "): ";
                    info += t.getClass().getName();
                    log.warn(info);
                }
                elementList.add(new DicomAttribute(tag, vr, value.trim(), /* owner */ currentObject));

                return true;
            }, /* visit nested? */ false);

        } catch (Throwable t) {
            String info = "Could not process DICOM file: ";
            info += t.getMessage();
            log.info(info, t);
        }
    }

    public String asText(boolean recurse) {
        return asText(recurse, "");
    }

    public final static String INDENT = "    ";

    public String asText(boolean recurse, String prefix) {
        String text = prefix + "[";
        if (null == id || id.length() == 0) {
            text += name;
            text += " : ";
        }
        text += description + "]";
        text += "\n";

        prefix += INDENT;

        for (DicomAttribute attribute : dicomAttributes) {
            text += attribute.asText(prefix);
        }
        text += "\n";

        if (recurse) {
            for (DicomElement child : children) {
                text += child.asText(true, prefix);
            }
        }
        return text;
    }


    @Override
    public String toString() {
        return "DicomElement {" + getDescription() + "}";
    }
}
