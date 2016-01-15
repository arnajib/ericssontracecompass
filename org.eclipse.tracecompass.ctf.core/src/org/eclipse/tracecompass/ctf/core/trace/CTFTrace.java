/*******************************************************************************
 * Copyright (c) 2011, 2014 Ericsson, Ecole Polytechnique de Montreal and others
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Matthew Khouzam - Initial API and implementation
 *     Alexandre Montplaisir - Initial API and implementation
 *     Simon Delisle - Replace LinkedList by TreeSet in callsitesByName attribute
 *******************************************************************************/

package org.eclipse.tracecompass.ctf.core.trace;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.eclipse.tracecompass.ctf.core.CTFException;
import org.eclipse.tracecompass.ctf.core.CTFStrings;
import org.eclipse.tracecompass.ctf.core.event.CTFClock;
import org.eclipse.tracecompass.ctf.core.event.IEventDeclaration;
import org.eclipse.tracecompass.ctf.core.event.io.BitBuffer;
import org.eclipse.tracecompass.ctf.core.event.metadata.DeclarationScope;
import org.eclipse.tracecompass.ctf.core.event.scope.IDefinitionScope;
import org.eclipse.tracecompass.ctf.core.event.scope.ILexicalScope;
import org.eclipse.tracecompass.ctf.core.event.types.AbstractArrayDefinition;
import org.eclipse.tracecompass.ctf.core.event.types.Definition;
import org.eclipse.tracecompass.ctf.core.event.types.IDefinition;
import org.eclipse.tracecompass.ctf.core.event.types.IntegerDefinition;
import org.eclipse.tracecompass.ctf.core.event.types.StructDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.StructDefinition;
import org.eclipse.tracecompass.internal.ctf.core.SafeMappedByteBuffer;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.MetadataStrings;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.exceptions.ParseException;
import org.eclipse.tracecompass.internal.ctf.core.trace.CTFStream;
import org.eclipse.tracecompass.internal.ctf.core.trace.Utils;

/**
 * A CTF trace on the file system.
 *
 * Represents a trace on the filesystem. It is responsible of parsing the
 * metadata, creating declarations data structures, indexing the event packets
 * (in other words, all the work that can be shared between readers), but the
 * actual reading of events is left to TraceReader.
 *
 * TODO: internalize CTFTrace and DeclarationScope
 *
 * @author Matthew Khouzam
 * @version $Revision: 1.0 $
 */
public class CTFTrace implements IDefinitionScope {

