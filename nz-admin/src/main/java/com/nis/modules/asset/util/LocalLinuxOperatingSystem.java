//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.nis.modules.asset.util;

import cn.hutool.core.io.IoUtil;
import cn.hutool.extra.ssh.ChannelType;
import cn.hutool.extra.ssh.JschUtil;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.nis.common.utils.Tool;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.AbstractOperatingSystem;
import oshi.software.os.FileSystem;
import oshi.software.os.InternetProtocolStats;
import oshi.software.os.NetworkParams;
import oshi.software.os.OSProcess;
import oshi.util.Constants;
import oshi.util.ParseUtil;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Triplet;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

@ThreadSafe
public class LocalLinuxOperatingSystem extends AbstractOperatingSystem {

    private static final String RELEASE_DELIM = " release ";
    private static final String DOUBLE_QUOTES = "(?:^\")|(?:\"$)";

    private static Session session;

    public LocalLinuxOperatingSystem(Session session) {
        this.session = session;
    }

    @Override
    protected String queryManufacturer() {
        return null;
    }

    public String getFamily(Pair<String, OSVersionInfo> pair) {
        return pair.getA();
    }

    public OSVersionInfo getVersionInfo(Pair<String, OSVersionInfo> pair) {
        return pair.getB();
    }

    @Override
    public Pair<String, OSVersionInfo> queryFamilyVersionInfo() {
        Triplet<String, String, String> familyVersionCodename = queryFamilyVersionCodenameFromReleaseFiles();
        String buildNumber = null;
        List<String> procVersion = this.readRemoteFile("/proc/version");

        if (!procVersion.isEmpty()) {
            String[] split = ParseUtil.whitespaces.split(procVersion.get(0));
            for (String s : split) {
                if (!"Linux".equals(s) && !"version".equals(s)) {
                    buildNumber = s;
                    break;
                }
            }
        }
        OSVersionInfo versionInfo = new OSVersionInfo(familyVersionCodename.getB(), familyVersionCodename.getC(),
                buildNumber);
        return new Pair<>(familyVersionCodename.getA(), versionInfo);
    }

    public Triplet<String, String, String> queryFamilyVersionCodenameFromReleaseFiles() {
        Triplet<String, String, String> familyVersionCodename;
        // There are two competing options for family/version information.
        // Newer systems are adopting a standard /etc/os-release file:
        // https://www.freedesktop.org/software/systemd/man/os-release.html
        //
        // Some systems are still using the lsb standard which parses a
        // variety of /etc/*-release files and is most easily accessed via
        // the commandline lsb_release -a, see here:
        // https://linux.die.net/man/1/lsb_release
        // In this case, the /etc/lsb-release file (if it exists) has
        // optional overrides to the information in the /etc/distrib-release
        // files, which show: "Distributor release x.x (Codename)"

        // Attempt to read /etc/system-release which has more details than
        // os-release on (CentOS and Fedora)
        if ((familyVersionCodename = readDistribRelease("/etc/system-release")) != null) {
            // If successful, we're done. this.family has been set and
            // possibly the versionID and codeName
            return familyVersionCodename;
        }

        // Attempt to read /etc/os-release file.
        if ((familyVersionCodename = readOsRelease()) != null) {
            // If successful, we're done. this.family has been set and
            // possibly the versionID and codeName
            return familyVersionCodename;
        }


        // The above two options should hopefully work on most
        // distributions. If not, we keep having fun.
        // Attempt to read /etc/lsb-release file
        if ((familyVersionCodename = readLsbRelease()) != null) {
            // If successful, we're done. this.family has been set and
            // possibly the versionID and codeName
            return familyVersionCodename;
        }

        return new Triplet<>(Constants.UNKNOWN, Constants.UNKNOWN, Constants.UNKNOWN);
    }

    private Triplet<String, String, String> readDistribRelease(String filename) {
        List<String> osRelease = this.readRemoteFile(filename);
        // Search for Distrib release x.x (Codename)
        for (String line : osRelease) {
            if (line.contains(RELEASE_DELIM)) {
                // If this parses properly we're done
                return parseRelease(line, RELEASE_DELIM);
            } else if (line.contains(" VERSION ")) {
                // If this parses properly we're done
                return parseRelease(line, " VERSION ");
            }
        }
        return null;
    }

    private Triplet<String, String, String> readOsRelease() {
        String family = null;
        String versionId = Constants.UNKNOWN;
        String codeName = Constants.UNKNOWN;
        List<String> osRelease = this.readRemoteFile("/etc/os-release");
        // Search for NAME=
        for (String line : osRelease) {
            if (line.startsWith("VERSION=")) {
                // remove beginning and ending '"' characters, etc from
                // VERSION="14.04.4 LTS, Trusty Tahr" (Ubuntu style)
                // or VERSION="17 (Beefy Miracle)" (os-release doc style)
                line = line.replace("VERSION=", "").replaceAll(DOUBLE_QUOTES, "").trim();
                String[] split = line.split("[()]");
                if (split.length <= 1) {
                    // If no parentheses, check for Ubuntu's comma format
                    split = line.split(", ");
                }
                if (split.length > 0) {
                    versionId = split[0].trim();
                }
                if (split.length > 1) {
                    codeName = split[1].trim();
                }
            } else if (line.startsWith("NAME=") && family == null) {
                // remove beginning and ending '"' characters, etc from
                // NAME="Ubuntu"
                family = line.replace("NAME=", "").replaceAll(DOUBLE_QUOTES, "").trim();
            } else if (line.startsWith("VERSION_ID=") && versionId.equals(Constants.UNKNOWN)) {
                // remove beginning and ending '"' characters, etc from
                // VERSION_ID="14.04"
                versionId = line.replace("VERSION_ID=", "").replaceAll(DOUBLE_QUOTES, "").trim();
            }
        }
        return family == null ? null : new Triplet<>(family, versionId, codeName);
    }

