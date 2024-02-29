/*
 * Copyright 2016-2024 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package com.nis.modules.asset.util;

import cn.hutool.core.io.IoUtil;
import cn.hutool.extra.ssh.ChannelType;
import cn.hutool.extra.ssh.JschUtil;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.nis.common.utils.Tool;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.annotation.concurrent.Immutable;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.linux.Lshw;
import oshi.hardware.CentralProcessor;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.Util;
import oshi.util.tuples.Triplet;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A CPU as defined in Linux /proc.
 */
@ThreadSafe
@Data
public class LoacalLinuxCentralProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(LoacalLinuxCentralProcessor.class);

    public static final String MODEL = "/proc/device-tree/model";
    public static final String CPUINFO = "/proc/cpuinfo";
    public static final String STAT = "/proc/stat";


    private Session session;

    private int physicalPackageCount;
    private int physicalProcessorCount;
    private int logicalProcessorCount;
    private List<CentralProcessor.LogicalProcessor> logicalProcessors;
    private List<CentralProcessor.PhysicalProcessor> physicalProcessors;

    public LoacalLinuxCentralProcessor(Session session) {
        this.session = session;
        this.init();
    }

    public void init() {
        Triplet<List<LogicalProcessor>, List<PhysicalProcessor>, List<ProcessorCache>> processorLists = this.initProcessorCounts();
        this.logicalProcessors = Collections.unmodifiableList((List) processorLists.getA());
        if (processorLists.getB() == null) {
            Set<Integer> pkgCoreKeys = (Set) this.logicalProcessors.stream().map((p) -> {
                return (p.getPhysicalPackageNumber() << 16) + p.getPhysicalProcessorNumber();
            }).collect(Collectors.toSet());
            List<CentralProcessor.PhysicalProcessor> physProcs = (List) pkgCoreKeys.stream().sorted().map((k) -> {
                return new CentralProcessor.PhysicalProcessor(k >> 16, k & '\uffff');
            }).collect(Collectors.toList());
            this.physicalProcessors = Collections.unmodifiableList(physProcs);
        } else {
            this.physicalProcessors = Collections.unmodifiableList((List) processorLists.getB());
        }

        Set<Integer> physPkgs = new HashSet();
        Iterator var7 = this.logicalProcessors.iterator();

        while (var7.hasNext()) {
            LogicalProcessor logProc = (LogicalProcessor) var7.next();
            int pkg = logProc.getPhysicalPackageNumber();
            physPkgs.add(pkg);
        }

        this.logicalProcessorCount = this.logicalProcessors.size();
        this.physicalProcessorCount = this.physicalProcessors.size();
        this.physicalPackageCount = physPkgs.size();
    }

    protected Triplet<List<LogicalProcessor>, List<PhysicalProcessor>, List<ProcessorCache>> initProcessorCounts() {
        // Attempt to read from sysfs
        Triplet<List<LogicalProcessor>, Map<Integer, Integer>, Map<Integer, String>> topology = readTopologyFromSysfs();
        // This sometimes fails so fall back to CPUID
        if (topology.getA().isEmpty()) {
            topology = this.readTopologyFromCpuinfo();
        }
        List<LogicalProcessor> logProcs = topology.getA();
        Map<Integer, Integer> coreEfficiencyMap = topology.getB();
        Map<Integer, String> modAliasMap = topology.getC();
        // Failsafe
        if (logProcs.isEmpty()) {
            logProcs.add(new LogicalProcessor(0, 0, 0));
        }
        if (coreEfficiencyMap.isEmpty()) {
            coreEfficiencyMap.put(0, 0);
        }
        // Sort
        logProcs.sort(Comparator.comparingInt(LogicalProcessor::getProcessorNumber));

        List<PhysicalProcessor> physProcs = coreEfficiencyMap.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    int pkgId = e.getKey() >> 16;
                    int coreId = e.getKey() & 0xffff;
                    return new PhysicalProcessor(pkgId, coreId, e.getValue(), modAliasMap.getOrDefault(e.getKey(), ""));
                }).collect(Collectors.toList());

        return new Triplet<>(logProcs, physProcs, null);
    }

    private Triplet<List<LogicalProcessor>, Map<Integer, Integer>, Map<Integer, String>> readTopologyFromSysfs() {
        List<LogicalProcessor> logProcs = new ArrayList<>();
        Map<Integer, Integer> coreEfficiencyMap = new HashMap<>();
        Map<Integer, String> modAliasMap = new HashMap<>();
        String cpuPath = "/sys/devices/system/cpu/";
        try {
            List<String> cpuFiles = this.runRemoteCommand("ls -d /sys/devices/system/cpu/cpu[0-9]*");
            cpuFiles.forEach(cpu -> {
                String syspath = cpu.toString(); // /sys/devices/system/cpu/cpuX
                Map<String, String> uevent = this.getKeyValueMapFromFile(syspath + "/uevent", "=");
                String modAlias = uevent.get("MODALIAS");
                // updates caches as a side-effect
                logProcs.add(
                        getLogicalProcessorFromSyspath(syspath, modAlias, coreEfficiencyMap, modAliasMap));
            });
        } catch (Exception e) {
            e.printStackTrace();
            // No udev and no cpu info in sysfs? Bad.
            LOG.warn("Unable to find CPU information in sysfs at path {}", cpuPath);
        }
        return new Triplet<>(logProcs, coreEfficiencyMap, modAliasMap);
    }

    private LogicalProcessor getLogicalProcessorFromSyspath(String syspath, String modAlias, Map<Integer, Integer> coreEfficiencyMap, Map<Integer, String> modAliasMap) {
        int processor = ParseUtil.getFirstIntValue(syspath);
        int coreId = this.getIntFromFile(syspath + "/topology/core_id");
        int pkgId = this.getIntFromFile(syspath + "/topology/physical_package_id");
        int pkgCoreKey = (pkgId << 16) + coreId;
        // The cpu_capacity value may not exist, this will just store 0
        coreEfficiencyMap.put(pkgCoreKey, this.getIntFromFile(syspath + "/cpu_capacity"));
        if (!Util.isBlank(modAlias)) {
            modAliasMap.put(pkgCoreKey, modAlias);
        }
        int nodeId = 0;
        // final String nodePrefix = syspath + "/node";
        // try (Stream<Path> path = Files.list(Paths.get(syspath))) {
        //     Optional<Path> first = path.filter(p -> p.toString().startsWith(nodePrefix)).findFirst();
        //     if (first.isPresent()) {
        //         nodeId = ParseUtil.getFirstIntValue(first.get().getFileName().toString());
        //     }
        // } catch (IOException e) {
        //     // ignore
        // }
        return new LogicalProcessor(processor, coreId, pkgId, nodeId);
    }

    private Triplet<List<LogicalProcessor>, Map<Integer, Integer>, Map<Integer, String>> readTopologyFromCpuinfo() {
        List<LogicalProcessor> logProcs = new ArrayList<>();
        Map<Integer, Integer> numaNodeMap = mapNumaNodesFromLscpu();
        Map<Integer, Integer> coreEfficiencyMap = new HashMap<>();

        List<String> procCpu = this.readRemoteFile(CPUINFO);
        int currentProcessor = 0;
        int currentCore = 0;
        int currentPackage = 0;

        boolean first = true;
        for (String cpu : procCpu) {
            // Count logical processors
            if (cpu.startsWith("processor")) {
                if (first) {
                    first = false;
                } else {
                    // add from the previous iteration
                    logProcs.add(new LogicalProcessor(currentProcessor, currentCore, currentPackage,
                            numaNodeMap.getOrDefault(currentProcessor, 0)));
                    // Count unique combinations of core id and physical id.
                    coreEfficiencyMap.put((currentPackage << 16) + currentCore, 0);
                }
                // start creating for this iteration
                currentProcessor = ParseUtil.parseLastInt(cpu, 0);
            } else if (cpu.startsWith("core id") || cpu.startsWith("cpu number")) {
                currentCore = ParseUtil.parseLastInt(cpu, 0);
            } else if (cpu.startsWith("physical id")) {
                currentPackage = ParseUtil.parseLastInt(cpu, 0);
            }
        }
        logProcs.add(new LogicalProcessor(currentProcessor, currentCore, currentPackage,
                numaNodeMap.getOrDefault(currentProcessor, 0)));
        coreEfficiencyMap.put((currentPackage << 16) + currentCore, 0);
        return new Triplet<>(logProcs, coreEfficiencyMap, Collections.emptyMap());
    }

    public long getContextSwitches() {
        List<String> procStat = this.readRemoteFile(STAT);
        Iterator var1 = procStat.iterator();

        while (var1.hasNext()) {
            String stat = (String) var1.next();
            if (stat.startsWith("ctxt ")) {
                String[] ctxtArr = ParseUtil.whitespaces.split(stat);
                if (ctxtArr.length == 2) {
                    return ParseUtil.parseLongOrDefault(ctxtArr[1], 0L);
                }
            }
        }
        return 0L;
    }

    public long getInterrupts() {
        List<String> procStat = this.readRemoteFile(STAT);
        Iterator var1 = procStat.iterator();

        while (var1.hasNext()) {
            String stat = (String) var1.next();
            if (stat.startsWith("intr ")) {
                String[] intrArr = ParseUtil.whitespaces.split(stat);
                if (intrArr.length > 2) {
                    return ParseUtil.parseLongOrDefault(intrArr[1], 0L);
                }
            }
        }
        return 0L;
    }

    public CentralProcessor.ProcessorIdentifier queryProcessorId() {
        String cpuVendor = "";
        String cpuName = "";
        String cpuFamily = "";
        String cpuModel = "";
        String cpuStepping = "";
        long cpuFreq = 0L;
        boolean cpu64bit = false;
        StringBuilder armStepping = new StringBuilder();
        String[] flags = new String[0];
        List<String> cpuInfo = this.readRemoteFile(CPUINFO);
        Iterator var13 = cpuInfo.iterator();

        while (true) {
            label111:
            while (var13.hasNext()) {
                String line = (String) var13.next();
                String[] splitLine = ParseUtil.whitespacesColonWhitespace.split(line);
                if (splitLine.length < 2) {
                    if (line.startsWith("CPU architecture: ")) {
                        cpuFamily = line.replace("CPU architecture: ", "").trim();
                    }
                } else {
                    switch (splitLine[0]) {
                        case "vendor_id":
                        case "CPU implementer":
                            cpuVendor = splitLine[1];
                            break;
                        case "model name":
                        case "Processor":
                            cpuName = splitLine[1];
                            break;
                        case "flags":
                            flags = splitLine[1].toLowerCase().split(" ");
                            String[] var18 = flags;
                            int var19 = flags.length;
                            int var20 = 0;

                            while (true) {
                                if (var20 >= var19) {
                                    continue label111;
                                }

                                String flag = var18[var20];
                                if ("lm".equals(flag)) {
                                    cpu64bit = true;
                                    continue label111;
                                }

                                ++var20;
                            }
                        case "stepping":
                            cpuStepping = splitLine[1];
                            break;
                        case "CPU variant":
                            if (!armStepping.toString().startsWith("r")) {
                                armStepping.insert(0, "r" + splitLine[1]);
                            }
                            break;
                        case "CPU revision":
                            if (!armStepping.toString().contains("p")) {
                                armStepping.append('p').append(splitLine[1]);
                            }
                            break;
                        case "model":
                        case "CPU part":
                            cpuModel = splitLine[1];
                            break;
                        case "cpu family":
                            cpuFamily = splitLine[1];
                            break;
                        case "cpu MHz":
                            cpuFreq = ParseUtil.parseHertz(splitLine[1]);
                    }
                }
            }

            if (cpuName.contains("Hz")) {
                cpuFreq = -1L;
            } else {
                long cpuCapacity = Lshw.queryCpuCapacity();
                if (cpuCapacity > cpuFreq) {
                    cpuFreq = cpuCapacity;
                }
            }

            if (cpuStepping.isEmpty()) {
                cpuStepping = armStepping.toString();
            }

            String processorID = "";
//            if (cpuVendor.startsWith("0x")) {
//                List<String> lscpu = ExecutingCommand.runNative("lscpu");
//                Iterator var24 = lscpu.iterator();
//
//                while(var24.hasNext()) {
//                    String line = (String)var24.next();
//                    if (line.startsWith("Architecture:")) {
//                        cpuVendor = line.replace("Architecture:", "").trim();
//                    }
//                }
//            }
            return new CentralProcessor.ProcessorIdentifier(cpuVendor, cpuName, cpuFamily, cpuModel, cpuStepping, processorID, cpu64bit, cpuFreq);
        }
    }


    private static Map<Integer, Integer> mapNumaNodesFromLscpu() {
        Map<Integer, Integer> numaNodeMap = new HashMap<>();
        // Get numa node info from lscpu
        List<String> lscpu = ExecutingCommand.runNative("lscpu -p=cpu,node");
        // Format:
        // # comment lines starting with #
        // # then comma-delimited cpu,node
        // 0,0
        // 1,0
        for (String line : lscpu) {
            if (!line.startsWith("#")) {
                int pos = line.indexOf(',');
                if (pos > 0 && pos < line.length()) {
                    numaNodeMap.put(ParseUtil.parseIntOrDefault(line.substring(0, pos), 0),
                            ParseUtil.parseIntOrDefault(line.substring(pos + 1), 0));
                }
            }
        }
        return numaNodeMap;
    }

    public int getIntFromFile(String filename) {
        try {
            List<String> read = this.readRemoteFile(filename);
            if (!read.isEmpty()) {
                return Integer.parseInt((String) read.get(0));
            }
        } catch (NumberFormatException var2) {
        }
        return 0;
    }

    public Map<String, String> getKeyValueMapFromFile(String filename, String separator) {
        Map<String, String> map = new HashMap();

        List<String> lines = this.readRemoteFile(filename);
        Iterator var4 = lines.iterator();

        while (var4.hasNext()) {
            String line = (String) var4.next();
            String[] parts = line.split(separator);
            if (parts.length == 2) {
                map.put(parts[0], parts[1].trim());
            }
        }
        return map;
    }

    public List<String> readRemoteFile(String filename) {
        List<String> list = Tool.ListUtil.list(true);
        try {
            ChannelExec channel = (ChannelExec) JschUtil.createChannel(session, ChannelType.EXEC);
            channel.setCommand("cat " + filename);
            channel.connect();

            IoUtil.readLines(channel.getInputStream(), Charset.forName("UTF-8"), list);
            JschUtil.close(channel);
        } catch (JSchException | IOException e) {
        }
        return list;
    }

    public List<String> runRemoteCommand(String cmdToRun) {
        List<String> list = Tool.ListUtil.list(true);
        try {
            ChannelExec channel = (ChannelExec) JschUtil.createChannel(session, ChannelType.EXEC);
            channel.setCommand(cmdToRun);
            channel.connect();

            IoUtil.readLines(channel.getInputStream(), Charset.forName("UTF-8"), list);
            JschUtil.close(channel);
        } catch (JSchException | IOException e) {
        }
        return list;
    }


    @Immutable
    class PhysicalProcessor {
        private final int physicalPackageNumber;
        private final int physicalProcessorNumber;
        private final int efficiency;
        private final String idString;

        public PhysicalProcessor(int physicalPackageNumber, int physicalProcessorNumber) {
            this(physicalPackageNumber, physicalProcessorNumber, 0, "");
        }

        public PhysicalProcessor(int physicalPackageNumber, int physicalProcessorNumber, int efficiency,
                                 String idString) {
            this.physicalPackageNumber = physicalPackageNumber;
            this.physicalProcessorNumber = physicalProcessorNumber;
            this.efficiency = efficiency;
            this.idString = idString;
        }

        /**
         * Gets a platform specific measure of processor performance vs. efficiency, useful for identifying cores in
         * hybrid/System on Chip (SoC) processors such as ARM's big.LITTLE architecture, Apple's M1, and Intel's P-core
         * and E-core hybrid technology. A core with a higher value for the efficiency class has intrinsically greater
         * performance and less efficiency than a core with a lower value for the efficiency class.
         *
         * @return On Windows 10 and higher, returns the {@code EfficiencyClass} value from the
         * {@code PROCESSOR_RELATIONSHIP} structure.
         * <p>
         * On macOS with Apple Silicon, emulates the same relative efficiency class values as Windows.
         * <p>
         * On Linux, returns the {@code cpu_capacity} value from sysfs. This is an optional cpu node property
         * representing CPU capacity expressed in normalized DMIPS/MHz.
         * <p>
         * On OpenBSD, FreeBSD, and Solaris with ARM big.LITTLE processors, emulates the same relative
         * efficiency class values as Windows.
         * <p>
         * For unimplemented operating systems or architectures, returns 0.
         * @see <a href=
         * "https://docs.microsoft.com/en-us/windows/win32/api/winnt/ns-winnt-processor_relationship">PROCESSOR_RELATIONSHIP</a>
         * @see <a href=
         * "https://www.kernel.org/doc/Documentation/devicetree/bindings/arm/cpu-capacity.txt">cpu-capacity</a>
         */
        public int getEfficiency() {
            return efficiency;
        }

        /**
         * Gets a platform specific identification string representing this core. This string requires user parsing to
         * obtain meaningful information. As this is an experimental feature, users should not rely on the format.
         *
         * @return On Windows, returns the per-core Processor ID (CPUID).
         * <p>
         * On macOS, returns a compatibility string from the IO Registry identifying hybrid cores.
         * <p>
         * On Linux, returns the {@code MODALIAS} value for the core's driver.
         * <p>
         * On OpenBSD, FreeBSD, and Solaris, returns a per-core CPU identification string.
         * <p>
         * For unimplemented operating systems, returns an empty string.
         */
        public String getIdString() {
            return idString;
        }

        @Override
        public String toString() {
            return "PhysicalProcessor [package/core=" + physicalPackageNumber + "/" + physicalProcessorNumber
                    + ", efficiency=" + efficiency + ", idString=" + idString + "]";
        }
    }

    @Immutable
    class ProcessorCache {

        /**
         * The type of cache.
         */
        public enum Type {
            UNIFIED, INSTRUCTION, DATA, TRACE;

            @Override
            public String toString() {
                return name().substring(0, 1) + name().substring(1).toLowerCase(Locale.ROOT);
            }
        }

        private final byte level;
        private final byte associativity;
        private final short lineSize;
        private final int cacheSize;
        private final Type type;

        public ProcessorCache(byte level, byte associativity, short lineSize, int cacheSize, Type type) {
            this.level = level;
            this.associativity = associativity;
            this.lineSize = lineSize;
            this.cacheSize = cacheSize;
            this.type = type;
        }

        public ProcessorCache(int level, int associativity, int lineSize, long cacheSize, Type type) {
            this((byte) level, (byte) associativity, (short) lineSize, (int) cacheSize, type);
        }

        /**
         * The cache level. This member can be 1 (L1), 2 (L2), 3 (L3), or 4 (L4).
         *
         * @return the level
         */
        public byte getLevel() {
            return level;
        }

        /**
         * The cache associativity. If this member is {@code 0xFF}, the cache is fully associative.
         *
         * @return the associativity
         */
        public byte getAssociativity() {
            return associativity;
        }

        /**
         * The cache line size, in bytes.
         *
         * @return the line size
         */
        public short getLineSize() {
            return lineSize;
        }

        /**
         * The cache size, in bytes.
         *
         * @return the cache size
         */
        public int getCacheSize() {
            return cacheSize;
        }

        /**
         * The cache type.
         *
         * @return the type
         */
        public Type getType() {
            return type;
        }

        @Override
        public String toString() {
            return "ProcessorCache [L" + level + " " + type + ", cacheSize=" + cacheSize + ", "
                    + (associativity > 0 ? associativity + "-way" : "unknown") + " associativity, lineSize=" + lineSize
                    + "]";
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || !(obj instanceof ProcessorCache)) {
                return false;
            }
            ProcessorCache other = (ProcessorCache) obj;
            return associativity == other.associativity && cacheSize == other.cacheSize && level == other.level
                    && lineSize == other.lineSize && type == other.type;
        }

        @Override
        public int hashCode() {
            return Objects.hash(associativity, cacheSize, level, lineSize, type);
        }
    }


    class LogicalProcessor {
        private final int processorNumber;
        private final int physicalProcessorNumber;
        private final int physicalPackageNumber;
        private final int numaNode;
        private final int processorGroup;

        public LogicalProcessor(int processorNumber, int physicalProcessorNumber, int physicalPackageNumber) {
            this(processorNumber, physicalProcessorNumber, physicalPackageNumber, 0, 0);
        }

        public LogicalProcessor(int processorNumber, int physicalProcessorNumber, int physicalPackageNumber, int numaNode) {
            this(processorNumber, physicalProcessorNumber, physicalPackageNumber, numaNode, 0);
        }

        public LogicalProcessor(int processorNumber, int physicalProcessorNumber, int physicalPackageNumber, int numaNode, int processorGroup) {
            this.processorNumber = processorNumber;
            this.physicalProcessorNumber = physicalProcessorNumber;
            this.physicalPackageNumber = physicalPackageNumber;
            this.numaNode = numaNode;
            this.processorGroup = processorGroup;
        }

        public int getProcessorNumber() {
            return this.processorNumber;
        }

        public int getPhysicalProcessorNumber() {
            return this.physicalProcessorNumber;
        }

        public int getPhysicalPackageNumber() {
            return this.physicalPackageNumber;
        }

        public int getNumaNode() {
            return this.numaNode;
        }

        public int getProcessorGroup() {
            return this.processorGroup;
        }

        public String toString() {
            return "LogicalProcessor [processorNumber=" + this.processorNumber + ", coreNumber=" + this.physicalProcessorNumber + ", packageNumber=" + this.physicalPackageNumber + ", numaNode=" + this.numaNode + ", processorGroup=" + this.processorGroup + "]";
        }
    }
}