    @Override
    public String toString() {
        /* Only for debugging, shouldn't be externalized */
        return "CTFTrace [path=" + fPath + ", major=" + fMajor + ", minor=" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                + fMinor + ", uuid=" + fUuid + "]"; //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * The trace directory on the filesystem.
     */
    private final File fPath;

    /**
     * Major CTF version number
     */
    private Long fMajor;

    /**
     * Minor CTF version number
     */
    private Long fMinor;

    /**
     * Trace UUID
     */
    private UUID fUuid;

    /**
     * Trace byte order
     */
    private ByteOrder fByteOrder;

    /**
     * Packet header structure declaration
     */
    private StructDeclaration fPacketHeaderDecl = null;

    /**
     * The clock of the trace
     */
    private CTFClock fSingleClock = null;

    /**
     * Packet header structure definition
     *
     * This is only used when opening the trace files, to read the first packet
     * header and see if they are valid trace files.
     */
    private StructDefinition fPacketHeaderDef;

    /**
     * Collection of streams contained in the trace.
     */
    private final Map<Long, ICTFStream> fStreams = new HashMap<>();

    /**
     * Collection of environment variables set by the tracer
     */
    private final Map<String, String> fEnvironment = new HashMap<>();

    /**
     * Collection of all the clocks in a system.
     */
    private final Map<String, CTFClock> fClocks = new HashMap<>();

    /** Handlers for the metadata files */
    private static final FileFilter METADATA_FILE_FILTER = new MetadataFileFilter();
    private static final Comparator<File> METADATA_COMPARATOR = new MetadataComparator();

    private final DeclarationScope fScope = new DeclarationScope(null, MetadataStrings.TRACE);

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    /**
     * Trace constructor.
     *
     * @param path
     *            Filesystem path of the trace directory
     * @throws CTFException
     *             If no CTF trace was found at the path
     */
    public CTFTrace(String path) throws CTFException {
        this(new File(path));
    }

    /**
     * Trace constructor.
     *
     * @param path
     *            Filesystem path of the trace directory.
     * @throws CTFException
     *             If no CTF trace was found at the path
     */
    public CTFTrace(File path) throws CTFException {
        fPath = path;
        final Metadata metadata = new Metadata(this);

        /* Set up the internal containers for this trace */
        if (!fPath.exists()) {
            throw new CTFException("Trace (" + path.getPath() + ") doesn't exist. Deleted or moved?"); //$NON-NLS-1$ //$NON-NLS-2$
        }

        if (!fPath.isDirectory()) {
            throw new CTFException("Path must be a valid directory"); //$NON-NLS-1$
        }

        /* Open and parse the metadata file */
        metadata.parseFile();

        init(path);
    }

    /**
     * Streamed constructor
     */
    public CTFTrace() {
        fPath = null;
    }

    private void init(File path) throws CTFException {

        /* Open all the trace files */

        /* List files not called metadata and not hidden. */
        File[] files = path.listFiles(METADATA_FILE_FILTER);
        Arrays.sort(files, METADATA_COMPARATOR);

        /* Try to open each file */
        for (File streamFile : files) {
            openStreamInput(streamFile);
        }

        /* Create their index */
        for (ICTFStream stream : getStreams()) {
            Set<CTFStreamInput> inputs = stream.getStreamInputs();
            for (CTFStreamInput s : inputs) {
                addStream(s);
            }
        }
    }

    // ------------------------------------------------------------------------
    // Getters/Setters/Predicates
    // ------------------------------------------------------------------------

    /**
     * Gets an event declaration list for a given streamID
     *
     * @param streamId
     *            The ID of the stream from which to read
     * @return The list of event declarations
     */
    public Collection<IEventDeclaration> getEventDeclarations(Long streamId) {
        ICTFStream stream = fStreams.get(streamId);
        if (stream == null) {
            return null;
        }
        return stream.getEventDeclarations();
    }

    /**
     * Method getStream gets the stream for a given id
     *
     * @param id
     *            Long the id of the stream
     * @return Stream the stream that we need
     * @since 2.0
     */
    public ICTFStream getStream(Long id) {
        if (id == null) {
            return fStreams.get(0L);
        }
        return fStreams.get(id);
    }

    /**
     * Method nbStreams gets the number of available streams
     *
     * @return int the number of streams
     */
    public int nbStreams() {
        return fStreams.size();
    }

    /**
     * Method setMajor sets the major version of the trace (DO NOT USE)
     *
     * @param major
     *            long the major version
     */
    public void setMajor(long major) {
        fMajor = major;
    }

    /**
     * Method setMinor sets the minor version of the trace (DO NOT USE)
     *
     * @param minor
     *            long the minor version
     */
    public void setMinor(long minor) {
        fMinor = minor;
    }

    /**
     * Method setUUID sets the UUID of a trace
     *
     * @param uuid
     *            UUID
     */
    public void setUUID(UUID uuid) {
        fUuid = uuid;
    }

    /**
     * Method setByteOrder sets the byte order
     *
     * @param byteOrder
     *            ByteOrder of the trace, can be little-endian or big-endian
     */
    public void setByteOrder(ByteOrder byteOrder) {
        fByteOrder = byteOrder;
    }

    /**
     * Method setPacketHeader sets the packet header of a trace (DO NOT USE)
     *
     * @param packetHeader
     *            StructDeclaration the header in structdeclaration form
     */
    public void setPacketHeader(StructDeclaration packetHeader) {
        fPacketHeaderDecl = packetHeader;
    }

    /**
     * Method majorIsSet is the major version number set?
     *
     * @return boolean is the major set?
     */
    public boolean majorIsSet() {
        return fMajor != null;
    }

    /**
     * Method minorIsSet. is the minor version number set?
     *
     * @return boolean is the minor set?
     */
    public boolean minorIsSet() {
        return fMinor != null;
    }

    /**
     * Method UUIDIsSet is the UUID set?
     *
     * @return boolean is the UUID set?
     */
    public boolean uuidIsSet() {
        return fUuid != null;
    }

    /**
     * Method byteOrderIsSet is the byteorder set?
     *
     * @return boolean is the byteorder set?
     */
    public boolean byteOrderIsSet() {
        return fByteOrder != null;
    }

    /**
     * Method packetHeaderIsSet is the packet header set?
     *
     * @return boolean is the packet header set?
     */
    public boolean packetHeaderIsSet() {
        return fPacketHeaderDecl != null;
    }

    /**
     * Method getUUID gets the trace UUID
     *
     * @return UUID gets the trace UUID
     */
    public UUID getUUID() {
        return fUuid;
    }

    /**
     * Method getMajor gets the trace major version
     *
     * @return long gets the trace major version
     */
    public long getMajor() {
        return fMajor;
    }

    /**
     * Method getMinor gets the trace minor version
     *
     * @return long gets the trace minor version
     */
    public long getMinor() {
        return fMinor;
    }

    /**
     * Method getByteOrder gets the trace byte order
     *
     * @return ByteOrder gets the trace byte order
     */
    public final ByteOrder getByteOrder() {
        return fByteOrder;
    }

    /**
     * Method getPacketHeader gets the trace packet header
     *
     * @return StructDeclaration gets the trace packet header
     */
    public StructDeclaration getPacketHeader() {
        return fPacketHeaderDecl;
    }

    /**
     * Method getTraceDirectory gets the trace directory
     *
     * @return File the path in "File" format.
     */
    public File getTraceDirectory() {
        return fPath;
    }

    /**
     * Get all the streams as an iterable.
     *
     * @return Iterable&lt;Stream&gt; an iterable over streams.
     */
    public Iterable<ICTFStream> getStreams() {
        return fStreams.values();
    }

    /**
     * Method getPath gets the path of the trace directory
     *
     * @return String the path of the trace directory, in string format.
     * @see java.io.File#getPath()
     */
    public String getPath() {
        return (fPath != null) ? fPath.getPath() : ""; //$NON-NLS-1$
    }

    // ------------------------------------------------------------------------
    // Operations
    // ------------------------------------------------------------------------

    private void addStream(CTFStreamInput s) {

        /*
         * add the stream
         */
        ICTFStream stream = s.getStream();
        fStreams.put(stream.getId(), stream);

        /*
         * index the trace
         */
        s.setupIndex();
    }

    /**
     * Tries to open the given file, reads the first packet header of the file
     * and check its validity. This will add a file to a stream as a streaminput
     *
     * @param streamFile
     *            A trace file in the trace directory.
     * @param index
     *            Which index in the class' streamFileChannel array this file
     *            must use
     * @throws CTFException
     *             if there is a file error
     */
    private ICTFStream openStreamInput(File streamFile) throws CTFException {
        ByteBuffer byteBuffer;
        BitBuffer streamBitBuffer;
        ICTFStream stream;

        if (!streamFile.canRead()) {
            throw new CTFException("Unreadable file : " //$NON-NLS-1$
                    + streamFile.getPath());
        }
        if (streamFile.length() == 0) {
            return null;
        }
        try (FileChannel fc = FileChannel.open(streamFile.toPath(), StandardOpenOption.READ)) {
            /* Map one memory page of 4 kiB */
            byteBuffer = SafeMappedByteBuffer.map(fc, MapMode.READ_ONLY, 0, (int) Math.min(fc.size(), 4096L));
            if (byteBuffer == null) {
                throw new IllegalStateException("Failed to allocate memory"); //$NON-NLS-1$
            }
            /* Create a BitBuffer with this mapping and the trace byte order */
            streamBitBuffer = new BitBuffer(byteBuffer, this.getByteOrder());
            if (fPacketHeaderDecl != null) {
                /* Read the packet header */
                fPacketHeaderDef = fPacketHeaderDecl.createDefinition(this, ILexicalScope.PACKET_HEADER, streamBitBuffer);
            }
        } catch (IOException e) {
            /* Shouldn't happen at this stage if every other check passed */
            throw new CTFException(e);
        }
        final StructDefinition packetHeaderDef = getPacketHeaderDef();
        if (packetHeaderDef != null) {
            validateMagicNumber(packetHeaderDef);

            validateUUID(packetHeaderDef);

            /* Read the stream ID */
            IDefinition streamIDDef = packetHeaderDef.lookupDefinition(MetadataStrings.STREAM_ID);

            if (streamIDDef instanceof IntegerDefinition) {
                /* This doubles as a null check */
                long streamID = ((IntegerDefinition) streamIDDef).getValue();
                stream = fStreams.get(streamID);
            } else {
                /* No stream_id in the packet header */
                stream = getStream(null);
            }

        } else {
            /* No packet header, we suppose there is only one stream */
            stream = getStream(null);
        }

        if (stream == null) {
            throw new CTFException("Unexpected end of stream"); //$NON-NLS-1$
        }

        if (!(stream instanceof CTFStream)) {
            throw new CTFException("Stream is not a CTFStream, but rather a " + stream.getClass().getCanonicalName()); //$NON-NLS-1$
        }
        CTFStream ctfStream = (CTFStream) stream;
        /*
         * Create the stream input and add a reference to the streamInput in the
         * stream.
         */
        ctfStream.addInput(new CTFStreamInput(ctfStream, streamFile));
        return ctfStream;
    }

    private void validateUUID(StructDefinition packetHeaderDef) throws CTFException {
        IDefinition lookupDefinition = packetHeaderDef.lookupDefinition("uuid"); //$NON-NLS-1$
        AbstractArrayDefinition uuidDef = (AbstractArrayDefinition) lookupDefinition;
        if (uuidDef != null) {
            UUID otheruuid = Utils.getUUIDfromDefinition(uuidDef);
            if (!fUuid.equals(otheruuid)) {
                throw new CTFException("UUID mismatch"); //$NON-NLS-1$
            }
        }
    }

    private static void validateMagicNumber(StructDefinition packetHeaderDef) throws CTFException {
        IntegerDefinition magicDef = (IntegerDefinition) packetHeaderDef.lookupDefinition(CTFStrings.MAGIC);
        if (magicDef != null) {
            int magic = (int) magicDef.getValue();
            if (magic != Utils.CTF_MAGIC) {
                throw new CTFException("CTF magic mismatch"); //$NON-NLS-1$
            }
        }
    }

    // ------------------------------------------------------------------------
    // IDefinitionScope
    // ------------------------------------------------------------------------

    /**
     * @since 1.0
     */
    @Override
    public ILexicalScope getScopePath() {
        return ILexicalScope.TRACE;
    }

    /**
     * Looks up a definition from packet
     *
     * @param lookupPath
     *            String
     * @return Definition
     * @see org.eclipse.tracecompass.ctf.core.event.scope.IDefinitionScope#lookupDefinition(String)
     */
    @Override
    public Definition lookupDefinition(String lookupPath) {
        if (lookupPath.equals(ILexicalScope.TRACE_PACKET_HEADER.getPath())) {
            return getPacketHeaderDef();
        }
        return null;
    }

    // ------------------------------------------------------------------------
    // Live trace reading
    // ------------------------------------------------------------------------

    /**
     * Add a new stream file to support new streams while the trace is being
     * read.
     *
     * @param streamFile
     *            the file of the stream
     * @throws CTFException
     *             A stream had an issue being read
     */
    public void addStreamFile(File streamFile) throws CTFException {
        openStreamInput(streamFile);
    }

    /**
     * Registers a new stream to the trace.
     *
     * @param stream
     *            A stream object.
     * @throws ParseException
     *             If there was some problem reading the metadata
     * @since 2.0
     */
    public void addStream(ICTFStream stream) throws ParseException {
        /*
         * If there is already a stream without id (the null key), it must be
         * the only one
         */
        if (fStreams.get(null) != null) {
            throw new ParseException("Stream without id with multiple streams"); //$NON-NLS-1$
        }

        /*
         * If the stream we try to add has no key set, it must be the only one.
         * Thus, if the streams container is not empty, it is not valid.
         */
        if ((!stream.isIdSet()) && (!fStreams.isEmpty())) {
            throw new ParseException("Stream without id with multiple streams"); //$NON-NLS-1$
        }

        /*
         * If a stream with the same ID already exists, it is not valid.
         */
        ICTFStream existingStream = fStreams.get(stream.getId());
        if (existingStream != null) {
            throw new ParseException("Stream id already exists"); //$NON-NLS-1$
        }

        /* This stream is valid and has a unique id. */
        fStreams.put(stream.getId(), stream);
    }

    /**
     * Gets the Environment variables from the trace metadata (See CTF spec)
     *
     * @return The environment variables in the form of an unmodifiable map
     *         (key, value)
     */
    public Map<String, String> getEnvironment() {
        return Collections.unmodifiableMap(fEnvironment);
    }

    /**
     * Add a variable to the environment variables
     *
     * @param varName
     *            the name of the variable
     * @param varValue
     *            the value of the variable
     */
    public void addEnvironmentVar(String varName, String varValue) {
        fEnvironment.put(varName, varValue);
    }

    /**
     * Add a clock to the clock list
     *
     * @param nameValue
     *            the name of the clock (full name with scope)
     * @param ctfClock
     *            the clock
     */
    public void addClock(String nameValue, CTFClock ctfClock) {
        fClocks.put(nameValue, ctfClock);
    }

    /**
     * gets the clock with a specific name
     *
     * @param name
     *            the name of the clock.
     * @return the clock
     */
    public CTFClock getClock(String name) {
        return fClocks.get(name);
    }

    /**
     * gets the clock if there is only one. (this is 100% of the use cases as of
     * June 2012)
     *
     * @return the clock
     */
    public final CTFClock getClock() {
        if (fSingleClock != null && fClocks.size() == 1) {
            return fSingleClock;
        }
        if (fClocks.size() == 1) {
            fSingleClock = fClocks.get(fClocks.keySet().iterator().next());
            return fSingleClock;
        }
        return null;
    }

    /**
     * gets the time offset of a clock with respect to UTC in nanoseconds
     *
     * @return the time offset of a clock with respect to UTC in nanoseconds
     */
    public final long getOffset() {
        if (getClock() == null) {
            return 0;
        }
        return fSingleClock.getClockOffset();
    }

    /**
     * gets the time offset of a clock with respect to UTC in nanoseconds
     *
     * @return the time offset of a clock with respect to UTC in nanoseconds
     */
    private double getTimeScale() {
        if (getClock() == null) {
            return 1.0;
        }
        return fSingleClock.getClockScale();
    }

    /**
     * Gets the current first packet start time
     *
     * @return the current start time
     */
    public long getCurrentStartTime() {
        long currentStart = Long.MAX_VALUE;
        for (ICTFStream stream : fStreams.values()) {
            for (CTFStreamInput si : stream.getStreamInputs()) {
                currentStart = Math.min(currentStart, si.getIndex().getElement(0).getTimestampBegin());
            }
        }
        return timestampCyclesToNanos(currentStart);
    }

    /**
     * Gets the current last packet end time
     *
     * @return the current end time
     */
    public long getCurrentEndTime() {
        long currentEnd = Long.MIN_VALUE;
        for (ICTFStream stream : fStreams.values()) {
            for (CTFStreamInput si : stream.getStreamInputs()) {
                currentEnd = Math.max(currentEnd, si.getTimestampEnd());
            }
        }
        return timestampCyclesToNanos(currentEnd);
    }

    /**
     * Does the trace need to time scale?
     *
     * @return if the trace is in ns or cycles.
     */
    private boolean clockNeedsScale() {
        if (getClock() == null) {
            return false;
        }
        return fSingleClock.isClockScaled();
    }

    /**
     * the inverse clock for returning to a scale.
     *
     * @return 1.0 / scale
     */
    private double getInverseTimeScale() {
        if (getClock() == null) {
            return 1.0;
        }
        return fSingleClock.getClockAntiScale();
    }

    /**
     * @param cycles
     *            clock cycles since boot
     * @return time in nanoseconds UTC offset
     */
    public long timestampCyclesToNanos(long cycles) {
        long retVal = cycles + getOffset();
        /*
         * this fix is since quite often the offset will be > than 53 bits and
         * therefore the conversion will be lossy
         */
        if (clockNeedsScale()) {
            retVal = (long) (retVal * getTimeScale());
        }
        return retVal;
    }

    /**
     * @param nanos
     *            time in nanoseconds UTC offset
     * @return clock cycles since boot.
     */
    public long timestampNanoToCycles(long nanos) {
        long retVal;
        /*
         * this fix is since quite often the offset will be > than 53 bits and
         * therefore the conversion will be lossy
         */
        if (clockNeedsScale()) {
            retVal = (long) (nanos * getInverseTimeScale());
        } else {
            retVal = nanos;
        }
        return retVal - getOffset();
    }

    /**
     * Add a new stream
     *
     * @param id
     *            the ID of the stream
     * @param streamFile
     *            new file in the stream
     * @throws CTFException
     *             The file must exist
     */
    public void addStream(long id, File streamFile) throws CTFException {
        final File file = streamFile;
        if (file == null) {
            throw new CTFException("cannot create a stream with no file"); //$NON-NLS-1$
        }
        ICTFStream stream = fStreams.get(id);
        if (stream == null) {
            stream = new CTFStream(this);
            fStreams.put(id, stream);
        }
        if (stream instanceof CTFStream) {
            CTFStream ctfStream = (CTFStream) stream;
            ctfStream.addInput(new CTFStreamInput(stream, file));
        } else {
            throw new CTFException("Stream does not support adding input"); //$NON-NLS-1$
        }
    }

    /**
     * Gets the current trace scope
     *
     * @return the current declaration scope
     *
     * @since 1.1
     */
    public DeclarationScope getScope() {
        return fScope;
    }

    /**
     * Gets the packet header definition (UUID, magic number and such)
     *
     * @return the packet header definition
     *
     * @since 2.0
     */
    public StructDefinition getPacketHeaderDef() {
        return fPacketHeaderDef;
    }
}

class MetadataFileFilter implements FileFilter {

    @Override
    public boolean accept(File pathname) {
        if (pathname.isDirectory()) {
            return false;
        }
        if (pathname.isHidden()) {
            return false;
        }
        if (pathname.getName().equals("metadata")) { //$NON-NLS-1$
            return false;
        }
        return true;
    }

}

class MetadataComparator implements Comparator<File>, Serializable {

    private static final long serialVersionUID = 1L;

    @Override
    public int compare(File o1, File o2) {
        return o1.getName().compareTo(o2.getName());
    }
}