    private Triplet<String, String, String> readLsbRelease() {
        String family = null;
        String versionId = Constants.UNKNOWN;
        String codeName = Constants.UNKNOWN;
        List<String> osRelease = this.readRemoteFile("/etc/lsb-release");
        // Search for NAME=
        for (String line : osRelease) {
            if (line.startsWith("DISTRIB_DESCRIPTION=")) {
                line = line.replace("DISTRIB_DESCRIPTION=", "").replaceAll(DOUBLE_QUOTES, "").trim();
                if (line.contains(RELEASE_DELIM)) {
                    Triplet<String, String, String> triplet = parseRelease(line, RELEASE_DELIM);
                    family = triplet.getA();
                    if (versionId.equals(Constants.UNKNOWN)) {
                        versionId = triplet.getB();
                    }
                    if (codeName.equals(Constants.UNKNOWN)) {
                        codeName = triplet.getC();
                    }
                }
            } else if (line.startsWith("DISTRIB_ID=") && family == null) {
                family = line.replace("DISTRIB_ID=", "").replaceAll(DOUBLE_QUOTES, "").trim();
            } else if (line.startsWith("DISTRIB_RELEASE=") && versionId.equals(Constants.UNKNOWN)) {
                versionId = line.replace("DISTRIB_RELEASE=", "").replaceAll(DOUBLE_QUOTES, "").trim();
            } else if (line.startsWith("DISTRIB_CODENAME=") && codeName.equals(Constants.UNKNOWN)) {
                codeName = line.replace("DISTRIB_CODENAME=", "").replaceAll(DOUBLE_QUOTES, "").trim();
            }
        }
        return family == null ? null : new Triplet<>(family, versionId, codeName);
    }

    private Triplet<String, String, String> parseRelease(String line, String splitLine) {
        String[] split = line.split(splitLine);
        String family = split[0].trim();
        String versionId = Constants.UNKNOWN;
        String codeName = Constants.UNKNOWN;
        if (split.length > 1) {
            split = split[1].split("[()]");
            if (split.length > 0) {
                versionId = split[0].trim();
            }
            if (split.length > 1) {
                codeName = split[1].trim();
            }
        }
        return new Triplet<>(family, versionId, codeName);
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

    public static List<String> runRemoteCommand(String cmdToRun) {
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

    @Override
    protected int queryBitness(int i) {
        return 0;
    }

    @Override
    protected List<OSProcess> queryAllProcesses() {
        return null;
    }

    @Override
    protected List<OSProcess> queryChildProcesses(int i) {
        return null;
    }

    @Override
    protected List<OSProcess> queryDescendantProcesses(int i) {
        return null;
    }

    @Override
    public FileSystem getFileSystem() {
        return null;
    }

    @Override
    public InternetProtocolStats getInternetProtocolStats() {
        return null;
    }

    @Override
    public OSProcess getProcess(int i) {
        return null;
    }

    @Override
    public int getProcessId() {
        return 0;
    }

    @Override
    public int getProcessCount() {
        return 0;
    }

    @Override
    public int getThreadCount() {
        return 0;
    }

    @Override
    public long getSystemUptime() {
        return 0;
    }

    @Override
    public long getSystemBootTime() {
        return 0;
    }

    @Override
    public NetworkParams getNetworkParams() {
        return null;
    }

    public static class LocalLinuxNetworkParams {

        public LocalLinuxNetworkParams() {

        }

        public String getIpv4DefaultGateway() {
            List<String> routes = runRemoteCommand("route -A inet -n");
            if (routes.size() <= 2) {
                return "";
            } else {
                String gateway = "";
                int minMetric = Integer.MAX_VALUE;

                for (int i = 2; i < routes.size(); ++i) {
                    String[] fields = ParseUtil.whitespaces.split((CharSequence) routes.get(i));
                    if (fields.length > 4 && fields[0].equals("0.0.0.0")) {
                        boolean isGateway = fields[3].indexOf(71) != -1;
                        int metric = ParseUtil.parseIntOrDefault(fields[4], Integer.MAX_VALUE);
                        if (isGateway && metric < minMetric) {
                            minMetric = metric;
                            gateway = fields[1];
                        }
                    }
                }

                return gateway;
            }
        }

        public String getIpv6DefaultGateway() {
            List<String> routes = runRemoteCommand("route -A inet6 -n");
            if (routes.size() <= 2) {
                return "";
            } else {
                String gateway = "";
                int minMetric = Integer.MAX_VALUE;

                for (int i = 2; i < routes.size(); ++i) {
                    String[] fields = ParseUtil.whitespaces.split((CharSequence) routes.get(i));
                    if (fields.length > 3 && fields[0].equals("::/0")) {
                        boolean isGateway = fields[2].indexOf(71) != -1;
                        int metric = ParseUtil.parseIntOrDefault(fields[3], Integer.MAX_VALUE);
                        if (isGateway && metric < minMetric) {
                            minMetric = metric;
                            gateway = fields[1];
                        }
                    }
                }
                return gateway;
            }
        }
    }
}
